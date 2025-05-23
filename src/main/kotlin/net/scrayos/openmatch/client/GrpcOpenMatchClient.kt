package net.scrayos.openmatch.client

import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import net.scrayos.openmatch.client.GrpcOpenMatchClient.Companion.FRONTEND_HOST_ENV_KEY
import net.scrayos.openmatch.client.GrpcOpenMatchClient.Companion.FRONTEND_PORT_ENV_KEY
import net.scrayos.openmatch.client.wrapper.TicketTemplate
import openmatch.Frontend
import openmatch.Frontend.AcknowledgeBackfillRequest
import openmatch.Frontend.AcknowledgeBackfillResponse
import openmatch.Frontend.CreateBackfillRequest
import openmatch.Frontend.CreateTicketRequest
import openmatch.Frontend.DeleteBackfillRequest
import openmatch.Frontend.DeleteTicketRequest
import openmatch.Frontend.GetBackfillRequest
import openmatch.Frontend.UpdateBackfillRequest
import openmatch.Frontend.WatchAssignmentsRequest
import openmatch.Frontend.WatchAssignmentsResponse
import openmatch.Messages.Assignment
import openmatch.Messages.Backfill
import openmatch.Messages.Ticket
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.NoSuchElementException
import java.util.concurrent.TimeUnit
import openmatch.FrontendServiceGrpcKt.FrontendServiceCoroutineStub as FrontendStub

/**
 * A [GrpcOpenMatchClient] represents the gRPC implementation of the [Open Match frontend][OpenMatchClient]. The
 * implementation is based on the official Protobufs released with Open Match. Each platform only needs one
 * implementation of the Open Match Client, but does not have to worry about selecting the best implementation. Instead,
 * the factory method specifies the implementation. All implementations fully comply with the Open Match specification.
 *
 * When creating an instance of this implementation, the corresponding [network channel][Channel] is dynamically
 * assembled for this purpose. The host is obtained through the environment variable [FRONTEND_HOST_ENV_KEY], and the
 * port is obtained through the environment variable [FRONTEND_PORT_ENV_KEY], if any value has been set for these keys.
 * The corresponding stubs for coroutine-based communication with the interface are instantiated for the
 * [channel][Channel] to the supplied frontend. No action is taken by creating this instance, and communication with
 * the external interface is not initiated.
 *
 * @param host The host, under which the gRPC server of the Open Match frontend can be reached and that will therefore
 * be used to establish the connection.
 * @param port The port, under which the gRPC server of the Open Match frontend can be reached and that will therefore
 * be used to establish the connection.
 */
class GrpcOpenMatchClient internal constructor(
    /** The host of the external interface of the Open Match frontend, that will be used to establish the connection. */
    val host: String = FRONTEND_HOST,
    /** The port of the external interface of the Open Match frontend, that will be used to establish the connection. */
    val port: Int = FRONTEND_PORT,
) : OpenMatchClient {

    /** The [channel][ManagedChannel], that will be used for the network communication with the external interface. */
    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .build()

    /** The coroutine-based stub for the communication the external interface of the Open Match frontend. */
    private val stub: FrontendStub = FrontendStub(channel)

    override suspend fun createTicket(template: TicketTemplate): Ticket {
        // call the endpoint with a template for the ticket and return the new ticket
        return stub.createTicket(
            CreateTicketRequest.newBuilder()
                .setTicket(template.createNewTicket())
                .build(),
        )
    }

    override suspend fun deleteTicket(ticketId: String) {
        // call the endpoint with the ticketId and ignore the response
        stub.deleteTicket(
            DeleteTicketRequest.newBuilder()
                .setTicketId(ticketId)
                .build(),
        )
    }

    override suspend fun getTicket(ticketId: String): Ticket? {
        // call the endpoint with the ticketId and return the retrieved ticket
        try {
            return stub.getTicket(
                Frontend.GetTicketRequest.newBuilder()
                    .setTicketId(ticketId)
                    .build(),
            )
        } catch (ex: StatusException) {
            // if there is no ticket for this identifier, just return null
            if (ex.status.code == Status.Code.NOT_FOUND) {
                return null
            }

            // rethrow the original exception in every other case
            throw ex
        }
    }

    override fun watchAssignments(ticketId: String): Flow<WatchAssignmentsResponse> {
        // call the endpoint with the ticketId and use the callback to handle responses
        return stub.watchAssignments(
            WatchAssignmentsRequest.newBuilder()
                .setTicketId(ticketId)
                .build(),
        )
    }

    override suspend fun createBackfill(template: TicketTemplate): Backfill {
        // call the endpoint with a template for the backfill and return the new backfill
        return stub.createBackfill(
            CreateBackfillRequest.newBuilder()
                .setBackfill(template.createNewBackfill())
                .build(),
        )
    }

    override suspend fun deleteBackfill(backfillId: String) {
        // call the endpoint with the ticketId and ignore the response
        stub.deleteBackfill(
            DeleteBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .build(),
        )
    }

    override suspend fun getBackfill(backfillId: String): Backfill? {
        // call the endpoint with the backfillId and return the retrieved backfill
        try {
            return stub.getBackfill(
                GetBackfillRequest.newBuilder()
                    .setBackfillId(backfillId)
                    .build(),
            )
        } catch (ex: StatusException) {
            // if there is no ticket for this identifier, just return null
            if (ex.status.code == Status.Code.NOT_FOUND) {
                return null
            }

            // rethrow the original exception in every other case
            throw ex
        }
    }

    override suspend fun updateBackfill(backfill: Backfill): Backfill {
        // call the endpoint with the updated backfill and return the new backfill
        try {
            return stub.updateBackfill(
                UpdateBackfillRequest.newBuilder()
                    .setBackfill(backfill)
                    .build(),
            )
        } catch (ex: StatusException) {
            // if the backfill is not found, convert the value
            if (ex.status.code == Status.Code.NOT_FOUND) {
                throw NoSuchElementException("No backfill was found for the id '${backfill.id}'!")
            }

            // rethrow the original exception in every other case
            throw ex
        }
    }

    override suspend fun acknowledgeBackfill(backfillId: String, assignment: Assignment): AcknowledgeBackfillResponse {
        // call the endpoint with the backfill and the assignment and return the response
        try {
            return stub.acknowledgeBackfill(
                AcknowledgeBackfillRequest.newBuilder()
                    .setBackfillId(backfillId)
                    .setAssignment(assignment)
                    .build(),
            )
        } catch (ex: StatusException) {
            // if the backfill is not found, convert the value
            if (ex.status.code == Status.Code.NOT_FOUND) {
                throw NoSuchElementException("No backfill was found for the id '$backfillId'!")
            }

            // rethrow the original exception in every other case
            throw ex
        }
    }

    override fun close() {
        try {
            // shutdown and wait for it to complete
            val finishedShutdown = channel
                .shutdown()
                .awaitTermination(SHUTDOWN_GRACE_PERIOD.toMillis(), TimeUnit.MILLISECONDS)

            // force shutdown if it did not terminate
            if (!finishedShutdown) {
                channel.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            // log so we know the origin/reason for this interruption
            LOG.debug("Thread was interrupted while waiting for the shutdown of a GrpcOpenMatchClient.", ex)

            // set interrupted status of this thread
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        /** The logger that will be utilized to perform any logging for the methods of this class. */
        private val LOG = LoggerFactory.getLogger(GrpcOpenMatchClient::class.java)

        /** The default host, that will be used to communicate with the gRPC server of the Open Match frontend. */
        private const val DEFAULT_FRONTEND_HOST: String = "localhost"

        /** The key of the environment variable, that can be used to retrieve the assigned gRPC host of the frontend. */
        private const val FRONTEND_HOST_ENV_KEY = "OPEN_MATCH_FRONTEND_GRPC_HOST"

        /** The default port, that will be used to communicate with the gRPC server of the Open Match frontend. */
        private const val DEFAULT_FRONTEND_PORT: Int = 50504

        /** The key of the environment variable, that can be used to retrieve the assigned gRPC port of the frontend. */
        private const val FRONTEND_PORT_ENV_KEY = "OPEN_MATCH_FRONTEND_GRPC_PORT"

        /** The [duration][Duration], that will be waited at maximum for the successful shutdown of the channel. */
        private val SHUTDOWN_GRACE_PERIOD = Duration.ofSeconds(5)

        /**
         * The gRPC host of the Open Match frontend from within the container of this platform.
         *
         * In order to get the effective host, it will be first tried to extract this port through an environment
         * variable with the key [FRONTEND_HOST_ENV_KEY]. If this variable is not set, [DEFAULT_FRONTEND_HOST] will
         * be used instead, which is the default gRPC host for any installation of Open Match.
         */
        val FRONTEND_HOST: String
            get() {
                // read the environment variable for the dynamic open match host
                val host = System.getenv(FRONTEND_HOST_ENV_KEY)

                // check that there was any value and that it is valid and fall back to default
                return host ?: DEFAULT_FRONTEND_HOST
            }

        /**
         * The gRPC port of the Open Match frontend from within the container of this platform.
         *
         * In order to get the effective port, it will be first tried to extract this port through an environment
         * variable with the key [FRONTEND_PORT_ENV_KEY]. If this variable is not set, [DEFAULT_FRONTEND_PORT] will
         * be used instead, which is the default gRPC port for any installation of Open Match.
         */
        val FRONTEND_PORT: Int
            get() {
                // read the environment variable for the dynamic open match port
                val textPort = System.getenv(FRONTEND_PORT_ENV_KEY)

                // check that there was any value and that it is valid
                return if (textPort == null) {
                    // fall back to the default port as it could not be found
                    DEFAULT_FRONTEND_PORT
                } else {
                    // parse the number from the textual environment variable value
                    try {
                        textPort.toInt()
                    } catch (ex: NumberFormatException) {
                        throw IllegalArgumentException(
                            "The supplied environment variable for the port did not contain a valid number.",
                        )
                    }
                }
            }
    }
}
