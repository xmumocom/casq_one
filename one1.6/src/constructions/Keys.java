package constructions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Keys{
	public Keys next=null;
	private Map<String,Splits> key=new LinkedHashMap<String,Splits>();
	public Keys(Map<String,Splits> m){
		this.key.putAll(m);
	}
	public boolean isEqual(Keys ky){
		for(String s:ky.getKey().keySet()){
			if(!this.key.containsKey(s)||!this.key.get(s).isEqual(ky.getKey().get(s))) return false;
		}
		return true;
	}
	/*
	 * ����filter���ά��
	 */
	public Keys(Filter f){
		Set<String> dimension=f.getDims().keySet();
		for(String s:dimension){
			double maxv=f.getDims().get(s).getMaxBord();
			double minv=f.getDims().get(s).getMinBord();
			Splits sp=new Splits(s,1,minv,maxv);
			this.key.put(s, sp);
		}
	}
	public Keys(Keys k){
		for(String s:k.getKey().keySet()){
			Splits sp=new Splits(s,1,k.getKey().get(s).getMinBord(),k.getKey().get(s).getMaxBord());
			this.key.put(s, sp);
		}
	}
	//�޸�keys��ĳ��ά�ȵ�ֵ
	public void changeDimensionValue(String dim,double minV,double maxV){
		if(this.key.containsKey(dim)){
			this.key.get(dim).setMinBord(minV);
			this.key.get(dim).setMaxBord(maxV);
			
		}else{
			System.out.println("�޸�filter cube ��key�е�ά��ֵʧ��");
		}
	}
	public void addKey(String s,double mind,double maxd){
		Splits sp=new Splits(s,1,mind,maxd);
		this.key.put(s, sp);
	}
	public Map<String,Splits> getKey() {
		return key;
	}
	public void setKey(Map<String,Splits> key) {
		this.key = key;
	}
	//�ж�һ��filter�Ƿ���key�ķ�Χ֮��
	public boolean inRange(Filter f){
		int i=0;
		if(this.key.size()<1) return false;
		for(String s:this.key.keySet()){
			if(f.getDims().keySet().contains(s)
					&&this.key.get(s).inRange(f.getDims().get(s)))
				i++;
		}
		if(i==this.key.keySet().size()) return true;
		else return false;
	}
	//�ж�һ��data�Ƿ���key�ķ�Χ֮��
	public boolean inRange(Data d){
		int i=0;
		if(this.key.size()<1) return false;
		for(String s:this.key.keySet()){
			if(d.getDimensions().keySet().contains(s)
					&&this.key.get(s).inRange(d.getDimensions().get(s)))
				i++;
		}
		if(i==this.key.keySet().size()) return true;
		else return false;
	}
	//�ж�һ��requst�Ƿ���key�ķ�Χ֮��
	public boolean inRange(Request r){
		int i=0;
		if(this.key.size()<1) return false;
		for(String s:this.key.keySet()){
			if(this.getKey().keySet().contains(s)
					&&this.key.get(s).inRange(r.getDims().get(s)))
				i++;
		}
		if(i==r.getDims().keySet().size()) return true;
		else return false;
	}
	public String toString(){
		String res="��filter cubeά�ȣ�\n";
		for(String s:this.key.keySet()){
			res=res+s+":���ֵ��"+this.key.get(s).getMaxBord()+"��Сֵ��"+this.key.get(s).getMinBord()+"\n";
		}
		return res;
	}
	public List<String> getDimensions(){
		List<String> res=new ArrayList<String>();
		for(String s:this.getKey().keySet()){
			res.add(s);
		}
		return res;
	}
	public Keys(){
		
	}
	/*
	 * ����һ����Ϊͷָ���key
	 */
	public static Keys creatHeadKey(){
		Keys k=new Keys();
		return k;
	}
	
}