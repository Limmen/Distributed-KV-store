package se.kth.id2203.broadcast.beb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class BEB extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(VSOverlayManager.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Negative<BEBPort> broadcastPort = provides(BEBPort.class);
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    protected final Handler<BEB_Broadcast> broadcastHandler = new Handler<BEB_Broadcast>() {
        @Override
        public void handle(BEB_Broadcast BEBBroadcast) {
            LOG.debug("Broadcasted {} to {} nodes", BEBBroadcast.message, BEBBroadcast.nodes.size());
            for (NetAddress recipient : BEBBroadcast.nodes) {
                BEB_Deliver BEBDeliver = new BEB_Deliver(BEBBroadcast.message, self);
                trigger(new Message(self, recipient, BEBDeliver), net);
            }
        }
    };

    protected final ClassMatchedHandler<BEB_Deliver, Message> deliverHandler = new ClassMatchedHandler<BEB_Deliver, Message>() {
        @Override
        public void handle(BEB_Deliver BEBDeliver, Message message) {
            LOG.debug("Received broadcast from {}", message.getSource());
            trigger(BEBDeliver.message, broadcastPort);
        }
    };


    {
        subscribe(broadcastHandler, broadcastPort);
    }

}
