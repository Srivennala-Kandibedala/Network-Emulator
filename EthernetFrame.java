/*
    Authors names: Sri Vennala Kandibedala
                   Rahul Mallela
*/
import java.io.*;
import java.nio.ByteBuffer;

public class EthernetFrame implements Serializable {
    private String type;
    private Message packet;
    private String sourceIP;
    private String destinationIP;
    private String sourceMac;
    private String destinationMac;

    public EthernetFrame() {

    }

    // Static method to deserialize a byte array into a Message object
    public static EthernetFrame deserialize(ByteBuffer bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes.array());
             ObjectInput in = new ObjectInputStream(bis)) {
            return (EthernetFrame) in.readObject();
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Message getPacket() {
        return packet;
    }

    public void setPacket(Message packet) {
        this.packet = packet;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getDestinationIP() {
        return destinationIP;
    }

    public void setDestinationIP(String destinationIP) {
        this.destinationIP = destinationIP;
    }

    public String getSourceMac() {
        return sourceMac;
    }

    public void setSourceMac(String sourceMac) {
        this.sourceMac = sourceMac;
    }

    public String getDestinationMac() {
        return destinationMac;
    }

    public void setDestinationMac(String destinationMac) {
        this.destinationMac = destinationMac;
    }

    // Creates an ARP (Address Resolution Protocol) Ethernet frame with the provided details.
    public void createArp(String type, String sourceIP, String destinationIP, String sourceMac, String destinationMac) {
        this.type = type;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.sourceMac = sourceMac;
        this.destinationMac = destinationMac;
    }

    // Creates a DataFrame Ethernet frame with the provided details.
    public void createDF(String type, Message packet, String sourceIP, String destinationIP, String sourceMac, String destinationMac) {
        this.type = type;
        this.packet = packet;
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.sourceMac = sourceMac;
        this.destinationMac = destinationMac;
    }

    // Serializes the EthernetFrame object into a byte array.
    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            return bos.toByteArray();
        }
    }

}
