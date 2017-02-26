/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.bootstrapping;

import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.events.Boot;
import se.kth.id2203.bootstrapping.events.Booted;
import se.kth.id2203.bootstrapping.events.CheckIn;
import se.kth.id2203.bootstrapping.events.Ready;
import se.kth.id2203.bootstrapping.ports.Bootstrapping;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.service.events.JoinPending;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.UUID;

/**
 * BootstrapClient, used by server that should connect to an existing cluster. Will use a type of handshake protocol
 * with BootstrapServer before initialized in the cluster and the Key-Value Store.
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class BootstrapClient extends ComponentDefinition {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BootstrapClient.class);
    //******* Ports ******
    final Negative<Bootstrapping> bootstrap = provides(Bootstrapping.class);
    final Positive<Timer> timer = requires(Timer.class);
    final Positive<Network> net = requires(Network.class);
    //******* Fields ******
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final NetAddress server = config().getValue("id2203.project.bootstrap-address", NetAddress.class);
    private State state = State.WAITING;
    private UUID timeoutId;


    /**
     * Startup, start timer for periodically sending check-in messages to bootstrap-server until asked to boot.
     */
    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            LOG.debug("Starting bootstrap client on {}", self);
            long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new BSTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };
    /**
     * CheckIn at server or notify server that we have already booted successfully
     */
    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {

        @Override
        public void handle(BSTimeout e) {
            if (state == State.WAITING) {
                trigger(new Message(self, server, new CheckIn(self)), net);
            } else if (state == State.STARTED) {
                trigger(new Message(self, server, Ready.event), net);
                suicide();
            } else if (state == State.PENDING){
                LOG.debug("My join request is pending.. awaiting enough nodes to create new partition");
            }
        }
    };

    /**
     * BoostrapServer gives a initial partition assignment and asks us to boot.
     */
    protected final ClassMatchedHandler<Boot, Message> bootHandler = new ClassMatchedHandler<Boot, Message>() {

        @Override
        public void handle(Boot content, Message context) {
            if (state == State.WAITING || state == State.PENDING) {
                LOG.info("{} Booting up.", self);
                trigger(new Booted(content.assignment, content.keyValues), bootstrap);
                //trigger(new CancelPeriodicTimeout(timeoutId), timer);
                trigger(new Message(self, server, Ready.event), net);
                state = State.STARTED;
            }
        }
    };

    /**
     * Join is pending, lacking enough members to create new partition, all existing partitions are full.
     */
    protected final ClassMatchedHandler<JoinPending, Message> pendingHandler = new ClassMatchedHandler<JoinPending, Message>() {

        @Override
        public void handle(JoinPending content, Message context) {
            state = State.PENDING;
        }
    };

    /**
     * Bootstrap complete
     */
    @Override
    public void tearDown() {
        trigger(new CancelPeriodicTimeout(timeoutId), timer);
        LOG.debug("Bootstrap complete");
    }

    /**
     * Kompics "instance initializer", subscribe handlers to ports.
     */ {
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(bootHandler, net);
        subscribe(pendingHandler, net);
    }

    public static enum State {

        WAITING, PENDING, STARTED;
    }
}

