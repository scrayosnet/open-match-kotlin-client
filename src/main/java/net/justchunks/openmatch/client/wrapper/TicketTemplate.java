package net.justchunks.openmatch.client.wrapper;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import openmatch.Messages.Backfill;
import openmatch.Messages.SearchFields;
import openmatch.Messages.Ticket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * .
 */
@SuppressWarnings("ClassCanBeRecord")
public final class TicketTemplate {

    //<editor-fold desc="LOCAL FIELDS">
    /** Die indexierten Metadaten, die für das {@link Ticket} oder den {@link Backfill} gesetzt werden. */
    @NotNull
    private final SearchFields searchFields;
    /** Die nicht indexierten Metadaten, die für das {@link Ticket} oder den {@link Backfill} gesetzt werden. */
    @NotNull
    private final Map<@NotNull String, @NotNull Any> extensions;
    //</editor-fold>


    //<editor-fold desc="CONSTRUCTORS">
    /**
     * .
     *
     * @param searchFields .
     * @param extensions   .
     */
    @Contract(pure = true)
    private TicketTemplate(
        @NotNull final SearchFields searchFields,
        @NotNull final Map<@NotNull String, @NotNull Any> extensions
    ) {
        // assign the already assembled fields (defensive copy)
        this.searchFields = SearchFields.newBuilder(searchFields).build();
        this.extensions = Map.copyOf(extensions);
    }
    //</editor-fold>


    //<editor-fold desc="conversion: match making entities">
    /**
     * .
     *
     * @return .
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public Ticket createNewTicket() {
        return Ticket.newBuilder()
            .setSearchFields(searchFields)
            .putAllExtensions(extensions)
            .build();
    }

    /**
     * .
     *
     * @return .
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public Backfill createNewBackfill() {
        return Backfill.newBuilder()
            .setSearchFields(searchFields)
            .putAllExtensions(extensions)
            .build();
    }
    //</editor-fold>


    //<editor-fold desc="builder factory">
    /**
     * .
     *
     * @return .
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static TicketTemplateBuilder newBuilder() {
        return new TicketTemplateBuilder();
    }

    /**
     * .
     *
     * @param preset .
     *
     * @return .
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static TicketTemplateBuilder newBuilder(@NotNull final TicketTemplate preset) {
        return new TicketTemplateBuilder(preset);
    }
    //</editor-fold>


    /**
     * .
     */
    public static final class TicketTemplateBuilder {

        //<editor-fold desc="LOCAL FIELDS">
        /** Die indexierten Metadaten, die für das {@link Ticket} oder den {@link Backfill} gesetzt werden. */
        @NotNull
        private final SearchFields.Builder searchFields;
        /** Die nicht indexierten Metadaten, die für das {@link Ticket} oder den {@link Backfill} gesetzt werden. */
        @NotNull
        private final Map<@NotNull String, @NotNull Any> extensions;
        //</editor-fold>


        //<editor-fold desc="CONSTRUCTORS">
        /**
         * Erstellt einen neuen {@link TicketTemplateBuilder Builder} mit leeren {@link SearchFields Suchfeldern} und
         * ohne erweiterte Metadaten. Mit dieser Instanz können beliebig viele {@link TicketTemplate Templates} erstellt
         * werden, die alle voneinander unabhängig sind.
         */
        @Contract(pure = true)
        private TicketTemplateBuilder() {
            // create a new builder for the search field aggregation
            this.searchFields = SearchFields.newBuilder();

            // create a new map for the extension aggregation
            this.extensions = new HashMap<>();
        }

        /**
         * Erstellt einen neuen {@link TicketTemplateBuilder Builder} mit einem {@link TicketTemplate Vorlage-Template},
         * dessen {@link SearchFields Suchfelder} und erweiterte Metadaten als Basis genutzt werden. Mit dieser Instanz
         * können beliebig viele {@link TicketTemplate Templates} erstellt werden, die alle voneinander unabhängig
         * sind.
         *
         * @param preset Das {@link TicketTemplate Vorlage-Template}, dessen Eigenschaften als Basis für den neuen
         *               {@link TicketTemplateBuilder Builder} verwendet werden.
         */
        @Contract(pure = true)
        private TicketTemplateBuilder(@NotNull final TicketTemplate preset) {
            // create a new builder for the search field aggregation
            this.searchFields = SearchFields.newBuilder(preset.searchFields);

            // create a new map for the extension aggregation
            this.extensions = new HashMap<>(preset.extensions);
        }
        //</editor-fold>


        //<editor-fold desc="search fields">
        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} ein neues String-Argument für die {@link SearchFields
         * Suchfelder} hinzu. Diese Felder werden indexiert und können genutzt werden, um innerhalb von Open-Match
         * Filterungen vorzunehmen. Die bereits gesetzten Felder können über diese Methode bei gleichem Schlüssel
         * überschrieben werden. Die Groß- und Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem
         * gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen String-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen String-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         */
        public void addStringArg(@NotNull final String key, @NotNull final String value) {
            searchFields.putStringArgs(key, value);
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} ein neues Double-Argument für die {@link SearchFields
         * Suchfelder} hinzu. Diese Felder werden indexiert und können genutzt werden, um innerhalb von Open-Match
         * Filterungen vorzunehmen. Die bereits gesetzten Felder können über diese Methode bei gleichem Schlüssel
         * überschrieben werden. Die Groß- und Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem
         * gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Double-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Double-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         */
        public void addDoubleArg(@NotNull final String key, final double value) {
            searchFields.putDoubleArgs(key, value);
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} ein neues String-Tag für die {@link SearchFields Suchfelder}
         * hinzu. Diese Felder werden indexiert und können genutzt werden, um innerhalb von Open-Match Filterungen
         * vorzunehmen. Die Groß- und Kleinschreibung wird berücksichtigt, wodurch mehrere Tags mit dem gleichen Namen
         * existieren können.
         *
         * @param tag Das Tag, das den {@link SearchFields Suchfeldern} dieses {@link TicketTemplateBuilder Builders}
         *            hinzugefügt werden soll.
         */
        public void addTag(@NotNull final String tag) {
            searchFields.addTags(tag);
        }
        //</editor-fold>

        //<editor-fold desc="extensions">
        public void addExtension(@NotNull final String key, final double value) {
            extensions.put(key, Any.pack(DoubleValue.of(value)));
        }

        public void addExtension(@NotNull final String key, final float value) {
            extensions.put(key, Any.pack(FloatValue.of(value)));
        }

        public void addExtension(@NotNull final String key, final long value) {
            extensions.put(key, Any.pack(Int64Value.of(value)));
        }

        public void addExtension(@NotNull final String key, final int value) {
            extensions.put(key, Any.pack(Int32Value.of(value)));
        }

        public void addExtension(@NotNull final String key, final boolean value) {
            extensions.put(key, Any.pack(BoolValue.of(value)));
        }

        public void addExtension(@NotNull final String key, @NotNull final String value) {
            extensions.put(key, Any.pack(StringValue.of(value)));
        }

        public void addExtension(@NotNull final String key, byte @NotNull [] value) {
            extensions.put(key, Any.pack(BytesValue.of(ByteString.copyFrom(value))));
        }

        public void addExtension(@NotNull final String key, @NotNull final UUID value) {
            extensions.put(key, Any.pack(StringValue.of(value.toString())));
        }
        //</editor-fold>

        //<editor-fold desc="creation">
        /**
         * Erstellt ein neues, unveränderbares {@link TicketTemplate Template} mit den Eigenschaften, die in diesem
         * {@link TicketTemplateBuilder Builder} konfiguriert wurden. Anschließende Änderungen an diesem {@link
         * TicketTemplateBuilder Builder} wirken sich nicht länger auf die zurückgegebene Instanz aus. Es können
         * beliebig viele, autarke Instanzen über diese Methode konstruiert werden, ohne das dieser {@link
         * TicketTemplateBuilder Builder} dadurch ungültig wird.
         *
         * @return Ein neues, unveränderbares {@link TicketTemplate Template} mit den Eigenschaften, die in diesem
         *     {@link TicketTemplateBuilder Builder} konfiguriert wurden.
         */
        @NotNull
        @Contract(value = " -> new", pure = true)
        public TicketTemplate build() {
            return new TicketTemplate(searchFields.build(), extensions);
        }
        //</editor-fold>
    }
}
