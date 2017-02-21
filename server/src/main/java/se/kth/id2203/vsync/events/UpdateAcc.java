package se.kth.id2203.vsync.events;

import java.io.Serializable;
import java.util.UUID;

import se.sics.kompics.KompicsEvent;

/**
 * @author Kim Hammar on 2017-02-21.
 */
public class UpdateAcc implements KompicsEvent, Serializable {
    public final UUID id;
    
    public UpdateAcc(UUID id) {
        this.id = id;
    }
}
