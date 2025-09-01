package com.n11.development.core.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class DeploymentManager {
    
    private final DeploymentStrategyFactory strategyFactory;
    
    @Autowired
    public DeploymentManager(DeploymentStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }
    
    public <R> R canary(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return strategyFactory.executeCanary(stable, experimental, serviceKey);
    }
    
    public <R> R shadow(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return strategyFactory.executeShadow(stable, experimental, serviceKey);
    }
    
    public <R> R blueGreen(Supplier<R> stable, Supplier<R> experimental, String serviceKey) {
        return strategyFactory.executeBlueGreen(stable, experimental, serviceKey);
    }
    

}

