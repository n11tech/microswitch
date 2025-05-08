package com.n11.development.application.config;

import java.util.Map;

public interface IEmbDeployerLoader {
    Map<String, EmbDeployer.ServiceConfig> getConfiguration(String domain);
}
