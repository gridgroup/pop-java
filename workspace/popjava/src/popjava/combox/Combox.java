package popjava.combox;


import popjava.baseobject.POPAccessPoint;
import popjava.buffer.BufferFactory;
import popjava.buffer.BufferFactoryFinder;
import popjava.buffer.POPBuffer;
import popjava.util.Configuration;
import popjava.util.POPRemoteCaller;
/**
 * This class is the base implementation for all Combox in the POP-Java library
 * All other combox must inherit from this class
 */
public abstract class Combox {
	
	protected int timeOut = 0;
	protected POPAccessPoint accessPoint;
	protected boolean available = false;
	protected BufferFactory bufferFactory;
	
	protected String networkUUID;
	
	protected POPRemoteCaller remoteCaller;
	
	protected final Configuration conf = Configuration.getInstance();

	/**
	 * Constructor for client Combox
	 * @param networkUUID	The network UUID that will be send to the other end
	 */
	public Combox(String networkUUID) {
		this.networkUUID = networkUUID;
		this.accessPoint = new POPAccessPoint();
		bufferFactory = BufferFactoryFinder.getInstance().findFactory(conf.getDefaultEncoding());
	}

	/**
	 * Connect the current combox to the other side combox
	 * @param accesspoint	Access point of the other side combox
	 * @param timeout		Connection time out
	 * @return true if the connection is established
	 */
	public final boolean connect(POPAccessPoint accesspoint, int timeout) {
		this.accessPoint = accesspoint;
		this.timeOut = timeout;
		boolean status = connect();
		
		return status;
	}

	/**
	 * Send the buffer to the other side
	 * @param buffer	The buffer to send
	 * @return	Number of byte sent
	 */
	public abstract int send(POPBuffer buffer);

	/**
	 * Receive buffer from the other side
	 * @param buffer	Buffer to receive
	 * @return	Number of byte received
	 */
	public abstract int receive(POPBuffer buffer, int requestId);

	/**
	 * Close the connection
	 */
	public abstract void close();

	/**
	 * Connect to the other side
	 * @return	true if the connection succeed
	 */
	protected abstract boolean connect();

	/**
	 * Associate a buffer factory to the combox
	 * @param bufferFactory	The buffer factory to associate
	 */
	public void setBufferFactory(BufferFactory bufferFactory) {		
		this.bufferFactory = bufferFactory;
	}

	/**
	 * Get the associated buffer factory
	 * @return	The associated buffer factory
	 */
	public BufferFactory getBufferFactory() {
		return bufferFactory;
	}

	/**
	 * Return the access point we are connected to
	 * @return 
	 */
	public POPAccessPoint getAccessPoint() {
		return accessPoint;
	}

	/**
	 * Information about who we are talking too
	 * @return 
	 */
	public POPRemoteCaller getRemoteCaller() {
		return remoteCaller;
	}

	/**
	 * The network we are connecting or are connected to.
	 * @return 
	 */
	public String getNetworkUUID() {
		return networkUUID;
	}
}
