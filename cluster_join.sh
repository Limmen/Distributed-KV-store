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

echo "Joining existing cluster with $1 servers"
{
J=$(($1-1))
for i in `seq 1 $J`;
       do
                 sleep 1
                 PORT=$(($i+4000))
                ./server.sh $PORT &
        done
} &> /dev/null
PORT=$(($1+4000))
./server.sh $PORT &
wait