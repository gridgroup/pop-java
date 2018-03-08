package popjava.util.upnp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import popjava.util.LogWriter;

public class UPNPManager {
	
	private static final GatewayDiscover discover = new GatewayDiscover();
	private static GatewayDevice d = null;
	
	private static final Set<Integer> mappedPorts = new HashSet<Integer>();
	
	private static boolean inited = false;
	
	private static void init() {
		if(!inited) {
			try {
				discover.discover();
				d = discover.getValidGateway();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			
			inited = true;
		}
	}
	
	public synchronized static void registerPort(int port) {
		if(mappedPorts.contains(port)) {
			System.out.println("We already mapped port "+port+" before");
			return;
		}
		
		init();
		
		System.out.println("Try to map port "+port);
		
		if (null != d) {
			LogWriter.writeDebugInfo("Found gateway device.\n"+d.getModelName()+" ("+d.getModelDescription()+")");
		} else {
			LogWriter.writeDebugInfo("No valid gateway device found.");
		    return;
		}
		
		InetAddress localAddress = d.getLocalAddress();
		String externalIPAddress = "";
		try {
			externalIPAddress = d.getExternalIPAddress();
			
			LogWriter.writeDebugInfo("Internal IP "+localAddress);
			LogWriter.writeDebugInfo("External IP "+externalIPAddress);
			
			PortMappingEntry portMapping = new PortMappingEntry();
			
			if (d.getSpecificPortMappingEntry(port,"TCP",portMapping)) {
				LogWriter.writeDebugInfo("Port "+port+" is already forwarded");
			} else {
				LogWriter.writeDebugInfo("Sending port mapping request");
				
			    if (!d.addPortMapping(port, port,
			            localAddress.getHostAddress(),"TCP","POP-Java")) {
			    	LogWriter.writeDebugInfo("Port mapping attempt failed");
			    }else {
			    	mappedPorts.add(port);
			    }
			}
		}catch (SAXException e) {
			LogWriter.writeExceptionLog(e);
		} catch (IOException e) {
			LogWriter.writeExceptionLog(e);
		}
	}
		
	public static synchronized void close() {
		
		if(mappedPorts.size() > 0) {
			GatewayDevice d = discover.getValidGateway();
			
			for(int port : mappedPorts) {
				try {
					d.deletePortMapping(port,"TCP");
				} catch (IOException e) {
					LogWriter.writeExceptionLog(e);
				} catch (SAXException e) {
					LogWriter.writeExceptionLog(e);
				}
			}
		}
		
	}

}
