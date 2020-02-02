package info.iskariot.pingger.java.streamOverlayHelper.monitors;

import java.awt.PointerInfo;
import java.awt.Robot;

/**
 * FIXME still necessary after rework?
 * 
 * @author Pingger
 *
 */
public interface Monitor
{

	public int getMouseMonitorInterval();

	public int getPixelMonitorInterval();

	public void onConnect();

	public void onMouseMonitor(PointerInfo pi);

	public void onPixelMonitor(Robot r);
}
