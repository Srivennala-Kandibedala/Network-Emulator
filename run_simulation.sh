#!/bin/bash

rm -f .cs*

xterm -T "BRIDGE cs1" -iconic -e java Bridge cs1 1 &
xterm -T "BRIDGE cs2" -iconic -e java Bridge cs2 8 &
xterm -T "BRIDGE cs3" -iconic -e java Bridge cs3 8 &

xterm -T "Host A" -e java Station -no ifaces/ifaces.a rtables/rtable.a hosts &

xterm -T "host B" -e java Station -no ifaces/ifaces.b rtables/rtable.b hosts &

xterm -T "host C" -e java Station -no ifaces/ifaces.c rtables/rtable.c hosts &

xterm -T "host D" -e java Station -no ifaces/ifaces.d rtables/rtable.d hosts &

xterm -T "host E" -e java Station -no ifaces/ifaces.e rtables/rtable.e hosts &

xterm -T "Router r1" -iconic -e java Station -route ifaces/ifaces.r1 rtables/rtable.r1 hosts &
xterm -T "Router r2" -iconic -e java Station -route ifaces/ifaces.r2 rtables/rtable.r2 hosts &