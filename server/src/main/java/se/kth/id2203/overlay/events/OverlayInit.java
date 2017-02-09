package se.kth.id2203.overlay.events;

import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.Component;
import se.sics.kompics.Init;

public class OverlayInit extends Init<VSOverlayManager> {

	public final Component kvService;

	public OverlayInit(Component kvService){
		this.kvService = kvService;
	}

}
