import java.io.*;

class Message implements Serializable {
    private String sourceIP;
    private String destinationIP;
    private String message;

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public void setDestinationIP(String destinationIP) {
        this.destinationIP = destinationIP;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Message(String sourceIP, String destinationIP, String message) {
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.message = message;
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            return bos.toByteArray();
        }
    }

    // Static method to deserialize a byte array into a Message object
    public static Message deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return (Message) in.readObject();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getSourceIP() {
        return this.sourceIP;
    }

    public String getDestinationIP() {
        return this.destinationIP;
    }
}