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

import java.math.BigDecimal;
import java.util.Collection;

import junit.framework.TestCase;

public class BasicTests extends TestCase {

	interface TestApi {
		int addition(int value);
		int substraction();
		void assert2Chars();
		<T> void assertAllChars();
	}
	
	public class TestModule extends Module implements TestApi { //can implement if method names match
		
		@Bind
		public Integer provideInt(){
			return new Integer(3);
		}
		
		@Bind
		protected String provideStr(){
			return "2";
		}
		
		@Bind
		BigDecimal provideBD(){
			return new BigDecimal("1");
		}

		// API interface routes
		@Bind
		@Override
		public int addition(int value) {
			Injector inj = getInjector();
			Integer intNum = inj.get(Integer.class);
			String strNum = inj.get(String.class);
			BigDecimal bdNum = inj.get(BigDecimal.class);
			return value + intNum + new Integer(strNum) + bdNum.intValue();
		}

		@Bind
		@Override
		public int substraction() {
			Injector inj = getInjector();
			Integer intNum = inj.get(Integer.class);
			String strNum = inj.get(String.class);
			BigDecimal bdNum = inj.get(BigDecimal.class);
			return intNum - new Integer(strNum) - bdNum.intValue();
		}
		
		@Bind
		Character provideA(){
			return 'A';
		}
		
		@Bind("givesB")
		Character provideB(){
			return 'B';
		}
		
		@Bind
		Collection<Character> provideAllChars(Collection<Character> chars){
			return chars;
		}
		
		@Bind
		Character[] provide2Chars(@Bind("provideA") Character a, @Bind("givesB") Character b){
			Character[] chars = new Character[2];
			chars[0] = a;
			chars[1] = b;
			return chars;
		}

		
		@Bind
		@Override
		public void assert2Chars() {
			Character[] chars = getInjector().get(Character[].class);
			assertNotNull(chars);
		}

		@Bind
		@Override
		public void assertAllChars() {
			Collection<Character> chars = getInjector().getAll(new TypeLiteral<Character>(){});
			assertNotNull(chars);
		}
	}
	
	public void testBasicProxy(){
		TestApi testApi = Fluf.createProxy(TestApi.class, new TestModule());
		assertEquals(7, testApi.addition(1));
		assertEquals(0, testApi.substraction());
	}
	
	public void testBasicInjector(){
		Injector injector = Fluf.createInjector(new TestModule());
		assertTrue(BigDecimal.ONE.equals(injector.get(BigDecimal.class)));
		assertEquals("2", injector.get(String.class));
		assertEquals(new Integer("3"), injector.get(Integer.class));
	}
	
	public void test2CharInjector(){
		TestApi testApi = Fluf.createProxy(TestApi.class, new TestModule());
		testApi.assert2Chars();
	}
	
	public void testAllCharInjector(){
		TestApi testApi = Fluf.createProxy(TestApi.class, new TestModule());
		testApi.assertAllChars();
	}
}