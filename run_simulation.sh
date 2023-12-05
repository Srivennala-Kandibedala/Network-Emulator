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
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces.a rtable.a hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces.b rtable.b hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces.c rtable.c hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces.d rtable.d hosts"'
sleep 1
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -no ifaces.e rtable.e hosts"'
sleep 2
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -route ifaces.r1 rtable.r1 hosts"'
osascript -e 'tell application "Terminal" to do script "cd /Users/srivennalakandibedala/Datacom/Project2 && java Station -route ifaces.r2 rtable.r2 hosts"'


