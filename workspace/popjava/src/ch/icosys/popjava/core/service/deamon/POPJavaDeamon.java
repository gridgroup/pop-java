package ch.icosys.popjava.core.service.deamon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import ch.icosys.popjava.core.system.POPJavaConfiguration;
import ch.icosys.popjava.core.util.Configuration;
import ch.icosys.popjava.core.util.SystemUtil;

/**
 * This class starts a daemon that listens to incoming POP requests.
 * It replaces the SSH service to start brokers.
 * @author Beat Wolf
 *
 */
public class POPJavaDeamon implements Runnable, Closeable{

	private static final String salt = "POPSALT";
	
	public static final String SUCCESS = "OK";
	private ServerSocket serverSocket;
	private int port;
	private String password = "";
	
	private static final String BACKUP_JAR = "build/popjava.jar";
	public static final String ERROR_PWD = "ERROR PASS";
	
	private static byte [] getIV(final String salt, final int size){
		final byte [] iv = new byte[size];
		final Random rand = new Random(salt.hashCode());
		
		for(int i = 0; i < size; i++){
			iv[i] = (byte)rand.nextFloat();
		}
		
		return iv;
	}
	
	/**
	 * Mode has is Cipher.ENCRYPT_MODE o 
	 * @param salt to mask the password
	 * @param password to match on the other side
	 * @param encrypt if we are encrypting or decrypting
	 * @return the cipher or null
	 */
	public static Cipher createKey(String salt, String password, boolean encrypt){
		try{
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, Math.min(Cipher.getMaxAllowedKeyLength("AES"), 256));
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			AlgorithmParameterSpec paramSpec = new IvParameterSpec(getIV(salt, 16));

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secret, paramSpec);
			
			return cipher;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public POPJavaDeamon(String password){
		this(password, Configuration.getInstance().getPopJavaDaemonPort());
	}
	
	public POPJavaDeamon(String password, int port){
		this.password = password;
		this.port = port;
	}
	
	/**
	 * Class that handles the accepted connections and runs the Broker with the provided paramters
	 * @author Beat Wolf
	 *
	 */
	private class Acceptor implements Runnable{
		
		private final Socket socket;
		private final Random rand = new Random();
		private static final int SALT_LENGTH = 10;
		
		public Acceptor(Socket socket){
			this.socket = socket;
		}
		
		private String createSecret(){
			StringBuilder secret = new StringBuilder();
			
			for(int i = 0;i < SALT_LENGTH; i++){
				secret.append((char) (rand.nextInt(26) + 'a'));
			}
			
			return secret.toString();
		}
		
		@Override
		public void run() {
			
			try {
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream()));
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				
				String salt = createSecret();
				
				//Send salt to connectin client
				writer.write(salt+"\n");
				writer.flush();
				
				System.out.println("Execute command: ");
				
				String saltedHash = getSaltedHash(salt, password);
				
				//Read command to execute
				String challengeAnswer = reader.readLine();
				
				if(!saltedHash.equals(challengeAnswer)){
					System.err.println("The supplied secret was wrong : "+challengeAnswer+" should be "+saltedHash+" using password "+password);
					writer.write(ERROR_PWD+"\n");
					writer.close();
					reader.close();
					return;
				}else{
					writer.write("SUCCESS\n");
					writer.flush();
				}
				
				System.out.println("Correct login");
				
				writer = new BufferedWriter(
						new OutputStreamWriter(
								new CipherOutputStream(socket.getOutputStream(), createKey(salt, password, true))));
				reader = new BufferedReader(
						new InputStreamReader(
								new CipherInputStream(socket.getInputStream(), createKey(salt, password, false))));
				
				int commandLength = Integer.parseInt(reader.readLine());
								
				List<String> commands = new ArrayList<>();
				
				System.out.println("Get commands "+commandLength);
				
				boolean isJava = false;
				boolean isClassPath = false;
				for(int i = 0; i < commandLength; i++){
					String line = reader.readLine();
					//If the current parameter is the classpath, modify it to fit local system
					if(isJava && isClassPath){ 
						if(!isClasspathValid(line)){
							String temp = POPJavaConfiguration.getClassPath();
							if(temp != null && !temp.isEmpty()){
								line = temp;
							}
						}
					}
					
					if(line.startsWith("-javaagent:")){
						String popJavaJar = POPJavaConfiguration.getPOPJavaCodePath();
						if(!POPJavaConfiguration.isJar()){
							popJavaJar = BACKUP_JAR;
						}
						line = "-javaagent:"+popJavaJar;
					}
					
					commands.add(line);
					System.out.print(line+" ");

					if(i == 0 && line.equals("java")){
						isJava = true;
					}
					
					isClassPath = line.equals("-cp");
					
					
				}
				System.out.println();
				System.out.println("Launching object");
				//Execute command
				if(isJava){
					SystemUtil.runCmd(commands);
					writer.write(SUCCESS+"\n");
				}else{
					writer.write("ERROR NOT JAVA\n");
				}

				System.out.println("Finished daemon stuff");
				writer.close();
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static boolean isClasspathValid(String classPath){
	    
	    String [] parts = classPath.split(File.pathSeparator);
	    
	    for(String part: parts){
	        if(new File(part).exists() || part.startsWith("http://")){
	            return true;
	        }
	    }
	    
	    return false;
	}
	
	public static void main(String ... args) throws IOException{
		POPJavaDeamon deamon = new POPJavaDeamon("test");
		deamon.start();
	}
	
	/**
	 * Starts the POP-Java listener daemon
	 * @throws IOException if any i/o problem occurs
	 */
	public void start() throws IOException{
		serverSocket = new ServerSocket(port);
		
		Executor executor = Executors.newCachedThreadPool();
		
		System.out.println("Started POP-Java daemon");
		
		try{
    		while(!Thread.currentThread().isInterrupted()){
		        Socket socket = serverSocket.accept();
	            System.out.println("Accepted connection");
	            executor.execute(new Acceptor(socket));
    		}
		}catch(IOException e){
        }
		
		if(serverSocket != null){
		    serverSocket.close();
		}
		
		System.out.println("Closed POP-Java daemon");
	}
	
	/**
	 * Stops the POP-Java listener daemon
	 * @throws IOException  if any i/o problem occurs
	 */
	@Override
    public synchronized void close() throws IOException{
		if(serverSocket != null){
			serverSocket.close();
			serverSocket = null;
		}
	}
	
	public static String getSaltedHash(String salt, String secret){
		return ""+(secret + salt).hashCode();
	}

	@Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
