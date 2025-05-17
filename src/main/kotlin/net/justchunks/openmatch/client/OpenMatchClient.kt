package net.justchunks.openmatch.client

import kotlinx.coroutines.flow.Flow
import net.justchunks.openmatch.client.wrapper.TicketTemplate
import openmatch.Frontend.AcknowledgeBackfillResponse
import openmatch.Frontend.WatchAssignmentsResponse
import openmatch.Messages.Assignment
import openmatch.Messages.Backfill
import openmatch.Messages.Ticket

/**
 * The [Open Match Client][OpenMatchClient] represents the technical interface through which this server can communicate
 * with Open Match and allows hooking into matchmaking process. This client allows interaction with the Open Match
 * frontend within the cluster to create and manage Tickets and Backfills. The internal services (query, backend,
 * matchmaking-function, etc.) cannot be contacted directly. For more advanced modifications on the matchmaking process,
 * another implementation of the gRPC interface must be used.
 *
 * Open Match does not work with persistent resources within Kubernetes, which is why changes are immediately visible
 * after the gRPC operation was finished through any of the other interface methods. This also applies to modifications
 * that have been performed by other clients and therefore are only read passively by this client. Those changes are not
 * cached and are directly transferred to the frontend of Open Match.
 *
 * All interfaces are executed asynchronously (through coroutines) and return their results after the response from OM
 * has been recorded. Interfaces that work with streams of data are also executed asynchronously and are wrapped
 * into [flows][Flow] to be compatible with the coroutines. Errors will always be returned immediately if they are
 * discovered and the operation will only be automatically retried, if the returned condition can be recovered from.
 *
 * The signatures of the endpoints of the frontend service of Open Match may have been slightly modified to better fit
 * our structure but generally adhere to the official interface definitions. The client will always be kept compatible
 * with the latest official recommendations and should therefore be used directly, since the individual steps are
 * designed to be atomic.
 *
 * @sample grpcSample
 *
 * @see <a href="https://open-match.dev/site/docs/reference/api/">Open Match API Documentation</a>
 */
interface OpenMatchClient : AutoCloseable {

    /**
     * Creates a new [Ticket] within Open Match with the metadata of a specific [TicketTemplate]. The
     * [identifier][Ticket.getId] and [creation moment][Ticket.getCreateTime] are generated and assigned by Open Match.
     * The [game server assignment][Assignment] is determined by the Director after this [Ticket] has been assigned to
     * a match. The status of the [assignment][Assignment] can be observed using [watchAssignments].
     *
     * @param template The [TicketTemplate] whose metadata should be used to create the [Ticket].
     *
     * @return The newly created [Ticket] with the metadata that has been supplied through the [TicketTemplate] and
     * the generated data by Open Match.
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun createTicket(template: TicketTemplate): Ticket

    /**
     * Deletes an existing [Ticket] with a specific [identifier][Ticket.getId] within Open Match. If this [Ticket] has
     * already been assigned to a match, this operation has no effect. However, the [Ticket] is immediately excluded
     * from further match creation considerations. If no [Ticket] with such an ID exists, this method does not cause any
     * changes within Open Match.
     *
     * @param ticketId The identifier of the [Ticket] to be deleted within Open Match.
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun deleteTicket(ticketId: String)

    /**
     * Retrieves an existing [Ticket] with a specific [identifier][Ticket.getId] within Open Match and returns it. If no
     * [Ticket] exists with this ID, `null` is returned instead. The [Ticket] is always queried in its currently valid
     * state from Open Match, and therefore it may contain changes not made by this client.
     *
     * @param ticketId The identifier of the [Ticket] to be queried from Open Match.
     *
     * @return The existing [Ticket] with the queried identifier or `null`, if no such [Ticket] exists.
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun getTicket(ticketId: String): Ticket?

    /**
     * Subscribes to changes of the [game server assignment][Assignment] of an individual [Ticket] with a specific
     * identifier. The [Flow] receives a new item, whenever a change is made to the [Assignment]. For changes triggered
     * by this client as well as for external changes applied by other clients or other components of the Open Match
     * matchmaking process. The item always contains the most recent state of the [Assignment].
     *
     * To unsubscribe from the updates, the returned [Flow] can be cancelled, and the underlying stream will be closed
     * and cleaned up. It can be started to subscribe again after that, by invoking this method again and obtaining a
     * new, independent [Flow].
     *
     * Since the stream (if not terminated beforehand) runs indefinitely, it may delay the shutdown of the Client, and
     * it should be terminated beforehand. To avoid waiting for the maximum timeout, all streams should be closed
     * beforehand. If there are still open streams, an error will be published in the [Flow], and the underlying stream
     * in gRPC will be terminated.
     *
     * @return A [Flow] of updated [assignment][Assignment] resources that returns a new element every time there was an
     * update to the underlying data. Always contains the whole dataset and not only the modified fields.
     *
     * @sample watchAssignmentsSample
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    fun watchAssignments(ticketId: String): Flow<WatchAssignmentsResponse>

    /**
     * Creates a new [Backfill] within Open Match with the metadata of a specific [TicketTemplate]. The
     * [identifier][Backfill.getId] and the [creation moment][Backfill.getCreateTime] are generated and assigned by Open
     * Match. The [game server assignment][Assignment] is subsequently determined by [acknowledgeBackfill].
     *
     * @param template The [TicketTemplate] whose metadata should be used to create the [Backfill].
     *
     * @return The newly created [Ticket] with the metadata that has been supplied through the [TicketTemplate] and
     * the generated data by Open Match.
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun createBackfill(template: TicketTemplate): Backfill

    /**
     * Deletes an existing [Backfill] with a specific [identifier][Backfill.getId] within Open Match. If this [Backfill]
     * has already been assigned to a match, this operation has no effect. However, the [Backfill] is immediately
     * excluded from further assignment considerations. If no [Backfill] with such an ID exists, this method does not
     * cause any changes within Open Match.
     *
     * @param backfillId The identifier of the [Backfill] to be deleted within Open Match.
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun deleteBackfill(backfillId: String)

    /**
     * Retrieves an existing [Backfill] with a specific [identifier][Backfill.getId] within Open Match and returns it.
     * If no [Backfill] exists with this ID, `null` is returned instead. The [Backfill] is always queried in its current
     * valid state from Open Match, so it may contain changes that were not made by this client.
     *
     * @param backfillId The identifier of the [Backfill] to be queried from Open Match.
     *
     * @return The existing [Backfill] with the queried identifier or `null`, if no such [Backfill] exists.
     *
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun getBackfill(backfillId: String): Backfill?

    /**
     * Updates the metadata associated with a [Backfill] and returns the new [Backfill]. The passed [Backfill] must have
     * the [identifier][Backfill.getId] set. The metadata from the object is completely overwritten, replacing the
     * current metadata of the [Backfill]. The [creation moment][Backfill.getCreateTime] is not updated, but the
     * [generation][Backfill.getGeneration] is incremented. This returns all [tickets][Ticket] waiting on this
     * [Backfill] back to the active pool, making them no longer pending.
     *
     * @param backfill The [Backfill] with the new metadata to be adopted for the existing data structure in Open Match.
     *
     * @return The [Backfill] with the updated metadata and specifications.
     *
     * @throws NoSuchElementException If no [Backfill] with this identifier can be found.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun updateBackfill(backfill: Backfill): Backfill

    /**
     * Notifies Open Match about the [Assignment] or address information of the respective game server. This starts the
     * assignment process and searches for [tickets][Ticket] that can be assigned to this [Backfill]. The retrieved
     * [tickets][Ticket] are included in the return value and can be connected to the corresponding server. If the
     * corresponding [Backfill] does not exist, a [NoSuchElementException] is thrown.
     *
     * @param backfillId The identifier of the [Backfill] whose [Assignment] should be acknowledged.
     * @param assignment The [Assignment] of the game server to be acknowledged for the [Backfill].
     *
     * @return The [status message][AcknowledgeBackfillResponse] for the notification of backfill acknowledgement.
     *
     * @throws NoSuchElementException If no [Backfill] with this identifier can be found.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Documentation</a>
     */
    suspend fun acknowledgeBackfill(backfillId: String, assignment: Assignment): AcknowledgeBackfillResponse

    /**
     * Closes all open resources that are associated with the [Open Match Client][OpenMatchClient]. After this
     * operation, this instance of the [Open Match Client][OpenMatchClient] may no longer be used, as all connections
     * are no longer usable. This method is idempotent and can therefore be called any number of times without changing
     * its behaviour. It is guaranteed, that after this call all open connections and allocated resources will be closed
     * or released. Although not all implementations have such resources, the method should still always be called (for
     * example, within a Try-With_Resources block or a use method) to cleanly terminate resource usage.
     *
     * **Implementation Note**: Closing the open connections and releasing the allocated resources must happen blocking
     * so that the main thread can be safely shut down after this method was called. The difference of this method to
     * [AutoCloseable.close] is, that it is not permitted to trigger any exceptions within this method.
     */
    override fun close()
}
