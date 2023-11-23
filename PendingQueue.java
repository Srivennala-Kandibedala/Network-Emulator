import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//
class PendingQueue implements Serializable {
    private Map<String, List<Message>> pendingPackets;

    public PendingQueue() {
        this.pendingPackets = new HashMap<>();
    }

    public void addPendingPacket(String nextHopIP, Message packet) {
//        this.pendingPackets.put(nextHopIP, packet);
        this.pendingPackets.computeIfAbsent(nextHopIP, k -> new ArrayList<>()).add(packet);
//        this.printPendingQueue();
    }

    public List<Message> getPendingPacket(String nextHopIP) {
        System.out.println("pending queue nexthop "+ nextHopIP);
        System.out.println(this.pendingPackets.get(nextHopIP));
        return this.pendingPackets.get(nextHopIP);
    }

    public void removePendingPacket(String nextHopIP) {
        pendingPackets.remove(nextHopIP);
    }


    public void printPendingQueue() {
        for (Map.Entry<String, List<Message>> entry : this.pendingPackets.entrySet()) {
            String key = entry.getKey();
            List<Message> value = entry.getValue();

            System.out.println("Key: " + key);
            System.out.print("Value:");

            for (Message obj : value) {
                System.out.println("  " + obj.getMessage());
            }
            System.out.println(); // Separate entries with a blank line
        }
    }
}