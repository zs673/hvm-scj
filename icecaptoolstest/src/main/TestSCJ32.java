package main;


/*
 * To run the automated tests make sure that gcc is installed and can be 
 * started from a normal command prompt.
 * 
 * If you run cygwin, include the path to gcc in your path environment 
 * variable (usually C:\cygwin\bin). Test that it works by starting gcc from
 * a CMD prompt (not a cygwin prompt). After changing environment variables, it may
 * be necessary to restart eclipse.
 * 
 * Then select your gcc compile command from one of the predefined commands below 
 * (look at the beginning of the 'compileAndExecute' method
 * 
 */
public class TestSCJ32 extends TestSCJ {

	public static void main(String[] args) throws Throwable {
		new TestSCJ32().performTest();
	}

	/* (non-Javadoc)
	 * @see main.TestAll#getGCCCommand()
	 */
	@Override
	protected String getGCCCommand() {
		/* for 32 bit Linux */
		String gccCommand =
		"gcc -Wall -pedantic -Os -DPC32 -DPRINTFSUPPORT -DSUPPORTGC -DJAVA_HEAP_SIZE=10240000 classes.c  icecapvm.c  methodinterpreter.c  methods.c gc.c natives_allOS.c natives_i86.c rom_heap.c allocation_point.c rom_access.c native_scj.c print.c x86_32_interrupt.s -lpthread -lrt -lm -o ";
		return gccCommand;
	}
}
