# ID2203 Project

The goal of the project is to implement and test and simple partitioned, distributed in-memory key-value store with linearisable operation semantics.

## Overview

The project is split into 3 sub parts:

- A common library shared between servers and clients, containing mostly messages and similar shared types
- A server library that manages bootstrapping and membership
- A client library with a simple CLI to interact with a cluster of servers

The bootstrapping procedure for the servers, requires one server to be marked as a bootstrap server, which the other servers (bootstrap clients) check in with, before the system starts up. The bootstrap server also assigns initial partitions.

## Getting Started

Clone (your fork of) the repository to your local machine and cd into that folder.

### Building
Build the project with

```
maven clean install
```

Note: If you include tests in build it will take ~14sec due to the aggregated simulations.

### Running

#### Bootstrap Server Node

To run a bootstrap server node `cd` into the `server` directory and execute:

```
java -jar target/project17-server-1.0-SNAPSHOT-shaded.jar -p 45678
```

Or, stay in root dir and run:

```
./bootstrap_server.sh
```

This will start the bootstrap server on localhost:45678.

#### Normal Server Node
After you started a bootstrap server on `<bsip>:<bsport>`, again from the `server` directory execute:

```
java -jar target/project17-server-1.0-SNAPSHOT-shaded.jar -p 56789 -c <bsip>:<bsport>
```

Or stay in root dir and run:

```
./server.sh port
```

This will start the bootstrap server on localhost:56789, and ask it to connect to the bootstrap server at `<bsip>:<bsport>`.
Make sure you start every node on a different port if they are all running directly on the local machine.

By default you need 3 nodes (including the bootstrap server), before the system will actually generate a lookup table and allow you to interact with it.
The number can be changed in the configuration file (cf. [Kompics docs](http://kompics.sics.se/current/tutorial/networking/basic/basic.html#cleanup-config-files-classmatchers-and-assembly) for background on Kompics configurations).

#### Clients
To start a client (after the cluster is properly running), `cd` into the `client` directory and execute:

```
java -jar target/project17-client-1.0-SNAPSHOT-shaded.jar -p 56787 -b <bsip>:<bsport>
```

Or stay in root dir and run:

```
./client.sh
```

Again, make sure not to double allocate ports on the same machine.

The client will attempt to contact the bootstrap server and give you a small command promt if successful. Type `help` to see the available commands.

#### Cluster

To start a cluster of N servers in one shell run the following in the root dir:

```
./cluster.sh N
```

Stop the whole cluster by pressing Control-C

To join en existing cluster with a set of N servers (ofcourse you can also join one by one manually with `./server.sh port`) run the followng in the root dir:

```
./cluster_join.sh N
```

Stop the set of joined servers by pressing Control-C

These startup scripts will start the bootstrap-server on port 45678 and the other servers on ports 3000-3000+N. 

The cluster_join will send join requests to server on port 45678 and allocate ports 4000-4000+N to the new servers.
 
So make sure these port-ranges are free on your machine before running. You can always just modify the scripts if necessary.

#### Example

Assuming you use the default config in reference.conf:

```
id2203.project {
  address.ip = "127.0.0.1"
  address.port = 45678
  bootThreshold = 3
  replicationDegree = 2
  keySpace = 50
  keepAlivePeriod = 2000
  epfd.delta = 4000
  omega.timeout = 4000
  gms.timeout = 2000
  vsync.timeout = 2000
  overlayservice.timeout = 2000
  kvservice.timeout = 2000
}
```

The most important part of the config is replicationDegree, bootThreshold and keySpace. 

Open one shell and run:

```
./cluster_join.sh 6
```

After a little while the cluster should have booted with 2 partitions/replicationgroups who have installed their respective views
and you should see the output:

```
03/10 20:02:31 DEBUG[Kompics-worker-1] s.k.i.o.s.VSOverlayService - LookupTable updated: LookupTable(
0 -> [PID{netAddress=/127.0.0.1:45678, pid=0}, PID{netAddress=/127.0.0.1:3001, pid=1}, PID{netAddress=/127.0.0.1:3002, pid=2}]
50 -> [PID{netAddress=/127.0.0.1:3003, pid=3}, PID{netAddress=/127.0.0.1:3004, pid=4}, PID{netAddress=/127.0.0.1:3005, pid=5}]
)
```

Notice that since bootThreshold is 3, one view will first be booted and not until that have completed the join-requests of the rest will be handled and the second partition will boot, so it might take a few seconds.

Now open up a second shell and run:

```
./client.sh
```

Note that ports are hardcoded into the bash-scripts so one of the servers in the cluster will listen on port 45678 and that is the one that the client will connect to.

On the client you have access to the commandline to run operations:

```
03/10 20:14:48 INFO [Kompics-worker-5] s.k.i.k.ClientService - Client connected to localhost/127.0.0.1:45678, cluster size is 6
>put 1 1
Put-Operation sent! Awaiting response...
03/10 20:15:00 INFO [Kompics-worker-3] s.k.i.k.ClientService - Got OpResponse: OpResponse{id=9a5506d6-366d-4742-a896-9b93081f6636, status=OK, value=Write successful}
Operation complete! Response was: OK value: Write successful
>get 1 
Get-Operation sent! Awaiting response...
03/10 20:15:20 INFO [Kompics-worker-5] s.k.i.k.ClientService - Got OpResponse: OpResponse{id=d05e4632-e75c-42db-ab61-a2c8e9b5d391, status=OK, value=1}
Operation complete! Response was: OK value: 1
>cas 1 1 15
CAS-Operation sent! Awaiting response...
03/10 20:15:28 INFO [Kompics-worker-3] s.k.i.k.ClientService - Got OpResponse: OpResponse{id=5c8eaa69-514b-4b3a-910e-ba6c3b5b3fc3, status=OK, value=1}
CAS complete! Response was: OK value: 1
>get 1
Get-Operation sent! Awaiting response...
03/10 20:15:36 INFO [Kompics-worker-5] s.k.i.k.ClientService - Got OpResponse: OpResponse{id=99777f7b-2a27-46a7-b204-b144739c0a7e, status=OK, value=15}
Operation complete! Response was: OK value: 15
>get -1
Get-Operation sent! Awaiting response...
03/10 20:15:45 INFO [Kompics-worker-3] NettyNetwork@56787 - Trying to send delayed messages: /127.0.0.1:3003 on TCP
03/10 20:15:45 INFO [Kompics-worker-5] s.k.i.k.ClientService - Got OpResponse: OpResponse{id=85378f15-8aa6-475c-a178-f5e438fea431, status=OK, value=not found}
Operation complete! Response was: OK value: not found
>cas 1 -4000 0
CAS-Operation sent! Awaiting response...
03/10 20:16:04 INFO [Kompics-worker-3] s.k.i.k.ClientService - Got OpResponse: OpResponse{id=35de22b9-2c8d-42c0-b2cf-0509c5205726, status=OK, value=15}
CAS complete! Response was: OK value: 15
>
```

## Configuration

See `src/main/resources/` and `src/test/resources/` in each project to find configuration parameters.


## Tests

A collection of test-scenarios using Kompics simulation framework can be found at `server/src/test/`

Run all tests with:

```
mvn test
```
## Authors

Template provided by 

Lars Kroll, lkroll@kth.se

Algorithms and tests written by

Kim Hammar, kimham@kth.se

Konstantin Sozinov, sozinov@kth.se


