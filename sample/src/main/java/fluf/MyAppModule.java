package fluf;

import java.util.ArrayList;
import java.util.Collection;

import fluf.Bind;
import fluf.Module;
import fluf.Provider;
import fluf.dto.Bike;
import fluf.dto.BikeStorageBuilding;
import fluf.dto.BikeWheel;
import fluf.dto.Color;
import fluf.dto.Storage;
import fluf.dto.StorageBuilding;
import fluf.dto.Wheel;

public class MyAppModule extends Module {

	// @Bind annotation defines a provider method, provider methods can be
	// public protected or package-protected
	@Bind(cache = true)
	// if 'true' will always return the same instance, defaults to 'false'
	protected Integer wheelSize() {
		return new Integer(10); // creates new instance only once
	}

	@Bind
	// by default providers are named as using method name, this the same as
	// @Bind("backWheel")
	Wheel backWheel(Integer wheelSize) { // dependencies are defiend as provider-method arguments
		return new BikeWheel(wheelSize); // inject through constructor
	}

	@Bind
	Wheel frontWheel(Integer wheelSize) {
		Wheel w = new BikeWheel();
		w.setSize(wheelSize * 2); // inject through setter
		return w;
	}

	// you can use dependency provider names to be specific
	@Bind
	Bike provideBike(@Bind("frontWheel") Wheel wheel1, @Bind("backWheel") Wheel wheel2) {
		Bike bike = new Bike();
		bike.setFrontWheel(wheel1);
		bike.setBackWheel(wheel2);
		return bike;
	}

	@Bind
	Bike makeMeABike(Color color) {
		// modules have access to their parent Injectors
		Bike bike = getInjector().get(Bike.class, "provideBike"); // read more about Injectors below
		bike.setColor(color);
		return bike;
	}

	@Bind
	Storage<Wheel> getWheelsByType(Collection<Wheel> wheels) {
		// collects from all providers providing Wheel type, include car tire
		return new Storage<Wheel>(wheels);
	}

	@Bind
	Storage<Wheel> wheelsByTypeAndFilterByName(@Bind(".+Wheel") Collection<Wheel> wheels) {
		// collects from all providers providing Wheel type whos name also String.matches(".+Wheel")
		// in this example 'forntWheel' and 'backWheel'
		return new Storage<Wheel>(wheels);
	}

	@Bind
	Storage<Bike> bikesByProvider(Provider<Bike> bikeProvider) {
		// every call to bikeProvider.get() will produce a Bike instance (always new insatnce if Bike provider is not cached)
		Collection<Bike> bikes = new ArrayList<Bike>();
		for (int i = 0; i < 10; i++)
			bikes.add(bikeProvider.get());
		return new Storage<Bike>(bikes);
	}

	StorageBuilding providesStorage(Storage<Bike> onlyBikes) { // no need to specify which Storage
		// type matching takes generics into account
		return new BikeStorageBuilding(onlyBikes);
	}
}
