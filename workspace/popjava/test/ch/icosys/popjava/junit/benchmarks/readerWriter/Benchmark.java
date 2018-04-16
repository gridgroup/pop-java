package ch.icosys.popjava.junit.benchmarks.readerWriter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import ch.icosys.popjava.core.PopJava;
import ch.icosys.popjava.core.system.POPSystem;

public class Benchmark {

	private static final int TOTAL = 20000;
	
	@Test
	public void test(){
		long start = System.currentTimeMillis();
		
		POPSystem.initialize();
		System.out.println("POPSystem initialized after "+(System.currentTimeMillis() - start)+" ms");
		
		int workerCount = 5;
		
		Writer writer = PopJava.newActive(this, Writer.class, "localhost", TOTAL);
		System.out.println("Writer created after "+(System.currentTimeMillis() - start)+" ms");
		
		List<Worker> workers = new ArrayList<>();
		
		for(int i = 0; i < workerCount; i++){
			workers.add(PopJava.newActive(this, Worker.class, "localhost", writer));
		}
		
		System.out.println("Workers created after "+(System.currentTimeMillis() - start)+" ms");
		
		Reader reader = new Reader(workers, TOTAL);
		reader.work();
		writer.join();
		assertEquals(TOTAL, writer.getWritten());
		System.out.println("Finished after "+(System.currentTimeMillis() - start)+" ms");
		POPSystem.end();
		
		System.out.println("Closed POPSystem after "+(System.currentTimeMillis() - start)+" ms");
	}
	
}
