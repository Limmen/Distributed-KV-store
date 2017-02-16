package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

/**
 * Broadcast event within the replication-group.
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class VS_Broadcast implements KompicsEvent {

    public final StateUpdate payload;
    public final long viewId;

    public VS_Broadcast(StateUpdate payload, long viewId) {
        this.payload = payload;
        this.viewId = viewId;
    }
}
