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
package se.kth.id2203.simulation.scenario.lin;

import junit.framework.Assert;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.common.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Test linearizable operation semantics of the KV-Store by creating a random execution and asserting properties
 * on the trace of events.
 *
 * Starts up a cluster of servers and clients and runs a simulation where clients issues requests to the cluster.
 * The servers will log every operation-invocation event as well as every operation-response events.
 *
 * After the simulation linearizable properties are verified on the trace of events.
 *
 * Linearizability is tested with the Wing & Gong linearizability algorithm
 *
 * @author Kim Hammar
 */
public class LinTest {

    private static final int NUM_MESSAGES = 10;
    private static final int SERVERS = 3;
    private static final int CLIENTS = 3;
    private static final int REPLICATION_DEGREE = 3;
    private static final int CRASHES = 2;
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    public static void main(String[] args) {

        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.linearizeTest(SERVERS, CLIENTS, REPLICATION_DEGREE, CRASHES);
        res.put("messages", NUM_MESSAGES);
        res.put("trace", new ConcurrentLinkedQueue<>());

        /**
         * Start a cluster of SERVERS and then start CLIENTS where each client will send NUM_MESSAGES operation-requests
         * sequentially, the operations are randomized (i.e random between PUT/GET/CAS).
         */
        simpleBootScenario.simulate(LauncherComp.class);

        //Check linearizability
        ConcurrentLinkedQueue history = res.get("trace", ConcurrentLinkedQueue.class);
        Assert.assertTrue(isLinearizable(history, new HashMap<Integer, String>()));
    }

    /**
     * Wing & Gong linearizability algorithm. The algorithm tests wether the given history is linearizable
     *
     * @param history
     * @param specificationObject
     * @return
     */
    private static boolean isLinearizable(ConcurrentLinkedQueue history, HashMap<Integer, String> specificationObject) {
        if (history.isEmpty())
            return true;
        else {
            for (HashMap<String, Object> operation : getMinimalOps(history)) {
                HashMap<String, Object> res = getResponse(history, (String) operation.get("id"));
                HashMap<Integer, String> undoReference = new HashMap<>(specificationObject);
                HashMap<String, Object> res2 = applyOperation(operation, specificationObject);
                ConcurrentLinkedQueue h1 = new ConcurrentLinkedQueue(history);
                h1.remove(operation);
                h1.remove(res);
                if (res.equals(res2) && isLinearizable(h1, specificationObject)) {
                    return true;
                } else
                    undo(specificationObject, undoReference);
            }
            return false;
        }
    }

    /**
     * Apply a operation to the specification object and return the simulated response-event.
     *
     * @param operation
     * @param specificationObject
     * @return
     */
    private static HashMap<String, Object> applyOperation(HashMap<String, Object> operation, HashMap<Integer, String> specificationObject) {
        String operationCode = (String) operation.get("operationCode");
        String key = (String) operation.get("key");
        String status = "OK";
        String opValue = (String) operation.get("value");
        String id = (String) operation.get("id");
        String referenceValue = (String) operation.get("referenceValue");
        String responseValue = "";
        if (operationCode.equals("GET")) {
            responseValue = specificationObject.get(key.hashCode());
            if (responseValue == null)
                responseValue = "not found";
        }
        if (operationCode.equals("PUT")) {
            specificationObject.put(key.hashCode(), opValue);
            responseValue = "Write successful";
        }
        if (operationCode.equals("CAS")) {
            String oldValue = specificationObject.get(key.hashCode());
            if (oldValue != null && referenceValue.equals(oldValue))
                specificationObject.put(key.hashCode(), opValue);
            if (oldValue == null)
                responseValue = "not found";
            else
                responseValue = oldValue;
        }
        HashMap<String, Object> response = new HashMap<>();
        response.put("value", responseValue);
        response.put("id", id);
        response.put("status", status);
        return response;
    }

    private static void undo(HashMap<Integer, String> specificationObject, HashMap<Integer, String> undoReference) {
        specificationObject.keySet().removeAll(specificationObject.keySet());
        specificationObject.putAll(undoReference);
    }

    /**
     * Get "minimal" operations.
     * "We say that an operation op is minimal in a given history if no return event of another operation is before
     * the call of op; this means that op could be linearized first".
     *
     * @param history
     * @return
     */
    private static List<HashMap<String, Object>> getMinimalOps(ConcurrentLinkedQueue history) {
        List<HashMap<String, Object>> minimalOps = new ArrayList<>();
        Iterator iterator = history.iterator();
        while (iterator.hasNext()) {
            HashMap<String, Object> operation = (HashMap<String, Object>) iterator.next();
            if (operation.get("operationCode") == null)//OperationResponse
                return minimalOps;
            else {
                if (getResponse(history, (String) operation.get("id")) != null)
                    minimalOps.add(operation);
            }
        }
        return minimalOps;
    }


    /**
     * Get response-event for a given invocation-event in the history
     *
     * @param history
     * @param opId
     * @return response
     */
    private static HashMap<String, Object> getResponse(ConcurrentLinkedQueue history, String opId) {
        for (Object item : history) {
            HashMap<String, Object> response = (HashMap) item;
            if (response != null && response.get("operationCode") == null && opId.equals(response.get("id"))) {
                return response;
            }
        }
        return null;
    }
}
