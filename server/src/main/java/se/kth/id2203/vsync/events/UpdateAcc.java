package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

/**
 * @author Kim Hammar on 2017-02-21.
 */
public class UpdateAcc implements KompicsEvent {
    public final long timestamp;

    public UpdateAcc(long timestamp) {
        this.timestamp = timestamp;
    }
}
