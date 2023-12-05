/*
    Authors Names: Sri Vennala Kandibedala
                   Rahul Mallela
*/
import java.io.*;

class Message implements Serializable {
    private String sourceIP;
    private String destinationIP;
    private String message;

    public Message(String sourceIP, String destinationIP, String message) {
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.message = message;
    }

    // Static method to deserialize a byte array into a Message object
    public static Message deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (Message) in.readObject();
        }
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            return bos.toByteArray();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSourceIP() {
        return this.sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getDestinationIP() {
        return this.destinationIP;
    }

    public void setDestinationIP(String destinationIP) {
        this.destinationIP = destinationIP;
    }
}