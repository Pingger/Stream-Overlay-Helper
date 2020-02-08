package info.iskariot.pingger.java.streamOverlayHelper.monitors.mappings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * A generalized Mapping editor
 *
 * @author Pingger
 *
 */
public class MappingEditor extends JFrame
{
	private static final int	GAPS				= 4;
	private static final long	serialVersionUID	= -7638999405661658059L;

	private static JButton createButton(String label, ActionListener al, Color backgroundColor)
	{
		JButton btn = new JButton(label);
		btn.addActionListener(al);
		if (backgroundColor != null)
		{
			btn.setBackground(backgroundColor);
		}
		return btn;
	}

	private final Panel					editPanel		= new Panel(new BorderLayout());
	private final WindowListener		listener		= new WindowListener();
	private HashMap<Mapping, JPanel>	listLabelPanels	= new HashMap<>();
	private HashMap<Mapping, JLabel>	listLabels		= new HashMap<>();
	private Mapping						m;
	private JPanel						main			= new JPanel(new GridBagLayout(), true);
	private final LinkedList<Mapping>	parents			= new LinkedList<>();

	private final JList<Mapping>		subs			= new JList<>();

	/**
	 * @param m
	 *            the root-Mapping to Edit
	 */
	public MappingEditor(Mapping m)
	{
		this.m = m;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(listener);
		setupUI();
		updateUI();
		pack();
		setSize(800, 600);
		setVisible(true);
	}

	private void addSubMapping()
	{
		NewMappingDialog nmp = new NewMappingDialog();
		nmp.setLocation(MouseInfo.getPointerInfo().getLocation());
		nmp.setVisible(true);
		nmp.setVisible(true);
	}

	private synchronized void addSubMapping(Mapping ma)
	{
		m.getSubMappings().add(ma);
		updateUI();
	}

	private synchronized void editParent()
	{
		if (!parents.isEmpty())
		{
			m = parents.removeLast();
			updateUI();
		}
	}

	private synchronized void editSubMapping()
	{
		if (subs.getSelectedValue() != null)
		{
			parents.addLast(m);
			m = subs.getSelectedValue();
			updateUI();
		}
	}

	private synchronized void removeSubMapping()
	{
		if (subs.getSelectedValue() != null)
		{
			m.getSubMappings().remove(subs.getSelectedValue());
			updateUI();
		}
	}

	private void setupUI()
	{
		setContentPane(main);
		GridBagConstraints gbs = new GridBagConstraints();
		gbs.weightx = 0;
		gbs.weighty = 0;
		gbs.insets = new Insets(GAPS, GAPS, GAPS, GAPS);
		gbs.fill = GridBagConstraints.BOTH;
		gbs.gridy = 0;
		gbs.gridwidth = 1;
		gbs.gridheight = 1;
		gbs.gridx = 0;
		main.add(createButton("Edit Parent", e -> editParent(), new Color(192, 192, 192, 255)), gbs);
		gbs.gridx = 3;
		main.add(createButton("Close", e -> listener.windowClosing(null), new Color(192, 192, 192, 255)), gbs);
		gbs.gridx = 0;
		gbs.gridy++;
		gbs.weightx = 0;
		main.add(createButton("Add Mapping", e -> addSubMapping(), new Color(128, 255, 128, 255)), gbs);
		gbs.weightx = 0.5;
		gbs.gridx++;
		main.add(createButton("Edit Mapping", e -> editSubMapping(), new Color(255, 255, 128, 255)), gbs);
		gbs.weightx = 0;
		gbs.gridx++;
		main.add(createButton("Remove Mapping", e -> removeSubMapping(), new Color(255, 128, 128, 255)), gbs);
		gbs.gridx++;
		main.add(createButton("Refresh UI", e -> updateUI(), new Color(128, 128, 255, 255)), gbs);
		gbs.gridx = 0;
		gbs.gridy++;
		gbs.gridwidth = 3;
		gbs.weightx = 0.75;
		gbs.weighty = 0.9;
		main.add(subs, gbs);
		gbs.weightx = 0.25;
		gbs.gridx += 3;
		gbs.gridwidth = 1;
		//		JPanel editPanelBox = new JPanel();
		//		editPanelBox.setLayout(new BoxLayout(editPanelBox, BoxLayout.Y_AXIS));
		//		editPanel.setMaximumSize(new Dimension(256, Integer.MAX_VALUE));
		//		editPanelBox.setMaximumSize(new Dimension(256, Integer.MAX_VALUE));
		//		editPanelBox.add(editPanel);
		main.add(editPanel, gbs);
		subs.setCellRenderer((jl, ma, i, sel, foc) -> {
			if (!listLabelPanels.containsKey(ma))
			{
				JPanel pan = new JPanel(new BorderLayout(), true);
				JLabel lbl = new JLabel("", SwingConstants.LEFT);
				pan.add(lbl, BorderLayout.CENTER);
				listLabelPanels.put(ma, pan);
				listLabels.put(ma, lbl);
			}
			JLabel l = listLabels.get(ma);
			JPanel p = listLabelPanels.get(ma);
			l.setText((sel ? "  " : "") + ma.getDisplayString() + (sel ? " <<< " : ""));
			p.setBackground(ma.getBackgroundColor());
			l.setToolTipText(ma.getTooltipString());
			return l;
		});
	}

	private void updateUI()
	{
		setTitle("Mapping Editor - " + m.getDisplayString());
		Mapping selected = subs.getSelectedValue();
		Mapping[] listData = m.getSubMappings().toArray(new Mapping[0]);
		Arrays.sort(listData, (o1, o2) -> o1.getDisplayString().compareTo(o2.getDisplayString()));
		subs.setListData(listData);
		subs.setSelectedValue(selected, true);
		editPanel.removeAll();
		Panel ep = m.getEditPanel();
		if (ep != null)
		{
			editPanel.add(m.getEditPanel(), BorderLayout.NORTH);
		}
		else
		{
			m.hideEditPanel();
		}
		main.revalidate();
	}

	/**
	 *
	 * @author Pingger
	 *
	 */
	private class NewMappingDialog extends Window implements WindowFocusListener
	{
		private static final long serialVersionUID = 5168368485986829042L;

		public NewMappingDialog()
		{
			super(MappingEditor.this);
			setLayout(new GridLayout(0, 1));
			add(createButton("Pixel Mapping", e -> addSubMapping(new PixelMapping(0, 0, new Color(255, 0, 255))), Color.BLACK));
			add(createButton("Process Mapping", e -> addSubMapping(new ProcessMapping()), Color.BLACK));
			add(createButton("Cancel", e -> windowLostFocus(null), Color.BLACK));
			setAlwaysOnTop(true);
			pack();
			addWindowFocusListener(this);
		}

		@Override
		public void windowGainedFocus(WindowEvent e)
		{
			// ignore
		}

		@Override
		public void windowLostFocus(WindowEvent e)
		{
			setVisible(false);
			dispose();
		}
	}

	private class WindowListener extends WindowAdapter
	{
		@Override
		public void windowClosing(WindowEvent e)
		{
			m.hideEditPanel();
			parents.clear();
			dispose();
		}
	}
}
