package pingger.obsAutomation.monitors.mappings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Panel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pingger.obsAutomation.monitors.OverlayManager.State;
import pingger.obsAutomation.util.StringConverters;

/**
 * A {@link ProcessMapping} is a mapping, that matches on currently running
 * processes
 *
 * @author Pingger
 *
 */
public class ProcessMapping implements Mapping
{
	private static long				lastUpdate	= 0;
	private static HashSet<String>	processes	= new HashSet<>();

	private static void updateProcesses()
	{
		if (lastUpdate + 50 < System.currentTimeMillis())
		{
			processes.clear();
			ProcessHandle.allProcesses().forEach(ph -> ph.info().commandLine().ifPresent(cmd -> processes.add(cmd)));
		}
	}

	private transient JComboBox<String>	editCommandBox	= null;
	private transient JTextField		editLabel		= null;
	private transient Panel				editPanel		= null;
	private transient JComboBox<State>	editStateBox	= null;
	private String						executable		= "<no executable>";
	private String						label			= "";
	private final Set<Mapping>			subs			= Collections.synchronizedSet(new HashSet<>());
	private State						targetState		= State.NO_CHANGE;

	private Color						userColor		= new Color(128, 0, 128);

	/**
	 * Creates a new ProcessMapping
	 */
	public ProcessMapping()
	{
		// TODO anything to do here?
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
	public synchronized Panel getEditPanel()
	{
		if (editPanel == null)
		{
			Panel ep = new Panel(new GridLayout(0, 2, 8, 8));
			ep.setPreferredSize(new Dimension(256, 0));
			editLabel = new JTextField();
			editLabel.getDocument().addDocumentListener(new DocumentListener()
			{

				@Override
				public void changedUpdate(DocumentEvent e)
				{
					label = editLabel.getText();
				}

				@Override
				public void insertUpdate(DocumentEvent e)
				{
					label = editLabel.getText();
				}

				@Override
				public void removeUpdate(DocumentEvent e)
				{
					label = editLabel.getText();
				}
			});
			editCommandBox = new JComboBox<>();
			editCommandBox.setEditable(true);
			editCommandBox.setSelectedItem(executable);
			editCommandBox
					.setToolTipText(
							"This field can either contain a string, the will match case SENSITIVE or a regex. "
									+ "The string is interpreted as a regex, when it starts and ends with a '/'."
					);
			editCommandBox.addItemListener(e -> {
				if (!(e.getItem() instanceof String))
				{
					String v = (String) e.getItem();
					if (executable.startsWith("/") && executable.endsWith("/"))
					{
						try
						{
							Pattern.compile(executable.substring(1, executable.length() - 1));
							executable = v;
						}
						catch (Exception exc)
						{
							exc.printStackTrace();
						}
					}
					else
					{
						executable = v;
					}
				}
			});
			editStateBox = new JComboBox<>(State.values());
			editStateBox.addItemListener(e -> targetState = (State) editStateBox.getSelectedItem());
			editPanel.add(new JLabel("Label: "));
			editPanel.add(editLabel);
			editPanel.add(new JLabel("Command: "));
			editPanel.add(editCommandBox);
			editPanel.add(new JLabel("Target State: "));
			editPanel.add(editStateBox);
			editPanel = ep;
		}
		updateProcesses();
		editCommandBox.setModel(new DefaultComboBoxModel<>(processes.toArray(new String[0])));
		editCommandBox.setSelectedItem(executable);
		editStateBox.setSelectedItem(targetState);
		editLabel.setText(label);
		return editPanel;
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
		return targetState;
	}

	@Override
	public String getTooltipString()
	{
		return "";
	}

	@Override
	public String getTypeID()
	{
		return "Process Mapping";
	}

	@Override
	public void hideEditPanel()
	{
		// ignore
	}

	@Override
	public boolean matches()
	{
		updateProcesses();
		if (executable.startsWith("/") && executable.endsWith("/"))
		{
			try
			{
				Pattern p = Pattern.compile(executable.substring(1, executable.length() - 1));
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

	private static class ProcessMappingFactory implements MappingFactory<ProcessMapping>
	{

		@Override
		public Mapping createNewMapping()
		{
			return new ProcessMapping();
		}

		@Override
		public String getGeneralName()
		{
			return "Process Mapping";
		}

		@Override
		public String getTypeID()
		{
			return "ProcM";
		}

		@Override
		public ProcessMapping loadMapping(String s)
		{
			String t[] = s.split(",");
			ProcessMapping pm = new ProcessMapping();
			pm.label = fromB64(t[0]);
			pm.executable = fromB64(t[1]);
			pm.userColor = StringConverters.colorFromString(t[2]);
			pm.targetState = State.valueOf(t[3]);
			return pm;
		}

		@Override
		public String storeToString(Mapping um)
		{
			if (!(um instanceof ProcessMapping))
			{ throw new IllegalArgumentException("Bad Mapping Type!"); }
			ProcessMapping m = (ProcessMapping) um;
			return toB64(m.label) + "," + toB64(m.executable) + "," + m.userColor.getRGB() + "," + m.targetState.name();
		}
	}
}
