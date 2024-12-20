package com.n11.architecture.tools.microswitch.infrastructure.strategy;

import com.n11.architecture.tools.microswitch.application.config.EmbDeployer;
import com.n11.architecture.tools.microswitch.application.config.IEmbDeployerLoader;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CanaryTest {
    @Mock
    private EmbDeployer embDeployer;

    @Mock
    private Environment environment;

    @Mock
    private IEmbDeployerLoader embDeployerLoader;

    @InjectMocks
    private Canary canary;

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

        when(deployment.getStrategy()).thenReturn(StrategyType.CANARY.getValue());
        when(deployment.isEnabled()).thenReturn(true);

        System.setOut(new PrintStream(outContent));
        canary = new Canary(embDeployerLoader);

        Map<String, EmbDeployer.ServiceConfig> services = Map.of(SERVICE_KEY, serviceConfig);
        when(embDeployerLoader.getConfiguration(DOMAIN)).thenReturn(services);
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void execute_whenDeployScopeIsEnabled_shouldReturnFunc1ResultForSequenceAlgorithm() {
        when(func1.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.SEQUENCE.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("70/30");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        String result = canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        // Assertions
        assertEquals(EXPECTED_OUTPUT, result);
        verify(func1, times(1)).apply(INPUT);
        verify(func2, never()).apply(INPUT);
    }

    @Test
    void execute_whenCanaryAlgorithmIsNull_shouldRunAsSequence() {
        when(func1.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(null);
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("60/40");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        String result = null;
        // Execute method
        for (int i = 0; i < 2; i++) {
            result = canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);
        }

        // Assertions
        assertEquals(EXPECTED_OUTPUT, result);
        verify(func1, times(2)).apply(INPUT);
        verify(func2, never()).apply(INPUT);
    }

    @Test
    void execute_whenCanaryAlgorithmIsNull_shouldReturnFunc1Result() {
        when(func1.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(func2.apply(INPUT)).thenReturn("otherOutput");
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(null);
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("60/40");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        String result = null;
        // Execute method
        for (int i = 0; i < 10; i++) {
            result = canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);
        }

        // Assertions
        assertEquals(EXPECTED_OUTPUT, result);
        verify(func1, times(6)).apply(INPUT);
        verify(func2, times(4)).apply(INPUT);
    }

    @Test
    void execute_whenCanaryPercentageIsNull_shouldRunAs100PercentageFunc1() {
        when(func1.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.SEQUENCE.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn(null);
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        String result = canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        // Assertions
        assertEquals(EXPECTED_OUTPUT, result);
        verify(func1, times(1)).apply(INPUT);
        verify(func2, never()).apply(INPUT);
    }

    @Test
    void execute_whenCanaryPercentageInValidFormat_shouldReturnIllegalArgument() {
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("70-30");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Test that the exception is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);
        });

        assertEquals("Invalid Canary Percentage", exception.getMessage());
    }

    @Test
    void execute_whenDeployScopeIsEnabled_shouldReturnFunc2ResultForSequenceAlgorithm() {
        when(func2.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.SEQUENCE.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("0/100");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        String result = canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        // Assertions
        assertEquals(EXPECTED_OUTPUT, result);
        verify(func1, never()).apply(INPUT);
        verify(func2, times(1)).apply(INPUT);
    }

    @Test
    void execute_whenRandomAlgorithm_shouldReturnFunc1OrFunc2BasedOnRandom() {
        when(func1.apply(INPUT)).thenReturn("output");
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.RANDOM.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("60/40");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method with random output
        String result = canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        // Assertions
        verify(func1, atMostOnce()).apply(INPUT);
        verify(func2, atMostOnce()).apply(INPUT);

        var func1CalledOnce = mockingDetails(func1).getInvocations().size() == 1;
        var func2CalledOnce = mockingDetails(func2).getInvocations().size() == 1;

        assertTrue(func1CalledOnce ^ func2CalledOnce);
    }

    @Test
    void execute_whenInvalidCanaryPercentage_shouldThrowException() {
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("60/50");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Test that the exception is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            canary.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);
        });

        assertEquals("Percentages must sum to 100", exception.getMessage());
    }

    @Test
    void execute_whenDeployScopeIsDisabled_shouldReturnFunc1Result() {
        // Setup
        String input = "input";
        String expectedOutput = "output";
        String serviceKey = "testService";

        Function<String, String> func1 = mock(Function.class);
        Function<String, String> func2 = mock(Function.class);

        when(func1.apply(input)).thenReturn(expectedOutput);
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(false);

        // Execute method
        String result = canary.execute(func1, func2, DOMAIN, serviceKey, input);

        // Assertions
        assertEquals(expectedOutput, result);
        verify(func1, times(1)).apply(input);
        verify(func2, never()).apply(input);
    }

    @Test
    void execute_whenPercentage100ForFunc1_shouldReturnFunc1Result() {
        // Setup
        String input = "input";
        String expectedOutput = "output";
        String serviceKey = "testService";

        Function<String, String> func1 = mock(Function.class);
        Function<String, String> func2 = mock(Function.class);

        when(func1.apply(input)).thenReturn(expectedOutput);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.RANDOM.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("100/0");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        String result = canary.execute(func1, func2, DOMAIN, serviceKey, input);

        // Assertions
        assertEquals(expectedOutput, result);
        verify(func1, times(1)).apply(input);
        verify(func2, never()).apply(input);
    }

    @Test
    void execute_whenPercentage100ForFunc2_shouldReturnFunc2Result() {
        // Setup
        String input = "input";
        String expectedOutput = "output";
        String serviceKey = "testService";

        Function<String, String> func1 = mock(Function.class);
        Function<String, String> func2 = mock(Function.class);

        when(func2.apply(input)).thenReturn(expectedOutput);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.RANDOM.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("0/100");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        String result = canary.execute(func1, func2, DOMAIN, serviceKey, input);

        // Assertions
        assertEquals(expectedOutput, result);
        verify(func1, never()).apply(input);
        verify(func2, times(1)).apply(input);
    }

    @Test
    void execute_whenRandom100Percentage_shouldReturnAccordingToGivenPercentage() {
        // Setup
        String input = "input";
        String expectedOutput = "output";
        String serviceKey = "testService";

        Function<String, String> func1 = mock(Function.class);
        Function<String, String> func2 = mock(Function.class);

        when(func1.apply(input)).thenReturn(expectedOutput);
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.RANDOM.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("100/0");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        for (short i = 0; i < 100; i++)
            canary.execute(func1, func2, DOMAIN, serviceKey, input);

        // Assertions
        verify(func1, times(100)).apply(input);
        verify(func2, never()).apply(input);
    }

    @Test
    void execute_whenSequenceCertainPercentage_shouldReturnAccordingToGivenPercentage() {
        // Setup
        String input = "input";
        String expectedOutput = "output";
        String serviceKey = "testService";

        Function<String, String> func1 = mock(Function.class);
        Function<String, String> func2 = mock(Function.class);

        when(func1.apply(input)).thenReturn(expectedOutput);
        when(func2.apply(input)).thenReturn("otherOutput");
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.SEQUENCE.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("99/1");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        for (short i = 0; i < 100; i++)
            canary.execute(func1, func2, DOMAIN, serviceKey, input);

        // Assertions
        verify(func1, times(99)).apply(input);
        verify(func2, times(1)).apply(input);
    }

    @Test
    void execute_whenRandomCertainPercentage_shouldReturnAccordingToGivenPercentage() {
        // Setup
        String input = "input";
        String expectedOutput = "output";
        String serviceKey = "testService";

        Function<String, String> func1 = mock(Function.class);
        Function<String, String> func2 = mock(Function.class);

        when(func1.apply(input)).thenReturn(expectedOutput);
        when(func2.apply(input)).thenReturn("otherOutput");
        when(serviceConfig.getSpec().getDeployment().getAlgorithm()).thenReturn(AlgorithmType.RANDOM.getValue());
        when(serviceConfig.getSpec().getDeployment().getCanaryPercentage()).thenReturn("5/95");
        when(canary.deployScopeIsEnabled(serviceConfig)).thenReturn(true);

        // Execute method
        for (short i = 0; i < 100; i++)
            canary.execute(func1, func2, DOMAIN, serviceKey, input);

        // Assertions
        verify(func1, times(5)).apply(input);
        verify(func2, times(95)).apply(input);
    }
}
