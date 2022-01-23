package net.justchunks.openmatch.client;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.Channel;
import io.grpc.Context;
import io.grpc.Context.CancellableContext;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import net.justchunks.client.base.observer.RelayStreamObserver;
import net.justchunks.client.base.observer.StreamConsumer;
import net.justchunks.client.base.operation.CancellableOperation;
import net.justchunks.client.base.operation.ContextCancellableOperation;
import net.justchunks.openmatch.client.wrapper.TicketTemplate;
import openmatch.Frontend.AcknowledgeBackfillRequest;
import openmatch.Frontend.AcknowledgeBackfillResponse;
import openmatch.Frontend.CreateBackfillRequest;
import openmatch.Frontend.CreateTicketRequest;
import openmatch.Frontend.DeleteBackfillRequest;
import openmatch.Frontend.DeleteTicketRequest;
import openmatch.Frontend.GetBackfillRequest;
import openmatch.Frontend.GetTicketRequest;
import openmatch.Frontend.UpdateBackfillRequest;
import openmatch.Frontend.WatchAssignmentsRequest;
import openmatch.Frontend.WatchAssignmentsResponse;
import openmatch.FrontendServiceGrpc;
import openmatch.FrontendServiceGrpc.FrontendServiceFutureStub;
import openmatch.FrontendServiceGrpc.FrontendServiceStub;
import openmatch.Messages.Assignment;
import openmatch.Messages.Backfill;
import openmatch.Messages.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.futureconverter.java8guava.FutureConverter.toCompletableFuture;

/**
 * Eine {@link GrpcOpenMatchClient} stellt eine gRPC-Implementation des {@link OpenMatchClient Open Match Clients} dar.
 * Die Implementation basiert auf den offiziellen Protobufs die mit Open Match veröffentlicht werden. Jede Plattform
 * benötigt nur genau eine Implementation des Open Match Clients, muss sich jedoch nicht selbst um die Auswahl der
 * jeweils besten Implementation kümmern. Stattdessen wird die Implementation durch die Fabrikmethode vorgegeben. Alle
 * Implementationen erfüllen die Spezifikation des Open Match Frontends vollständig.
 */
@SuppressWarnings({"FieldCanBeLocal", "java:S1192"})
public final class GrpcOpenMatchClient implements OpenMatchClient {

    //<editor-fold desc="LOGGER">
    /** Der Logger, der für das Senden der Fehlermeldungen  in dieser Klasse verwendet werden soll. */
    @NotNull
    private static final Logger LOG = LogManager.getLogger(GrpcOpenMatchClient.class);
    //</editor-fold>


    //<editor-fold desc="CONSTANTS">

    //<editor-fold desc="host">
    /** Der Host, über den die Kommunikation mit dem Open Match Frontend über gRPC standardmäßig stattfindet. */
    @NotNull
    private static final String DEFAULT_FRONTEND_HOST = "localhost";
    /** Der Schlüssel der Umgebungsvariable, aus der der Host für das Open Match Frontend ausgelesen werden kann. */
    @NotNull
    private static final String FRONTEND_HOST_ENV_KEY = "OPEN_MATCH_FRONTEND_GRPC_HOST";
    //</editor-fold>

    //<editor-fold desc="port">
    /** Der Port, über den die Kommunikation mit dem Open Match Frontend über gRPC standardmäßig stattfindet. */
    @Range(from = 0, to = 65_535)
    private static final int DEFAULT_FRONTEND_PORT = 50504;
    /** Der Schlüssel der Umgebungsvariable, aus der der Port für das Open Match Frontend ausgelesen werden kann. */
    @NotNull
    private static final String FRONTEND_PORT_ENV_KEY = "OPEN_MATCH_FRONTEND_GRPC_PORT";
    //</editor-fold>

    //<editor-fold desc="shutdown">
    /** Die {@link java.time.Duration Dauer}, die beim Herunterfahren maximal gewartet werden soll. */
    @NotNull
    private static final java.time.Duration SHUTDOWN_GRACE_PERIOD = java.time.Duration.ofSeconds(5);
    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="LOCAL FIELDS">

    //<editor-fold desc="runtime">
    /** Der {@link ManagedChannel Channel}, über den die Kommunikation mit der externen Schnittstelle abläuft. */
    @NotNull
    private final ManagedChannel channel;
    //</editor-fold>

    //<editor-fold desc="stubs">
    /**
     * Der asynchrone, nebenläufige {@link FrontendServiceStub Stub} für die Kommunikation mit der externen
     * Schnittstelle von Open Match, der über {@link StreamObserver Stream-Observer} kontrolliert wird.
     */
    @NotNull
    private final FrontendServiceStub asyncStub;
    /**
     * Der asynchrone, nebenläufige {@link FrontendServiceFutureStub Stub} für die Kommunikation mit der externen
     * Schnittstelle von Open Match, der über {@link ListenableFuture Futures} kontrolliert wird.
     */
    @NotNull
    private final FrontendServiceFutureStub futureStub;
    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="CONSTRUCTORS">
    /**
     * Erstellt eine neue Instanz der gRPC-Implementation des Open Match Clients. Dafür wird der entsprechende {@link
     * Channel Netzwerk-Channel} dynamisch zusammengebaut. Host und Port werden (falls möglich) über die
     * Umgebungsvariablen bezogen. Dabei werden für den {@link Channel} die zugehörigen Stubs für asynchrone und auf
     * Zukünften basierende Kommunikation mit der Schnittstelle instantiiert. Durch die Erstellung dieser Instanz wird
     * noch keine Aktion unternommen und entsprechend auch nicht die Kommunikation mit der externen Schnittstelle
     * aufgenommen.
     *
     * @param executorService Der {@link ScheduledExecutorService Executor-Service}, der für das Ausführen der Callbacks
     *                        verwendet werden soll.
     */
    @Contract(pure = true)
    GrpcOpenMatchClient(@NotNull final ScheduledExecutorService executorService) {
        // redirect to the other constructor with automatic host and port resolution
        this(executorService, getAutomaticHost(), getAutomaticPort());
    }

    /**
     * Erstellt eine neue Instanz der gRPC-Implementation des Open Match Clients. Dafür wird der entsprechende {@link
     * Channel Netzwerk-Channel} mit expliziten Werten zusammengebaut. Host und Port werden direkt übergeben und
     * unverändert für die Erstellung des {@link Channel Channels} genutzt. Dabei werden für den {@link Channel} die
     * zugehörigen Stubs für asynchrone und auf Zukünften basierende Kommunikation mit der Schnittstelle instantiiert.
     * Durch die Erstellung dieser Instanz wird noch keine Aktion unternommen und entsprechend auch nicht die
     * Kommunikation mit der externen Schnittstelle aufgenommen.
     *
     * @param executorService Der {@link ScheduledExecutorService Executor-Service}, der für das Ausführen der Callbacks
     *                        verwendet werden soll.
     * @param host            Der Host, unter dem der gRPC Server erreichbar ist und zu dem die Verbindung entsprechend
     *                        aufgenommen werden soll.
     * @param port            Der Port, unter dem der gRPC Server erreichbar ist und zu dem die Verbindung entsprechend
     *                        aufgenommen werden soll.
     */
    @Contract(pure = true)
    GrpcOpenMatchClient(
        @NotNull final ScheduledExecutorService executorService,
        @NotNull final String host,
        @Range(from = 0, to = 65_535) final int port
    ) {
        // assemble the address components and create the corresponding channel (client communication does not use TLS)
        this.channel = ManagedChannelBuilder
            .forAddress(host, port)
            .executor(executorService)
            .offloadExecutor(executorService)
            .usePlaintext()
            .build();

        // create the async and  stubs for the communication with open match
        this.asyncStub = FrontendServiceGrpc.newStub(channel);
        this.futureStub = FrontendServiceGrpc.newFutureStub(channel);
    }
    //</editor-fold>


    //<editor-fold desc="implementation">
    @NotNull
    @Override
    @Contract(value = "_ -> new")
    public CompletableFuture<@NotNull Ticket> createTicket(@NotNull final TicketTemplate template) {
        // check that there were actually search fields supplied
        Preconditions.checkNotNull(
            template,
            "The supplied template cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.createTicket(
            CreateTicketRequest.newBuilder()
                .setTicket(template.createNewTicket())
                .build()
        ));
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new")
    public CompletableFuture<Void> deleteTicket(@NotNull final String ticketId) {
        // check that there was actually a ticket id supplied
        Preconditions.checkNotNull(
            ticketId,
            "The supplied ticket id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.deleteTicket(
            DeleteTicketRequest.newBuilder()
                .setTicketId(ticketId)
                .build()
        )).thenApply(empty -> null);
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new", pure = true)
    public CompletableFuture<@NotNull Optional<Ticket>> getTicket(@NotNull final String ticketId) {
        // check that there was actually a ticket id supplied
        Preconditions.checkNotNull(
            ticketId,
            "The supplied ticket id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.getTicket(
            GetTicketRequest.newBuilder()
                .setTicketId(ticketId)
                .build()
        )).handle(this::handleGetNotFound);
    }

    @NotNull
    @Override
    @Contract(value = "_, _ -> new")
    public CancellableOperation watchAssignments(
        @NotNull final String ticketId,
        @NotNull final StreamConsumer<@NotNull WatchAssignmentsResponse> consumer
    ) {
        // check that there was actually a ticket id supplied
        Preconditions.checkNotNull(
            ticketId,
            "The supplied ticket id cannot be null!"
        );

        // check that there was actually a consumer supplied
        Preconditions.checkNotNull(
            consumer,
            "The supplied consumer cannot be null!"
        );

        // create a new context object that can be used to terminate the stream
        final CancellableContext context = Context.current().withCancellation();

        // call the endpoint with the ticket id request and use the callback to handle responses
        context.run(() -> asyncStub.watchAssignments(
            WatchAssignmentsRequest.newBuilder()
                .setTicketId(ticketId)
                .build(),
            RelayStreamObserver.getInstance(consumer, context)
        ));

        // return the wrapped cancellable context
        return new ContextCancellableOperation(context);
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new")
    public CompletableFuture<@NotNull Backfill> createBackfill(@NotNull final TicketTemplate template) {
        // check that there were actually search fields supplied
        Preconditions.checkNotNull(
            template,
            "The supplied template cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.createBackfill(
            CreateBackfillRequest.newBuilder()
                .setBackfill(template.createNewBackfill())
                .build()
        ));
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new")
    public CompletableFuture<Void> deleteBackfill(@NotNull final String backfillId) {
        // check that there was actually a backfill id supplied
        Preconditions.checkNotNull(
            backfillId,
            "The supplied backfill id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.deleteBackfill(
            DeleteBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .build()
        )).thenApply(empty -> null);
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new", pure = true)
    public CompletableFuture<@NotNull Optional<Backfill>> getBackfill(@NotNull final String backfillId) {
        // check that there was actually a backfill id supplied
        Preconditions.checkNotNull(
            backfillId,
            "The supplied backfill id cannot be null!"
        );


        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.getBackfill(
            GetBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .build()
        )).handle(this::handleGetNotFound);
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new", pure = true)
    public CompletableFuture<@NotNull Backfill> updateBackfill(@NotNull final Backfill backfill) {
        // check that there was actually a backfill supplied
        Preconditions.checkNotNull(
            backfill,
            "The supplied backfill cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.updateBackfill(
            UpdateBackfillRequest.newBuilder()
                .setBackfill(backfill)
                .build()
        )).handle(this::handleBackfillReferenceNotFound);
    }

    @NotNull
    @Override
    @Contract(value = "_, _ -> new", pure = true)
    public CompletableFuture<@NotNull AcknowledgeBackfillResponse> acknowledgeBackfill(
        @NotNull final String backfillId,
        @NotNull final Assignment assignment
    ) {
        // check that there was actually a backfill id supplied
        Preconditions.checkNotNull(
            backfillId,
            "The supplied backfill id cannot be null!"
        );

        // check that there was actually an assignment supplied
        Preconditions.checkNotNull(
            assignment,
            "The supplied assignment cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return toCompletableFuture(futureStub.acknowledgeBackfill(
            AcknowledgeBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .setAssignment(assignment)
                .build()
        )).handle(this::handleBackfillReferenceNotFound);
    }
    //</editor-fold>

    //<editor-fold desc="internal">
    @Override
    public void close() {
        try {
            // shutdown and wait for it to complete
            final boolean finishedShutdown = channel
                .shutdown()
                .awaitTermination(SHUTDOWN_GRACE_PERIOD.toMillis(), TimeUnit.MILLISECONDS);

            // force shutdown if it did not terminate
            if (!finishedShutdown) {
                channel.shutdownNow();
            }
        } catch (final InterruptedException ex) {
            // log so we know the origin/reason for this interruption
            LOG.debug("Thread was interrupted while waiting for the shutdown of a GrpcOpenMatchClient.", ex);

            // set interrupted status of this thread
            Thread.currentThread().interrupt();
        }
    }
    //</editor-fold>

    //<editor-fold desc="utility: connection resolution">
    /**
     * Ermittelt automatisch den Host für die Verbindung zum gRPC-Server der externen Schnittstelle des Open Match
     * Frontends. Dabei wird zunächst versucht den Host über die Umgebungsvariable {@value FRONTEND_HOST_ENV_KEY}
     * aufzulösen. Ist diese Variable nicht gesetzt, wird zum Standard-Host für die gRPC-Schnittstelle im Open Match
     * Frontend ({@value DEFAULT_FRONTEND_HOST}) zurückgefallen.
     *
     * @return Der automatisch ermittelte Host für die Verbindung zum gRPC-Server der externen Schnittstelle des Open
     *     Match Frontends.
     */
    @NotNull
    @VisibleForTesting
    @Contract(pure = true)
    static String getAutomaticHost() {
        // read the environment variable for the dynamic frontend host
        final String host = System.getenv(FRONTEND_HOST_ENV_KEY);

        // check that there was any value and that it is valid
        return Objects.requireNonNullElse(host, DEFAULT_FRONTEND_HOST);
    }

    /**
     * Ermittelt automatisch den Port für die Verbindung zum gRPC-Server der externen Schnittstelle des Open Match
     * Frontends. Dabei wird zunächst versucht den Port über die Umgebungsvariable {@value FRONTEND_PORT_ENV_KEY}
     * aufzulösen. Ist diese Variable nicht gesetzt, wird zum Standard-Port für die gRPC-Schnittstelle im Open Match
     * Frontend ({@value DEFAULT_FRONTEND_PORT}) zurückgefallen.
     *
     * @return Der automatisch ermittelte Port für die Verbindung zum gRPC-Server der externen Schnittstelle des Open
     *     Match Frontends.
     */
    @VisibleForTesting
    @Contract(pure = true)
    @Range(from = 0, to = 65_535)
    static int getAutomaticPort() {
        // read the environment variable for the dynamic frontend port
        final String textPort = System.getenv(FRONTEND_PORT_ENV_KEY);

        // check that there was any value and that it is valid
        if (textPort == null) {
            // fall back to the default port as it could not be found
            return DEFAULT_FRONTEND_PORT;
        } else {
            // parse the number from the textual environment variable value
            try {
                return Integer.parseInt(textPort);
            } catch (final NumberFormatException ex) {
                throw new IllegalArgumentException(
                    "The supplied environment variable for the port did not contain a valid number."
                );
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="utility: conversion">
    /**
     * Verarbeitet die Rückgabe einer {@link CompletableFuture Future} und konvertiert dabei den Fehler {@link
     * Code#NOT_FOUND} in einen leeren {@link Optional}. Andere Fehler werden einfach wieder erneut geworfen und so
     * weitergegeben. Durch die Konvertierung des Fehlers können wir leichter mit nicht vorhandenen {@link E Elementen}
     * umgehen und müssen nicht jedes Mal eine komplette Fehlerbehandlung durchführen.
     *
     * @param result    Das vorliegende {@link E Element}, falls bei der Abfrage kein Fehler aufgetreten ist.
     * @param exception Der vorliegende {@link Throwable Fehler}, falls bei der Abfrage ein Fehler aufgetreten ist.
     * @param <E>       Der generische Typ des {@link E Elements}, das zurückgegeben werden soll, falls es gefunden
     *                  werden konnte.
     *
     * @return Das ermittelte {@link E Element} oder ein leerer {@link Optional}, falls innerhalb von Open Match kein
     *     Element mit dieser ID gefunden werden konnte.
     */
    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    private <E> Optional<E> handleGetNotFound(@Nullable final E result, @Nullable final Throwable exception) {
        if (exception != null) {
            // get the status from the triggered exception
            final Status state = Status.fromThrowable(exception);

            // if the backfill is not found, convert the value
            if (state.getCode().equals(Code.NOT_FOUND)) {
                return Optional.empty();
            }

            // in any other case, rethrow the original exception
            throw new CompletionException(exception);
        }

        // convert the result if there was no exception
        return Optional.ofNullable(result);
    }

    /**
     * Verarbeitet die Rückgabe einer {@link CompletableFuture Future} und konvertiert dabei den Fehler {@link
     * Code#NOT_FOUND} in eine {@link NoSuchElementException}. Andere Fehler werden einfach wieder erneut geworfen und
     * so weitergegeben. Durch die Konvertierung des Fehlers können wir diesen Zustand in den nutzenden Plattformen
     * besser erwarten und müssen dort nicht die gRPC-Abhängigkeit einfügen.
     *
     * @param result    Das vorliegende {@link E Element}, falls bei der Abfrage kein Fehler aufgetreten ist.
     * @param exception Der vorliegende {@link Throwable Fehler}, falls bei der Abfrage ein Fehler aufgetreten ist.
     * @param <E>       Der generische Typ des {@link E Elements}, das zurückgegeben werden soll, falls es gefunden
     *                  werden konnte.
     *
     * @return Das ermittelte {@link E Element}. Falls kein Element gefunden werden konnte, wird stattdessen eine {@link
     *     NoSuchElementException} ausgelöst.
     */
    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    private <E> E handleBackfillReferenceNotFound(@Nullable final E result, @Nullable final Throwable exception) {
        if (exception != null) {
            // get the status from the triggered exception
            final Status state = Status.fromThrowable(exception);

            // if the backfill is not found, convert the value
            if (state.getCode().equals(Code.NOT_FOUND)) {
                throw new NoSuchElementException("No backfill with was found for the supplied id!");
            }

            // in any other case, rethrow the original exception
            throw new CompletionException(exception);
        }

        // we can assert that it is set, if there was no exception
        assert result != null : "There was no exception, so the value must be present!";

        // return the result as-is if there was no exception
        return result;
    }
    //</editor-fold>
}
