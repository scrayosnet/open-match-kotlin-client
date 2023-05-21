package net.justchunks.openmatch.client

import kotlinx.coroutines.flow.Flow
import net.justchunks.openmatch.client.wrapper.TicketTemplate
import openmatch.Frontend.AcknowledgeBackfillResponse
import openmatch.Frontend.WatchAssignmentsResponse
import openmatch.Messages.Assignment
import openmatch.Messages.Backfill
import openmatch.Messages.Ticket

interface OpenMatchClient : AutoCloseable {

    suspend fun createTicket(template: TicketTemplate): Ticket

    suspend fun deleteTicket(ticketId: String)

    suspend fun getTicket(ticketId: String): Ticket?

    fun watchAssignments(ticketId: String): Flow<WatchAssignmentsResponse>

    suspend fun createBackfill(template: TicketTemplate): Backfill

    suspend fun deleteBackfill(backfillId: String)

    suspend fun getBackfill(backfillId: String): Backfill?

    suspend fun updateBackfill(backfill: Backfill): Backfill

    suspend fun acknowledgeBackfill(backfillId: String, assignment: Assignment): AcknowledgeBackfillResponse
}
