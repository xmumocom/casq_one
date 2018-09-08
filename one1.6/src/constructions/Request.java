package constructions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import core.Coord;
import core.MessageCenter;
import core.SimClock;

public class Request {
	private int createdType=0;//0Ϊͨ�����������1Ϊͨ���켣����
	private int isReplyed=0;//����������ʾ��request�Ƿ񱻻ظ������޷����ظ�����ʼֵΪ0������ȴ��ظ���ֵΪ1�����ѻظ���ֵΪ2�����request���ܱ��ظ�
	private Coord location;//Ҫ��ѯ�����ݵĵص�
	private double time;//��ѯ��ʱ��
	private int type;//Ҫ��ѯ�����ݵ�����
	private double size=0;//��ѯ�������С���ݴ�С
	private int level;//��ѯ�ľ��ȼ���
	//ά��
	private Map<String,Double> dims=new LinkedHashMap<String,Double>();
	
	//status���ڱ�ʾһ��request��Ҫup��0����Ҫdown:1
	private int status;
	
	public Request(Coord l,double t,int ty,int level,int sta){
		this.location=l;
		this.time=t;
		this.type=ty;
		this.level=level;
		this.status=sta;
	}
	//���request�е�ά��
	public void addDim(String dimension,double d){
		this.dims.put(dimension, d);
	}
	/*
	 * �ж�һ�������Ƿ�����Ҫ������
	 * 1�������������͵���request���������
	 * 2���������ݵص���request����ص����
	 *3��request��ά�ȼ��������ݵ�ά�ȼ��ϵ��Ӽ�
	 */
	public boolean judgeData(Data d){
		if(this.getType()!=d.getType()) return false;
		if(this.getLocation().distance(d.getLocation())>MessageCenter.dis)
			return false;
		if(Math.abs(this.getTime()-d.getTime())>MessageCenter.okTime) return false;
		//���ñ�ʶĬ��Ϊ0��Ϊfalse
		int sign=0;
		for(String t:this.dims.keySet()){
//			if(t.equals("Size")){
//				if(d.getSize()<this.dims.get(t)){
//					sign=1;
//					break;
//				}
//			}
			if(!d.getDimensions().containsKey(t)){
				sign=1;
				break;
			}
		}
		if(sign==1) return false;
		return true;
	}

	public Coord getLocation() {
		return location;
	}

	public void setLocation(Coord loc) {
		this.location = loc;
		this.dims.remove("LonX");
		this.dims.remove("LatY");
		this.dims.put("LonX", this.location.getX());
		this.dims.put("LatY", this.location.getY());
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
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

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	public int getLevel(){
		return this.level;
	}
	public void setLevel(int level){
		this.level=level;
	}
	public String toString(){
		StringBuilder res=new StringBuilder("");
		res.append(new Time(time).toString()).append("���ȼ���").append(level).append("��ѯ����")
			.append(type).append("\n");
		for(String s:this.dims.keySet()){
			res=res.append(s).append(":").append(this.dims.get(s)).append(",");
		}
				
		return res.toString();
	}

	public Map<String,Double> getDims() {
		return dims;
	}

	public void setDims(Map<String,Double> dims) {
		this.dims = dims;
	}
	
	/*
	 * request�������ά��
	 */
	public void addDimensions(){
		if(type==0){
			//���������Ƶʱ�䳤�ȣ�5-15���ӷ�Χ��
			double lenOfTime=60*(Math.random()*10+5);
			
			//����������ɵ���Ƶʱ����������Ƶ��С
			//��ʱ����1������51.5KB��С
			this.size=51.5*lenOfTime;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==1){
		
			//ʹ�������������ͼ���С
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5) this.size*=2;
			this.size*=1024;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==2){
			
			//����������ݴ�С��25KB-125KB)
			this.size=Math.random()*100+25;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==3){
			
			//����������ݴ�С(10-25KB)
			this.size=Math.random()*15+10;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==4){
			
			//������ɰ���ͼƬ���ݴ�С
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5) this.size*=2;
			this.size*=1024;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}
	}
	public int getIsReplyed() {
		return isReplyed;
	}
	public void setIsReplyed(int isReplyed) {
		this.isReplyed = isReplyed;
	}
	public int getCreatedType() {
		return createdType;
	}
	public void setCreatedType(int createdType) {
		this.createdType = createdType;
	}

}
