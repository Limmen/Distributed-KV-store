package se.kth.id2203.gms.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

/**
 * Node wants to join the group event
 *
 * @author Kim Hammar on 2017-02-22.
 */
public class GMSJoin implements KompicsEvent {

    public final PID node;

    public GMSJoin(PID node) {
        this.node = node;
    }
}
