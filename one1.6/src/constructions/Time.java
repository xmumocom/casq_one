package constructions;

public class Time {
	private int year;
	private int month;
	private int date;
	private int hour;
	private int minute;
	private int second;
	//ͨ��string���ɣ�string���ӣ�2014-07-01 00:03:13.0
	public Time(String s){
		String[] splits=s.split(" ");
		if(splits.length==2){
			String[] s1=splits[0].split("-");
			String[] s2=splits[1].split(":");
			this.year=(int)Double.parseDouble(s1[0]);
			this.month=(int)Double.parseDouble(s1[1]);
			this.date=(int)Double.parseDouble(s1[2]);
			this.hour=(int)Double.parseDouble(s2[0]);
			this.minute=(int)Double.parseDouble(s2[1]);
			this.second=(int)Double.parseDouble(s2[2]);
		}
	}
	public Time(){
		year=2014;
		month=7;
		date=3;
		hour=7;
		minute=0;
		second=0;
	}
	public Time(int x,int y,int z,int a,int b,int c){
		this.year=x;
		this.month=y;
		this.date=z;
		this.hour=a;
		this.minute=b;
		this.second=c;
	}
	public Time(double time){
		year=2014;
		month=7;
		date=3;
		int times=(int)time;
		hour=times/3600;
		times=times%3600;
		minute=times/60;
		second=times%60;
	}
	public int[] getTime(){
		int res[]=new int [6];
		res[0]=this.year;
		res[1]=this.month;
		res[2]=this.date;
		res[3]=this.hour;
		res[4]=this.minute;
		res[5]=this.second;
		return res;
	}
	public String toString(){
		StringBuilder s=new StringBuilder(this.year);
		s.append("-").append(this.month).append("-").append(this.date).append(" ")
			.append(this.hour).append(":").append(this.minute).append(":").append(this.second);
		
		return s.toString();
	}
	//������Ӧ�ڷ������е�ʱ�䣬���ｨ�������ģ�����1����������0ʱ0��0�뿪ʼ
	public double getSimTime(){
		return this.second+this.minute*60+this.hour*3600;
	}
}
