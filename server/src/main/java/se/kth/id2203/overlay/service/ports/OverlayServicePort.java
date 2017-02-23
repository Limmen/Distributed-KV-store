package se.kth.id2203.overlay.service.ports;

import se.kth.id2203.overlay.service.events.GlobalView;
import se.kth.id2203.overlay.service.events.VSOverlayServiceInit;
import se.sics.kompics.PortType;

/**
 * @author Kim Hammar on 2017-02-22.
 */
public class OverlayServicePort extends PortType {

    {
        indication(GlobalView.class);
        request(VSOverlayServiceInit.class);
    }
}
