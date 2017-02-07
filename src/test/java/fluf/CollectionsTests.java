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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

public class CollectionsTests extends TestCase {

	interface TestApi {
		
		@Bind("apiIntro")
		StringBuilder intro();
		
		HashSet<String> all();
	}
	
	public class ModuleA extends Module {
		
		@Bind
		protected String provideTwo(){
			return "2";
		}
		
		@Bind
		String provideOne(){
			return "1";
		}
	}
	
	public class ModuleB extends ModuleA {
		
		@Bind
		public String provideTen(@Bind("provideOne") String one){
			return one + "0";
		}
		
		@Bind
		public Set<String> provideAll(Collection<String> strings){
			return (Set<String>) strings;
		}
		
		@Bind
		public StringBuilder provideStrBuilder(Set<String> strings){
			strings = new TreeSet<String>(strings);
			StringBuilder sb = new StringBuilder();
			for (String s : strings)
				sb.append(s);
			return sb;
		}

		// API interface routes
		@Bind
		public StringBuilder apiIntro() {
			return getInjector().get(StringBuilder.class, "provideStrBuilder");
		}
		
		@Bind
		public HashSet<String> all() {
			return (HashSet<String>) getInjector().get(new TypeLiteral<Set<String>>(){});
		}
	}
	
	public void testCollectionTreeProviders(){
		TestApi testApi = Fluf.createProxy(TestApi.class, new ModuleB());
		assertEquals("1102", testApi.intro().toString());
	}
	
	public void testDelegatingCollectionProviders(){
		TestApi testApi = Fluf.createProxy(TestApi.class, new ModuleB());
		assertTrue(testApi.all().size() == 3);
		assertTrue(testApi.all().contains("1"));
		assertTrue(testApi.all().contains("10"));
		assertTrue(testApi.all().contains("2"));
	}
}