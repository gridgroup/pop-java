package popjava.util;

import popjava.combox.ComboxSocketFactory;
import popjava.combox.ssl.ComboxSecureSocketFactory;
import popjava.service.jobmanager.connector.POPConnectorJobManager;

/**
 * This class regroup some configuration values
 */
public class Configuration {

	/**
	 * Creates a new instance of POPConfiguration
	 */
	public static final boolean DEBUG = false;
	public static final boolean DEBUG_COMBOBOX = false;
	public static final int RESERVE_TIMEOUT = 60000;
	public static final int ALLOC_TIMEOUT = 30000;
	public static final int CONNECTION_TIMEOUT = 30000;
	
	public static final int UPDATE_MIN_INTERVAL = 10000;
	public static final int SELF_REGISTER_INTERVAL = 43200;
	public static final int UNLOCK_TIMEOUT = 10000;
	public static final int SEARCH_TIMEOUT = 0;
	public static final int UNLIMITED_HOPS = Integer.MAX_VALUE;
	public static final int MAXREQTOSAVE = 300;
	public static final String DEFAULT_JOBMANAGER_CONNECTOR = POPConnectorJobManager.IDENTITY;
	public static final int EXPLORATION_MAX_SIZE = 300;
	
	public static final String DEFAULT_ENCODING = "xdr";
	public static final String SELECTED_ENCODING = "raw";
	public static final String DEFAULT_PROTOCOL = ComboxSecureSocketFactory.PROTOCOL;

	public static final boolean ASYNC_CONSTRUCTOR = true;
	public static final boolean ACTIVATE_JMX = false;
	public static boolean CONNECT_TO_POPCPP = false;//Util.getOSType().equals(OSType.UNIX);
	
	public static boolean CONNECT_TO_JAVA_JOBMANAGER = true;
	
	public static final boolean REDIRECT_OUTPUT_TO_ROOT = true;
	public static final boolean USE_NATIVE_SSH_IF_POSSIBLE = true;
	
	public static final String DEFAULT_JM_CONFIG_FILE = 
		System.getenv("POPJAVA_LOCATION") == null ? "jobmgr.conf" : System.getenv("POPJAVA_LOCATION") + "/etc/jobmgr.conf";
	
	public static final String TRUST_STORE = "truststore.jks";
	public static final String TRUST_STORE_PWD = "trustpass";
	public static final String TRUST_STORE_PK_PWD = "trustpass";
	public static final String PUBLIC_CERTIFICATE = "local_certificate.cer"; // RFC format
	public static final String TRUST_TEMP_STORE_DIR = "_temp_certificates";
	
	public static final String SSL_PROTOCOL = "TLSv1.2";
	
	/**
	 * Default constructor
	 */
	public Configuration() {
	}

}
