package dsv.pis.chat.server;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.UUID;

/**
 * @author andrew, Innometrics
 */
public class Client implements Serializable {
    private final RemoteEventListener listener;
    private final Statistics statistics;
    private String name;

    public Client(UUID uuid, RemoteEventListener rel) {
        this.listener = rel;
        this.statistics = new Statistics();
        this.name = uuid.toString();
        this.statistics.setJoinDateNow();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void sendMessage(RemoteEvent event) throws UnknownEventException, RemoteException {
        listener.notify(event);
        statistics.addRecMsgCount();
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public class Statistics implements Serializable {
        private int nrReceivedMessages = 0;
        private int nrSentMessages = 0;
        private Date joinDate;

        public Date getJoinDate() {
            return joinDate;
        }

        public void setJoinDateNow() {
            setJoinDate(new Date());
        }

        public void setJoinDate(Date joinDate) {
            this.joinDate = joinDate;
        }

        public void addSntMsgCount() {
            nrSentMessages++;
        }

        public int getNrSentMessages() {
            return nrSentMessages;
        }

        public void addRecMsgCount() {
            nrReceivedMessages++;
        }

        public int getNrReceivedMessages() {
            return nrReceivedMessages;
        }

        public String toString() {
            return "Joined:" + getJoinDate().toString() + " received:" + getNrReceivedMessages() + " messages" + " sent:" + getNrSentMessages() + " messages";
        }
    }
}
