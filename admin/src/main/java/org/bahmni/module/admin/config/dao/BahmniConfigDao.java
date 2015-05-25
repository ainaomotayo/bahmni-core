package org.bahmni.module.admin.config.dao;

import org.bahmni.module.admin.config.model.BahmniConfig;

import java.util.List;

public interface BahmniConfigDao {
    BahmniConfig get(String appName, String configName);

    List<BahmniConfig> getAllFor(String appName);
}