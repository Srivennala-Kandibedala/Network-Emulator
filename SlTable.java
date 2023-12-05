import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class SlTable implements Serializable {

    private static final int DEFAULT_TTL = 60;
    private static ConcurrentHashMap<String, List<Object>> slCache;

    // Constructor to initialize the SlTable.
    public SlTable() {
        this.slCache = new ConcurrentHashMap<>();
    }

    // Updates the TTL for each entry in the slCache and removes expired entries.
    private static void updateSlTimer() {
        List<String> keysToRemove = new ArrayList<>();

        for (Map.Entry<String, List<Object>> entry : slCache.entrySet()) {
            int currentTTL = (int) entry.getValue().get(2);
            if (currentTTL > 0) {
                entry.getValue().set(2, currentTTL - 1);
            } else {
                System.out.println("Entry with MAC " + entry.getKey() + " has expired and will be removed from slCache.");
                keysToRemove.add(entry.getKey());
            }
        }

        for (String keyToRemove : keysToRemove) {
            slCache.remove(keyToRemove);
            System.out.println("Entry with MAC " + keyToRemove + " has been removed from slCache.");
        }
    }

    // Adds a new entry to the slCache with the provided MAC address, SocketChannel, and port information.
    public void addEntry(String mac, SocketChannel fd, int port) {
        List<Object> entry = new ArrayList<>();
        entry.add(fd);
        entry.add(port);
        entry.add(DEFAULT_TTL);
        slCache.put(mac, entry);
    }

    // Retrieves the SocketChannel associated with the provided MAC address.
    public SocketChannel getEntry(String macAddress) {
        return (SocketChannel) this.slCache.get(macAddress).get(0);
    }

    public boolean isKey(String macAddress) {
        return this.slCache.containsKey(macAddress);
    }

    // Initializes a timer task for periodically updating the slCache entries' TTL values.
    public void myTimer() {
        Timer slTimer = new Timer();
        slTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSlTimer();
            }
        }, 0, 1000);
    }

    // Resets the TTL value for a specific entry in the slCache based on the provided MAC address.
    public void resetTTl(String key) {
        List<Object> entry = slCache.get(key);
        entry.set(2, 60);
        slCache.put(key, entry);
    }

    // Prints the contents of the slCache, displaying MAC addresses, ports, and TTL values.
    public void printSl() {
        System.out.println("MAC Address\t\t| Port\t| TTL");
        for (Map.Entry<String, List<Object>> entry : slCache.entrySet()) {
            String macAddress = entry.getKey();
            int port = (int) entry.getValue().get(1);
            int ttl = (int) entry.getValue().get(2);

            System.out.printf("%s\t| %d\t| %d%n", macAddress, port, ttl);
        }
    }

    // Removes an entry from the slCache based on the provided SocketChannel.
    public void remove(SocketChannel fd) {
        Iterator<Map.Entry<String, List<Object>>> iterator = slCache.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<Object>> entry = iterator.next();
            List<Object> values = entry.getValue();

            if (values.get(0) == fd) {
                iterator.remove();
                return;
            }
        }
    }
}