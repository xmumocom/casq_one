package constructions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import core.Coord;
import core.DTNHost;
import core.SimClock;
import core.SimScenario;

public class ZipfGenerator {
	public int usedId=0;//当前被使用的地点id
	public double genTime=0;
	private static ZipfGenerator myZipfGenerator=null;
	private Random rnd = new Random(System.currentTimeMillis());
	private int size;
	private double skew;
	private double bottom = 0;
	public int[] signlist={0,0,0,0,0,0,0,0,0,0};
	public int signpl=0;
	public List<Integer> places=new ArrayList<Integer>();
	public List<Coord> placesForRSU=new ArrayList<Coord>();
	public ZipfGenerator(int sizes, double skew) {
		this.size = sizes;
		this.skew = skew;
		for(int i=25;i<50;i++){
			for(int j=24;j>=0;j--){
				this.places.add(i*100+j);
			}
			for(int j=25;j<50;j++){
				this.places.add(i*100+j);
			}
			
		}
		for(int i=24;i>=0;i--){
			for(int j=24;j>=0;j--){
				this.places.add(i*100+j);
			}
			for(int j=25;j<50;j++){
				this.places.add(i*100+j);
			}
			
		}
	
		for(int i=1;i < size; i++) {
			this.bottom += (1/Math.pow(i, this.skew));
		}
		this.genSignList();
	}
	//此时利用rsu的位置生成相关数据
	public ZipfGenerator(int sizes, double skew,int type) {
		this.size = sizes;
		this.skew = skew;
		List<DTNHost> edge=SimScenario.getInstance().getEdges();
		for(int i=0;i<edge.size();i++){
			this.placesForRSU.add(edge.get(i).getLocation());
		}
		this.size=edge.size();
	
		for(int i=1;i < this.size; i++) {
			this.bottom += (1/Math.pow(i, this.skew));
		}
		this.changePlaceIDForRSU(0.5);
		this.genSignList();
	}
	public void changePlaceID(double x){
		List<Integer> newplaces=new ArrayList<Integer>(2500);
		for(int i=(int)(x*this.size);i<this.size;i++){
			newplaces.add(this.places.get(i));
		}
		for(int i=0;i<(int)(x*this.size);i++){
			newplaces.add(this.places.get(i));
		}
		this.genSignList();
	}
	public void changePlaceIDForRSU(double x){
		List<Coord> newplaces=new ArrayList<Coord>();
		for(int i=(int)(x*this.size);i<this.size;i++){
			newplaces.add(this.placesForRSU.get(i));
		}
		for(int i=0;i<(int)(x*this.size);i++){
			newplaces.add(this.placesForRSU.get(i));
		}
		this.genSignList();
	}
 // the next() method returns an random rank id.
 // The frequency of returned rank ids are follows Zipf distribution.
	public int next() {
		int rank;
		double friquency = 0;
		double dice;
 
		rank = rnd.nextInt(size);
		friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
		dice = rnd.nextDouble();
		
		while(!(dice < friquency)) {
			rank = rnd.nextInt(size);
			friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
			dice = rnd.nextDouble();
		}
 
		return rank;
	}
 
 	// This method returns a probability that the given rank occurs.
 	public double getProbability(int rank) {
 		return (1.0d / Math.pow(rank, this.skew)) / this.bottom;
 	}
 
// public static void main(String[] args) {
//   if(args.length != 2) {
//     System.out.println("usage: ./zipf size skew");
//     System.exit(-1);
//   }
// 
//   ZipfGenerator zipf = new ZipfGenerator(Integer.valueOf(args[0]),
//   Double.valueOf(args[1]));
//   for(int i=1;i <= 100; i++)
//     System.out.println(i+" "+zipf.getProbability(i));
// }
 	//生成列表判断是否产生查询
 	public void genSignList(){
 		this.usedId=this.next();
 		for(int i=0;i<10;i++){
 			signlist[i]=0;
 		}
 		Random r=new Random(System.currentTimeMillis());
 		int b=r.nextInt(10);
 		int len=r.nextInt(10);
 		for(int i=0;i<len;i++){
 			if(b+i>9) signlist[b+i-10]=1;
 			else signlist[b+i]=1;
 		}
 		this.signpl=0;
 	}
 	//获得查询地点，x范围1000-10500 y范围2000-15000
 	public Coord getCoord(){
 		if(signpl>9){
 			this.genSignList();
 			this.signpl=0;
 		}
 		if(this.signlist[signpl]==0){
 			signpl++;
 			return getCoord();
 		}else{
 			double xs=Math.random(),ys=Math.random();
 			double x=1000+(9500.0/50)*(this.places.get(this.usedId)/100+xs);
 			double y=2000+(13000.0/50)*(this.places.get(this.usedId)%100+ys);
 			signpl++;
 			Coord res=new Coord(x,y);
 			return res;
 		}
 		
 	}
 	//获得查询地点RSU
 	 	public Coord getCoordForRSU(){
 	 		if(signpl>9){
 	 			this.genSignList();
 	 			this.signpl=0;
 	 		}
 	 		if(this.signlist[signpl]==0){
 	 			signpl++;
 	 			return null;
 	 		}else{
 	 			signpl++;
 	 			Coord res=this.placesForRSU.get(usedId);
 	 			return res;
 	 		}
 	 		
 	 	}
 	public static ZipfGenerator getInstance(){
 		if(myZipfGenerator==null){
 			myZipfGenerator=new ZipfGenerator(2500,0.8,0);
 		}
 		if(SimClock.getTime()-myZipfGenerator.genTime>3600){
 			double x=Math.random();
 			myZipfGenerator.changePlaceIDForRSU(x);
 			myZipfGenerator.genTime=SimClock.getTime();
 		}
		return myZipfGenerator;	
 	}
}
