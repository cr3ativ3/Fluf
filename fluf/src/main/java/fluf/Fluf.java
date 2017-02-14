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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

/**
 * The entry point to the framework. Creates {@link Injector}s from
 * {@link Module}s.
 */
public class Fluf implements InvocationHandler {

	/**
	 * Creates an {@link Injector} from given {@link Module}s.
	 * 
	 * @param modules the injector's modules
	 * @return the injector instance
	 */
	public static Injector createInjector(Module...modules) {
		return new Injector(modules);
	}

	/**
	 * Creates a dynamic proxy backed by provided {@link Injector}. If any additional
	 * {@link Module}s are given, then the given injector is extended using those modules.
	 * 
	 * @see Injector#extendWith(Module...)
	 * 
	 * @param interfaceClass dynamic proxy interface class
	 * @param injector the parent injector for the proxy
	 * @param modules modules used to extend the injector
	 * @param <T> proxy instance type
	 * 
	 * @return proxy instance
	 */
	public static <T> T createProxy(Class<T> interfaceClass, Injector injector, Module...modules) {
		if (modules.length > 0){
			injector = injector.extendWith(modules);
		}
		return new Fluf(injector).asProxy(interfaceClass);
	}
	
	/**
	 * Creates a dynamic proxy backed an {@link Injector} created using given {@link Module}s.
	 * 
	 * @param interfaceClass dynamic proxy interface class
	 * @param modules modules used to create the injector for this proxy
	 * @param <T> proxy instance type
	 * 
	 * @return proxy instance
	 */
	public static <T> T createProxy(Class<T> interfaceClass, Module...modules) {
		return new Fluf(new Injector(modules)).asProxy(interfaceClass);
	}

	/** Injector used by this proxy. */
	private final Injector injector;
	
	/**
	 * Constructor.
	 * 
	 * @param injector the injector for this instance to use
	 */
	protected Fluf(Injector injector) {
		this.injector = injector;
	}
	
	/**
	 * Returns this instance as a dynamic java proxy.
	 * 
	 * @param interfaceClass the interface class
	 * @return dynamic proxy of type {@code T}
	 */
	protected <T> T asProxy(Class<T> interfaceClass) {
		return interfaceClass.cast(Proxy.newProxyInstance(
				interfaceClass.getClassLoader(), new Class[] { interfaceClass }, this));
	}

	@Override
	public Object invoke(Object instance, Method method, Object[] argValues) throws Throwable {
		final String boundName = method.isAnnotationPresent(Bind.class) ?
				method.getAnnotation(Bind.class).value() : null;
		final String name = (boundName == null  || boundName.length() == 0 ? method.getName() : boundName);
		final Type returnType = method.getGenericReturnType();
		final Type[] arguments = method.getGenericParameterTypes();
		final Provider<?> provider = injector.find(returnType, name, arguments);
		if (provider == null) {
			throw new RuntimeException(String.format("Method not found. %s %s (%s)", returnType, name, arrayToString(arguments)));
		}
		return provider.get(argValues);
	}

	/**
	 * Constructs a string representation by calling {@link #toString()} on each method
	 * in the given array or null if the array object is null.
	 * 
	 * @param items the array
	 * @return string representation of item array
	 */
	static <T> String arrayToString(T[] items) {
		if (items == null) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		for (T i : items) {
			sb.append(", ").append(i);
		}
		return sb.toString().substring(2);
	}
}