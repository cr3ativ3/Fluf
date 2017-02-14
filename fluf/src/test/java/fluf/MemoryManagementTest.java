package fluf;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

@SuppressWarnings("unused")
public class MemoryManagementTest { // extends TestCase { uncomment for JUnit to run these tests

	class MemoryModule extends Module {
		
		private byte[] toMakeModuleAllocateMoreMemory = new byte[50000000]; //50Mb
		
		@Bind
		public String provideBook(){
			return "Great Book";
		}
	}
	
	List<Object> injectorCollection = new ArrayList<Object>();
	long maxDuration = 5; //seconds
	
	public void testCreatingObjects() {
		// Scenario one
		long failedInMs = 0;
		long startTime = System.currentTimeMillis();
		long timeToStop = startTime + (maxDuration * 1000);
		try {
			while (System.currentTimeMillis() < timeToStop){
				Injector inj = Fluf.createInjector(new MemoryModule());
				
				// to retain reference to instance
				injectorCollection.add(inj);
				
				inj = null;
			}
			
			Assert.fail("Should run out of memory");
			
		} catch (OutOfMemoryError e){
			injectorCollection = null;
			failedInMs = System.currentTimeMillis() - startTime;
			//System.out.println("Scenario one ran out of memory in " + failedInMs + "ms");
		}
		// Scenario two
		try {
			timeToStop = System.currentTimeMillis() + (failedInMs * 3);
			while (System.currentTimeMillis() < timeToStop){
				
				Injector inj = Fluf.createInjector(new MemoryModule());
				
				// some action on the instance not to be "optimized"
				inj.equals(new String());
				
				// as long as you unreference the Injector everything should be GC'ed
				inj = null;
			}
			//System.out.println("Scenario two didn't in " + (failedInMs * 3) + "ms");
		} catch (OutOfMemoryError e){
			Assert.fail("Should not run out of memory");
		}
	}
}