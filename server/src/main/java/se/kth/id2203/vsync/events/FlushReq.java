package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Event sent by a process that believs it should be leader in a new view, it indicates to receiver that it should
 * flush its state to the leader.
 *
 * @author Kim Hammar on 2017-02-14.
 */
public class FlushReq implements KompicsEvent, Serializable {

    public final long viewId;
    public final long oldView;

    public FlushReq(long viewId, long oldView) {
        this.viewId = viewId;
        this.oldView = oldView;
    }
}
