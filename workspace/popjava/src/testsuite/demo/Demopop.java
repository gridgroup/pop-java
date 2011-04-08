package testsuite.demo;

import popjava.base.POPObject;
import popjava.base.Semantic;

public class Demopop extends POPObject {
	
	int iD = 11;
	
	public Demopop(){
		Class<?> c = Demopop.class;
		initializePOPObject(c);
		addSemantic(c, "sendIDto", Semantic.Asynchronous | Semantic.Sequence);
		addSemantic(c, "getID", Semantic.Synchronous | Semantic.Concurrent);
		addSemantic(c, "recAnId", Semantic.Asynchronous | Semantic.Concurrent);
		addSemantic(c, "wait", Semantic.Synchronous | Semantic.Mutex);
	}
	
	public Demopop(int newID, int wanted, int minp){
		Class<?> c = Demopop.class;
		od.setPower(wanted, minp);
		initializePOPObject(c);
//		printf("POPCobject with ID=%d created (by JobMgr) on machine:%s\n", newID, (const char*)POPSystem::GetHost());
		addSemantic(c, "sendIDto", Semantic.Asynchronous | Semantic.Sequence);
		addSemantic(c, "getID", Semantic.Synchronous | Semantic.Concurrent);
		addSemantic(c, "recAnId", Semantic.Asynchronous | Semantic.Concurrent);
		addSemantic(c, "wait", Semantic.Synchronous | Semantic.Mutex);
		iD=newID;
		System.out.println("Demopop with ID="+iD+" created (byJobMgr) on machine:"+getAccessPoint().toString());
	}

	public Demopop(int newID, String machine){
		Class<?> c = Demopop.class;
		od.setHostname(machine);
		initializePOPObject(c);
//		printf("POPCobject with ID=%d created on machine:%s\n", newID, (const char*)POPSystem::GetHost());
		addSemantic(c, "sendIDto", Semantic.Asynchronous | Semantic.Sequence);
		addSemantic(c, "getID", Semantic.Synchronous | Semantic.Concurrent);
		addSemantic(c, "recAnId", Semantic.Asynchronous | Semantic.Concurrent);
		addSemantic(c, "wait", Semantic.Synchronous | Semantic.Mutex);
		iD=newID;
		System.out.println("Demopop with ID="+iD+" created on machine:"+getAccessPoint().toString());
	}

	
	public void sendIDto(Demopop o)
	{
//		printf("POPCobject:%d on machine:%s is sending his iD to object:%d\n", iD, (const char*)POPSystem::GetHost(), o.getID());
		o.recAnID(iD);
	}

	public int getID()	{
		return iD;
	}

	public void recAnID(int i)
	{
//		printf("POPCobject:%d on machine:%s is receiving id = %d\n", iD, (const char*)POPSystem::GetHost(), i);
	}

	public void wait(int sec)
	{
//		printf("POPCobject:%d on machine:%s is waiting %d sec.\n", iD, (const char*)POPSystem::GetHost(), sec);
		try {
			Thread.sleep(sec*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
