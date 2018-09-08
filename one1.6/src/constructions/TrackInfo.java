package constructions;

import core.Coord;

public class TrackInfo {
	private Coord location;
	private Time time;
	private double speed;
	private double direction;
	public Time getTime() {
		return time;
	}

	public void setTime(Time time) {
		this.time = time;
	}

	public Coord getLocation() {
		return location;
	}

	public void setLocation(Coord location) {
		this.location = location;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getDirection() {
		return direction;
	}

	public void setDirection(double direction) {
		this.direction = direction;
	}
	//ͨ��string������trackInfo,��ʽΪ��ʱ�䣬x���꣬y���꣬�ٶȣ�����
	public TrackInfo(String s){
		String[] splits=s.split(",");
		this.time=new Time(splits[0]);
		this.location=new Coord(Double.parseDouble(splits[1]),Double.parseDouble(splits[2]));
		if(splits[3].equals("null")){
			this.speed=0;
		}else this.speed=Double.parseDouble(splits[3]);
		this.direction=Double.parseDouble(splits[4]);
	}
}
