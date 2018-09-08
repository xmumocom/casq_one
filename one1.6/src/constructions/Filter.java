package constructions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import core.Coord;
import core.DTNHost;
import core.MessageCenter;
import core.SimClock;

public class Filter {
	public FilterUpdateTimer filterUpdateTimer=new FilterUpdateTimer(SimClock.getTime());
	public Map<Integer,CellStatus> basicInfo=new LinkedHashMap<Integer,CellStatus>();
	//filter���һ�θ��¿���״̬��ʱ��
	public double nearUpdateStatusTime=0;
	//filter���Ƶ������б�ʹ�õĸ���
	private int usedDatas=0;
	//filter���Ƶ������б���������ʹ�ô���Ϊ0������
	private int unusedPassDatas=0;
	//filter���Ƶ������в������������Ǳ�ʹ�ò�ѯ��ε�����
	private int usedBlockDatas=0;
	
	//����cost_radio�Ĳ�����factor for data transmission
	private double dataTransmFactor=1;
	//����cost_radio�Ĳ�����factor for request transmission
	private double reqTransmFactor=40;
	private int type;//filter Ҫ����������ͣ��ı�/ͼ��/��Ƶ/��Ƶ
	private double time;//filter����ʱ��
	private Coord loc;//filter��������
	private int size;//�������洢��
	private int level;//Ҫ��ȡ�����ݵľ��ȵȼ�
	private int status;//��ʾfilter״̬open:0/close:1/basic:2/more:3
	private double periodTime=1200;//��һ��filter����ʱ��
	private double basicTime=this.getTime();//��һ��filter�����¼�����ʼʱ��
	private int basicStatus=0;//һ��filter�Ƿ�ָΪbasic filter��0�����ǣ�1�����ǣ�Ĭ��Ϊ0
	private double threshold;//��ֵ�������ж�һ����Ϣ�Ƿ��ʺ�
	private int newTimes;
	private int oldTimes;
	private Map<String,Splits> dims=new LinkedHashMap<String,Splits>();
	public Filter(int type,Coord loc,int level,int state){
		this.time=SimClock.getTime();
		this.type=type;
		this.loc=loc;
		this.level=level;
		this.setStatus(state);
		this.setBasicStatus(0);
		this.setNewTimes(this.setOldTimes(0));
	}
//	public Filter copy(){
//		Filter f=new Filter(this.getType(),this.getLoc()
//				,this.getLevel(),this.getStatus());
//		f.setDataTransmFactor(dataTransmFactor);
//		f.setReqTransmFactor(reqTransmFactor);
//		f.setTime(time);
//		f.setSize(size);
//		f.setLevel(level);
//		f.setStatus(status);
//		f.setPeriodTime(periodTime);
//		f.setBasicTime(basicTime);
//		f.setThreshold(threshold);
//		f.setNewTimes(newTimes);
//		f.setOldTimes(oldTimes);
//		f.setDims(dims);
//		f.setBasicStatus(0);
//		return f;
//	}
//	//����һ��ֵ�ж��Ƿ�Ӧ��������
//	//1��������0��������
//	public int evaluData(Data d){
//		if(d.getSize()>this.getSize()) return 1;
//		else return 0;
//	}
	//���filter�е�ά��,���ݵ�����key��value���ж�
	public void addDimension(String key,double value1,double value2){
		Splits s=new Splits(key,1,value1,value2);
		this.dims.put(key, s);
	}
	/*
	 * filter tostring,չʾfilter�Ľṹ�͹���
	 */
	public String toString(){
		String res="";
		res=res+"filter��ά�ȣ�\n";
		for(String s:this.dims.keySet()){
			res=res+this.dims.get(s).toString()+"\n";
		}
		return res;
	}
	/*
	 * ��ʱ����filter,�����filter��data����request��Ϊ0������Ϊclose״̬
	 * ���������cost ratio��ֵ��������1 ����Ϊopen״̬����֮��Ϊclose״̬
	 */
	public void updateStatusByRadioCost(int datas,int requests){
		if(SimClock.getTime()-this.nearUpdateStatusTime<1200) return;
		if(datas==0||requests==0) this.setStatus(1);
		else if(this.calRadioCost()>1) this.setStatus(0);
		else this.setStatus(1);
		this.nearUpdateStatusTime=SimClock.getTime();
	}
	//����filter�Ŀ����������ݵĿɷ񴫲����
	public void resetDataStatus(Data d){
		
		if(this.getStatus()==0)d.setExpandState(0);
		else {
			d.setExpandState(1);
		}
	}
	//�������ݵı�ʹ�ô�����ʱ��ĳ��ȱ����жϸ����ݵĿ��ÿ��ų̶�
	public double getUsedWithTime(Data d){
		return d.getUsageCount()/(SimClock.getTime()-d.getTime());
	}
	//�ж������Ƿ��Ƿ��ϸ�filter��Ҫ�� false:�����ϣ�true:����,Ĭ�ϲ�����
	public boolean judgeData(Data d){
		//�����ж���������ά�ȶ�������filterά��
		Map<String,Double> data=d.getDimensions();
		@SuppressWarnings("unchecked")
		Set<String> l= data.keySet();
		/*
		 * �ж����ݵ�ά���Ƿ񶼴�����filterά����
		 */
		int sign=0;//signΪ1ʱ��ʾ�����е�ά�Ȳ�������filter��
		for(String s:l){
			if(!this.getDims().containsKey(s)){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		/*
		 * �ж����ݵ�ά��ֵ����filter�Ķ�Ӧά��ֵ������֮��
		 */
		for(String s:l){
			if(!this.getDims().get(s).inRange(data.get(s))){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		
		else return true;
	}
	/*
	 * filter��ʱ��Ƭmove����
	 */
	public void move(int datas,int requests){
		this.filterUpdateTimer.move(this,datas,requests);
	}
	//�ж�request�Ƿ����filter��Ҫ��
	public boolean judgeRequest(Request r){
		//�����ж�request����ά�ȶ�������filterά��
		Map<String,Double> req=r.getDims();
		Set<String> l=req.keySet();
		/*
		* �ж����ݵ�ά���Ƿ񶼴�����filterά����
		* �ж����ݵ�ά��ֵ����filter�Ķ�Ӧά��ֵ������֮��
		*/
		int sign=0;//signΪ1ʱ��ʾ�����е�ά�Ȳ�������filter��
		for(String s:l){
			if((!this.getDims().containsKey(s))||(!this.getDims().get(s).inRange(req.get(s)))){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		else return true;
	}
	//�ж�һ��filter�Ƿ�������һ��filter����
	public boolean isEqual(Filter f){
		if(this.getType()==f.getType())return true;
		else return false;
	}
	//�������е�filter�������µ�filter
	public static Filter copyFromFilter(Filter f){
		Filter nf=new Filter(f.getType(),f.getLoc(),f.getLevel(),f.getStatus());
		for(Entry<String,Splits> entry:f.getDims().entrySet()){
			String s=entry.getKey();
			Splits sp=entry.getValue();
			nf.addDimension(sp.getDimension(), sp.getMinBord(), sp.getMaxBord());
		}
		return nf;
	}
	

	//����filter��radio cost
	public double calRadioCost(){
		double res=0;
		double dataMatchRatio=this.filterUpdateTimer.getDataMatchRatio();
		double requestToDataRatio=this.filterUpdateTimer.getRequestMatchRatio();
		res= dataMatchRatio+(this.reqTransmFactor/this.dataTransmFactor)*requestToDataRatio;
		
		return res;
	}
	public int getMaxCellNum(){
		int res=1;
		for(String s:this.getDims().keySet()){
			res=res*FilterCube.getMaxSplits(s);
		}
		return res;
	}
	//����basic filter����Ƭ��״̬
	public int calStatus(double usedDatas,double datas,double requests){
		int res=0;
		double dataMatchRatio=0;
		double requestToDataRatio=0;
		if(datas==0) res=1;
		else{
			dataMatchRatio=usedDatas/datas;
			requestToDataRatio=requests/datas;
			if((dataMatchRatio+(this.reqTransmFactor/this.dataTransmFactor)*requestToDataRatio)>1)
				res=0;
			else res=1;
			
		}
		return res;
	}
//	//����ĳ��filter�е�data match ratio,�������ݵ�ʹ�ô����ж�
//	public double dataMatchRatio(List<Data> datas){
//		if(datas.size()==0) return 0;
//		int newNum=0;
//		
//		if(newNum>this.usedDatas) newNum=this.usedDatas;
//		return (double)newNum/datas.size();
//	}
	
//	//����ĳ��filter��request to data ratio
//	public double requestToDataRatio(List<Data>datas,List<Request>requests){
//		return (double)requests.size()/datas.size();
//	}
	//����filter��mistake factor
	public double getMismatchFactor(List<Data>datas){
		double res=0.5;
		return res*(this.usedBlockDatas+this.unusedPassDatas)/datas.size();
	}
	/*
	 * ����һ��filter�ǲ�����һ��filter��base filter
	 */
	public boolean isBased(Filter f){
		Iterator it=this.dims.keySet().iterator();
		while(it.hasNext()){
			String d=(String) it.next();
			//�жϸ���ά��ֵ�Ƿ��Ǹ�ά���ϵĻ���ֵ������ʱ��
//			if(d!="Time"&&this.dims.get(d)!=0){
//				return false;
//			}
			//�ж�Ҫ�Ƚϵ�filter�Ƿ������filter��ȫ��ά��
			if(!f.getDims().containsKey(d)){
				return false;
			}
		}
		return true;
	}
//	/*
//	 * ��������filter���ɻ���filter
//	 */
//	public Filter generBasicFilter(){
//		Filter f=this.copy();
//		return f;
//	}

	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public Coord getLoc() {
		return loc;
	}
	public void setLoc(Coord loc) {
		this.loc = loc;
	}
	
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	
	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public int getStatus() {
		return this.status;
	}
	public void setStatus(int state) {
		this.status = state;
	}
	public int getSize(){
		return this.size;
	}
	public void setSize(int size){
		this.size=size;
	}
	public int getNewTimes() {
		return newTimes;
	}
	public void setNewTimes(int newTimes) {
		this.newTimes = newTimes;
	}
	public int getOldTimes() {
		return oldTimes;
	}
	public int setOldTimes(int oldTimes) {
		this.oldTimes = oldTimes;
		return oldTimes;
	}

	public double getDataTransmFactor() {
		return dataTransmFactor;
	}
	public void setDataTransmFactor(double dataTransmFactor) {
		this.dataTransmFactor = dataTransmFactor;
	}
	public double getReqTransmFactor() {
		return reqTransmFactor;
	}
	public void setReqTransmFactor(double reqTransmFactor) {
		this.reqTransmFactor = reqTransmFactor;
	}
	public double getPeriodTime() {
		return periodTime;
	}
	public void setPeriodTime(double periodTime) {
		this.periodTime = periodTime;
	}
	public double getBasicTime() {
		return basicTime;
	}
	public void setBasicTime(double basicTime) {
		this.basicTime = basicTime;
	}
	public Map<String,Splits> getDims(){
		return this.dims;
	}
	public void setDims(Map<String,Splits> m){
		this.dims=m;
	}
	public int getBasicStatus() {
		return basicStatus;
	}
	public void setBasicStatus(int basicStatus) {
		this.basicStatus = basicStatus;
		if(this.basicStatus==1){
			this.basicInfo.clear();
			for(int i=0;i<this.getMaxCellNum();i++){
				CellStatus s=new CellStatus();
				this.basicInfo.put(i, s);
			}
		}else if(this.basicStatus==0){
			this.basicInfo.clear();
			for(int i=0;i<this.getMaxCellNum();i++){
				CellStatus s=new CellStatus();
				this.basicInfo.put(i, s);
			}
		}
	}
	public int getUsedDatas() {
		return usedDatas;
	}
	public void setUsedDatas(int usedDatas) {
		this.usedDatas = usedDatas;
	}
	public void addUsedDatas(int i){
		this.setUsedDatas(this.getUsedDatas()+i);
	}
	public void addUnusedPassDatas(int i){
		this.setUnusedPassDatas(this.getUnusedPassDatas()+i);
	}
	public void addUsedBlockDatas(int i){
		this.setUsedBlockDatas(this.getUsedBlockDatas()+i);
	}
	public int getUnusedPassDatas() {
		return unusedPassDatas;
	}
	public void setUnusedPassDatas(int unusedPassDatas) {
		this.unusedPassDatas = unusedPassDatas;
	}
	public int getUsedBlockDatas() {
		return usedBlockDatas;
	}
	public void setUsedBlockDatas(int usedBlockDatas) {
		this.usedBlockDatas = usedBlockDatas;
	}
}
