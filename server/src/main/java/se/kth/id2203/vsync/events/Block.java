package se.kth.id2203.vsync.events;

import se.sics.kompics.KompicsEvent;

/**
 * Event sent by process in view that has a pending view-change to KV-layer to indicate that it should block
 * requests for a while.
 *
 * @author Kim Hammar on 2017-02-08.
 */
public class Block implements KompicsEvent {
}
