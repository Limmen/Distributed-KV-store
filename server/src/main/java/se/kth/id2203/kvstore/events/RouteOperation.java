package se.kth.id2203.kvstore.events;

import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * @author Kim Hammar on 2017-02-16.
 */
public class RouteOperation implements KompicsEvent, Serializable {

    public final Operation operation;
    public final NetAddress client;

    public RouteOperation(Operation operation, NetAddress client) {
        this.operation = operation;
        this.client = client;
    }
}
