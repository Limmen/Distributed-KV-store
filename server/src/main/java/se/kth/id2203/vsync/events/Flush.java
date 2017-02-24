package se.kth.id2203.vsync.events;

import se.kth.id2203.overlay.PID;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Flush-message that is used to ensure consistency before a new view is installed
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class Flush implements KompicsEvent, Serializable {

    public final StateTransfer latestUpdate;
    public final long viewId;
    public final long oldView;
    public final PID source;

    public Flush(StateTransfer latestUpdate, long viewId, long oldView, PID source) {
        this.latestUpdate = latestUpdate;
        this.viewId = viewId;
        this.oldView = oldView;
        this.source = source;
    }
}
