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
package se.kth.id2203.simulation.scenario;

import se.kth.id2203.ParentComponent;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.simulation.gv.SimulationObserver;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.Operation3;
import se.sics.kompics.simulator.adaptor.distributions.ConstantDistribution;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of test scenarios and operations
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public abstract class ScenarioGen {

    /**
     * Operation that takes 1 parameter which defines the pid of the server and starts a
     * server. The server with pid = 1 will be bootstrap-server
     */
    private static final Operation1 startServerOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.0." + self), 45678);
                        bsAdr = new NetAddress(InetAddress.getByName("192.168.0.1"), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return ParentComponent.class;
                }

                @Override
                public String toString() {
                    return "StartNode<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);
                    if (self != 1) { // don't put this at the bootstrap server, or it will act as a bootstrap client
                        config.put("id2203.project.bootstrap-address", bsAdr);
                    }
                    return config;
                }
            };
        }
    };

    /**
     * Operation to start a cluster for scenario
     */
    private static final Operation3 startScenarioServerOp = new Operation3<StartNodeEvent, Integer, Integer, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self, final Integer replicationDegree, final Integer bootThreshold) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.0." + self), 45678);
                        bsAdr = new NetAddress(InetAddress.getByName("192.168.0.1"), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return LinScenarioParentComponent.class;
                }

                @Override
                public String toString() {
                    return "StartNode<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);
                    config.put("id2203.project.replicationDegree", replicationDegree);
                    config.put("id2203.project.bootThreshold", bootThreshold);
                    if (self != 1) { // don't put this at the bootstrap server, or it will act as a bootstrap client
                        config.put("id2203.project.bootstrap-address", bsAdr);
                    }
                    return config;
                }
            };
        }
    };

    /**
     * Operation for starting a client, takes one parameter which defines the pid of the client.
     */
    private static final Operation1 startClientOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.1." + self), 45678);
                        bsAdr = new NetAddress(InetAddress.getByName("192.168.0.1"), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return SimpleOpsScenarioClient.class;
                }

                @Override
                public String toString() {
                    return "StartClient<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);
                    config.put("id2203.project.bootstrap-address", bsAdr);
                    return config;
                }
            };
        }
    };

    /**
     * Operation for starting a client, takes one parameter which defines the pid of the client.
     */
    private static final Operation1 startLinClientOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("192.168.1." + self), 45678);
                        bsAdr = new NetAddress(InetAddress.getByName("192.168.0.1"), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return LinScenarioClient.class;
                }

                @Override
                public String toString() {
                    return "StartClient<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("id2203.project.address", selfAdr);
                    config.put("id2203.project.bootstrap-address", bsAdr);
                    return config;
                }
            };
        }
    };

    /**
     * Operation for starting a observer (node to check the global state periodically during execution).
     */
    static Operation startObserverOp = new Operation<StartNodeEvent>() {
        @Override
        public StartNodeEvent generate() {
            return new StartNodeEvent() {
                NetAddress selfAdr;

                {
                    try {
                        selfAdr = new NetAddress(InetAddress.getByName("0.0.0.0"), 0);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("kvstore.simulation.checktimeout", 2000);
                    return config;
                }
                
                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return SimulationObserver.class;
                }

                @Override
                public Init getComponentInit() {
                    return new SimulationObserver.Init(3);
                }
            };
        }
    };

    /**
     * Operation for killing a node by generating a KillNodeEvent, takes pid as argument.
     */
    static Operation1 killNodeOp = new Operation1<KillNodeEvent, Integer>() {
        @Override
        public KillNodeEvent generate(final Integer self) {
            return new KillNodeEvent() {
                NetAddress selfAdr;

                {
                    try {
                    	selfAdr = new NetAddress(InetAddress.getByName("192.168.0." + self), 45678);
                    } catch (UnknownHostException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                
                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }
                
                @Override
                public String toString() {
                    return "KillNode<" + selfAdr.toString() + ">";
                }
            };
        }
    };

    /**
     * Scenario (composition of stochastic processes) for starting cluster of servers + 1 client
     *
     * @param servers number of servers
     * @return
     */
    public static SimulationScenario simpleOps(final int servers) {
        return new SimulationScenario() {
            {
                SimulationScenario.StochasticProcess startCluster = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(servers, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };

                SimulationScenario.StochasticProcess startClients = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                startCluster.start();
                startClients.startAfterTerminationOf(60000, startCluster);
                terminateAfterTerminationOf(100000, startClients);
            }
        };
    }

    /**
     * Scenario for starting a observer and a cluster of servers and then killing them with the killNode operation.
     *
     * @param servers number of servers in the cluster
     * @return
     */
    public static SimulationScenario killNodes(final int servers) {
        SimulationScenario scen = new SimulationScenario() {
            {
                
                SimulationScenario.StochasticProcess observer = new SimulationScenario.StochasticProcess() {
                    {
                        raise(1, startObserverOp);
                    }
                };

                SimulationScenario.StochasticProcess startCluster = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(servers, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                
                SimulationScenario.StochasticProcess killNode = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(servers, killNodeOp, new BasicIntSequentialDistribution((1)));
                    }  
                };

                observer.start();
                startCluster.startAfterStartOf(1000, observer);
                killNode.startAfterTerminationOf(1000, startCluster);
                terminateAfterTerminationOf(1000*10000, startCluster);
            }
        };

        return scen;
    }

    /**
     * Start test cluster of servers and cluster of clients. Clients generate random sequence of invocation events
     * and server respond with response-events, log all events to a trace.
     *
     * @param servers number of servers
     * @return
     */
    public static SimulationScenario linearizeTest(final int servers, final int clients, final int replicationDegree) {
        return new SimulationScenario() {
            {
                SimulationScenario.StochasticProcess startCluster = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(servers, startScenarioServerOp, new BasicIntSequentialDistribution(1), new ConstantDistribution(Integer.class, replicationDegree), new ConstantDistribution(Integer.class, servers));
                    }
                };

                SimulationScenario.StochasticProcess startClients = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(clients, startLinClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                startCluster.start();
                startClients.startAfterTerminationOf(60000, startCluster);
                terminateAfterTerminationOf(100000, startClients);
            }
        };
    }
}
