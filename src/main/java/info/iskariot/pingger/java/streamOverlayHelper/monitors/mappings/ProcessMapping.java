package info.iskariot.pingger.java.streamOverlayHelper.monitors.mappings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import info.iskariot.pingger.java.streamOverlayHelper.monitors.OverlayManager.State;
import info.iskariot.pingger.java.streamOverlayHelper.util.ColorSelector;
import info.iskariot.pingger.java.streamOverlayHelper.util.StringConverters;

/**
 * A {@link ProcessMapping} is a mapping, that matches on currently running
 * processes
 *
 * @author Pingger
 *
 */
public class ProcessMapping implements Mapping
{
	/** The {@link MappingFactory} for ProcessMappings */
	public static final ProcessMappingFactory	FACTORY		= new ProcessMappingFactory();

	/** time since last getting all Processes from the OS */
	private static long							lastUpdate	= 0;
	/** The Processes that have been retrieved from the OS */
	private static final Set<String>			processes	= Collections.synchronizedSet(new HashSet<>());

	private static Component box(Component c, Dimension d)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setMaximumSize(d);
		c.setMaximumSize(d);
		p.add(c);

		return p;
	}

	private static Component box(Component c, int width)
	{
		return box(c, width, Integer.MAX_VALUE);
	}

	private static Component box(Component c, int width, int height)
	{
		return box(c, new Dimension(width, height));
	}

	/**
	 * Method to Update the Process List
	 */
	private static void updateProcesses()
	{
		if (lastUpdate + 50 < System.currentTimeMillis())
		{
			processes.clear();
			ProcessHandle
					.allProcesses()
					.forEach(
							ph -> ph
									.info()
									.command()
									.ifPresent(
											cmd -> processes
													.add(cmd + " " + StringConverters.concatStrings(ph.info().arguments().orElseGet(() -> new String[]
													{
															"[NO ARGS]"
													})))
									)
					);
			lastUpdate = System.currentTimeMillis();
		}
	}

	private transient ColorSelector		colorSelector	= null;
	/** The command or Regular Expression to match. */
	private String						command			= "<no executable>";
	/** The Box to edit the command variable */
	private transient JComboBox<String>	editCommandBox	= null;
	/** Text Field to edit the Label */
	private transient JTextField		editLabel		= null;
	/** The Panel containing the compononents for editing this mapping */
	private transient Panel				editPanel		= null;
	/** the Box to change the TargetState */
	private transient JComboBox<State>	editStateBox	= null;
	/** The user defined Label */
	private String						label			= "";
	/** The Sub Mappings */
	private final Set<Mapping>			subs			= Collections.synchronizedSet(new HashSet<>());

	/** The Target State when this Mapping matches */
	private State						targetState		= State.NO_CHANGE;

	/** The user defined Color of this Mapping */
	private Color						userColor		= new Color(128, 0, 128);

	@Override
	public Color getBackgroundColor()
	{
		return userColor;
	}

	@Override
	public String getDisplayString()
	{
		return (label.isBlank() ? "" : label + ": ") + command;
	}

	@Override
	public synchronized Panel getEditPanel()
	{
		if (editPanel == null)
		{
			Panel ep = new Panel(new GridBagLayout());
			GridBagConstraints gbs = new GridBagConstraints(
					0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0
			);
			//ep.setPreferredSize(new Dimension(256, -1));
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
			editCommandBox.setSelectedItem(command);
			editCommandBox
					.setToolTipText(
							"This field can either contain a string, the will match case SENSITIVE or a regex. "
									+ "The string is interpreted as a regex, when it starts and ends with a '/'."
					);
			editCommandBox.addItemListener(e -> {
				if (!(e.getItem() instanceof String))
				{
					String v = (String) e.getItem();
					if (command.startsWith("/") && command.endsWith("/"))
					{
						try
						{
							Pattern.compile(command.substring(1, command.length() - 1));
							command = v;
						}
						catch (Exception exc)
						{
							exc.printStackTrace();
						}
					}
					else
					{
						command = v;
					}
				}
			});
			//JPanel commandBoxBox = new JPanel();
			//commandBoxBox.setLayout(new BoxLayout(commandBoxBox, BoxLayout.X_AXIS));
			//editCommandBox.setMaximumSize(new Dimension(192, 64));
			//	commandBoxBox.add(editCommandBox);
			JButton openColorSelector = new JButton("Change Color");
			openColorSelector.addActionListener(e -> {
				if (colorSelector == null)
				{
					colorSelector = new ColorSelector(c -> userColor = c, userColor, false, SwingUtilities.getWindowAncestor(openColorSelector));
				}
				colorSelector.setLocation(openColorSelector.getLocationOnScreen());
				colorSelector.setAlwaysOnTop(true);
				colorSelector.setVisible(true);
				colorSelector.setVisible(true); // Also take focus
			});
			editStateBox = new JComboBox<>(State.values());
			editStateBox.addItemListener(e -> targetState = (State) editStateBox.getSelectedItem());
			ep.add(new JLabel("Label: "), gbs);
			gbs.gridx = 1;
			ep.add(box(editLabel, 192), gbs);
			gbs.gridy++;
			gbs.gridx = 0;
			ep.add(new JLabel("Command: "), gbs);
			gbs.gridx = 1;
			ep.add(box(editCommandBox, 192), gbs);
			gbs.gridy++;
			gbs.gridx = 0;
			ep.add(new JLabel("Target State: "), gbs);
			gbs.gridx = 1;
			ep.add(box(editStateBox, 192), gbs);
			gbs.gridy++;
			gbs.gridx = 0;
			ep.add(new JLabel(" "), gbs);
			gbs.gridx = 1;
			ep.add(box(openColorSelector, 192), gbs);
			editPanel = ep;
		}
		updateProcesses();
		editCommandBox.setModel(new DefaultComboBoxModel<>(processes.toArray(new String[0])));
		editCommandBox.setSelectedItem(command);
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
		if (colorSelector != null)
		{
			colorSelector.dispose();
			colorSelector = null;
		}
	}

	@Override
	public boolean matches()
	{
		updateProcesses();
		if (command.startsWith("/") && command.endsWith("/"))
		{
			try
			{
				Pattern p = Pattern.compile(command.substring(1, command.length() - 1));
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
				if (s.contains(command))
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

	private static class ProcessMappingFactory implements MappingFactory
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
			pm.command = fromB64(t[1]);
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
			return toB64(m.label) + "," + toB64(m.command) + "," + m.userColor.getRGB() + "," + m.targetState.name();
		}
	}
}
