package se.kth.id2203.overlay.service.events;

import se.kth.id2203.gms.events.View;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * @author Kim Hammar on 2017-02-22.
 */
public class Gossip implements KompicsEvent, Serializable {

    public final int partitionId;
    public final View view;
    public final boolean crashed;

    public Gossip(int partitionId, View view, boolean crashed) {
        this.partitionId = partitionId;
        this.view = view;
        this.crashed = crashed;
    }
}
