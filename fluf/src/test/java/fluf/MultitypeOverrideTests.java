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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class MultitypeOverrideTests extends TestCase {

	interface TestApi {
		int addition();
	}
	
	interface Listable {
		List<?> list();
	}
	
	public class ModuleA extends Module implements Listable {

		@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
		@Bind
		@Override
		public List list() {
			return new ArrayList<String>(){{ add(new String("hero")); }};
		}
	}
	
	public class ModuleB extends ModuleA implements Listable {
		
		@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
		@Bind
		@Override
		public List<Integer> list() {
			return new ArrayList(){{ add(new Integer(6)); }};
		}
	}

	@SuppressWarnings("rawtypes")
	public void testOverridenProviders(){
		Injector injA = Fluf.createInjector(new ModuleA());
		Injector injB = Fluf.createInjector(new ModuleB());
		// ModuleB extends ModuleA
		
		assertEquals(null, injA.get(new TypeLiteral<List<?>>(){})); // type is specific
		assertEquals("hero", injA.get(new TypeLiteral<List>(){}).iterator().next());
		// ModuleB overrides A and changes to more specific type that is why
		assertEquals(null, injB.get(new TypeLiteral<List>(){}));
		//but
		assertEquals(null, injA.get(new TypeLiteral<List<Integer>>(){}));
		assertEquals(new Integer(6), injB.get(new TypeLiteral<List<Integer>>(){}).iterator().next());
	}
}