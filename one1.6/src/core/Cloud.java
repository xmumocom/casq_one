package core;

import static core.Constants.DEBUG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import constructions.Data;
import constructions.Filter;
import constructions.FilterCube;
import constructions.Keys;
import constructions.Request;
import constructions.Values;
import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.util.RoutingInfo;

public class Cloud {
	public static final Logger logger=LogManager.getLogger(Cloud.class.getName());
	private static Cloud myCloud=null;
	//�������ƶˣ���������location Ϊnull
	private Coord location=null;
	//�ڵ���
	private String name="Cloud";
	private Map<Data,Integer> datas=new LinkedHashMap<Data,Integer>();
    private List<Filter> filters=new ArrayList<>();
    private Map<Request,Integer> requests=new LinkedHashMap<Request, Integer>();
    //�����洢�ȴ�RSU��ȡ���ݺ���ܻظ�����Ϣ
    private List<Message> waitDataMessages=new ArrayList<>();
    private List<Message> repliedMessages=new ArrayList<>();
    private List<Message> unrepliedMessage=new ArrayList<>();
    private int type=2;//�ڵ����ͣ�����һ��ڵ㣨0����RSU��1������cloud�ڵ㣨2��
    private int time;//������¼�ڵ���µĴ���
    //Cloud�˴������ʣ�ÿ��
    private double transferSpeed=1024*10;
    //����filter���õĲ���
    
    private long numOfQuery=0;//������ʾ�ýڵ���յ���query����
    private long numOfRepliedQuery=0;//������ʾ�������Ĳ�ѯ�ڵ����Ѿ��п��Իظ������ݵĲ�ѯ����
    private double replyQueryTime=0;//����ͳ��RSU�ڵ�ظ����ݵ�ʱ��
    private long numOfRepliedImmidia=0;//���ڴ洢�ܹ����ñ������ݻظ��Ĳ�ѯ�ĸ���
  //�洢filter cube�и���filter����ά�ȵ�����һ��ʱ��
    private double oldUpdateTime=0;
    
    private double space=5*1024*1024;//������ʾ�ڵ�Ĵ洢�ռ��С
    private double restSpace=5*1024*1024;//������ʾ�ڵ��ʣ���С
    /*
     * ����dimensional split factor��balanced factor,��ʱ�ٶ�Ϊ0.5
     */
    private double balanceFactor=0.5;
    /*
     * FilterCube
     */
    private Map<Integer,FilterCube> filterCubes=new LinkedHashMap<Integer,FilterCube>();
    /*
     * Cloud �˳�ʼ������
     */
    public Cloud(){
    	this.initOriginFilterCube();    	
    }
    public void initOriginFilterCube(){
    	//��ʼ������filtercube
    	this.createOrginFilterCube();
    	for(FilterCube f:this.filterCubes.values()){
    		f.initFilterCube();
    	}
    	this.splitFilterCubeFirst();
    	for(Integer types:this.filterCubes.keySet()){
    		this.filterCubes.get(types).setFullSpace(this.space);
    		this.filterCubes.get(types).setRestSpace(this.restSpace);
    		if(types==2||types==3){
    			this.filterCubes.get(types).setFullSpace(0.5*1024*1024);
    			this.filterCubes.get(types).setRestSpace(0.5*1024*1024);
    		}
//    		this.filterCubes.get(types).showFilterCubeStruct();
    	}
    	
    }
    /*
     * �´����ݵ�edge��
     */
    public void downloadDataToEdge(Data d){
    	List<DTNHost> dtns=SimScenario.getInstance().getHosts();
    	Iterator it=dtns.iterator();
    	while(it.hasNext()){
    		DTNHost dtn=(DTNHost) it.next();
    		if(dtn.getType()==1&&d.getLocation().distance(dtn.getLocation())<MessageCenter.dis)
    			dtn.getFilterCubes().get(d.getType()).putData(d,dtn);
    	}
    }

    //�����ȡ�ռ�ռ����
	public double getRestSpaceRate() {
		// TODO Auto-generated method stub
		double alls=0;
		int nums=0;
		nums=this.filterCubes.keySet().size();
		for(Integer types:this.filterCubes.keySet()){
    		alls=alls+this.filterCubes.get(types).getRestSpace()/this.filterCubes.get(types).fullSpace;
    	}
		return alls/nums;
	}
    //����������
    public double getReplyRate(){
    	if(this.numOfQuery==0) return 0;
    	else return (double)this.numOfRepliedQuery/this.numOfQuery;
    }
    //�����ѯƽ��ռ��ʱ��
    public double getAverReplyTime(){
    	return this.replyQueryTime/this.numOfRepliedQuery;
    }
    //���㱾������������
    public double getReplyByLocalRate(){
    	if(this.numOfRepliedQuery==0) return 0;
    	else return (double)this.numOfRepliedImmidia/this.numOfRepliedQuery;
    }
    /*
     * ����ԭʼfilter cube
     */
    public void createOrginFilterCube(){
    	Filter orginFilter0=new Filter(0
    			,this.location,0,1);
    	orginFilter0.addDimension("LonX",0,13540);
    	orginFilter0.addDimension("LatY",0,15300);
    	orginFilter0.addDimension("Size",51.5*300,51.5*900);
    	
    	Filter orginFilter1=new Filter(1
    			,this.location,0,1);
    	orginFilter1.addDimension("LonX",0,13540);
    	orginFilter1.addDimension("LatY",0,15300);
    	orginFilter1.addDimension("Size",0.2*1024,2*1024);
//    	System.out.println(orginFilter.toString());
    	
    	
    	
    	Filter orginFilter2=new Filter(2
    			,this.location,0,1);
    	orginFilter2.addDimension("LonX",0,13540);
    	orginFilter2.addDimension("LatY",0,15300);
    	orginFilter2.addDimension("Size",25,125);
    	
    	Filter orginFilter3=new Filter(3
    			,this.location,0,1);
    	orginFilter3.addDimension("LonX",0,13540);
    	orginFilter3.addDimension("LatY",0,15300);
    	orginFilter3.addDimension("Size",10,25);
    	
    	Filter orginFilter4=new Filter(4
    			,this.location,0,1);
    	orginFilter4.addDimension("LonX",0,13540);
    	orginFilter4.addDimension("LatY",0,15300);
    	orginFilter4.addDimension("Size",0.2*1024,2*1024);
    	FilterCube f0=new FilterCube();
    	FilterCube f1=new FilterCube();
    	FilterCube f2=new FilterCube();
    	FilterCube f3=new FilterCube();
    	FilterCube f4=new FilterCube();
    	f0.addDimFrameByFilter(orginFilter0);
    	f1.addDimFrameByFilter(orginFilter1);
    	f2.addDimFrameByFilter(orginFilter2);
    	f3.addDimFrameByFilter(orginFilter3);
    	f4.addDimFrameByFilter(orginFilter4);
    	this.filterCubes.put(0, f0);
    	this.filterCubes.put(1, f1);
    	this.filterCubes.put(2, f2);
    	this.filterCubes.put(3, f3);
    	this.filterCubes.put(4, f4);
    }
    /*
     * ��filter cube�����з�
     */
    public void splitFilterCubeFirst(){
    	for(Integer types:this.filterCubes.keySet()){
    		//�������Ǽ������Ϊһ�����ݣ�������������ά��Ϊ4
    		int len=1;
    		double[] min=new double[len];
    		int[] split=new int[len];
    		for(int i=0;i<len;i++){
    			min[i]=1000000;
    			split[i]=1;
    		}
    		Map<Keys,Values> addKV=new LinkedHashMap<Keys,Values>();
    		List<Keys> orginKey=new ArrayList<Keys>();
    		for(Keys k:this.filterCubes.get(types).getFC().keySet()){
    			int befores=addKV.size();
    			for(int i=0;i<len;i++){
    				String dim=this.filterCubes.get(types).getDimensions().get(i);
    				for(int j=1,maxSplits=this.filterCubes.get(types).getMaxSplits(dim);j<=maxSplits;j++){
    					double dimSplitFac=this.getDimSplitFactor(this.filterCubes.get(types),k, dim, j);
    					if(dimSplitFac<min[i]){
    						min[i]=dimSplitFac;
    						split[i]=j;
    					}
    				}
    				Map<Keys,Values> newMap=this.filterCubes.get(types).splitDimension(k,this.filterCubes.get(types).getFC().get(k), dim, split[i]);
    				if(newMap!=null&&newMap.size()>1){
    					addKV.putAll(newMap);
    				}
    			}
    			if(addKV.size()>befores) orginKey.add(k);
    		}
    		if(orginKey.size()>0&&addKV.size()>1){
    			for(Keys t:orginKey) this.filterCubes.get(types).getFc().remove(t);
    			this.filterCubes.get(types).getFc().putAll(addKV);
    		}else{
    			if(this.filterCubes.get(types).getFC().keySet().size()==1){
    				Map<Keys,Values> addsNew=new LinkedHashMap<Keys,Values>();
    				Keys news=null;
    				for(Keys k:this.filterCubes.get(types).getFc().keySet()){
    					news=k;
    					for(int i=0;i<len;i++){
    						Map<Keys,Values> newMap=this.filterCubes.get(types).splitDimension(k,this.filterCubes.get(types).getFC().get(k), this.filterCubes.get(types).getDimensions().get(i), 2);
    						if(newMap.size()>1) addsNew.putAll(newMap);
    					}
    				}
    				if(news!=null&&addsNew.size()>1){
    					this.filterCubes.get(types).getFc().remove(news);
    					this.filterCubes.get(types).getFC().putAll(addsNew);
    				}
    			}
    		}
    	}

    	
    }
    /*
     * ����dimensional split factor
     */
    public double getDimSplitFactor(FilterCube f,Keys k,String di,int x){
    	double res=(this.getBalanceFactor()*(double)x)/FilterCube.getMaxSplits(di);
    	res=res+(1-this.getBalanceFactor())*f.getSumOfMis(k,di, x);
    	return res;
    }
	/*
	 * ��filter cube���и���
	 */
    public void updateFilterCubes(){
    	for(Integer types:this.filterCubes.keySet()){
        	this.filterCubes.get(types).update();
        }
    }
//    
//    //���½ڵ��еĴ洢����
//    public void updateDatas(){
////    	System.out.println(this.name+" Updating datas...................");
//    	for(Integer types:this.filterCubes.keySet()){
//    		if (this.filters==null){//��filterΪ�գ���������
//    			return;
//    		}
//        	this.filterCubes.get(types).updateDatas();
//        }
//    	
//  
//    }
    
    //ɾ���ڵ��д洢�����ݲ���
    public void deleteData(Data noda){
    	this.datas.remove(noda);
    }
    
    //��ڵ����������
    public void addData(Data d){
    	int sign=0;
    	for(Data nd:this.datas.keySet()){
    		if(nd.isEqual(d)){
    			int num=this.datas.get(nd);
    			this.datas.replace(nd, num,num+1);
    			sign=1;
    		}
    	}
    	if(sign==0) this.datas.put(d, 0);
    	
    }

    
 
	/**
	 * Returns the current location of this host.
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Sets the Node's location overriding any location set by movement model
	 * @param location The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name =name ;
	}
	
	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update() {
//		for(Integer types:this.filterCubes.keySet()){
//			//�ж�filter cube�е��������Ƿ���࣬���ǣ�������޸�ɾ��
//			if((double)this.filterCubes.get(types).getRestSpace()/this.filterCubes.get(types).fullSpace<0.1){
//				this.filterCubes.get(types).updateDatas();
//			}	
//    	}
		if(SimClock.getTime()-this.oldUpdateTime>3600){
				this.oldUpdateTime=SimClock.getTime();
				long beginTime=System.currentTimeMillis();
				for(Integer types:this.filterCubes.keySet()){
		    		this.filterCubes.get(types).update();
		    	}
				MessageCenter.filterCubeUpdateTime=MessageCenter.filterCubeUpdateTime+System.currentTimeMillis()-beginTime;
				MessageCenter.filterCubeUpdates=MessageCenter.filterCubeUpdates+1;
		}
	}
	public double getBalanceFactor() {
		return balanceFactor;
	}
	public void setBalanceFactor(double balanceFactor) {
		this.balanceFactor = balanceFactor;
	}

	/*
	 * cloud�ƶ˽��մ�edge��ȡ������
	 */
	public void receiveDataFromEdge(Data d){
		d.setExpandState(1);
		this.filterCubes.get(d.getType()).putDataForCloud(d);
	}
	public static Cloud getInstance(){
		if(myCloud==null){
			myCloud=new Cloud();
		}
		return myCloud;
	}
	/*
	 * Cloud�ƶ˻�ȡ���edge�ص���䣬�����ݴ�������Coord
	 */
	public Coord getRandLocation(){
		Coord r=null;
		List<DTNHost> ld=SimScenario.getInstance()
				.getEdges();
		int length=ld.size();
		Random random=new Random(System.currentTimeMillis());
		int rnum=random.nextInt(length);
		r=ld.get(rnum).getLocation();
		return r;
	}
	/*
	 * Cloud��ȡ������ѯ�ص��rsu
	 */
	public List<DTNHost> getEdgesFromMessage(Message m){
		List<DTNHost> res=new ArrayList<DTNHost>();
		List<DTNHost> ld=SimScenario.getInstance().getEdges();
		for(DTNHost d:ld){
			Request r=(Request) m.getProperty("Query");
			if(d.getLocation().distance(r.getLocation())
					<SimScenario.getInstance().getMaxHostRange()){
				res.add(d);
			}
		}
		return res;
	}
	/*
	 * Cloud�ƶ˶��������·ַ�ʱѡ����Ӧ��edge
	 */
	public List<DTNHost> searchDataExpandEdges(Data d){
		List<DTNHost> ld=SimScenario.getInstance().getEdges();
		List<DTNHost> res=new ArrayList<DTNHost>();
		for(DTNHost ed:ld){
			if(ed.getLocation().distance(d.getLocation())<MessageCenter.dis)
				res.add(ed);
		}
		return res;
	}
	/*
	 * �ƶ���edge�ڵ��ȡ����
	 */
	public List<Data> getDataFromEdge(Request r){
		List<Data> datas=new ArrayList<>();
		List<DTNHost> dtns=SimScenario.getInstance().getEdges();
		for(DTNHost d:dtns){
			if(d.getLocation().distance(r.getLocation())<MessageCenter.dis){
				MessageCenter.messageTransmission=MessageCenter.messageTransmission+1;
				d.getFilterCubes().get(r.getType()).putRequest(r);
				datas.addAll(d.getFilterCubes().get(r.getType()).answerRequest(r,this));
				if(datas.size()>0) return datas;
			}
		}
		return datas;
	}
	/*
	 * �ƶ˴洢��ʱ�޷��ظ�����Ϣ
	 */
	public void addToWaitDataMessage(Message m){
		this.waitDataMessages.add(m);
		this.numOfQuery++;
	}
	public void addToRepliedMessage(Message m){
		this.repliedMessages.add(m);
	}
	public void addToUnRepliedMessage(Message m){
		this.unrepliedMessage.add(m);
	}
	/*
	 * �ƶ˵���ȡ�����ݺ���֮ǰ�޷��ظ�������
	 */
	public void workOnWaitMessage(List<Data> ds){
		for(Data d:ds){
			List<Message> dels=new ArrayList<Message>();
			List<Message> unreplied=new ArrayList<Message>();
			for(Message m:this.waitDataMessages){
				Request r=(Request) m.getProperty("Query");
				if(SimClock.getTime()-r.getTime()>MessageCenter.exitTime){
					unreplied.add(m);
				}else if(r.judgeData(d)){
					this.filterCubes.get(d.getType()).putDataForCloud(d);
					//Ȼ�����ݴ��ݵ�edge node��������֮ǰ�޷��ظ�����Ϣ
					m.getTo().workOnWaitMessage(d);
					dels.add(m);
					m.setReceiveReplyTime(SimClock.getTime());
					this.addNumOfRepliedQuery();
					MessageCenter.messageTransmission=MessageCenter.messageTransmission+Math.ceil(d.getSize()/500);
					
				}
				
			}
			this.unrepliedMessage.addAll(unreplied);
			this.repliedMessages.addAll(dels);
			this.waitDataMessages.removeAll(dels);
			this.waitDataMessages.removeAll(unreplied);
		}
	}
	/*
	 * �ƶ˵���ȡ�����ݺ���֮ǰ�޷��ظ�������,����
	 */
	public void workOnWaitMessage(Data d){
		List<Message> dels=new ArrayList<Message>();
		List<Message> unreplied=new ArrayList<Message>();
		int sign=0;
		for(Message m:this.waitDataMessages){
			Request r=(Request) m.getProperty("Query");
			if(SimClock.getTime()-r.getTime()>MessageCenter.exitTime){
				unreplied.add(m);
			}else if(r.judgeData(d)){
				sign++;
				if(sign==1) this.filterCubes.get(d.getType()).putDataForCloud(d);
				//Ȼ�����ݴ��ݵ�edge node��������֮ǰ�޷��ظ�����Ϣ
				m.getTo().workOnWaitMessage(d);
				dels.add(m);
				m.setReceiveReplyTime(SimClock.getTime());
				this.addNumOfRepliedQuery();
				//��Ӽ����������´�������
				MessageCenter.messageTransmission=MessageCenter.messageTransmission+Math.ceil(d.getSize()/500);
			}
			
		}
		this.unrepliedMessage.addAll(unreplied);
		this.repliedMessages.addAll(dels);
		this.waitDataMessages.removeAll(dels);
		this.waitDataMessages.removeAll(unreplied);
	}
	public void showEffect(){
		System.out.println("Cloud��ʣ��ռ����Ϊ��"+this.getRestSpaceRate()
				+"����Ϣ��ѯ�ɹ�ƽ��ʱ��Ϊ��"+this.getAverReplyTime()
				+"����Ϣ��ѯ�ɹ���Ϊ��"+this.getReplyRate()
				+"���ɹ��ظ���Ϣ�дӱ������ݻ�ȡ�ظ��ı���Ϊ��"+this.getReplyByLocalRate());
	}
	//��ȡ���ݴ���ʱ��
	public double getTransferTime(double size){
		return size/this.getTransferSpeed();
	}
	/*
	 * �ظ���ѯ
	 */
	public List<Data> answerRequest(Request r){
		List<Data> d=this.filterCubes.get(r.getType()).answerRequest(r,this);
		return d;
	}
	public void setFilterCubes(Map<Integer,FilterCube> f){
		this.filterCubes=f;
	}
	public Map<Integer,FilterCube> getFilterCubes(){
		return this.filterCubes;
	}
	public double getTransferSpeed() {
		return transferSpeed;
	}
	public void setTransferSpeed(double transferSpeed) {
		this.transferSpeed = transferSpeed;
	}
	public List<Message> getRepliedMessages() {
		return repliedMessages;
	}
	public void setRepliedMessages(List<Message> repliedMessages) {
		this.repliedMessages = repliedMessages;
	}
	public List<Message> getUnrepliedMessage() {
		return unrepliedMessage;
	}
	public void setUnrepliedMessage(List<Message> unrepliedMessage) {
		this.unrepliedMessage = unrepliedMessage;
	}
	public long getNumOfQuery(){
		return this.numOfQuery;
	}
	public long getNumOfRepliedQuery(){
		return this.numOfRepliedQuery;
	}
	public void setNumOfQuery(long i){
		this.numOfQuery=i;
	}
	public void setNumOfRepliedQuery(long i){
		this.numOfRepliedQuery=i;
	}
	public long getNumOfRepliedImmidia() {
		return numOfRepliedImmidia;
	}
	public void setNumOfRepliedImmidia(long numOfRepliedImmidia) {
		this.numOfRepliedImmidia = numOfRepliedImmidia;
	}
	public void addNumOfRepliedImmidia() {
		this.numOfRepliedImmidia++;
	}
	public void addNumOfQuery(){
		this.numOfQuery++;
	}
	public void addNumOfRepliedQuery(){
		this.numOfRepliedQuery++;
	}
}
