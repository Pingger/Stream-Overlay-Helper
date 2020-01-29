package pingger.obsAutomation.monitors;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PixelMonitor implements Runnable
{
	private static HashMap<Monitor, Long>	last					= new HashMap<>();
	private static final Color				MainMenuColor			= new Color(29, 171, 174);

	private static final Color				MainMenuConfirmColor	= new Color(32, 57, 57);
	private static Set<Monitor>				mons					= Collections.synchronizedSet(new HashSet<>());

	public static void addMonitor(Monitor m)
	{
		if (m == null)
		{
			Thread.dumpStack();
			return;
		}
		mons.add(m);
	}

	public static void removeMonitor(Monitor m)
	{
		mons.remove(m);
	}

	public boolean		keepRunning		= true;

	private int			lastl			= 0;

	private File		overwatchFile	= new File("overwatch_scene.temp");

	private final Robot	r;

	public PixelMonitor() throws AWTException
	{
		r = new Robot();
	}

	@Override
	public void run()
	{
		try
		{
			while (keepRunning)
			{
				synchronized (mons)
				{
					for (Monitor m : mons)
					{
						if (last.containsKey(m))
						{
							if (last.get(m) + m.getPixelMonitorInterval() > System.currentTimeMillis())
							{
								continue;
							}
						}
						m.onPixelMonitor(r);
						last.put(m, System.currentTimeMillis());
					}
				}
				/*
				 * Point p = MouseInfo.getPointerInfo().getLocation();
				 * if (p == null)
				 * {
				 * Thread.sleep(250);
				 * continue;
				 * }
				 */
				Color c = r.getPixelColor(1671, 63);
				if (c.equals(Color.black))
				{
					// BlackScreen, do nothing
				}
				else if (c.equals(MainMenuColor))
				{
					// Main-Menu
					if (lastl != 1)
					{
						writeText(overwatchFile, "MainMenu");
					}
					lastl = 1;
				}
				else if (c.equals(MainMenuConfirmColor))
				{
					// Main-Menu
					if (lastl != 2)
					{
						writeText(overwatchFile, "MainMenu");
					}
					lastl = 2;
				}
				else
				{
					if (lastl != 0)
					{
						writeText(overwatchFile, "");
					}
					lastl = 0;
				}
				Thread.sleep(5);
			}
		}
		catch (Exception exc)
		{
			exc.printStackTrace();
		}
	}

	private void writeText(File f, String text)
	{
		try (PrintStream x = new PrintStream(f))
		{
			x.println(text);
			x.flush();
		}
		catch (Exception exc)
		{
			exc.printStackTrace();
		}
	}
}
