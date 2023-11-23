#!/bin/csh -f

killall xterm

cd $PWD

rm .cs*

make clean
sleep 2
make

sleep 1
xterm -T "BRIDGE cs1" -iconic -e java Bridge cs1 8 &
xterm -T "BRIDGE cs2" -iconic -e java Bridge cs2 8 &
xterm -T "BRIDGE cs3" -iconic -e java Bridge cs3 8 &
sleep 1
xterm -T "Host A" -e java Station -no ifaces.a rtable.a hosts &
sleep 1
xterm -T "Host B" -e java Station -no ifaces.b rtable.b hosts &
# sleep 1
xterm -T "Host C" -e java Station -no ifaces.c rtable.c hosts &
sleep 1
xterm -T "Host D" -e java Station -no ifaces.d rtable.d hosts &
sleep 1
xterm -T "Host E" -e java Station -no ifaces.e rtable.e hosts &
sleep 1
xterm -T "Router r1" -iconic -e java Station -route ifaces.r1 rtable.r1 hosts &
xterm -T "Router r2" -iconic -e java Station -route ifaces.r2 rtable.r2 hosts &
