package popjava.combox.ssl;

import popjava.combox.*;
import popjava.baseobject.AccessPoint;
import popjava.baseobject.POPAccessPoint;
import popjava.broker.Broker;
import popjava.buffer.POPBuffer;

/**
 * This class is the factory for all combox socket
 */
public class ComboxSecureSocketFactory extends ComboxFactory {
	/**
	 * Name of the implemented protocol
	 */
	public static final String PROTOCOL = "ssl";

	@Override
	public String getComboxName() {
		return PROTOCOL;
	}

	@Override
	public Combox createClientCombox(POPAccessPoint accessPoint) {
		return new ComboxSecureSocket(accessPoint, 0);
	}

	@Override
	public Combox createClientCombox(POPAccessPoint accessPoint, int timeout) {
		return new ComboxSecureSocket(accessPoint, timeout);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			POPBuffer buffer, Broker broker) {
		return new ComboxServerSecureSocket(accessPoint, 0, buffer, broker);
	}

	@Override
	public ComboxServer createServerCombox(AccessPoint accessPoint,
			int timeout, POPBuffer buffer, Broker broker) {
		return new ComboxServerSecureSocket(accessPoint, timeout, buffer, broker);
	}

	@Override
	public ComboxAllocate createAllocateCombox() {
		return new ComboxAllocateSecureSocket();
	}

}