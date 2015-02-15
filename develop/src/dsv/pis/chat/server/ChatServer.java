// ChatServer.java
// Fredrik Kilander, DSV SU/KTH
// 14-oct-2004/FK New package.
// 25-mar-2004/FK New package.
// 18-mar-2004/FK First version
//
// This program is a simple chat-server using Jini. It answers to requests
// from ChatClient instances, which deposit message strings on the methods
// that implement ChatServerInterface. The message strings are then sent
// back out as CharNotification events to all callbacks that are
// registered with the server.

package dsv.pis.chat.server;

// Standard Java

import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.core.lookup.ServiceID;
import net.jini.lookup.JoinManager;
import net.jini.lookup.ServiceIDListener;
import net.jini.lookup.entry.Name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Jini

/**
 * The ChatServer class is a main program application that implements
 * a simple chat service. It provides service to ChatClient instances
 * which also host the user interface application.
 */
public class ChatServer
        extends
        java.rmi.server.UnicastRemoteObject    // for Java RMI
        implements
        ChatServerInterface,    // for clients
        Runnable            // for the distribution thread.
{
    /**
     * The server's message counter. Increments monotonically with each
     * message dispatched.
     */
    protected int msgCount = 0;

    /**
     * Incoming messages are placed on the message queue. The distribution
     * thread consumes the queue by sending copies off to registered clients.
     */
    protected LinkedList<ChatNotification> msgQueue = new LinkedList<>();

    /**
     * The notification objects of registered clients are held in this
     * vector.
     */
    protected Map<UUID, Client> clients = new ConcurrentHashMap<>();

    /**
     * The printed name of this server instance.
     */
    protected String serverName = null;

    /**
     * The join manager is a Jini utility object that helps us being
     * registered with lookup servers.
     */
    protected JoinManager jmgr = null;

    /**
     * The delivery thread runs while this flag is true.
     */
    protected boolean runDelivery = true;

    /**
     * This inner class implements the ServiceIDListener interface on which
     * the Jini join manager will notify us when it has created a service id
     * for us and registered us with the lookup servers.
     */
    private class IDListener implements ServiceIDListener {
        public IDListener() {
        }

        public void serviceIDNotify(ServiceID sid) {
            System.out.println("Registered as Jini service " + sid);
        }
    }

    /**
     * Creates a new ChatServer.
     *
     * @param idName The identifying name of this server instance.
     */
    public ChatServer(String idName)
            throws
            java.io.IOException,
            java.rmi.RemoteException,    // if join doesn't work
            java.net.UnknownHostException // if we don't know where we are
    {
        // Find out our hostname so that clients can see it in the registration.

        String host = InetAddress.getLocalHost().getHostName().toLowerCase();

        serverName =
                "ChatServer " + ((idName != null) ? idName : "") + " on " + host;

        // Compose the arguments for the registration attempt with the
        // Jini lookup server.

        Entry[] attributes = new Entry[1];
        attributes[0] = new Name(serverName);

        // Create a Join manager that will hunt out any Jini lookup servers
        // out there and register us with them.

        jmgr = new JoinManager
                (
                        this,            // this is the service object
                        attributes,        // how we describe ourselves
                        new IDListener(),    // to learn of a registration
                        null,            // default service discovery manager
                        null            // default lease renewal manager
                );

        // Start the service thread.
        new Thread(this).start();
    }

    /**
     * Shuts down the server by asking the join manager to stop working.
     * This will deregister this ChatServer instance from the lookup
     * servers so they don't deal out dead service objects to clients.
     * The registration usually times out in five minutes but this is,
     * well, <strong>cleaner</strong>.
     */
    protected void shutdown() {
        jmgr.terminate();
    }

    /**
     * Adds a message the the output queue. This call is synchronized so
     * we have unperturbed access to the modification of the message queue.
     *
     * @param msg The text message to add.
     */
    protected synchronized void addMessage(Object source, String msg, UUID... targets) {
        try {
            if (targets != null && targets.length > 0) {
                msgQueue.addLast(new ChatNotification(source, msg, msgCount, targets));
                if (source instanceof Client) {
                    ((Client) source).getStatistics().addSntMsgCount();
                }
                msgCount++;
            } else {
                System.out.println(msg + "<- does not have any receiver, dropped");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Wake up the distribution thread.
        wakeUp();
    }

    /**
     * Retrieves the oldest (first) message from the message queue.
     * This call is synchronized to prevent simultaneous update of
     * the message queue.
     *
     * @return The next message, or null if the queue is empty.
     */
    protected synchronized ChatNotification getNextMessage() {
        ChatNotification rtn = null;
        try {
            rtn = msgQueue.removeFirst();
        } catch (java.util.NoSuchElementException nse) {
        }
        return rtn;
    }

    // In interface ChatServerInterface

    public void say(UUID uuid, String msg) throws IOException {
        if (msg != null) {
            addMessage(clients.get(uuid), msg, allClients());
        }
    }

    // In interface ChatServerInterface

    public String getName() throws java.rmi.RemoteException {
        return serverName;
    }

    // In interface ChatServerInterface

    public void register(UUID uuid, RemoteEventListener rel) {
        if (rel != null && uuid != null) {
            clients.put(uuid, new Client(uuid, rel));
            addMessage(this, uuid.toString() + " just joined our chat", allClientsExcept(uuid));
            System.out.println("Added client : " + rel.toString() + " totally " + clients.size() + " clients");
        }
    }

    // In interface ChatServerInterface

    public void unregister(UUID uuid)
            throws java.rmi.RemoteException {
        if (uuid != null && clients.containsKey(uuid)) {
            clients.remove(uuid);
            addMessage(this, clients.get(uuid).getName() + "(" + uuid.toString() + ") just left our chat", allClientsExcept(uuid));
            System.out.println("Removed client : " + uuid.toString());
        }
    }

    @Override
    public void setName(UUID uuid, String name) {
        clients.get(uuid).setName(name);
        addMessage(this, clients.get(uuid).getName() + " now using name:" + name, allClientsExcept(uuid));
    }

    @Override
    public void listClients(UUID uuid) throws RuntimeException {
        StringBuilder toPrint = new StringBuilder("Connected Clients\n");
        for (Map.Entry<UUID, Client> eachClient : clients.entrySet()) {
            toPrint.append(eachClient.getValue().getName()).append("(").append(eachClient.getKey().toString()).append(")").append(eachClient.getValue().getStatistics().toString()).append("\n");
        }
        addMessage(this, toPrint.toString(), uuid);
    }

    /**
     * This method is where the delivery thread (in method run()) rests
     * while the message queue is empty.
     */
    protected synchronized void snooze() {
        try {
            wait();
        } catch (InterruptedException | IllegalMonitorStateException iex) {
        }
    }

    /**
     * This method is called when the service interface has added a new
     * message to the message queue. If the delivery thread is waiting
     * in snooze(), it will continue as soon as this method is exited.
     * The thread that calls this method is the RMI service thread, the
     * thread that channels remote requests into the service interface code.
     * The call sequence is: say(String):addMessage(String):wakeUp().
     */
    protected synchronized void wakeUp() {
        notify();
    }

    /**
     * This is where the distribution thread spends its time. It dequeues
     * the message queue, builds a ChatNotification event and sends it to
     * each client that has registered a remote event listener with us.
     * When the message queue is empty, the thread calls snooze() and does
     * nothing until it is awakened by the code that has added a new
     * message to the message queue.
     */
    public void run() {

        while (runDelivery) {
            ChatNotification msg = getNextMessage();
            if (msg != null) {
                try {
                    for (UUID uuid : msg.getTargets()) {
                        clients.get(uuid).sendMessage(msg.getEvent());
                    }
                } catch (UnknownEventException | RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                snooze();
            }
        } // while runDelivery

        System.out.println("\nDelivery thread exiting.");
    }

    /**
     * This method implements a small command interpreter which only
     * exists to perform a graceful shutdown of the server.
     */
    public void readLoop() {
        boolean halted = false;
        BufferedReader d = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Server " + serverName + " started.");
        while (!halted) {
            System.out.print("Server> ");
            System.out.flush();
            String buf = null;
            try {
                buf = d.readLine();
            } catch (java.io.IOException iox) {
                iox.printStackTrace();
                System.out.println("\nI/O error in command interface.");
                halted = true;
                continue;
            }

            if (buf == null) {
                halted = true;
                continue;
            }

            String arg = buf.trim();

            if (arg.length() == 0) {
                continue;
            }

            if (arg.equalsIgnoreCase("quit") ||
                    arg.equalsIgnoreCase("stop") ||
                    arg.equalsIgnoreCase("halt") ||
                    arg.equalsIgnoreCase("exit")) {
                halted = true;
            } else if (arg.equalsIgnoreCase("help")) {
                System.out.println("Available commands:");
                System.out.println("quit      Shuts down the server.");
                System.out.println("help      This text.");
            } else {
                System.out.println("\nUnknown server command : " + arg);
            }
        }

        System.out.println("\nShutting down, please wait...");
        runDelivery = false;
        wakeUp();
        shutdown();
        System.out.println("Join manager terminated.");
    }

    /**
     * Contains the help text strings for the commandline interface.
     */
    protected static String[] usageText = new String[]{
            "Usage: [-n server-name]",
            "       [-h|--help]"
    };


    private UUID[] allClients() {
        return allClientsExcept();
    }

    private UUID[] allClientsExcept(UUID... uuids) {
        Set<UUID> keys = new HashSet<>(clients.keySet());
        if (uuids != null) {
            for (UUID eachToEx : uuids) {
                keys.remove(eachToEx);
            }
        }
        return keys.toArray(new UUID[keys.size()]);
    }

    /**
     * This method implements the commandline help command.
     */
    protected static void usage() {
        for (String anUsageText : usageText) {
            System.out.println(anUsageText);
        }
    }

    // The main program.

    public static void main(String[] argv)
            throws
            java.io.IOException,
            java.rmi.RemoteException,
            java.net.UnknownHostException {

        String serverName = null;
        int state = 0;

        for (String av : argv) {
            if (state == 0) {
                if (av.equalsIgnoreCase("-n")) {
                    state = 1;
                } else if (av.equalsIgnoreCase("-h") ||
                        av.equalsIgnoreCase("--help")) {
                    usage();
                    System.exit(0);
                } else {
                    usage();
                    System.exit(1);
                }
            } else if (state == 1) {
                serverName = av;
                state = 0;
            }
        }

        System.setSecurityManager(new RMISecurityManager());
        ChatServer cs = new ChatServer(serverName);
        cs.readLoop();
        System.exit(0);
    }
}
