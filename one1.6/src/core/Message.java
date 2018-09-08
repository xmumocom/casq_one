/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A message that is created at a node or passed between nodes.
 */
public class Message implements Comparable<Message> {
	/** Value for infinite TTL of message */
	public static final int INFINITE_TTL = -1;
	private DTNHost from;
	private DTNHost to;
	/** Identifier of the message */
	private String id;
	/** Size of the message (bytes) */
	private int size;
	/** List of nodes this message has passed */
	private List<DTNHost> path;
	/** Next unique identifier to be given */
	private static int nextUniqueId;
	/** Unique ID of this message */
	private int uniqueId;
	/** The time this message was received */
	private double timeReceived;
	/** The time when this message was created */
	private double timeCreated;
	/** Initial TTL of the message */
	private int initTtl;

	/** if a response to this message is required, this is the size of the
	 * response message (or 0 if no response is requested) */
	private int responseSize;
	/** if this message is a response message, this is set to the request msg*/
	private Message requestMsg;

	/** Container for generic message properties. Note that all values
	 * stored in the properties should be immutable because only a shallow
	 * copy of the properties is made when replicating messages */
	private Map<String, Object> properties=new HashMap<>();

	/** Application ID of the application that created the message */
	private String	appID;
	
	//������Ϣ�ķ��ͺͽ���ʱ��
	private double receiveQueryTime;
	private double receiveReplyTime;
	
	//��Ϣ���ƶ˺�rsu֮��Ĵ���ʱ�䣬Ĭ��Ϊ0
	private double tranTimeRAC=0;
	
	//��Ϣ���ͣ�0:һ����Ϣ��1����ѯ��Ϣ��2���ظ���Ϣ��3�����ݴ�����Ϣ��4��RSU������ȡ���ݴ���
	private int type;
	public static final int Gener_Type=0;
	public static final int Query_Type=1;
	public static final int Reply_Type=2;
	public static final int Data_Transfer_Type=3;
	public static final int Pull_Data_Type=4;
	//�ظ���Ϣ�Ľ���ʱ��
	private Long replyTime=(long) 0;
	static {
		reset();
		DTNSim.registerForReset(Message.class.getCanonicalName());
	}

	/**
	 * Creates a new Message.
	 * @param from Who the message is (originally) from
	 * @param to Who the message is (originally) to
	 * @param id Message identifier (must be unique for message but
	 * 	will be the same for all replicates of the message)
	 * @param size Size of the message (in bytes)
	 */
	public Message(DTNHost from, DTNHost to, String id, int size) {
		this.from = from;
		this.to = to;
		this.id = id;
		this.size = size;
		this.path = new ArrayList<DTNHost>();
		this.uniqueId = nextUniqueId;

		this.receiveQueryTime=this.receiveReplyTime=(long) 0;
		
		this.timeCreated = SimClock.getTime();
		this.timeReceived = this.timeCreated;
		this.initTtl = INFINITE_TTL;
		this.responseSize = 0;
		this.requestMsg = null;
		this.properties = null;
		this.appID = null;
		this.type=this.Gener_Type;
		Message.nextUniqueId++;
		addNodeOnPath(from);
	}
	public Message(DTNHost from, DTNHost to, String id, int size,int type) {
		this.receiveQueryTime=this.receiveReplyTime=(long) 0;
		
		
		this.from = from;
		this.to = to;
		this.id = id;
		this.size = size;
		this.path = new ArrayList<DTNHost>();
		this.uniqueId = nextUniqueId;

		this.timeCreated = SimClock.getTime();
		this.timeReceived = this.timeCreated;
		this.initTtl = INFINITE_TTL;
		this.responseSize = 0;
		this.requestMsg = null;
		this.properties = null;
		this.appID = null;
		this.type=type;
		Message.nextUniqueId++;
		addNodeOnPath(from);
	}

	/**
	 * Returns the node this message is originally from
	 * @return the node this message is originally from
	 */
	public DTNHost getFrom() {
		return this.from;
	}

	/**
	 * Returns the node this message is originally to
	 * @return the node this message is originally to
	 */
	public DTNHost getTo() {
		return this.to;
	}

	/**
	 * Returns the ID of the message
	 * @return The message id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Returns an ID that is unique per message instance
	 * (different for replicates too)
	 * @return The unique id
	 */
	public int getUniqueId() {
		return this.uniqueId;
	}

	/**
	 * Returns the size of the message (in bytes)
	 * @return the size of the message
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Adds a new node on the list of nodes this message has passed
	 * @param node The node to add
	 */
	public void addNodeOnPath(DTNHost node) {
		this.path.add(node);
	}

	/**
	 * Returns a list of nodes this message has passed so far
	 * @return The list as vector
	 */
	public List<DTNHost> getHops() {
		return this.path;
	}

	/**
	 * Returns the amount of hops this message has passed
	 * @return the amount of hops this message has passed
	 */
	public int getHopCount() {
		return this.path.size() -1;
	}

	/**
	 * Returns the time to live (minutes) of the message or Integer.MAX_VALUE
	 * if the TTL is infinite. Returned value can be negative if the TTL has
	 * passed already.
	 * @return The TTL (minutes)
	 */
	public int getTtl() {
		if (this.initTtl == INFINITE_TTL) {
			return Integer.MAX_VALUE;
		}
		else {
			return (int)( ((this.initTtl * 60) -
					(SimClock.getTime()-this.timeCreated)) /60.0 );
		}
	}


	/**
	 * Sets the initial TTL (time-to-live) for this message. The initial
	 * TTL is the TTL when the original message was created. The current TTL
	 * is calculated based on the time of
	 * @param ttl The time-to-live to set
	 */
	public void setTtl(int ttl) {
		this.initTtl = ttl;
	}

	/**
	 * Sets the time when this message was received.
	 * @param time The time to set
	 */
	public void setReceiveTime(double time) {
		this.timeReceived = time;
	}

	/**
	 * Returns the time when this message was received
	 * @return The time
	 */
	public double getReceiveTime() {
		return this.timeReceived;
	}

	/**
	 * Returns the time when this message was created
	 * @return the time when this message was created
	 */
	public double getCreationTime() {
		return this.timeCreated;
	}

	public void setCreationTime(double time) {
		this.timeCreated=time;
	}
	/**
	 * If this message is a response to a request, sets the request message
	 * @param request The request message
	 */
	public void setRequest(Message request) {
		this.requestMsg = request;
	}

	
	
	/**
	 * Returns the message this message is response to or null if this is not
	 * a response message
	 * @return the message this message is response to
	 */
	public Message getRequest() {
		return this.requestMsg;
	}

	/**
	 * Returns true if this message is a response message
	 * @return true if this message is a response message
	 */
	public boolean isResponse() {
		return this.requestMsg != null;
	}

	/**
	 * Sets the requested response message's size. If size == 0, no response
	 * is requested (default)
	 * @param size Size of the response message
	 */
	public void setResponseSize(int size) {
		this.responseSize = size;
	}

	/**
	 * Returns the size of the requested response message or 0 if no response
	 * is requested.
	 * @return the size of the requested response message
	 */
	public int getResponseSize() {
		return responseSize;
	}

	
	/**
	 * Returns a string representation of the message
	 * @return a string representation of the message
	 */
	public String toString () {
		String res="id: "+this.getId();
		if(this.properties!=null){
			Set<String> keySet=this.properties.keySet();
			int sign=0;
			for(String key:keySet){
				if(!key.contains("Router"))
				res=res+","+"type:"+this.getType()+"key:"+key+" value:";
//				+this.properties.get(key).toString()+"	";
			}
		}
		return res;
	}

	/**
	 * Deep copies message data from other message. If new fields are
	 * introduced to this class, most likely they should be copied here too
	 * (unless done in constructor).
	 * @param m The message where the data is copied
	 */
	protected void copyFrom(Message m) {
		this.path = new ArrayList<DTNHost>(m.path);
		this.timeCreated = m.timeCreated;
		this.responseSize = m.responseSize;
		this.requestMsg  = m.requestMsg;
		this.initTtl = m.initTtl;
		this.appID = m.appID;
		this.type=m.getType();
		this.receiveQueryTime=m.receiveQueryTime;
		this.receiveReplyTime=m.receiveReplyTime;
		this.replyTime=m.replyTime;
		this.tranTimeRAC=m.tranTimeRAC;
		if (m.properties != null) {
			Set<String> keys = m.properties.keySet();
			for (String key : keys) {
				updateProperty(key, m.getProperty(key));
			}
		}
	}

	/**
	 * Adds a generic property for this message. The key can be any string but
	 * it should be such that no other class accidently uses the same value.
	 * The value can be any object but it's good idea to store only immutable
	 * objects because when message is replicated, only a shallow copy of the
	 * properties is made.
	 * @param key The key which is used to lookup the value
	 * @param value The value to store
	 * @throws SimError if the message already has a value for the given key
	 */
	public void addProperty(String key, Object value) throws SimError {
		if (this.properties != null && this.properties.containsKey(key)) {
			/* check to prevent accidental name space collisions */
			throw new SimError("Message " + this + " already contains value " +
					"for a key " + key);
		}

		this.updateProperty(key, value);
	}

	/**
	 * Returns an object that was stored to this message using the given
	 * key. If such object is not found, null is returned.
	 * @param key The key used to lookup the object
	 * @return The stored object or null if it isn't found
	 */
	public Object getProperty(String key) {
		if (this.properties == null) {
			return null;
		}
		return this.properties.get(key);
	}

	/**
	 * Updates a value for an existing property. For storing the value first
	 * time, {@link #addProperty(String, Object)} should be used which
	 * checks for name space clashes.
	 * @param key The key which is used to lookup the value
	 * @param value The new value to store
	 */
	public void updateProperty(String key, Object value) throws SimError {
		if (this.properties == null) {
			/* lazy creation to prevent performance overhead for classes
			   that don't use the property feature  */
			this.properties = new HashMap<String, Object>();
		}
		this.properties.put(key, value);
	}

	/**
	 * Returns a replicate of this message (identical except for the unique id)
	 * @return A replicate of the message
	 */
	public Message replicate() {
		Message m = new Message(from, to, id, size);
		m.copyFrom(this);
		return m;
	}

	/**
	 * Compares two messages by their ID (alphabetically).
	 * @see String#compareTo(String)
	 */
	public int compareTo(Message m) {
		return toString().compareTo(m.toString());
	}

	/**
	 * Resets all static fields to default values
	 */
	public static void reset() {
		nextUniqueId = 0;
	}

	/**
	 * @return the appID
	 */
	public String getAppID() {
		return appID;
	}

	/**
	 * @param appID the appID to set
	 */
	public void setAppID(String appID) {
		this.appID = appID;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	
	public Set<String> getProKeys(){
		return this.properties.keySet();
	}
	//����Ŀ�Ľڵ�
	public void setTo(DTNHost dto) {
		this.to=dto;
	}
	//������ʼ�ڵ�
	public void setFrom(DTNHost fro){
		this.from=fro;
	}
	public Long getReplyTime() {
		return replyTime;
	}
	public void setReplyTime(Long replyTime) {
		this.replyTime = replyTime;
	}
	
	public double getReceiveQueryTime() {
		return this.receiveQueryTime;
	}
	public void setReceiveQueryTime(double sendQueryTime) {
		this.receiveQueryTime = sendQueryTime;
	}
	public double getReceiveReplyTime() {
		return receiveReplyTime;
	}
	public void setReceiveReplyTime(double receiveReplyTime) {
		this.receiveReplyTime = receiveReplyTime;
	}
	public void setSize(int s){
		this.size=s;
	}
	public double getTranTimeRAC() {
		return tranTimeRAC;
	}
	public void setTranTimeRAC(double tranTimeRAC) {
		this.tranTimeRAC = tranTimeRAC;
	}
}
