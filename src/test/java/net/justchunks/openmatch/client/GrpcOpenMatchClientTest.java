package net.justchunks.openmatch.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Testcontainers
@SuppressWarnings("ResultOfMethodCallIgnored")
class GrpcOpenMatchClientTest {

    private static final int GRPC_PORT = 50504;
    private static final int HTTP_PORT = 51504;
    private static final int WAIT_TIMEOUT_MILLIS = 10_000;


    private static ScheduledExecutorService executorService;


    @Container
    private GenericContainer<?> frontendContainer = new GenericContainer<>(
        DockerImageName.parse("gcr.io/open-match-public-images/openmatch-frontend:1.3.0")
    )
        .withCommand(
            "--help"
        )
        .withExposedPorts(GRPC_PORT, HTTP_PORT)
        .waitingFor(
            Wait
                .forHttp("/")
                .forPort(HTTP_PORT)
                .forStatusCode(404)
        );
    private GrpcOpenMatchClient client;
    private WaitingConsumer logConsumer;


    @BeforeAll
    static void before() {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterAll
    static void after() {
        executorService.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        client = new GrpcOpenMatchClient(
            executorService,
            frontendContainer.getHost(),
            frontendContainer.getMappedPort(GRPC_PORT)
        );
        logConsumer = new WaitingConsumer();
        frontendContainer.followOutput(logConsumer);
    }

    @AfterEach
    void afterEach() {
        client.close();
    }


    @Test
    @DisplayName("Close should be idempotent")
    void closeShouldBeIdempotent() {
        // when
        client.close();

        // then
        Assertions.assertDoesNotThrow(() -> client.close());
    }

    @Test
    @DisplayName("Close should refresh interrupted flag")
    void closeShouldBeInterruptable() {
        // given
        Thread.currentThread().interrupt();

        // when
        client.close();

        // then
        Assertions.assertTrue(Thread.interrupted());
    }

    @Test
    @DisplayName("Automatic host should throw ISE with missing environment")
    void automaticHostShouldThrowISE() {
        // when, then
        Assertions.assertThrows(IllegalStateException.class, GrpcOpenMatchClient::getAutomaticHost);
    }

    @Test
    @DisplayName("Automatic port should fall back to default port")
    void automaticPortShouldFallBack() {
        // when
        final int defaultPort = GrpcOpenMatchClient.getAutomaticPort();

        // then
        Assertions.assertEquals(GRPC_PORT, defaultPort);
    }


    private boolean containsLogLine(String logMessagePart) {
        return getLogLine(logMessagePart).isPresent();
    }

    private Optional<String> getLogLine(String logMessagePart) {
        for (final String line : frontendContainer.getLogs().split("\\n")) {
            if (line.contains(logMessagePart)) {
                return Optional.of(line);
            }
        }

        return Optional.empty();
    }
}
