package org.bahmni.module.bahmnicore.service.impl;

import org.bahmni.module.bahmnicore.service.OrderService;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:applicationContext-Test.xml"}, inheritLocations = true)
public class OrderServiceImplIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private OrderService bahmniOrderService;

    @Autowired
    private org.openmrs.api.OrderService orderService;
    @Autowired
    private PatientService patientService;

    @Test
    public void shouldCheckForExistenceOfConcept() throws Exception {
        String patientUuid = "75e04d42-3ca8-11e3-bf2b-0800271c1b75";
        String radiologyOrderTypeUuid = "92c1bdef-72d4-49d9-8a1f-804892f66abd";

        executeDataSet("radiologyOrderTestData.xml");
        ensureCorrectDataSetup(patientUuid, radiologyOrderTypeUuid);

        List<Order> pendingOrders = bahmniOrderService.getPendingOrders(patientUuid, radiologyOrderTypeUuid);

        Assert.assertEquals(1, pendingOrders.size());

        Order pendingOrder = pendingOrders.get(0);
        Assert.assertEquals("6d0ae386-707a-4629-9850-f15206e63ab0", pendingOrder.getUuid());
        Assert.assertEquals(patientUuid, pendingOrder.getPatient().getUuid());
        Assert.assertEquals(radiologyOrderTypeUuid, pendingOrder.getOrderType().getUuid());
        Assert.assertEquals("Radiology Order", pendingOrder.getOrderType().getName());
    }

    private void ensureCorrectDataSetup(String patientUuid, String radiologyOrderTypeUuid) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        OrderType orderType = orderService.getOrderTypeByUuid(radiologyOrderTypeUuid);
        List<Order> allRadiologyOrdersForPatient = orderService.getOrders(Order.class, Arrays.asList(patient), null, org.openmrs.api.OrderService.ORDER_STATUS.NOTVOIDED, null, null, Arrays.asList(orderType));
        Assert.assertTrue("More than 1 radiology orders are setup for the patient", allRadiologyOrdersForPatient.size() > 1);
    }

}