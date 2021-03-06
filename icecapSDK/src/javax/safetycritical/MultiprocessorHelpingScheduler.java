package javax.safetycritical;

import thread.JavaLangThreadScheduler;
import vm.Monitor;

class MultiprocessorHelpingScheduler extends JavaLangThreadScheduler{
	
	@Override
	public Monitor getDefaultMonitor() {
		return null;
	}
	
	protected static Monitor getMultiprocessorMonitor(int ceiling){
		return getSCJMultiprocessorMonitor(ceiling);
	}

}
