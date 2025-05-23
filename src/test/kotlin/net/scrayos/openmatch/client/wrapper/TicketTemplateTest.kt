package net.scrayos.openmatch.client.wrapper

import com.google.protobuf.Any
import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.DoubleValue
import com.google.protobuf.FloatValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.google.protobuf.StringValue
import com.google.protobuf.UInt32Value
import com.google.protobuf.UInt64Value
import net.scrayos.openmatch.client.wrapper.TicketTemplate.Builder
import net.scrayos.openmatch.client.wrapper.TicketTemplate.Companion.newBuilder
import openmatch.Messages.Backfill
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.Array
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.UInt
import kotlin.ULong
import kotlin.arrayOf
import kotlin.byteArrayOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class TicketTemplateTest {

    @Test
    @DisplayName("Should create new (non-null) builder")
    fun shouldCreateNewBuilder() {
        // when
        val builder: Builder = newBuilder()

        // then
        assertNotNull(builder)
    }

    @Test
    @DisplayName("Should create empty builder")
    fun shouldCreateEmptyBuilder() {
        // given
        val builder: Builder = newBuilder()
        val template: TicketTemplate = builder.build()

        // when
        val ticket = template.createNewTicket()
        val backfill: Backfill = template.createNewBackfill()

        // then
        assertEquals("", ticket.id)
        assertEquals("", ticket.assignment.connection)
        assertEquals(0, ticket.assignment.extensionsCount)
        assertEquals(0, ticket.searchFields.doubleArgsCount)
        assertEquals(0, ticket.searchFields.stringArgsCount)
        assertEquals(0, ticket.searchFields.tagsCount)
        assertEquals(0, ticket.extensionsCount)
        assertEquals(0, ticket.createTime.seconds)
        assertEquals(0, ticket.createTime.nanos)
        assertEquals("", backfill.id)
        assertEquals(0, backfill.searchFields.doubleArgsCount)
        assertEquals(0, backfill.searchFields.stringArgsCount)
        assertEquals(0, backfill.searchFields.tagsCount)
        assertEquals(0, backfill.extensionsCount)
        assertEquals(0, backfill.createTime.seconds)
        assertEquals(0, backfill.createTime.nanos)
    }

    @Test
    @DisplayName("Should create equivalent builder")
    fun shouldCreateEquivalentBuilder() {
        // given
        val template: TicketTemplate = newBuilder()
            .addDoubleArg("a", 1.3)
            .addStringArg("b", "c")
            .addTag("d")
            .addExtension("a", "b")
            .addExtension("c", true)
            .build()
        val builder: Builder = newBuilder(template)

        // when
        val otherTemplate: TicketTemplate = builder.build()

        // then
        assertEquals(template, otherTemplate)
        assertEquals(template.createNewTicket(), otherTemplate.createNewTicket())
        assertEquals(template.createNewBackfill(), otherTemplate.createNewBackfill())
    }

    @Test
    @DisplayName("Should create new (non-null) ticket from template")
    fun shouldCreateTicketFromTemplate() {
        // when
        val template: TicketTemplate = newBuilder()
            .build()

        // then
        assertNotNull(template.createNewTicket())
    }

    @Test
    @DisplayName("Should create new (non-null) backfill from template")
    fun shouldCreateBackfillFromTemplate() {
        // when
        val template: TicketTemplate = newBuilder()
            .build()

        // then
        assertNotNull(template.createNewBackfill())
    }

    @Test
    @DisplayName("Should create equal tickets from template")
    fun shouldCreateEqualTicketsFromTemplate() {
        // when
        val template: TicketTemplate = newBuilder()
            .build()

        // then
        assertEquals(template.createNewTicket(), template.createNewTicket())
    }

    @Test
    @DisplayName("Should create equal backfills from template")
    fun shouldCreateEqualBackfillsFromTemplate() {
        // when
        val template: TicketTemplate = newBuilder()
            .build()

        // then
        assertEquals(template.createNewBackfill(), template.createNewBackfill())
    }

    @Test
    @DisplayName("Should be immutable")
    fun shouldBeImmutable() {
        // given
        val builder: Builder = newBuilder()
            .addDoubleArg("a", 10.0)

        // when
        val template: TicketTemplate = builder.build()
        builder.addDoubleArg("a", 20.0)

        // then
        assertEquals(10.0, template.createNewTicket().searchFields.getDoubleArgsOrDefault("a", 5.0))
        assertEquals(10.0, template.createNewBackfill().searchFields.getDoubleArgsOrDefault("a", 5.0))
    }

    @Test
    @DisplayName("Should be equal")
    fun shouldBeEqual() {
        // given
        val template1: TicketTemplate = newBuilder()
            .addExtension("a", "banana")
            .build()
        val template2: TicketTemplate = newBuilder()
            .addExtension("a", "banana")
            .build()

        // then
        assertEquals(template1, template1)
        assertEquals(template2, template2)
        assertEquals(template1, template2)
        assertEquals(template2, template1)
    }

    @Test
    @DisplayName("Should not be equal (searchFields)")
    fun shouldNotBeEqualSearchFields() {
        // given
        val template1: TicketTemplate = newBuilder()
            .addStringArg("a", "banana")
            .build()
        val template2: TicketTemplate = newBuilder()
            .addStringArg("a", "banana2")
            .build()

        // then
        assertNotEquals(template1, template2)
    }

    @Test
    @DisplayName("Should not be equal (extensions)")
    fun shouldNotBeEqualExtensions() {
        // given
        val template1: TicketTemplate = newBuilder()
            .addExtension("a", "banana")
            .build()
        val template2: TicketTemplate = newBuilder()
            .addExtension("a", "banana2")
            .build()

        // then
        assertNotEquals(template1, template2)
    }

    @Test
    @DisplayName("Should not be equal (null)")
    fun shouldNotBeEqualNull() {
        // given
        val template: TicketTemplate = newBuilder()
            .addExtension("a", "banana")
            .build()

        // then
        assertFalse(template.equals(null))
    }

    @Test
    @DisplayName("Should not be equal (other type)")
    fun shouldNotBeEqualType() {
        // given
        val template = newBuilder()
            .addExtension("a", "banana")
            .build()

        // then
        assertFalse(template.equals(""))
    }

    @Test
    @DisplayName("Should have equal HashCode")
    fun shouldHaveEqualHashCode() {
        // given
        val template1: TicketTemplate = newBuilder()
            .addExtension("a", "b")
            .build()
        val template2: TicketTemplate = newBuilder()
            .addExtension("a", "b")
            .build()

        // then
        assertEquals(template1.hashCode(), template2.hashCode())
    }

    @Test
    @DisplayName("Should not have equal HashCode")
    fun shouldNotHaveEqualHashCode() {
        // given
        val template1: TicketTemplate = newBuilder()
            .addExtension("a", "a")
            .build()
        val template2: TicketTemplate = newBuilder()
            .addExtension("a", "b")
            .build()

        // then
        assertNotEquals(template1.hashCode(), template2.hashCode())
    }

    @Test
    @DisplayName("Should generate correct toString()")
    fun shouldGenerateCorrectToString() {
        // given
        val className = TicketTemplate::class.java.simpleName
        val template1: TicketTemplate = newBuilder()
            .addExtension("a", "d")
            .build()
        val template2: TicketTemplate = newBuilder()
            .addExtension("a", "b")
            .build()

        // when
        val template1String = template1.toString()
        val template2String = template2.toString()

        // then
        assertTrue(template1String.contains(className))
        assertTrue(template2String.contains(className))
        assertNotEquals(template1String, template2String)
    }

    @Nested
    internal inner class BuilderTest {
        @ParameterizedTest(name = "#addStringArg({0})")
        @ValueSource(strings = ["test", "otherTest", "other_test", "banana", "UPPERCASE", "lowercase", "high⛰foot️"])
        @DisplayName("Should add string arg (searchFields)")
        fun shouldAddStringArg(value: String) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addStringArg("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val stringArgsMap = ticket.searchFields.stringArgsMap

            // then
            assertTrue(stringArgsMap.containsKey("a"))
            assertEquals(value, stringArgsMap["a"])
        }

        @Test
        @DisplayName("Should override string arg (searchFields)")
        fun shouldOverrideStringArg() {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addStringArg("a", "b")
            builder.addStringArg("a", "c")
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals("c", ticket.searchFields.getStringArgsOrDefault("a", "a"))
        }

        @Test
        @DisplayName("Should add differently cased string arg (searchFields)")
        fun shouldAddDifferentlyCasedStringArg() {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addStringArg("a", "b")
            builder.addStringArg("A", "c")
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals("b", ticket.searchFields.getStringArgsOrDefault("a", "a"))
            assertEquals("c", ticket.searchFields.getStringArgsOrDefault("A", "a"))
            assertEquals(2, ticket.searchFields.stringArgsCount)
        }

        @ParameterizedTest(name = "#addDoubleArg({0})")
        @ValueSource(doubles = [0.0, -0.0, 5.0, -5.0, 1.5, -1.5, 1000.0, -1000.0])
        @DisplayName("Should add double arg (searchFields)")
        fun shouldAddDoubleArg(value: Double) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addDoubleArg("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val doubleArgsMap = ticket.searchFields.doubleArgsMap

            // then
            assertTrue(doubleArgsMap.containsKey("a"))
            assertEquals(value, doubleArgsMap["a"])
        }

        @Test
        @DisplayName("Should override double arg (searchFields)")
        fun shouldOverrideDoubleArg() {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addDoubleArg("a", 10.0)
            builder.addDoubleArg("a", 15.0)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(15.0, ticket.searchFields.getDoubleArgsOrDefault("a", 5.0))
        }

        @Test
        @DisplayName("Should add differently cased double arg (searchFields)")
        fun shouldAddDifferentlyCasedDoubleArg() {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addDoubleArg("a", 10.0)
            builder.addDoubleArg("A", 15.0)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(10.0, ticket.searchFields.getDoubleArgsOrDefault("a", 5.0))
            assertEquals(15.0, ticket.searchFields.getDoubleArgsOrDefault("A", 5.0))
            assertEquals(2, ticket.searchFields.doubleArgsCount)
        }

        @ParameterizedTest(name = "#addTag({0})")
        @ValueSource(strings = ["test", "otherTest", "other_test", "banana", "UPPERCASE", "LOWERCASE", "high⛰foot️"])
        @DisplayName("Should add tag (searchFields)")
        fun shouldAddTag(value: String) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addTag(value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val tagList = ticket.searchFields.tagsList

            // then
            assertEquals(1, tagList.size)
            assertTrue(tagList.contains(value))
        }

        @Test
        @DisplayName("Should add differently cased tag (searchFields)")
        fun shouldAddDifferentlyCasedTag() {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addTag("a")
            builder.addTag("A")
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertTrue(ticket.searchFields.tagsList.contains("a"))
            assertTrue(ticket.searchFields.tagsList.contains("A"))
            assertEquals(2, ticket.searchFields.tagsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(doubles = [0.0, -0.0, 5.0, -5.0, 1.5, -1.5, 1000.0, -1000.0])
        @DisplayName("Should add double extension (extensions)")
        fun shouldAddDoubleExtension(value: Double) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(DoubleValue.of(value)), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override double extension (extensions)")
        fun shouldOverrideDoubleExtension() {
            // given
            val firstValue = 10.0
            val secondValue = 15.0
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(DoubleValue.of(secondValue)), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased double extension (extensions)")
        fun shouldAddDifferentlyCasedDoubleExtension() {
            // given
            val firstValue = 10.0
            val secondValue = 15.0
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(DoubleValue.of(firstValue)), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(DoubleValue.of(secondValue)), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(floats = [0f, -0f, 5f, -5f, 1.5f, -1.5f, 1000f, -1000f])
        @DisplayName("Should add float extension (extensions)")
        fun shouldAddFloatExtension(value: Float) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(FloatValue.of(value)), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override float extension (extensions)")
        fun shouldOverrideFloatExtension() {
            // given
            val firstValue = 10f
            val secondValue = 15f
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(FloatValue.of(secondValue)), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased float extension (extensions)")
        fun shouldAddDifferentlyCasedFloatExtension() {
            // given
            val firstValue = 10f
            val secondValue = 15f
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(FloatValue.of(firstValue)), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(FloatValue.of(secondValue)), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(longs = [0L, -0L, 5L, -5L, 1000L, -1000L])
        @DisplayName("Should add long extension (extensions)")
        fun shouldAddLongExtension(value: Long) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(Int64Value.of(value)), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override long extension (extensions)")
        fun shouldOverrideLongExtension() {
            // given
            val firstValue = 10L
            val secondValue = 15L
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(Int64Value.of(secondValue)), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased long extension (extensions)")
        fun shouldAddDifferentlyCasedLongExtension() {
            // given
            val firstValue = 10L
            val secondValue = 15L
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(Int64Value.of(firstValue)), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(Int64Value.of(secondValue)), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(ints = [0, -0, 5, -5, 1000, -1000])
        @DisplayName("Should add int extension (extensions)")
        fun shouldAddIntExtension(value: Int) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(Int32Value.of(value)), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override int extension (extensions)")
        fun shouldOverrideIntExtension() {
            // given
            val firstValue = 10
            val secondValue = 15
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(Int32Value.of(secondValue)), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased int extension (extensions)")
        fun shouldAddDifferentlyCasedIntExtension() {
            // given
            val firstValue = 10
            val secondValue = 15
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(Int32Value.of(firstValue)), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(Int32Value.of(secondValue)), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(booleans = [false, true])
        @DisplayName("Should add boolean extension (extensions)")
        fun shouldAddBooleanExtension(value: Boolean) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(BoolValue.of(value)), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override boolean extension (extensions)")
        fun shouldOverrideBooleanExtension() {
            // given
            val firstValue = false
            val secondValue = true
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(BoolValue.of(secondValue)), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased boolean extension (extensions)")
        fun shouldAddDifferentlyCasedBooleanExtension() {
            // given
            val firstValue = false
            val secondValue = true
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(BoolValue.of(firstValue)), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(BoolValue.of(secondValue)), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(strings = ["test", "otherTest", "other_test", "banana", "UPPERCASE", "LOWERCASE", "high⛰foot️"])
        @DisplayName("Should add string extension (extensions)")
        fun shouldAddStringExtension(value: String) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(StringValue.of(value)), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override string extension (extensions)")
        fun shouldOverrideStringExtension() {
            // given
            val firstValue = "banana"
            val secondValue = "apple"
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(StringValue.of(secondValue)), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased string extension (extensions)")
        fun shouldAddDifferentlyCasedStringExtension() {
            // given
            val firstValue = "banana"
            val secondValue = "apple"
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(StringValue.of(firstValue)), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(StringValue.of(secondValue)), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(
            strings = [
                "94be7616-cf3b-4dc8-8e13-a30fdfd90ac1",
                "8bee6ec0-87d5-424e-8d4c-cfe6acd4fd11",
                "f33cde18-152a-44b8-aadc-040d5b736844",
            ],
        )
        @DisplayName("Should add UUID extension (extensions)")
        fun shouldAddUuidExtension(rawValue: String?) {
            // given
            val value = UUID.fromString(rawValue)
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(
                Any.pack(StringValue.of(value.toString())),
                ticket.getExtensionsOrDefault("a", null),
            )
        }

        @Test
        @DisplayName("Should override UUID extension (extensions)")
        fun shouldOverrideUuidExtension() {
            // given
            val firstValue = UUID.randomUUID()
            val secondValue = UUID.randomUUID()
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(StringValue.of(secondValue.toString())), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased UUID extension (extensions)")
        fun shouldAddDifferentlyCasedUuidExtension() {
            // given
            val firstValue = UUID.randomUUID()
            val secondValue = UUID.randomUUID()
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(StringValue.of(firstValue.toString())), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(StringValue.of(secondValue.toString())), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @MethodSource("net.scrayos.openmatch.client.wrapper.TicketTemplateTest#testBytes")
        @DisplayName("Should add bytes extension (extensions)")
        fun shouldAddBytesExtension(value: ByteArray) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(
                Any.pack(BytesValue.of(ByteString.copyFrom(value))),
                ticket.getExtensionsOrDefault("a", null),
            )
        }

        @Test
        @DisplayName("Should override bytes extension (extensions)")
        fun shouldOverrideBytesExtension() {
            // given
            val firstValue = byteArrayOf(0.toByte(), 1.toByte(), 2.toByte())
            val secondValue = byteArrayOf(2.toByte(), 1.toByte(), 0.toByte())
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(BytesValue.of(ByteString.copyFrom(secondValue))), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased bytes extension (extensions)")
        fun shouldAddDifferentlyCasedBytesExtension() {
            // given
            val firstValue = byteArrayOf(0.toByte(), 1.toByte(), 2.toByte())
            val secondValue = byteArrayOf(2.toByte(), 1.toByte(), 0.toByte())
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(BytesValue.of(ByteString.copyFrom(firstValue))), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(BytesValue.of(ByteString.copyFrom(secondValue))), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(longs = [0L, -0L, 5L, -5L, 1000L, -1000L])
        @DisplayName("Should add ulong extension (extensions)")
        fun shouldAddULongExtension(value: ULong) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(UInt64Value.of(value.toLong())), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override ulong extension (extensions)")
        fun shouldOverrideULongExtension() {
            // given
            val firstValue: ULong = 10u
            val secondValue: ULong = 15u
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(UInt64Value.of(secondValue.toLong())), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased ulong extension (extensions)")
        fun shouldAddDifferentlyCasedULongExtension() {
            // given
            val firstValue: ULong = 10u
            val secondValue: ULong = 15u
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(UInt64Value.of(firstValue.toLong())), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(UInt64Value.of(secondValue.toLong())), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(ints = [0, -0, 5, -5, 1000, -1000])
        @DisplayName("Should add uint extension (extensions)")
        fun shouldAddUIntExtension(value: UInt) {
            // given
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", value)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()

            // then
            assertEquals(Any.pack(UInt32Value.of(value.toInt())), ticket.getExtensionsOrDefault("a", null))
        }

        @Test
        @DisplayName("Should override uint extension (extensions)")
        fun shouldOverrideUIntExtension() {
            // given
            val firstValue = 10u
            val secondValue = 15u
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("a", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(UInt32Value.of(secondValue.toInt())), extensionMap["a"])
        }

        @Test
        @DisplayName("Should add differently cased uint extension (extensions)")
        fun shouldAddDifferentlyCasedUIntExtension() {
            // given
            val firstValue = 10u
            val secondValue = 15u
            val builder: Builder = newBuilder()

            // when
            builder.addExtension("a", firstValue)
            builder.addExtension("A", secondValue)
            val template: TicketTemplate = builder.build()
            val ticket = template.createNewTicket()
            val extensionMap = ticket.extensionsMap

            // then
            assertTrue(extensionMap.containsKey("a"))
            assertEquals(Any.pack(UInt32Value.of(firstValue.toInt())), extensionMap["a"])
            assertTrue(extensionMap.containsKey("A"))
            assertEquals(Any.pack(UInt32Value.of(secondValue.toInt())), extensionMap["A"])
            assertEquals(2, ticket.extensionsCount)
        }

        @Test
        @DisplayName("Should create new (non-null) template")
        fun shouldCreateNewTemplate() {
            // given
            val builder: Builder = newBuilder()

            // when
            val template: TicketTemplate = builder.build()

            // then
            assertNotNull(template)
        }

        @Test
        @DisplayName("Should be equal")
        fun shouldBeEqual() {
            // given
            val builder1: Builder = newBuilder()
                .addExtension("a", "banana")
            val builder2: Builder = newBuilder()
                .addExtension("a", "banana")

            // then
            assertEquals(builder1, builder1)
            assertEquals(builder2, builder2)
            assertEquals(builder1, builder2)
            assertEquals(builder2, builder1)
        }

        @Test
        @DisplayName("Should not be equal (searchFields)")
        fun shouldNotBeEqualSearchFields() {
            // given
            val builder1: Builder = newBuilder()
                .addStringArg("a", "banana")
            val builder2: Builder = newBuilder()
                .addStringArg("a", "banana2")

            // then
            assertNotEquals(builder1, builder2)
        }

        @Test
        @DisplayName("Should not be equal (extensions)")
        fun shouldNotBeEqualExtensions() {
            // given
            val builder1: Builder = newBuilder()
                .addExtension("a", "banana")
            val builder2: Builder = newBuilder()
                .addExtension("a", "banana2")

            // then
            assertNotEquals(builder1, builder2)
        }

        @Test
        @DisplayName("Should not be equal (null)")
        fun shouldNotBeEqualNull() {
            // given
            val builder: Builder = newBuilder()
                .addExtension("a", "banana")

            // then
            assertFalse(builder.equals(null))
        }

        @Test
        @DisplayName("Should not be equal (other type)")
        fun shouldNotBeEqualType() {
            // given
            val builder: Builder = newBuilder()
                .addExtension("a", "banana")

            // then
            assertFalse(builder.equals(""))
        }

        @Test
        @DisplayName("Should have equal HashCode")
        fun shouldHaveEqualHashCode() {
            // given
            val builder1: Builder = newBuilder()
                .addExtension("a", "b")
            val builder2: Builder = newBuilder()
                .addExtension("a", "b")

            // then
            assertEquals(builder1.hashCode(), builder2.hashCode())
        }

        @Test
        @DisplayName("Should not have equal HashCode")
        fun shouldNotHaveEqualHashCode() {
            // given
            val builder1: Builder = newBuilder()
                .addExtension("a", "a")
            val builder2: Builder = newBuilder()
                .addExtension("a", "b")

            // then
            assertNotEquals(builder1.hashCode(), builder2.hashCode())
        }

        @Test
        @DisplayName("Should generate correct toString()")
        fun shouldGenerateCorrectToString() {
            // given
            val className: String = Builder::class.java.simpleName
            val builder1: Builder = newBuilder()
                .addExtension("a", "d")
            val builder2: Builder = newBuilder()
                .addExtension("a", "b")

            // when
            val builder1String: String = builder1.toString()
            val builder2String: String = builder2.toString()

            // then
            assertTrue(builder1String.contains(className))
            assertTrue(builder2String.contains(className))
            assertNotEquals(builder1String, builder2String)
        }
    }

    companion object {

        @JvmStatic
        fun testBytes(): Array<ByteArray> = arrayOf(
            byteArrayOf(2.toByte(), 3.toByte(), 5.toByte()),
            byteArrayOf((-2).toByte(), 2.toByte(), (-4).toByte()),
            byteArrayOf(127.toByte(), (-0).toByte(), (-128).toByte()),
        )
    }
}
