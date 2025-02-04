package org.peergos.config;

import org.peergos.util.JsonHelper;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class MetricsSection implements Jsonable {
    public final boolean enabled;
    public final String address;
    public final int port;

    public MetricsSection(boolean enabled, String address, int port) {
        this.enabled = enabled;
        this.address = address;
        this.port = port;
    }

    public static MetricsSection defaultConfig(Optional<Integer> instance_id) {
        System.out.println("Creating default metrics config, for instance_id: "+instance_id);
        if (instance_id.isPresent()) {
            return new MetricsSection(true, "localhost", Integer.parseInt("910" + instance_id.get()));
        } else {
            return new MetricsSection(true, "localhost", Integer.parseInt("9100"));
        }
    }

    public Map<String, Object> toJson() {
        Map<String, Object> metricsMap = new LinkedHashMap<>();
        metricsMap.put("Enabled", enabled);
        metricsMap.put("Address", address);
        metricsMap.put("Port", port);
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("Metrics", metricsMap);
        return configMap;
    }
    public static MetricsSection fromJson(Map<String, Object> json) {
        Optional<Map<String, Object>> metricsOpt =  JsonHelper.getOptionalPropertyMap(json, "Metrics");
        MetricsSection metrics = metricsOpt.map( f ->
             new MetricsSection(JsonHelper.getBooleanProperty(f, "Enabled"),
                JsonHelper.getStringProperty(f, "Address"),
                JsonHelper.getIntProperty(f, "Port"))
        ).get(); //.orElse(MetricsSection.defaultConfig());
        return metrics;
    }
}
