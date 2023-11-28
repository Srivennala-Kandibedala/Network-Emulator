#!/bin/csh -f

cd $PWD

rm .cs*

make clean
sleep 2
make

osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Bridge cs1 8"'
sleep 2
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Bridge cs2 8"'
#sleep 2
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Bridge cs3 8"'

sleep 2
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces/ifaces.a rtables/rtable.a hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces/ifaces.b rtables/rtable.b hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces/ifaces.c rtables/rtable.c hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces/ifaces.d rtables/rtable.d hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces/ifaces.e rtables/rtable.e hosts"'
sleep 2
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -route ifaces/ifaces.r1 rtables/rtable.r1 hosts"'
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -route ifaces/ifaces.r2 rtables/rtable.r2 hosts"'