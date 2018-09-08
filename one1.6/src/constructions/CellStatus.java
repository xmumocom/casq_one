package constructions;

public class CellStatus {
	private double usedDatas=0;
	private double datas=0;
	private double requests=0;
	public double getDatas() {
		return datas;
	}
	public void setDatas(double datas) {
		this.datas = datas;
	}
	public void addDatas(){
		this.datas=this.datas+1;
	}
	public double getRequests() {
		return requests;
	}
	public void setRequests(double requests) {
		this.requests = requests;
	}
	public void addRequests(){
		this.requests=this.requests+1;
	}
	public double getUsedDatas() {
		return usedDatas;
	}
	public void setUsedDatas(double usedDatas) {
		this.usedDatas = usedDatas;
	}
	public void addUsedDatas(){
		this.usedDatas=this.usedDatas+1;
	}
	
}
