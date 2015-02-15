// ChatServerInterface.java
// Fredrik Kilander, DSV SU/KTH
// 14-oct-2004/FK New package.
// 25-mar-2004/FK New package.
// 18-mar-2004/FK First version

package dsv.pis.chat.server;

import net.jini.core.event.RemoteEventListener;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * This interface is implemented by the ChatServer, and is used by ChatClient
 * to place requests. It must therefore be known to both implementations.
 */
public interface ChatServerInterface
        extends
        java.rmi.Remote {
    /**
     * Used by ChatClient instances to inject a text message to be
     * distributed to registered ChatClientNotificationInterfaces.
     *
     * @param msg The message.
     */
    public void say(UUID uuid, String msg)
            throws IOException;

    /**
     * Returns the server's user-friendly name.
     *
     * @return The server's user-friendly name.
     */
    public String getName() throws RemoteException;

    /**
     * Used by ChatClient instances to register themselves as receivers of
     * remote notifications.
     *
     * @param rel An object that implements net.jini.core.event.RemoteEvent
     *            interface.
     */
    public void register(UUID uuid, RemoteEventListener rel)
            throws RemoteException;

    /**
     * Used by ChatClient instances to unregister themselves as receivers of
     * remote notifications.
     *
     * @param rel An object that implements net.jini.core.event.RemoteEvent
     *            interface. This should be the same object as was originally
     *            used to register.
     */
    public void unregister(UUID uuid)
            throws RemoteException;

    public void setName(UUID uuid, String name) throws RemoteException;

    public void printClients(UUID uuid) throws RemoteException;
}
