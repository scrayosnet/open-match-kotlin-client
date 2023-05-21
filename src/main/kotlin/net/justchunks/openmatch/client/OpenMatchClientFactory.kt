package net.justchunks.openmatch.client

/**
 * An [OpenMatchClientFactory] is a factory for the creation of new instance of the
 * [Open Match Client][OpenMatchClient]. Through this factory, the concrete implementations of the Client can be
 * retrieved and used for the corresponding platform afterward. The factory already sets defaults and preferences for
 * the different implementations, so that the most robust and most performant variant will be chosen.
 */
class OpenMatchClientFactory private constructor() {

    companion object {

        /**
         * Creates a new instance of an [Open Match Client][OpenMatchClient] with the best possible robustness and
         * performance. This method returns a new, independent instance with each invocation, that shared no connections
         * or resources with all instances that have been created before. In particular, it is guaranteed, that two
         * invocations of this method will create distinct objects. Each platform only needs a single instance of the
         * [client][OpenMatchClient].
         *
         * @return A new instance of the [Open Match Client][OpenMatchClient], that will be used for the communication
         * with the external interface of the Open Match frontend on this platform.
         */
        fun createNewClient(): OpenMatchClient {
            return GrpcOpenMatchClient()
        }
    }
}
