package popjava.service.jobmanager.yaml;

import java.util.Collections;
import java.util.List;

/**
 * Describe the whole Job Manager in the YAML configuration file.
 * A configuration file may look something like this.
 * Notice how YAML file are indentation sensitive.
 * 
 * ```yaml
 * 
 * machineResources:
 *   flops: 1000.0
 *   memory: 8000.0
 *   bandwidth: 100.0
 *   
 * jobResources:
 *   flops: 200.0
 *   memory: 500.0
 *   bandwidth: 10.0
 *   
 * jobLimit: 100
 * defaultNetwork: 6ff6b4d0-97bb-468d-99c5-2a4d042471e2
 * 
 * networks:
 *   - uuid: 6ff6b4d0-97bb-468d-99c5-2a4d042471e2
 *     friendlyName: second net friendly name
 *     connectors: 
 *     - type: jobmanager
 *       nodes:
 *       - host: localhost
 *         port: 2713
 *         protocol: socket
 * 
 *     - type: tfc
 *       nodes:
 *       - host: localhost
 *         port: 2713
 *         protocol: ssl
 * 
 *       - host: localhost
 *         port: 2712
 *         protocol: ssl
 *   
 *   - uuid: ff3789de-4471-4f14-8b04-cfdcf668e31c
 *     friendlyName: my second network
 * ```
 * @author Davide Mazzoleni
 */
public class YamlJobManager {

	private YamlResource machineResources;
	private YamlResource jobResources;
	private int jobLimit;

	private String defaultNetwork;

	@SuppressWarnings("unchecked")
	private List<YamlNetwork> networks = Collections.EMPTY_LIST;

	/**
	 * The maximum amount of resources that the Job Manager can allocate.
	 * 
	 * @return the resources available globally
	 */
	public YamlResource getMachineResources() {
		return machineResources;
	}

	/**
	 * The maximum amount of resources that the Job Manager can allocate.
	 * 
	 * @param machineResources the resources available globally
	 */
	public void setMachineResources(YamlResource machineResources) {
		this.machineResources = machineResources;
	}

	/**
	 * The maximum amount of resources that the Job Manager can allocate for a single object.
	 * 
	 * @return the resources available for a single job
	 */
	public YamlResource getJobResources() {
		return jobResources;
	}

	/**
	 * The maximum amount of resources that the Job Manager can allocate for a single object.
	 * 
	 * @param jobResources the resources available for a single job
	 */
	public void setJobResources(YamlResource jobResources) {
		this.jobResources = jobResources;
	}

	/**
	 * How many object can the Job Manager manage at the same time.
	 * 
	 * @return the maximum number of jobs
	 */
	public int getJobLimit() {
		return jobLimit;
	}

	/**
	 * How many object can the Job Manager manage at the same time.
	 * 
	 * @param jobLimit the maximum number of jobs
	 */
	public void setJobLimit(int jobLimit) {
		this.jobLimit = jobLimit;
	}

	/**
	 * Which network is used if none was specified.
	 * 
	 * @return the default network
	 */
	public String getDefaultNetwork() {
		return defaultNetwork;
	}

	/**
	 * Which network is used if none was specified.
	 * 
	 * @param defaultNetwork the default network
	 */
	public void setDefaultNetwork(String defaultNetwork) {
		this.defaultNetwork = defaultNetwork;
	}

	/**
	 * All the networks in the Job Manager.
	 * 
	 * @return the networks in the job manager
	 */
	public List<YamlNetwork> getNetworks() {
		return networks;
	}

	/**
	 * All the networks in the Job Manager.
	 * 
	 * @param networks the networks in the job manager
	 */
	public void setNetworks(List<YamlNetwork> networks) {
		this.networks = networks;
	}
}
