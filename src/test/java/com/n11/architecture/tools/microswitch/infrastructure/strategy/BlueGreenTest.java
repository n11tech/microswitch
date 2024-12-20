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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlueGreenTest {
    @Mock
    private EmbDeployer embDeployer;

    @Mock
    private Environment environment;

    @Mock
    private IEmbDeployerLoader embDeployerLoader;

    @InjectMocks
    private BlueGreen blueGreen;

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

        when(deployment.getStrategy()).thenReturn(StrategyType.BLUE_GREEN.getValue());
        when(deployment.isEnabled()).thenReturn(true);

        System.setOut(new PrintStream(outContent));
        blueGreen = new BlueGreen(embDeployerLoader);

        Map<String, EmbDeployer.ServiceConfig> services = Map.of(SERVICE_KEY, serviceConfig);
        when(embDeployerLoader.getConfiguration(DOMAIN)).thenReturn(services);
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testExecute_func1AppliedWhenDeployScopeIsDisabled() {
        when(func1.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(blueGreen.deployScopeIsEnabled(serviceConfig)).thenReturn(false);

        var result = blueGreen.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        assertEquals(EXPECTED_OUTPUT, result);
    }

    @Test
    void testExecute_func1AppliedBasedOnWeight_whenDeployScopeIsEnabled() {
        when(func2.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(blueGreen.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(serviceConfig.getSpec().getDeployment().getWeight()).thenReturn("0/1");

        var result = blueGreen.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);

        assertEquals(EXPECTED_OUTPUT, result);
    }

    @Test
    void testExecute_func2AppliedWhenTtlExpires() {
        when(func1.apply(INPUT)).thenReturn("otherOutput");
        when(func2.apply(INPUT)).thenReturn(EXPECTED_OUTPUT);
        when(blueGreen.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(deployment.getWeight()).thenReturn("0/1");
        when(deployment.getTtl()).thenReturn((long) 15.0);

        String result;
        // Call execute
        Instant startTime = Instant.now();
        while (Duration.between(startTime, Instant.now()).getSeconds() < 19) {
            result = blueGreen.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT);
            if (Duration.between(startTime, Instant.now()).getSeconds() > 14) {
                assertEquals("otherOutput", result);
                break;
            }
            assertEquals(EXPECTED_OUTPUT, result);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Test
    void testExecute_throwsExceptionIfWeightsDontSumTo1() {
        when(blueGreen.deployScopeIsEnabled(serviceConfig)).thenReturn(true);
        when(deployment.getWeight()).thenReturn("0/0");

        // Call execute and expect an exception due to invalid weights
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                blueGreen.execute(func1, func2, DOMAIN, SERVICE_KEY, INPUT)
        );

        assertEquals("Weights must sum to 1", exception.getMessage());
    }
}
