package fluf;

/**
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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
 * @param <T> the type of this {@link Provider<T>} indicating what type of instances
 * this provider creates.
 */
class Provider<T> {
	
	private static final Object UNINITIALIZED = new Object();
	
	private volatile Object result = UNINITIALIZED;
	
	/** Flag indicating whether this provider caches its result. */
	private final boolean isCached;
	
	/** The parent {@link Module} of this provider. */
	private final Module parentModule;
	
	/** The provider method that returns the object instance. */
	private final Method method;

	/** Name of this provider. If not specified otherwise, it is the name of the provider method. */
	private final String name;

	/** Parent {@link Injector} used to resolve dependencies of this provider. */
	private Injector injector;
	
	/**
	 * Constructor.
	 * 
	 * @param injector the parent {@link Injector}
	 * @param method the provider method
	 * @param parentModule instance of the parent {@link Module}
	 */
	Provider(Injector injector, Method method, Module parentModule){
		checkNotNull(injector, "injector");
		checkNotNull(method, "method");
		checkNotNull(method, "instance");
		
		this.injector = injector;
		this.method = method;
		this.parentModule = parentModule;
		
		Bind bindAnnotation = method.getAnnotation(Bind.class);
		final String bindingName = bindAnnotation.value();
		this.name = (bindingName == null || bindingName.length() == 0 ? method.getName() : bindingName);
		this.isCached = bindAnnotation.cache();
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != Provider.class)
			return false;
		@SuppressWarnings("rawtypes")
		Provider other = (Provider) obj;
		return parentModule.equals(other.parentModule)
				&& name.equals(other.name)
				&& getType().equals(other.getType()) ;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(parentModule.getClass().toString())
			.append("#")
			.append(method)
			.toString(); 
	};
	
	/**
	 * Returns the name of this provider.
	 * 
	 * @return the name
	 */
	String getName() {
		return name;
	}
	
	/**
	 * Returns the type of this provider.
	 * 
	 * @return the type
	 */
	Type getType() {
		return method.getGenericReturnType();
	}

	/**
	 * Returns an array of types of this provider's dependencies.
	 * 
	 * @return the type array
	 */
	Type[] getDependencies() {
		return method.getGenericParameterTypes();
	}
	
	/**
	 * Invokes this provider to provide an instance with given values to be used
	 * for its dependencies.
	 * 
	 * @param dependencyValues dependency instances
	 * @return object instance whose actual type is the type of this provider
	 */
	Object get(Object[] dependencyValues) {
		try {
			return method.invoke(parentModule, dependencyValues);
		} catch (Throwable e) {
			throw new RuntimeException(
					String.format("Error invoking method %s\nwith arguments %s", method, dependencyValues), e);
		}
	}
	
	/**
	 * Provides an instance of type matching this provider's type.
	 * 
	 * @return an instance of type <T>
	 */
	@SuppressWarnings("unchecked")
	T get(){
		Object value = result;
		if (!isCached){
			value = get(resolveArgumentValues());
		} else if (value == UNINITIALIZED) {
			synchronized (this) {
				value = result;
				if (value == UNINITIALIZED) {
					value = get(resolveArgumentValues());
					result = value;
					// We won't need this anymore
					this.injector = null;
				}
			}
		}
		return (T) value;
	}

	private Object[] resolveArgumentValues() {
		final int count = getDependencies().length;
		final Object[] argValues = new Object[count];
		for (int i = 0; i < count; i++){
			final Type type = getDependencies()[i];
			final String name = getBoundName(method, i);
			if (type == Injector.class && name == null){
				argValues[i] = injector;
				continue;
			}
			if (type == Provider.class){
				Provider<?> provider = injector.find(getTypeParameter(type), name, null);
				argValues[i] = provider;
				continue;
			}
			Provider<?> provider = injector.find(type, name, null);
			provider = (this == provider ? null : provider); 
			if (provider == null && isCollection(type)){
				argValues[i] = collectFromAllProviders(injector, type, name);
				continue;
			}
			checkNotNull(provider, String.format("Provider[type: %s, name: %s, args: %s] ", type, name, getDependencies()));
			argValues[i] = provider.get();
		}
		return argValues;
	}

	private String getBoundName(Method method, int pos) {
		final Annotation[] annot = method.getParameterAnnotations()[pos];
		for (Annotation a : annot){
			if (a.annotationType().equals(Bind.class)){
				final String value = ((Bind)a).value();
				return (value == null || value.length() == 0 ? null : value);
			}
		}
		return null;
	}
	
	private boolean isCollection(Type type) {
		if (type instanceof Class){
			return Collection.class.isAssignableFrom((Class<?>) type)
					|| Set.class.isAssignableFrom((Class<?>) type);
		} else if (type instanceof ParameterizedType){
			Type rawType = ((ParameterizedType) type).getRawType();
			return isCollection(rawType);
		}
		return false;
	}
	
	private Collection<?> collectFromAllProviders(Injector injector, Type type, String name) {
		@SuppressWarnings("rawtypes")
		Collection<Provider> providers = injector.findAll(getTypeParameter(type), name, null);
		Set<Object> all = new HashSet<Object>();
		for (Provider<?> mp : providers){
			if (this != mp){
				all.add(mp.get());
			}
		}
		return all;
	}

	private Type getTypeParameter(Type type) {
		if (type instanceof ParameterizedType){
			Type[] args = ((ParameterizedType) type).getActualTypeArguments();
			if (args != null && args.length == 1){
				return args[0];
			}
		}
		throw new RuntimeException("Cannot resolve for type " + type);
	}
	
	private static <T> T checkNotNull(T o, String name) {
		if (o == null)
			throw new NullPointerException(name + " is null");
		return o;		
	}
}