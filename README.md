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

### Cluster

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

## Configuration

See `src/main/resources/` and `src/test/resources/` in each project to find configuration parameters.

## Tests

A collection of test-scenarios using Kompics simulation framework can be found at `server/src/test/`, each test has its own main-method and is supposed to be ran individually.

## Authors

Template provided by 

Lars Kroll, lkroll@kth.se

Algorithms and tests written by

Kim Hammar, kimham@kth.se

Konstantin Sozinov, sozinov@kth.se


