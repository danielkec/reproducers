
package bare;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.SubmissionPublisher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import io.helidon.common.reactive.Multi;
import io.helidon.messaging.connectors.jms.JmsMessage;
import io.helidon.microprofile.server.Server;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.Utilities;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;

@Path("/frank")
@ApplicationScoped
public class FrankResource {

    SubmissionPublisher<String> emitter = new SubmissionPublisher<>();
    private SseBroadcaster sseBroadcaster;

    @Incoming("from-wls")
    public CompletionStage<Void> receive(JmsMessage<String> msg) {
        if (sseBroadcaster == null) {
            System.out.println("No SSE client subscribed yet: " + msg.getPayload());
            return CompletableFuture.completedStage(null);
        }
        sseBroadcaster.broadcast(new OutboundEvent.Builder().data(msg.getPayload()).build());
        return CompletableFuture.completedStage(null);
    }

    @Outgoing("to-wls")
    public Publisher<Message<String>> registerPublisher() {
        return FlowAdapters.toPublisher(Multi.create(emitter).map(s -> JmsMessage.builder(s)
                //.property("JMS_BEA_DeliveryTime", 60000)
                .build()
        ));
    }

    @POST
    @Path("/send/{msg}")
    public void send(@PathParam("msg") String msg) {
        emitter.submit(msg);
    }

    @GET
    @Path("sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void listenToEvents(@Context SseEventSink eventSink, @Context Sse sse) {
        if (sseBroadcaster == null) {
            sseBroadcaster = sse.newBroadcaster();
        }
        sseBroadcaster.register(eventSink);
    }

    public static void main(String[] args) {

        Server.create().start();
    }
}
