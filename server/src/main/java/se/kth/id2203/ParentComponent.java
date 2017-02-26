package se.kth.id2203;

import com.google.common.base.Optional;
import se.kth.id2203.bootstrapping.BootstrapClient;
import se.kth.id2203.bootstrapping.BootstrapServer;
import se.kth.id2203.bootstrapping.ports.Bootstrapping;
import se.kth.id2203.broadcast.beb.BEB;
import se.kth.id2203.broadcast.beb.ports.BEBPort;
import se.kth.id2203.epfd.EPFD;
import se.kth.id2203.epfd.ports.EPFDPort;
import se.kth.id2203.gms.GMS;
import se.kth.id2203.gms.ports.GMSPort;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.kvstore.ports.KVPort;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.omega.Omega;
import se.kth.id2203.omega.ports.OmegaPort;
import se.kth.id2203.overlay.manager.VSOverlayManager;
import se.kth.id2203.overlay.manager.ports.Routing;
import se.kth.id2203.overlay.service.VSOverlayService;
import se.kth.id2203.overlay.service.ports.OverlayServicePort;
import se.kth.id2203.vsync.VSyncService;
import se.kth.id2203.vsync.ports.VSyncPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * Parent component initializes subcomponents and connects them. Subcomponents are VSOverlayManager, KVService and
 * BootstrapClient or BootstrapServer
 */
public class ParentComponent extends ComponentDefinition {

    /* Ports */
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    /* Children */
    protected final Component kv = create(KVService.class, Init.NONE);
    protected final Component overlay = create(VSOverlayManager.class, Init.NONE);
    protected final Component boot;
    protected final Component epfd = create(EPFD.class, Init.NONE);
    protected final Component omega = create(Omega.class, Init.NONE);
    protected final Component beb = create(BEB.class, Init.NONE);
    protected final Component gms = create(GMS.class, Init.NONE);
    protected final Component vSync = create(VSyncService.class, Init.NONE);
    protected final Component epfd2 = create(EPFD.class, Init.NONE);
    protected final Component overlayService = create(VSOverlayService.class, Init.NONE);


    {
        Optional<NetAddress> serverO = config().readValue("id2203.project.bootstrap-address", NetAddress.class);
        if (serverO.isPresent()) { // start in client mode
            boot = create(BootstrapClient.class, Init.NONE);
        } else { // start in server mode
            boot = create(BootstrapServer.class, Init.NONE);
        }
        connect(timer, boot.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, boot.getNegative(Network.class), Channel.TWO_WAY);
        //Overlay
        connect(boot.getPositive(Bootstrapping.class), overlay.getNegative(Bootstrapping.class), Channel.TWO_WAY);
        connect(net, overlay.getNegative(Network.class), Channel.TWO_WAY);
        connect(kv.provided(KVPort.class), overlay.required(KVPort.class), Channel.TWO_WAY);
        connect(overlayService.provided(OverlayServicePort.class), overlay.required(OverlayServicePort.class), Channel.TWO_WAY);
        //OverlayService
        connect(net, overlayService.required(Network.class), Channel.TWO_WAY);
        connect(timer, overlayService.required(Timer.class), Channel.TWO_WAY);
        connect(vSync.provided(VSyncPort.class), overlayService.required(VSyncPort.class), Channel.TWO_WAY);
        connect(kv.provided(KVPort.class), overlayService.required(KVPort.class), Channel.TWO_WAY);
        //KV
        connect(overlay.getPositive(Routing.class), kv.getNegative(Routing.class), Channel.TWO_WAY);
        connect(net, kv.getNegative(Network.class), Channel.TWO_WAY);
        connect(timer, kv.required(Timer.class), Channel.TWO_WAY);
        connect(kv.required(VSyncPort.class), vSync.provided(VSyncPort.class), Channel.TWO_WAY);
        //EPFD
        connect(epfd.provided(EPFDPort.class), omega.required(EPFDPort.class), Channel.TWO_WAY);
        connect(net, epfd.required(Network.class), Channel.TWO_WAY);
        connect(timer, epfd.required(Timer.class), Channel.TWO_WAY);
        connect(epfd.provided(EPFDPort.class), gms.required(EPFDPort.class), Channel.TWO_WAY);
        //EPFD2
        connect(epfd2.provided(EPFDPort.class), overlayService.required(EPFDPort.class), Channel.TWO_WAY);
        connect(net, epfd2.required(Network.class), Channel.TWO_WAY);
        connect(timer, epfd2.required(Timer.class), Channel.TWO_WAY);
        //Omega
        connect(omega.provided(OmegaPort.class), gms.required(OmegaPort.class), Channel.TWO_WAY);
        connect(timer, omega.required(Timer.class), Channel.TWO_WAY);
        //BEB
        connect(beb.provided(BEBPort.class), gms.required(BEBPort.class), Channel.TWO_WAY);
        connect(beb.provided(BEBPort.class), vSync.required(BEBPort.class), Channel.TWO_WAY);
        connect(beb.provided(BEBPort.class), overlayService.required(BEBPort.class), Channel.TWO_WAY);
        connect(net, beb.required(Network.class), Channel.TWO_WAY);
        //GMS
        connect(gms.provided(GMSPort.class), vSync.required(GMSPort.class), Channel.TWO_WAY);
        connect(timer, gms.required(Timer.class), Channel.TWO_WAY);
        connect(net, gms.required(Network.class), Channel.TWO_WAY);
        //VSync
        connect(timer, vSync.required(Timer.class), Channel.TWO_WAY);
        connect(net, vSync.required(Network.class), Channel.TWO_WAY);
    }

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        System.out.println("Parent fault: " + fault.getEvent());
        return null;
    }
}
