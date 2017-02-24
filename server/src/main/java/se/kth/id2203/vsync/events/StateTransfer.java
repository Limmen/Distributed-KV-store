package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * StateTransfer event, basic state-transfer that is issued by leader to all replicas before a write to the shared memory
 * is completed.
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class StateTransfer implements KompicsEvent, Serializable{

    public final Map<Integer, String> keyValues;
    public final long timestamp;
    public final UUID id;

    public StateTransfer(Map<Integer, String> keyValues, long timestamp) {
        this.keyValues = keyValues;
        this.timestamp = timestamp;
        this.id = UUID.randomUUID();
    }

}
