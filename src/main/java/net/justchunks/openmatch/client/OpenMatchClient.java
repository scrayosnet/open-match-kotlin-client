package net.justchunks.openmatch.client;

import com.google.common.util.concurrent.ListenableFuture;
import net.justchunks.openmatch.client.wrapper.TicketTemplate;
import openmatch.Frontend.AcknowledgeBackfillResponse;
import openmatch.Frontend.WatchAssignmentsResponse;
import openmatch.Messages.Assignment;
import openmatch.Messages.Backfill;
import openmatch.Messages.Ticket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Der {@link OpenMatchClient Open Match Client} stellt die technische Schnittstelle dar, mit der diese Plattform mit
 * Open Match kommunizieren und sich in den Matchmaking-Prozess einklinken kann. Mit diesem Client kann mit dem Frontend
 * Service innerhalb des Clusters interagiert werden um dort Tickets und Backfills zu verwalten. Die internen Services
 * (Query, Backend, Matchmaking-Function, etc.) können nicht kontaktiert werden. Für tiefergreifende Anpassungen an dem
 * Matchmaking-Prozess sollte daher eine eigene Implementation der gRPC Schnittstelle genutzt werden.
 *
 * <p>Open Match arbeitet nicht mit persistenten Ressourcen innerhalb von Kubernetes, weshalb die Änderungen direkt
 * nach der Beendigung der gRPC-Transaktion über die anderen Schnittstellen sichtbar sind. Dies gilt auch für
 * Änderungen, die außerhalb dieses Clients vorgenommen werden und daher nur passiv vom Client gelesen werden. Die
 * Änderungen werden nicht zwischengespeichert und so direkt an das Frontend übertragen.
 *
 * <p>Alle Schnittstellen werden asynchron ausgeführt und sind non-blocking. Sie bieten (falls sinnvoll) eine {@link
 * ListenableFuture Zukunft} an, für die Listener registriert werden können, die ausgeführt werden, sobald die Aktion
 * innerhalb von gRPC abgeschlossen wurde. Falls mehr als eine Rückgabe erwartet wird, so kann ein {@link Consumer
 * Callback} angegeben werden, der die eintreffenden Antworten verarbeitet.
 *
 * <p>Die Signaturen der Endpunkte des Frontends wurden teilweise geringfügig auf unsere Struktur angepasst,
 * entsprechen aber im Allgemeinen den offiziellen Schnittstellen. Der Client wird immer kompatibel zu den offiziellen
 * Empfehlungen gehalten und sollte möglichst direkt verwendet werden, da die einzelnen Schritte atomar aufgebaut sind.
 *
 * @see <a href="https://open-match.dev/site/docs/reference/api/">Open Match API Dokumentation</a>
 */
public interface OpenMatchClient extends AutoCloseable {

    //<editor-fold desc="ticket">
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<Ticket> createTicket(@NotNull final TicketTemplate template);

    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<Void> deleteTicket(@NotNull String ticketId);

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    CompletableFuture<Ticket> getTicket(@NotNull String ticketId);

    void watchAssignments(@NotNull String ticketId, @NotNull Consumer<@NotNull WatchAssignmentsResponse> callback);
    //</editor-fold>

    //<editor-fold desc="backfill">
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<Backfill> createBackfill(@NotNull final TicketTemplate template);

    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<Void> deleteBackfill(@NotNull String backfillId);

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    CompletableFuture<Backfill> getBackfill(@NotNull String backfillId);

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    CompletableFuture<Backfill> updateBackfill(@NotNull Backfill backfill);

    @NotNull
    @Contract(value = "_, _ -> new")
    CompletableFuture<AcknowledgeBackfillResponse> acknowledgeBackfill(
        @NotNull String backfillId,
        @NotNull Assignment assignment
    );
    //</editor-fold>

    //<editor-fold desc="maintenance">

    /**
     * Schließt alle mit diesem {@link OpenMatchClient Open Match Client} zusammenhängenden, offenen Ressourcen.
     * Anschließend kann dieser {@link OpenMatchClient Open Match Client} nicht mehr verwendet werden. Diese Methode ist
     * idempotent und kann daher beliebig oft aufgerufen werden, ohne dass sich das Verhalten dadurch verändert. Es wird
     * garantiert, dass nach dem Aufruf dieser Methode alle offenen Verbindungen und genutzten Ressourcen geschlossen
     * bzw. freigegeben wurden. Obwohl nicht alle Implementationen über solche Ressourcen verfügen, sollte die Methode
     * dennoch immer (zum Beispiel innerhalb eines Try-With-Resources Blocks) aufgerufen werden, um die Nutzung sauber
     * zu beenden.
     *
     * @implNote Das Schließen der offenen Verbindungen und das Freigeben der Ressourcen muss blocking passieren,
     *     damit nach dem Aufruf garantiert heruntergefahren werden kann. Der Unterschied dieser Methode zu {@link
     *     AutoCloseable#close()} ist, dass die Auslösung von Fehlern nicht gestattet wird.
     * @see AutoCloseable
     */
    @Override
    void close();
    //</editor-fold>
}
