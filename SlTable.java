import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SlTable implements Serializable {

    private Map<String, List<Object>> slCache;
    private static final int DEFAULT_TTL = 60;
    public SlTable() {
        this.slCache = new HashMap<>();
    }

    public void addEntry(String mac, SocketChannel fd, int port) {
        List<Object> entry = new ArrayList<>();
        entry.add(fd);
        entry.add(port);
        entry.add(DEFAULT_TTL);

        slCache.put(mac,entry);
    }

    public SocketChannel getEntry(String macAddress){
        return (SocketChannel) this.slCache.get(macAddress).get(0);
    }

    public boolean isKey(String macAddress){
        return this.slCache.containsKey(macAddress);
    }

}