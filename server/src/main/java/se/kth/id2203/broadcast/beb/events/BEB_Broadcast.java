package se.kth.id2203.broadcast.beb.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class BEB_Broadcast implements KompicsEvent {

    public final KompicsEvent message;
    public final Set<NetAddress> nodes;

    public BEB_Broadcast(KompicsEvent message, Set<NetAddress> nodes) {
        this.message = message;
        this.nodes = nodes;
    }
}
