package javax.safetycritical;

public class LaunchLevel2 extends Launcher {

	public LaunchLevel2(Safelet<?> app) {
		super(app, 2);
	}
	
	@Override
	protected void start() {
		startLevel1_2();
	}
}