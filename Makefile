# Makefile

# Compiler
JAVAC = javac

# Java source files
BRIDGE_SOURCES = Bridge.java
STATION_SOURCES = Station.java

SHELL: bridge station

# Build rule for Bridge class
bridge:
	$(JAVAC) $(BRIDGE_SOURCES)

# Build rule for Station class
station:
	$(JAVAC) $(STATION_SOURCES)

# Clean rule
clean:
	rm -f .cs*
	rm -rf *.class