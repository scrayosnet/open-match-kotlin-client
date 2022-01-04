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
import org.jetbrains.annotations.Range;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein {@link TicketTemplate} ist eine Vorlage für die Erstellung von neuen {@link Ticket Tickets} und {@link Backfill
 * Backfills}. Das {@link TicketTemplate Template} enthält dabei nur die Metadaten, die für die Erstellung neuer {@link
 * Ticket Tickets} und {@link Backfill Backfills} benötigt (und berücksichtigt) werden und verwaltet daher zum Beispiel
 * nicht die ID. Ein {@link TicketTemplate} kann verwendet werden, um unlimitiert viele neue Objekte zu erstellen. Für
 * die Erstellung wird ein {@link TicketTemplateBuilder Builder} verwendet.
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
     * Erstellt ein neues {@link TicketTemplate Template} für die Generierung von {@link Ticket Tickets} und {@link
     * Backfill Backfills} mit festen, unveränderlichen Metadaten. Die Metadaten werden bei der Erstellung kopiert und
     * sind so vollständig gegenüber externen Veränderungen geschützt. Mehrere {@link TicketTemplate Templates}, die mit
     * denselben Metadaten erstellt werden, haben also dennoch unabhängige Metadaten-Referenzen.
     *
     * @param searchFields Die indexierten Metadaten, die für das {@link Ticket} oder den {@link Backfill} gesetzt
     *                     werden.
     * @param extensions   Die nicht indexierten Metadaten, die für das {@link Ticket} oder den {@link Backfill} gesetzt
     *                     werden.
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


    //<editor-fold desc="utility">
    @Override
    @Contract(value = "null -> false", pure = true)
    public boolean equals(final Object o) {
        // if they are the same reference, they must be equal
        if (this == o) {
            return true;
        }

        // if they are not of the same type, they cannot be equal
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // cast the comparison object and compare all fields individually
        final TicketTemplate template = (TicketTemplate) o;
        return searchFields.equals(template.searchFields) && extensions.equals(template.extensions);
    }

    @Override
    @Range(from = Integer.MIN_VALUE, to = Integer.MAX_VALUE)
    public int hashCode() {
        return Objects.hash(searchFields, extensions);
    }

    @NotNull
    @Override
    @Contract(value = " -> new", pure = true)
    public String toString() {
        return "TicketTemplate{"
               + "searchFields=" + searchFields
               + ", extensions=" + extensions
               + '}';
    }
    //</editor-fold>

    //<editor-fold desc="conversion: match making entities">
    /**
     * Erstellt ein neues {@link Ticket} mit den in diesem {@link TicketTemplate Template} hinterlegten Eigenschaften
     * für die Erstellung. Die ID, die Zuweisung und der Zeitstempel werden dabei nicht gesetzt und verbleiben daher auf
     * ihren Standardwerten. Alle durch diese Methode erstellten {@link Ticket Ticketss} verhalten sich vollständig
     * autark zueinander und Änderungen an ihnen wirken sich also weder auf dieses {@link TicketTemplate Template}, noch
     * auf die anderen zurückgegebenen Instanzen aus.
     *
     * @return Ein neues {@link Ticket} mit den in diesem {@link TicketTemplate Template} hinterlegten Eigenschaften für
     *     die Erstellung.
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
     * Erstellt einen neuen {@link Backfill} mit den in diesem {@link TicketTemplate Template} hinterlegten
     * Eigenschaften für die Erstellung. Die ID und der Zeitstempel werden dabei nicht gesetzt und verbleiben daher auf
     * ihren Standardwerten. Alle durch diese Methode erstellten {@link Backfill Backfills} verhalten sich vollständig
     * autark zueinander und Änderungen an ihnen wirken sich also weder auf dieses {@link TicketTemplate Template}, noch
     * auf die anderen zurückgegebenen Instanzen aus.
     *
     * @return Ein neuer {@link Backfill} mit den in diesem {@link TicketTemplate Template} hinterlegten Eigenschaften
     *     für die Erstellung.
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
     * Erstellt einen neuen {@link TicketTemplateBuilder Builder} ohne dabei Werte für die Initialisierung zu übergeben.
     * Der {@link TicketTemplateBuilder Builder} ist also leer und enthält keinerlei Informationen. Er kann beliebig
     * verändert werden und anschließend genutzt werden, um daraus ein {@link TicketTemplate Template} zu erstellen.
     *
     * @return Ein neuer {@link TicketTemplateBuilder Builder}, der mit leeren Standardwerten initialisiert wurde.
     */
    @NotNull
    @Contract(value = " -> new", pure = true)
    public static TicketTemplateBuilder newBuilder() {
        return new TicketTemplateBuilder();
    }

    /**
     * Erstellt einen neuen {@link TicketTemplateBuilder Builder} und initialisiert ihn mit den Werten eines bestimmten,
     * existierenden {@link TicketTemplate Templates}. Die Werte werden bei der Konstruktion kopiert und die Werte
     * hängen daher nach der Erstellung nicht länger voneinander ab. Er kann beliebig verändert werden und auch die so
     * gesetzten Werte nachträglich überschreiben.
     *
     * @param preset Das bereits existierende {@link TicketTemplate Template}, dessen Attribute als Ausgangswerte für
     *               den neuen {@link TicketTemplateBuilder Builder} genutzt werden sollen.
     *
     * @return Ein neuer {@link TicketTemplateBuilder Builder}, der mit dem Werten des übergebenen {@link TicketTemplate
     *     Templates} initialisiert wurde.
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static TicketTemplateBuilder newBuilder(@NotNull final TicketTemplate preset) {
        return new TicketTemplateBuilder(preset);
    }
    //</editor-fold>


    /**
     * Ein {@link TicketTemplateBuilder} erlaubt die verkettete Erstellung von {@link TicketTemplate Templates}. Ein
     * {@link TicketTemplateBuilder Builder} kann über {@link TicketTemplate#newBuilder()} bezogen werden und optional
     * (über {@link TicketTemplate#newBuilder(TicketTemplate)}) auch schon mit Standardwerten initialisiert werden. Der
     * {@link TicketTemplateBuilder Builder} kann anschließend verwendet werden um beliebig viele {@link TicketTemplate
     * Templates} zu erstellen und es können auch nach der Erstellung eines {@link TicketTemplate Templates} weiterhin
     * Änderungen vorgenommen werden, die sich aber nur auf neue {@link TicketTemplate Templates} auswirken werden.
     */
    @SuppressWarnings("UnusedReturnValue")
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
         * Suchfelder} hinzu. Diese Felder werden indexiert und können genutzt werden, um innerhalb von Open Match
         * Filterungen vorzunehmen. Die bereits gesetzten String-Felder können über diese Methode bei gleichem Schlüssel
         * überschrieben werden. Die Groß- und Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem
         * gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen String-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen String-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addStringArg(@NotNull final String key, @NotNull final String value) {
            // apply the new metadata to this builder
            searchFields.putStringArgs(key, value);

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} ein neues Double-Argument für die {@link SearchFields
         * Suchfelder} hinzu. Diese Felder werden indexiert und können genutzt werden, um innerhalb von Open Match
         * Filterungen vorzunehmen. Die bereits gesetzten Double-Felder können über diese Methode bei gleichem Schlüssel
         * überschrieben werden. Die Groß- und Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem
         * gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Double-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Double-Arguments, das den {@link SearchFields Suchfeldern} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addDoubleArg(@NotNull final String key, final double value) {
            // apply the new metadata to this builder
            searchFields.putDoubleArgs(key, value);

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} ein neues String-Tag für die {@link SearchFields Suchfelder}
         * hinzu. Diese Felder werden indexiert und können genutzt werden, um innerhalb von Open Match Filterungen
         * vorzunehmen. Die Groß- und Kleinschreibung wird berücksichtigt, wodurch mehrere Tags mit dem gleichen Namen
         * existieren können.
         *
         * @param tag Das Tag, das den {@link SearchFields Suchfeldern} dieses {@link TicketTemplateBuilder Builders}
         *            hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_ -> this", mutates = "this")
        public TicketTemplateBuilder addTag(@NotNull final String tag) {
            // apply the new metadata to this builder
            searchFields.addTags(tag);

            return this;
        }
        //</editor-fold>

        //<editor-fold desc="extensions">
        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, final double value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(DoubleValue.of(value)));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, final float value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(FloatValue.of(value)));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, final long value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(Int64Value.of(value)));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, final int value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(Int32Value.of(value)));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, final boolean value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(BoolValue.of(value)));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, @NotNull final String value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(StringValue.of(value)));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, final byte @NotNull [] value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(BytesValue.of(ByteString.copyFrom(value))));

            return this;
        }

        /**
         * Fügt dem {@link TicketTemplateBuilder Builder} einen neuen Metadaten-Satz für die {@link #extensions
         * Erweiterungen} hinzu. Diese Felder werden nicht indexiert und können daher auch nicht genutzt werden, um
         * innerhalb von Open Match Filterungen vorzunehmen. Sie stehen aber für die Matchmaking-Funktion zur Verfügung
         * und können daher trotzdem für die Zusammenstellung der Matches verwendet werden. Die bereits gesetzen
         * Erweiterungen können über diese Methode bei gleichem Schlüssel überschrieben werden. Die Groß- und
         * Kleinschreibung wird berücksichtigt, wodurch mehrere Schlüssel mit dem gleichen Namen existieren können.
         *
         * @param key   Der Schlüssel des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses
         *              {@link TicketTemplateBuilder Builders} hinzugefügt werden soll.
         * @param value Der Wert des neuen Metadaten-Satzes, der den {@link #extensions Erweiterungen} dieses {@link
         *              TicketTemplateBuilder Builders} hinzugefügt werden soll.
         *
         * @return Die Instanz dieses {@link TicketTemplateBuilder Builders}, um verkettete Aufrufe zu unterstützen.
         */
        @NotNull
        @Contract(value = "_, _ -> this", mutates = "this")
        public TicketTemplateBuilder addExtension(@NotNull final String key, @NotNull final UUID value) {
            // apply the new metadata to this builder
            extensions.put(key, Any.pack(StringValue.of(value.toString())));

            return this;
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

        //<editor-fold desc="utility">
        @Override
        @Contract(value = "null -> false", pure = true)
        public boolean equals(final Object o) {
            // if they are the same reference, they must be equal
            if (this == o) {
                return true;
            }

            // if they are not of the same type, they cannot be equal
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            // cast the comparison object and compare all fields individually
            final TicketTemplateBuilder builder = (TicketTemplateBuilder) o;
            return Objects.equals(this.searchFields.build(), builder.searchFields.build())
                   && Objects.equals(this.extensions, builder.extensions);
        }

        @Override
        @Range(from = Integer.MIN_VALUE, to = Integer.MAX_VALUE)
        public int hashCode() {
            return Objects.hash(searchFields.build(), extensions);
        }

        @NotNull
        @Override
        @Contract(value = " -> new", pure = true)
        public String toString() {
            return "TicketTemplateBuilder{"
                   + "searchFields=" + searchFields.build()
                   + ", extensions=" + extensions
                   + '}';
        }
        //</editor-fold>
    }
}
