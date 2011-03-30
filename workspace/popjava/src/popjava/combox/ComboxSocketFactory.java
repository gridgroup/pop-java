package popjava.combox;

import popjava.baseobject.AccessPoint;
import popjava.baseobject.POPAccessPoint;
import popjava.broker.Broker;
import popjava.buffer.Buffer;

/**
 * This class is the factory for all combox socket
 */
public class ComboxSocketFactory extends ComboxFactory {
	/**
	 * Name of the implemented protocol
	 */
	public static final String Protocol = "socket";

	@Override
	public String getComboxName() {
		return Protocol;
	}

	@Override
	public Combox createClientCombox(POPAccessPoint accessPoint) {
		return new ComboxSocket(accessPoint, 0);
	}

	@Override
	public Combox createClientCombox(POPAccessPoint accessPoint, int timeout) {
		return new ComboxSocket(accessPoint, timeout);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			Buffer buffer, Broker broker) {
		return new ComboxServerSocket(accessPoint, 0, buffer, broker);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			int timeout, Buffer buffer, Broker broker) {
		return new ComboxServerSocket(accessPoint, timeout, buffer, broker);
	}

}
