package junit.localtests.popjava;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import popjava.PopJava;
import popjava.system.POPSystem;

public class POPJavaTests {
	
	private static class TestClass{
		TestObject temp;
	}

	@Test(expected = Exception.class)
	public void testExit() {
		POPSystem.initialize();
		TestObject test = PopJava.newActive(this, TestObject.class);

		assertEquals(1234, test.test());

		PopJava.destroy(test);

		assertEquals(0, test.test());

		POPSystem.end();
	}

	@Test(expected = Exception.class)
	public void testClose() throws InterruptedException {
		POPSystem.initialize();
		
		TestObject test = PopJava.newActive(this, TestObject.class);

		assertEquals(1234, test.test());

		PopJava.disconnect(test);

		assertEquals(0, test.test());

		POPSystem.end();
	}
}
