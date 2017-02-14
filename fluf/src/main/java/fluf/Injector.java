package fluf;

/**
 * Copyright 2017 Simonas Galinis
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class responsible for gathering injection bindings from modules as well as
 * resolving the instance for requested type. Basically it acts as both a <b>{@code Binder}</b>
 * and an <b>{@code Injector}</b> as used in terms of other popular injection frameworks.
 */
@SuppressWarnings("rawtypes")
public class Injector {
	
	/** Synchronized list of providers. */
	private List<Provider> providers = Collections.synchronizedList(new ArrayList<Provider>());
	
	/**
	 * Constructor.
	 * 
	 * @param modules modules to install into this {@link Injector}
	 */
	Injector(Module[] modules) {
		install(modules);
	}

	/**
	 * Creates new instance of {@link Injector} that inherits parent injector's providers
	 * and can have additional {@link Module}s installed.
	 * 
	 * @param modules additional modules the extended injector should install
	 * 
	 * @return the extending injector
	 */
	public Injector extendWith(Module...modules) {
		Injector extended = new Injector(new Module[]{});
		for (Provider<?> p : providers) {
			add(p);
		}
		extended.install(modules);
		return extended;
	}

	/**
	 * Returns an instance from a provider matching given {@link TypeLiteral}'s type.
	 * 
	 * @param type type as {@link TypeLiteral}
	 * @param <T> requested type
	 * 
	 * @return instance of type {@code T}
	 */
	public <T> T get(TypeLiteral<T> type) {
		return get(type, null);
	}
	
	/**
	 * Returns an instance from a provider matching given {@link TypeLiteral}'s type
	 * and given name.
	 * 
	 * @param type type as {@link TypeLiteral}
	 * @param name the name
	 * @param <T> requested type
	 * 
	 * @return instance of type {@code T}
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(TypeLiteral<T> type, String name) {
		Provider provider = find(type.getType(), name, null);
		return (provider == null ? null : (T) provider.get()); 
	}
	
	/**
	 * Returns an instance from a provider matching given class type.
	 * 
	 * @param typeClass type as Class
	 * @param <T> requested type
	 * 
	 * @return instance of type {@code T}
	 */
	public <T> T get(Class<T> typeClass) {
		return get(typeClass, null);
	}
	
	/**
	 * Returns an instance from a provider matching given class type and name.
	 * 
	 * @param typeClass type as Class
	 * @param name the name
	 * @param <T> requested type
	 * 
	 * @return instance of type {@code T}
	 */
	public <T> T get(Class<T> typeClass, String name) {
		Provider provider = find(typeClass, name, null);
		return (provider == null ? null : typeClass.cast(provider.get()));
	}
	
	/**
	 * Returns instances from all providers for the given type.
	 * 
	 * @param type requested type as a {@link TypeLiteral}
	 * @param <T> requested type
	 * 
	 * @return a set of instances
	 */
	public <T> Set<T> getAll(TypeLiteral<T> type) {
		return getAll(type, null);
	}
	
	/**
	 * Returns instances from all providers for the given type and matching name.
	 * Name matching is done like {@link String#matches(String)}.
	 * 
	 * @see String#matches(String)
	 * 
	 * @param type requested type as a {@link TypeLiteral}
	 * @param name name to match, will not be used if null
	 * @param <T> requested type
	 * 
	 * @return set of instances
	 */
	public <T> Set<T> getAll(TypeLiteral<T> type, String name) {
		Collection<Provider> providers = findMatchingProviders(type.getType(), name, null);
		return multipleResults(providers, type.getRawType()); 
	}

	/**
	 * Returns set of instances produced by all providers.
	 * 
	 * @param typeClass instance type as class
	 * @param <T> requested type
	 * 
	 * @return set of instances
	 */
	public <T> Collection<T> getAll(Class<T> typeClass) {
		return getAll(typeClass, null);
	}
	
	/**
	 * Returns a set of instances produced by all providers.
	 * Name matching is done like {@link String#matches(String)}.
	 * 
	 * @see String#matches(String)
	 * 
	 * @param typeClass instance type as class
	 * @param name name qualifier, will not be used if null
	 * @param <T> requested type
	 * 
	 * @return set of instances
	 */
	public <T> Set<T> getAll(Class<T> typeClass, String name) {
		Collection<Provider> providers = findMatchingProviders(typeClass, name, null);
		return multipleResults(providers, typeClass);
	}
	
	/**
	 * Returns unmodifiable read-only list of providers.
	 * 
	 * @return list of providers
	 */
	final List<Provider> getProviders() {
		return Collections.unmodifiableList(this.providers);
	}

	/**
	 * Searches and returns a single provider for the given type, name and/or
	 * dependency type array. Null arguments are not set as search criteria.
	 * Name matching is done like {@link String#matches(String)}. Returns single
	 * found provider or null if none match. Throws {@link RuntimeException} if multiple
	 * candidates match.
	 * 
	 * @see String#matches(String)
	 * 
	 * @param type provider type to match
	 * @param name provider name to match
	 * @param dependencies dependency types to match
	 * 
	 * @return matching provider or null if none match
	 */
	Provider find(Type type, String name, Type[] dependencies) {
		return singleResult(findMatchingProviders(type, name, dependencies));
	}
	
	/**
	 * Searches and returns all providers for the given type, name and/or
	 * dependency type array. Null arguments are not set as search criteria.
	 * Name matching is done like {@link String#matches(String)}. Returns single
	 * found provider or empty collection if none match.
	 * 
	 * @see String#matches(String)
	 * 
	 * @param type provider type to match
	 * @param name provider name to match
	 * @param dependencies dependency types to match
	 * 
	 * @return collection with all matching providers, can be empty
	 */
	protected Collection<Provider> findMatchingProviders(Type type, String name, Type[] dependencies) {
		ProviderFinder finder = new ProviderFinder(getProviders())
			.byReturnType(type);
		if (name != null) {
			finder.byName(name);
		}
		if (dependencies != null && dependencies.length > 0) {
			finder.byArguments(dependencies);
		}
		return finder.find();
	}
	
	/**
	 * Adds provider to this {@link Injector} instance.
	 * 
	 * @param provider {@link Provider} to add
	 */
	void add(Provider provider) {
		int pos = this.providers.indexOf(provider);
		if (pos == -1) {
			this.providers.add(provider);
			return;
		}
		Provider other = this.providers.get(pos);
		throw new RuntimeException(
				String.format("Multiple provider methods with matching signatures:\n%s\n%s", provider, other));
	}
	
	private void install(Module...modules) {
		Provider.checkNotNull(modules, "module array");
		for (Module module : modules){
			Provider.checkNotNull(module, "module");
			module.configure(this);
		}
	}
	
	private <T> Set<T> multipleResults(Collection<Provider> collection, Class<T> clazz) {
		Set<T> all = new HashSet<T>();
		for (Provider mp : collection) {
			@SuppressWarnings("unchecked")
			T value = (T) mp.get();
			all.add(value);
		}
		return all;
	}
	
	private Provider singleResult(Collection<Provider> collection) {
		if (collection == null || collection.isEmpty()) {
			return null;
		}
		if (collection.size() > 1) {
			throw new RuntimeException(String.format("Multiple candidates found:\n%s", collection));
		}
		return collection.iterator().next();
	}
}