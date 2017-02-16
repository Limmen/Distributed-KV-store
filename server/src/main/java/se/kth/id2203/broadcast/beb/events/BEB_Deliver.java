package se.kth.id2203.broadcast.beb.events;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.PatternExtractor;

import java.io.Serializable;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class BEB_Deliver implements PatternExtractor<Class, KompicsEvent>, Serializable {

    public final KompicsEvent payload;
    public final NetAddress source;

    public BEB_Deliver(KompicsEvent payload, NetAddress source) {
        this.payload = payload;
        this.source = source;
    }

    @Override
    public Class extractPattern() {
        return payload.getClass();
    }

    @Override
    public KompicsEvent extractValue() {
        return payload;
    }
}
