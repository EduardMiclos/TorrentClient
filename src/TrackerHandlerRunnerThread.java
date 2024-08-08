import java.io.IOException;

public class TrackerHandlerRunnerThread extends Thread {
	private TrackerHandler trackerHandler;
	
	public TrackerHandlerRunnerThread(TrackerHandler trackerHandler) {
		this.trackerHandler = trackerHandler;
	}
	
	@Override
	public void run() {
		try {
			trackerHandler.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
