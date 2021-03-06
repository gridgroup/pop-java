import ch.icosys.popjava.core.annotation.POPClass;

@POPClass(isDistributable = false)
public class CharTestMain {
	public static void main(String[] args) {
		System.out.println("Char Array test started ...");
		PARObject pa = new PARObject();
		char[] tab = { 'P', 'O', 'P', '-', 'J', 'a', 'v', 'a' };
		char[] tab2 = { 'P', 'O', 'P', '-', 'J', 'a', 'v', 'a' };
		System.out.println("Before "+new String(tab));
		pa.sendChar(tab.length, tab, tab2.length, tab2);
		System.out.println("After "+new String(tab));
	}
}
