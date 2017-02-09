package se.kth.id2203.vsync.ports;

import se.kth.id2203.broadcast.beb.events.BEB_Broadcast;
import se.kth.id2203.broadcast.beb.events.BEB_Deliver;
import se.kth.id2203.gms.events.View;
import se.kth.id2203.vsync.events.Block;
import se.kth.id2203.vsync.events.BlockOk;
import se.kth.id2203.vsync.events.VSyncInit;
import se.sics.kompics.PortType;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class VSyncPort extends PortType{
    {
        request(VSyncInit.class);
        request(BEB_Broadcast.class);
        request(BlockOk.class);
        indication(BEB_Deliver.class);
        indication(View.class);
        indication(Block.class);
    }
}
