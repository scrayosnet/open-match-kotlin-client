# Module Open Match Kotlin Client

This Open Match Client provides the interface for communication with the Open Match matchmaking service and
infrastructure. The different interfaces need to be used in order to register new tickets within Open Match and watch on
changes regarding those tickets or their assignments. Each instance contacts the same (replicated) Frontend and the API
calls are relayed from there to the corresponding services of Open Match. The client stays in contact with the Frontend
throughout the whole lifetime of the instance.

We needed to implement our own Open Match client because there is no official client and the existing alternative would
be to manually implement the gRPC calls on at the game servers (frontend). Therefore, we started implementing our own
solution on-top of the Protobuf definitions that Open Match already provides. We'll try to update the Open Match
mappings every time that there is an API change.

# Package net.justchunks.openmatch.client

The main package of the Client implementation that hosts the overall interface as well as the concrete implementations.
This package can be used to instantiate the desired implementation and to start the communication with the Frontend.

# Package net.justchunks.openmatch.client.wrapper

The package that contains the wrappers of the raw gRPC objects so that those objects can be easier used and constructed
without touching any of the gRPC internals.
