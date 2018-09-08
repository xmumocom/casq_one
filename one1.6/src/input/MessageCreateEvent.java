/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import constructions.Request;
import core.DTNHost;
import core.Message;
import core.MessageCenter;
import core.SimClock;
import core.World;

/**
 * External event for creating a message.
 */
public class MessageCreateEvent extends MessageEvent {
	private int size;
	private int responseSize;
	//��Ϣ���ͣ�0:һ����Ϣ��1����ѯ��Ϣ��2���ظ���Ϣ��3�����ݴ�����Ϣ
	private int eventType;

	/**
	 * Creates a message creation event with a optional response request
	 * @param from The creator of the message
	 * @param to Where the message is destined to
	 * @param id ID of the message
	 * @param size Size of the message
	 * @param responseSize Size of the requested response message or 0 if
	 * no response is requested
	 * @param time Time, when the message is created
	 * ��������Ĭ��Ϊ��ѯ��Ϣ
	 */
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
		this.eventType=1;
	}
	public MessageCreateEvent(int from, int to, String id, int size,
			int responseSize, double time,int type) {
		super(from,to, id, time);
		this.size = size;
		this.responseSize = responseSize;
		this.eventType=type;
	}


	


	/**
	 * Creates the message this event represents.
	 */
	@Override
	public void processEvent(World world) {
		DTNHost to = world.getNodeByAddress(this.toAddr);
		DTNHost from = world.getNodeByAddress(this.fromAddr);

		
		
		//�ж��Ƿ�Ϊ��ѯ�¼�����eventTypeΪ1 ����Ϊ��ѯ�¼���
		if(this.eventType==Message.Query_Type) {
			//���¼��ǲ�ѯ�����¼���������������Ϣ��id,��ʽΪ����query��+������ѯ�Ľڵ�����+��ʼ��������Ϣ��ʱ�䣩
			Message m = new Message(from, to, "query"+from.getName()+System.currentTimeMillis(), this.size);
			m.setResponseSize(this.responseSize);
			
			//������Ϣ����Ϊquery
			m.setType(Message.Query_Type);
			
			from.createNewMessage(m);
		}
		else if(this.eventType==0){
			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
//			System.out.println("��Ϣ�����¼�������һ���¼�");
			m.setType(Message.Gener_Type);
			from.createNewMessage(m);
		}else if(this.eventType==2){
			Message m = new Message(from, to, this.id, this.size);
			m.setResponseSize(this.responseSize);
//			System.out.println("��Ϣ�����¼��������¼�����Ϊ�ظ��¼�");
			m.setType(Message.Reply_Type);
			from.createNewMessage(m);
		}
		
	}

	@Override
	public String toString() {
		return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
		"size:" + size + " CREATE";
	}
}
