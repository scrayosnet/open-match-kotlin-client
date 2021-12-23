package net.justchunks.openmatch.client;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.justchunks.openmatch.client.observer.CallbackStreamObserver;
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
import openmatch.Messages.Assignment;
import openmatch.Messages.Backfill;
import openmatch.Messages.SearchFields;
import openmatch.Messages.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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

    //<editor-fold desc="port">
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
    /** Der asynchrone, nebenläufige Stub für die Kommunikation mit der externen Schnittstelle von Open Match. */
    @NotNull
    private final FrontendServiceGrpc.FrontendServiceStub stub;
    /** Der auf Zukünften basierende Stub für die Kommunikation mit der externen Schnittstelle von Open Match. */
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
        this.stub = FrontendServiceGrpc.newStub(channel);
        this.futureStub = FrontendServiceGrpc.newFutureStub(channel);
    }
    //</editor-fold>


    //<editor-fold desc="implementation">
    @NotNull
    @Override
    @Contract(value = "_, _ -> new")
    public ListenableFuture<Ticket> createTicket(
        @NotNull final SearchFields searchFields,
        @NotNull final Map<@NotNull String, @NotNull Any> extensions
    ) {
        // check that there were actually search fields supplied
        Preconditions.checkNotNull(
            searchFields,
            "The supplied search fields cannot be null!"
        );

        // check that there were actually extensions supplied
        Preconditions.checkNotNull(
            extensions,
            "The supplied extensions cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.createTicket(
            CreateTicketRequest.newBuilder()
                .setTicket(
                    Ticket.newBuilder()
                        .setSearchFields(searchFields)
                        .putAllExtensions(extensions)
                        .build()
                )
                .build()
        );
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new")
    public ListenableFuture<Empty> deleteTicket(@NotNull final String ticketId) {
        // check that there was actually a ticket id supplied
        Preconditions.checkNotNull(
            ticketId,
            "The supplied ticket id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.deleteTicket(
            DeleteTicketRequest.newBuilder()
                .setTicketId(ticketId)
                .build()
        );
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new", pure = true)
    public ListenableFuture<Ticket> getTicket(@NotNull final String ticketId) {
        // check that there was actually a ticket id supplied
        Preconditions.checkNotNull(
            ticketId,
            "The supplied ticket id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.getTicket(
            GetTicketRequest.newBuilder()
                .setTicketId(ticketId)
                .build()
        );
    }

    @Override
    public void watchAssignments(
        @NotNull final String ticketId,
        @NotNull final Consumer<@NotNull WatchAssignmentsResponse> callback
    ) {
        // check that there was actually a ticket id supplied
        Preconditions.checkNotNull(
            ticketId,
            "The supplied ticket id cannot be null!"
        );

        // check that there was actually a callback supplied
        Preconditions.checkNotNull(
            callback,
            "The supplied callback cannot be null!"
        );

        // call the endpoint with the ticket id request and use the callback to handle responses
        stub.watchAssignments(
            WatchAssignmentsRequest.newBuilder()
                .setTicketId(ticketId)
                .build(),
            CallbackStreamObserver.getInstance(callback)
        );
    }

    @NotNull
    @Override
    @Contract(value = "_, _ -> new")
    public ListenableFuture<Backfill> createBackfill(
        @NotNull final SearchFields searchFields,
        @NotNull final Map<@NotNull String, @NotNull Any> extensions
    ) {
        // check that there were actually search fields supplied
        Preconditions.checkNotNull(
            searchFields,
            "The supplied search fields cannot be null!"
        );

        // check that there were actually extensions supplied
        Preconditions.checkNotNull(
            extensions,
            "The supplied extensions cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.createBackfill(
            CreateBackfillRequest.newBuilder()
                .setBackfill(
                    Backfill.newBuilder()
                        .setSearchFields(searchFields)
                        .putAllExtensions(extensions)
                        .build()
                )
                .build()
        );
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new")
    public ListenableFuture<@NotNull Empty> deleteBackfill(@NotNull final String backfillId) {
        // check that there was actually a backfill id supplied
        Preconditions.checkNotNull(
            backfillId,
            "The supplied backfill id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.deleteBackfill(
            DeleteBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .build()
        );
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new", pure = true)
    public ListenableFuture<@NotNull Backfill> getBackfill(@NotNull final String backfillId) {
        // check that there was actually a backfill id supplied
        Preconditions.checkNotNull(
            backfillId,
            "The supplied backfill id cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.getBackfill(
            GetBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .build()
        );
    }

    @NotNull
    @Override
    @Contract(value = "_ -> new", pure = true)
    public ListenableFuture<@NotNull Backfill> updateBackfill(@NotNull final Backfill backfill) {
        // check that there was actually a backfill supplied
        Preconditions.checkNotNull(
            backfill,
            "The supplied backfill cannot be null!"
        );

        // call the endpoint with a new request and relay the future of the response
        return futureStub.updateBackfill(
            UpdateBackfillRequest.newBuilder()
                .setBackfill(backfill)
                .build()
        );
    }

    @NotNull
    @Override
    @Contract(value = "_, _ -> new", pure = true)
    public ListenableFuture<@NotNull AcknowledgeBackfillResponse> acknowledgeBackfill(
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
        return futureStub.acknowledgeBackfill(
            AcknowledgeBackfillRequest.newBuilder()
                .setBackfillId(backfillId)
                .setAssignment(assignment)
                .build()
        );
    }
    //</editor-fold>

    //<editor-fold desc="internal">
    @Override
    public void close() {
        try {
            // shutdown and wait for it to complete
            channel
                .shutdown()
                .awaitTermination(SHUTDOWN_GRACE_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
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
     * Frontends. Dabei wird versucht den Host über die Umgebungsvariable {@value FRONTEND_HOST_ENV_KEY} aufzulösen. Ist
     * diese Variable nicht gesetzt, wird stattdessen eine {@link IllegalStateException} ausgelöst, da der Host so nicht
     * ermittelt werden kann.
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
        if (host != null) {
            // return the host that was found in the environment variable
            return host;
        }

        // throw an exception, because we cannot recover in this case
        throw new IllegalStateException(
            "The environment variable " + FRONTEND_HOST_ENV_KEY + " was missing. Therefore, the client cannot be "
            + "instantiated."
        );
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
}
