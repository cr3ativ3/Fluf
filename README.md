# Fluf
Fluf is a very flexible and lightweight Java dependency injection (DI) micro-framework. Inspired by the likes of [Guice], [Picocontainer] and [Feather] it is designed as a small independant jar (under 15kb) that you can just "drop-in" to provided basic dependency management functionality to your applications.

``` java
// Simple!
Injector injector = Fluf.createInjector(new MyModule());
MyService service = injector.get(MyService.class);
```

## Purpose
It is sad when you need a few specific features and suddenly majority of your application consists of never used code because you included a big can-do-it-all library. Fluf does not try to be an all purpose solution, it just dose what it is meant to do without bloating or interfering and its feature set enforces best practices and well though out design sm**all** apps should follow.

## Features
 * Provider-Method based injection from multiple Modules
 * Constructor & Setter injection
 * Supports injectin parameterized types, collections, poviders
 * Supports specific and wildcard qualifiers
 * Does not intrude your codebase and you have all the control over the injection process
 * No need to modify (not even annotate) injectable classes
 * Supports providing singletons, scoping and factory-like assisted injection

## Download
* [Download] the latest Jar

[//]: # (Below are markdown link targets)
[Guice]:<https://github.com/google/guice>
[Picocontainer]:<https://github.com/picocontainer/picocontainer>
[Feather]:<https://github.com/zsoltherpai/feather>
[Download]:<#>

## Want to contribute?
 * Use it. If you find a bug, report it.
 * Pull requests for more unit tests are always welcome.
 * New feature or improvement suggestions are also welcome if they align with Fluf's core mission.

## Usage examples

### Modules
``` java
// all modules must extend from fluf.Module
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
```
### Injectors
``` java
// create new Injector from any number of modules
Injector injector = Fluf.createInjector(new MyAppModule(), new AnotherModule());

// retrieve injected insatnces by class
StorageBuilding building = injector.get(StorageBuilding.class);

// use TypeLiteral to retrieve parametrized types
Storage<Bike> bikeStorage = injector.get(new TypeLiteral<Storage<Bike>>(){});

// retrieve by specifying provider's name the same way as with dependencies
Storage<Wheel> wheelStorage = injector.get(new TypeLiteral<Storage<Wheel>>(){}, "getWheelsByType");
```
### Extending Injectors (scoping)
``` java
// application scope injector uses application scope modules
Injector appInjector = Fluf.createInjector(new MyAppModule());

// sub-scope injectors inherit parent's providers and includes providers from additional modules
Injector sessionInjector = appInjector.exendedWith(new SessionModule());

// when sessionInjector is no longer referenced, it, session module and its providers will also be garbage collected
sessionInjector = null;
```
### Assisted injection (dynamic proxy factories)
``` java
public class Application {

	// proxy factory is an interface with methods bound to some module's provider method
	public interface BikeFactory {
		// binds to a provider by matching name return type and argument types
		// does not require @Bind, unless you want to provide an alias like method below
		Bike makeMeABike(Color color); 

		@Bind("makeMeABike")
		// these both bind to the same provider, name can be aliased
		Bike createBike(Color color);
	}
	
    ...
	
	public Application() {
		Injector injector = Fluf.createInjector(new MyAppModule());

		// create factory proxy providing underlying Injector and even adding additional modules
		BikeFactory factory = Fluf.createProxy(BikeFactory.class, injector,
				new AnotherModule());

		// or create factory proxy straight from modules
		factory = Fluf.createProxy(BikeFactory.class, new MyAppModule());

		// now you can provide argument input to assist injection
		Bike redBike = factory.makeMeABike(Color.RED);
		Bike blueBike = factory.createBike(Color.BLUE);
	}
}
```

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.