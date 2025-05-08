package com.n11.development.infrastructure.strategy;

import com.n11.development.application.config.EmbDeployer;
import com.n11.development.application.config.IEmbDeployerLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShadowTest {
    @Mock
    private EmbDeployer embDeployer;

    @Mock
    private Environment environment;

    @Mock
    private IEmbDeployerLoader embDeployerLoader;

    @InjectMocks
    private Shadow shadow;

    @Mock
    private EmbDeployer.ServiceConfig serviceConfig;

    @Mock
    private EmbDeployer.ServiceConfig.Metadata metadata;

    @Mock
    private EmbDeployer.ServiceConfig.Spec spec;

    @Mock
    private EmbDeployer.ServiceConfig.Spec.Deployment deployment;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    private static final String INPUT = "input";
    private static final String EXPECTED_OUTPUT = "output";
    private static final String DOMAIN = "basket";
    private static final String SERVICE_KEY = "testService";
    Function<String, String> func1;
    Function<String, String> func2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mocking functions
        func1 = mock(Function.class);
        func2 = mock(Function.class);

        when(serviceConfig.getMetadata()).thenReturn(metadata);
        when(serviceConfig.getSpec()).thenReturn(spec);
        when(spec.getDeployment()).thenReturn(deployment);

        when(deployment.getStrategy()).thenReturn(StrategyType.SHADOW.getValue());
        when(deployment.isEnabled()).thenReturn(true);

        System.setOut(new PrintStream(outContent));
        shadow = new Shadow(embDeployerLoader);

        Map<String, EmbDeployer.ServiceConfig> services = Map.of(SERVICE_KEY, serviceConfig);
        when(embDeployerLoader.getConfiguration(DOMAIN)).thenReturn(services);
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void execute_whenDeployScopeIsEnabled_shouldReturnFunc1ResultAndFunc2Null() {
        when(func1.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(func2.apply(INPUT)).thenReturn(null);
        when(shadow.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(serviceConfig.getSpec().getDeployment().getShadowWeight()).thenReturn((short)1);

        String result = shadow.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        assertEquals(EXPECTED_OUTPUT, result);
        verify(func1, times(1)).apply(INPUT);
        verify(func2, times(1)).apply(INPUT);
    }

    @Test
    void execute_whenDeployScopeIsDisabled_shouldExecuteFunction1() {
        String func1Result = "func1Result";

        when(func1.apply(INPUT)).thenReturn(func1Result);
        when(shadow.deployScopeIsEnabled(serviceConfig)).thenReturn(false);

        String result = shadow.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        assertEquals(func1Result, result);
        verify(func1, times(1)).apply(INPUT);
    }

    @Test
    void execute_whenDeployScopeShadowWeightExist_shouldExecuteBothFunctionsAccordingToGivenWeight() {
        String func1Result = "func1Result";
        String func2Result = "func2Result";

        when(func1.apply(INPUT)).thenReturn(func1Result);
        when(func2.apply(INPUT)).thenReturn(func2Result);
        when(shadow.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(deployment.getShadowWeight()).thenReturn((short) 3);

        String result = "";
        for (int i = 0; i < 12; i++)
            result = shadow.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        assertEquals(func1Result, result);
        verify(func1, times(12)).apply(INPUT);
        verify(func2, times(4)).apply(INPUT);
    }

    @Test
    void execute_whenWeightIs1Func2ResultIsNull_shouldLogNullMessage() {
        String func1Result = "func1Result";

        when(func1.apply(INPUT)).thenReturn(func1Result);
        when(func2.apply(INPUT)).thenReturn(null);
        when(shadow.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(deployment.getShadowWeight()).thenReturn((short) 1);

        shadow.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        assertTrue(outContent.toString().contains("shadow result is null"));
    }

    @Test
    void execute_whenFunc1AndFunc2ReturnDifferentResults_shouldLogNotEqualsMessage() {
        String func1Result = "func1Result";
        String func2Result = "differentResult";

        when(func1.apply(INPUT)).thenReturn(func1Result);
        when(func2.apply(INPUT)).thenReturn(func2Result);
        when(shadow.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(deployment.getShadowWeight()).thenReturn((short) 1);

        shadow.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        // Assert
        assertTrue(outContent.toString().contains("shadow result not equals origin"));
    }
}
