package constructions;

import core.SimClock;

public class FilterUpdateTimer {
	public int dis=900;
	public int len=3;
	public double beginTime=0;
	public double[] datas={0,0,0,0};
	public double[] requests={0,0,0,0};
	public double[] usedDatas={0,0,0,0};
	public FilterUpdateTimer(double time){
		this.beginTime=time;
	}
	public void addDatas(Data d){
		if(SimClock.getTime()-d.getTime()>3600) return;
		int i=(int)((3600-SimClock.getTime()+d.getTime())/dis);
		if(i>len) i=len;
		datas[i]=datas[i]+d.getSize();
	}
	public void addRequest(Request r){
		if(SimClock.getTime()-r.getTime()>3600) return;
		int i=(int)((3600-SimClock.getTime()+r.getTime())/dis);
		if(i>len) i=len;
		requests[i]=requests[i]+10;
	}
	public void addUsedDatas(Data d){
		if(SimClock.getTime()-d.getTime()>3600) return;
		int i=(int)((3600-SimClock.getTime()+d.getTime())/dis);
		if(i>len) i=len;
		usedDatas[i]=usedDatas[i]+d.getSize();
	}
	public void setBeginTime(double times){
		this.beginTime=times;
	}
	public double getBeginTime(){
		return this.beginTime;
	}
	public double getDatas(){
		double res=0;
		for(int i=0;i<=len;i++) res+=datas[i];
		return res;
	}
	public double getUsedDatas(){
		double res=0;
		for(int i=0;i<=len;i++) res+=usedDatas[i];
		return res;
	}
	public double getRequests(){
		double res=0;
		for(int i=0;i<=len;i++) res+=requests[i];
		return res;
	}
	public void moveTimer(){
		for(int i=0;i<len;i++){
			datas[i]=datas[i+1];
			requests[i]=requests[i+1];
			usedDatas[i]=usedDatas[i+1];
		}
		datas[len]=0;
		requests[len]=0;
		usedDatas[len]=0;
		this.beginTime=this.beginTime+dis;
	}
	public double getDataMatchRatio(){
		if(this.getDatas()==0||this.getRequests()==0) return 0;
		double up=0;
		for(int i=0;i<len;i++){
			if(requests[i]==0) up=up+0;
			else{
				up=up+(datas[i]>usedDatas[i]?usedDatas[i]:datas[i]);
			}
		}
		return up/this.getDatas();
	}
	public double getRequestMatchRatio(){
		if(this.getDatas()==0) return 0;
		else return this.getRequests()/this.getDatas();
	}
	public void move(Filter f,int datas,int requests){
		if(SimClock.getTime()-this.getBeginTime()>3600){
			f.updateStatusByRadioCost(datas, requests);
			this.beginTime+=dis;
			this.moveTimer();
		}
	}
}
