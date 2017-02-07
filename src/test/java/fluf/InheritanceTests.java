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

import junit.framework.TestCase;

public class InheritanceTests extends TestCase {

	interface TestApi {
		int addition();
		int substraction();
	}
	
	public class ModuleA extends Module {
		
		@Bind
		public Integer provideInt(){
			return new Integer(10);
		}
		
		@Bind
		protected String provideStr(){
			return "2";
		}
		
		@Bind
		BigDecimal provideBD(){
			return new BigDecimal("1");
		}
	}
	
	public class ModuleB extends ModuleA {
		
		@Bind
		@Override
		public Integer provideInt(){
			return new Integer(3);
		}

		// API interface routes
		@Bind
		public int addition() {
			Injector inj = getInjector();
			Integer intNum = inj.get(Integer.class);
			String strNum = inj.get(String.class);
			BigDecimal bdNum = inj.get(BigDecimal.class);
			return intNum + new Integer(strNum) + bdNum.intValue();
		}

		@Bind
		public int substraction() {
			Injector inj = getInjector();
			Integer intNum = inj.get(Integer.class);
			String strNum = inj.get(String.class);
			BigDecimal bdNum = inj.get(BigDecimal.class);
			return intNum - new Integer(strNum) - bdNum.intValue();
		}
	}
	
	public void testInheritedProviders(){
		TestApi testApi = Fluf.createProxy(TestApi.class, new ModuleB());
		assertEquals(6, testApi.addition());
		assertEquals(0, testApi.substraction());
	}
}