package dsv.pis.chat.server.model;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import javax.xml.bind.DatatypeConverter;
import java.rmi.RemoteException;

/**
 * @author andrew, Innometrics
 */
public class Client {
    private final String id;
    private final RemoteEventListener listener;
    private final Statistics statistics;
    private String name;

    public Client(RemoteEventListener rel) {
        id = DatatypeConverter.printBase64Binary(rel.toString().getBytes());
        this.listener = rel;
        this.statistics = new Statistics();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void sendMessage(RemoteEvent toSend) throws UnknownEventException, RemoteException {
        listener.notify(toSend);
        statistics.addMsgCount();
    }

    public class Statistics {
        private int nrMessages = 0;

        public void addMsgCount() {
            nrMessages++;
        }

        public int getNrMessages() {
            return nrMessages;
        }
    }
}
