package se.kth.id2203.vsync;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.gms.events.GMSInit;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.gms.ports.GMSPort;
import se.kth.id2203.vsync.events.VSyncInit;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class VSyncService extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(VSyncService.class);
    protected final Negative<VSyncPort> vSyncPort = provides(VSyncPort.class);
    protected final Positive<GMSPort> gmsPort = requires(GMSPort.class);
    protected final Positive<BEBPort> broadcastPort = requires(BEBPort.class);
    private View currentView;

    protected final Handler<VSyncInit> vSyncInitHandler = new Handler<VSyncInit>() {
        @Override
        public void handle(VSyncInit vSyncInit) {
            LOG.debug("VSyncService initialized");
            trigger(new GMSInit(ImmutableSet.copyOf(vSyncInit.nodes)), gmsPort);
        }
    };

    protected final Handler<BEB_Broadcast> broadcastHandler = new Handler<BEB_Broadcast>() {
        @Override
        public void handle(BEB_Broadcast beb_broadcast) {
            trigger(beb_broadcast, broadcastPort);
        }
    };

    protected final Handler<BEB_Deliver> deliverHandler = new Handler<BEB_Deliver>() {
        @Override
        public void handle(BEB_Deliver BEBDeliver) {

        }
    };

    protected final Handler<View> viewHandler = new Handler<View>() {
        @Override
        public void handle(View view) {
            LOG.debug("VSync received new view from GMS");
            currentView = view;
            trigger(view, vSyncPort);
        }
    };

    {
        subscribe(viewHandler, gmsPort);
        subscribe(deliverHandler, broadcastPort);
        subscribe(broadcastHandler, vSyncPort);
        subscribe(vSyncInitHandler, vSyncPort);
    }


}
