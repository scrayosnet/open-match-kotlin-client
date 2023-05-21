package net.justchunks.openmatch.client;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import net.justchunks.client.base.observer.StreamConsumer;
import net.justchunks.client.base.operation.CancellableOperation;
import net.justchunks.openmatch.client.wrapper.TicketTemplate;
import openmatch.Frontend.AcknowledgeBackfillResponse;
import openmatch.Frontend.WatchAssignmentsResponse;
import openmatch.Messages.Assignment;
import openmatch.Messages.Backfill;
import openmatch.Messages.SearchFields;
import openmatch.Messages.Ticket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@SuppressWarnings({"ResultOfMethodCallIgnored", "FieldMayBeFinal"})
class GrpcOpenMatchClientTest {

    private static final int GRPC_PORT = 50504;
    private static final int HTTP_PORT = 51504;
    private static final int REDIS_PORT = 6379;
    private static final int SHORT_WAIT_TIMEOUT_MILLIS = 5_000;
    private static final int WAIT_TIMEOUT_MILLIS = 10_000;
    private static final Network SHARED_NETWORK = Network.newNetwork();


    private static ScheduledExecutorService executorService;


    @Container
    private GenericContainer<?> redisContainer = new GenericContainer<>(
        DockerImageName.parse("docker.io/bitnami/redis:7.0.5")
    )
        .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
        .withEnv("REDIS_PORT", "6379")
        .withEnv("REDIS_REPLICATION_MODE", "master")
        .withEnv("REDIS_TLS_ENABLED", "no")
        .withNetworkAliases("redis")
        .withNetwork(SHARED_NETWORK);
    @Container
    private GenericContainer<?> frontendContainer = new GenericContainer<>(
        DockerImageName.parse("gcr.io/open-match-public-images/openmatch-frontend:1.5.0")
    )
        .withClasspathResourceMapping(
            "matchmaker_config_default.yaml",
            "/app/config/default/matchmaker_config_default.yaml",
            BindMode.READ_ONLY
        )
        .withClasspathResourceMapping(
            "matchmaker_config_override.yaml",
            "/app/config/override/matchmaker_config_override.yaml",
            BindMode.READ_ONLY
        )
        .withNetwork(SHARED_NETWORK)
        .withExposedPorts(GRPC_PORT, HTTP_PORT)
        .waitingFor(
            Wait
                .forHttp("/")
                .forPort(HTTP_PORT)
                .forStatusCode(404)
        );
    private GrpcOpenMatchClient client;


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
    }

    @AfterEach
    void afterEach() {
        client.close();
    }


    @Test
    @DisplayName("Should create ticket without data")
    void shouldCreateTicket() {
        // when
        Ticket ticket = client.createTicket(
            TicketTemplate.newBuilder()
                .build()
        ).join();

        // then
        Assertions.assertNotNull(ticket);
        Assertions.assertNotNull(ticket.getId());
        Assertions.assertNotNull(ticket.getCreateTime());
        Assertions.assertNotNull(ticket.getSearchFields());
        Assertions.assertNotNull(ticket.getExtensionsMap());
        Ticket returnedTicket = client.getTicket(ticket.getId()).join().orElse(null);
        Assertions.assertEquals(ticket, returnedTicket);
    }

    @Test
    @DisplayName("Should create ticket with data")
    void shouldCreateTicketWithData() throws InvalidProtocolBufferException {
        // when
        Ticket ticket = client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("test1", "test")
                .addDoubleArg("test2", 1.5D)
                .addTag("test3")
                .addExtension("test4", "test")
                .build()
        ).join();

        // then
        Assertions.assertNotNull(ticket);
        Assertions.assertNotNull(ticket.getId());
        Assertions.assertNotNull(ticket.getCreateTime());
        Assertions.assertNotNull(ticket.getSearchFields());
        Assertions.assertEquals("test", ticket.getSearchFields().getStringArgsMap().get("test1"));
        Assertions.assertEquals(1.5D, ticket.getSearchFields().getDoubleArgsMap().get("test2"));
        Assertions.assertTrue(ticket.getSearchFields().getTagsList().contains("test3"));
        Assertions.assertNotNull(ticket.getExtensionsMap());
        Assertions.assertEquals("test", ticket.getExtensionsMap().get("test4").unpack(StringValue.class).getValue());
        Ticket returnedTicket = client.getTicket(ticket.getId()).join().orElse(null);
        Assertions.assertEquals(ticket, returnedTicket);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null create ticket template")
    void shouldThrowOnNullCreateTicketTemplate() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.createTicket(null));
    }

    @Test
    @DisplayName("Should delete ticket")
    void shouldDeleteTicket() {
        // given
        Ticket ticket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        client.deleteTicket(ticket.getId()).join();

        // then
        Assertions.assertTrue(client.getTicket(ticket.getId()).join().isEmpty());
    }

    @Test
    @DisplayName("Should not throw when deleting non-existing ticket")
    void shouldNotThrowWhenDeletingNonExistingTicket() {
        // when, then
        Assertions.assertDoesNotThrow(() -> client.deleteTicket("test").join());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null delete ticket id")
    void shouldThrowOnNullDeleteTicketId() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.deleteTicket(null));
    }

    @Test
    @DisplayName("Should get existing ticket")
    void shouldGetTicket() {
        // given
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        Optional<Ticket> optTicket = client.getTicket(originalTicket.getId()).join();

        // then
        Assertions.assertTrue(optTicket.isPresent());
        Ticket ticket = optTicket.get();
        Assertions.assertEquals(originalTicket, ticket);
    }

    @Test
    @DisplayName("Should not get non-existing ticket")
    void shouldNotGetTicket() {
        // when
        Optional<Ticket> optTicket = client.getTicket("non-existing").join();

        // then
        Assertions.assertTrue(optTicket.isEmpty());
    }

    @Test
    @DisplayName("Should rethrow #getTicket(String) on closed client")
    void shouldRethrowGetTicketOnClosedClient() {
        // when
        client.close();
        CompletableFuture<Optional<Ticket>> response = client.getTicket("non-existing");

        // then
        Assertions.assertInstanceOf(
            StatusRuntimeException.class,
            Assertions.assertThrows(CompletionException.class, response::join).getCause()
        );
    }

    @Test
    @DisplayName("Should get equal tickets for the same ticket id")
    void shouldGetEqualTickets() {
        // given
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        Optional<Ticket> optTicket1 = client.getTicket(originalTicket.getId()).join();
        Optional<Ticket> optTicket2 = client.getTicket(originalTicket.getId()).join();

        // then
        Assertions.assertEquals(optTicket1.orElseThrow(), optTicket2.orElseThrow());
    }

    @Test
    @DisplayName("Should get equal tickets for the same ticket id")
    void shouldGetDifferentTickets() {
        // given
        Ticket originalTicket1 = client.createTicket(TicketTemplate.newBuilder().build()).join();
        Ticket originalTicket2 = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        Optional<Ticket> optTicket1 = client.getTicket(originalTicket1.getId()).join();
        Optional<Ticket> optTicket2 = client.getTicket(originalTicket2.getId()).join();

        // then
        Assertions.assertNotEquals(optTicket1.orElseThrow(), optTicket2.orElseThrow());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null get ticket id")
    void shouldThrowOnNullGetTicketId() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.getTicket(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    @Disabled("This is currently disabled, as we cannot trigger assignment updates with the frontend alone")
    @DisplayName("Should receive ticket assignments updates")
    void shouldReceiveUpdates() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);
        when(consumer.onNext(any())).thenReturn(true);
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        CancellableOperation operation = client.watchAssignments(originalTicket.getId(), consumer);
        // client.changeAssignment()

        // then
        verify(consumer, timeout(SHORT_WAIT_TIMEOUT_MILLIS).times(1)).onNext(any());
        verify(consumer, times(0)).onError(any());
        verify(consumer, times(0)).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should not receive ticket assignments updates for non-existent ticket")
    void shouldNotReceiveUpdatesForNonExistingId() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);
        when(consumer.onNext(any())).thenReturn(true);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);

        // when
        CancellableOperation operation = client.watchAssignments("non-existing", consumer);

        // then
        verify(consumer, timeout(SHORT_WAIT_TIMEOUT_MILLIS).times(1)).onError(errorCaptor.capture());
        verify(consumer, times(0)).onCompleted();
        Assertions.assertEquals(Code.NOT_FOUND, Status.fromThrowable(errorCaptor.getValue()).getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should cancel ticket assignment watch (through operation)")
    void shouldCancelWatchExternal() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);
        when(consumer.onNext(any())).thenReturn(true);
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        CancellableOperation operation = client.watchAssignments(originalTicket.getId(), consumer);
        operation.cancel();

        // then
        verify(consumer, Mockito.after(SHORT_WAIT_TIMEOUT_MILLIS).times(1)).onCompleted();
        verify(consumer, times(0)).onError(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @Disabled("This is currently disabled, as we cannot trigger assignment updates with the frontend alone")
    @DisplayName("Should cancel ticket assignment watch (through onNext())")
    void shouldCancelWatchInternal() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);
        when(consumer.onNext(any())).thenReturn(false);
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        CancellableOperation operation = client.watchAssignments(originalTicket.getId(), consumer);
        // client.changeAssignment()

        // then
        verify(consumer, Mockito.after(SHORT_WAIT_TIMEOUT_MILLIS).times(1)).onNext(any());
        verify(consumer, times(1)).onCompleted();
        verify(consumer, times(0)).onError(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should cancel ticket assignment watch (on deletion)")
    void shouldCancelWatchOnDeletion() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);
        when(consumer.onNext(any())).thenReturn(true);
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);

        // when
        CancellableOperation operation = client.watchAssignments(originalTicket.getId(), consumer);

        // when
        client.deleteTicket(originalTicket.getId());

        // then
        verify(consumer, timeout(SHORT_WAIT_TIMEOUT_MILLIS).times(1)).onError(errorCaptor.capture());
        verify(consumer, times(0)).onCompleted();
        Assertions.assertEquals(Code.NOT_FOUND, Status.fromThrowable(errorCaptor.getValue()).getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should cancel ticket assignment watch (on #close())")
    void shouldCancelWatchClose() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);
        when(consumer.onNext(any())).thenReturn(true);
        Ticket originalTicket = client.createTicket(TicketTemplate.newBuilder().build()).join();

        // when
        CancellableOperation operation = client.watchAssignments(originalTicket.getId(), consumer);

        // when
        client.close();

        // then
        verify(consumer, timeout(WAIT_TIMEOUT_MILLIS).times(1)).onError(any());
    }

    @Test
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @DisplayName("Should throw NPE on #watchAssignments(null, consumer)")
    void shouldThrowOnNullWatchTicketId() {
        // given
        StreamConsumer<WatchAssignmentsResponse> consumer =
            (StreamConsumer<WatchAssignmentsResponse>) mock(StreamConsumer.class);

        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.watchAssignments(null, consumer));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on #watchAssignments(\"\", null)")
    void shouldThrowOnNullWatchConsumer() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.watchAssignments("", null));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on #watchAssignments(null, null)")
    void shouldThrowOnNullWatch() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.watchAssignments(null, null));
    }

    @Test
    @DisplayName("Should create backfill without data")
    void shouldCreateBackfill() {
        // when
        Backfill backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        ).join();

        // then
        Assertions.assertNotNull(backfill);
        Assertions.assertNotNull(backfill.getId());
        Assertions.assertNotNull(backfill.getCreateTime());
        Assertions.assertNotNull(backfill.getSearchFields());
        Assertions.assertNotNull(backfill.getExtensionsMap());
        Backfill returnedBackfill = client.getBackfill(backfill.getId()).join().orElse(null);
        Assertions.assertEquals(backfill, returnedBackfill);
    }

    @Test
    @DisplayName("Should create backfill with data")
    void shouldCreateBackfillWithData() throws InvalidProtocolBufferException {
        // when
        Backfill backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .addStringArg("test1", "test")
                .addDoubleArg("test2", 1.5D)
                .addTag("test3")
                .addExtension("test4", "test")
                .build()
        ).join();

        // then
        Assertions.assertNotNull(backfill);
        Assertions.assertNotNull(backfill.getId());
        Assertions.assertNotNull(backfill.getCreateTime());
        Assertions.assertNotNull(backfill.getSearchFields());
        Assertions.assertEquals("test", backfill.getSearchFields().getStringArgsMap().get("test1"));
        Assertions.assertEquals(1.5D, backfill.getSearchFields().getDoubleArgsMap().get("test2"));
        Assertions.assertTrue(backfill.getSearchFields().getTagsList().contains("test3"));
        Assertions.assertNotNull(backfill.getExtensionsMap());
        Assertions.assertEquals("test", backfill.getExtensionsMap().get("test4").unpack(StringValue.class).getValue());
        Backfill returnedBackfill = client.getBackfill(backfill.getId()).join().orElse(null);
        Assertions.assertEquals(backfill, returnedBackfill);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null create backfill template")
    void shouldThrowOnNullCreateBackfillTemplate() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.createBackfill(null));
    }

    @Test
    @DisplayName("Should delete backfill")
    void shouldDeleteBackfill() {
        // given
        Backfill backfill = client.createBackfill(TicketTemplate.newBuilder().build()).join();

        // when
        client.deleteBackfill(backfill.getId()).join();

        // then
        Assertions.assertTrue(client.getBackfill(backfill.getId()).join().isEmpty());
    }

    @Test
    @DisplayName("Should not throw when deleting non-existing backfill")
    void shouldNotThrowWhenDeletingNonExistingBackfill() {
        // when, then
        Assertions.assertDoesNotThrow(() -> client.deleteBackfill("non-existing").join());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null delete backfill id")
    void shouldThrowOnNullDeleteBackfillId() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.deleteBackfill(null));
    }

    @Test
    @DisplayName("Should get existing backfill")
    void shouldGetBackfill() {
        // given
        Backfill originalBackfill = client.createBackfill(TicketTemplate.newBuilder().build()).join();

        // when
        Optional<Backfill> optBackfill = client.getBackfill(originalBackfill.getId()).join();

        // then
        Assertions.assertTrue(optBackfill.isPresent());
        Backfill backfill = optBackfill.get();
        Assertions.assertEquals(originalBackfill, backfill);
    }

    @Test
    @DisplayName("Should not get non-existing backfill")
    void shouldNotGetBackfill() {
        // when
        Optional<Backfill> optBackfill = client.getBackfill("non-existing").join();

        // then
        Assertions.assertTrue(optBackfill.isEmpty());
    }

    @Test
    @DisplayName("Should get equal backfills for the same backfill id")
    void shouldGetEqualBackfills() {
        // given
        Backfill originalBackfill = client.createBackfill(TicketTemplate.newBuilder().build()).join();

        // when
        Optional<Backfill> optBackfill1 = client.getBackfill(originalBackfill.getId()).join();
        Optional<Backfill> optBackfill2 = client.getBackfill(originalBackfill.getId()).join();

        // then
        Assertions.assertEquals(optBackfill1.orElseThrow(), optBackfill2.orElseThrow());
    }

    @Test
    @DisplayName("Should get equal backfills for the same backfill id")
    void shouldGetDifferentBackfills() {
        // given
        Backfill originalBackfill1 = client.createBackfill(TicketTemplate.newBuilder().build()).join();
        Backfill originalBackfill2 = client.createBackfill(TicketTemplate.newBuilder().build()).join();

        // when
        Optional<Backfill> optBackfill1 = client.getBackfill(originalBackfill1.getId()).join();
        Optional<Backfill> optBackfill2 = client.getBackfill(originalBackfill2.getId()).join();

        // then
        Assertions.assertNotEquals(optBackfill1.orElseThrow(), optBackfill2.orElseThrow());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null get backfill id")
    void shouldThrowOnNullGetBackfillId() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.getBackfill(null));
    }

    @Test
    @DisplayName("Should update backfill")
    void shouldUpdateBackfill() throws InvalidProtocolBufferException {
        // given
        Backfill originalBackfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .addStringArg("test1a", "test")
                .addStringArg("test1b", "test")
                .addDoubleArg("test2a", 1.5D)
                .addDoubleArg("test2b", 1.5D)
                .addTag("test3a")
                .addTag("test3b")
                .addExtension("test4a", "test")
                .addExtension("test4b", "test")
                .build()
        ).join();
        SearchFields originalFields = originalBackfill.getSearchFields();
        Map<String, Any> originalExtensions = originalBackfill.getExtensionsMap();
        Backfill newBackfill = originalBackfill.toBuilder()
            .setSearchFields(
                originalBackfill.getSearchFields().toBuilder()
                    .putStringArgs("test1a", "other")
                    .putStringArgs("other1", "other")
                    .putDoubleArgs("test2a", 3D)
                    .putDoubleArgs("other2", 3D)
                    .clearTags()
                    .addTags("test3b")
                    .addTags("other3")
                    .build()
            )
            .putExtensions("test4a", Any.pack(StringValue.of("other")))
            .putExtensions("other4", Any.pack(StringValue.of("other")))
            .build();

        // when
        Backfill updatedBackfill = client.updateBackfill(newBackfill).join();

        // then
        SearchFields updatedFields = updatedBackfill.getSearchFields();
        Map<String, Any> updatedExtensions = updatedBackfill.getExtensionsMap();
        Assertions.assertNotNull(originalBackfill);
        Assertions.assertNotNull(newBackfill);
        Assertions.assertNotNull(updatedBackfill);
        // should not be equal because of the generation
        Assertions.assertNotEquals(newBackfill, updatedBackfill);
        Assertions.assertEquals(0, originalBackfill.getGeneration());
        Assertions.assertEquals(0, newBackfill.getGeneration());
        Assertions.assertEquals(1, updatedBackfill.getGeneration());
        Assertions.assertEquals(originalBackfill.getId(), updatedBackfill.getId());
        Assertions.assertEquals(originalBackfill.getCreateTime(), updatedBackfill.getCreateTime());
        Assertions.assertNotNull(updatedFields);
        Assertions.assertEquals("test", updatedFields.getStringArgsMap().get("test1b"));
        Assertions.assertEquals("other", updatedFields.getStringArgsMap().get("test1a"));
        Assertions.assertEquals("other", updatedFields.getStringArgsMap().get("other1"));
        Assertions.assertEquals(1.5D, updatedFields.getDoubleArgsMap().get("test2b"));
        Assertions.assertEquals(3D, updatedFields.getDoubleArgsMap().get("test2a"));
        Assertions.assertEquals(3D, updatedFields.getDoubleArgsMap().get("other2"));
        Assertions.assertTrue(updatedFields.getTagsList().contains("test3b"));
        Assertions.assertFalse(updatedFields.getTagsList().contains("test3a"));
        Assertions.assertTrue(updatedFields.getTagsList().contains("other3"));
        Assertions.assertNotNull(updatedExtensions);
        Assertions.assertEquals("test", updatedExtensions.get("test4b").unpack(StringValue.class).getValue());
        Assertions.assertEquals("other", updatedExtensions.get("test4a").unpack(StringValue.class).getValue());
        Assertions.assertEquals("other", updatedExtensions.get("other4").unpack(StringValue.class).getValue());
        Backfill returnedBackfill = client.getBackfill(originalBackfill.getId()).join().orElse(null);
        Assertions.assertEquals(updatedBackfill, returnedBackfill);
    }

    @Test
    @DisplayName("Should throw NSEE update non-existing backfill")
    void shouldThrowOnUpdateNonExistingBackfill() {
        // when
        CompletableFuture<Backfill> result = client.updateBackfill(
            Backfill.newBuilder()
                .setId("non-existing")
                .build()
        );

        // then
        Assertions.assertInstanceOf(
            NoSuchElementException.class,
            Assertions.assertThrows(CompletionException.class, result::join).getCause()
        );
    }

    @Test
    @DisplayName("Should throw INVALID_ARGUMENT on update backfill without backfill id")
    void shouldThrowOnUpdateBackfillWithoutId() {
        // when
        CompletableFuture<Backfill> result = client.updateBackfill(
            TicketTemplate.newBuilder()
                .build()
                .createNewBackfill()
        );

        // then
        Assertions.assertEquals(
            Code.INVALID_ARGUMENT,
            Status.fromThrowable(Assertions.assertThrows(CompletionException.class, result::join).getCause()).getCode()
        );
    }

    @Test
    @DisplayName("Should not update backfill without backfill create time")
    void shouldNotUpdateBackfillWithoutCreateTime() {
        // when
        Backfill backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        ).join();
        CompletableFuture<Backfill> result = client.updateBackfill(
            backfill.toBuilder()
                .clearCreateTime()
                .build()
        );

        // then
        final Backfill updatedBackfill = Assertions.assertDoesNotThrow(result::join);
        Assertions.assertEquals(
            backfill.getCreateTime().getSeconds(),
            updatedBackfill.getCreateTime().getSeconds()
        );
    }

    @Test
    @DisplayName("Should not update backfill without backfill id")
    void shouldNotUpdateBackfillWithUpdatedCreateTime() {
        // when
        Backfill backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        ).join();
        CompletableFuture<Backfill> result = client.updateBackfill(
            backfill.toBuilder()
                .setCreateTime(
                    Timestamp.newBuilder()
                        .setSeconds(backfill.getCreateTime().getSeconds() + 1)
                        .build()
                )
                .build()
        );

        // then
        final Backfill updatedBackfill = Assertions.assertDoesNotThrow(result::join);
        Assertions.assertEquals(
            backfill.getCreateTime().getSeconds(),
            updatedBackfill.getCreateTime().getSeconds()
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null update backfill")
    void shouldThrowOnNullUpdateBackfill() {
        // when, then
        Assertions.assertThrows(NullPointerException.class, () -> client.getBackfill(null));
    }

    @Test
    @DisplayName("Should acknowledge backfill")
    void shouldAcknowledgeBackfill() {
        // given
        final Backfill backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        ).join();
        Assignment assignment = Assignment.newBuilder()
            .setConnection("test")
            .build();

        // when
        final AcknowledgeBackfillResponse response = client.acknowledgeBackfill(
            backfill.getId(),
            assignment
        ).join();

        // when, then
        Assertions.assertEquals(backfill, response.getBackfill());
        Assertions.assertEquals(0, response.getTicketsCount());
    }

    @Test
    @DisplayName("Should acknowledge backfill with tickets")
    void shouldAcknowledgeBackfillWithTickets() {
        // given
        client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("key", "value")
                .build()
        ).join();
        client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("key", "value")
                .build()
        ).join();
        client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("key", "wrong")
                .build()
        ).join();
        final Backfill backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .addStringArg("key", "value")
                .build()
        ).join();
        Assignment assignment = Assignment.newBuilder()
            .setConnection("test")
            .build();

        // when
        final AcknowledgeBackfillResponse response = client.acknowledgeBackfill(
            backfill.getId(),
            assignment
        ).join();

        // when, then
        Assertions.assertEquals(backfill, response.getBackfill());
        Assertions.assertEquals(0, response.getTicketsCount());
    }

    @Test
    @DisplayName("Should not acknowledge non-existing backfill")
    void shouldThrowOnAcknowledgeBackfillNonExisting() {
        // given
        Assignment assignment = Assignment.newBuilder()
            .setConnection("test")
            .build();

        // when
        CompletableFuture<AcknowledgeBackfillResponse> result = client.acknowledgeBackfill(
            "non-existing",
            assignment
        );

        // then
        Assertions.assertInstanceOf(
            NoSuchElementException.class,
            Assertions.assertThrows(CompletionException.class, result::join).getCause()
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null acknowledge backfill id")
    void shouldThrowOnNullAcknowledgeBackfillId() {
        // given
        Assignment assignment = Assignment.newBuilder()
            .setConnection("test")
            .build();

        // when, then
        Assertions.assertThrows(
            NullPointerException.class,
            () -> client.acknowledgeBackfill(null, assignment)
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null acknowledge backfill assignment")
    void shouldThrowOnNullAcknowledgeBackfillAssignment() {
        // when, then
        Assertions.assertThrows(
            NullPointerException.class,
            () -> client.acknowledgeBackfill("", null)
        );
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Should throw NPE on null acknowledge backfill")
    void shouldThrowOnNullAcknowledgeBackfill() {
        // when, then
        Assertions.assertThrows(
            NullPointerException.class,
            () -> client.acknowledgeBackfill(null, null)
        );
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
    @DisplayName("Should throw on any method if channel is closed")
    void shouldThrowIfChannelIsClosed() {
        // given
        client.close();

        // when
        CompletableFuture<Ticket> createFuture = client.createTicket(TicketTemplate.newBuilder().build());

        // then
        Assertions.assertInstanceOf(
            StatusRuntimeException.class,
            Assertions.assertThrows(
                CompletionException.class,
                createFuture::join
            ).getCause()
        );
    }

    @Test
    @DisplayName("Automatic host should fall back to default host")
    void automaticHostShouldFallBack() {
        // when
        final String defaultHost = GrpcOpenMatchClient.getAutomaticHost();

        // then
        Assertions.assertEquals("localhost", defaultHost);
    }

    @Test
    @DisplayName("Automatic port should fall back to default port")
    void automaticPortShouldFallBack() {
        // when
        final int defaultPort = GrpcOpenMatchClient.getAutomaticPort();

        // then
        Assertions.assertEquals(GRPC_PORT, defaultPort);
    }
}
