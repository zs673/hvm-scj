package javax.safetycritical;

public final class LaunchLevel1 extends Launcher {

	public LaunchLevel1(Safelet<?> app) {
		super(app, 1);
	}
	
	@Override
	void start() {
		startLevel1_2();
	}
}
