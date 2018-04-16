package ch.icosys.popjava.core.baseobject;

import ch.icosys.popjava.core.buffer.POPBuffer;
import ch.icosys.popjava.core.dataswaper.IPOPBase;

/**
 * This class keep track of the calls toward a specific method.
 * 
 * @author Davide Mazzoleni
 */
public class POPTrackingMethod implements IPOPBase {

	private String method;
	private long timeUsed;
	private long calls;

	public POPTrackingMethod() {
		this(null);
	}

	POPTrackingMethod(String method) {
		this.method = method;
	}

	/**
	 * Register a new method call.
	 * @param time how much did it takes
	 */
	public synchronized void increment(long time) {
		calls++;
		timeUsed += time;
	}

	/**
	 * The total time used by this method in milliseconds.
	 * @return total time used
	 */
	public long getTimeUsed() {
		return timeUsed;
	}

	/**
	 * The method used.
	 * @return the method name
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Number of calls to this method.
	 * @return number of calls to the method
	 */
	public long getNumCalls() {
		return calls;
	}

	@Override
	public boolean serialize(POPBuffer buffer) {
		buffer.putString(method);
		buffer.putLong(timeUsed);
		buffer.putLong(calls);
		return true;
	}

	@Override
	public boolean deserialize(POPBuffer buffer) {
		method = buffer.getString();
		timeUsed = buffer.getLong();
		calls = buffer.getLong();
		return true;
	}

	@Override
	public String toString() {
		return String.format("%d (%d ms) %s", calls, timeUsed, method);
	}
}
