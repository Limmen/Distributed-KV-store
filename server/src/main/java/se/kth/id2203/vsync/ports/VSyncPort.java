package se.kth.id2203.vsync.ports;

import se.kth.id2203.gms.events.View;
import se.kth.id2203.vsync.events.*;
import se.sics.kompics.PortType;

/**
 * @author Kim Hammar on 2017-02-08.
 */
public class VSyncPort extends PortType{
    {
        request(VSyncInit.class);
        request(VS_Broadcast.class);
        request(BlockOk.class);
        indication(VS_Deliver.class);
        indication(View.class);
        indication(Block.class);
    }
}
