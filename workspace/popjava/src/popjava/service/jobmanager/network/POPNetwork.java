package popjava.service.jobmanager.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import popjava.service.jobmanager.POPJavaJobManager;
import popjava.service.jobmanager.connector.POPConnectorBase;
import popjava.service.jobmanager.connector.POPConnectorFactory;

/**
 * Describe a POP Network, made of POP Connector with relative members to of a POP COnnector.
 * 
 * @author Davide Mazzoleni
 */
public class POPNetwork {

	private final String name;
	private final Map<Class<? extends POPConnectorBase>, POPConnectorBase> connectors;
	private final Map<Class<? extends POPConnectorBase>, List<POPNetworkNode>> members;
	private final POPJavaJobManager jobManager;

	public POPNetwork(String name, POPJavaJobManager jobManager) {
		this.name = name;
		this.connectors = new HashMap<>();
		this.members = new HashMap<>();
		this.jobManager = jobManager;
	}

	public String getName() {
		return name;
	}

	public POPConnectorBase[] getConnectors() {
		return connectors.values().toArray(new POPConnectorBase[0]);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends POPConnectorBase> T getConnector(Class<T> connector) {
		return (T) connectors.get(connector);
	}

	public int size() {
		int size = 0;
		for (List<POPNetworkNode> l : members.values()) {
			size += l.size();
		}
		return size;
	}

	/**
	 * Get NetworkNode already casted to correct type
	 *
	 * @param <T> The type we want the set of NetworkNode
	 * @param connector Which connector we are using
	 * @return An immutable set we can loop through
	 */
	@SuppressWarnings("unchecked")
	public <T extends POPNetworkNode> List<T> getMembers(Class<? extends POPConnectorBase> connector) {
		List<POPNetworkNode> nodes = members.get(connector);
		if (nodes == null) {
			return new ArrayList<>();
		}
		return (List<T>) new ArrayList(nodes);
	}

	/**
	 * Add a NetworkNode to this network
	 *
	 * @param node The node
	 * @return true if the Node is added, false if not or not compatible
	 */
	@SuppressWarnings("unchecked")
	public boolean add(POPNetworkNode node) {
		// connector
		POPConnectorBase connector = connectors.get(node.getConnectorClass());
		if (connector == null) {
			connector = POPConnectorFactory.makeConnector(node.getConnectorName());
			connector.setJobManager(jobManager);
			connector.setNetwork(this);
			connectors.put(node.getConnectorClass(), connector);
		}
		// members
		List<POPNetworkNode> connectorMembers = members.get(connector.getClass());
		if (connectorMembers == null) {
			connectorMembers = new ArrayList<>();
			members.put(node.getConnectorClass(), connectorMembers);
		}
		if (connectorMembers.contains(node)) {
			return true;
		}
		if (connector.isValidNode(node)) {
			return connectorMembers.add(node);
		}
		return false;
	}

	/**
	 * Remove a node from this Network
	 *
	 * @param o The node
	 * @return true if the Node is remove, false otherwise
	 */
	public boolean remove(POPNetworkNode o) {
		// connector
		POPConnectorBase connector = connectors.get(o.getConnectorClass());
		if (connector == null) {
			return false;
		}
		// members
		List<POPNetworkNode> mem = members.get(connector.getClass());
		if (mem == null) {
			return false;
		}
		boolean status = mem.remove(o);
		if (mem.isEmpty()) {
			members.remove(connector.getClass());
			connectors.remove(connector.getClass());
		}
		return status;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + Objects.hashCode(this.name);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final POPNetwork other = (POPNetwork) obj;
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		if (!Objects.equals(this.members, other.members)) {
			return false;
		}
		if (!Objects.equals(this.jobManager, other.jobManager)) {
			return false;
		}
		return true;
	}

}
