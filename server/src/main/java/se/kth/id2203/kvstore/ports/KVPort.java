package se.kth.id2203.kvstore.ports;

import se.kth.id2203.kvstore.events.ReplicationInit;
import se.sics.kompics.PortType;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class KVPort extends PortType {

    {
        request(ReplicationInit.class);
    }
}