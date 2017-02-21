package se.kth.id2203.kvstore.events;

import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * RouteWrapper for routing within replication-groups. Sent by backups to primary.
 *
 * @author Kim Hammar on 2017-02-16.
 */
public class RouteOperation implements KompicsEvent, Serializable {

    public final Operation operation;
    public final NetAddress client;
    public UUID id;

    public RouteOperation(Operation operation, NetAddress client) {
        this.operation = operation;
        this.client = client;
    }
    
    public void setId(UUID id){
    	this.id = id;
    }
    
    
}
