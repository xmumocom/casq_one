package constructions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.corba.se.impl.oa.poa.ActiveObjectMap.Key;

import core.Cloud;
import core.DTNHost;
import core.MessageCenter;
import core.SimClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class FilterCube {
	public Map<String,Map<Integer,Splits>> dimNum=new LinkedHashMap<String,Map<Integer,Splits>>();
	public Map<Integer,Keys> keysNum=new LinkedHashMap<Integer,Keys>();
	/*
	 * ���ȶ���filter cube�е�key�ࡢvalue���split��
	 */
	
	
	private Map<Keys,Values> fc=new LinkedHashMap<Keys,Values>();

	public FilterCube(){
		
	}
	/*
	 * ��ʼ��filtercube�е�dimNum��keyNum��
	 */
	public void initFilterCube(){
		List<String> dims=this.getDimensions();
		for(String s:dims){
			Map<Integer,Splits> is=new LinkedHashMap<Integer,Splits>();
			dimNum.put(s, is);
		}
		for(Keys k:this.fc.keySet()){
			int kPlace=1;
			for(String s:k.getDimensions()){
				int n=this.putSplits(k.getKey().get(s));
				kPlace=kPlace*n;
			}
			if(!this.keysNum.containsKey(kPlace)){
				Keys head=Keys.creatHeadKey();
				this.putKeyInKeyNum(kPlace, head);
			}
			this.putKeyInKeyNum(kPlace, k);
		}
		
	}
	/*
	 * ����洢�������
	 */
	public double fullSpace=1*1024*1024;
	private double restSpace=1*1024*1024;//��¼filter cube�е�ʣ����ô洢�ռ䣬��ʼĬ��ֵΪ1GB
	
	private double updateThreshold1=0.25;
	private double updateThreshold2=4;	
	//�жϴ洢�ռ��Ƿ���µ���ֵ
	private double spaceThreshold=0.2;
	/*
	 * ����洢������ϵ����
	 */
	private double numOfData=0;
	private double numOfRequest=0;
	private double numOfFilter=0;
	private long numOfExpandDatas=0;
	/*
     * ����dimensional split factor��balanced factor,��ʱ�ٶ�Ϊ0.5
     */
    private double balanceFactor=0.5;
	/*
	 *��һ��������ӵ�filter cube��,ͬʱ�ж��Ƿ񴫲�
	 */
    public void putData(Data d,DTNHost dtn){
  		if(this.fc.keySet().size()==0) System.out.println(dtn.getName()+"Filter cube δ��������Ҫ����������bug");
//    	System.out.println(dtn.getName()+"�е�filter cube�Ĳ㼶����"+ks.size());
//  		System.out.println(d.toString());
  		Keys k=this.getKeysByData(d);
  		if(k==null) System.err.println("filter cube�������ݵ�DTNHost����δ�ҵ���Ӧkey"+"\n"+d.toString());
    	if(k.inRange(d)){
    		if(!this.fc.containsKey(k)){
    			System.err.println("wrong in there,putData function");
    			
    		}
    		Values v=this.fc.get(k);
    		//�������ǰ����filter �ж����������Ƿ��ϴ�״̬
    		for(Filter f:v.getFilters()){
    			if(f.judgeData(d)) f.resetDataStatus(d);
    			if(f.getBasicStatus()==1){
    				if(f.basicInfo==null||f.basicInfo.size()<f.getMaxCellNum()) f.setBasicStatus(1);
    				int loc=0;
    				for(Entry<String,Splits> entry:f.getDims().entrySet()){
    					String s=entry.getKey();
    					Splits sp=(Splits) entry.getValue();
    					if(s.equals("LonX")){
    						int si=FilterCube.getMaxSplits(s);
    						loc=loc+(int)((d.getDimensions().get(s)-sp.getMinBord())/(sp.getDistance()/si));
    					}else if(s.equals("LatY")){
    						int si=FilterCube.getMaxSplits(s);
    						loc=loc+FilterCube.getMaxSplits("LonX")*(int)((d.getDimensions().get(s)-sp.getMinBord())/(sp.getDistance()/si));
    					}else if(s.equals("Size")){
    						int si=FilterCube.getMaxSplits(s);
    						loc=loc+FilterCube.getMaxSplits("LonX")*FilterCube.getMaxSplits("LatY")*(int)((d.getDimensions().get(s)-sp.getMinBord())/(sp.getDistance()/si));
    					}
    				}
    				if(loc>=f.basicInfo.size()) loc=f.basicInfo.size()-1;
    				if(f.basicInfo==null){
    					System.err.println("wrongInbasicInfo");
    					System.exit(-1);
    				}
    				if(f.basicInfo.get(loc)==null){
    					System.err.println(f.getMaxCellNum()+"��ȡbasic filter����"+loc+"��ʱ��basicInfo�Ĵ�СΪ��"+f.basicInfo.size());
    					System.err.println(d.toString());
    					System.err.println(f.toString());
    					System.err.println(k.toString());
    				}
    				f.basicInfo.get(loc).addDatas();
    			}
    		}
    		if(this.getRestSpaceRate()<this.spaceThreshold){
    			this.updateDatas();
    		}
    		if(this.getRestSpaceRate()<this.spaceThreshold){
    			this.clearDatas();
    		}
    		for(Filter f:v.getFilters()) f.filterUpdateTimer.addDatas(d);
    		v.addData(d);
    		//�������ʱ����filter cube��ʣ��ռ�
    		this.restSpace=this.restSpace-d.getSize();
    		//������ݺ��жϳ��Խ������ϴ�
    		if(dtn.getType()==0){
    			this.numOfExpandDatas++;
    			if(d.getExpandState()==0) dtn.uploadDataToEdge(d);
    			 
    		}
    		else if(dtn.getType()==1) {
    			this.numOfExpandDatas++;
    			if(d.getExpandState()==0) dtn.uploadDataToCloud(d);
    		}
    		
    		if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(1);
    		if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(1);
    		if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(1);
//    			this.updateFilter(k, this.fc.get(k));
    		v.getFilters().get(0).updateStatusByRadioCost(v.getDatas().size(), v.getRequests().size());
   			this.numOfData=this.numOfData+1;  
   		}else{
   			System.err.println("��������ʧ��");
   		}
  			
//    	System.out.println("���ݴ�СΪ��"+d.getSize()
//    		+"��"+dtn.getName()+"���ǰʣ���СΪ��"+this.getRestSpace()
//    		+"����Ӻ��ʣ���СΪ��"+this.getRestSpace());
    }
    /*
	 *��һ��������ӵ�filter cube��,���ж��Ƿ񴫲�
	 */
    public void putDataForCloud(Data d){
  		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube δ��������Ҫ����������bug");
  		Keys k=this.getKeysByData(d);
  		if(k==null) System.err.println("�������ݵ�cloud�У�filter cube�������ݳ���,δ�ҵ���Ӧkey");
    		if(k.inRange(d)){
    			//�������ǰ����filter �ж����������Ƿ��ϴ�״̬
    			Values v=this.fc.get(k);
    			for(Filter f:v.getFilters()){
    				if(f.judgeData(d)) f.resetDataStatus(d);
    				if(f.getBasicStatus()==1){
        				if(f.basicInfo==null||f.basicInfo.size()<f.getMaxCellNum()) f.setBasicStatus(1);
        				int loc=0;
        				for(Entry<String,Splits> entry:f.getDims().entrySet()){
        					String s=entry.getKey();
        					Splits sp=(Splits) entry.getValue();
        					if(s.equals("LonX")){
        						int si=FilterCube.getMaxSplits(s);
        						loc=loc+(int)((d.getDimensions().get(s)-sp.getMinBord())/(sp.getDistance()/si));
        					}else if(s.equals("LatY")){
        						int si=FilterCube.getMaxSplits(s);
        						loc=loc+FilterCube.getMaxSplits("LonX")*(int)((d.getDimensions().get(s)-sp.getMinBord())/(sp.getDistance()/si));
        					}else if(s.equals("Size")){
        						int si=FilterCube.getMaxSplits(s);
        						loc=loc+FilterCube.getMaxSplits("LonX")*FilterCube.getMaxSplits("LatY")*(int)((d.getDimensions().get(s)-sp.getMinBord())/(sp.getDistance()/si));
        					}
        				}
        				if(loc>=f.basicInfo.size()) loc=f.basicInfo.size()-1;
        				if(f.basicInfo==null){
        					System.err.println("wrongInbasicInfo");
        					System.exit(-1);
        				}
        				if(f.basicInfo.get(loc)==null){
        					System.err.println(f.getMaxCellNum()+"��ȡbasic filter����"+loc+"��ʱ��basicInfo�Ĵ�СΪ��"+f.basicInfo.size());
        					System.err.println(d.toString());
        					System.err.println(f.toString());
        					System.err.println(k.toString());
        				}
        				f.basicInfo.get(loc).addDatas();
        			}
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.updateDatasForCloud();
    			}
    			if(this.getRestSpaceRate()<this.spaceThreshold){
    				this.clearDatas();
    			}
    			for(Filter f:v.getFilters()) f.filterUpdateTimer.addDatas(d);
    			v.addData(d);
    			//�������ʱ����filter cube��ʣ��ռ�
    			this.restSpace=this.restSpace-d.getSize();
    			if(d.getExpandState()==0&&d.getUsageCount()==0) this.fc.get(k).getFilters().get(0).addUnusedPassDatas(1);
    			if(d.getExpandState()==1&&d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedBlockDatas(1);
    			if(d.getUsageCount()>0) this.fc.get(k).getFilters().get(0).addUsedDatas(1);
//    			this.updateFilter(k, this.fc.get(k));
    			this.numOfData=this.numOfData+1;    
    		}
    }
    /*
	 * ��filter cube�����request
	 */
	public void putRequest(Request r){
		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube δ��������Ҫ����������bug");
  		List<Keys> ks=this.getKeysByRequest(r);
  		if(ks.size()==0){
  			System.err.println("filter cube����request����,δ�ҵ���Ӧkey"+r.toString()+"request����"+r.getType()+"\n");
  			System.err.println(this.dimNumString());
  			for(Keys k:this.keysNum.values()){
  				while(k!=null){
  					System.err.println(k.toString());
  					k=k.next;
  				}
  			}
  			
//  			for(Keys k:this.keysNum.values()){
//  				while(k!=null){
//  					System.err.println(k.toString());
//  					k=k.next;
//  				}
//  			}
  		}
    	for(Keys k:ks)
    		if(k.inRange(r)){
    			this.fc.get(k).addRequest(r);
    			for(Filter f:this.fc.get(k).getFilters()){
    				if(f.getBasicStatus()==1){
        				if(f.basicInfo==null||f.basicInfo.size()<f.getMaxCellNum()) f.setBasicStatus(1);
        				int loc=0;
        				for(Entry<String,Splits> entry:f.getDims().entrySet()){
        					String s=entry.getKey();
        					Splits sp=(Splits) entry.getValue();
        					if(s.equals("LonX")){
        						int si=FilterCube.getMaxSplits(s);
        						loc=loc+(int)((r.getDims().get(s)-sp.getMinBord())/(sp.getDistance()/si));
        					}else if(s.equals("LatY")){
        						int si=FilterCube.getMaxSplits(s);
        						loc=loc+FilterCube.getMaxSplits("LonX")*(int)((r.getDims().get(s)-sp.getMinBord())/(sp.getDistance()/si));
        					}else if(s.equals("Size")){
        						int si=FilterCube.getMaxSplits(s);
        						loc=loc+FilterCube.getMaxSplits("LonX")*FilterCube.getMaxSplits("LatY")*(int)((r.getDims().get(s)-sp.getMinBord())/(sp.getDistance()/si));
        					}
        				}
        				if(loc>=f.basicInfo.size()) loc=f.basicInfo.size()-1;
        				if(f.basicInfo==null){
        					System.err.println("wrongInbasicInfo");
        					System.exit(-1);
        				}
        				if(f.basicInfo.get(loc)==null){
        					System.err.println(f.getMaxCellNum()+"��ȡbasic filter����"+loc+"��ʱ��basicInfo�Ĵ�СΪ��"+f.basicInfo.size());
        					System.err.println(r.toString());
        					System.err.println(f.toString());
        					System.err.println(k.toString());
        				}
        				f.basicInfo.get(loc).addRequests();
        			}
    				f.filterUpdateTimer.addRequest(r);
    			}
    		}
    	this.numOfRequest=this.numOfRequest+1;
    	
	}
    //����filter cube��ÿһ�е�filter
	public void update(){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			Filter f=v.getFilters().get(0);//��ȡ��һ��filter
			//���filter����basicFilter,����м����жϸ���״̬����Ϊbasic
			if(f.getBasicStatus()==0){
				double mf=f.getMismatchFactor(v.getDatas());
				if(mf<this.getUpdateThreshold1()){
//					double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
					f.updateStatusByRadioCost(v.getDatas().size(),v.getRequests().size());
				}else{
//					FilterCube newFilterCube=new FilterCube();
//					newFilterCube.addFilter(f);
					//����basic filter ����ʼʱ��
					f.setBasicTime(SimClock.getTime());
					//����filterΪbasic filter
					f.setBasicStatus(1);
				}
			}else{
				//��basicFilter���д���
				if(Timer.judgeFilter(f, f.getPeriodTime())){
					//���ȼ���avg�����ں������������Դ���������ݹ��
					double misDAF=0;
					double sum=0;
					for(CellStatus c:f.basicInfo.values()){
						if(f.calStatus(c.getUsedDatas(), c.getDatas(), c.getRequests())!=f.getStatus())
							misDAF=misDAF+1;
						sum=sum+1;
					}
//					for(Data d:v.getDatas()){
//						if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
//						sum=sum+1;
//					}
					double avg=misDAF/sum;
					//����mismatch factor
					double mismatchs=f.getMismatchFactor(v.getDatas());
					double er=avg/mismatchs;
					if(er>this.getUpdateThreshold2()){
						f.updateStatusByRadioCost(v.getDatas().size(),v.getRequests().size());
						//��ʱҪ��basic״̬ȡ��
						f.setBasicStatus(0);
					}else{
						//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
						//ȡ��filter��basic״̬
						f.setBasicStatus(0);
						int len=f.getDims().size();
						double[] min=new double[len];
				    	int[] split=new int[len];
				    	for(int i=0;i<len;i++){
				    		min[i]=1000000;
				    		split[i]=1;
				    	}
				    	List<String> l=this.getDimensions();
				    	Map<Keys,Values> addTemps=new LinkedHashMap<Keys,Values>();
				    	addTemps.put(nk, v);
				    	for(int i=0;i<len;i++){
				    		String dim= l.get(i);
				    		for(int j=2,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
				    			double dimSplitFac=this.getDimSplitFactor(nk,dim, j);
				    			if(dimSplitFac<min[i]){
				    				min[i]=dimSplitFac;
				    				split[i]=j;
				    			}
				    		}
				    		Map<Keys,Values> temps=new LinkedHashMap<Keys,Values>();
				    		for(Entry<Keys,Values> kv:addTemps.entrySet()){
				    			Keys tk=kv.getKey();
				    			Values tv=kv.getValue();
				    			Map<Keys,Values> newss=this.splitDimension(tk,tv, dim, split[i]);
				    			
				    			//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
				    			if(newss.size()>1){
				    				temps.putAll(newss);		    				
				    			}else{
				    				temps.put(tk, tv);
				    			}
				    		}
				    		addTemps.clear();
				    		addTemps.putAll(temps);
				    		
				    	}
				    	if(addTemps.size()>1){
				    		adds.putAll(addTemps);
				    		delKeys.add(nk);
				    		MessageCenter.splitedFilters=MessageCenter.splitedFilters+1;
				    	}
					}
				}
			}
			
		}
		//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
		if(delKeys.size()>0){
			for(Keys kk:delKeys){
				this.fc.remove(kk);
			}
		}
		if(adds.size()>0){
			this.fc.putAll(adds);
		}
	}
	
	/*
	 * ����filter cube�е�һ��filter
	 */
	public void updateFilter(Keys k,Values v){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		Filter f=v.getFilters().get(0);//��ȡ��һ��filter
		//���filter����basicFilter,����м����жϸ���״̬����Ϊbasic
		if(f.getBasicStatus()==0){
			double mf=f.getMismatchFactor(v.getDatas());
			if(mf<this.getUpdateThreshold1()){
//				double radioCost=f.calRadioCost(v.getDatas(),v.getRequests());
				f.updateStatusByRadioCost(v.getDatas().size(),v.getRequests().size());
			}else{
//				FilterCube newFilterCube=new FilterCube();
//				newFilterCube.addFilter(f);
				//����basic filter ����ʼʱ��
				f.setBasicTime(SimClock.getTime());
				//����filterΪbasic filter
				f.setBasicStatus(1);
			}
		}else{
			//��basicFilter���д���
			if(Timer.judgeFilter(f, f.getPeriodTime())){
				//���ȼ���avg�����ں������������Դ���������ݹ��
				double misDAF=0;
				double sum=0;
				for(CellStatus c:f.basicInfo.values()){
					if(f.calStatus(c.getUsedDatas(), c.getDatas(), c.getRequests())!=f.getStatus())
						misDAF=misDAF+1;
					sum=sum+1;
				}
//				for(Data d:v.getDatas()){
//					if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
//					sum=sum+1;
//				}
				double avg=misDAF/sum;
				//����mismatch factor
				double mismatchs=f.getMismatchFactor(v.getDatas());
				double er=avg/mismatchs;
				if(er>this.getUpdateThreshold2()){
					f.updateStatusByRadioCost(v.getDatas().size(),v.getRequests().size());
					//��ʱҪ��basic״̬ȡ��
					f.setBasicStatus(0);
				}else{
					//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
					//ȡ��filter��basic״̬
					f.setBasicStatus(0);
					int len=f.getDims().size();
					double[] min=new double[len];
			    	int[] split=new int[len];
			    	for(int i=0;i<len;i++){
			    		min[i]=1000000;
			    		split[i]=1;
			    	}
			    	List<String> l=this.getDimensions();
			    	Map<Keys,Values> addTemps=new LinkedHashMap<Keys,Values>();
			    	addTemps.put(k, v);
			    	for(int i=0;i<len;i++){
			    		String dim= l.get(i);
			    		for(int j=2,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
			    			double dimSplitFac=this.getDimSplitFactor(k, dim, j);
			    			if(dimSplitFac<min[i]){
			    				min[i]=dimSplitFac;
			    				split[i]=j;
			    			}
			    		}
			    		Map<Keys,Values> temps=new LinkedHashMap<Keys,Values>();
			    		for(Entry<Keys,Values> kv:addTemps.entrySet()){
			    			Keys tk=kv.getKey();
			    			Values tv=kv.getValue();
			    			Map<Keys,Values> newss=this.splitDimension(tk,tv, dim, split[i]);
			    			
			    			//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
			    			if(newss.size()>1){
			    				temps.putAll(newss);		    				
			    			}else{
			    				temps.put(tk, tv);
			    			}
			    		}
			    		addTemps.clear();
			    		addTemps.putAll(temps);
			    		
			    	}
			    	if(addTemps.size()>1){
			    		adds.putAll(addTemps);
			    		delKeys.add(k);
			    		MessageCenter.splitedFilters=MessageCenter.splitedFilters+1;
			    	}
				}
			}
		}
			
		//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
		if(delKeys.size()>0){
			for(Keys kk:delKeys){
				this.fc.remove(kk);
			}
		}
		if(adds.size()>0){
			this.fc.putAll(adds);
		}
	}
	/*
	 * ����Cloud��filter cube�е�ÿһ�����ݣ��ж������ɾ��
	 */
	public void updateDatasForCloud(){
		MessageCenter.clearDatas=MessageCenter.clearDatas+1;
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			v.getDatas().removeIf(d->{
				if(SimClock.getTime()-d.getTime()>1200){
					this.restSpace=this.restSpace+d.getSize();
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
					return true;
				}else {
					return false;
				}
					
			});
			
		}
//		System.out.println("������������.....");
	}
	/*
	 * ����filter cube�е�ÿһ�����ݣ��ж������ɾ��
	 */
	public void updateDatas(){
		MessageCenter.clearDatas=MessageCenter.clearDatas+1;
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			v.getDatas().removeIf(d->{
				if(SimClock.getTime()-d.getTime()>3600||(SimClock.getTime()-d.getTime()>1800&&d.getUsageCount()<5)){
					this.restSpace=this.restSpace+d.getSize();
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
					return true;
				}else {
					return false;
				}
					
			});
//			List<Data> datas=v.getDatas();
//			List<Data> dels=new ArrayList<Data>();
//			double releaseSpace=0;
//			double delNum=0;
//			int sum=datas.size();
//			for(Data d:datas){
//				/*
//				 * ������ʱʹ������ʱ�������ʹ�ô�����Ϊɾ�����ݵ�����
//				 */
//				
//				if((SimClock.getTime()-d.getTime()>1800&&d.getUsageCount()==0)||(SimClock.getTime()-d.getTime()>3600&&d.getUsageCount()<5)){
//					dels.add(d);
//					releaseSpace=releaseSpace+d.getSize();
//					delNum=delNum+1;
//					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
//					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
//					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
//				}
//				if(delNum/sum>0.4) break;
//			}
//			if(dels.size()>0){
//				datas.removeAll(dels);
//				
//			}
//			this.numOfData=this.numOfData-delNum;
//			this.restSpace=this.restSpace+releaseSpace;
			
		}
//		System.out.println("������������.....");
	}
	/*
	 * ɾ���������ݣ����޷�ɾ������ʱ������ɾ���߶�ɾ����������
	 */
	public void clearDatas(){
		MessageCenter.clearDatas=MessageCenter.clearDatas+1;
		for(Entry<Keys, Values> entry:this.fc.entrySet()){
			Keys nk=entry.getKey();
			Values v=entry.getValue();
			List<Data> datas=v.getDatas();
			List<Data> dels=new ArrayList<Data>();
			double releaseSpace=0;
			double delNum=0;
			int sum=datas.size();
			for(Data d:datas){
				/*
				 * ������ʱʹ������ʱ����Ϊɾ�����ݵ�����
				 */
				
				if(SimClock.getTime()-d.getTime()>1200){
					dels.add(d);
					releaseSpace=releaseSpace+d.getSize();
					delNum=delNum+1;
					if(d.getUsageCount()>0) v.getFilters().get(0).addUsedDatas(-1);
					if(d.getExpandState()==0&&d.getUsageCount()==0) v.getFilters().get(0).addUnusedPassDatas(-1);
					if(d.getExpandState()==1&&d.getUsageCount()>0) v.getFilters().get(0).addUsedBlockDatas(-1);
				}
				if(delNum/sum>0.4) break;
				/*
				 * �����������ϵĴ���
				 */
				
			}
			if(dels.size()>0){
				datas.removeAll(dels);
			}
			this.numOfData=this.numOfData-delNum;
			this.restSpace=this.restSpace+releaseSpace;
		}
	}
//	/*
//	 * �����ж�data��״̬�������ݵ�ǰDTNHost�����rsu�������ȣ�
//	 * ������
//	 * ���д���
//	 */
//	public void updateDataStatusByEdgeOCar(DTNHost dtn,Data d){
//		Set<Keys> k=this.fc.keySet();
//		Iterator it=k.iterator();
//		while(it.hasNext()){
//			Keys key=(Keys) it.next();
//			Values v=this.fc.get(key);
//			List<Filter> fs=v.getFilters();
//			for(Filter f:fs){
//				for(Data d:v.getDatas()){
//					f.resetDataStatus(d);
//					//����ýڵ���RSU�ڵ�
//					if(dtn.getType()==1){
//						dtn.uploadDataToCloud(d);
//					}else if(dtn.getType()==0){
//						//����ڵ��ǳ����ڵ�
//						dtn.uploadDataToEdge(d);
//					}
//				}
//			}
//			
//		}
//	}
//	/*
//	 * �����ж�data��״̬����cloud�˵ľ��Ĵ���
//	 * ������
//	 * ���д���
//	 */
//	public void updateDataStatusByCloud(){
//		Set<Keys> k=this.fc.keySet();
//		Iterator it=k.iterator();
//		while(it.hasNext()){
//			Keys key=(Keys) it.next();
//			Values v=this.fc.get(key);
//			List<Filter> fs=v.getFilters();
//			for(Filter f:fs){
//				for(Data d:v.getDatas()){
//					f.resetDataStatus(d);
//					Cloud.getInstance().downloadDataToEdge(d);
//				}
//			}
//			
//		}
//	}
	
	/*
	 * ��DTNHost(Edge��Car)
	 * ��request���д����ҳ�filtercube���ܹ������request������
	 * ͬʱ��request�������ݵ�ʹ��������������ǣ������������ݴ���״̬
	 * ��������Ҫ���д���
	 */
	public List<Data> answerRequest(Request r,DTNHost dtn){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		List<Data> res=new ArrayList<Data>();
		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube δ��������Ҫ����������bug");
  		List<Keys> keyss=this.getKeysByRequest(r);
  		if(keyss.size()==0) System.err.println("δ�ҵ���Ӧrequest��key"+r.toString());
  		Map<Keys,Values> updateds=new LinkedHashMap<Keys,Values>(10);
  		for(Keys k:keyss){
//  			Set<Keys> kt=this.fc.keySet();
    		Values v=this.fc.get(k);
    		Filter f=v.getFilters().get(0);
    		if(f.judgeRequest(r)){
    			List<Data> s=v.getDatas();
    			for(Data t:s){
    				if(r.judgeData(t)) {
    					t.addUsageCount();//�������ʹ�ô���
    					if(f.getBasicStatus()==1){
    	    				if(f.basicInfo==null||f.basicInfo.size()<f.getMaxCellNum()) f.setBasicStatus(1);
    	    				int loc=0;
    	    				for(Entry<String,Splits> entry:f.getDims().entrySet()){
    	    					String st=entry.getKey();
    	    					Splits sp=(Splits) entry.getValue();
    	    					if(s.equals("LonX")){
    	    						int si=FilterCube.getMaxSplits(st);
    	    						loc=loc+(int)((t.getDimensions().get(st)-sp.getMinBord())/(sp.getDistance()/si));
    	    					}else if(s.equals("LatY")){
    	    						int si=FilterCube.getMaxSplits(st);
    	    						loc=loc+FilterCube.getMaxSplits("LonX")*(int)((t.getDimensions().get(st)-sp.getMinBord())/(sp.getDistance()/si));
    	    					}else if(s.equals("Size")){
    	    						int si=FilterCube.getMaxSplits(st);
    	    						loc=loc+FilterCube.getMaxSplits("LonX")*FilterCube.getMaxSplits("LatY")*(int)((t.getDimensions().get(st)-sp.getMinBord())/(sp.getDistance()/si));
    	    					}
    	    				}
    	    				if(loc>=f.basicInfo.size()) loc=f.basicInfo.size()-1;
    	    				if(f.basicInfo==null){
    	    					System.err.println("wrongInbasicInfo");
    	    					System.exit(-1);
    	    				}
    	    				if(f.basicInfo.get(loc)==null){
    	    					System.err.println(f.getMaxCellNum()+"��ȡbasic filter����"+loc+"��ʱ��basicInfo�Ĵ�СΪ��"+f.basicInfo.size());
    	    					System.err.println(t.toString());
    	    					System.err.println(f.toString());
    	    					System.err.println(k.toString());
    	    				}
    	    				f.basicInfo.get(loc).addUsedDatas();;
    	    			}
    					int sign=0;//�ڴ˴����ڱ���Ƿ�������Ƿ��Ǵ���״̬
    					if(t.getExpandState()==0) sign=1;
    					f.resetDataStatus(t);
    					f.filterUpdateTimer.addUsedDatas(t);
    					if(t.getUsageCount()==1){
    						
    						if(sign==0){
    							if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						else{
    							if(t.getExpandState()==0) f.addUnusedPassDatas(-1);
    							else if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						f.addUsedDatas(1);
    						updateds.put(k, v);
    					}
//    					//�ж��Ƿ��ϴ�
//    					if(t.getExpandState()==0&&sign==0){
//    					
//    						if(dtn.getType()==0) dtn.uploadDataToEdge(t);
//    						else if(dtn.getType()==1) dtn.uploadDataToCloud(t);
//    					}
    					res.add(t);
    				}
    			}
    			//�����filter��һ��basic filter����Ҫ�ж��Ƿ��ڿ���ʱ����
    			if(f.getBasicStatus()==1){
    				//��basicFilter���д���
    				if(Timer.judgeFilter(f, f.getPeriodTime())){
    					updateds.put(k, v);
    				}
    			}
    		}
  		}
  		for(Entry<Keys,Values> en:updateds.entrySet()){
  			Keys k=en.getKey();
  			Values v=en.getValue();
  			Filter f=v.getFilters().get(0);
  			if(f.getBasicStatus()==1){
  			//���ȼ���avg�����ں������������Դ���������ݹ��
				double misDAF=0;
				
				double sum=0;
				for(CellStatus c:f.basicInfo.values()){
					if(f.calStatus(c.getUsedDatas(), c.getDatas(), c.getRequests())!=f.getStatus())
						misDAF=misDAF+1;
					sum=sum+1;
				}
//				for(Data d:v.getDatas()){
//					if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
//					sum=sum+1;
//				}
				double avg=misDAF/sum;
				//����mismatch factor
				double mismatchs=this.calMismatchFactor(v.getDatas());
				double er=avg/mismatchs;
				if(er>this.getUpdateThreshold2()){
					f.updateStatusByRadioCost(v.getDatas().size(),v.getRequests().size());
					//��ʱҪ��basic״̬ȡ��
					f.setBasicStatus(0);
				}else{
					//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
					//ȡ��filter��basic״̬
					f.setBasicStatus(0);
					int len=f.getDims().size();
					double[] min=new double[len];
			    	int[] split=new int[len];
			    	for(int i=0;i<len;i++){
			    		min[i]=1000000;
			    		split[i]=1;
			    	}
			    	List<String> l=this.getDimensions();
			    	Map<Keys,Values> addTemps=new LinkedHashMap<Keys,Values>();
			    	addTemps.put(k, v);
			    	for(int i=0;i<len;i++){
			    		String dim= l.get(i);
			    		for(int j=2,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
			    			double dimSplitFac=this.getDimSplitFactor(k,dim, j);
			    			if(dimSplitFac<min[i]){
			    				min[i]=dimSplitFac;
			    				split[i]=j;
			    			}
			    		}
			    		Map<Keys,Values> temps=new LinkedHashMap<Keys,Values>();
			    		for(Entry<Keys,Values> kv:addTemps.entrySet()){
			    			Keys tk=kv.getKey();
			    			Values tv=kv.getValue();
			    			Map<Keys,Values> newss=this.splitDimension(tk,tv, dim, split[i]);
			    			
			    			//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
			    			if(newss.size()>1){
			    				temps.putAll(newss);		    				
			    			}else{
			    				temps.put(tk, tv);
			    			}
			    		}
			    		addTemps.clear();
			    		addTemps.putAll(temps);
			    		
			    	}
			    	if(addTemps.size()>1){
			    		adds.putAll(addTemps);
			    		delKeys.add(k);
			    		MessageCenter.splitedFilters=MessageCenter.splitedFilters+1;
			    	}
					
				}
  			}else{
  				this.updateFilter(k, v);
  			}
  		}
    	//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
    	if(delKeys.size()>0){
    		for(Keys kk:delKeys){
    			this.fc.remove(kk);
    		}
    	}
    	if(adds.size()>0){
    		this.fc.putAll(adds);
    	}
		return res;
	}
	/*
	 * Cloud
	 * ��request���д����ҳ�filtercube���ܹ������request������
	 * ͬʱ��request�������ݵ�ʹ��������������ǣ������������ݴ���״̬
	 * ��������Ҫ���д���
	 */
	public List<Data> answerRequest(Request r,Cloud dtn){
		Map<Keys,Values> adds=new LinkedHashMap<Keys,Values>();
		List<Keys> delKeys =new ArrayList<Keys>();
		List<Data> res=new ArrayList<Data>();
		if(this.fc.keySet().size()==0) System.err.println("Cloud Filter cube δ��������Ҫ����������bug");
		List<Keys> keyss=this.getKeysByRequest(r);
  		if(keyss.size()==0) System.err.println("δ�ҵ���Ӧrequest��key"+r.toString());
  		Map<Keys,Values> updateds=new LinkedHashMap<Keys,Values>(10);
  		for(Keys k:keyss){
    		Values v=this.fc.get(k);
    		Filter f=v.getFilters().get(0);
    		if(f.judgeRequest(r)){
    			List<Data> s=v.getDatas();
    			int sign=0;
    			for(Data t:s){
    				if(r.judgeData(t)) {
    					t.addUsageCount();//�������ʹ�ô���
    					if(f.getBasicStatus()==1){
    	    				if(f.basicInfo==null||f.basicInfo.size()<f.getMaxCellNum()) f.setBasicStatus(1);
    	    				int loc=0;
    	    				for(Entry<String,Splits> entry:f.getDims().entrySet()){
    	    					String st=entry.getKey();
    	    					Splits sp=(Splits) entry.getValue();
    	    					if(s.equals("LonX")){
    	    						int si=FilterCube.getMaxSplits(st);
    	    						loc=loc+(int)((t.getDimensions().get(st)-sp.getMinBord())/(sp.getDistance()/si));
    	    					}else if(s.equals("LatY")){
    	    						int si=FilterCube.getMaxSplits(st);
    	    						loc=loc+FilterCube.getMaxSplits("LonX")*(int)((t.getDimensions().get(st)-sp.getMinBord())/(sp.getDistance()/si));
    	    					}else if(s.equals("Size")){
    	    						int si=FilterCube.getMaxSplits(st);
    	    						loc=loc+FilterCube.getMaxSplits("LonX")*FilterCube.getMaxSplits("LatY")*(int)((t.getDimensions().get(st)-sp.getMinBord())/(sp.getDistance()/si));
    	    					}
    	    				}
    	    				if(loc>=f.basicInfo.size()) loc=f.basicInfo.size()-1;
    	    				if(f.basicInfo==null){
    	    					System.err.println("wrongInbasicInfo");
    	    					System.exit(-1);
    	    				}
    	    				if(f.basicInfo.get(loc)==null){
    	    					System.err.println(f.getMaxCellNum()+"��ȡbasic filter����"+loc+"��ʱ��basicInfo�Ĵ�СΪ��"+f.basicInfo.size());
    	    					System.err.println(t.toString());
    	    					System.err.println(f.toString());
    	    					System.err.println(k.toString());
    	    				}
    	    				f.basicInfo.get(loc).addUsedDatas();;
    	    			}
    					//�ڴ˴����ڱ���Ƿ�������Ƿ��Ǵ���״̬
    					if(t.getExpandState()==0) sign=1;
    					f.resetDataStatus(t);
    					f.filterUpdateTimer.addUsedDatas(t);
    					if(t.getUsageCount()==1){
    						
    						if(sign==0){
    							if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						else{
    							if(t.getExpandState()==0) f.addUnusedPassDatas(-1);
    							else if(t.getExpandState()==1) f.addUsedBlockDatas(1);
    						}
    						f.addUsedDatas(1);
    						updateds.put(k, v);
    						
    					}
    					res.add(t);
    				}
    			}
    			
    			//�����filter��һ��basic filter����Ҫ�ж��Ƿ��ڿ���ʱ����
    			if(f.getBasicStatus()==1){
    				//��basicFilter���д���
    				if(Timer.judgeFilter(f, f.getPeriodTime())){
    					updateds.put(k, v);
    					
    				}
    			}
    		
    		}
  		}
  		for(Entry<Keys,Values> en:updateds.entrySet()){
  			Keys k=en.getKey();
  			Values v=en.getValue();
  			Filter f=v.getFilters().get(0);
  			if(f.getBasicStatus()==1){
  			//���ȼ���avg�����ں������������Դ���������ݹ��
				double misDAF=0;
				double sum=0;
				for(CellStatus c:f.basicInfo.values()){
					if(f.calStatus(c.getUsedDatas(), c.getDatas(), c.getRequests())!=f.getStatus())
						misDAF=misDAF+1;
					sum=sum+1;
				}
//				for(Data d:v.getDatas()){
//					if(d.getExpandState()!=f.getStatus()) misDAF=misDAF+1;
//					sum=sum+1;
//				}
				double avg=misDAF/sum;
				//����mismatch factor
				double mismatchs=this.calMismatchFactor(v.getDatas());
				double er=avg/mismatchs;
				if(er>this.getUpdateThreshold2()){
					f.updateStatusByRadioCost(v.getDatas().size(),v.getRequests().size());
					//��ʱҪ��basic״̬ȡ��
					f.setBasicStatus(0);
				}else{
					//��filter��Ϊmore״̬����ʱû��Ҫ���ݲ����в���
					//ȡ��filter��basic״̬
					f.setBasicStatus(0);
					int len=f.getDims().size();
					double[] min=new double[len];
			    	int[] split=new int[len];
			    	for(int i=0;i<len;i++){
			    		min[i]=1000000;
			    		split[i]=1;
			    	}
			    	List<String> l=this.getDimensions();
			    	Map<Keys,Values> addTemps=new LinkedHashMap<Keys,Values>();
			    	addTemps.put(k, v);
			    	for(int i=0;i<len;i++){
			    		String dim= l.get(i);
			    		for(int j=2,maxSplits=FilterCube.getMaxSplits(dim);j<=maxSplits;j++){
			    			double dimSplitFac=this.getDimSplitFactor(k, dim, j);
			    			if(dimSplitFac<min[i]){
			    				min[i]=dimSplitFac;
			    				split[i]=j;
			    			}
			    		}
			    		Map<Keys,Values> temps=new LinkedHashMap<Keys,Values>();
			    		for(Entry<Keys,Values> kv:addTemps.entrySet()){
			    			Keys tk=kv.getKey();
			    			Values tv=kv.getValue();
			    			Map<Keys,Values> newss=this.splitDimension(tk,tv, dim, split[i]);
			    			
			    			//����зֺ��filter��������һ�����������з֣�������µķ�Ƭ��ɾ���ɵķ�Ƭ
			    			if(newss.size()>1){
			    				temps.putAll(newss);		    				
			    			}else{
			    				temps.put(tk, tv);
			    			}
			    		}
			    		addTemps.clear();
			    		addTemps.putAll(temps);
			    		
			    	}
			    	if(addTemps.size()>1){
			    		adds.putAll(addTemps);
			    		delKeys.add(k);
			    		MessageCenter.splitedFilters=MessageCenter.splitedFilters+1;
			    	}
				}
  			}else{
  				this.updateFilter(k, v);
  			}
  		}
    	//ɾ�����зֵķ�Ƭ����ӷֳ��ķ�Ƭ
    	if(delKeys.size()>0){
    		for(Keys kk:delKeys){
    			this.fc.remove(kk);
    		}
    	}
    	if(adds.size()>0){
    		this.fc.putAll(adds);
    	}
		return res;
	}
	
	//dimension split
	public Map<Keys, Values> splitDimension(Keys k,Values v,String dimension,int num){
		/*
		 * ά���з֣�����Ҫ���ɵ�ά�ȶ�Ӧɾ�����ٽ��µ�ά�ȷ�Ƭ��ӽ�ȥ
		 */
		
		Map<Keys,Values> addKV=new LinkedHashMap<>();
		
		if(num>1){
			List<Splits> addSp=new ArrayList<Splits>();
//			List<Splits> delSp=new ArrayList<Splits>();
//			delSp.add(k.getKey().get(dimension));
			int beNumKey=1;
			for(String di:k.getKey().keySet()){
				if(!di.equals(dimension)){
					int temp=this.getDimNum(di,k.getKey().get(di));
					beNumKey=beNumKey*temp;
				}
			}
			
//			List<Keys> del=new ArrayList<>();
			if(k.getKey().containsKey(dimension)){
				
				//��dimensionά�Ƚ����з�,ƽ���зֳ�num��filter
				//���Ȼ�õ�ǰfilter�ڸ�ά�ȵĿ��ֵ��Ȼ������з�
				double max=k.getKey().get(dimension).getMaxBord()-k.getKey().get(dimension).getMinBord();
				double aver=max/num;
				for(int i=0;i<num;i++){
					Keys nKey=new Keys(k);
					nKey.changeDimensionValue(dimension, k.getKey().get(dimension).getMinBord()+i*aver,k.getKey().get(dimension).getMinBord()+(i+1)*aver);
					addSp.add(nKey.getKey().get(dimension));
					Values newValues=new Values(v);
					//�����newValues�е����ݣ�Ȼ���ԭ����������ѡ���ϵķ���newValues��
					newValues.clearAllDatas();
					newValues.clearAllRequests();
					newValues.changeDimensionValue(dimension, k.getKey().get(dimension).getMinBord()+i*aver,k.getKey().get(dimension).getMinBord()+(i+1)*aver);
					int usedDatas=0;
					int unusedPassDatas=0;
					int usedBlockDatas=0;
					for(Data d:v.getDatas()){
						if(nKey.inRange(d)){
							newValues.getDatas().add(d);
							newValues.getFilters().get(0).filterUpdateTimer.addDatas(d);
						}
						
						if(d.getUsageCount()>0){
							usedDatas++;
							newValues.getFilters().get(0).filterUpdateTimer.addUsedDatas(d);
						}
						if(d.getExpandState()==0&&d.getUsageCount()==0) unusedPassDatas++;
						if(d.getExpandState()==1&&d.getUsageCount()>0) usedBlockDatas++;
					}
					newValues.getFilters().get(0).setUsedDatas(usedDatas);
					newValues.getFilters().get(0).setUnusedPassDatas(unusedPassDatas);
					newValues.getFilters().get(0).setUsedBlockDatas(usedBlockDatas);
					for(Request r:v.getRequests()){
						if(nKey.inRange(r)){
							newValues.getRequests().add(r);
							newValues.getFilters().get(0).filterUpdateTimer.addRequest(r);
						}
					}
					newValues.getFilters().get(0).updateStatusByRadioCost(newValues.getDatas().size(), newValues.getRequests().size());
					//���зֺ��filter���뵽filtercube��
					addKV.put(nKey, newValues);
				}
			}
//			for(Splits s:delSp){
//				this.delSplits(s);
//			}
			for(Splits s:addSp){
				int pl=this.putSplits(s);
				if(pl<=0) System.err.println("��ӳ���");
			}
			for(Keys nk:addKV.keySet()){
				int nums=this.getPosByKey(nk);
				this.putKeyInKeyNum(nums, nk);
				
			}
			this.delKeyInKeyNum(this.getPosByKey(k), k);
			return addKV;
		}else{
			addKV.put(k, v);
			return addKV;
		}
	}
	/*
	 * ��filter cube�����һ��filter
	 */
	public void addFilter(Filter f){
		Keys k=this.getKeysByFIlter(f);
  		if(k==null) System.err.println("filter cube��ӳ���δ�ҵ���Ӧkey");
		if(k.inRange(f)){
			this.fc.get(k).addFilter(f);
			this.numOfFilter=this.numOfFilter+1;
		}
	}
	/*
	 * �����ȡһ��ά�ȵ�����Ƭ����
	 * ������ʱʹ�õ�·����ͼ������type==1�Ƚ��й���
	 */
	public static int getMaxSplits(String dimension){
		if(dimension.equals("Weather")) return 3;
		else if(dimension.equals("Time")) return 5;
		else if(dimension.equals("TrafficCondition")) return 3;
		else if(dimension.equals("Size")) return 3;
		else return 3;
	}
	/*
	 * ��������ȡfilter cubeĳ��ά�ȵ�mis(f,s)ֵ
	 */
	public int getSumOfMis(Keys sk,String dimension,double num){
		Values sv=this.fc.get(sk);
		double max=this.getMaxNumInDim(sk,dimension);
		double lowest=sk.getKey().get(dimension).getMinBord();
		int sumSplits=FilterCube.getMaxSplits(dimension);
		double sumAver=max/sumSplits;
		List<Integer> basicStatuss=new ArrayList<Integer>();
		for(int i=0;i<sumSplits;i++){
			int sumData=0;
			int sumUsedData=0;
			int sumRequest=0;
			for(Data d:sv.getDatas()){
				if(d.getDimensions().containsKey(dimension)&&(d.getDimensions().get(dimension)>=i*sumAver+lowest)
						&&(d.getDimensions().get(dimension)<=(i+1)*sumAver+lowest)){
					sumData++;
					if(d.getUsageCount()>0) sumUsedData++;
				}
			}
			for(Request r:sv.getRequests()){
				if(r.getDims().containsKey(dimension)&&(r.getDims().get(dimension)>=i*sumAver+lowest)
						&&(r.getDims().get(dimension)<=(i+1)*sumAver+lowest)){
					sumRequest++;
				}
			}
			double dataMatchRatios=0;
			double requestToDataRatios=0;
			if(sumData>0){
				dataMatchRatios=(double)sumUsedData/sumData;
				requestToDataRatios=(double)sumRequest/sumData;
			}
			double cost_ratio=dataMatchRatios+(sv.getFilters().get(0).getReqTransmFactor()/sv.getFilters().get(0).getDataTransmFactor())*requestToDataRatios;
			if(cost_ratio>1) basicStatuss.add(0);
			else basicStatuss.add(1);
		}
		
		
		double aver=max/num;
		int sum=0;
		for(int i=0;i<num;i++){
			int status=0;
			int sumData=0;
			int sumUsedData=0;
			int sumRequest=0;
			for(Data d:sv.getDatas()){
				if(d.getDimensions().containsKey(dimension)&&(d.getDimensions().get(dimension)>=i*aver+lowest)
						&&(d.getDimensions().get(dimension)<=(i+1)*aver+lowest)){
					sumData++;
					if(d.getUsageCount()>0) sumUsedData++;
				}
			}
			for(Request r:sv.getRequests()){
				if(r.getDims().containsKey(dimension)&&(r.getDims().get(dimension)>=i*aver+lowest)
						&&(r.getDims().get(dimension)<=(i+1)*aver+lowest)){
					sumRequest++;
				}
			}
			double dataMatchRatios=0;
			double requestToDataRatios=0;
			if(sumData>0){
				dataMatchRatios=(double)sumUsedData/sumData;
				requestToDataRatios=(double)sumRequest/sumData;
			}
			double cost_ratio=dataMatchRatios+(sv.getFilters().get(0).getReqTransmFactor()/sv.getFilters().get(0).getDataTransmFactor())*requestToDataRatios;
			
    		if(cost_ratio>1) status=0;
    		else status=1;
    		for(int j=0;j<sumSplits;j++){
    			if	(((j+1)*sumAver+lowest)>((i+1)*aver+lowest)) break;
    			if(((j*sumAver+lowest)>=(i*aver+lowest)&&((j+1)*sumAver+lowest)<=((i+1)*aver+lowest)))
    					if(status!=basicStatuss.get(j)) sum++;
    					
    		}
    	}
		return sum;
	}
	/*
	 * ����һ��ά�ȵ����ֵ�������ڼ�����η�Ƭ
	 * ������ʱʹ�õ�·����ͼ������type==1�Ƚ��й���
	 */
	public double getMaxNumInDim(Keys k,String dimension){
		return k.getKey().get(dimension).getMaxBord()-k.getKey().get(dimension).getMinBord();
	}
	/*
	 * ���filter cube �е�dimension �б�
	 */
	public List<String> getDimensions(){
		List<String> l=new ArrayList<String>();
		for(Keys k:this.fc.keySet()){
			l=k.getDimensions();
			break;
		}
		if(l.size()==0) System.err.println("filter cube��ȡά�ȴ���");
		return l;
	}
//	/*
//	 * �����filter cube��ĳһ�еĸ���
//	 * 
//	 */
//	public void copyFromKeyValue(Keys k,Values v){
//		
//	}
	/*
	 * ��dimNum��ɾ��ԭ��splits,
	 *  public int delSplits(Splits olds){
		int delNum=-1;
		for(Integer i:this.dimNum.get(olds.getDimension()).keySet()){
			if(this.dimNum.get(olds.getDimension()).get(i).isEqual(olds)){
				delNum=i;
				break;
			}
		}
		if(delNum==-1){
			System.err.println("wrong in deleting splits");
			return -1;
		}
		else {
			this.dimNum.get(olds.getDimension()).remove(delNum);
			return delNum;
		}
			
	}

	 */
	
	/*
	 * ��dimNum������µ�splits
	 */
	public int  putSplits(Splits news){
		for(Splits s:this.dimNum.get(news.getDimension()).values()){
			if(s.equals(news)) return -1;
		}
		int len=this.dimNum.get(news.getDimension()).size()+1;
		while(this.dimNum.get(news.getDimension()).containsKey(len)) len++;
		this.dimNum.get(news.getDimension()).put(len, news);		
		return len;
	}

	/*
	 * ��keyNum������µ�key
	 */
	public void putKeyInKeyNum(int pos,Keys k){
		if(this.keysNum.containsKey(pos)){
			Keys tk=this.keysNum.get(pos);
			while(tk.next!=null) tk=tk.next;
			tk.next=k;
		}else{
			Keys heads=Keys.creatHeadKey();
			this.keysNum.put(pos, heads);
			this.keysNum.get(pos).next=k;
		}
	}
	
	/*
	 * ��keyNum��ɾ��ԭ�е�key
	 */
	public void delKeyInKeyNum(int pos, Keys k){
		if(this.keysNum.containsKey(pos)){
			Keys hkey=this.keysNum.get(pos);
			while(hkey.next!=null){
				if(hkey.next.isEqual(k)){
					hkey.next=hkey.next.next;
				}else{
					hkey=hkey.next;
				}
			}
		}
	}
	
	/*
	 * ����dimension��ֵ��ȡ��Ӧ��dimNum
	 */
	public List<Integer> getDimNum(String dimension,double value){
		List<Integer> reslist=new ArrayList<Integer>();
		if(this.dimNum.keySet().contains(dimension)){
			for(Integer i:this.dimNum.get(dimension).keySet()){
				if(this.dimNum.get(dimension).get(i).inRange(value)){
					reslist.add(i);
					
				}
			}
		}
		return reslist;
	}
	/*
	 * ����dimension�ͷ�Ƭ��ȡ��Ӧ��dimNum
	 */
	public int getDimNum(String dimension,Splits s){
		int res=-1;
		if(this.dimNum.keySet().contains(dimension)){
			for(Integer i:this.dimNum.get(dimension).keySet()){
				if(this.dimNum.get(dimension).get(i).isEqual(s)){
					res=i;
					break;
				}
					
			}
		}
		return res;
	}
	/*
	 * ����request��ȡkey
	 */
	public List<Keys> getKeysByRequest(Request r){
		Keys k=null;
		List<Integer> keyNumList = new ArrayList<Integer>();
		for(String s:r.getDims().keySet()){
			List<Integer> num=this.getDimNum(s, r.getDims().get(s));
			if(keyNumList.size()==0){
				keyNumList=num;
			}else{
				List<Integer> temp=new ArrayList<Integer>();
				for(int i:keyNumList){
					for(int j:num){
						int t=i*j;
						if(!temp.contains(t)){
							temp.add(t);
						}
					}
				}
				keyNumList=temp;
			}
		}
//		String anoDim="Size";
//		if(this.dimNum.containsKey(anoDim)){
//			List<Integer> num=new ArrayList<Integer>();
//			for(int i:this.dimNum.get(anoDim).keySet()) num.add(i);
//			if(keyNumList.size()==0){
//				keyNumList=num;
//			}else{
//				List<Integer> temp=new ArrayList<Integer>();
//				for(int i:keyNumList){
//					for(int j:num){
//						int t=i*j;
//						if(!temp.contains(t)){
//							temp.add(t);
//						}
//					}
//				}
//				keyNumList=temp;
//			}
//		}
		List<Keys> resK=new ArrayList<Keys>();
		for(int keynum:keyNumList){
			k=this.keysNum.get(keynum);
			if(!this.keysNum.containsKey(keynum)){
				continue;
			}
			while(!k.inRange(r)){
				k=k.next;
				if(k==null) break;
			}
			if(k==null) continue;
			else{
				resK.add(k);
				continue;
			}
		}
		return resK;
	}
	
	/*
	 * ����data��ȡkey
	 */
	public Keys getKeysByData(Data d){
		Keys k=null;
		List<Integer> keyNumList = new ArrayList<Integer>();
		List<Integer> num=new ArrayList<Integer>();
		List<Integer> temp=new ArrayList<Integer>();
		for(String s:this.getDimensions()){
			num=this.getDimNum(s, d.getDimensions().get(s));
			if(keyNumList.size()==0){
				keyNumList=num;
			}else{
				temp.clear();
				for(int i:keyNumList){
					for(int j:num){
						int t=i*j;
						if(!temp.contains(t)){
							temp.add(t);
						}
					}
				}
				keyNumList.clear();
				keyNumList.addAll(temp);
			}
		}
		Keys resK=null;
		for(int keynum:keyNumList){
			k=this.keysNum.get(keynum);
			resK=k;
			while(resK!=null&&!resK.inRange(d)){
				resK=resK.next;
				if(resK==null) break;
			}
			if(resK==null) continue;
			else{
				break;
			}
		}
		if(resK==null){
			System.err.println(d.toString());
		}
		return resK;
	}
	/*
	 * ����filter��ȡkey
	 */
	public Keys getKeysByFIlter(Filter f){
		Keys k=null;
		int keynum=1;
		for(String s:this.getDimensions()){
			int num=this.getDimNum(s,f.getDims().get(s));
			if(num==-1) System.err.println("����δ�ܻ�ȡdimension��λ��ֵ");
			else keynum=keynum*num;
		}
		k=this.keysNum.get(keynum);
		Keys resK=k;
		while(!this.fc.containsKey(resK)||!resK.inRange(f)) resK=resK.next;
		return resK;
	}
	/*
	 * ��ȡkeysӦ�����ڵ�λ��
	 */
	public int getPosByKey(Keys k){
		int res=1;
		for(String s:this.getDimensions()){
			int ns=this.getDimNum(s,k.getKey().get(s));
			if(ns<0){
				System.err.println("��ȡkey���ڵ�λ�ó���");
				System.exit(-1);
			}
			res=res*ns;
		}
		
		return res;
	}
	public String dimNumString(){
		StringBuilder res=new StringBuilder("");
		for(String s:this.dimNum.keySet()){
			res.append(s).append("\n");
			for(int i:this.dimNum.get(s).keySet()){
				res.append(i).append(":").append(this.dimNum.get(s).get(i).toString()).append(",");
			}
			res.append("\n");
		}
		return res.toString();
	}
	public Map<Keys,Values> getFc() {
		return fc;
	}
	public void setFc(Map<Keys,Values> fc) {
		this.fc = fc;
	}
	public double getUpdateThreshold1() {
		return updateThreshold1;
	}
	public void setUpdateThreshold1(double updateThreshold1) {
		this.updateThreshold1 = updateThreshold1;
	}
	public double getUpdateThreshold2() {
		return updateThreshold2;
	}
	public void setUpdateThreshold2(double updateThreshold2) {
		this.updateThreshold2 = updateThreshold2;
	}
	public double getDimSplitFactor(Keys k,String di,int x) {
		double res=(this.getBalanceFactor()*(double)x)/FilterCube.getMaxSplits(di);
    	res=res+(1-this.getBalanceFactor())*this.getSumOfMis(k,di, x);
    	return res;
	}

	public double getBalanceFactor() {
		return balanceFactor;
	}
	public void setBalanceFactor(double balanceFactor) {
		this.balanceFactor = balanceFactor;
	}
	public double getNumOfData() {
		return numOfData;
	}
	public void setNumOfData(double numOfData) {
		this.numOfData = numOfData;
	}
	public double getNumOfRequest() {
		return numOfRequest;
	}
	public void setNumOfRequest(double numOfRequest) {
		this.numOfRequest = numOfRequest;
	}
	public double getNumOfFilter() {
		return numOfFilter;
	}
	public void setNumOfFilter(double numOfFilter) {
		this.numOfFilter = numOfFilter;
	}
	public void showFilterCubeStruct(){
		System.out.println("��filter cube��size��СΪ��"+this.fc.size());
		for(Keys k:this.fc.keySet()){
			System.out.println(k.toString());
			System.out.println(this.fc.get(k).getFilters().get(0).toString());
		}
	}
	/*
	 * Ϊfiltercube���ά�ȿ�ܣ�����ԭʼfilter��
	 */
	public void addDimFrameByFilter(Filter f){
		Keys k=new Keys(f);
		
		Values v=new Values(f);
		this.fc.put(k, v);
	}
	public Map<Keys,Values> getFC(){
		return this.fc;
	}
	/*
	 * ����mismatch factor
	 */
	public double calMismatchFactor(List<Data> dats){
		double numSum=0;
		double numPassNoReq=0;
		double numBlockReq=0;
		for(Data d:dats){
			if(d.getExpandState()==0&&d.getUsageCount()==0) numPassNoReq=numPassNoReq+1;
			else if(d.getExpandState()==1&&d.getUsageCount()>0) numBlockReq=numBlockReq+1;
			numSum=numSum+1;
		}
		return 0.5*(numPassNoReq+numBlockReq)/numSum;
	}
	public long getNumOfExpandDatas() {
		return numOfExpandDatas;
	}
	public void setNumOfExpandDatas(long numOfExpandDatas) {
		this.numOfExpandDatas = numOfExpandDatas;
	}
	public double getRestSpaceRate(){
		return this.restSpace/this.fullSpace;
	}
	public void setFullSpace(double sp){
		this.fullSpace=sp;
	}
	public double getFullSpace(){
		return this.fullSpace;
	}
	public void setRestSpace(double rp){
		this.restSpace=rp;
	}
	public double getRestSpace(){
		return this.restSpace;
	}
	public double getSpaceThreshold(){
		return this.spaceThreshold;
	}
	public void setSpaceThreshold(double t){
		this.spaceThreshold=t;
	}
}
