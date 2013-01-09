package popjava.codemanager;

import java.util.HashMap;
import java.util.Map;

import popjava.base.POPObject;
import popjava.base.Semantic;
import popjava.dataswaper.POPString;
import popjava.system.POPJavaConfiguration;
import popjava.util.LogWriter;

public class POPJavaAppService extends POPObject implements AppService{
	
	private static final String ALL_PLATFORMS = "*-*";
	
	public POPJavaAppService() {
		this.setClassId(99923);
		this.hasDestructor(false);
		od.setHostname("localhost");
		Class<?> c = this.getClass();
		initializePOPObject(c);
		this.definedMethodId = true;
		addSemantic(c, "registerCode", Semantic.Sequence | Semantic.Synchronous);
		addSemantic(c,"queryCode", Semantic.Sequence | Semantic.Synchronous);
		addSemantic(c,"getPlatform", Semantic.Sequence | Semantic.Synchronous);
		addSemantic(c,"getPOPCAppID", Semantic.Sequence | Semantic.Synchronous);
	}

	//Platform, objectname, codefile
	private Map<String, Map<String, String>> registeredCode =
			new HashMap<String, Map<String,String>>();
	
	/**
	 * Register a executable code file in the CodeMgr service
	 * @param objname	Name of the parallel object
	 * @param platform	Platform of the executable
	 * @param codefile	Path of the executable code file
	 */
	public void registerCode(String objname, String platform, String codefile) {
		Map<String, String> platf = registeredCode.get(platform);
		if(platf == null){
			platf = new HashMap<String, String>();
			registeredCode.put(platform, platf);
		}
		platf.put(objname, codefile);
	}

	/**
	 * Query the CodeMgr to retrieve the code file for a specific object on a specific architecture
	 * @param objname	Name of the object
	 * @param platform	Platform desired
	 * @param codefile	Output argument - code file for the specific object and the specific platform
	 * @return	0 if the code file is not available
	 */
	public int queryCode(String objname, String platform, POPString codefile) {
		Map<String, String> platf = registeredCode.get(platform);
		String storeCodeFile = null;
		
		if(platf == null){
			if(!platform.equals(ALL_PLATFORMS)){
				return queryCode(objname, ALL_PLATFORMS, codefile);
			}
			LogWriter.writeDebugInfo("Platform not found");
		}else{
			storeCodeFile = platf.get(objname);
			
		}
		
		if(storeCodeFile == null){
			storeCodeFile = getLocalJavaFileLocation(objname);
		}
		
		if(storeCodeFile == null){
			return 0;
		}
		
		codefile.setValue(storeCodeFile);
		
		return 1;
	}
	
	public String getLocalJavaFileLocation(String objname){
		String codePath = null;
		try{
			ClassLoader classloader = this.getClass().getClassLoader();
			Class<?> javaClass = classloader.loadClass(objname);
			
			codePath = String.format(
					POPJavaConfiguration.getBrokerCommand(),
					POPJavaConfiguration.getPOPJavaCodePath()) + 
					javaClass.getProtectionDomain().getCodeSource().getLocation().getPath();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return codePath;
	}

	/**
	 * Query the CodeMgr to know the platforms of a specific object
	 * @param objname	Name of the object
	 * @param platform	Output argument - platform available for the object
	 * @return	number of platform available
	 */
	public int getPlatform(String objname, POPString platform) {
		return 0;
	}
	
	public String getPOPCAppID(){
		return "PopJavaApp";
	}
	
}