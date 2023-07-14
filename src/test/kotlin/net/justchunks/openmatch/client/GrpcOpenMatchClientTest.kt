package net.justchunks.openmatch.client

import com.google.protobuf.Any
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.StringValue
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.StatusException
import kotlin.NoSuchElementException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import net.justchunks.openmatch.client.wrapper.TicketTemplate
import openmatch.Frontend.WatchAssignmentsResponse
import openmatch.Messages
import openmatch.Messages.Backfill
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Disabled("Integration tests are currently not possible")
@Testcontainers
internal class GrpcOpenMatchClientTest {

    @Container
    private val redisContainer: GenericContainer<*> = GenericContainer(
        DockerImageName.parse("docker.io/bitnami/redis:7.0.11")
    )
        .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
        .withEnv("REDIS_PORT", "6379")
        .withEnv("REDIS_REPLICATION_MODE", "master")
        .withEnv("REDIS_TLS_ENABLED", "no")
        .withNetworkAliases("redis")
        .withNetwork(SHARED_NETWORK)

    @Container
    private val frontendContainer: GenericContainer<*> = GenericContainer(
        DockerImageName.parse("gcr.io/open-match-public-images/openmatch-frontend:1.7.0")
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
        )
    private lateinit var client: GrpcOpenMatchClient

    @BeforeEach
    fun beforeEach() {
        client = GrpcOpenMatchClient(
            frontendContainer.host,
            frontendContainer.getMappedPort(GRPC_PORT)
        )
    }

    @AfterEach
    fun afterEach() {
        client.close()
    }

    @Test
    @DisplayName("Should create ticket without data")
    fun shouldCreateTicket() = runTest {
        // when
        val ticket = client.createTicket(
            TicketTemplate.newBuilder()
                .build()
        )

        // then
        assertNotNull(ticket)
        assertNotNull(ticket.id)
        assertNotNull(ticket.createTime)
        assertNotNull(ticket.searchFields)
        assertNotNull(ticket.extensionsMap)
        assertEquals(ticket, client.getTicket(ticket.id))
    }

    @Test
    @DisplayName("Should create ticket with data")
    @Throws(InvalidProtocolBufferException::class)
    fun shouldCreateTicketWithData() = runTest {
        // when
        val ticket = client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("test1", "test")
                .addDoubleArg("test2", 1.5)
                .addTag("test3")
                .addExtension("test4", "test")
                .build()
        )

        // then
        assertNotNull(ticket)
        assertNotNull(ticket.id)
        assertNotNull(ticket.createTime)
        assertNotNull(ticket.searchFields)
        assertEquals("test", ticket.searchFields.stringArgsMap["test1"])
        assertEquals(1.5, ticket.searchFields.doubleArgsMap["test2"])
        assertTrue(ticket.searchFields.tagsList.contains("test3"))
        assertNotNull(ticket.extensionsMap)
        assertEquals(
            "test",
            ticket.extensionsMap["test4"]!!.unpack(
                StringValue::class.java
            ).value
        )
        assertEquals(ticket, client.getTicket(ticket.id))
    }

    @Test
    @DisplayName("Should delete ticket")
    fun shouldDeleteTicket() = runTest {
        // given
        val ticket = client.createTicket(TicketTemplate.newBuilder().build())

        // when
        client.deleteTicket(ticket.id)

        // then
        assertNull(client.getTicket(ticket.id))
    }

    @Test
    @DisplayName("Should not throw when deleting non-existing ticket")
    fun shouldNotThrowWhenDeletingNonExistingTicket() = runTest {
        // when, then
        client.deleteTicket("test")
    }

    @Test
    @DisplayName("Should get existing ticket")
    fun shouldGetTicket() = runTest {
        // given
        val originalTicket = client.createTicket(TicketTemplate.newBuilder().build())

        // when
        val retrievedTicket = client.getTicket(originalTicket.id)

        // then
        assertNotNull(retrievedTicket)
        assertEquals(originalTicket, retrievedTicket)
    }

    @Test
    @DisplayName("Should not get non-existing ticket")
    fun shouldNotGetTicket() = runTest {
        // when
        val retrievedTicket = client.getTicket("non-existing")

        // then
        assertNull(retrievedTicket)
    }

    @Test
    @DisplayName("Should rethrow #getTicket(String) on closed client")
    fun shouldRethrowGetTicketOnClosedClient() = runTest {
        // given
        client.close()

        // when, then
        val exception = assertFailsWith<StatusException> {
            client.getTicket("non-existing")
        }
        assertEquals(Status.UNAVAILABLE.code, exception.status.code)
        assertEquals("Channel shutdown invoked", exception.status.description)
    }

    @Test
    @DisplayName("Should get equal tickets for the same ticket id")
    fun shouldGetEqualTickets() = runTest {
        // given
        val originalTicket = client.createTicket(TicketTemplate.newBuilder().build())

        // when
        val ticket1 = client.getTicket(originalTicket.id)
        val ticket2 = client.getTicket(originalTicket.id)

        // then
        assertEquals(ticket1, ticket2)
    }

    @Test
    @DisplayName("Should get equal tickets for the same ticket id")
    fun shouldGetDifferentTickets() = runTest {
        // given
        val originalTicket1 = client.createTicket(TicketTemplate.newBuilder().build())
        val originalTicket2 = client.createTicket(TicketTemplate.newBuilder().build())

        // when
        val ticket1 = client.getTicket(originalTicket1.id)
        val ticket2 = client.getTicket(originalTicket2.id)

        // then
        assertNotEquals(ticket1, ticket2)
    }

    @Test
    @Disabled("This is currently disabled, as we cannot trigger assignment updates with the frontend alone")
    @DisplayName("Should receive ticket assignments updates")
    fun shouldReceiveUpdates() = runTest {
        // given
        val originalTicket = client.createTicket(TicketTemplate.newBuilder().build())

        // when
        val updates: Flow<WatchAssignmentsResponse> = client.watchAssignments(originalTicket.id)
        // client.changeAssignment()

        // then
        updates.take(1).collect {
            assertTrue(it.hasAssignment())
        }
    }

    @Test
    @DisplayName("Should not receive ticket assignments updates for non-existent ticket")
    fun shouldNotReceiveUpdatesForNonExistingId() = runTest {
        // given
        val updates = client.watchAssignments("non-existing")

        // when, then
        val exception = assertFailsWith<StatusException> {
            updates.count()
        }
        assertEquals(Status.NOT_FOUND.code, exception.status.code)
        assertEquals("Ticket id: non-existing not found", exception.status.description)
    }

    @Test
    @DisplayName("Should cancel ticket assignment watch (on deletion)")
    fun shouldCancelWatchOnDeletion() = runTest {
        // given
        val originalTicket = client.createTicket(TicketTemplate.newBuilder().build())
        val operation = client.watchAssignments("non-existing")

        // when
        client.deleteTicket(originalTicket.id)

        // then
        val exception = assertFailsWith<StatusException> {
            operation.count()
        }
        assertEquals(Status.NOT_FOUND.code, exception.status.code)
        assertEquals("Ticket id: non-existing not found", exception.status.description)
    }

    @Test
    @DisplayName("Should cancel ticket assignment watch (on #close())")
    fun shouldCancelWatchClose() = runTest {
        // given
        client.createTicket(TicketTemplate.newBuilder().build())
        val operation = client.watchAssignments("non-existing")

        // when
        client.close()

        // then
        val exception = assertFailsWith<StatusException> {
            operation.count()
        }
        assertEquals(Status.UNAVAILABLE.code, exception.status.code)
        assertEquals("Channel shutdown invoked", exception.status.description)
    }

    @Test
    @DisplayName("Should create backfill without data")
    fun shouldCreateBackfill() = runTest {
        // when
        val backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        )

        // then
        assertNotNull(backfill)
        assertNotNull(backfill.id)
        assertNotNull(backfill.createTime)
        assertNotNull(backfill.searchFields)
        assertNotNull(backfill.extensionsMap)
        assertEquals(backfill, client.getBackfill(backfill.id))
    }

    @Test
    @DisplayName("Should create backfill with data")
    fun shouldCreateBackfillWithData() = runTest {
        // when
        val backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .addStringArg("test1", "test")
                .addDoubleArg("test2", 1.5)
                .addTag("test3")
                .addExtension("test4", "test")
                .build()
        )

        // then
        assertNotNull(backfill)
        assertNotNull(backfill.id)
        assertNotNull(backfill.createTime)
        assertNotNull(backfill.searchFields)
        assertEquals("test", backfill.searchFields.stringArgsMap["test1"])
        assertEquals(1.5, backfill.searchFields.doubleArgsMap["test2"])
        assertTrue(backfill.searchFields.tagsList.contains("test3"))
        assertNotNull(backfill.extensionsMap)
        assertEquals(
            "test",
            backfill.extensionsMap["test4"]!!.unpack(
                StringValue::class.java
            ).value
        )
        assertEquals(backfill, client.getBackfill(backfill.id))
    }

    @Test
    @DisplayName("Should delete backfill")
    fun shouldDeleteBackfill() = runTest {
        // given
        val backfill = client.createBackfill(TicketTemplate.newBuilder().build())

        // when
        client.deleteBackfill(backfill.id)

        // then
        assertNull(client.getBackfill(backfill.id))
    }

    @Test
    @DisplayName("Should not throw when deleting non-existing backfill")
    fun shouldNotThrowWhenDeletingNonExistingBackfill() = runTest {
        // when, then
        client.deleteBackfill("non-existing")
    }

    @Test
    @DisplayName("Should get existing backfill")
    fun shouldGetBackfill() = runTest {
        // given
        val originalBackfill = client.createBackfill(TicketTemplate.newBuilder().build())

        // when
        val retrievedBackfill = client.getBackfill(originalBackfill.id)

        // then
        assertNotNull(retrievedBackfill)
        assertEquals(originalBackfill, retrievedBackfill)
    }

    @Test
    @DisplayName("Should not get non-existing backfill")
    fun shouldNotGetBackfill() = runTest {
        // when
        val backfill = client.getBackfill("non-existing")

        // then
        assertNull(backfill)
    }

    @Test
    @DisplayName("Should rethrow #getBackfill(String) on closed client")
    fun shouldRethrowGetBackfillOnClosedClient() = runTest {
        // given
        client.close()

        // when, then
        val exception = assertFailsWith<StatusException> {
            client.getBackfill("non-existing")
        }
        assertEquals(Status.UNAVAILABLE.code, exception.status.code)
        assertEquals("Channel shutdown invoked", exception.status.description)
    }

    @Test
    @DisplayName("Should get equal backfills for the same backfill id")
    fun shouldGetEqualBackfills() = runTest {
        // given
        val originalBackfill = client.createBackfill(TicketTemplate.newBuilder().build())

        // when
        val backfill1 = client.getBackfill(originalBackfill.id)
        val backfill2 = client.getBackfill(originalBackfill.id)

        // then
        assertEquals(backfill1, backfill2)
    }

    @Test
    @DisplayName("Should get equal backfills for the same backfill id")
    fun shouldGetDifferentBackfills() = runTest {
        // given
        val originalBackfill1 = client.createBackfill(TicketTemplate.newBuilder().build())
        val originalBackfill2 = client.createBackfill(TicketTemplate.newBuilder().build())

        // when
        val backfill1 = client.getBackfill(originalBackfill1.id)
        val backfill2 = client.getBackfill(originalBackfill2.id)

        // then
        assertNotEquals(backfill1, backfill2)
    }

    @Test
    @DisplayName("Should update backfill")
    fun shouldUpdateBackfill() = runTest {
        // given
        val originalBackfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .addStringArg("test1a", "test")
                .addStringArg("test1b", "test")
                .addDoubleArg("test2a", 1.5)
                .addDoubleArg("test2b", 1.5)
                .addTag("test3a")
                .addTag("test3b")
                .addExtension("test4a", "test")
                .addExtension("test4b", "test")
                .build()
        )
        val newBackfill = originalBackfill.toBuilder()
            .setSearchFields(
                originalBackfill.searchFields.toBuilder()
                    .putStringArgs("test1a", "other")
                    .putStringArgs("other1", "other")
                    .putDoubleArgs("test2a", 3.0)
                    .putDoubleArgs("other2", 3.0)
                    .clearTags()
                    .addTags("test3b")
                    .addTags("other3")
                    .build()
            )
            .putExtensions("test4a", Any.pack(StringValue.of("other")))
            .putExtensions("other4", Any.pack(StringValue.of("other")))
            .build()

        // when
        val updatedBackfill = client.updateBackfill(newBackfill)

        // then
        val updatedFields = updatedBackfill.searchFields
        val updatedExtensions = updatedBackfill.extensionsMap
        assertNotNull(originalBackfill)
        assertNotNull(newBackfill)
        assertNotNull(updatedBackfill)
        // should not be equal because of the generation
        assertNotEquals(newBackfill, updatedBackfill)
        assertEquals(1, originalBackfill.generation)
        assertEquals(1, newBackfill.generation)
        assertEquals(2, updatedBackfill.generation)
        assertEquals(originalBackfill.id, updatedBackfill.id)
        assertEquals(originalBackfill.createTime, updatedBackfill.createTime)
        assertNotNull(updatedFields)
        assertEquals("test", updatedFields.stringArgsMap["test1b"])
        assertEquals("other", updatedFields.stringArgsMap["test1a"])
        assertEquals("other", updatedFields.stringArgsMap["other1"])
        assertEquals(1.5, updatedFields.doubleArgsMap["test2b"])
        assertEquals(3.0, updatedFields.doubleArgsMap["test2a"])
        assertEquals(3.0, updatedFields.doubleArgsMap["other2"])
        assertTrue(updatedFields.tagsList.contains("test3b"))
        assertFalse(updatedFields.tagsList.contains("test3a"))
        assertTrue(updatedFields.tagsList.contains("other3"))
        assertNotNull(updatedExtensions)
        assertEquals(
            "test",
            updatedExtensions["test4b"]!!.unpack(
                StringValue::class.java
            ).value
        )
        assertEquals(
            "other",
            updatedExtensions["test4a"]!!.unpack(
                StringValue::class.java
            ).value
        )
        assertEquals(
            "other",
            updatedExtensions["other4"]!!.unpack(
                StringValue::class.java
            ).value
        )
        val returnedBackfill = client.getBackfill(originalBackfill.id)
        assertEquals(updatedBackfill, returnedBackfill)
    }

    @Test
    @DisplayName("Should throw NSEE update non-existing backfill")
    fun shouldThrowOnUpdateNonExistingBackfill() = runTest {
        // when, then
        assertFailsWith<NoSuchElementException> {
            client.updateBackfill(
                Backfill.newBuilder()
                    .setId("non-existing")
                    .build()
            )
        }
    }

    @Test
    @DisplayName("Should throw INVALID_ARGUMENT on update backfill without backfill id")
    fun shouldThrowOnUpdateBackfillWithoutId() = runTest {
        // when, then
        val exception = assertFailsWith<StatusException> {
            client.updateBackfill(
                TicketTemplate.newBuilder()
                    .build()
                    .createNewBackfill()
            )
        }
        assertEquals(Status.INVALID_ARGUMENT.code, exception.status.code)
        assertEquals("backfill ID should exist", exception.status.description)
    }

    @Test
    @DisplayName("Should not update backfill creation time (deleted)")
    fun shouldNotUpdateBackfillWithoutCreateTime() = runTest {
        // when
        val backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        )
        val updatedBackfill = client.updateBackfill(
            backfill.toBuilder()
                .clearCreateTime()
                .build()
        )

        // then
        assertEquals(
            backfill.createTime.seconds,
            updatedBackfill.createTime.seconds
        )
    }

    @Test
    @DisplayName("Should not update backfill creation time (overwritten)")
    fun shouldNotUpdateBackfillWithUpdatedCreateTime() = runTest {
        // when
        val backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        )
        val updatedBackfill = client.updateBackfill(
            backfill.toBuilder()
                .setCreateTime(
                    Timestamp.newBuilder()
                        .setSeconds(backfill.createTime.seconds + 1)
                        .build()
                )
                .build()
        )

        // then
        assertEquals(
            backfill.createTime.seconds,
            updatedBackfill.createTime.seconds
        )
    }

    @Test
    @DisplayName("Should acknowledge backfill")
    fun shouldAcknowledgeBackfill() = runTest {
        // given
        val backfill: Backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .build()
        )
        val assignment = Messages.Assignment.newBuilder()
            .setConnection("test")
            .build()

        // when
        val response = client.acknowledgeBackfill(
            backfill.id,
            assignment
        )

        // when, then
        assertEquals(backfill, response.backfill)
        assertEquals(0, response.ticketsCount)
    }

    @Test
    @DisplayName("Should acknowledge backfill with tickets")
    fun shouldAcknowledgeBackfillWithTickets() = runTest {
        // given
        client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("key", "value")
                .build()
        )
        client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("key", "value")
                .build()
        )
        client.createTicket(
            TicketTemplate.newBuilder()
                .addStringArg("key", "wrong")
                .build()
        )
        val backfill: Backfill = client.createBackfill(
            TicketTemplate.newBuilder()
                .addStringArg("key", "value")
                .build()
        )
        val assignment = Messages.Assignment.newBuilder()
            .setConnection("test")
            .build()

        // when
        val response = client.acknowledgeBackfill(
            backfill.id,
            assignment
        )

        // when, then
        assertEquals(backfill, response.backfill)
        assertEquals(0, response.ticketsCount)
    }

    @Test
    @DisplayName("Should not acknowledge non-existing backfill")
    fun shouldThrowOnAcknowledgeBackfillNonExisting() = runTest {
        // given
        val assignment = Messages.Assignment.newBuilder()
            .setConnection("test")
            .build()

        // when, then
        assertFailsWith<NoSuchElementException> {
            client.acknowledgeBackfill(
                "non-existing",
                assignment
            )
        }
    }

    @Test
    @DisplayName("Close should be idempotent")
    fun closeShouldBeIdempotent() {
        // when
        client.close()

        // then
        client.close()
    }

    @Test
    @DisplayName("Should throw on any method if channel is closed")
    fun shouldThrowIfChannelIsClosed() = runTest {
        // given
        client.close()

        // when, then
        val exception = assertFailsWith<StatusException> {
            client.createTicket(TicketTemplate.newBuilder().build())
        }
        assertEquals(Status.UNAVAILABLE.code, exception.status.code)
        assertEquals("Channel shutdown invoked", exception.status.description)
    }

    @Test
    @DisplayName("Close should refresh interrupted flag")
    fun closeShouldBeInterruptable() {
        // given
        Thread.currentThread().interrupt()

        // when
        client.close()

        // then
        assertTrue(Thread.interrupted())
    }

    @Test
    @DisplayName("Automatic host should fall back to default host")
    fun automaticHostShouldFallBack() {
        // when
        val defaultHost: String = GrpcOpenMatchClient.FRONTEND_HOST

        // then
        assertEquals("localhost", defaultHost)
    }

    @Test
    @DisplayName("Automatic port should fall back to default port")
    fun automaticPortShouldFallBack() {
        // when
        val defaultPort: Int = GrpcOpenMatchClient.FRONTEND_PORT

        // then
        assertEquals(GRPC_PORT, defaultPort)
    }

    companion object {
        private const val GRPC_PORT = 50504
        private const val HTTP_PORT = 51504

        private val SHARED_NETWORK = Network.newNetwork()
    }
}
