fun grpcSample() {
    // host and port are supplied by the default environment variables
    val client = GrpcOpenMatchClient()

    // any request can be performed on the client while it is open
    client.deleteTicket("ticket-id")
}

fun watchAssignmentsSample() {
    // host and port are supplied by the default environment variables
    val client = GrpcOpenMatchClient()

    // watch the ticket assignments and act on updates until a condition is met
    client.watchAssignments("ticket-id")
        .takeWhile { !initialized }
        .collect { // do something }
}
