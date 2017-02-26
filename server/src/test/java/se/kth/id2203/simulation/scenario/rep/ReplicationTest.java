package se.kth.id2203.simulation.scenario.rep;

import junit.framework.Assert;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.simulation.result.SimulationResultMap;
import se.kth.id2203.simulation.result.SimulationResultSingleton;
import se.kth.id2203.simulation.scenario.common.ScenarioGen;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Replication test
 *
 * Starts a cluster of servers and a set of clients and runs a simulation where clients make requests to the cluster.
 * Then verify that every node in the same replication-group has the same state afterwards (i.e replication-degree is satisfied).
 *
 * @author Kim Hammar
 */
public class ReplicationTest {

    private static final int NUM_MESSAGES = 10;
    private static final int SERVERS = 5;
    private static final int CLIENTS = 3;
    private static final int REPLICATION_DEGREE = 3;
    private static final int CRASHES = 2;
    private final static SimulationResultMap res = SimulationResultSingleton.getInstance();

    public static void main(String[] args) {

        long seed = 123;
        SimulationScenario.setSeed(seed);

        /**
         * Re-use the linearizable test scenario, start SERVERS in a cluster (a single replication-group/partition)
         * and sequential CLIENTS that will send operations to the cluster. Then verify that all replicas are in a
         * consistent state and that the state is consistent with what operations where returned to clients and in what order.
         */
        SimulationScenario simpleBootScenario = ScenarioGen.linearizeTest(SERVERS, CLIENTS, REPLICATION_DEGREE, CRASHES);
        res.put("messages", NUM_MESSAGES);
        res.put("trace", new ConcurrentLinkedQueue<>());
        simpleBootScenario.simulate(LauncherComp.class);

        /**
         * Get the latest keyvalue-stores for each replica
         */
        ArrayList<HashMap<Integer, String>> nodeStores = new ArrayList<>();
        for (int i = 1; i <= SERVERS; i++) {
            String ip = "192.168.0." + i;
            nodeStores.add(res.get(ip+"-values", HashMap.class));
        }

        /**
         * Build up the keyvalue store from the trace of operations (i.e apply all updates in order)
         */
        ConcurrentLinkedQueue trace = res.get("trace", ConcurrentLinkedQueue.class);
        HashMap<Integer, String> stateFromTrace = new HashMap<>();
        Iterator iterator = trace.iterator();
        while (iterator.hasNext()) {
            HashMap<String, Object> op = (HashMap<String, Object>) iterator.next();
            if (op.get("operationCode") != null && completed(trace, (String) op.get("id"))) {
                if (op.get("operationCode").equals(Operation.OperationCode.PUT.toString())) {
                    stateFromTrace.put(op.get("key").hashCode(), (String) op.get("value"));
                }
                if (op.get("operationCode").equals(Operation.OperationCode.CAS.toString())) {
                    if (op.get("referenceValue").equals(stateFromTrace.get(op.get("key"))))
                        stateFromTrace.put(op.get("key").hashCode(), (String) op.get("value"));
                }
            }
        }
        /**
         * Verify that all replicas are in the same state and that the state is equal to the one constructed from
         * the trace.
         */
        HashMap<Integer, String> reference = nodeStores.get(0);
        Assert.assertEquals(stateFromTrace,reference);
        for (HashMap<Integer, String> nodeStore : nodeStores) {
            Assert.assertTrue(reference.equals(nodeStore));
        }
    }


    private static boolean completed(ConcurrentLinkedQueue trace, String opId) {
        for (Object item : trace) {
            HashMap<String, Object> response = (HashMap) item;
            if (response != null && response.get("operationCode") == null && opId.equals(response.get("id"))){
                return true;
            }
        }
        return false;
    }

}
