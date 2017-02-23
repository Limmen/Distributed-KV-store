package se.kth.id2203.epfd.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.util.Set;

/**
 * @author Kim Hammar on 2017-02-23.
 */
public class Reconfigure implements KompicsEvent {

    public final Set<PID> nodes;

    public Reconfigure(Set<PID> nodes) {
        this.nodes = nodes;
    }
}
