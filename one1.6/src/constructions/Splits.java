package constructions;

public class Splits{
	private String dimension;
	private int splits=1;
	private double minBord;
	private double maxBord;
	public Splits(String str,int split,double min,double max){
		this.setDimension(str);
		this.setSplits(split);
		this.setMinBord(min);
		this.setMaxBord(max);
	}
//	public void doSplit(){
//		
//	}
	/*
	 * �ж�һ��splits�Ƿ�����һ��splits��ͬ
	 */
	public boolean isEqual(Splits s){
		if(this.getDimension().equals(s.getDimension())
				&&this.getMinBord()==s.getMinBord()
				&&this.getMaxBord()==s.getMaxBord())
			return true;
		else return false;
	}
	//�ж�һ��ֵ�ǲ��Ǵ����ڸ�splits�ķ�Χ
	public boolean inRange(double s){
		if(s<=this.getMaxBord()&&s>=this.getMinBord()) return true;
		else return false;
	}
	//�ж�һ��split�ǲ��Ǵ����ڸ�split�ķ�Χ��
	public boolean inRange(Splits s){
		if(!s.getDimension().equals(this.dimension)) return false;
		else{
			if(s.getMinBord()>=this.getMinBord()&&s.getMaxBord()<=this.getMaxBord())
				return true;
		}
		return false;
	}
	public String toString(){
		return this.getDimension()+"��Ƭ��Ϊ��"+this.getSplits()+"�����ֵΪ��"
				+this.getMaxBord()+"����СֵΪ��"+this.getMinBord();
	}
	public double getMinBord() {
		return minBord;
	}
	public void setMinBord(double minBord) {
		this.minBord = minBord;
	}
	public double getMaxBord() {
		return maxBord;
	}
	public void setMaxBord(double maxBord) {
		this.maxBord = maxBord;
	}
	public String getDimension() {
		return dimension;
	}
	public void setDimension(String dimension) {
		this.dimension = dimension;
	}
	public int getSplits() {
		return splits;
	}
	public void setSplits(int splits) {
		this.splits = splits;
	}
	public double getDistance(){
		return (this.maxBord-this.minBord);
	}
	public Splits copySplits(){
		Splits s=new Splits(this.dimension,this.splits,this.minBord,this.maxBord);
		return s;
	}
}