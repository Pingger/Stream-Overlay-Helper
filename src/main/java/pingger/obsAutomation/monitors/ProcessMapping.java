package pingger.obsAutomation.monitors;

import java.awt.Color;
import java.awt.Panel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import pingger.obsAutomation.monitors.OverlayManager.State;

public class ProcessMapping implements Mapping
{
	private static long				lastUpdate	= 0;
	private static HashSet<String>	processes	= new HashSet<>();
	private String					executable	= "<no executable>";
	private String					label		= "";
	private final Set<Mapping>		subs		= Collections.synchronizedSet(new HashSet<>());
	private Color					userColor	= new Color(128, 0, 128);

	public ProcessMapping()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public Color getBackgroundColor()
	{
		return userColor;
	}

	@Override
	public String getDisplayString()
	{
		return (label.isBlank() ? "" : label + ": ") + executable;
	}

	@Override
	public Panel getEditPanel()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel()
	{
		return label;
	}

	@Override
	public Set<Mapping> getSubMappings()
	{
		return subs;
	}

	@Override
	public State getTargetState()
	{
		return State.NO_CHANGE;
	}

	@Override
	public String getTooltipString()
	{
		return "";
	}

	@Override
	public void hideEditPanel()
	{
		// TODO Auto-generated method stub

	}

	public boolean matches()
	{
		if (lastUpdate + 50 < System.currentTimeMillis())
		{
			processes.clear();
			ProcessHandle.allProcesses().forEach(ph -> ph.info().commandLine().ifPresent(cmd -> processes.add(cmd)));
		}
		if (executable.startsWith("::"))
		{
			try
			{
				Pattern p = java.util.regex.Pattern.compile(executable.substring(2));
				for (String s : processes)
				{
					if (p.matcher(s).find())
					{ return true; }
				}
			}
			catch (Exception exc)
			{
				exc.printStackTrace();
			}
		}
		else
		{
			for (String s : processes)
			{
				if (s.contains(executable))
				{ return true; }
			}
		}
		return false;
	}

	@Override
	public void setLabel(String label)
	{
		if (label == null)
		{
			label = "";
		}
		else
		{
			this.label = label;
		}
	}

}
