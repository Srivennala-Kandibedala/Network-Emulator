/*
    Authors names: Sri Vennala Kandibedala
                   Rahul Mallela
*/
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

    // Adds a pending packet to the queue for a specified next hop IP.
    public void addPendingPacket(String nextHopIP, Message packet) {
        this.pendingPackets.computeIfAbsent(nextHopIP, k -> new ArrayList<>()).add(packet);
    }

    // Retrieves the list of pending packets for a specified next hop IP.
    public List<Message> getPendingPacket(String nextHopIP) {
        System.out.println("pending queue nexthop " + nextHopIP);
        return this.pendingPackets.get(nextHopIP);
    }

    // Removes the pending packet entry for a specified next hop IP.
    public void removePendingPacket(String nextHopIP) {
        this.pendingPackets.remove(nextHopIP);
    }

    // Prints the contents of the Pending Queue in a tabular format.
    public void printPendingQueue() {
        System.out.println("IP Address\t\t  Message");
        for (Map.Entry<String, List<Message>> entry : this.pendingPackets.entrySet()) {
            String key = entry.getKey();
            List<Message> value = entry.getValue();

            System.out.print(key + "\t\t");

            for (Message obj : value) {
                System.out.print("  " + obj.getMessage());
            }
            System.out.println();
        }
    }
}