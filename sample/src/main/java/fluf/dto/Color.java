package fluf.dto;

public class Color {

	public static final Color RED = new Color("red");
	public static final Color BLUE = new Color("blue");
	private String colorName;

	private Color(String name) {
		this.colorName = name;
	}
	
	@Override
	public String toString() {
		return this.colorName;
	}
}