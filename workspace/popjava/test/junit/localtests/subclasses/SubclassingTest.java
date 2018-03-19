package junit.localtests.subclasses;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import popjava.PopJava;
import popjava.system.POPSystem;

public class SubclassingTest {

	@Test
	@Ignore
	public void test(){
		POPSystem.initialize();
		
		D d = PopJava.newActive(this, C.class);
		
		A a = d.getTest();
		
		assertNotNull(a);

		assertEquals("asdf", a.a());
		
		POPSystem.end();
	}
	
}
