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
	private int createdType=0;//0为通过随机创建，1为通过轨迹创建
	private int isReplyed=0;//该项用来表示该request是否被回复，或无法被回复。初始值为0，代表等待回复，值为1代表已回复，值为2代表该request不能被回复
	private Coord location;//要查询的数据的地点
	private double time;//查询的时间
	private int type;//要查询的数据的类型
	private double size=0;//查询需求的最小数据大小
	private int level;//查询的精度级别
	//维度
	private Map<String,Double> dims=new LinkedHashMap<String,Double>();
	
	//status用于表示一个request是要up：0还是要down:1
	private int status;
	
	public Request(Coord l,double t,int ty,int level,int sta){
		this.location=l;
		this.time=t;
		this.type=ty;
		this.level=level;
		this.status=sta;
	}
	//添加request中的维度
	public void addDim(String dimension,double d){
		this.dims.put(dimension, d);
	}
	/*
	 * 判断一条数据是否是想要的数据
	 * 1、本地数据类型等于request请求的数据
	 * 2、本地数据地点与request请求地点相近
	 *3、request的维度集合是数据的维度集合的子集
	 */
	public boolean judgeData(Data d){
		if(this.getType()!=d.getType()) return false;
		if(this.getLocation().distance(d.getLocation())>MessageCenter.dis)
			return false;
		if(Math.abs(this.getTime()-d.getTime())>MessageCenter.okTime) return false;
		//设置标识默认为0即为false
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
		res.append(new Time(time).toString()).append("精度级别").append(level).append("查询类型")
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
	 * request添加数据维度
	 */
	public void addDimensions(){
		if(type==0){
			//随机生成视频时间长度（5-15分钟范围）
			double lenOfTime=60*(Math.random()*10+5);
			
			//根据随机生成的视频时长来生成视频大小
			//暂时假设1秒钟有51.5KB大小
			this.size=51.5*lenOfTime;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==1){
		
			//使用随机数据生成图像大小
			this.size=Math.random();
			if(this.size<0.2) this.size+=0.3;
			else if(this.size>=0.5) this.size*=2;
			this.size*=1024;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==2){
			
			//随机生成数据大小（25KB-125KB)
			this.size=Math.random()*100+25;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==3){
			
			//随机生成数据大小(10-25KB)
			this.size=Math.random()*15+10;
			this.dims.put("Size", this.size);
			this.dims.put("LonX", this.location.getX());
			this.dims.put("LatY", this.location.getY());
		}else if(type==4){
			
			//随机生成包含图片数据大小
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
