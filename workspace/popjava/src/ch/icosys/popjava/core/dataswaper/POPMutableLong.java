package ch.icosys.popjava.core.dataswaper;

import ch.icosys.popjava.core.buffer.POPBuffer;

/**
 * Primitive settable long for POPJava, needed for
 * {@link ch.icosys.popjava.core.annotation.POPParameter} direction use in
 * methods
 * 
 * @author Davide Mazzoleni
 */
public class POPMutableLong implements IPOPBase {
	/**
	 * long value stored in this object
	 */
	private long value;

	/**
	 * Default constructor
	 */
	public POPMutableLong() {
		value = 0;
	}

	/**
	 * Constructor with given value
	 * 
	 * @param value
	 *            long value to be stored in this object
	 */
	public POPMutableLong(long value) {
		this.value = value;
	}

	/**
	 * Set the long value of this object
	 * 
	 * @param value
	 *            new long value
	 */
	public void setValue(long value) {
		this.value = value;
	}

	/**
	 * Get the current value of this object
	 * 
	 * @return current long value
	 */
	public long getValue() {
		return value;
	}

	/**
	 * Serialize the POPLong into the buffer
	 */
	@Override
	public boolean serialize(POPBuffer buffer) {
		buffer.putLong(value);
		return false;
	}

	/**
	 * Deserialize the POPLong from the buffer
	 */
	@Override
	public boolean deserialize(POPBuffer buffer) {
		value = buffer.getLong();
		return false;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
