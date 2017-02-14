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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module {
	
	/** The parent {@link Injector}'s instance. */
	private Injector injector;
	
	/**
	 * Can be overridden to perform any kind of module binding.
	 * The default implementation calls {@link #bindProviderMethods()}.
	 */
	protected void bind() {
		bindProviderMethods();
	}
	
	/**
	 * The {@link Injector} this module is being installed into.
	 * 
	 * @param injector the {@link Injector} instance
	 */
	final void configure(Injector injector) {
		if (this.injector != null) {
			throw new RuntimeException(String.format(
					"This module instance %s is already bound", getClass().getName()));
		}
		this.injector = injector;
		bind();
	}

	/**
	 * Returns the {@link Injector} instance that this module was originally installed in.
	 * The module doesn't know if that injector was {@link Injector#extendWith(Module...)}.
	 * The additionally installed modules in the extended injector would return the instance
	 * of the extending injector.
	 * 
	 * @return this {@link Module}'s parent {@link Injector}
	 */
	protected Injector getInjector() {
		return this.injector;
	}
	
	/**
	 * Binds all the module's provider methods.
	 */
	protected void bindProviderMethods() {
		final List<Method> processed = new ArrayList<Method>();
		for (Class<?> c = getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.isAnnotationPresent(Bind.class)
						&& !Modifier.isPrivate(m.getModifiers())	// don't bind private methods
						&& !isOverride(m, processed)) {				// filter overrides
					m.setAccessible(true);
					@SuppressWarnings("rawtypes")
					Provider provider = new MethodProvider(this, m);
					bindProvider(provider);
					processed.add(m);
				}
			}
		}
	}

	/**
	 * Adds provider to the {@link Injector}.
	 * 
	 * @param provider the provider to add
	 */
	protected void bindProvider(Provider<?> provider) {
		getInjector().add(provider);
	}

	private boolean isOverride(Method method, List<Method> processed) {
		for (Method m : processed) {
			if (m.getName().equals(method.getName())
				&& m.getReturnType().equals(method.getReturnType())
				&& Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) {
				return true;
			}
		}
		return false;
	}
}