package popjava.util.ssl;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.style.BCStyle;

/**
 * This class is meant to be send to {@link SSLUtils#generateKeyStore(popjava.combox.ssl.KeyStoreOptions) }
 *
 * @author Davide Mazzoleni
 */
public class KeyStoreOptions {
	
	public static enum KeyStoreFormat {
		JKS,
		PKCS12;
	}

	private String alias;
	private String storepass;
	private String keypass;
	private String keyStoreFile;
	private KeyStoreFormat keyStoreFormat;
	private Date validUntil;
	private int keySize;
	private final Map<ASN1ObjectIdentifier, String> rdn = new HashMap<>();
	
	private String tempCertFolder;

	// validation
	boolean hasName = false;

	/**
	 * When using this constructor we must ensure {@link #validate()} return true
	 */
	public KeyStoreOptions() {
	}
	
	/**
	 * Copy constructor
	 * @param other 
	 */
	public KeyStoreOptions(KeyStoreOptions other) {
		this.alias = other.alias;
		this.storepass = other.storepass;
		this.keypass = other.keypass;
		this.keyStoreFile = other.keyStoreFile;
		this.keyStoreFormat = other.keyStoreFormat;
		this.validUntil = other.validUntil;
		this.keySize = other.keySize;
		this.tempCertFolder = other.tempCertFolder;
		this.rdn.putAll(other.rdn);
		this.hasName = other.hasName;
	}

	/**
	 * Full constructor
	 * 
	 * @param alias The alias of this node, used to find its own public certificate
	 * @param storepass The main password for the KeyStore, protect from tempering with the file
	 * @param keypass The password of the primate key, used to extract it
	 * @param keyStoreFile Where to save the file
	 * @param keyStoreFormat Which format to save the KeyStore: JKS, PKCS12 (may have issue)
	 * @param validUntil Until when the certificate should be valid
	 * @param keySize The complexity of the RSA key, must be greater than 1024 bits
	 * @param tempCertFolder The location on the temporary certificate folder
	 */
	public KeyStoreOptions(String alias, String storepass, String keypass, String keyStoreFile, KeyStoreFormat keyStoreFormat, Date validUntil, int keySize, String tempCertFolder) {
		this.alias = alias;
		this.storepass = storepass;
		this.keypass = keypass;
		this.keyStoreFile = keyStoreFile;
		this.keyStoreFormat = keyStoreFormat;
		this.validUntil = validUntil;
		this.keySize = keySize;
		this.tempCertFolder = tempCertFolder;
		this.rdn.put(BCStyle.OU, "PopJava");
		this.rdn.put(BCStyle.CN, alias);
		hasName = true;
	}
	
	/**
	 * Parameters to create a KeyStore with sane defaults.
	 * Consider using {@link #setValidFor(int)}
	 * Defaults are: 
	 *  keyStoreFormat := JKS 
	 *  validity := 30 days
	 *  keySize = 2048 bits
	 *
	 * @param alias The alias of this node, used to find its own public certificate
	 * @param storepass The main password for the KeyStore, protect from tempering with the file
	 * @param keypass The password of the primate key, used to extract it
	 * @param keyStoreFile Where to save the file
	 */
	public KeyStoreOptions(String alias, String storepass, String keypass, String keyStoreFile) {
		this(alias, storepass, keypass, keyStoreFile, KeyStoreFormat.JKS, Date.from(Instant.now().plus(30, ChronoUnit.DAYS)), 2048, "tempCerts");
	}

	/**
	 * Constructor specific for the configuration of POP-Java.
	 * ValidUntil is null and RSA complexity is 0.
	 * If created with this constructor {@link #validate() } will throw and exception.
	 * 
	 * @param alias
	 * @param storepass
	 * @param keypass
	 * @param keyStoreFile
	 * @param keyStoreFormat
	 * @param tempCertFolder 
	 */
	public KeyStoreOptions(String alias, String storepass, String keypass, String keyStoreFile, KeyStoreFormat keyStoreFormat, String tempCertFolder) {
		this(alias, storepass, keypass, keyStoreFile, keyStoreFormat, null, 0, tempCertFolder);
	}

	public String getAlias() {
		return alias;
	}

	/**
	 * Set the alias of the local node
	 * 
	 * @param alias 
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getStorePass() {
		return storepass;
	}

	/**
	 * Password of the keystore file, protect the integrity of the file
	 * 
	 * @param storepass 
	 */
	public void setStorePass(String storepass) {
		this.storepass = storepass;
	}

	public String getKeyPass() {
		return keypass;
	}

	/**
	 * Password of the Private Key in the KeyStore
	 * 
	 * @param keypass 
	 */
	public void setKeyPass(String keypass) {
		this.keypass = keypass;
	}

	public String getKeyStoreFile() {
		return keyStoreFile;
	}

	/**
	 * Where to save the file
	 * 
	 * @param keyStoreFile 
	 */
	public void setKeyStoreFile(String keyStoreFile) {
		this.keyStoreFile = keyStoreFile;
	}

	public KeyStoreFormat getKeyStoreFormat() {
		return keyStoreFormat;
	}

	/**
	 * Format of the keystore.
	 * Be aware that format different from JKS could have issues.
	 * 
	 * @param keyStoreFormat 
	 */
	public void setKeyStoreFormat(KeyStoreFormat keyStoreFormat) {
		this.keyStoreFormat = keyStoreFormat;
	}

	public Date getValidUntil() {
		return validUntil;
	}

	/**
	 * Until when the certificate should be valid
	 * 
	 * @param validUntil 
	 */
	public void setValidUntil(Date validUntil) {
		this.validUntil = validUntil;
	}

	/**
	 * For how many day from now should the certificate be valid
	 * 
	 * @param days 
	 */
	public void setValidFor(int days) {
		this.validUntil = Date.from(Instant.now().plus(days, ChronoUnit.DAYS));
	}

	public int getKeySize() {
		return keySize;
	}

	/**
	 * The complexity of the RSA key, must be greater than 1024 bits
	 * 
	 * @param keySize 
	 */
	public void setKeySize(int keySize) {
		this.keySize = keySize;
	}

	/**
	 * Specify the certificate Relative Distinguished Name (RDN).
	 *
	 * @see BCStyle
	 * @param name What we want to specify, like {@link BCStyle#CN}
	 * @param value
	 */
	public void addRDN(ASN1ObjectIdentifier name, String value) {
		this.rdn.put(name, value);
	}

	public void removeRDN(ASN1ObjectIdentifier name) {
		this.rdn.remove(name);
	}

	public Map<ASN1ObjectIdentifier, String> getRDN() {
		return Collections.unmodifiableMap(rdn);
	}

	public String getTempCertFolder() {
		return tempCertFolder;
	}

	/**
	 * The location where temporary certificates should be stored
	 * 
	 * @param tempCertFolder 
	 */
	public void setTempCertFolder(String tempCertFolder) {
		this.tempCertFolder = tempCertFolder;
	}

	/**
	 * @throws InvalidParameterException when something is set incorrectly for creating a new KeyStore
	 */
	public void validate() {
		if (rdn.isEmpty()) {
			throw new InvalidParameterException("At least one argument of the RDN must be provided");
		}
		if (alias == null || alias.isEmpty()) {
			throw new InvalidParameterException("An alias must be given and not empty");
		}
		if (storepass == null || storepass.length() < 6) {
			throw new InvalidParameterException("Store password must be set and at least 6 character long");
		}
		if (keypass == null || keypass.length() < 6) {
			throw new InvalidParameterException("Key password must be set and at least 6 character long");
		}
		if (keyStoreFile == null || keyStoreFile.isEmpty()) {
			throw new InvalidParameterException("KeyStore file must be set and not empty");
		}
		if (keyStoreFormat == null) {
			throw new InvalidParameterException("A format for the keystore must be provided: JKS, PKCS12, ...");
		}
		if (validUntil == null) {
			throw new InvalidParameterException("A expiration date must be set");
		}
		if (keySize < 1024) {
			throw new InvalidParameterException("Keys below 1024 bits are insecure (consider using 2048 or higher)");
		}

		if (keyStoreFormat == KeyStoreFormat.PKCS12 && !keypass.equals(storepass)) {
			throw new InvalidParameterException("When using PKCS12 storePass and keyPass must match");
		}
	}
}
