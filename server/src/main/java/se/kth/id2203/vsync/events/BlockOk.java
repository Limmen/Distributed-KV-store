package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

/**
 * Event sent by process in view to indicate to KV-layer that it now is ok to send request again
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class BlockOk implements KompicsEvent {
}
