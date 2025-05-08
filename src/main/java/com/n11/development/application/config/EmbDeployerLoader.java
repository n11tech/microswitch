package com.n11.development.application.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@RefreshScope
public class EmbDeployerLoader implements IEmbDeployerLoader {
    private List<EmbDeployer> embDeployers;
    private final Environment environment;

    @Value("classpath:emb-deployer/**/*.yml")
    private Resource[] configResources;

    @Value("${emb-deployer.refresh-version}")
    private String embDeployerVersion;

    private String embDeployerLastVersion;

    public EmbDeployerLoader(Environment environment) {
        this.environment = environment;
    }

    @Bean
    protected List<EmbDeployer> loadConfigurations() {
        var configurations = getEmbDeployers();
        embDeployers = configurations;
        return configurations;
    }

    private void reloadConfigurations() {
        embDeployers = getEmbDeployers();
    }

    List<EmbDeployer> getEmbDeployers() {
        List<EmbDeployer> configurations = new ArrayList<>();
        log.info("Loading emb-deployer configurations version: {}", embDeployerVersion);
        if (configResources == null) {
            log.error("Noting any emb-deployer configurations to load");
            return configurations;
        }
        for (Resource resource : configResources) {
            try {
                var properties = loadYamlAsProperties(resource);
                Map<String, Object> map = propertiesToMap(properties);
                var config = bindPropertiesToConfig(map);
                configurations.add(config);
            } catch (Exception e) {
                throw new RuntimeException("Error loading configuration from " + resource.getFilename(), e);
            }
        }
        embDeployerLastVersion = embDeployerVersion;
        return configurations;
    }

    @Override
    public Map<String, EmbDeployer.ServiceConfig> getConfiguration(String domain) {
        if (Objects.isNull(embDeployerLastVersion))
            this.reloadConfigurations();
        var activeProfile = Arrays.stream(environment.getActiveProfiles()).findFirst().orElse("default");
        var service = Objects.requireNonNull(embDeployers.stream().filter(x -> Objects.equals(x.getDomain(), domain) &&
                        Objects.equals(x.getPlatform(), activeProfile))
                .findFirst().orElse(new EmbDeployer()));
        return service.getEmbeddedServices();
    }

    private Properties loadYamlAsProperties(Resource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    private Map<String, Object> propertiesToMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        Map.Entry::getValue
                ));
    }

    private EmbDeployer bindPropertiesToConfig(Map<String, Object> map) {
        Binder binder = new Binder(new MapConfigurationPropertySource(map));
        return binder.bind("emb-deploy", Bindable.of(EmbDeployer.class)).orElseThrow(() ->
                new IllegalStateException("Failed to bind properties to EmbDeployer class"));
    }
}

