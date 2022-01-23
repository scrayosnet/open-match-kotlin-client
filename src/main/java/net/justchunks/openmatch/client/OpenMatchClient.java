package net.justchunks.openmatch.client;

import net.justchunks.client.base.observer.StreamConsumer;
import net.justchunks.client.base.operation.CancellableOperation;
import net.justchunks.openmatch.client.wrapper.TicketTemplate;
import openmatch.Frontend.AcknowledgeBackfillResponse;
import openmatch.Frontend.WatchAssignmentsResponse;
import openmatch.Messages.Assignment;
import openmatch.Messages.Backfill;
import openmatch.Messages.Ticket;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Optional;
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
 * CompletableFuture Zukunft} an, für die weitere Verarbeiter registriert werden können, die ausgeführt werden, sobald
 * die Aktion innerhalb von gRPC abgeschlossen wurde. Falls mehr als eine Rückgabe erwartet wird, so kann ein {@link
 * Consumer Callback} angegeben werden, der die eintreffenden Antworten verarbeitet.
 *
 * <p>Die Signaturen der Endpunkte des Frontends wurden teilweise geringfügig auf unsere Struktur angepasst,
 * entsprechen aber im Allgemeinen den offiziellen Schnittstellen. Der Client wird immer kompatibel zu den offiziellen
 * Empfehlungen gehalten und sollte möglichst direkt verwendet werden, da die einzelnen Schritte atomar aufgebaut sind.
 *
 * @see <a href="https://open-match.dev/site/docs/reference/api/">Open Match API Dokumentation</a>
 */
public interface OpenMatchClient extends AutoCloseable {

    //<editor-fold desc="ticket">

    /**
     * Erstellt innerhalb von Open-Match ein neues {@link Ticket} mit den Metadaten eines bestimmten {@link
     * TicketTemplate Templates}. Die {@link Ticket#getId() ID} und die {@link Ticket#getCreateTime() Erstellungszeit}
     * werden von Open-Match festgelegt. Die {@link Assignment Game-Server-Zuweisung} wird von dem Director festgelegt,
     * nachdem dieses {@link Ticket} einem Match zugewiesen wurde. Der Status der {@link Assignment Zuweisung} kann über
     * {@link #watchAssignments(String, StreamConsumer)} beobachtet werden.
     *
     * @param template Das {@link TicketTemplate}, dessen Metadaten für die Erstellung des {@link Ticket Tickets}
     *                 genutzt werden sollen.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft} mit dem neuen {@link Ticket}, die abgeschlossen
     *     wird, sobald das {@link Ticket} erstellt wurde oder ein Fehler dabei auftritt.
     *
     * @throws NullPointerException Falls für das {@link TicketTemplate Template} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<@NotNull Ticket> createTicket(@NotNull final TicketTemplate template);

    /**
     * Löscht ein bereits existierendes {@link Ticket} mit einer bestimmten, einzigartigen ID innerhalb von Open-Match.
     * Wurde dieses {@link Ticket} bereits einem Match zugewiesen, so hat diese Operation keine Auswirkungen. Das {@link
     * Ticket} wird jedoch augenblicklich nicht weiter für die Erstellung von Matches berücksichtigt. Falls kein solches
     * {@link Ticket} existiert, bewirkt diese Methode keine Veränderung.
     *
     * @param ticketId Die einzigartige ID des {@link Ticket Tickets}, das innerhalb von Open-Match gelöscht werden
     *                 soll.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft}, die abgeschlossen wird, sobald das {@link
     *     Ticket} gelöscht wurde oder ein Fehler dabei auftritt.
     *
     * @throws NullPointerException Falls für die ID des {@link Ticket Tickets} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<Void> deleteTicket(@NotNull String ticketId);

    /**
     * Ermittelt ein bereits existierendes {@link Ticket} mit einer bestimmten, einzigartigen ID innerhalb von
     * Open-Match und gibt es zurück. Falls kein {@link Ticket} mit dieser ID existiert, wird stattdessen ein leerer
     * {@link Optional} zurückgegeben. Das {@link Ticket} wird immer in seinem aktuell gültigen Zustand von Open Match
     * abgefragt und kann daher auch Änderungen enthalten, die nicht durch diesen Client vorgenommen wurden.
     *
     * @param ticketId Die einzigartige ID des {@link Ticket Tickets}, das von Open-Match abgefragt werden soll.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft} mit dem existierenden {@link Ticket}, die
     *     abgeschlossen wird, sobald das {@link Ticket} empfangen wurde oder ein Fehler dabei auftritt. Falls kein
     *     solches {@link Ticket} existiert wird in der {@link CompletableFuture Future} ein leerer {@link Optional}
     *     zurückgegeben.
     *
     * @throws NullPointerException Falls für die ID des {@link Ticket Tickets} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    CompletableFuture<@NotNull Optional<Ticket>> getTicket(@NotNull String ticketId);

    /**
     * Beobachtet die {@link Assignment Zuweisungen} eines einzelnen {@link Ticket Tickets} mit einer bestimmten ID und
     * registriert einen {@link StreamConsumer Consumer}, der die Änderungen an der {@link Assignment Zuweisung}
     * verarbeitet. Der Stream wird geschlossen, sobald das {@link Ticket} innerhalb von Open Match gelöscht wird und es
     * wird jedes Mal ein neues Element übermittelt, wenn sich die {@link Assignment Zuweisung} ändert.
     *
     * <p>Der {@link StreamConsumer Consumer} wird asynchron ausgelöst, aber {@link StreamConsumer#onNext(Object)} wird
     * immer nur für ein Element gleichzeitig ausgelöst. Die Implementation muss also nicht threadsicher sein.
     *
     * <p>Um den Stream zu beenden, kann entweder die zurückgegebene {@link CancellableOperation Operation} {@link
     * CancellableOperation#cancel() abgeschlossen} werden oder in der {@link StreamConsumer#onNext(Object)} Methode des
     * {@link StreamConsumer Consumers} wird {@code false} zurückgegeben. Dadurch wird der Stream sauber geschlossen und
     * an den {@link StreamConsumer Consumer} werden anschließend keine weiteren Elemente gesendet.
     *
     * <p>Da der Stream (falls er nicht vorher beendet wird) endlos weiterläuft, kann er das {@link #close()
     * Herunterfahren} des SDKs verzögern und sollte entsprechend zuvor beendet werden. Damit nicht der maximale Timeout
     * abgewartet werden muss, sollten alle Streams zuvor geschlossen werden. Sollten noch offene Streams existieren,
     * wird {@link StreamConsumer#onError(Throwable)} ausgelöst und der Stream beendet.
     *
     * @param ticketId Die einzigartige ID des {@link Ticket Tickets}, dessen Änderungen an der {@link Assignment
     *                 Zuweisung} beobachtet werden sollen.
     * @param consumer Der {@link StreamConsumer Consumer}, der die empfangenen Änderungen an der {@link Assignment
     *                 Zuweisung} des {@link Ticket Tickets} verarbeiten soll.
     *
     * @return Eine {@link CancellableOperation unterbrechbare Operation}, die {@link CancellableOperation#cancel()
     *     abgebrochen} werden kann, um damit aufzuhören weitere Nachrichten zu empfangen.
     *
     * @throws NullPointerException Falls für die ID des {@link Ticket Tickets} oder den {@link StreamConsumer Consumer}
     *                              {@code null} übergeben wird. Da die ganze Funktionsweise dieser Methode auf dem
     *                              Observer basiert, ist ein Aufruf ohne Observer nicht im Sinne dieser Methode.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_, _ -> new")
    CancellableOperation watchAssignments(
        @NotNull String ticketId,
        @NotNull StreamConsumer<@NotNull WatchAssignmentsResponse> consumer
    );
    //</editor-fold>

    //<editor-fold desc="backfill">

    /**
     * Erstellt innerhalb von Open-Match einen neuen {@link Backfill} mit den Metadaten eines bestimmten {@link
     * TicketTemplate Templates}. Die {@link Backfill#getId() ID} und die {@link Backfill#getCreateTime()
     * Erstellungszeit} werden von Open-Match festgelegt. Die {@link Assignment Game-Server-Zuweisung} wird anschließend
     * durch {@link #acknowledgeBackfill(String, Assignment)} festgelegt.
     *
     * @param template Das {@link TicketTemplate}, dessen Metadaten für die Erstellung des {@link Backfill Backfills}
     *                 genutzt werden sollen.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft} mit dem neuen {@link Backfill}, die
     *     abgeschlossen wird, sobald der {@link Backfill} erstellt wurde oder ein Fehler dabei auftritt.
     *
     * @throws NullPointerException Falls für das {@link TicketTemplate Template} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<@NotNull Backfill> createBackfill(@NotNull final TicketTemplate template);

    /**
     * Löscht einen bereits existierenden {@link Backfill} mit einer bestimmten, einzigartigen ID innerhalb von
     * Open-Match. Wurde dieser {@link Backfill} bereits einem Match zugewiesen, so hat diese Operation keine
     * Auswirkungen. Der {@link Backfill} wird jedoch augenblicklich nicht weiter für die Zuweisung berücksichtigt.
     * Falls kein solcher {@link Backfill} existiert, bewirkt diese Methode keine Veränderung.
     *
     * @param backfillId Die einzigartige ID des {@link Backfill Backfills}, der innerhalb von Open-Match gelöscht
     *                   werden soll.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft}, die abgeschlossen wird, sobald der {@link
     *     Backfill} gelöscht wurde oder ein Fehler dabei auftritt.
     *
     * @throws NullPointerException Falls für die ID des {@link Backfill Backfills} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<Void> deleteBackfill(@NotNull String backfillId);

    /**
     * Ermittelt einen bereits existierenden {@link Backfill} mit einer bestimmten, einzigartigen ID innerhalb von
     * Open-Match und gibt ihn zurück. Falls kein {@link Backfill} mit dieser ID existiert, wird stattdessen ein leerer
     * {@link Optional} zurückgegeben. Der {@link Backfill} wird immer in seinem aktuell gültigen Zustand von Open Match
     * abgefragt und kann daher auch Änderungen enthalten, die nicht durch diesen Client vorgenommen wurden.
     *
     * @param backfillId Die einzigartige ID des {@link Backfill Backfills}, der von Open-Match abgefragt werden soll.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft} mit dem existierenden {@link Backfill}, die
     *     abgeschlossen wird, sobald der {@link Backfill} empfangen wurde oder ein Fehler dabei auftritt. Falls kein
     *     solcher {@link Backfill} existiert wird in der {@link CompletableFuture Future} ein leerer {@link Optional}
     *     zurückgegeben.
     *
     * @throws NullPointerException Falls für die ID des {@link Backfill Backfills} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    CompletableFuture<@NotNull Optional<Backfill>> getBackfill(@NotNull String backfillId);

    /**
     * Aktualisiert die mit einem {@link Backfill} verbundenen Metadaten und gibt den neuen {@link Backfill} zurück. In
     * dem übergebenen {@link Backfill} muss die {@link Backfill#getId() ID} gesetzt sein. Die Metadaten aus dem Objekt
     * werden vollständig überschrieben und ersetzen damit die aktuellen Metadaten des {@link Backfill Backfills}. Die
     * {@link Backfill#getCreateTime() Erstellungszeit} wird nicht aktualisiert, die {@link Backfill#getGeneration()
     * Generation} wird inkrementiert. Dadurch werden alle {@link Ticket Tickets}, die auf diesen {@link Backfill}
     * warten zurück zum aktiven Pool gegeben und stehen somit nicht länger aus.
     *
     * @param backfill Der {@link Backfill} mit den neuen Metadaten, die für die in Open Match vorhandene Datenstruktur
     *                 übernommen werden sollen.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft} mit dem aktualisierten {@link Backfill}, die
     *     abgeschlossen wird, sobald der {@link Backfill} modifiziert wurde oder ein Fehler dabei auftritt.
     *
     * @throws NullPointerException Falls für dan {@link Backfill} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_ -> new")
    CompletableFuture<@Nullable Backfill> updateBackfill(@NotNull Backfill backfill);

    /**
     * Benachrichtigt Open Match zu der {@link Assignment Zuweisung} bzw. den Adressdaten des jeweiligen Game-Servers.
     * Dadurch wird der Prozess der Zuweisung gestartet und es werden {@link Ticket Tickets} gesucht, die diesem {@link
     * Backfill} zugewiesen werden können. Die ermittelten {@link Ticket Tickets} sind in der Rückgabe enthalten und
     * können auf den entsprechenden Server verbunden werden. Wenn der entsprechende {@link Backfill} nicht existiert,
     * wird in der {@link CompletableFuture Future} eine {@link NoSuchElementException} ausgelöst.
     *
     * @param backfillId Die einzigartige ID des {@link Backfill Backfills}, dessen {@link Assignment Zuweisung}
     *                   bestätigt werden soll.
     * @param assignment Die {@link Assignment Zuweisung} des Game-Servers, die für den {@link Backfill} bestätigt
     *                   werden soll.
     *
     * @return Eine {@link CompletableFuture vervollständigbare Zukunft} mit der {@link AcknowledgeBackfillResponse
     *     Statusmeldung}, die abgeschlossen wird, sobald der {@link Backfill} zugewiesen wurde oder ein Fehler dabei
     *     auftritt.
     *
     * @throws NullPointerException Falls für die ID oder die {@link Assignment Zuweisung} des {@link Backfill
     *                              Backfills} {@code null} übergeben wird.
     * @see <a href="https://open-match.dev/site/docs/reference/api/#frontendservice">Open Match Dokumentation</a>
     */
    @NotNull
    @Contract(value = "_, _ -> new")
    CompletableFuture<@Nullable AcknowledgeBackfillResponse> acknowledgeBackfill(
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
