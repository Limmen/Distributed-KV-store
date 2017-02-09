package se.kth.id2203.broadcast.beb.ports;

import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.sics.kompics.PortType;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class BEBPort extends PortType {

    {
        request(BEB_Broadcast.class);
        indication(BEB_Deliver.class);
    }
}
