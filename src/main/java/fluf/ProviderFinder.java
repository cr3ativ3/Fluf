package fluf;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Matcher class that finds, depending on search criteria, and returns all matching Providers 
 * from the collection of Providers it has.
 */
@SuppressWarnings("rawtypes")
class ProviderFinder {

	/** Collection to search in. */
	private Collection<Provider> providers;
	
	/** ReturnType search criteria. */
	private Type returnType;
	
	/** Provider's name search criteria.  */
	private String name;
	
	/** Arguments' types search criteria.  */
	private Type[] arguments;

	/**
	 * Constructor.
	 * 
	 * @param providers providers to search in
	 */
	public ProviderFinder(List<Provider> providers) {
		this.providers = providers;
	}

	/**
	 * Sets return type search criteria.
	 * 
	 * @param returnType the provider's return type
	 * @return this instance of {@link ProviderFinder}
	 */
	ProviderFinder byReturnType(Type returnType) {
		this.returnType = returnType;
		return this;
	}

	/**
	 * Sets name search criteria. Name is matched using {@link String#matches(String)}.
	 * 
	 * @param name the provider's name
	 * @return this instance of {@link ProviderFinder}
	 */
	ProviderFinder byName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Sets argument types search criteria.
	 * 
	 * @param arguments types of providers dependencies
	 * @return this instance of {@link ProviderFinder}
	 */
	ProviderFinder byArguments(Type[] arguments) {
		this.arguments = arguments;
		return this;
	}
	
	/**
	 * Returns all providers matching search criteria.
	 * 
	 * @return collection of providers
	 */
	Collection<Provider> find(){
		Collection<Provider> matched = new ArrayList<Provider>();
		for (Provider p : this.providers){
			if (matches(p)){
				matched.add(p);
			}
		}
		return matched;
	}
	
	private boolean matches(Provider p) {
		boolean match = true;
		match = (match && returnType != null ? p.getType().equals(returnType) : match);
		match = (match && name != null ? p.getName().matches(name) : match);
		match = (match && arguments != null ? Arrays.equals(p.getDependencies(), arguments) : match);
		return match;
	}
}