package se.kth.id2203.vsync.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.PatternExtractor;

import java.io.Serializable;

/**
 * Delivery event from the view-synchronous layer
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class VS_Deliver implements PatternExtractor<Class, KompicsEvent>, Serializable {

    public final KompicsEvent payload;
    public final PID source;
    public final long viewId;

    public VS_Deliver(KompicsEvent payload, PID source, long viewId) {
        this.payload = payload;
        this.source = source;
        this.viewId = viewId;
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
