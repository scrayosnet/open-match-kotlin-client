package net.justchunks.openmatch.client.wrapper;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.StringValue;
import net.justchunks.openmatch.client.wrapper.TicketTemplate.TicketTemplateBuilder;
import openmatch.Messages.Backfill;
import openmatch.Messages.Ticket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.UUID;

class TicketTemplateTest {

    @Test
    @DisplayName("Should create new (non-null) builder")
    void shouldCreateNewBuilder() {
        // when
        TicketTemplateBuilder builder = TicketTemplate.newBuilder();

        // then
        Assertions.assertNotNull(builder);
    }

    @Test
    @DisplayName("Should create empty builder")
    void shouldCreateEmptyBuilder() {
        // given
        TicketTemplateBuilder builder = TicketTemplate.newBuilder();
        TicketTemplate template = builder.build();

        // when
        Ticket ticket = template.createNewTicket();
        Backfill backfill = template.createNewBackfill();

        // then
        Assertions.assertEquals("", ticket.getId());
        Assertions.assertEquals("", ticket.getAssignment().getConnection());
        Assertions.assertEquals(0, ticket.getAssignment().getExtensionsCount());
        Assertions.assertEquals(0, ticket.getSearchFields().getDoubleArgsCount());
        Assertions.assertEquals(0, ticket.getSearchFields().getStringArgsCount());
        Assertions.assertEquals(0, ticket.getSearchFields().getTagsCount());
        Assertions.assertEquals(0, ticket.getExtensionsCount());
        Assertions.assertEquals(0, ticket.getCreateTime().getSeconds());
        Assertions.assertEquals(0, ticket.getCreateTime().getNanos());

        Assertions.assertEquals("", backfill.getId());
        Assertions.assertEquals(0, backfill.getSearchFields().getDoubleArgsCount());
        Assertions.assertEquals(0, backfill.getSearchFields().getStringArgsCount());
        Assertions.assertEquals(0, backfill.getSearchFields().getTagsCount());
        Assertions.assertEquals(0, backfill.getExtensionsCount());
        Assertions.assertEquals(0, backfill.getCreateTime().getSeconds());
        Assertions.assertEquals(0, backfill.getCreateTime().getNanos());
    }

    @Test
    @DisplayName("Should create equivalent builder")
    void shouldCreateEquivalentBuilder() {
        // given
        TicketTemplate template = TicketTemplate.newBuilder()
            .addDoubleArg("a", 1.3D)
            .addStringArg("b", "c")
            .addTag("d")
            .addExtension("a", "b")
            .addExtension("c", true)
            .build();
        TicketTemplateBuilder builder = TicketTemplate.newBuilder(template);

        // when
        TicketTemplate otherTemplate = builder.build();

        // then
        Assertions.assertEquals(template, otherTemplate);
        Assertions.assertEquals(template.createNewTicket(), otherTemplate.createNewTicket());
        Assertions.assertEquals(template.createNewBackfill(), otherTemplate.createNewBackfill());
    }

    @Test
    @DisplayName("Should create new (non-null) ticket from template")
    void shouldCreateTicketFromTemplate() {
        // when
        TicketTemplate template = TicketTemplate.newBuilder()
            .build();

        // then
        Assertions.assertNotNull(template.createNewTicket());
    }

    @Test
    @DisplayName("Should create new (non-null) backfill from template")
    void shouldCreateBackfillFromTemplate() {
        // when
        TicketTemplate template = TicketTemplate.newBuilder()
            .build();

        // then
        Assertions.assertNotNull(template.createNewBackfill());
    }

    @Test
    @DisplayName("Should create equal tickets from template")
    void shouldCreateEqualTicketsFromTemplate() {
        // when
        TicketTemplate template = TicketTemplate.newBuilder()
            .build();

        // then
        Assertions.assertEquals(template.createNewTicket(), template.createNewTicket());
    }

    @Test
    @DisplayName("Should create equal backfills from template")
    void shouldCreateEqualBackfillsFromTemplate() {
        // when
        TicketTemplate template = TicketTemplate.newBuilder()
            .build();

        // then
        Assertions.assertEquals(template.createNewBackfill(), template.createNewBackfill());
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        // given
        TicketTemplateBuilder builder = TicketTemplate.newBuilder()
            .addDoubleArg("a", 10);

        // when
        TicketTemplate template = builder.build();
        builder.addDoubleArg("a", 20);

        // then
        Assertions.assertEquals(10, template.createNewTicket().getSearchFields().getDoubleArgsOrDefault("a", 5));
        Assertions.assertEquals(10, template.createNewBackfill().getSearchFields().getDoubleArgsOrDefault("a", 5));
    }

    @Test
    @DisplayName("Should be equal")
    void shouldBeEqual() {
        // given
        TicketTemplate template1 = TicketTemplate.newBuilder()
            .addExtension("a", "banana")
            .build();
        TicketTemplate template2 = TicketTemplate.newBuilder()
            .addExtension("a", "banana")
            .build();

        // then
        Assertions.assertEquals(template1, template1);
        Assertions.assertEquals(template2, template2);
        Assertions.assertEquals(template1, template2);
        Assertions.assertEquals(template2, template1);
    }

    @Test
    @DisplayName("Should not be equal (searchFields)")
    void shouldNotBeEqualSearchFields() {
        // given
        TicketTemplate template1 = TicketTemplate.newBuilder()
            .addStringArg("a", "banana")
            .build();
        TicketTemplate template2 = TicketTemplate.newBuilder()
            .addStringArg("a", "banana2")
            .build();

        // then
        Assertions.assertNotEquals(template1, template2);
    }

    @Test
    @DisplayName("Should not be equal (extensions)")
    void shouldNotBeEqualExtensions() {
        // given
        TicketTemplate template1 = TicketTemplate.newBuilder()
            .addExtension("a", "banana")
            .build();
        TicketTemplate template2 = TicketTemplate.newBuilder()
            .addExtension("a", "banana2")
            .build();

        // then
        Assertions.assertNotEquals(template1, template2);
    }

    @Test
    @SuppressWarnings("java:S5785")
    @DisplayName("Should not be equal (null)")
    void shouldNotBeEqualNull() {
        // given
        TicketTemplate template = TicketTemplate.newBuilder()
            .addExtension("a", "banana")
            .build();

        // then
        //noinspection ConstantConditions,SimplifiableAssertion
        Assertions.assertFalse(template.equals(null));
    }

    @Test
    @SuppressWarnings("java:S5785")
    @DisplayName("Should not be equal (other type)")
    void shouldNotBeEqualType() {
        // given
        TicketTemplate template = TicketTemplate.newBuilder()
            .addExtension("a", "banana")
            .build();

        // then
        //noinspection SimplifiableAssertion,EqualsBetweenInconvertibleTypes
        Assertions.assertFalse(template.equals(""));
    }

    @Test
    @DisplayName("Should have equal HashCode")
    void shouldHaveEqualHashCode() {
        // given
        TicketTemplate template1 = TicketTemplate.newBuilder()
            .addExtension("a", "b")
            .build();
        TicketTemplate template2 = TicketTemplate.newBuilder()
            .addExtension("a", "b")
            .build();

        // then
        Assertions.assertEquals(template1.hashCode(), template2.hashCode());
    }

    @Test
    @DisplayName("Should not have equal HashCode")
    void shouldNotHaveEqualHashCode() {
        // given
        TicketTemplate template1 = TicketTemplate.newBuilder()
            .addExtension("a", "a")
            .build();
        TicketTemplate template2 = TicketTemplate.newBuilder()
            .addExtension("a", "b")
            .build();

        // then
        Assertions.assertNotEquals(template1.hashCode(), template2.hashCode());
    }

    @Test
    @DisplayName("Should generate correct toString()")
    void shouldGenerateCorrectToString() {
        // given
        String className = TicketTemplate.class.getSimpleName();
        TicketTemplate template1 = TicketTemplate.newBuilder()
            .addExtension("a", "d")
            .build();
        TicketTemplate template2 = TicketTemplate.newBuilder()
            .addExtension("a", "b")
            .build();

        // when
        String template1String = template1.toString();
        String template2String = template2.toString();

        // then
        Assertions.assertTrue(template1String.contains(className));
        Assertions.assertTrue(template2String.contains(className));
        Assertions.assertNotEquals(template1String, template2String);
    }

    @Nested
    class TicketTemplateBuilderTest {

        @ParameterizedTest(name = "#addStringArg({0})")
        @ValueSource(strings = {"test", "otherTest", "other_test", "banana", "UPPERCASE", "lowercase", "high⛰foot️"})
        @DisplayName("Should add string arg (searchFields)")
        void shouldAddStringArg(String value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addStringArg("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            final Map<String, String> stringArgsMap = ticket.getSearchFields().getStringArgsMap();

            // then
            Assertions.assertTrue(stringArgsMap.containsKey("a"));
            Assertions.assertEquals(value, stringArgsMap.get("a"));
        }

        @Test
        @DisplayName("Should override string arg (searchFields)")
        void shouldOverrideStringArg() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addStringArg("a", "b");
            builder.addStringArg("a", "c");
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals("c", ticket.getSearchFields().getStringArgsOrDefault("a", "a"));
        }

        @Test
        @DisplayName("Should add differently cased string arg (searchFields)")
        void shouldAddDifferentlyCasedStringArg() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addStringArg("a", "b");
            builder.addStringArg("A", "c");
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals("b", ticket.getSearchFields().getStringArgsOrDefault("a", "a"));
            Assertions.assertEquals("c", ticket.getSearchFields().getStringArgsOrDefault("A", "a"));
            Assertions.assertEquals(2, ticket.getSearchFields().getStringArgsCount());
        }

        @ParameterizedTest(name = "#addDoubleArg({0})")
        @ValueSource(doubles = {0D, -0D, 5D, -5D, 1.5D, -1.5D, 1000D, -1000D})
        @DisplayName("Should add double arg (searchFields)")
        void shouldAddDoubleArg(double value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addDoubleArg("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            final Map<String, Double> doubleArgsMap = ticket.getSearchFields().getDoubleArgsMap();

            // then
            Assertions.assertTrue(doubleArgsMap.containsKey("a"));
            Assertions.assertEquals(value, doubleArgsMap.get("a"));
        }

        @Test
        @DisplayName("Should override double arg (searchFields)")
        void shouldOverrideDoubleArg() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addDoubleArg("a", 10D);
            builder.addDoubleArg("a", 15D);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(15D, ticket.getSearchFields().getDoubleArgsOrDefault("a", 5D));
        }

        @Test
        @DisplayName("Should add differently cased double arg (searchFields)")
        void shouldAddDifferentlyCasedDoubleArg() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addDoubleArg("a", 10D);
            builder.addDoubleArg("A", 15D);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(10D, ticket.getSearchFields().getDoubleArgsOrDefault("a", 5D));
            Assertions.assertEquals(15D, ticket.getSearchFields().getDoubleArgsOrDefault("A", 5D));
            Assertions.assertEquals(2, ticket.getSearchFields().getDoubleArgsCount());
        }

        @ParameterizedTest(name = "#addTag({0})")
        @ValueSource(strings = {"test", "otherTest", "other_test", "banana", "UPPERCASE", "LOWERCASE", "high⛰foot️"})
        @DisplayName("Should add tag (searchFields)")
        void shouldAddTag(String value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addTag(value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            final ProtocolStringList tagList = ticket.getSearchFields().getTagsList();

            // then
            Assertions.assertEquals(1, tagList.size());
            Assertions.assertTrue(tagList.contains(value));
        }

        @Test
        @DisplayName("Should add differently cased tag (searchFields)")
        void shouldAddDifferentlyCasedTag() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addTag("a");
            builder.addTag("A");
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertTrue(ticket.getSearchFields().getTagsList().contains("a"));
            Assertions.assertTrue(ticket.getSearchFields().getTagsList().contains("A"));
            Assertions.assertEquals(2, ticket.getSearchFields().getTagsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(doubles = {0D, -0D, 5D, -5D, 1.5D, -1.5D, 1000D, -1000D})
        @DisplayName("Should add double extension (extensions)")
        void shouldAddDoubleExtension(double value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", (value));
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(DoubleValue.of(value)), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override double extension (extensions)")
        void shouldOverrideDoubleExtension() {
            // given
            double firstValue = 10D;
            double secondValue = 15D;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(DoubleValue.of(secondValue)), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased double extension (extensions)")
        void shouldAddDifferentlyCasedDoubleExtension() {
            // given
            double firstValue = 10D;
            double secondValue = 15D;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(DoubleValue.of(firstValue)), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(DoubleValue.of(secondValue)), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(floats = {0F, -0F, 5F, -5F, 1.5F, -1.5F, 1000F, -1000F})
        @DisplayName("Should add float extension (extensions)")
        void shouldAddFloatExtension(float value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", (value));
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(FloatValue.of(value)), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override float extension (extensions)")
        void shouldOverrideFloatExtension() {
            // given
            float firstValue = 10F;
            float secondValue = 15F;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(FloatValue.of(secondValue)), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased float extension (extensions)")
        void shouldAddDifferentlyCasedFloatExtension() {
            // given
            float firstValue = 10F;
            float secondValue = 15F;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(FloatValue.of(firstValue)), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(FloatValue.of(secondValue)), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(longs = {0L, -0L, 5L, -5L, 1000L, -1000L})
        @DisplayName("Should add long extension (extensions)")
        void shouldAddLongExtension(long value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(Int64Value.of(value)), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override long extension (extensions)")
        void shouldOverrideLongExtension() {
            // given
            long firstValue = 10L;
            long secondValue = 15L;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(Int64Value.of(secondValue)), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased long extension (extensions)")
        void shouldAddDifferentlyCasedLongExtension() {
            // given
            long firstValue = 10L;
            long secondValue = 15L;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(Int64Value.of(firstValue)), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(Int64Value.of(secondValue)), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(ints = {0, -0, 5, -5, 1000, -1000})
        @DisplayName("Should add int extension (extensions)")
        void shouldAddIntExtension(int value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(Int32Value.of(value)), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override int extension (extensions)")
        void shouldOverrideIntExtension() {
            // given
            int firstValue = 10;
            int secondValue = 15;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(Int32Value.of(secondValue)), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased int extension (extensions)")
        void shouldAddDifferentlyCasedIntExtension() {
            // given
            int firstValue = 10;
            int secondValue = 15;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(Int32Value.of(firstValue)), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(Int32Value.of(secondValue)), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(booleans = {false, true})
        @DisplayName("Should add boolean extension (extensions)")
        void shouldAddBooleanExtension(boolean value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(BoolValue.of(value)), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override boolean extension (extensions)")
        void shouldOverrideBooleanExtension() {
            // given
            boolean firstValue = false;
            boolean secondValue = true;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(BoolValue.of(secondValue)), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased boolean extension (extensions)")
        void shouldAddDifferentlyCasedBooleanExtension() {
            // given
            boolean firstValue = false;
            boolean secondValue = true;
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(BoolValue.of(firstValue)), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(BoolValue.of(secondValue)), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(strings = {"test", "otherTest", "other_test", "banana", "UPPERCASE", "LOWERCASE", "high⛰foot️"})
        @DisplayName("Should add string extension (extensions)")
        void shouldAddStringExtension(String value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(StringValue.of(value)), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override string extension (extensions)")
        void shouldOverrideStringExtension() {
            // given
            String firstValue = "banana";
            String secondValue = "apple";
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(StringValue.of(secondValue)), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased string extension (extensions)")
        void shouldAddDifferentlyCasedStringExtension() {
            // given
            String firstValue = "banana";
            String secondValue = "apple";
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(StringValue.of(firstValue)), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(StringValue.of(secondValue)), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @MethodSource("testBytes")
        @DisplayName("Should add bytes extension (extensions)")
        void shouldAddBytesExtension(byte[] value) {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(
                Any.pack(BytesValue.of(ByteString.copyFrom(value))),
                ticket.getExtensionsOrDefault("a", null)
            );
        }

        @Test
        @DisplayName("Should override bytes extension (extensions)")
        void shouldOverrideBytesExtension() {
            // given
            byte[] firstValue = new byte[] {(byte) 0, (byte) 1, (byte) 2};
            byte[] secondValue = new byte[] {(byte) 2, (byte) 1, (byte) 0};
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(BytesValue.of(ByteString.copyFrom(secondValue))), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased bytes extension (extensions)")
        void shouldAddDifferentlyCasedBytesExtension() {
            // given
            byte[] firstValue = new byte[] {(byte) 0, (byte) 1, (byte) 2};
            byte[] secondValue = new byte[] {(byte) 2, (byte) 1, (byte) 0};
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(BytesValue.of(ByteString.copyFrom(firstValue))), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(BytesValue.of(ByteString.copyFrom(secondValue))), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @ParameterizedTest(name = "#addExtension({0})")
        @ValueSource(strings = {"94be7616-cf3b-4dc8-8e13-a30fdfd90ac1", "8bee6ec0-87d5-424e-8d4c-cfe6acd4fd11", "f33cde18-152a-44b8-aadc-040d5b736844"})
        @DisplayName("Should add UUID extension (extensions)")
        void shouldAddUuidExtension(String rawValue) {
            // given
            UUID value = UUID.fromString(rawValue);
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", value);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();

            // then
            Assertions.assertEquals(Any.pack(StringValue.of(value.toString())), ticket.getExtensionsOrDefault("a", null));
        }

        @Test
        @DisplayName("Should override UUID extension (extensions)")
        void shouldOverrideUuidExtension() {
            // given
            UUID firstValue = UUID.randomUUID();
            UUID secondValue = UUID.randomUUID();
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("a", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(StringValue.of(secondValue.toString())), extensionMap.get("a"));
        }

        @Test
        @DisplayName("Should add differently cased UUID extension (extensions)")
        void shouldAddDifferentlyCasedUuidExtension() {
            // given
            UUID firstValue = UUID.randomUUID();
            UUID secondValue = UUID.randomUUID();
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            builder.addExtension("a", firstValue);
            builder.addExtension("A", secondValue);
            TicketTemplate template = builder.build();
            Ticket ticket = template.createNewTicket();
            Map<String, Any> extensionMap = ticket.getExtensionsMap();

            // then
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(StringValue.of(firstValue.toString())), extensionMap.get("a"));
            Assertions.assertTrue(extensionMap.containsKey("a"));
            Assertions.assertEquals(Any.pack(StringValue.of(secondValue.toString())), extensionMap.get("A"));
            Assertions.assertEquals(2, ticket.getExtensionsCount());
        }

        @Test
        @DisplayName("Should create new (non-null) template")
        void shouldCreateNewTemplate() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder();

            // when
            TicketTemplate template = builder.build();

            // then
            Assertions.assertNotNull(template);
        }

        @Test
        @DisplayName("Should be equal")
        void shouldBeEqual() {
            // given
            TicketTemplateBuilder builder1 = TicketTemplate.newBuilder()
                .addExtension("a", "banana");
            TicketTemplateBuilder builder2 = TicketTemplate.newBuilder()
                .addExtension("a", "banana");

            // then
            Assertions.assertEquals(builder1, builder1);
            Assertions.assertEquals(builder2, builder2);
            Assertions.assertEquals(builder1, builder2);
            Assertions.assertEquals(builder2, builder1);
        }

        @Test
        @DisplayName("Should not be equal (searchFields)")
        void shouldNotBeEqualSearchFields() {
            // given
            TicketTemplateBuilder builder1 = TicketTemplate.newBuilder()
                .addStringArg("a", "banana");
            TicketTemplateBuilder builder2 = TicketTemplate.newBuilder()
                .addStringArg("a", "banana2");

            // then
            Assertions.assertNotEquals(builder1, builder2);
        }

        @Test
        @DisplayName("Should not be equal (extensions)")
        void shouldNotBeEqualExtensions() {
            // given
            TicketTemplateBuilder builder1 = TicketTemplate.newBuilder()
                .addExtension("a", "banana");
            TicketTemplateBuilder builder2 = TicketTemplate.newBuilder()
                .addExtension("a", "banana2");

            // then
            Assertions.assertNotEquals(builder1, builder2);
        }

        @Test
        @SuppressWarnings("java:S5785")
        @DisplayName("Should not be equal (null)")
        void shouldNotBeEqualNull() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder()
                .addExtension("a", "banana");

            // then
            //noinspection ConstantConditions,SimplifiableAssertion
            Assertions.assertFalse(builder.equals(null));
        }

        @Test
        @SuppressWarnings("java:S5785")
        @DisplayName("Should not be equal (other type)")
        void shouldNotBeEqualType() {
            // given
            TicketTemplateBuilder builder = TicketTemplate.newBuilder()
                .addExtension("a", "banana");

            // then
            //noinspection SimplifiableAssertion,EqualsBetweenInconvertibleTypes
            Assertions.assertFalse(builder.equals(""));
        }

        @Test
        @DisplayName("Should have equal HashCode")
        void shouldHaveEqualHashCode() {
            // given
            TicketTemplateBuilder builder1 = TicketTemplate.newBuilder()
                .addExtension("a", "b");
            TicketTemplateBuilder builder2 = TicketTemplate.newBuilder()
                .addExtension("a", "b");

            // then
            Assertions.assertEquals(builder1.hashCode(), builder2.hashCode());
        }

        @Test
        @DisplayName("Should not have equal HashCode")
        void shouldNotHaveEqualHashCode() {
            // given
            TicketTemplateBuilder builder1 = TicketTemplate.newBuilder()
                .addExtension("a", "a");
            TicketTemplateBuilder builder2 = TicketTemplate.newBuilder()
                .addExtension("a", "b");

            // then
            Assertions.assertNotEquals(builder1.hashCode(), builder2.hashCode());
        }

        @Test
        @DisplayName("Should generate correct toString()")
        void shouldGenerateCorrectToString() {
            // given
            String className = TicketTemplateBuilder.class.getSimpleName();
            TicketTemplateBuilder builder1 = TicketTemplate.newBuilder()
                .addExtension("a", "d");
            TicketTemplateBuilder builder2 = TicketTemplate.newBuilder()
                .addExtension("a", "b");

            // when
            String builder1String = builder1.toString();
            String builder2String = builder2.toString();

            // then
            Assertions.assertTrue(builder1String.contains(className));
            Assertions.assertTrue(builder2String.contains(className));
            Assertions.assertNotEquals(builder1String, builder2String);
        }


        @Contract(value = " -> new", pure = true)
        static byte @NotNull [] @NotNull [] testBytes() {
            return new byte[][] {
                {(byte) 2, (byte) 3, (byte) 5},
                {(byte) -2, (byte) 2, (byte) -4},
                {(byte) 127, (byte) -0, (byte) -128},
            };
        }
    }
}
