import java.lang.*;

import popjava.annotation.POPClass;

@POPClass(isDistributable = false)
public class MainClass{
   public static void main(String[] args){
      System.out.println("Start broker-side semantic test ...");
      System.out.println("-- Sequential call --");
      MethodObj mo = new MethodObj();
      mo.setSeq(1);
      mo.setSeq(2);
      mo.setSeq(3);
      mo.setSeq(4);

      int i=0;

      while(i<4){
         int crt = mo.get();
         if(crt != i)
            System.out.println("Sequential call failed");
         else
            System.out.println("Value should be " + crt +" // Value is "+ crt);
         try{
            Thread.sleep(1000);
         } catch(InterruptedException e){

         }
         i++;
      }
      System.out.println("Value should be " + 4 +" // Value is "+ mo.get());

      System.out.println("-- Concurrent call --");
      mo.setConc(5);
      mo.setConc(6);
      mo.setConc(7);
      mo.setConc(8);
      
      try{
         Thread.sleep(1500);
      } catch(InterruptedException e){

      }

      int crt2 = mo.get();
      if(crt2 != 8){
         System.out.println("Concurrent call failed"+crt2);
      }else{
         System.out.println("Value should be " + 8 +" // Value is " + crt2);
      }
      

      System.out.println("-- Mutex call --");
      mo.setMutex(10);
      mo.setConc(20);

      try{
         Thread.sleep(1500);
      } catch(InterruptedException e){

      }
      
      int crt3 = mo.get();
      if(crt3 != 10)
         System.out.println("Mutex call failed");
      else 
         System.out.println("Value should be 10 // Value is "+crt3);

      
       try{
         Thread.sleep(3000);
      } catch(InterruptedException e){

      }

      int crt4 = mo.get();
      if(crt4 != 20)
         System.out.println("Mutex call failed");
      else 
         System.out.println("Value should be 20 // Value is "+crt4);

      System.out.println("Broker-side semantic test ended...");
   }
}
