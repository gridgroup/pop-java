package ch.icosys.popjava.junit.localtests.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.icosys.popjava.core.PopJava;
import ch.icosys.popjava.core.system.POPSystem;

public class TestConcurrency {

	@Test
	public void test() {
		POPSystem.initialize();

		ParallelObject object = PopJava.newActive(this, ParallelObject.class);

		object.seq();
		object.mutex();
		object.seq();
		object.conc();

		boolean correct = object.success(4);

		POPSystem.end();

		assertTrue(correct);
	}

	@Test
	public void testConcurrentSleep() {
		POPSystem.initialize();

		ParallelObject object = PopJava.newActive(this, ParallelObject.class);

		long start = System.currentTimeMillis();
		object.concSleep();
		object.concSleep();
		object.concSleep();
		object.concSleep();
		object.concSleepEnd(4);

		long time = System.currentTimeMillis() - start;
		POPSystem.end();

		assertTrue(time < 3000);
	}

	@Test
	public void testThreadedConcurrency() throws InterruptedException {
		POPSystem.initialize();

		final ParallelObject object = PopJava.newActive(this, ParallelObject.class);

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					@SuppressWarnings("unused")
					int ret = object.ping(1500, 20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		for (int i = 0; i < 15; i++) {
			if (i == 1) {
				System.out.println("asdf");
			}
			int value = object.ping(0, i);

			System.out.println("Got " + value + " for " + i);
			assertEquals(i, value);
		}

		POPSystem.end();
	}
}
