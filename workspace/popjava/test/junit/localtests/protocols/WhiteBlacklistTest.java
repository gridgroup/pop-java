package junit.localtests.protocols;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import popjava.combox.ComboxFactory;
import popjava.combox.ComboxFactoryFinder;
import popjava.util.Configuration;
import popjava.util.Util;
import popjava.util.ssl.KeyPairDetails;
import popjava.util.ssl.SSLUtils;
import static org.junit.Assert.*;
import org.junit.Before;
import popjava.PopJava;
import popjava.baseobject.POPAccessPoint;
import popjava.system.POPSystem;
import popjava.util.ssl.KeyStoreDetails;

/**
 *
 * @author dosky
 */
public class WhiteBlacklistTest {
		
	static File keystore;
	static File userConfig;
	static KeyPairDetails keyDetails;
	static KeyStoreDetails ksDetails;
	
	static Configuration conf = Configuration.getInstance();
	
	@BeforeClass
	public static void setup() throws IOException {
		userConfig = File.createTempFile("popjunit", ".properties");
		keystore = new File(String.format("popjunit-%s.jks", Util.generateUUID()));
		keyDetails = new KeyPairDetails("myTest");
		ksDetails = new KeyStoreDetails("storepass", "keypass", keystore);
		
		Configuration conf = Configuration.getInstance();
		conf.setDebug(false);
		
		SSLUtils.generateKeyStore(ksDetails, keyDetails);
		conf.setSSLKeyStoreOptions(ksDetails);
		conf.setUserConfig(userConfig);
		
		conf.store();
	}
	
	@AfterClass
	@SuppressWarnings("unchecked")
	public static void cleanup() {
		userConfig.deleteOnExit();
		keystore.deleteOnExit();
		conf.setProtocolsBlacklist(Collections.EMPTY_SET);
		conf.setProtocolsWhitelist(Collections.EMPTY_SET);
		conf.setUserConfig(null);
	}
	
	@Before
	public void before() {
		POPSystem.initialize();
	}
	
	@After
	public void end() {
		POPSystem.end();
	}
	
	private Set<String> startAndCheck() {
		ComboxFactoryFinder finder = ComboxFactoryFinder.getInstance();
		Set<String> expected = new HashSet<>();
		for (ComboxFactory factory : finder.getAvailableFactories()) {
			expected.add(factory.getComboxName());
		}
		
		A a = PopJava.newActive(this, A.class, "localhost", new String[] { "" });
		a.sync();
		
		POPAccessPoint ap = a.getAccessPoint();
		Set<String> got = new HashSet<>();
		for (int i = 0; i < ap.size(); i++) {
			got.add(ap.get(i).getProtocol());
		}
		
		System.out.println("Object access points  : " + got);
		System.out.println("Expected access points: " + expected);
		
		assertEquals(expected, got);
		return got;
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void emptyLists() {
		conf.setProtocolsBlacklist(Collections.EMPTY_SET);
		conf.setProtocolsWhitelist(Collections.EMPTY_SET);
		System.out.println("Empty lists:");
		startAndCheck();
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void useWhitelist() {
		conf.setProtocolsBlacklist(Collections.EMPTY_SET);
		conf.setProtocolsWhitelist(new HashSet<>(Arrays.asList("socket")));
		System.out.println("Whitelist:");
		Set<String> got = startAndCheck();
		assertEquals("only socket should be visible", 1, got.size());
		assertFalse("ssl is blacklisted", got.contains("ssl"));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void useBlacklist() {
		conf.setProtocolsBlacklist(new HashSet<>(Arrays.asList("ssl")));
		conf.setProtocolsWhitelist(Collections.EMPTY_SET);
		System.out.println("Blacklist:");
		Set<String> got = startAndCheck();
		assertFalse("ssl is blacklisted", got.contains("ssl"));
	}
	
	@Test
	public void useBothLists() {
		conf.setProtocolsBlacklist(new HashSet<>(Arrays.asList("ssl")));
		conf.setProtocolsWhitelist(new HashSet<>(Arrays.asList("ssl", "socket")));
		System.out.println("Both lists:");
		Set<String> got = startAndCheck();
		assertFalse("ssl is blacklisted", got.contains("ssl"));
	}
}
