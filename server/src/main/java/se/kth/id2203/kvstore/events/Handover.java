package se.kth.id2203.kvstore.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 *
 * This event is triggered by overlay service when overlay changed and some keys from the store need to be
 * handover
 *
 * @author Kim Hammar on 2017-02-25.
 */
public class Handover implements KompicsEvent, Serializable{

    public final Map<Integer, String> keyValues;
    public final UUID id;

    public Handover(Map<Integer, String> keyValues) {
        this.keyValues = keyValues;
        this.id = UUID.randomUUID();
    }

}
