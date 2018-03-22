package popjava.combox.socket.ssl;

import popjava.broker.Broker;
import popjava.broker.RequestQueue;
import popjava.util.LogWriter;
import popjava.combox.socket.ComboxAcceptSocket;
import popjava.combox.socket.raw.ComboxAcceptRawSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import popjava.util.ssl.SSLUtils;

/**
 * This class is responsible to accept the new connection for the associated server combox socket
 */
public class ComboxAcceptSecureSocket extends ComboxAcceptSocket<SSLSocket>{
	
	protected final SSLContext sslContext;

	/**
	 * Create a new instance of the ComboxAccept socket
	 * @param broker		The associated broker
	 * @param requestQueue	The associated request queue
	 * @param serverSocket		The associated combox socket
	 * @throws java.io.IOException if any problem occurs
	 */
	public ComboxAcceptSecureSocket(Broker broker, RequestQueue requestQueue,
			ServerSocket serverSocket) throws IOException {
		super(broker, requestQueue, serverSocket);
		
		try {
			sslContext = SSLUtils.getSSLContext();
		} catch(CertificateException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
			throw new IOException("Can't initialize SSL Context", e);
		}
	}

	/**
	 * Start the local thread
	 */
	public void run() {
		// used to upgrade plain sockets to SSL one
		SSLSocketFactory ssf = sslContext.getSocketFactory();
		
		while (status != EXIT) {
			try {
				Socket plainConnection = serverSocket.accept();
				SSLSocket sslConnection = (SSLSocket) ssf.createSocket(plainConnection, plainConnection.getInputStream(), true);
				// set SSL parameters
				sslConnection.setUseClientMode(false);
				sslConnection.setNeedClientAuth(true);
				
				LogWriter.writeDebugInfo("[SSL Accept] Connection accepted "+sslConnection.getLocalPort()+" local:"+sslConnection.getPort());	
				if(broker != null){
					broker.onNewConnection();
				}

				ComboxSecureSocket combox = new ComboxSecureSocket();
				if (combox.serverAccept(broker, sslConnection)) {
				    ComboxAcceptSocket.serveConnection(broker, requestQueue, combox, 1);
					concurentConnections.add(sslConnection);
				}
			} catch (IOException e) {
				LogWriter.writeDebugInfo("[SSL Accept] Error while setting up connection: %s", e.getMessage());
			}
		}
		
		LogWriter.writeDebugInfo("[SSL Accept] Combox Server finished");
		this.close();
	}

}