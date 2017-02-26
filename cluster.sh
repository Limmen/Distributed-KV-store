#!/usr/bin/env bash

intexit() {
    kill -HUP -$$
}

hupexit() {
    echo
    echo "Cluster stopped"
    exit
}

trap hupexit HUP
trap intexit INT

echo "Starting cluster of $1 servers"
./bootstrap_server.sh &
{
sleep 2
BOOTCLIENTS=$(($1-1))
for i in `seq 1 $BOOTCLIENTS`;
       do
                 sleep 1
                 PORT=$(($i+3000))
                (./server.sh $PORT &)
        done
} &> /dev/null
wait