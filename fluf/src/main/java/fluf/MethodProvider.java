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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class MethodProvider<T> extends Provider<T> {
	
	/** The source {@link Module} of this provider. */
	private final Module sourceModule;
	
	/** The provider method that returns the object instance. */
	private final Method method;

	/** Name of this provider. If not specified otherwise, it is the name of the provider method. */
	protected String name;

	/** Provider type. */
	protected Type type;
	
	/** Dependency types. */
	protected Type[] dependencies;
	
	/**
	 * Constructor.
	 * 
	 * @param sourceModule instance of the source {@link Module}
	 * @param method the provider method
	 */
	MethodProvider(Module sourceModule, Method method) {
		checkNotNull(method, "method");
		checkNotNull(sourceModule, "module instance");

		this.sourceModule = sourceModule;
		this.method = method;
		setInjector(sourceModule.getInjector());
		
		Bind bindAnnotation = method.getAnnotation(Bind.class);
		final String bindingName = bindAnnotation.value();
		this.name = (bindingName == null || bindingName.length() == 0 ? method.getName() : bindingName);
		setCached(bindAnnotation.cache());
	}

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
		if (this.type == null) {
			this.type = Primitives.convertToObject(method.getGenericReturnType());
		}
		return this.type;
	}

	/**
	 * Returns an array of types of this provider's dependencies.
	 * 
	 * @return the type array
	 */
	Type[] getDependencies() {
		if (this.dependencies == null) {
			this.dependencies = method.getGenericParameterTypes();
		}
		return this.dependencies;
	}
	
	protected Module getSourceModule(){
		return this.sourceModule;
	}
	
	@Override
	Object get(Object[] dependencyValues) {
		try {
			return method.invoke(sourceModule, dependencyValues);
		} catch (Throwable e) {
			throw new RuntimeException(
					String.format("Error invoking method %s\nwith arguments %s", method, Fluf.arrayToString(dependencyValues)), e);
		}
	}
	
	@Override
	String getDependencyName(int pos) {
		final Annotation[] annot = method.getParameterAnnotations()[pos];
		for (Annotation a : annot) {
			if (a.annotationType().equals(Bind.class)) {
				final String value = ((Bind)a).value();
				return (value == null || value.length() == 0 ? null : value);
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass())
			return false;
		@SuppressWarnings("rawtypes")
		MethodProvider other = (MethodProvider) obj;
		return sourceModule.equals(other.getSourceModule())
				&& name.equals(other.name)
				&& getType().equals(other.getType()) ;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(sourceModule.getClass().toString())
			.append("#")
			.append(method)
			.toString(); 
	};
}