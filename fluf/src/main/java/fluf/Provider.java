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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Provider instance. These providers are created from a {@link Module}'s binding methods.
 * <p>
 * The return type of a method acts as a binding key while the name of a method can be 
 * used as a qualifier in case of return type ambiguity.
 * 
 * @param <T> the type of this {@link Provider} indicating what type of instances
 * this provider creates.
 */
public abstract class Provider<T> {
	
	private static final Object UNINITIALIZED = new Object();
	
	private volatile Object result = UNINITIALIZED;

	/** Parent {@link Injector} used to resolve dependencies of this provider. */
	private Injector injector;
	
	/** Flag indicating whether this provider caches its result. */
	private boolean isCached;

	/**
	 * Returns the name of this provider.
	 * 
	 * @return the name
	 */
	abstract String getName();
	
	/**
	 * Returns the type of this provider.
	 * 
	 * @return the type
	 */
	abstract Type getType();

	/**
	 * Returns an array of types of this provider's dependencies.
	 * 
	 * @return the type array
	 */
	abstract Type[] getDependencies();
	
	/**
	 * Returns the name of a named dependency or null if the dependency is unnamed.
	 * 
	 * @param i dependency position corresponding to the dependency in the
	 * array returned by {@link #getDependencies()}
	 * @return name of the dependency or null if it has no specified name
	 */
	abstract String getDependencyName(int i);
	
	/**
	 * Invokes this provider to provide an instance with given values to be used
	 * for its dependencies.
	 * 
	 * @param dependencyValues dependency instances
	 * @return object instance whose actual type is the type of this provider
	 */
	abstract Object get(Object[] dependencyValues);

	/**
	 * Provides an instance of type matching this provider's type. Depending on
	 * configuration, some providers might return a new instance on every {@link #get()}
	 * call while other might always return the same.
	 * 
	 * @return an instance of type {@code T}
	 */
	@SuppressWarnings("unchecked")
	public T get() {
		Object value = result;
		if (!isCached) {
			value = get(resolveDependencyValues());
		} else if (value == UNINITIALIZED) {
			synchronized (this) {
				value = result;
				if (value == UNINITIALIZED) {
					value = get(resolveDependencyValues());
					result = value;
				}
			}
		}
		return (T) value;
	}
	
	/**
	 * Sets this {@link Provider}'s {@link Injector}.
	 * 
	 * @param injector the {@link Injector}
	 */
	protected void setInjector(Injector injector) {
		checkNotNull(injector, "Injector");
		this.injector = injector;
	}
	
	/**
	 * Gets this {@link Provider}'s {@link Injector}.
	 * 
	 * @return the {@link Injector}
	 */
	protected Injector getInjector() {
		return this.injector;
	}
	
	/**
	 * Sets a flag determining if this {@link Provider} should cache its result.
	 * 
	 * @param cached the flag value
	 */
	protected void setCached(boolean cached) {
		this.isCached = cached;
	}

	/**
	 * Returns true if this {@link Provider} should cache its result, false otherwise.
	 * 
	 * @return the {@link Provider}'s {@code isChached} flag
	 */
	protected boolean isCached() {
		return this.isCached;
	}
	
	/**
	 * Resolves values of {@link Provider}'s dependencies.
	 * 
	 * @return array of dependency values
	 */
	protected Object[] resolveDependencyValues() {
		final int count = getDependencies().length;
		final Object[] argValues = new Object[count];
		for (int i = 0; i < count; i++) {
			final Type type = getDependencies()[i];
			final String name = getDependencyName(i);
			if (type == Injector.class && name == null) {
				argValues[i] = getInjector();
				continue;
			}
			if (type == Provider.class) {
				Provider<?> provider = getInjector().find(getParametersType(type, 0), name, null);
				argValues[i] = provider;
				continue;
			}
			Provider<?> provider = getInjector().find(type, name, null);
			provider = (this == provider ? null : provider); 
			if (provider == null && isCollection(type)) {
				argValues[i] = collectFromAllProviders(type, name);
				continue;
			}
			checkNotNull(provider, String.format("Provider[type: %s, name: %s, args: %s] ", type, name, getDependencies()));
			argValues[i] = provider.get();
		}
		return argValues;
	}

	/**
	 * Returns true if a particular {@link Type} defines a {@link Collection}, false otherwise.
	 * 
	 * @param type the type to check
	 * @return true if the {@link Type} defines a {@link Collection}
	 */
	protected boolean isCollection(Type type) {
		if (type instanceof Class){
			return (Collection.class.isAssignableFrom((Class<?>) type)
					|| Set.class.isAssignableFrom((Class<?>) type));
		} else if (type instanceof ParameterizedType){
			Type rawType = ((ParameterizedType) type).getRawType();
			return isCollection(rawType);
		}
		return false;
	}

	/**
	 * Resolves the {@code n'th} parameter's type given a ParameterizedType argument.
	 * 
	 * @param type the parameterized type
	 * @param n parameter's index
	 * @return type of parameter at n'th index
	 * 
	 * @throws ArrayIndexOutOfBoundsException if parameter atrray's {@code length < n+1}
	 * @throws RuntimeException if given type is not a parameterized type
	 */
	protected Type getParametersType(Type type, int n) {
		if (type instanceof ParameterizedType) {
			Type[] args = ((ParameterizedType) type).getActualTypeArguments();
			checkNotNull(args, "parameter array");
			if (args.length < n + 1) {
				throw new ArrayIndexOutOfBoundsException(
						String.format("Cannot resolve parameter at %d from array of %d elements", n, args.length));
			}
			return args[n];
		}
		throw new RuntimeException(type + " is not an instance of " + ParameterizedType.class);
	}
	
	/**
	 * Returns a collection of values resolved from all matching {@link Provider}s.
	 * 
	 * @param type the {@link Provider}'s type identifier
	 * @param name the {@link Provider}'s name identifier
	 * @return a collection of matching {@link Provider}s
	 */
	protected Collection<?> collectFromAllProviders(Type type, String name) {
		Injector injector = getInjector();
		@SuppressWarnings("rawtypes")
		Collection<Provider> providers = injector.findMatchingProviders(getParametersType(type, 0), name, null);
		Set<Object> all = new HashSet<Object>();
		for (Provider<?> mp : providers) {
			if (this != mp) {
				all.add(mp.get());
			}
		}
		return all;
	}
	
	/**
	 * Checks if a given object is not null.
	 * 
	 * @param o the object to check
	 * @param name object's name
	 * @return the given object or throws a {@link NullPointerException} if it is null
	 */
	static <T> T checkNotNull(T o, String name) {
		if (o == null) {
			throw new NullPointerException(name + " is null");
		}
		return o;		
	}
}