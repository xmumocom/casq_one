package constructions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import core.Coord;
import core.MessageCenter;
import core.SimClock;

public class Data {
	public String savePlace="";
	//���ݲ���ʱ��
	private double time;
	private int id;//������Դ
	//���ݸ�ʽ,����5�֣���Ƶ0��ͼ��1������״̬2����ʻԱ��Ϊ3���˹����4��
	private int type;
	private double size;//���ݴ�С
	private Coord location;//���ݲ����ص�
	private int level;//���ݾ��ȵȼ�
	private double threshold=2.5;//������ֵ
	private Map<String,Double> dims=new LinkedHashMap<String, Double>();
	private int usageCount=0;//���ݱ�request���ɵĴ���
	private int expandState=0;//�����ܷ�ͨ��filter��״̬��ʾ��pass:0,close:1
	public Data(double t,int i,int ty,int l,Coord loca){
		this.time=t;
		this.id=i;
		this.type=ty;
		this.level=l;
		this.location=loca;
		this.size=0;
	}
	public void fillData(){
		if(type==0){
			//���������Ƶʱ�䳤�ȣ�5-15���ӷ�Χ��
			double lenOfTime=60*(Math.random()*10+5);
			this.dims.put("Duration",lenOfTime );
			//��������龳������0/����1,���֮5�ĸ��ʳ���
			double situation=Math.random();
			if(situation<0.0005) situation=0;
			else situation=1;
			this.dims.put("Situation", situation);
			double traSitua=Math.random()*2;
			//������ɽ�ͨ���������0/һ��1/ӵ��2
			this.dims.put("TrafficCondition", traSitua);
			//����������ɵ���Ƶʱ����������Ƶ��С
			//��ʱ����1������51.5KB��С
			this.size=51.5*lenOfTime;
			this.dims.put("Size",this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==1){
			//�����������״��������0/����1/����2
			double weather=Math.random()*2;

			this.dims.put("Weather", weather);
			//���ݵ�ǰʱ������ʱ���,�糿0/����1/����2/����3/����4
			double hour=SimClock.getTime()/3600;
			if(hour<7) hour=0;
			else if(hour<12) hour=1;
			else if(hour<14) hour=2;
			else if(hour<18) hour=3;
			else hour=4;
			
			this.dims.put("Time",hour);
			//������ɽ�ͨ״��,����0/һ��1/ӵ��2
			double trafficSitu=Math.random()*2;
			this.dims.put("TrafficCondition",trafficSitu);
			//ʹ�������������ͼ���С
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5) this.size*=2;
			this.size*=1024;
			this.dims.put("Size",this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());

		}else if(type==2){
		
			//�����������ɳ���״̬,����0/�ϲ�1
			double states=Math.random(); 
			this.dims.put("VehicleStatus",states);
			//��ʱ������ɳ���
			this.dims.put("VehicleSpeed", Math.random()*90);
			//����������ݴ�С��25KB-125KB)
			this.size=Math.random()*100+25;
			this.dims.put("Size",this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());

		}else if(type==3){
			//������ɷ�����ת��
			this.dims.put("SteeringWheelAngle",Math.random()*180);
			//������ɼ����ų̶�
			this.dims.put("GasPedal", Math.random());
			//����������ݴ�С(10-25KB)
			this.size=Math.random()*15+10;
			this.dims.put("Size",this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());

		}else if(type==4){
			//���������������ͼƬ�еĳ�����(0-9��)
			this.dims.put("NumOfVehicles",(double)((int)Math.random()*10));
			//������ɳ�����λ�ã�ƫ��0���м�1��ƫ��2��
			this.dims.put("LanePosition", (double)((int)Math.random()*3));
			//������ɰ���ͼƬ���ݴ�С
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5)this.size*=2;
			this.size*=1024;
			this.dims.put("Size",this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());

		}
	}
	public void addDimension(String key,double value){
		this.dims.put(key, value);
	}
	public Map<String,Double> getDimensions(){
		return this.dims;
	}
	 
	/*
	 * �ж������Ƿ�����һ���������ƣ�������(non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public boolean isSimalar(Data nd){
		if(this.getType()!=nd.getType()||
				(this.getTime()-nd.getTime())>MessageCenter.okTime||
				(nd.getTime()-this.getTime())>MessageCenter.okTime||
				this.getLocation().distance(nd.getLocation())>MessageCenter.dis)
			return false;
		else{
			for(String s:this.getDimensions().keySet())
				if(!nd.getDimensions().containsKey(s)||!this.isInRange(s, this.getDimensions().get(s), nd.getDimensions().get(s)))
					return false;
			
		}
		return true;
	}
	
	public String toString(){
		StringBuilder res=new StringBuilder("����������");
		res.append(this.id).append(",���ݸ�ʽΪ��").append(this.type).append(" ,���ݴ�СΪ��")
			.append(this.size);
		for(String dim:this.dims.keySet()){
			res=res.append(dim).append(":").append(this.dims.get(dim)).append(",");
		}
		return res.toString();
	}
	/*
	 * �ж����������Ƿ���һ�µ�����
	 */
	public boolean isEqual(Data d){
		//����ͨ���������󷵻�true����������һ��
		if(this.getType()!=d.getType()||
				(this.getTime()-d.getTime())>MessageCenter.okTime||
						(d.getTime()-this.getTime())>MessageCenter.okTime||
						this.getLocation().distance(d.getLocation())>MessageCenter.dis)
			return false;
		else{
			for(String s:this.getDimensions().keySet()){
				if(!d.getDimensions().containsKey(s)
						||!this.isInRange(s, this.getDimensions().get(s), d.getDimensions().get(s)))
						return false;
			}
		}
		return true;
	}
	/*
	 *�ж�һ�������ϵ�ά��ֵ����һ�����ݵĶ�Ӧά���ϵ�ֵ�Ƿ���һ����Χ�ڿ���Ϊ���
	 */
	public boolean isInRange(String s,double dest1,double dest2){
		if(!this.dims.containsKey(s)) return false;
		//���������ͨ���������Ϊ���
		if(s.equals("Weather")){
			if(Math.abs(dest1-dest2)>0.5) return false;
		}else if(s.equals("Time")){
			if(Math.abs(dest1-dest2)>0.5) return false;
		}else if(s.equals("TrafficCondition")){
			if(Math.abs(dest1-dest2)>0.5) return false;
		}else{
			if(Math.abs(dest1-dest2)>0.5) return false;
		}
		return true;
	}
	public Coord getLocation(){
		return this.location;
	}
	public void setLocation(Coord location) {
		this.location = location;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public double getTime(){
		return this.time;
	}
	public void setTime(double t){
		this.time=t;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}

	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public int getUsageCount() {
		return usageCount;
	}
	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}
	public int getExpandState() {
		return expandState;
	}
	public void setExpandState(int expandState) {
		this.expandState = expandState;
	}
	//�������ʹ�ô���
	public void addUsageCount(){
		this.usageCount++;
	}
}
