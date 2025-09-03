package com.microswitch.application.metric;

import java.util.Map;

public interface DeploymentMetricsService {

    Map<String, Object> getAllDeploymentMetrics();

    double getCanarySuccessRate(String serviceKey);

    double getShadowAccuracyRate(String serviceKey, String strategy);

    String getBlueGreenStatus(String serviceKey);
}
