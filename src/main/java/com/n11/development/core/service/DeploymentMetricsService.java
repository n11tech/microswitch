package com.n11.development.core.service;

import java.util.Map;

public interface DeploymentMetricsService {

    Map<String, Object> getAllDeploymentMetrics();

    double getCanarySuccessRate(String serviceKey);

    double getShadowAccuracyRate(String serviceKey, String strategy);

    String getBlueGreenStatus(String serviceKey);
}
