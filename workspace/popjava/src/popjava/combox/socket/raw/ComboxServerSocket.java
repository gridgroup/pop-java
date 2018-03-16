

package popjava.combox.socket.raw;

import popjava.broker.Broker;
import popjava.buffer.*;
import popjava.baseobject.AccessPoint;

import java.net.*;
import java.io.*;
import popjava.combox.ComboxServer;
import popjava.combox.ComboxUtils;
/**
 * This class is an implementation of the combox with the protocol socket for the server side.
 */
public class ComboxServerSocket extends ComboxServer {
    
	public static final int BUFFER_LENGTH = 1024;
    private final int RECEIVE_BUFFER_SIZE = 1024 * 8 * 500;
    
	protected ServerSocket serverSocket = null;
	private ComboxAcceptSocket serverCombox = null;

	/**
	 * Default constructor. Create a new instance of a socket combox
	 * @param accessPoint	Access point of the combox
	 * @param timeout		Connection timeout
	 * @param buffer		Buffer associated with this combox
	 * @param broker		Broker associated with this combox
	 * @throws java.io.IOException if any problem occurs
	 */
	public ComboxServerSocket(AccessPoint accessPoint, int timeout,
			POPBuffer buffer, Broker broker) throws IOException {
		super(accessPoint, timeout, broker);
		createServer();
	}

	/**
	 * Get the URL of the combox
	 * @return	URL as a string value
	 */
	public String getUrl() {
		return String.format("%s://%s:%d", ComboxSocketFactory.PROTOCOL,
				serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());
	}

	/**
	 * Create and start the combox server
	 * @throws java.io.IOException if any problem occurs
	 */
	public final void createServer() throws IOException {
		serverSocket = ComboxUtils.createServerSocket(accessPoint.getPort(), ss -> ss.setReceiveBufferSize(RECEIVE_BUFFER_SIZE), broker.isUPNPEnabled());
		serverCombox = new ComboxAcceptSocket(broker, getRequestQueue(), serverSocket);
		serverCombox.setStatus(RUNNING);
		Thread thread = new Thread(serverCombox, "Server combox acception thread");
		thread.start();
		accessPoint.setProtocol(ComboxSocketFactory.PROTOCOL);
		accessPoint.setHost(accessPoint.getHost());
		accessPoint.setPort(serverSocket.getLocalPort());
	}

	@Override
	public void close() {
		serverCombox.close();
	}
}
