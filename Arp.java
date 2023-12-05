/*
    Authors names: Sri Vennala Kandibedala
                   Rahul Mallela
*/
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class Arp implements Serializable {

    // ConcurrentHashMap to store ARP cache entries with destination IP as key
    private static ConcurrentHashMap<String, Map<String, Object>> arpCache = new ConcurrentHashMap<>();

    // Resets the Time To Live (TTL) for a given key in the ARP cache.
    static void resetTTl(String key) {
        Map<String, Object> entry = arpCache.get(key);
        if (entry != null) {
            entry.put("ttl", 60);
            arpCache.put(key, entry);
        }
    }

    // Updates the TTL for each entry in the ARP cache and removes expired entries.
    private static void updateTimer() {
        List<Map.Entry<String, Map<String, Object>>> entriesToRemove = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : arpCache.entrySet()) {
            int currentTTL = (int) entry.getValue().get("ttl");
            if (currentTTL > 0) {
                entry.getValue().put("ttl", currentTTL - 1);
            } else {
                System.out.println("Entry with IP " + entry.getKey() + " has expired and will be removed.");
                entriesToRemove.add(entry);
            }
        }

        // Remove expired entries
        for (Map.Entry<String, Map<String, Object>> entryToRemove : entriesToRemove) {
            arpCache.remove(entryToRemove.getKey());
            System.out.println("Entry with IP " + entryToRemove.getKey() + " has been removed.");
        }
    }

    // Adds a new entry to the ARP cache with the specified destination IP and MAC address.
    static void addArpCache(String destIP, String destMac) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("destMac", destMac);
        entry.put("ttl", 60);
        arpCache.put(destIP, entry);
    }

    // Retrieves the MAC address associated with the specified destination IP from the ARP cache.
    static String getMac(String destIP) {
        String mac = "";
        if (arpCache.containsKey(destIP)) {
            mac = (String) arpCache.get(destIP).get("destMac");
        }

        if (mac != "") {
            return mac;
        } else {
            System.out.println("Mac not found for " + destIP);
            return "";
        }
    }

    // Prints the contents of the ARP cache in a tabular format.
    static void printArpCache() {
        System.out.println("|Destination IP\t|\tDestination Mac\t|\tTime To Live|");
        for (Map.Entry<String, Map<String, Object>> entry : arpCache.entrySet()) {
            String destIP = entry.getKey();
            Map<String, Object> entryDetails = entry.getValue();

            String destMac = entryDetails.get("destMac").toString();
            int ttl = (int) entryDetails.get("ttl");
            System.out.printf("|%s\t|\t%s\t|\t%d|%n", destIP, destMac, ttl);
        }
        System.out.println("End of Arp Cache Table");
    }

    // Starts a timer to periodically update the TTL values and remove expired entries from the ARP cache.
    static void myTimer() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }
}

