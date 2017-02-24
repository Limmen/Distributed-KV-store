package se.kth.id2203.vsync.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Kim Hammar on 2017-02-21.
 */
public class OperationAck implements KompicsEvent, Serializable {
    public final UUID id;
    public final PID source;
    
    public OperationAck(UUID id, PID source) {
        this.id = id;
        this.source = source;
    }
}
