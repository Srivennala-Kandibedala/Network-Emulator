import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

class SlTable implements Serializable {
    private Map<String, Map<String, Object>> slCache;

    public SlTable() {
        this.slCache = new HashMap<>();
    }

    void add(String destIP, String destMac) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("destMac", destMac);
        entry.put("ttl", 60);
        this.slCache.put(destIP, entry);
    }

}