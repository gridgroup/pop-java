package popjava.service.jobmanager.network;

import java.security.cert.Certificate;
import java.util.List;
import popjava.PopJava;
import popjava.base.POPErrorCode;
import popjava.base.POPException;
import popjava.base.POPObject;
import popjava.baseobject.ObjectDescription;
import popjava.baseobject.POPAccessPoint;
import popjava.util.ssl.SSLUtils;
import popjava.dataswaper.POPMutableFloat;
import popjava.dataswaper.POPString;
import popjava.interfacebase.Interface;
import popjava.service.jobmanager.POPJavaJobManager;
import popjava.service.jobmanager.Resource;
import popjava.service.jobmanager.search.SNExploration;
import popjava.service.jobmanager.search.SNNodesInfo;
import popjava.service.jobmanager.search.SNRequest;
import popjava.service.jobmanager.search.SNResponse;
import popjava.service.jobmanager.search.SNWayback;
import popjava.system.POPSystem;
import popjava.util.Configuration;
import popjava.util.LogWriter;
import popjava.util.POPRemoteCaller;
import popjava.util.Util;

/**
 *
 * @author Davide Mazzoleni
 */
public class POPConnectorJobManager extends POPConnector implements POPConnectorSearchNodeInterface {
	
	private static class DescriptorMethodImpl implements POPNetworkDescriptorMethod {
		@Override
		public POPConnector createConnector() {
			return new POPConnectorJobManager();
		}

		@Override
		public POPNode createNode(List<String> params) {
			return new POPNodeJobManager(params);
		}
	}
	static final POPNetworkDescriptor DESCRIPTOR = new POPNetworkDescriptor("jobmanager", new DescriptorMethodImpl());
	
	private final Configuration conf = Configuration.getInstance();

	public POPConnectorJobManager() {
		super(DESCRIPTOR);
	}
	
	@Override
	public int createObject(POPAccessPoint localservice, String objname, ObjectDescription od,
			int howmany, POPAccessPoint[] objcontacts, int howmany2, POPAccessPoint[] remotejobcontacts) {
		// check local resource
		Resource currAva = jobManager.getAvailableResources();
		// od request
		Resource resourceReq = new Resource(od.getPowerReq(), od.getMemoryReq(), od.getBandwidthReq());
		Resource resourceMin = new Resource(od.getPowerMin(), od.getMemoryMin(), od.getBandwidthMin());

		// check if we have enough resources locally
		// NOTE could be kept if we doun't want to pass through the SN, it's faster too
		/*if (currAva.canHandle(resourceReq) || currAva.canHandle(resourceMin)) {
			POPFloat fitness = new POPFloat();
			int[] resIDs = new int[howmany];
			for (int i = 0; i < howmany; i++)
				resIDs[i] = jobManager.reserve(od, fitness, "", "");
			POPString pobjname = new POPString(objname);
			return jobManager.execObj(pobjname, howmany, resIDs, localservice.toString(), objcontacts);
		}*/
		
		// the POPAccessPoint could contains the fingerprint of the AppService certificate
		String appServiceFingerprint = localservice.getFingerprint();
		
		// use search node to find a suitable node
		SNRequest request = new SNRequest(Util.generateUUID(), resourceReq, resourceMin, network.getUUID(), descriptor.getGlobalName(), appServiceFingerprint);
		// setup request
		// distance between nodes
		if (od.getSearchMaxDepth() > 0) {
			request.setHopLimit(od.getSearchMaxDepth());
		}
		// size? not implemented
		if (od.getSearchMaxSize() > 0) {

		}
		int timeout = conf.getSearchNodeSearchTimeout();
		if (od.getSearchWaitTime() >= 0) {
			timeout = od.getSearchWaitTime();
		}
		if (!od.getPlatform().isEmpty()) {
			request.setOS(od.getPlatform());
		}
		if (od.getSearchHosts().length > 0) {
			request.setHosts(od.getSearchHosts());
		}
		// TODO get appId from AppService
		String appId = "", reqId = "";

		// send request
		SNNodesInfo remoteJobMngs = jobManager.launchDiscovery(request, timeout);
		POPAccessPoint[] chosenRemoteJobM = new POPAccessPoint[howmany];
		if (remoteJobMngs.isEmpty()) {
			throw new POPException(POPErrorCode.ALLOCATION_EXCEPTION, "No answer from the network while looking for resource " + resourceReq);
		}

		int[] resIDs = new int[howmany];
		// make requests
		for (int jobIdx = 0, jmIdx = 0, failed = 0; jobIdx < howmany; jobIdx++, jmIdx = (jmIdx + 1) % remoteJobMngs.size()) {
			// connect to remote JM
			POPJavaJobManager jm = PopJava.newActiveConnect(null, POPJavaJobManager.class, remoteJobMngs.get(jmIdx).getJobManager());
			POPMutableFloat fitness = new POPMutableFloat();
			resIDs[jobIdx] = jm.reserve(od, fitness, appId, reqId);

			// failed requests
			if (resIDs[jobIdx] == 0) {
				LogWriter.writeDebugInfo("[JM] Usable to reserve on %s", jm.getAccessPoint());
				// failed creation
				failed++;
				jobIdx--;

				jm.exit();
				if (failed == remoteJobMngs.size()) {
					// cancel previous registrations on remote jms
					for (int k = 0; k < jobIdx; k++) {
						jm = PopJava.connect(POPJavaJobManager.class, od.getNetwork(), chosenRemoteJobM[k]);
						jm.cancelReservation(new int[] { resIDs[k] }, 1);
						jm.exit();
					}
					return 1;
				}
			}
			// successful reservation
			else {
				chosenRemoteJobM[jobIdx] = jm.getAccessPoint();
				jm.exit();
			}
		}

		// execute objects
		int started = 0;
		for (int i = 0; i < howmany; i++) {
			if (!chosenRemoteJobM[i].isEmpty()) {
				POPJavaJobManager jm = PopJava.connect(POPJavaJobManager.class, od.getNetwork(), chosenRemoteJobM[i]);
				try {
					// execution
					POPString pobjname = new POPString(objname);
					int[] localRIDs = { resIDs[i] };
					POPAccessPoint[] localObjContact = { objcontacts[i] };
					int status = jm.execObj(pobjname, 1, localRIDs, localservice.toString(), localObjContact);
					// force set return
					objcontacts[i] = localObjContact[0];
					started++;
					// failed, free resources
					if (status != 0) {
						started--;
						LogWriter.writeDebugInfo("[JM] execution failed");
						jm.cancelReservation(localRIDs, 1);
						return POPErrorCode.OBJECT_NO_RESOURCE;
					}
					
					// add certificate to newly created object temporary store
					POPRemoteCaller remote = PopJava.getRemoteCaller();
					if (remote != null && remote.isSecure() && !remote.isUsingConfidenceLink()) {
						POPObject object = PopJava.connect(POPObject.class, od.getNetwork(), objcontacts[i]);
						Certificate cert = SSLUtils.getCertificate(remote.getFingerprint());
						object.PopRegisterFutureConnectorCertificate(SSLUtils.certificateBytes(cert));
						object.exit();
					}
				}
				// cancel remote registration
				catch (Exception e) {
					jm.cancelReservation(new int[] { resIDs[i] }, 1);
					return POPErrorCode.POP_JOBSERVICE_FAIL;
				} finally {
					jm.exit();
				}
			}
		}

		LogWriter.writeDebugInfo("[JM] Object count=%d, require=%d", started, howmany);
		// created all objects
		if (started >= howmany) {
			return 0;
		}

		// failed to start all objects, kill already started objects
		for (int i = 0; i < started; i++) {
			try {
				Interface obj = new Interface(null, objcontacts[i]);
				obj.kill();
			} catch (POPException e) {
				LogWriter.writeDebugInfo("[JM] Exception while killing objects: %s", e.getMessage());
			}
		}

		return POPErrorCode.POP_EXEC_FAIL;
	}

	@Override
	public void askResourcesDiscoveryAction(SNRequest request, POPAccessPoint sender, SNExploration oldExplorationList) {
		// check local resource
		Resource available = jobManager.getAvailableResources();
		
		// check local available resources to see if we can handle the request to the requester
		if (available.canHandle(request.getResourceNeeded()) ||
				available.canHandle(request.getMinResourceNeeded())) {
			// build response and give it back to the original sender
			SNNodesInfo.Node nodeinfo = new SNNodesInfo.Node(jobManager.getNodeId(), jobManager.getAccessPoint(), POPSystem.getPlatform(), available);
			SNResponse response = new SNResponse(request.getUID(), request.getNetworkUUID(), request.getExplorationList(), nodeinfo);

			// we want to save the requester's certificate if there is one
			if (request.getPublicCertificate().length > 0) {
				SSLUtils.addCertToTempStore(request.getPublicCertificate());
			}
			
			// we want to save the AppService's node certiicate
			if (request.getAppServiceCertificate().length > 0) {
				SSLUtils.addCertToTempStore(request.getAppServiceCertificate());
			}

			// route response to the original JM
			jobManager.rerouteResponse(response, new SNWayback(request.getWayback()));
		}
	}

	@Override
	public boolean broadcastPresence() {
		return true;
	}
}
