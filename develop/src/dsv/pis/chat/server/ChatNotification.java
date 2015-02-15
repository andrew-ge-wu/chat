// ChatNotification.java
// Fredrik Kilander, DSV SU/KTH
// 18-mar-2004/FK First version

package dsv.pis.chat.server;

import net.jini.core.event.RemoteEvent;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

/**
 * This class implements the notification that is sent to the ChatClients
 * as a new piece of text in the discussion is distributed. The clients
 * can obtain the serial number by calling method getSequenceNumber ()
 * (defined in the superclass RemoteEvent) and the message text by calling
 * method getText () defined below.
 */
public class ChatNotification implements Serializable {
    private final Message event;
    private final UUID[] targets;

    /**
     * Creates a new ChatNotification instance.
     *
     * @param source The object from which this instance originates.
     * @param msg    The message to the client.
     * @param serial The serial number of the message in the server's sequence.
     */
    public ChatNotification(Object source, String msg, int serial, UUID... targets) throws IOException {
        // Call the constructor of the superclass (RemoteEvent) explicitly
        // so that its fields can be initialized to what we want. Actually,
        // we are only putting the serial number in as the sequence nr, but
        // the other arguments could be there as well if we had use for them.
        String prefix = "";
        if (source instanceof Client) {
            Client client = Client.class.cast(source);
            prefix = "[" + client.getName() + "]: ";
        } else if (source instanceof ChatServer) {
            ChatServer server = ChatServer.class.cast(source);
            prefix = "[" + server.getName() + "]: ";
        }
        this.event = new Message(source,        // Source
                0,            // ID
                serial,        // sequence number
                prefix + msg);        // handback
        this.targets = targets;
    }

    public Message getEvent() {
        return event;
    }

    public UUID[] getTargets() {
        return targets;
    }

    public class Message extends RemoteEvent {

        private final String msg;

        public Message(Object o, long l, long l1, String msg) {
            super(o, l, l1, null);
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }


}
