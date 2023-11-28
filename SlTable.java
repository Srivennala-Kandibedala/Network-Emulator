import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.*;

class SlTable implements Serializable {

    private static final int DEFAULT_TTL = 60;
    private static Map<String, List<Object>> slCache;

    public SlTable() {
        this.slCache = new HashMap<>();
    }

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

    public void addEntry(String mac, SocketChannel fd, int port) {
        List<Object> entry = new ArrayList<>();
        entry.add(fd);
        entry.add(port);
        entry.add(DEFAULT_TTL);
        slCache.put(mac, entry);
    }

    public SocketChannel getEntry(String macAddress) {
        return (SocketChannel) this.slCache.get(macAddress).get(0);
    }

    public boolean isKey(String macAddress) {
        return this.slCache.containsKey(macAddress);
    }

    public void myTimer() {
        Timer slTimer = new Timer();
        slTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSlTimer();
            }
        }, 0, 1000);
    }

    public void resetTTl(String key) {
        List<Object> entry = slCache.get(key);
        entry.set(2, 60);  // Update the value at index 2 in the list
        slCache.put(key, entry);  // Put the modified list back into the map
    }

    public void printSl() {
        System.out.println(slCache.toString());
    }

    public void remove(SocketChannel fd) {
        Iterator<Map.Entry<String, List<Object>>> iterator = slCache.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<Object>> entry = iterator.next();
            List<Object> values = entry.getValue();

            // Check if the first element of the entry is the given SocketChannel
            if (values.get(0) == fd) {
                // Remove the entry if found
                iterator.remove();
//                System.out.println("Entry removed for SocketChannel: " + fd);
                return;
            }
        }
    }
}