package pingger.obsAutomation.monitors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
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
	private static final int	GAPS				= 8;
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

	private Panel						east		= new Panel(new BorderLayout(GAPS, GAPS));
	private Panel						eastTop		= new Panel(new GridLayout(0, 1, GAPS, GAPS));
	private final Panel					editPanel	= new Panel(new BorderLayout());
	private final WindowListener		listener	= new WindowListener();
	private Mapping						m;
	private Panel						main		= new Panel(new BorderLayout(GAPS, GAPS));
	private final LinkedList<Mapping>	parents		= new LinkedList<>();
	private final JList<Mapping>		subs		= new JList<>();

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
		main.add(subs, BorderLayout.CENTER);
		main.add(east, BorderLayout.EAST);
		east.add(eastTop, BorderLayout.NORTH);
		eastTop.add(createButton("Edit Parent", e -> editParent(), new Color(255, 255, 128, 255)));
		eastTop.add(createButton("Add Mapping", e -> addSubMapping(), new Color(128, 255, 128, 255)));
		eastTop.add(createButton("Edit Mapping", e -> editSubMapping(), new Color(255, 255, 128, 255)));
		eastTop.add(createButton("Remove Mapping", e -> removeSubMapping(), new Color(255, 128, 128, 255)));
		eastTop.add(createButton("Refresh UI", e -> updateUI(), new Color(128, 128, 255, 255)));
		eastTop.add(createButton("Close", e -> listener.windowClosing(null), new Color(64, 64, 64, 255)));

		east.add(editPanel, BorderLayout.SOUTH);

		subs.setCellRenderer((jl, ma, i, sel, foc) -> {
			JLabel lbl = new JLabel((sel ? ">>>" : "") + ma.getDisplayString(), SwingConstants.LEFT);
			lbl.setBackground(ma.getBackgroundColor());
			lbl.setToolTipText(ma.getTooltipString());
			return lbl;
		});
	}

	private void updateUI()
	{
		setTitle("Mapping Editor - " + m.getDisplayString());
		Mapping selected = subs.getSelectedValue();
		Mapping[] listData = m.getSubMappings().toArray(new Mapping[0]);
		System.out.println(m.getSubMappings().size());
		Arrays.sort(listData, (o1, o2) -> o1.getDisplayString().compareTo(o2.getDisplayString()));
		System.out.println(Arrays.toString(listData));
		subs.setListData(listData);
		subs.setSelectedValue(selected, true);
		editPanel.removeAll();
		Panel ep = m.getEditPanel();
		if (ep != null)
		{
			editPanel.add(m.getEditPanel(), BorderLayout.CENTER);
		}
		else
		{
			m.hideEditPanel();
		}
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
