package fluf.dto;

public class Bike {

	private Wheel frontWheel;
	private Wheel backWheel;
	private Color color;

	public void setFrontWheel(Wheel wheel) {
		this.frontWheel = wheel;
	}

	public void setBackWheel(Wheel wheel) {
		this.backWheel = wheel;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public String toString() {
		return this.color + " bike";
	}
}
