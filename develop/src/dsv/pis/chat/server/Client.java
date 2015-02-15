package dsv.pis.chat.server;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * @author andrew, Innometrics
 */
public class Client implements Serializable{
    private final RemoteEventListener listener;
    private final Statistics statistics;
    private String name;

    public Client(RemoteEventListener rel) {
        this.listener = rel;
        this.statistics = new Statistics();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void sendMessage(RemoteEvent event) throws UnknownEventException, RemoteException {
        listener.notify(event);
        statistics.addMsgCount();
    }


    public class Statistics implements Serializable {
        private int nrMessages = 0;

        public void addMsgCount() {
            nrMessages++;
        }

        public int getNrMessages() {
            return nrMessages;
        }
    }
}