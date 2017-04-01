package popjava.service.jobmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import popjava.annotation.POPAsyncConc;
import popjava.annotation.POPClass;
import popjava.annotation.POPConfig;
import popjava.annotation.POPConfig.Type;
import popjava.annotation.POPParameter.Direction;
import popjava.baseobject.ObjectDescription;
import popjava.baseobject.POPAccessPoint;
import popjava.annotation.POPObjectDescription;
import popjava.annotation.POPParameter;
import popjava.annotation.POPSyncConc;
import popjava.base.POPErrorCode;
import popjava.base.POPException;
import popjava.dataswaper.POPString;
import popjava.service.jobmanager.network.Network;
import popjava.service.jobmanager.network.NetworkNode;
import popjava.serviceadapter.POPJobManager;
import popjava.util.Configuration;
import popjava.util.LogWriter;

@POPClass(classId = 10, deconstructor = false, useAsyncConstructor = false)
public class POPJavaJobManager extends POPJobManager{
	
	/** Currently used resources of a node */
	protected final Resource availble = new Resource();
	/** Total resources a job can have */
	protected final Resource limit = new Resource();
	
	
	/** Total number of requests received */
	protected int requestCounter;
	
	/** Number of job alive */
	protected Set<AppResource> jobs = new HashSet<>();
	
	/** Networks saved in this JobManager */
	protected Map<String,Network> networks = new HashMap<>();
	
	/** Max number of jobs */
	protected int maxJobs;
	
	/** Node extra information, value as List if multiple are supplied */
	protected Map<String, List<String>> nodeExtra = new HashMap<>();

	@POPObjectDescription(url = "localhost:" + POPJobManager.DEFAULT_PORT)
	public POPJavaJobManager() {
		init(Configuration.DEFAULT_JM_CONFIG_FILE);
	}
	
	public POPJavaJobManager(@POPConfig(Type.URL) String url, String conf) {
		init(conf);
	}

	/**
	 * Read configuration file and setup system
	 * Has some sane defaults
	 * @param confFile Path to configuration file
	 */
	private void init(String confFile) {
		File config = new File(confFile);
		
		// early exit
		if (!config.exists()) {
			LogWriter.writeDebugInfo("Open config file [" + confFile + "] fail");
			POPException.throwObjectNoResource();
		}
		
		// default num of jobs
		maxJobs = 100;
		
		// set resource by default hoping for the best
		availble.add(new Resource(300, 256, 100));
		// no restrictions on default limit
		limit.add(availble);
		
		// config file is read line by line, information is extracted as see fit
		try (BufferedReader br = new BufferedReader(new FileReader(config))) {
			String line;
			String[] token;
			while ((line = br.readLine()) != null) {
				// split line for reading
				token = line.trim().split("\\s+");
				
				// skip only key or empty lines
				if (token.length < 2 )
					continue;
				// skip commented lines
				if (token[0].startsWith("#"))
					continue;
				
				// handle first token
				switch (token[0]) {
					// create network
					// format: network <name> <protocol> [params...]
					case "network": 
						// not enough elements
						if (token.length < 3) {
							LogWriter.writeDebugInfo(String.format("Network %s not enough parameters supplied", token[1]));
							continue;
						}
						
						// check if exists
						if (networks.containsKey(token[1])) {
							LogWriter.writeDebugInfo(String.format("Network %s already exists", token[1]));
							continue;
						}
						
						// get extra information
						String[] other = new String[ token.length - 3 ];
						System.arraycopy(token, 3, other, 0, token.length - 3);
						
						// create network
						Network network = new Network(token[1], token[2], this, other);
						// add to map
						networks.put(token[1], network);
						break;
						
						
						
					// handle node in network
					// format: node <network> <params...>
					case "node":
						// not enough elements
						if (token.length < 3) {
							LogWriter.writeDebugInfo(String.format("Node not enough parameters supplied: %s", line));
							continue;
						}
						
						// get network
						network = networks.get(token[1]);
						// no network
						if (network == null) {
							LogWriter.writeDebugInfo(String.format("Node, network %s not found", token[1]));
							continue;
						}
						
						// params for node, at least one
						other = new String[ token.length - 2 ];
						System.arraycopy(token, 2, other, 0, token.length - 2);
						
						// create the node for the network
						NetworkNode node = network.makeNode(other);
						// add it to the network
						network.add(node);
						break;
						
						
						
					// set available resources
					// format: resource <ram|memory|bandwidth> <value>
					case "resource":
						if (token.length < 3) {
							LogWriter.writeDebugInfo(String.format("Resource set fail, not enough parameters: %s", line));
							continue;
						}
						
						// type of set <ram|memory|bandwidth>
						switch (token[1]) {
							case "ram":
								try {
									availble.setMemory(Integer.parseInt(token[2]));
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Resource set fail, ram value: %s", token[2]));
								}
								break;
							case "memory":
								try {
									availble.setBandwidth(Integer.parseInt(token[2]));
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Resource set fail, memory value: %s", token[2]));
								}
								break;
							case "bandwidth":
								try {
									availble.setBandwidth(Integer.parseInt(token[2]));
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Resource set fail, bandwidth value: %s", token[2]));
								}
								break;
							default:
								LogWriter.writeDebugInfo(String.format("Resource set fail, unknow resource: %s", token[1]));
						}
						break;
						
						
						
					// set jobs limit, resources and number
					// format: job <limit|ram|memory|bandwidth> <value>
					case "job":
						if (token.length < 3) {
							LogWriter.writeDebugInfo(String.format("Limit set fail, not enough parameters: %s", line));
							continue;
						}
						// type of set <limit|ram|memory|bandwidth>
						switch (token[1]) {
							case "limit":
								try {
									maxJobs = Integer.parseInt(token[2]);
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Limit set fail, limit value: %s", token[2]));
								}
								break;
							case "ram":
								try {
									limit.setMemory(Integer.parseInt(token[2]));
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Limit set fail, ram value: %s", token[2]));
								}
								break;
							case "memory":
								try {
									limit.setBandwidth(Integer.parseInt(token[2]));
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Limit set fail, memory value: %s", token[2]));
								}
								break;
							case "bandwidth":
								try {
									limit.setBandwidth(Integer.parseInt(token[2]));
								} catch (NumberFormatException e) {
									LogWriter.writeDebugInfo(String.format("Limit set fail, bandwidth value: %s", token[2]));
								}
								break;
							default:
								LogWriter.writeDebugInfo(String.format("Limit set fail, unknow resource: %s", token[1]));
						}
						break;
						
						
					// any other value is store in a map for possible future use
					default:
						// get or create and add to map
						List<String> val = nodeExtra.get(token[0]);
						if (val == null) {
							val = new ArrayList<>();
							nodeExtra.put(token[0], val);
						}
						// add extra value
						val.add(line.substring(token[0].length()));
						break;
				}
			}
		} catch (IOException e) {
			throw new POPException(0, e.getMessage());
		}
	}
	
	@Override
	public int createObject(POPAccessPoint localservice,
			String objname,
			@POPParameter(Direction.IN) ObjectDescription od,
			int howmany, POPAccessPoint[] objcontacts,
			int howmany2, POPAccessPoint[] remotejobcontacts) {
		
		// get network from od
		String networkString = od.getNetwork();
		// get real network
		Network network = networks.get(networkString);
		
		// throw exception if no network is found
		if (network == null)
			throw new POPException(POPErrorCode.POP_JOBSERVICE_FAIL, networkString);
		
		// call the protocol specific createObject
		return network.getProtocol().createObject(localservice, objname, od, howmany, objcontacts, howmany2, remotejobcontacts);
	}

	@Override
	public int execObj(POPString objname, int howmany, int[] reserveIDs, String localservice, POPAccessPoint[] objcontacts) {
		return super.execObj(objname, howmany, reserveIDs, localservice, objcontacts);
	}

	/**
	 * NOTE: not in parent class
	 * Add a resource reservation
	 * @param od
	 * @param fitness
	 * @param popAppId
	 * @param reqID
	 * @return the reservation ID for this request used in the other methods
	 */
	@POPSyncConc(id = 16)
	public int reserve(@POPParameter(Direction.IN) POPObjectDescription od, @POPParameter(Direction.INOUT) float fitness, String popAppId, String reqID) {
	
		return 0;
	}
	
	@Override
	public void cancelReservation(int[] req, int howmany) {
		super.cancelReservation(req, howmany);
	}

	@Override
	public void dump() {
		super.dump();
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public int query(POPString type, POPString value) {
		return super.query(type, value);
	}

	@Override
	public void selfRegister() {
		super.selfRegister();
	}
	
	/**
	 * Register node to a network by supplying an array of string matching the format in the configuration file
	 * @param networkName The name of an existing network in this JM
	 * @param params An array of String that will be processed by {@link Network#makeNode(java.lang.String...)}
	 */
	@POPAsyncConc
	public void registerNode(String networkName, String... params) {
		// get network
		Network network = networks.get(networkName);
		if (network == null) {
			LogWriter.writeDebugInfo(String.format("Node %s not registered, network not found", Arrays.toString(params)));
			return;
		}
		
		network.add(network.makeNode(params));
	}
	
	/**
	 * Remove a node from a network
	 * @see #registerNode(java.lang.String, java.lang.String...) 
	 * @param networkName The name of an existing network in this JM
	 * @param params An array of String that will be processed by {@link Network#makeNode(java.lang.String...)}
	 */
	@POPAsyncConc
	public void unregisterNode(String networkName, String... params) {
		// get network
		Network network = networks.get(networkName);
		if (network == null) {
			LogWriter.writeDebugInfo(String.format("Node %s not registered, network not found", Arrays.toString(params)));
			return;
		}
		
		network.remove(network.makeNode(params));
	}

	/**
	 * XXX: Not compatible with Network base JM
	 * @param url 
	 */
	@Override
	public void registerNode(String url) {
		super.registerNode(url);
		// TODO default network?
	}

	/**
	 * NOTE: not in parent class
	 * XXX: Not compatible with Network base JM
	 * @param url 
	 */
	@POPAsyncConc
	public void unregisterNode(@POPParameter(Direction.IN) POPAccessPoint url) {
		// TODO remove from default 
	}
}