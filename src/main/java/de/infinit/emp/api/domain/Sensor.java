package de.infinit.emp.api.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import de.infinit.emp.Application;
import de.infinit.emp.api.model.CapabilityModel;
import de.infinit.emp.api.model.SensorModel;

@DatabaseTable(tableName = "sensors")
public class Sensor {
	static final Logger log = Logger.getLogger(Sensor.class.getName());

	@DatabaseField(generatedId = true)
	UUID uuid;

	@NotNull
	@Pattern(regexp = "^.{10,50}$")
	@DatabaseField(unique = true, canBeNull = false)
	String code;

	@NotNull
	@DatabaseField(unique = true, canBeNull = false)
	String sdevice;

	@Pattern(regexp = "^.{0,200}$")
	@SerializedName("description")
	@DatabaseField
	String description;

	@DatabaseField(defaultValue = "EnergyCam")
	String model;

	@SerializedName("recv_interval")
	@DatabaseField()
	int recvInterval;

	@DatabaseField
	@SerializedName("recv_time")
	long recvTime;

	@SerializedName("battery_ok")
	@DatabaseField()
	boolean batteryOk;

	@DatabaseField(foreign = true, columnName = "owner_id", foreignAutoRefresh = true)
	private transient User owner;

	@ForeignCollectionField
	private transient Collection<Tag> tags;

	@ForeignCollectionField(orderColumnName = "order", orderAscending = true)
	private transient Collection<Capability> capabilities;

	@DatabaseField
	@SerializedName("is_event_sent")
	boolean eventSent;

	ScheduledFuture<?> future;

	public Sensor() {
		// ORMLite needs a no-arg constructor
		this.tags = new ArrayList<>();
		this.capabilities = new ArrayList<>();
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSdevice() {
		return sdevice;
	}

	public void setSdevice(String sdevice) {
		this.sdevice = sdevice;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getRecvInterval() {
		return recvInterval;
	}

	public void setRecvInterval(int recvInterval) {
		this.recvInterval = recvInterval;
	}

	public long getRecvTime() {
		return recvTime;
	}

	public void setRecvTime(long recvTime) {
		this.recvTime = recvTime;
	}

	public boolean isBatteryOk() {
		return batteryOk;
	}

	public void setBatteryOk(boolean batteryOk) {
		this.batteryOk = batteryOk;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public boolean isEventSent() {
		return eventSent;
	}

	public void setEventSent(boolean sentAsEvent) {
		this.eventSent = sentAsEvent;
	}

	public Collection<Tag> getTags() {
		return this.tags;
	}

	public void setTags(Collection<Tag> tags) {
		this.tags = tags;
	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(Collection<Capability> capabilities) {
		this.capabilities = capabilities;
	}

	public UUID getOwnerUuid() {
		for (Tag tag : tags) {
			for (Policy policy : tag.getPolicies()) {
				if (policy.getPolicy() == Policy.OWNER) {
					return tag.getUuid();
				}
			}
		}
		return null;
	}

	// ordering is ensured by ORMLite
	// see @ForeignCollectionField configuration above
	public Collection<Capability> getCapabilitiesByOrder() {
		return getCapabilities();
	}

	public void startSimulation() {
		Runnable task = () -> {
			if (isEventSent()) {
				return;
			}
			for (Capability c : getCapabilitiesByOrder()) {
				if (c.getDelta() != null) {
					Long value = c.getValue();
					c.setValue(value + c.getDelta());
					CapabilityModel.instance().update(c);
				}
			}
			setRecvTime(Instant.now().getEpochSecond());
			setEventSent(false);
			SensorModel.instance().update(this);
		};
		ScheduledExecutorService executor = Application.getScheduledExecutor();
		future = executor.scheduleWithFixedDelay(task, 0, getRecvInterval(), TimeUnit.SECONDS);
	}

	public void stopSimulation() {
		if (future != null)
			future.cancel(false);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sensor other = (Sensor) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
