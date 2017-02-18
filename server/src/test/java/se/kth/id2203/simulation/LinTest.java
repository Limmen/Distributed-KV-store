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
package se.kth.id2203.simulation;

import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Test linearizable operation semantics of the KV-Store by creating a random execution and asserting properties
 * on the trace of events.
 *
 *  Starts up a cluster of servers and clients and runs a simulation where clients issues requests to the cluster.
 * The servers will log every operation-invocation event as well as every operation-response events.
 *
 * After the simulation linearizable properties are verified on the trace of events.
 *
 * TODO: Design the assertions according to the definition of linearizability.
 *
 * @author Kim Hammar
 */
public class LinTest {
    
    private static final int NUM_MESSAGES = 10;
    private static final int SERVERS = 3;
    private static final int CLIENTS = 3;
    private static final int REPLICATION_DEGREE = 3;
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    public static void main(String[] args) {
        
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.linearizeTest(SERVERS, CLIENTS, REPLICATION_DEGREE);
        res.put("messages", NUM_MESSAGES);
        res.put("trace", new ConcurrentLinkedQueue<>());
        simpleBootScenario.simulate(LauncherComp.class);
        System.out.println("-------------------------------------------------------");
        System.out.println("Trace of the execution (sequence of observable events):");
        for(Object object : res.get("trace", ConcurrentLinkedQueue.class)){
            System.out.println(object.toString());
        }
        System.out.println("-------------------------------------------------------");
    }

}
