package ch.icosys.popjava.core.dataswaper;

import ch.icosys.popjava.core.buffer.POPBuffer;
/**
 * Primitive settable short for POPJava, needed for {@link ch.icosys.popjava.core.annotation.POPParameter} direction use in methods 
 * @author Davide Mazzoleni
 */
public class POPMutableShort implements IPOPBase {
	/**
	 * short value stored in this object
	 */
	private short value;

	/**
	 * Default constructor
	 */
	public POPMutableShort() {
		value = 0;
	}

	/**
	 * Constructor with given value
	 * @param value	short value to be stored in this object
	 */
	public POPMutableShort(short value) {
		this.value = value;
	}
	
	/**
	 * Set the short value of this object
	 * @param value	new short value
	 */
	public void setValue(short value)
	{
		this.value=value;
	}

	/**
	 * Set the short value of this object, from an int
	 * @param value	new short value from int
	 */
	public void setValue(int value) {
		this.value=(short)value;
	}

	/**
	 * Get the current value of this object
	 * @return	current short value
	 */
	public short getValue() {
		return value;
	}

	/**
	 * Serialize the POPShort into the buffer
	 */
	@Override
	public boolean serialize(POPBuffer buffer) {
		buffer.putShort(value);
		return false;
	}

	/**
	 * Deserialize the POPShort from the buffer
	 */
	@Override
	public boolean deserialize(POPBuffer buffer) {
		value = buffer.getShort();
		return false;
	}
	
	@Override
	public String toString(){
		return String.valueOf(value);
	}
}
