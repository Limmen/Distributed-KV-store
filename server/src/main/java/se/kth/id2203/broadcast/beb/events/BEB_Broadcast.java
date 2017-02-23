package se.kth.id2203.broadcast.beb.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class BEB_Broadcast implements KompicsEvent {

    public final KompicsEvent payload;
    public final Set<PID> nodes;
    public final PID source;

    public BEB_Broadcast(KompicsEvent payload, Set<PID> nodes, PID source) {
        this.payload = payload;
        this.nodes = nodes;
        this.source = source;
    }
}
