package pingger.obsAutomation.monitors;

import java.awt.PointerInfo;
import java.awt.Robot;

public interface Monitor
{

	public int getMouseMonitorInterval();

	public int getPixelMonitorInterval();

	public void onConnect();

	public void onMouseMonitor(PointerInfo pi);

	public void onPixelMonitor(Robot r);
}
