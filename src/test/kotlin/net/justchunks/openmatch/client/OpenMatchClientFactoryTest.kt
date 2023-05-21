package net.justchunks.openmatch.client

import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class OpenMatchClientFactoryTest {

    @Test
    @DisplayName("Should get an instance")
    fun shouldGetInstance() {
        // given
        val sdk = OpenMatchClientFactory.createNewClient()

        // then
        assertNotNull(sdk)

        // cleanup
        sdk.close()
    }

    @Test
    @DisplayName("Should get a new instance")
    fun shouldGetNewInstance() {
        // given
        val sdk1 = OpenMatchClientFactory.createNewClient()
        val sdk2 = OpenMatchClientFactory.createNewClient()

        // then
        assertNotEquals(sdk1, sdk2)

        // cleanup
        sdk1.close()
        sdk2.close()
    }
}
