package net.justchunks.openmatch.client.wrapper

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
import openmatch.Messages.Backfill
import openmatch.Messages.SearchFields
import openmatch.Messages.Ticket
import org.jetbrains.annotations.Contract
import java.util.Objects
import java.util.UUID

/**
 * A [TicketTemplate] is a preset for the creation of new [tickets][Ticket] and [backfills][Backfill]. The template
 * contains only the metadata, that is necessary (or relevant) for the creation of new [tickets][Ticket] and
 * [backfills][Backfill] and therefor does not manage something like the identifier. A [TicketTemplate] can be used to
 * create an unlimited amount of new objects. For the creation of a template, a corresponding builder has to be used.
 *
 * @param searchFields The indexed metadata, that will be assigned for the [Ticket] or [Backfill] on creation.
 * @param extensions The non-indexed metadata, that will be assigned for the [Ticket] or [Backfill] on creation.
 */
class TicketTemplate private constructor(
    /** The indexed metadata, that will be assigned for the [Ticket] or [Backfill] on creation. */
    private val searchFields: SearchFields,
    /** The non-indexed metadata, that will be assigned for the [Ticket] or [Backfill] on creation. */
    private val extensions: Map<String, Any>,
) {

    /**
     * Creates a new [Ticket] with the properties defined in this [TicketTemplate] for creation. The identifier,
     * assignment, and timestamp are not set and remain at their default values. All [tickets][Ticket] created by this
     * method behave completely autonomously, and changes to them do not affect this [TicketTemplate] or other returned
     * instances that may have already been created.
     *
     * @return A new [Ticket] with the properties defined in this [TicketTemplate] for creation.
     */
    fun createNewTicket(): Ticket = Ticket.newBuilder()
        .setSearchFields(searchFields)
        .putAllExtensions(extensions)
        .build()

    /**
     * Creates a new [Backfill] with the properties defined in this [TicketTemplate] for creation. The identifier and
     * timestamp are not set and remain at their default values. All [backfills][Backfill] created by this method behave
     * completely autonomously, and changes to them do not affect this [TicketTemplate] or other returned instances that
     * have already been created.
     *
     * @return A new [Backfill] with the properties defined in this [TicketTemplate] for creation.
     */
    fun createNewBackfill(): Backfill = Backfill.newBuilder()
        .setSearchFields(searchFields)
        .putAllExtensions(extensions)
        .build()

    override fun equals(other: kotlin.Any?): Boolean {
        // if they are the same reference, they must be equal
        if (this === other) {
            return true
        }

        // if they are not of the same type, they cannot be equal
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        // cast the comparison object and compare all fields individually
        val template = other as TicketTemplate
        return searchFields == template.searchFields && extensions == template.extensions
    }

    override fun hashCode(): Int = Objects.hash(searchFields, extensions)

    @Contract(value = " -> new", pure = true)
    override fun toString(): String = (
        "TicketTemplate{" +
            "searchFields=" + searchFields +
            ", extensions=" + extensions +
            '}'
        )

    /**
     * A [Builder] represents a mutable builder instance for the chained creation of [TicketTemplate] instances. An
     * instance of this builder can be obtained using the [TicketTemplate.newBuilder] method, and it can optionally be
     * initialized with default values using the overloaded function. The [builder][Builder] can then be used to create
     * any number of [TicketTemplate] instances, and even after creating a [TicketTemplate], further modifications can
     * be made that will only affect new [TicketTemplate] instances.
     */
    class Builder internal constructor(
        /** The indexed metadata, that will be assigned for the [Ticket] or [Backfill] on creation. */
        private val searchFields: SearchFields.Builder = SearchFields.newBuilder(),
        /** The non-indexed metadata, that will be assigned for the [Ticket] or [Backfill] on creation. */
        private val extensions: MutableMap<String, Any> = mutableMapOf(),
    ) {

        /**
         * Creates a new immutable [TicketTemplate] with the properties configured in this [builder][Builder].
         * Subsequent changes to this [builder][Builder] will no longer affect the returned instance. Multiple
         * independent instances can be constructed using this method without invalidating this [builder][Builder].
         *
         * @return A new immutable [TicketTemplate] with the properties configured in this [builder][Builder].
         */
        fun build(): TicketTemplate = TicketTemplate(searchFields.build(), extensions.toMap())

        /**
         * Sets a specific [key] for the string arguments in the [searchFields] of the [builder][Builder] to a specific
         * [value]. Those fields are indexed and can be used for filtering within Open Match. The already set string
         * fields can be overwritten by specifying an existing [key]. The [key] is case-sensitive, which allows for
         * multiple keys with the same name, but different casing.
         */
        fun addStringArg(key: String, value: String) = apply {
            searchFields.putStringArgs(key, value)
        }

        /**
         * Sets a specific [key] for the double arguments in the [searchFields] of the [builder][Builder] to a specific
         * [value]. Those fields are indexed and can be used for filtering within Open Match. The already set double
         * fields can be overwritten by specifying an existing [key]. The [key] is case-sensitive, which allows for
         * multiple keys with the same name, but different casing.
         */
        fun addDoubleArg(key: String, value: Double) = apply {
            searchFields.putDoubleArgs(key, value)
        }

        /**
         * Adds a new [tag] to the tags in the [searchFields] of the [builder][Builder]. Those fields are indexed and
         * can be used for filtering within Open Match. The [tag] is case-sensitive, which allows for multiple tags with
         * the same name, but different casing.
         */
        fun addTag(tag: String) = apply {
            searchFields.addTags(tag)
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: Double) = apply {
            extensions[key] = Any.pack(DoubleValue.of(value))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: Float) = apply {
            extensions[key] = Any.pack(FloatValue.of(value))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: Long) = apply {
            extensions[key] = Any.pack(Int64Value.of(value))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: Int) = apply {
            extensions[key] = Any.pack(Int32Value.of(value))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: Boolean) = apply {
            extensions[key] = Any.pack(BoolValue.of(value))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: String) = apply {
            extensions[key] = Any.pack(StringValue.of(value))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: UUID) = apply {
            extensions[key] = Any.pack(StringValue.of(value.toString()))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: ByteArray) = apply {
            extensions[key] = Any.pack(BytesValue.of(ByteString.copyFrom(value)))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: ULong) = apply {
            extensions[key] = Any.pack(UInt64Value.of(value.toLong()))
        }

        /**
         * Sets a specific [key] for the [extensions] of the [builder][Builder] to a specific [value]. Those fields are
         * not indexed and cannot be used for filtering within Open Match. However, they are available for the
         * matchmaking process and can be used for assembling matches. The already set [extensions] can be overwritten
         * by specifying an existing [key]. The [key] is case-sensitive, which allows for multiple keys with the same
         * name, but different casing.
         */
        fun addExtension(key: String, value: UInt) = apply {
            extensions[key] = Any.pack(UInt32Value.of(value.toInt()))
        }

        override fun equals(other: kotlin.Any?): Boolean {
            // if they are the same reference, they must be equal
            if (this === other) {
                return true
            }

            // if they are not of the same type, they cannot be equal
            if (other == null || javaClass != other.javaClass) {
                return false
            }

            // cast the comparison object and compare all fields individually
            val builder: Builder = other as Builder
            return searchFields.build() == builder.searchFields.build() && extensions == builder.extensions
        }

        override fun hashCode(): Int = Objects.hash(searchFields.build(), extensions)

        override fun toString(): String = (
            "TicketTemplate.Builder{" +
                "searchFields=" + searchFields.build() +
                ", extensions=" + extensions +
                '}'
            )
    }

    companion object {

        /**
         * Creates a new [TicketTemplateBuilder][Builder] without passing any values for initialization. The new
         * [builder][Builder] is empty and contains no information. It can be modified as desired and then used to create a
         * new [TicketTemplate] that can be utilized to create [tickets][Ticket] and [backfills][Backfill].
         *
         * @return A new [TicketTemplateBuilder][Builder] initialized with empty default values.
         */
        @JvmStatic
        fun newBuilder(): Builder = Builder()

        /**
         * Creates a new [TicketTemplateBuilder][Builder] and initializes it with the values of a specific existing
         * [TicketTemplate]. The values are copied during construction, and the values are no longer dependent on each
         * other after creation. It can be modified as desired and then used to create a new [TicketTemplate] that can be
         * utilized to create [tickets][Ticket] and [backfills][Backfill].
         *
         * @param preset The existing [TicketTemplate] whose attributes should be used as initial values for the new
         * [builder][Builder].
         *
         * @return A new [TicketTemplateBuilder][Builder] initialized with the values of the provided [TicketTemplate].
         */
        @JvmStatic
        fun newBuilder(preset: TicketTemplate): Builder = Builder(
            SearchFields.newBuilder(preset.searchFields),
            preset.extensions.toMutableMap(),
        )
    }
}
