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
package se.kth.id2203.simulation.scenario.ops;

import junit.framework.Assert;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

/**
 *
 * Tests operations of the key-value store.
 * Currently tests only PUT/GET, todo: CAS operation.
 *
 * Starts a cluster of servers and 1 test client and then runs a simulation where the client will issue a set of PUT
 * requests and then after all those requests completed it will issue corresponding get requests. Responses are put in
 * result-map and then verified.
 *
 * @author Kim Hammar
 */
public class OpsTest {
    
    private static final int NUM_MESSAGES = 10;
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    public static void main(String[] args) {
        
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleOps(3);
        res.put("messages", NUM_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);
        for (int i = 0; i < NUM_MESSAGES/2; i++) {
            Assert.assertEquals("OK - Write successful", res.get("PUT-test"+i, String.class));
        }
        for (int i = 0; i < NUM_MESSAGES/2; i++) {
            Assert.assertEquals("OK - " + i, res.get("GET-test"+i, String.class));
        }
    }

}
