package ch.icosys.popjava.core.service.jobmanager;

import java.nio.file.Path;
import java.util.Objects;

import ch.icosys.popjava.core.baseobject.ObjectDescription;
import ch.icosys.popjava.core.baseobject.POPAccessPoint;

/**
 * This is an Application/Object own resource
 *
 * @author Davide Mazzoleni
 */
public class AppResource extends Resource {

	protected int id;

	protected String appId;

	protected String reqId;

	protected long accessTime;

	protected POPAccessPoint contact;

	protected POPAccessPoint appService;

	protected ObjectDescription od;

	protected Path appDirectory;

	protected boolean used;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getReqId() {
		return reqId;
	}

	public void setReqId(String reqId) {
		this.reqId = reqId;
	}

	public long getAccessTime() {
		return accessTime;
	}

	public void setAccessTime(long accessTime) {
		this.accessTime = accessTime;
	}

	public POPAccessPoint getContact() {
		return contact;
	}

	public void setContact(POPAccessPoint contact) {
		this.contact = contact;
	}

	public POPAccessPoint getAppService() {
		return appService;
	}

	public void setAppService(POPAccessPoint appService) {
		this.appService = appService;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public ObjectDescription getOd() {
		return od;
	}

	public void setOd(ObjectDescription od) {
		this.od = od;
	}

	public Path getAppDirectory() {
		return appDirectory;
	}

	public void setAppDirectory(Path appDirectory) {
		this.appDirectory = appDirectory;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 41 * hash + this.id;
		hash = 41 * hash + Objects.hashCode(this.appId);
		hash = 41 * hash + Objects.hashCode(this.reqId);
		hash = 41 * hash + Objects.hashCode(this.contact);
		hash = 41 * hash + Objects.hashCode(this.appService);
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
		final AppResource other = (AppResource) obj;
		if (this.id != other.id) {
			return false;
		}
		if (!Objects.equals(this.appId, other.appId)) {
			return false;
		}
		if (!Objects.equals(this.reqId, other.reqId)) {
			return false;
		}
		if (!Objects.equals(this.contact, other.contact)) {
			return false;
		}
		if (!Objects.equals(this.appService, other.appService)) {
			return false;
		}
		return true;
	}

}
