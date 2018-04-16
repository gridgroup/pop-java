package popjava.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javassist.util.proxy.ProxyObject;
import popjava.PopJava;
import popjava.annotation.POPAsyncConc;
import popjava.annotation.POPAsyncMutex;
import popjava.annotation.POPAsyncSeq;
import popjava.annotation.POPClass;
import popjava.annotation.POPConfig;
import popjava.annotation.POPPrivate;
import popjava.annotation.POPObjectDescription;
import popjava.annotation.POPSyncConc;
import popjava.annotation.POPSyncMutex;
import popjava.annotation.POPSyncSeq;
import popjava.baseobject.ConnectionType;
import popjava.baseobject.ObjectDescription;
import popjava.baseobject.POPAccessPoint;
import popjava.baseobject.POPTracking;
import popjava.broker.Broker;
import popjava.buffer.POPBuffer;
import popjava.combox.Combox;
import popjava.util.ssl.SSLUtils;
import popjava.dataswaper.IPOPBase;
import popjava.util.ClassUtil;
import popjava.util.LogWriter;
import popjava.util.MethodUtil;
import popjava.util.POPRemoteCaller;
/**
 * This class is the base class of all POP-Java parallel classes. Every POP-Java parallel classes must inherit from this one.
 */
public class POPObject implements IPOPBase {
	
	protected int refCount;
	private int classId = 0;
	protected boolean generateClassId = true;
	protected boolean definedMethodId = false;
	private boolean hasDestructor = false;	
	protected ObjectDescription od = new ObjectDescription();
	private String className = "";
	private final ConcurrentHashMap<MethodInfo, Integer> semantics = new ConcurrentHashMap<>();
	private final HashMap<MethodInfo, Method> methodInfos = new HashMap<>();
	private final HashMap<Method, MethodInfo> reverseMethodInfos = new HashMap<>();
	private final HashMap<MethodInfo, Constructor<?>> constructorInfos = new HashMap<>();
	private final HashMap<Constructor<?>, MethodInfo> reverseConstructorInfos = new HashMap<>();

	private boolean temporary = false;
	
	private POPObject me = null; //This cache
    
    private Broker broker = null;
	
	/**
	 * Creates a new instance of POPObject
	 */
	public POPObject() {
		refCount = 0;
		className = getRealClass().getName();
		
		loadClassAnnotations();
		initializePOPObject();
	}

	@SuppressWarnings("unchecked")
	private Class<? extends POPObject> getRealClass(){
		if(this instanceof ProxyObject){
			return (Class<? extends POPObject>) getClass().getSuperclass();
		}
		
		return getClass();
	}
	
	private void loadClassAnnotations(){
		for (Annotation annotation : getRealClass().getDeclaredAnnotations()) {
			if(annotation instanceof POPClass){
				POPClass popClassAnnotation = (POPClass) annotation;
				if(!popClassAnnotation.className().isEmpty()){
					setClassName(popClassAnnotation.className());
				}
				if(popClassAnnotation.classId() != -1){
					setClassId(popClassAnnotation.classId());
				}
				hasDestructor(popClassAnnotation.deconstructor());
			}
		}
	}
	
	/**
	 * Loads the OD from the specified constructor
	 * @param constructor the called constructor
	 */
	private void loadODAnnotations(Constructor<?> constructor){
		POPObjectDescription objectDescription = constructor.getAnnotation(POPObjectDescription.class);
		if(objectDescription != null){
			od.setHostname(objectDescription.url());
			od.setJVMParamters(objectDescription.jvmParameters());
			od.setConnectionType(objectDescription.connection());
			od.setConnectionSecret(objectDescription.connectionSecret());
			od.setEncoding(objectDescription.encoding().toString());
			od.setProtocols(objectDescription.protocols());
			od.setNetwork(objectDescription.network());
			od.setConnector(objectDescription.connector());
			od.setPower(objectDescription.power(), objectDescription.minPower());
			od.setMemory(objectDescription.memory(), objectDescription.minMemory());
			od.setBandwidth(objectDescription.bandwidth(), objectDescription.minBandwidth());
			// TODO size (-1) is not implemented, may want to add it to POPObjectDescription
			od.setSearch(objectDescription.searchDepth(), -1, objectDescription.searchTime());
			od.setUseLocalJVM(objectDescription.localJVM());
			od.setTracking(objectDescription.tracking());
			od.setUPNP(objectDescription.upnp());
		}
	}
	
	private void loadParameterAnnotations(Constructor<?> constructor, Object ... argvs){
		Annotation [][] annotations = constructor.getParameterAnnotations();
		for(int i = 0; i < annotations.length; i++){
			for(int loop = 0; loop < annotations[i].length; loop++){
				if(annotations[i][loop].annotationType().equals(POPConfig.class)){
					POPConfig config = (POPConfig)annotations[i][loop];
					 
					if(argvs[i] == null){
						throw new InvalidParameterException("Annotated paramater "+i+" for "+getClassName()+" is null");
					}
					
					switch(config.value()){
					case URL:
						if(argvs[i] instanceof String){
							od.setHostname((String)argvs[i]);
						}else{
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type String for Annotation "+config.value().name());
						}
						
						break;
					case CONNECTION:
						if(argvs[i] instanceof ConnectionType){
							od.setConnectionType((ConnectionType) argvs[i]);
						}else{
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type ConnectionType for Annotation "+config.value().name());
						}
						break;
					case CONNECTION_PWD:
						if(argvs[i] instanceof String){
							od.setConnectionSecret((String)argvs[i]);
						}else{
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type String for Annotation "+config.value().name());
						}
						break;
					case ACCESS_POINT:
						if(argvs[i]  instanceof String){
							od.setRemoteAccessPoint((String)argvs[i]);
						}else{
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type String for Annotation "+config.value().name());
						}
						break;
					case LOCAL_JVM:
						if(argvs[i]  instanceof Boolean){
							od.setUseLocalJVM((Boolean)argvs[i]);
						}else{
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type Boolean for Annotation  "+config.value().name());
						}
						break;
					case UPNP:
						if(argvs[i]  instanceof Boolean){
							od.setUPNP((Boolean)argvs[i]);
						}else{
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type Boolean for Annotation "+config.value().name());
						}
						break;
					case PROTOCOLS:
						if (argvs[i] instanceof String) {
							od.setProtocols(new String[] { (String) argvs[i] });
						}
						else if (argvs[i] instanceof String[]) {
							od.setProtocols((String[]) argvs[i]);
						}
						else {
							throw new InvalidParameterException("Annotated paramater "+i+" in "+getClassName()+
									" was not of type String or String[] for Annotation "+config.value().name());
						}
						break;
					}
					
				}
			}
		}
	}
	
	/**
	 * Loads the OD from the annotated attributes
	 */
	private void loadDynamicOD(Constructor<?> constructor, Object ... argvs){
		loadODAnnotations(constructor);
		loadParameterAnnotations(constructor, argvs);
	}
	
	public void loadPOPAnnotations(Constructor<?> constructor, Object ... argvs){
		loadDynamicOD(constructor, argvs);
	}

	/**
	 * Initialize the method identifiers of a POPObject
	 */
	protected final void initializePOPObject() {
		if (generateClassId){
			classId = ClassUtil.classId(getRealClass());
		}
		
		Class<?> c = getRealClass();
		initializeConstructorInfo(c);
		initializeMethodInfo(c);
	}
	
	/**
	 * Specify if the parallel object is running like a deamon
	 * @return true if it's a deamon
	 */
	public boolean isDaemon() {
		return false;
	}

	/**
	 * Ask if the object can be killed
	 * @return	true if the object can be killed
	 */
	public final boolean canKill() {
		return true;
	}

	/**
	 * Get the object description of the POPObject
	 * @return the object description of the POPObject
	 */
	public final ObjectDescription getOd() {
		return od;
	}

	/**
	 * Set a new object description to the POPObject
	 * @param od	the new object description
	 */
	public final void setOd(ObjectDescription od) {
		this.od = od;
	}

	/**
	 * Retrieve the access point of the parallel object
	 * @return	POPAccessPoint object containing all access points to the parallel object
	 */
	public POPAccessPoint getAccessPoint() {
		if(broker == null){
			throw new RuntimeException("Can not pass object as parameter before it has been initialized");
		}
		return broker.getAccessPoint();
	}
	
	public Broker getBroker() {
		return broker;
	}

	/**
	 * Retrieve the class name of the parallel object
	 * @return	class name as a String value
	 */
	public final String getClassName() {
		return className;
	}

	/**
	 * Set the class name
	 * @param className	the class name
	 */
	protected final void setClassName(String className) {
		this.className = className;
	}

	/**
	 * Return the value of the hasDestrcutor variable
	 * @return	true if the parclass has a destrcutor
	 */
	protected final boolean hasDestructor() {
		return hasDestructor;
	}

	/**
	 * Set the destructor value. Must be set to true if the parclass has a destructor
	 * @param hasDestructor	set to true if the parclass has a destructor
	 */
	protected final void hasDestructor(boolean hasDestructor) {
		this.hasDestructor = hasDestructor;
	}

	/**
	 * Get the class unique identifier
	 * @return the class unique identifier
	 */
	public final int getClassId() {
		return classId;
	}

	/**
	 * Set the class unique identifier
	 * @param classId	the class unique identifier
	 */
	protected final void setClassId(int classId) {
		generateClassId = false;
		this.classId = classId;
	}

	/**
	 * Retrieve a specific method in the parallel class with some information
	 * @param info	informations about the method to retrieve
	 * @return	A method object that represent the method found in the parallel class
	 * @throws NoSuchMethodException	thrown is the method is not found
	 */
	public Method getMethodByInfo(MethodInfo info) throws NoSuchMethodException {
		Method method = methodInfos.get(info);
		
		/*if (method != null) {
			method = findSuperMethod(method);
		}*/
		
		if (method == null) {
			for(MethodInfo key : methodInfos.keySet()){
				System.out.println(key.getClassId()+" "+key.getMethodId()+" "+methodInfos.get(key).getName());
			}
			
			throw new NoSuchMethodException();
		}
		
		return method;
	}

	/**
	 * Retrieve a constructor by its informations
	 * @param info	Informations about the constructor to retrieve
	 * @return	The constructor found
	 * @throws NoSuchMethodException	thrown if no constrcutor is found
	 */
	public Constructor<?> getConstructorByInfo(MethodInfo info)
			throws NoSuchMethodException {
		Constructor<?> c = constructorInfos.get(info);
		if (c == null) {
			throw new NoSuchMethodException();
		}
		return c;
	}

	/**
	 * Retrieve a method by its informations
	 * @param method	Informations about the method to retrieve
	 * @return	The method found
	 */
	public MethodInfo getMethodInfo(Method method) {
		return reverseMethodInfos.getOrDefault(method, new MethodInfo(0, 0));
	}

	/**
	 * Retrieve a specific method by its constructor informations
	 * @param constructor	Informations about the constructor
	 * @return	The method found
	 */
	public MethodInfo getMethodInfo(Constructor<?> constructor) {	    
		MethodInfo c = reverseConstructorInfos.get(constructor);
		if (c == null) {
			throw new RuntimeException("Could not find constructor " + constructor.toGenericString());
		}
		return c;
	}

	/**
	 * Retrieve the invocation semantic of a specific method
	 * @param methodInfo	informations about the specific method
	 * @return	int value representing the semantics of the method
	 */
	public int getSemantic(MethodInfo methodInfo) {
		return semantics.getOrDefault(methodInfo, Semantic.SYNCHRONOUS);
	}

	/**
	 * Retrieve the invocation semantic of a specific method
	 * @param method	method to look at
	 * @return	int value representing the semantics of the method
	 */
	public int getSemantic(Method method) {
		MethodInfo methodInfo = getMethodInfo(method);
		return getSemantic(methodInfo);
	}

	/**
	 * Set an invocation semantic to a specific method. 
	 * @param c				class of the method
	 * @param methodName	method to modify
	 * @param semantic		semantic to set on the method
	 */
	public final void addSemantic(Class<?> c, String methodName, int semantic) {
		Method[] allMethods = c.getDeclaredMethods();
		if (allMethods.length > 0) {
			for (Method m : allMethods) {
				if (m.getName().equals(methodName)) {
					MethodInfo methodInfo = getMethodInfo(m);
					if (methodInfo.getMethodId() > 0) {
						if (semantics.containsKey(methodInfo)) {
							semantics.replace(methodInfo, semantic);
						} else {
							semantics.put(methodInfo, semantic);
						}
					}
				}
			}
		}
	}

	/**
	 * Set an invocation semantic to a specific method that is overloaded
	 * @param c					class of the method
	 * @param methodName		method to modify
	 * @param semantic			semantic to set on the method
	 * @param parameterTypes	parameters types of the method
	 * @throws java.lang.NoSuchMethodException if the method name is not found
	 */
	public final void addSemantic(Class<?> c, String methodName, int semantic,
			Class<?>... parameterTypes) throws java.lang.NoSuchMethodException {
		Method method = c.getMethod(methodName, parameterTypes);
		MethodInfo methodInfo = getMethodInfo(method);
		if (methodInfo.getMethodId() > 0) {
			if (semantics.containsKey(methodInfo)) {
				semantics.replace(methodInfo, semantic);
			} else {
				semantics.put(methodInfo, semantic);
			}
		} else {
			String errorMessage = ClassUtil.getMethodSign(method);
			throw new java.lang.NoSuchMethodException(errorMessage);
		}
	}
	
	/**
	 * Initialize the method identifier for all the methods in a class
	 * @param c	class to initialize
	 */
	protected void initializeMethodInfo(Class<?> c) {
		if (!definedMethodId) {
			// to every all class until Object (excluded)
			while (c != Object.class) {
				// get the new declared methods (this include overrode ones)
				Method[] allMethods = c.getDeclaredMethods();
				// add all method containing POP annotations to mathodInfos
				for (Method m : allMethods) {
					Class<?> declaringClass = m.getDeclaringClass();
					POPClass popClassAnnotation = declaringClass.getAnnotation(POPClass.class);
					if ((popClassAnnotation != null || declaringClass.equals(POPObject.class))
							&& Modifier.isPublic(m.getModifiers()) && MethodUtil.isMethodPOPAnnotated(m)) {
						int methodId = MethodUtil.methodId(m);
						int methodClassId = ClassUtil.classId(c);
						MethodInfo methodInfo = new MethodInfo(methodClassId, methodId);
						
						//System.out.println("___ " + methodInfo + " @ " + m.toGenericString());
						
						methodInfos.put(methodInfo, m);
						reverseMethodInfos.put(m, methodInfo);
						addMethodSemantic(methodInfo, m);
					}
				}
				// map super class
				c = c.getSuperclass();
			}
		}
	}
	
	/**
	 * Add the semantics of the given method
	 * @param mi the method identifier
	 * @param m the method
	 */
	private void addMethodSemantic(MethodInfo mi, Method m) {
		if(m.isAnnotationPresent(POPPrivate.class)) {
			return;
		}
		
		Annotation[] annotations = {
			MethodUtil.getMethodPOPAnnotation(m, POPSyncConc.class),
			MethodUtil.getMethodPOPAnnotation(m, POPSyncSeq.class),
			MethodUtil.getMethodPOPAnnotation(m, POPSyncMutex.class),
			MethodUtil.getMethodPOPAnnotation(m, POPAsyncConc.class),
			MethodUtil.getMethodPOPAnnotation(m, POPAsyncSeq.class),
			MethodUtil.getMethodPOPAnnotation(m, POPAsyncMutex.class)
		};
		
		Annotation annotation = null;
		for (Annotation ia : annotations) {
			if (Objects.isNull(ia)) {
				continue;
			}
			if (annotation != null) {
				throw new POPException(POPErrorCode.METHOD_ANNOTATION_EXCEPTION, 
					"Can not declare mutliple POP Semantics for same method " + m.toGenericString());
			}
			annotation = ia;
		}
		
		int semantic = -1;
		//Sync
		if(annotation.annotationType() == POPSyncConc.class){
			semantic = Semantic.SYNCHRONOUS | Semantic.CONCURRENT;
		}
		else if(annotation.annotationType() == POPSyncSeq.class){
			semantic = Semantic.SYNCHRONOUS | Semantic.SEQUENCE;
		}
		else if(annotation.annotationType() == POPSyncMutex.class){
			semantic = Semantic.SYNCHRONOUS | Semantic.MUTEX;
		}
		//Async
		else if(annotation.annotationType() == POPAsyncConc.class){
			semantic = Semantic.ASYNCHRONOUS | Semantic.CONCURRENT;
		}
		else if(annotation.annotationType() == POPAsyncSeq.class){
			semantic = Semantic.ASYNCHRONOUS | Semantic.SEQUENCE;
		}
		if(annotation.annotationType() == POPAsyncMutex.class){
			semantic = Semantic.ASYNCHRONOUS | Semantic.MUTEX;
		}

		if(semantic != -1){
			semantics.put(mi, semantic);
		}
	}

	/**
	 * Initialize the constructor identifier and the semantic
	 * @param c				class to initialize
	 */
	protected void initializeConstructorInfo(Class<?> c) {
		if (!definedMethodId) {
			// initializeMethodId
			Constructor<?>[] allConstructors = c.getDeclaredConstructors();

			Arrays.sort(allConstructors, new Comparator<Constructor<?>>() {
				@Override
                public int compare(Constructor<?> first, Constructor<?> second) {
					String firstSign = ClassUtil.getMethodSign(first);
					String secondSign = ClassUtil.getMethodSign(second);
					return firstSign.compareTo(secondSign);
				}
			});

			for (Constructor<?> constructor : allConstructors) {
				if (Modifier.isPublic(constructor.getModifiers())) {
					int id = MethodUtil.constructorId(constructor);
					
					MethodInfo info = new MethodInfo(getClassId(), id);
					constructorInfos.put(info, constructor);
					reverseConstructorInfos.put(constructor, info);
					semantics.put(info, Semantic.CONSTRUCTOR | Semantic.SYNCHRONOUS | Semantic.SEQUENCE);
				}
			}
		}
	}
	
	/**
	 * Define informations about a method
	 * @param c				Class of the method
	 * @param methodName	Name of the method
	 * @param methodId		Unique identifier of the method
	 * @param semanticId	Semantic applied to the method
	 * @param paramTypes	Parameters of the method
	 */
	protected void defineMethod(Class<?>c,String methodName, int methodId, int semanticId, Class<?>...paramTypes)
	{
		try {
			Method m = c.getMethod(methodName, paramTypes);
			MethodInfo methodInfo = new MethodInfo(getClassId(), methodId);
			methodInfos.put(methodInfo, m);
			
			if (semantics.containsKey(methodInfo)) {
				semantics.replace(methodInfo, semanticId);
			} else {
				semantics.put(methodInfo, semanticId);
			}
			
		} catch (SecurityException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Define information about a constructor
	 * @param c				Class of the constructor
	 * @param constructorId	Unique identifier of the constructor
	 * @param paramTypes	Parameters of the constructor
	 */
	protected void defineConstructor(Class<?>c,int constructorId, Class<?>...paramTypes)
	{
		try {
			Constructor<?> constructor = c.getConstructor(paramTypes);
			MethodInfo info = new MethodInfo(getClassId(),
					constructorId);
			constructorInfos.put(info, constructor);
			semantics.put(info, Semantic.CONSTRUCTOR
					| Semantic.SYNCHRONOUS | Semantic.SEQUENCE);
			
		} catch (SecurityException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Deserialize the object from the buffer
	 * @param buffer	The buffer to deserialize from
	 */
	@Override
    public boolean deserialize(POPBuffer buffer) {
		return true;
	}
	
	public boolean deserialize(Combox sourceCombox, POPBuffer buffer) {
		return true;
	}

	/**
	 * Serialize the object into the buffer
	 * @param buffer	The buffer to serialize in
	 */
	@Override
    public boolean serialize(POPBuffer buffer) {
		if(od.useLocalJVM() && broker != null){
			//broker.onNewConnection();
			
			od.serialize(buffer);
			broker.getAccessPoint().serialize(buffer);
			buffer.putInt(1);//TODO: Find out what this number does
			return true;
		}
		
		return true;
	}

	/**
	 * Exit method
	 */
	public void exit() {
		
	}

	/**
	 * Print object information on the standard output
	 */
	public void printMethodInfo() {
		System.out.println("===========ConstructorInfo============");
		constructorInfos.forEach((mi, c) -> System.out.format("ClassId:%d.ConstructorId:%d.Sign:%s",
            mi.getClassId(), mi.getMethodId(), c.toGenericString()));

		System.out.println("===========MethodInfo============");
		methodInfos.forEach((mi, m) -> System.out.format("ClassId:%d.MethodId:%d.Sign:%s",
            mi.getClassId(), mi.getMethodId(), m.toGenericString()));

		System.out.println("===========SemanticsInfo============");
		semantics.forEach((mi, s) -> System.out.format("ClassId:%d.ConstructorId:%d.Semantics:%d",
            mi.getClassId(), mi.getMethodId(), s));
	}
		
	/**
	 * Return the reference of this object with a POP-C++ format
	 * @return access point of the object as a formatted string
	 */
	public String getPOPCReference(){
		return getAccessPoint().toString();
	}
	
	public boolean isTemporary(){
		return temporary;
	}
	
	public void makeTemporary(){
		temporary = true;
	}

	@SuppressWarnings("unchecked")
	public <T extends POPObject> T makePermanent(){
		temporary = false;
		return (T) this;
	}
	
	public void setBroker(Broker broker){
	    this.broker = broker;
	}

	public <T> T getThis(Class<T> myClass){
		return getThis();
	}

	@SuppressWarnings("unchecked")
	public <T> T getThis(){
		if(me == null){
			me = PopJava.newActiveConnect(this, getClass(), getAccessPoint());

			//After establishing connection with self, artificially decrease connection by one
			//This is to avoid the issue of never closing objects with reference to itself
			if(me != null && broker != null){
				broker.onCloseConnection("SelfReference");
			}
		}

		return (T) me;
	}
	
	/**
	 * Register a certificate on the node 
	 * TODO Handle this method for other kind of Combox (only SSL ATM)
	 * 
	 * @param cert the certificate to save locally
	 */
	@POPSyncConc
	public void PopRegisterFutureConnectorCertificate(byte[] cert) {
		LogWriter.writeDebugInfo("Writing certificate received from middleman.");
		SSLUtils.addCertToTempStore(cert, true);
	}
	
	/**
	 * Get the tracked user list.
	 * 
	 * @return a callerID array of strings.
	 */
	@POPSyncSeq(localhost = true)
	public POPRemoteCaller[] getTrackedUsers() {
		return broker.getTrackingUsers();
	}
	
	/**
	 * Get the resources used by an user.
	 * 
	 * @param caller the identifier we want connection details of
	 * @return the details on the user use of the object
	 */
	@POPSyncSeq(localhost = true)
	public POPTracking getTracked(POPRemoteCaller caller) {
		return broker.getTracked(caller);
	}
	
	/**
	 * Get the resources used until now by caller.
	 * 
	 * @return my own usage of the object
	 */
	@POPSyncSeq
	public POPTracking getTracked() {
		return broker.getTracked(PopJava.getRemoteCaller());
	}
	
	/**
	 * Is tracking enabled on the remote object.
	 *
	 * @return true if object's tracking is turned on
	 */
	@POPSyncConc
	public boolean isTracking() {
		return broker.isTraking();
	}
	
	@Override
    protected void finalize() throws Throwable {
		super.finalize();
		
		PopJava.disconnect(this);
	}
}
