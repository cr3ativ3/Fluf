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
		this.returnType = Primitives.convertToObject(returnType);
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