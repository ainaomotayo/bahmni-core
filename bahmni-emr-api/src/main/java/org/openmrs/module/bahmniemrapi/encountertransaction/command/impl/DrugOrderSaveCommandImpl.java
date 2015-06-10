package org.openmrs.module.bahmniemrapi.encountertransaction.command.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.module.bahmniemrapi.drugorder.DrugOrderUtil;
import org.openmrs.module.bahmniemrapi.encountertransaction.command.EncounterDataPreSaveCommand;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.encounter.service.OrderMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DrugOrderSaveCommandImpl implements EncounterDataPreSaveCommand {

    private OrderMetadataService orderMetadataService;
    private ConceptService conceptService;

    Comparator<EncounterTransaction.DrugOrder> drugOrderStartDateComparator = new Comparator<EncounterTransaction.DrugOrder>() {
        @Override
        public int compare(EncounterTransaction.DrugOrder o1, EncounterTransaction.DrugOrder o2) {
            Date date1 = o1.getScheduledDate();
            Date date2 = o2.getScheduledDate();
            if(date1 == null){
                date1 = new Date();
            }
            if(date2 == null){
                date2 = new Date();
            }
            return date1.compareTo(date2);
        }
    };


    @Autowired
    public DrugOrderSaveCommandImpl(OrderMetadataService orderMetadataService, ConceptService conceptService) {
        this.orderMetadataService = orderMetadataService;
        this.conceptService = conceptService;
    }

    @Override
    public BahmniEncounterTransaction update(BahmniEncounterTransaction bahmniEncounterTransaction) {
        List<EncounterTransaction.DrugOrder> drugOrders = bahmniEncounterTransaction.getDrugOrders();
        Map<String,List<EncounterTransaction.DrugOrder>> sameDrugNameOrderLists = new LinkedHashMap<>();
        for (EncounterTransaction.DrugOrder drugOrder : drugOrders) {
            String name = drugOrder.getDrug().getName();
            if(sameDrugNameOrderLists.get(name) == null){
                sameDrugNameOrderLists.put(name, new ArrayList<EncounterTransaction.DrugOrder>());
            }
            sameDrugNameOrderLists.get(name).add(drugOrder);
        }

        for (List<EncounterTransaction.DrugOrder> orders : sameDrugNameOrderLists.values()) {
            Collections.sort(orders, drugOrderStartDateComparator);
            checkAndFixChainOverlapsWithCurrentDateOrder(orders);
        }

        return bahmniEncounterTransaction;
    }

    private void checkAndFixChainOverlapsWithCurrentDateOrder(Collection<EncounterTransaction.DrugOrder> orders) {
//        Refactor using Lambda expressions after updating to Java 8
        EncounterTransaction.DrugOrder currentDateOrder = getCurrentOrderFromOrderList(orders);

        if(currentDateOrder != null){
            Date expectedStartDateForCurrentOrder = setExpectedStartDateForOrder(currentDateOrder);
            Date expectedStopDateForCurrentOrder = setExpectedStopDateForOrder(currentDateOrder, expectedStartDateForCurrentOrder);

            for (EncounterTransaction.DrugOrder order : orders) {
                if(order!=currentDateOrder && !"DISCONTINUE".equals(order.getAction()) && DateUtils.isSameDay(setExpectedStartDateForOrder(order), expectedStopDateForCurrentOrder)){
                    currentDateOrder.setScheduledDate(expectedStartDateForCurrentOrder);
                    currentDateOrder.setAutoExpireDate(expectedStopDateForCurrentOrder);

                    order.setScheduledDate(DrugOrderUtil.aSecondAfter(expectedStopDateForCurrentOrder));

                    currentDateOrder = order;
                    expectedStartDateForCurrentOrder = setExpectedStartDateForOrder(order);
                    expectedStopDateForCurrentOrder = setExpectedStopDateForOrder(currentDateOrder, expectedStartDateForCurrentOrder);
                }else if(!"DISCONTINUE".equals(order.getAction()) && order.getScheduledDate() == null){
                    order.setScheduledDate(expectedStartDateForCurrentOrder);
                }
            }
        }
    }

    private Date setExpectedStopDateForOrder(EncounterTransaction.DrugOrder order, Date expectedStartDateForCurrentOrder) {
        Concept durationUnitConcept = conceptService.getConceptByName(order.getDurationUnits());
        return DrugOrderUtil.calculateAutoExpireDate(order.getDuration(), durationUnitConcept, null, expectedStartDateForCurrentOrder, orderMetadataService.getOrderFrequencyByName(order.getDosingInstructions().getFrequency(), false));
    }

    private Date setExpectedStartDateForOrder(EncounterTransaction.DrugOrder order) {
        if( order.getScheduledDate() == null){
            return new Date();
        }
        return order.getScheduledDate();
    }

    private EncounterTransaction.DrugOrder getCurrentOrderFromOrderList(Collection<EncounterTransaction.DrugOrder> orders) {
        for (EncounterTransaction.DrugOrder order : orders) {
            if (!"DISCONTINUE".equals(order.getAction())) { // To detect orders with dateActivated = current date
                return order;
            }
        }
        return null;
    }

}
