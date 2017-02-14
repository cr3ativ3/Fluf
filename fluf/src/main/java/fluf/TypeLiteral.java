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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Represents a generic type {@code T}. Java doesn't yet provide a way to
 * represent generic types, so this class does. Forces clients to create a
 * subclass of this class which enables retrieval the type information even at
 * runtime.
 *
 * <p>For example, to create a type literal for {@code List<String>}, you can
 * create an empty anonymous inner class:
 * <p>
 * {@code TypeLiteral<List<String>> list = new TypeLiteral<List<String>>() {};}
 * 
 * @param <T> type this literal represents
 */
@SuppressWarnings("unchecked")
public abstract class TypeLiteral<T> {
	
	/** Type this instance represents. */
	private Type type;
	
	/** Raw type this instance represents. */
	private Class<?> rawType;

	/**
	 * Constructs a new type literal. Derives represented class from type
	 * parameter.
	 * 
	 * <p>
	 * Clients create an empty anonymous subclass. Doing so embeds the type
	 * parameter in the anonymous class's type hierarchy so we can reconstitute
	 * it at runtime despite erasure.
	 */
	public TypeLiteral() {
		ParameterizedType genClass = (ParameterizedType) getClass().getGenericSuperclass();
		Type[] types = genClass.getActualTypeArguments();
		if (types == null || types.length == 0) {
			throw new RuntimeException("TypeLiteral<T> must have a specfied type <T>");
		}
		this.type = types[0];
		if (this.type instanceof GenericArrayType)
			throw new RuntimeException("TypeLiteral does not support GenericArrayTypes, use Injector.get(ComponentType[].class)");
		if (this.type instanceof TypeVariable)
			throw new RuntimeException("TypeLiteral does not support TypeVariables");
		
		if (type instanceof Class) {
			this.rawType = (Class<T>) this.type;
		} else if (type instanceof ParameterizedType) {
			this.rawType =  (Class<T>) ((ParameterizedType) type).getRawType();
		} else {
			this.rawType = Object.class;
		}
	}

	/**
	 * Returns type this {@link TypeLiteral<T>} represents.
	 * 
	 * @return type
	 */
	Type getType() {
		return this.type;
	}
	
	/**
	 * Returns raw type (top most parent type) as class this {@link TypeLiteral<T>} represents.
	 * 
	 * @return type if its instance of class, raw type as class if type is an instance
	 * of {@link ParameterizedType}
	 */
	Class<T> getRawType() {
		return (Class<T>) this.rawType;
	}
}