package fluf;

import fluf.Bind;
import fluf.Fluf;
import fluf.dto.Bike;
import fluf.dto.Color;

public class Application {

	private Bike redBike;
	private Bike blueBike;
	
	/**
	 * Runnable main method.
	 * @param args
	 */
	public static void main(String[] args) {
		Application app = new Application();
		app.run();
	}
	
	public Application() {
		Injector injector = Fluf.createInjector(new MyAppModule());

		// create factory proxy providing underlying Injector and even adding additional modules
		BikeFactory factory = Fluf.createProxy(BikeFactory.class, injector,
				new AnotherModule());

		// or create factory proxy straight from modules
		factory = Fluf.createProxy(BikeFactory.class, new MyAppModule());

		// now you can provide argument input to assist injection
		this.redBike = factory.makeMeABike(Color.RED);
		this.blueBike = factory.createBike(Color.BLUE);
	}

	private void run() {
		System.out.println("This is a " + this.redBike);
		System.out.println("This is a " + this.blueBike);
	}

	// proxy factory is an interface with methods bound to some module's provider method
	public interface BikeFactory {
		// binds to a provider by matching name return type and argument types
		// does not require @Bind, unless you want to provide an alias like method below
		Bike makeMeABike(Color color); 

		@Bind("makeMeABike")
		// these both bind to the same provider, name can be aliased
		Bike createBike(Color color);
	}
}