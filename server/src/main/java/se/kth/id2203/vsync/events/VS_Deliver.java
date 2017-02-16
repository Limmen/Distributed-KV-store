package se.kth.id2203.vsync.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.PatternExtractor;

import java.io.Serializable;

/**
 * Delivery event from the view-synchronous layer
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class VS_Deliver implements PatternExtractor<Class, StateUpdate>, Serializable {

    public final StateUpdate payload;
    public final NetAddress source;
    public final long viewId;

    public VS_Deliver(StateUpdate payload, NetAddress source, long viewId) {
        this.payload = payload;
        this.source = source;
        this.viewId = viewId;
    }

    @Override
    public Class extractPattern() {
        return payload.getClass();
    }

    @Override
    public StateUpdate extractValue() {
        return payload;
    }
}
