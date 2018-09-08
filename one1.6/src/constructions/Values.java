package constructions;

import java.util.ArrayList;
import java.util.List;


public class Values{
	//��״̬��
	private int status=0;
	private List<Filter> filters=new ArrayList<Filter>();
	private List<Data> datas=new ArrayList<Data>(10000);
	private List<Request> requests=new ArrayList<Request>(1000);
	private int splits=0;
	public Values(Values v){
		this.status=v.getStatus();
		this.datas=v.getDatas();
		this.requests=v.getRequests();
		this.splits=v.getSplits();
		Filter f=Filter.copyFromFilter(v.getFilters().get(0));
		this.addFilter(f);
	}
	/*
	 * ���values�е�����
	 */
	public void clearAllDatas(){
		this.datas.clear();
	}
	/*
	 * ���value���filter
	 */
	public void addFilter(Filter f){
		this.filters.add(f);
	}
	public void addData(Data d){
		this.datas.add(d);
	}
	public void addRequest(Request r){
		this.requests.add(r);
	}
	public Values(){
	}
	public Values(Filter f){
		this.filters.add(f);
	}
	public int getStatus() {
		return this.status;
	}
	public void setStatus(int st) {
		this.status=st;
	}
	public List<Data> getDatas() {
		return datas;
	}
	public void setDatas(List<Data> datas) {
		this.datas = datas;
	}
	public List<Request> getRequests() {
		return requests;
	}
	public void setRequests(List<Request> requests) {
		this.requests = requests;
	}
	public int getSplits() {
		return splits;
	}
	public void setSplits(int splits) {
		this.splits = splits;
	}
	public void setFilters(List<Filter> f){
		this.filters=f;
	}
	public List<Filter> getFilters(){
		return this.filters;
	}
	public void clearAllRequests() {
		// TODO Auto-generated method stub
		this.requests.clear();
	}
	public void changeDimensionValue(String dimension, double minV, double maxV) {
		// TODO Auto-generated method stub
		if(this.filters.get(0).getDims().containsKey(dimension)){
			this.filters.get(0).getDims().get(dimension).setMinBord(minV);
			this.filters.get(0).getDims().get(dimension).setMaxBord(maxV);
		}else{
			System.err.println("�ı�ά��ֵ����");
		}
		
	}
}