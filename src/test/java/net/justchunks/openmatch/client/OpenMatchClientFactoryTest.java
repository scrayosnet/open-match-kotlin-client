package net.justchunks.openmatch.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class OpenMatchClientFactoryTest {

    private static ScheduledExecutorService executorService;


    @BeforeAll
    static void before() {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterAll
    static void after() {
        executorService.shutdown();
    }


    @Test
    @DisplayName("Should get an instance")
    void shouldGetInstance() {
        // given
        OpenMatchClient client = OpenMatchClientFactory.createNewClient(executorService);

        // then
        Assertions.assertNotNull(client);

        // cleanup
        client.close();
    }

    @Test
    @DisplayName("Should get a new instance")
    void shouldGetNewInstance() {
        // given
        OpenMatchClient client = OpenMatchClientFactory.createNewClient(executorService);
        OpenMatchClient sdk2 = OpenMatchClientFactory.createNewClient(executorService);

        // then
        Assertions.assertNotEquals(client, sdk2);

        // cleanup
        client.close();
        sdk2.close();
    }
}
