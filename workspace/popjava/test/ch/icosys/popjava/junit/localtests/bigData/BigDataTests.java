package ch.icosys.popjava.junit.localtests.bigData;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import ch.icosys.popjava.core.PopJava;
import ch.icosys.popjava.core.system.POPSystem;

public class BigDataTests {

    @Test
    @Ignore
    public void testBigIntArray(){
        POPSystem.initialize();
        BigDataObject test = PopJava.newActive(this, BigDataObject.class);
        
        for(int i = 100000; i < (Integer.MAX_VALUE - 10000) / 8; i += 10000000){
        	assertEquals(i, test.arrayTest(new int[i]));
        }
        
        POPSystem.end();
        
    }
    
    @Test
    @Ignore
    public void testBigCharArray(){
        POPSystem.initialize();
        BigDataObject test = PopJava.newActive(this, BigDataObject.class);
        
        for(int i = 100000; i < (Integer.MAX_VALUE - 10000) / 8; i += 10000000){
        	char [] temp = new char[i];
        	Arrays.fill(temp, 'a');
        	assertEquals(i, test.arrayTest(temp));
        }
        
        POPSystem.end();
        
    }
    
    @Test
    @Ignore
    public void testBigByteArray(){
        POPSystem.initialize();
        BigDataObject test = PopJava.newActive(this, BigDataObject.class);
        
        for(int i = 100000; i < (Integer.MAX_VALUE - 10000) / 8; i += 10000000){
        	assertEquals(i, test.arrayTest(new byte[i]));
        }
        
        POPSystem.end();
        
    }
    
    @Test
    @Ignore
    public void testBigStringArray(){
        POPSystem.initialize();
        BigDataObject test = PopJava.newActive(this, BigDataObject.class);
        
        for(int i = 100000; i < (Integer.MAX_VALUE - 10000) / 8; i += 10000000){
        	char [] temp = new char[i];
        	Arrays.fill(temp, 'a');
        	assertEquals(i, test.arrayTest(new String(temp)));
        }
        
        POPSystem.end();
        
    }
}
