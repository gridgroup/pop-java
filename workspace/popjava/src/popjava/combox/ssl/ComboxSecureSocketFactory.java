package popjava.combox.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import popjava.baseobject.AccessPoint;
import popjava.broker.Broker;
import popjava.buffer.POPBuffer;
import popjava.combox.Combox;
import popjava.combox.ComboxAllocate;
import popjava.combox.ComboxFactory;
import popjava.combox.ComboxServer;
import popjava.util.Configuration;
import popjava.util.LogWriter;

/**
 * This class is the factory for all combox socket
 */
public class ComboxSecureSocketFactory extends ComboxFactory {
	
	/**
	 * Name of the implemented protocol
	 */
	public static final String PROTOCOL = "ssl";
	private static final Configuration conf = Configuration.getInstance();
	private static File sslKeyStoreFile = conf.getSSLKeyStoreFile();
	private static String sslKeyStorePassword = conf.getSSLKeyStorePassword();
	private static Status status = Status.UNKNOW;
	
	private enum Status {
		UNKNOW,
		AVAILABLE,
		NOT_AVAILABLE
	}

	@Override
	public String getComboxName() {
		return PROTOCOL;
	}

	@Override
	public Combox createClientCombox(String networkUUID) {
		return new ComboxSecureSocket(networkUUID);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			POPBuffer buffer, Broker broker) throws IOException {
		return createServerCombox(accessPoint, conf.getConnectionTimeout(), buffer, broker);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			int timeout, POPBuffer buffer, Broker broker) throws IOException {
		return new ComboxServerSecureSocket(accessPoint, timeout, buffer, broker);
	}

	@Override
	public ComboxAllocate createAllocateCombox() {
		return new ComboxAllocateSecureSocket();
	}

	@Override
	public boolean isAvailable() {
		if (!super.isAvailable()) {
			return false;
		}
		if (status == Status.UNKNOW 
			|| sslKeyStoreFile != conf.getSSLKeyStoreFile()
			|| sslKeyStorePassword != conf.getSSLKeyStorePassword()) {
			try {
				sslKeyStoreFile = conf.getSSLKeyStoreFile();
				sslKeyStorePassword = conf.getSSLKeyStorePassword();
				
				KeyStore keyStore = KeyStore.getInstance(conf.getSSLKeyStoreFormat().name());
				keyStore.load(new FileInputStream(sslKeyStoreFile), sslKeyStorePassword.toCharArray());
				
				status = Status.AVAILABLE;
			} catch (Exception e) {
				LogWriter.writeDebugInfo("[SSL Combox] can't be initialized correctly: %s", e.getMessage());
				status = Status.NOT_AVAILABLE;
			}
		}
		switch (status) {
			case AVAILABLE:
				return true;
			case NOT_AVAILABLE:
			case UNKNOW:
			default:
				return false;
		}
	}

	@Override
	public boolean isSecure() {
		return true;
	}
}
