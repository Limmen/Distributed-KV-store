package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

import java.util.UUID;

/**
 * @author Kim Hammar on 2017-02-21.
 */
public class OperationComplete implements KompicsEvent {
    public final UUID id;

    public OperationComplete(UUID id) {
        this.id = id;
    }
}
