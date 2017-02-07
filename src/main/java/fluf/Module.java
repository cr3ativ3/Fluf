package fluf;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module {
	
	/** The parent {@link Injector}'s instance. */
	private Injector injector;
	
	/**
	 * The {@link Injector} this module is being installed into.
	 * 
	 * @param injector the {@link Injector} instance
	 */
	void configure(Injector injector){
		if (this.injector != null){
			throw new RuntimeException(String.format(
					"Module instance %s is already installed", getClass().getName()));
		}
		this.injector = injector;
		resolveMethodProviders();
	}
	
	/**
	 * Returns the {@link Injector} instance that this module was originally installed in.
	 * The module doesn't know if that injector was {@link Injector#extendWith(Module...)}.
	 * The additionally installed modules in the extended injector would return the instance
	 * of the extending injector.
	 * 
	 * @return this {@link Module}'s parent {@link Injector}
	 */
	protected Injector getInjector(){
		return this.injector;
	}
	
	private void resolveMethodProviders(){
		final List<Method> processed = new ArrayList<Method>();
		for (Class<?> c = getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.isAnnotationPresent(Bind.class)
						&& !Modifier.isPrivate(m.getModifiers())	// don't bind private methods
						&& !isOverride(m, processed)){				// filter overrides
					m.setAccessible(true);
					@SuppressWarnings("rawtypes")
					Provider provider = new Provider(getInjector(), m, this);
					getInjector().add(provider);
					processed.add(m);
				}
			}
		}
	}

	private boolean isOverride(Method method, List<Method> processed) {
		for (Method m : processed){
			if (m.getName().equals(method.getName())
				&& m.getReturnType().equals(method.getReturnType())
				&& Arrays.equals(m.getParameterTypes(), method.getParameterTypes())){
				return true;
			}
		}
		return false;
	}
}