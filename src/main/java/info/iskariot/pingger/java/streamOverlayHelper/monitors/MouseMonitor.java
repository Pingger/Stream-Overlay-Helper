package info.iskariot.pingger.java.streamOverlayHelper.monitors;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;

import info.iskariot.pingger.java.streamOverlayHelper.Main;

/**
 *
 * FIXME Rework to be more than just a proof of conecept.
 * IDEA Detect Screensaver?
 * 
 * @author Pingger
 *
 */
public class MouseMonitor implements Runnable
{
	public boolean	keepRunning		= true;
	public boolean	prevShouldShow	= false;
	public boolean	shouldShow		= false;
	public int		x				= 1920 / 2;
	public int		y				= 1080 / 2;

	@Override
	public void run()
	{
		try
		{
			long count = 0;
			double average = 0;
			long min = Integer.MAX_VALUE;
			long max = Integer.MIN_VALUE;
			while (keepRunning)
			{
				PointerInfo pi = MouseInfo.getPointerInfo();
				if (pi == null)
				{
					Thread.sleep(100);
					continue;
				}
				Main.pm.onMouseMonitor(pi);
				Point p = pi.getLocation();
				if (p == null)
				{
					Thread.sleep(250);
					continue;
				}
				int diff = Math.abs(p.x - x) + Math.abs(p.y - y);
				average = (average * count + diff) / ++count;
				min = Math.min(min, diff);
				max = Math.max(max, diff);
				if (count >= 25)
				{
					//System.out.println("Average: " + Math.round(average * 10) / 10.0 + "  of " + count);
					// System.out.println("Min: " + min + "   Max: " + max);
					prevShouldShow = shouldShow;
					boolean pr = shouldShow();
					shouldShow = min == 0 || shouldShow() && average < 25;
					if (min > 30)
					{
						prevShouldShow = false;
					}
					if (pr != shouldShow())
					{
						Main.main.broadcast("ow " + (shouldShow() ? "show" : "hide"));
					}
					count = 0;
					average = 0;
					min = Integer.MAX_VALUE;
					max = Integer.MIN_VALUE;
				}
				Thread.sleep(5);
			}
		}
		catch (InterruptedException iexc)
		{
			iexc.printStackTrace();
			System.exit(1);
		}
	}

	public boolean shouldShow()
	{
		return shouldShow || prevShouldShow;
	}
}
