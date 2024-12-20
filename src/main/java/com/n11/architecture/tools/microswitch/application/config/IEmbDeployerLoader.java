package com.n11.architecture.tools.microswitch.application.config;

import java.util.Map;

public interface IEmbDeployerLoader {
    Map<String, EmbDeployer.ServiceConfig> getConfiguration(String domain);
}
