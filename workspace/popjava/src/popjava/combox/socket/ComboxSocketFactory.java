package popjava.combox.socket;

import popjava.baseobject.AccessPoint;
import popjava.baseobject.POPAccessPoint;
import popjava.broker.Broker;
import popjava.buffer.POPBuffer;
import popjava.combox.Combox;
import popjava.combox.ComboxAllocate;
import popjava.combox.ComboxFactory;
import popjava.combox.ComboxServer;
import popjava.util.Configuration;

/**
 * This class is the factory for all combox socket
 */
public class ComboxSocketFactory extends ComboxFactory {
	/**
	 * Name of the implemented protocol
	 */
	public static final String PROTOCOL = "socket";
	private static final Configuration conf = Configuration.getInstance();

	@Override
	public String getComboxName() {
		return PROTOCOL;
	}

	@Override
	public Combox createClientCombox(POPAccessPoint accessPoint) {
		return createClientCombox(accessPoint, conf.getConnectionTimeout());
	}

	@Override
	public Combox createClientCombox(POPAccessPoint accessPoint, int timeout) {
		return new ComboxSocket(accessPoint, timeout);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			POPBuffer buffer, Broker broker) {
		return createServerCombox(accessPoint, conf.getConnectionTimeout(), buffer, broker);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			int timeout, POPBuffer buffer, Broker broker) {
		return new ComboxServerSocket(accessPoint, timeout, buffer, broker);
	}

	@Override
	public ComboxAllocate createAllocateCombox() {
		return new ComboxAllocateSocket();
	}

	@Override
	public boolean isAvailable() {
		return super.isAvailable();
	}

	@Override
	public boolean isSecure() {
		return false;
	}

}