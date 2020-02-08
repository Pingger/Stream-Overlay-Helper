package info.iskariot.pingger.java.streamOverlayHelper.monitors.mappings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Panel;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import info.iskariot.pingger.java.streamOverlayHelper.monitors.Monitor;
import info.iskariot.pingger.java.streamOverlayHelper.monitors.OverlayManager;
import info.iskariot.pingger.java.streamOverlayHelper.monitors.OverlayManager.State;
import info.iskariot.pingger.java.streamOverlayHelper.monitors.PixelMonitor;
import info.iskariot.pingger.java.streamOverlayHelper.util.ColorSelector;

/**
 * A Pixel Mapping matches onto a single pixel on the Screen and checks if its
 * color matches the configured color. The color can be matched exact or loosly,
 * based on the accuracy selected.
 *
 * FIXME REWORK THIS CLASS
 *
 * @author Pingger
 */
public class PixelMapping implements Mapping
{
	/**
	 *
	 * @param s
	 *            the String-Representation of a PixelMapping
	 * @return the resulting {@link PixelMapping}
	 */
	public static PixelMapping loadFromString(String s)
	{
		String[] parts = s.split(",", 9);
		PixelMapping pm = new PixelMapping(0, 0, null);
		pm.label = parts[0];
		pm.state = State.valueOf(parts[1]);
		pm.x = Integer.parseInt(parts[2]);
		pm.y = Integer.parseInt(parts[3]);
		pm.color = new Color(Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6]));
		pm.accuracy = Integer.parseInt(parts[7]) / 255.0;
		ArrayList<String> subs = new ArrayList<>();
		int level = 0;
		int start = 0;
		int end = 0;
		for (int i = 0; i < parts[8].length(); i++)
		{
			char c = parts[8].charAt(i);
			if (c == '{')
			{
				if (level == 1)
				{
					start = i + 1;
				}
				level++;
			}
			else if (c == '}')
			{
				level--;
				if (level == 1)
				{
					end = i - 1;
				}
			}
			else if (level == 1 && c == ',' && end != 0)
			{
				subs.add(parts[8].substring(start, end));
				end = 0;
			}
		}
		Runnable r = () -> {
			for (String pms : subs)
			{
				pm.subMappings.add(loadFromString(pms));
			}
		};
		if (Thread.currentThread().getStackTrace().length > 200)
		{
			Thread t = new Thread(r);
			t.start();
			while (t.isAlive())
			{
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException ignore)
				{

				}
			}
		}
		else
		{
			r.run();
		}

		return pm;
	}

	/**
	 * Converts a PixelMapping recursively to a String represntation
	 *
	 * @param pm
	 *            the {@link PixelMapping} to store
	 * @return the resulting {@link String}
	 */
	public static String storeToString(PixelMapping pm)
	{
		StringBuilder sb = new StringBuilder();
		sb
				.append(
						pm.label + ","
								+ pm.state + ","
								+ pm.x + ","
								+ pm.y + ","
								+ pm.color.getRed() + ","
								+ pm.color.getGreen() + ","
								+ pm.color.getBlue() + ","
								+ (int) (pm.accuracy * 255) + ",{"
				);
		Runnable r = () -> {
			for (Mapping p : pm.subMappings)
			{
				if (p instanceof PixelMapping)
				{
					sb.append("{" + storeToString((PixelMapping) p) + "},");
				}
			}
		};
		if (Thread.currentThread().getStackTrace().length > 200)
		{
			Thread t = new Thread(r);
			t.start();
			while (t.isAlive())
			{
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException ignore)
				{

				}
			}
		}
		else
		{
			r.run();
		}
		sb.append("}");
		return sb.toString();
	}

	/** the color accurary */
	public double						accuracy;

	/** the color */
	public Color						color;
	/** The Label of this Mapping */
	public String						label					= "";
	/**
	 * the target state or null if no state change e.g. condition for other states
	 */
	public OverlayManager.State			state					= State.NO_CHANGE;
	/** the submappings */
	public final Set<Mapping>			subMappings;
	/** the x position of the pixel */
	public int							x;
	/** the y position of the pixel */
	public int							y;

	private transient ActionListener	al_refresh				= null;
	private transient java.awt.Window	colorSelector			= null;
	private transient ColorSelector		colorSelector2			= null;
	private transient Consumer<Boolean>	colorSelectorUpdater	= null;
	private transient JFrame			editGUI					= null;

	private transient Panel				editPanel				= null;

	private transient PixelDisplay		pd						= null;

	/**
	 * Creates an Empty Pixel Mapping at the given location and color with 100%
	 * color-accuracy
	 *
	 * @param x
	 *            the x Position of the Pixel
	 * @param y
	 *            the y Position of the Pixel
	 * @param color
	 *            the target Color
	 */
	public PixelMapping(int x, int y, Color color)
	{
		this(x, y, color, 0);
	}

	/**
	 * Creates an Empty Pixel Mapping at the given location, color and
	 * color-accuracy (0.0 = exact color (100% accurate), 1.0 = any color(0%
	 * accurate));
	 *
	 * @param x
	 *            the x Position of the Pixel
	 * @param y
	 *            the y Position of the Pixel
	 * @param color
	 *            the target Color
	 * @param accuracy
	 *            the color accuracy, 0.0 is exact, 1.0 is any color
	 */
	public PixelMapping(int x, int y, Color color, double accuracy)
	{
		this.x = x;
		this.y = y;
		this.color = color;
		this.accuracy = accuracy;
		subMappings = Collections.synchronizedSet(new HashSet<>());
	}

	@Override
	public Color getBackgroundColor()
	{
		return color;
	}

	@Override
	public String getDisplayString()
	{
		return (label.isBlank() ? "" : label + ": ") + x + ":" + y + ";" + (int) (accuracy * 255);
	}

	@Override
	public Panel getEditPanel()
	{
		if (editPanel == null)
		{
			editPanel = new Panel(new GridLayout(0, 1));
			JButton csb = new JButton("Select Color");
			csb.addActionListener(e -> {
				if (SwingUtilities.getWindowAncestor(editPanel) == null)
				{ return; }
				if (colorSelector2 == null)
				{
					colorSelector2 = new ColorSelector(c -> color = c, color, true, SwingUtilities.getWindowAncestor(editPanel));
				}
				colorSelector2.setLocation(MouseInfo.getPointerInfo().getLocation());
				colorSelector2.setVisible(true);
			});
			editPanel.add(csb);
		}
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
		return subMappings;
	}

	@Override
	public State getTargetState()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTooltipString()
	{
		return "<html><body>X: " + x + "<br>Y: " + y + "<br>Color: <span style=\"background-color:rgb("
				+ color.getRed() + "," + color.getGreen() + ","
				+ color.getBlue() + ");\">" + color.getRed() + "," + color.getGreen() + "," +
				color.getBlue() + "</span><br>Accuracy: " + (int) (accuracy * 255) + "</body></html>";
	}

	@Override
	public String getTypeID()
	{
		return "Pixel Mapping";
	}

	@Override
	public void hideEditPanel()
	{
		if (colorSelector2 != null)
		{
			colorSelector2.dispose();
			colorSelector2 = null;
		}
	}

	@Override
	public boolean matches()
	{
		// TODO implement
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

	/**
	 * Creates a new Edit GUI for this PixelMapping (once shown, the changeable
	 * setting doe nothing)
	 *
	 * @param changeable
	 *            if false, the location and color editing options are
	 *            hidden/disabled
	 */
	public synchronized void showEditGUI(boolean changeable)
	{
		if (editGUI == null)
		{
			JFrame frm = new JFrame("Edit - " + x + ":" + y);
			editGUI = frm;
			frm.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			Panel main = new Panel(new BorderLayout());
			Panel mainright = new Panel(new BorderLayout());
			Panel right = new Panel(new GridLayout(0, 1, 4, 4));
			Panel bottomright = new Panel(new GridLayout(3, 3));
			mainright.add(right, BorderLayout.NORTH);
			main.add(mainright, BorderLayout.EAST);

			/*
			 * right controls
			 */
			JButton add = new JButton("Add Mapping");
			add.setBackground(new Color(0, 255, 0, 128));
			JButton edit = new JButton("Edit Selected Mapping");
			edit.setBackground(new Color(255, 255, 0, 128));
			JButton remove = new JButton("Remove Selected Mapping");
			remove.setBackground(new Color(255, 0, 0, 128));
			JButton refresh = new JButton("Refresh View");
			JButton close = new JButton("Close");
			JTextArea notice = new JTextArea("Below shows/edits this Mapping, not the selected one on the left!");
			JComboBox<State> setAction = new JComboBox<>(State.values());
			JButton setColor = new JButton("Set Color (advanced)");
			JTextField setLabel = new JTextField(label);
			JButton bu = new JButton("UP");
			JButton bl = new JButton("LEFT");
			JButton fromMouseLocation = new JButton("point");
			JButton br = new JButton("RIGHT");
			JButton bd = new JButton("DOWN");
			JButton set = new JButton("set");

			pd = new PixelDisplay(x, y);
			setAction.setSelectedItem(state);
			setAction.setEditable(changeable);
			setColor.setEnabled(changeable);
			setLabel.setEnabled(changeable);
			notice.setWrapStyleWord(true);
			notice.setLineWrap(true);
			notice.setEditable(false);
			close.addActionListener(e -> {
				PixelMonitor.removeMonitor(pd);
				frm.setVisible(false);
			});

			/* RIGHT - TOP */
			right.add(add);
			right.add(edit);
			right.add(remove);
			right.add(refresh);
			right.add(close);
			right.add(notice);
			right.add(setLabel);
			right.add(setAction);
			right.add(setColor);

			/* RIGHT - MID */
			mainright.add(pd, BorderLayout.CENTER);

			/* RIGHT - BOTTOM */
			bottomright.add(new Panel());
			bottomright.add(bu);
			bottomright.add(new Panel());
			bottomright.add(bl);
			bottomright.add(fromMouseLocation);
			bottomright.add(br);
			bottomright.add(new Panel());
			bottomright.add(bd);
			bottomright.add(set);

			/* Should the controls for bottom be shown */
			if (changeable)
			{
				mainright.add(bottomright, BorderLayout.SOUTH);
			}
			bl.addActionListener(e -> pd.x--);
			br.addActionListener(e -> pd.x++);
			bu.addActionListener(e -> pd.y--);
			bd.addActionListener(e -> pd.y++);
			fromMouseLocation
					.addActionListener(e ->
					{
						// Wait 10 seconds, THEN take the location
						new Thread(() ->
						{
							try
							{
								Thread.sleep(10000);
								Point p = MouseInfo.getPointerInfo().getLocation();
								pd.x = p.x;
								pd.y = p.y;
							}
							catch (Exception ignore)
							{

							}
						}).start();
					});

			JList<PixelMapping> list = new JList<>(subMappings.toArray(new PixelMapping[0]));

			/* Refresh Button, because it depends on others */
			al_refresh = e -> {
				frm.setTitle("Edit - " + x + ":" + y);
				PixelMapping pm = list.getSelectedValue();
				list.setListData(subMappings.toArray(new PixelMapping[0]));
				list.setSelectedValue(pm, true);
				frm.revalidate();
			};
			refresh.addActionListener(al_refresh);

			/* Button actions, that depend on others */
			set.addActionListener(e -> {
				x = pd.x;
				y = pd.y;
				color = new Color(pd.bi.getRGB(8, 8));
				al_refresh.actionPerformed(null);
			});

			add.addActionListener(e -> {
				PixelMapping pm = new PixelMapping(0, 0, Color.PINK);
				subMappings.add(pm);
				pm.showEditGUI(true);
			});

			edit.addActionListener(e -> {
				list.getSelectedValue().showEditGUI(true);
			});

			remove.addActionListener(e -> {
				subMappings.remove(list.getSelectedValue());
				al_refresh.actionPerformed(e);
			});
			setAction.addItemListener(e -> {
				try
				{
					state = (State) e.getItem();
				}
				catch (Exception ex)
				{
					setAction.setSelectedItem(state);
				}
			});
			setColor.addActionListener(e -> showColorSelector());
			setLabel.setInputVerifier(new InputVerifier()
			{

				@Override
				public boolean verify(JComponent input)
				{
					if (setLabel.getText().contains(",")
							|| setLabel.getText().contains("{")
							|| setLabel.getText().contains("}"))
					{ return false; }
					return true;
				}
			});
			setLabel.getDocument().addDocumentListener(new DocumentListener()
			{

				@Override
				public void changedUpdate(DocumentEvent e)
				{
					if (setLabel.getInputVerifier().verify(setLabel))
					{
						label = setLabel.getText();
					}
					else
					{
						setLabel.setText(label);
					}
				}

				@Override
				public void insertUpdate(DocumentEvent e)
				{
					if (setLabel.getInputVerifier().verify(setLabel))
					{
						label = setLabel.getText();
					}
					else
					{
						setLabel.setText(label);
					}
				}

				@Override
				public void removeUpdate(DocumentEvent e)
				{
					if (setLabel.getInputVerifier().verify(setLabel))
					{
						label = setLabel.getText();
					}
					else
					{
						setLabel.setText(label);
					}
				}
			});
			list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			list.setLayoutOrientation(JList.VERTICAL);
			list.setMinimumSize(new Dimension(128, 0));
			list.setPreferredSize(new Dimension(128, 0));
			list.setCellRenderer((li, value, index, isSelected, cellHasFocus) -> {
				JPanel p = new JPanel();
				p.setPreferredSize(new Dimension(128, 32));
				p.setMinimumSize(new Dimension(128, 0));
				p.setBackground(value.getBackgroundColor());
				// FIXME
				p.setToolTipText(value.getTooltipString());
				JLabel l = new JLabel((isSelected ? "<<<" : "") + value.getDisplayString() + (isSelected ? ">>>" : ""), SwingConstants.LEFT);
				p.add(l);
				return p;
			});
			main.add(list, BorderLayout.CENTER);
			list.setVisibleRowCount(-1);
			frm.setContentPane(main);
			frm.pack();
			frm.setAlwaysOnTop(true);
			frm.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent we)
				{
					PixelMonitor.removeMonitor(pd);
					frm.setVisible(false);
				}
			});
		}
		al_refresh.actionPerformed(null);
		PixelMonitor.addMonitor(pd);
		if (!editGUI.isVisible())
		{
			new Thread(() -> {
				try
				{
					do
					{
						Thread.sleep(500);
						al_refresh.actionPerformed(null);
					}
					while (editGUI.isVisible());
				}
				catch (InterruptedException e1)
				{
				}
			}).start();
		}
		editGUI.setVisible(true);
		editGUI.setVisible(true);
	}

	@Override
	public String toString()
	{
		return storeToString(this);
	}

	private synchronized void showColorSelector()
	{
		if (colorSelector == null)
		{
			colorSelector = new Window(editGUI);
			colorSelector.addWindowFocusListener(new WindowFocusListener()
			{
				@Override
				public void windowGainedFocus(WindowEvent e)
				{
				}

				@Override
				public void windowLostFocus(WindowEvent e)
				{
					colorSelector.setVisible(false);
				}
			});
			Panel main = new Panel(new BorderLayout(8, 8));
			Panel p = new Panel(new GridLayout(0, 2));
			Panel p2 = new Panel(new GridLayout(0, 2));
			main.add(p, BorderLayout.NORTH);
			main.add(p2, BorderLayout.CENTER);
			colorSelector.add(main);
			JSlider jsl_red = new JSlider(0, 255, color.getRed());
			JSlider jsl_blue = new JSlider(0, 255, color.getBlue());
			JSlider jsl_green = new JSlider(0, 255, color.getGreen());
			JSlider jsl_accurarcy = new JSlider(0, 255, (int) (accuracy * 255));
			JLabel l_red = new JLabel();
			JLabel l_blue = new JLabel();
			JLabel l_green = new JLabel();
			JLabel l_accuracy = new JLabel();

			p.add(l_red);
			p.add(jsl_red);
			p.add(l_green);
			p.add(jsl_green);
			p.add(l_blue);
			p.add(jsl_blue);
			p.add(l_accuracy);
			p.add(jsl_accurarcy);
			class hexColorPane extends Panel
			{
				private static final long serialVersionUID = -2615270705312936986L;

				public hexColorPane()
				{
					setMinimumSize(new Dimension(256, 256));
					setPreferredSize(getMinimumSize());
				}

				@Override
				public void paint(Graphics g1)
				{
					int w = getWidth();
					int h = getHeight();
					super.paint(g1);
					Graphics2D g2 = (Graphics2D) g1;
					// mid values
					int r = color.getRed();
					int g = color.getGreen();
					int b = color.getBlue();
					// bottom values
					int rb = Math.max(0, (int) (r - accuracy * 255));
					int gb = Math.max(0, (int) (g - accuracy * 255));
					int bb = Math.max(0, (int) (b - accuracy * 255));
					// top values
					int rt = Math.min(255, (int) (r + accuracy * 255));
					int gt = Math.min(255, (int) (g + accuracy * 255));
					int bt = Math.min(255, (int) (b + accuracy * 255));
					// transparent
					Color tt = new Color(0, 0, 0, 0);
					// Hexagon
					Polygon hex = new Polygon();
					hex.addPoint(w / 2, 0);
					hex.addPoint(w, h / 3);
					hex.addPoint(w, 2 * h / 3);
					hex.addPoint(w / 2, h);
					hex.addPoint(0, 2 * h / 3);
					hex.addPoint(0, h / 3);
					hex.addPoint(w / 2, 0);

					g2.setColor(new Color(r, g, b));
					g2.fillPolygon(hex);

					g2.setPaint(new GradientPaint(0f, h / 3, new Color(rb, g, b, 255), w / 2, h / 2, tt, false));
					g2.fillPolygon(hex);
					g2.setPaint(new GradientPaint(0f, 2 * h / 3, new Color(r, gb, b, 255), w / 2, h / 2, tt, false));
					g2.fillPolygon(hex);
					g2.setPaint(new GradientPaint(w / 2, h / 2, tt, w / 2, h / 2, new Color(r, g, bb, 255), false));
					g2.fillPolygon(hex);
					g2.setPaint(new GradientPaint(w / 2, h / 2, tt, w, 2 * h / 3, new Color(rt, g, b, 255), false));
					g2.fillPolygon(hex);
					g2.setPaint(new GradientPaint(w, h / 3, new Color(r, gt, b, 255), w / 2, h / 2, tt, false));
					g2.fillPolygon(hex);
					g2.setPaint(new GradientPaint(w / 2, 0, new Color(r, g, bt, 255), w / 2, h / 2, tt, false));
					g2.fillPolygon(hex);
				}
			}
			class simpleColorPane extends Panel
			{
				private static final long serialVersionUID = -6725761828570015273L;

				public simpleColorPane()
				{
					setMinimumSize(new Dimension(256, 256));
					setPreferredSize(getMinimumSize());
				}

				@Override
				public void paint(Graphics g1)
				{
					int w = getWidth();
					int h = getHeight();
					super.paint(g1);
					Graphics2D g2 = (Graphics2D) g1;
					// mid values
					int r = color.getRed();
					int g = color.getGreen();
					int b = color.getBlue();
					// bottom values
					int rb = Math.max(0, (int) (r - accuracy * 255));
					int gb = Math.max(0, (int) (g - accuracy * 255));
					int bb = Math.max(0, (int) (b - accuracy * 255));
					// top values
					int rt = Math.min(255, (int) (r + accuracy * 255));
					int gt = Math.min(255, (int) (g + accuracy * 255));
					int bt = Math.min(255, (int) (b + accuracy * 255));
					// transparent
					Color tt = new Color(0, 0, 0, 0);
					// Hexagon

					g2.setColor(new Color(r, g, b));
					g2.fillRect(0, 0, w, h);

					g2.setPaint(new GradientPaint(0f, 0f, new Color(rb, g, b, 255), w / 2, 0f, tt, false));
					g2.fillRect(0, 0, w / 2, h / 4);
					g2.setPaint(new GradientPaint(0f, 0f, new Color(r, gb, b, 255), w / 2, 0f, tt, false));
					g2.fillRect(0, h / 4, w / 2, 2 * h / 4);
					g2.setPaint(new GradientPaint(0f, 0f, new Color(r, g, bb, 255), w / 2, 0f, tt, false));
					g2.fillRect(0, 2 * h / 4, w / 2, 3 * h / 4);
					g2.setPaint(new GradientPaint(w, 0f, new Color(rt, g, b, 255), w / 2, 0f, tt, false));
					g2.fillRect(w / 2, 0, w, h / 4);
					g2.setPaint(new GradientPaint(w, 0f, new Color(r, gt, b, 255), w / 2, 0f, tt, false));
					g2.fillRect(w / 2, h / 4, w, 2 * h / 4);
					g2.setPaint(new GradientPaint(w, 0f, new Color(r, g, bt, 255), w / 2, 0f, tt, false));
					g2.fillRect(w / 2, 2 * h / 4, w, 3 * h / 4);
					g2.setPaint(new GradientPaint(0f, 0f, new Color(rb, gb, bb, 255), w / 2, 0f, tt, false));
					g2.fillRect(0, 3 * h / 4, w / 2, h);
					g2.setPaint(new GradientPaint(w, 0f, new Color(rt, gt, bt, 255), w / 2, 0f, tt, false));
					g2.fillRect(w / 2, 3 * h / 4, w, h);
				}
			}
			hexColorPane hcp = new hexColorPane();
			simpleColorPane scp = new simpleColorPane();
			p2.add(hcp);
			p2.add(scp);
			AtomicBoolean ab = new AtomicBoolean(false);
			colorSelectorUpdater = a -> {
				if (ab.get())
				{ return; }
				ab.set(true);
				l_red.setText("Red: " + color.getRed());
				l_blue.setText("Blue: " + color.getBlue());
				l_green.setText("Green: " + color.getGreen());
				jsl_red.setValue(color.getRed());
				jsl_blue.setValue(color.getBlue());
				jsl_green.setValue(color.getGreen());
				jsl_accurarcy.setValue((int) (accuracy * 255));
				l_accuracy.setText("Accuracy (not alpha): " + jsl_accurarcy.getValue());
				hcp.repaint();
				scp.repaint();
				ab.set(false);
			};
			jsl_red.addChangeListener(e -> {
				color = new Color(jsl_red.getValue(), color.getGreen(), color.getBlue());
				colorSelectorUpdater.accept(true);
			});
			jsl_blue.addChangeListener(e -> {
				color = new Color(color.getRed(), color.getGreen(), jsl_blue.getValue());
				colorSelectorUpdater.accept(true);
			});
			jsl_green.addChangeListener(e -> {
				color = new Color(color.getRed(), jsl_green.getValue(), color.getBlue());
				colorSelectorUpdater.accept(true);
			});
			jsl_accurarcy.addChangeListener(e -> {
				accuracy = jsl_accurarcy.getValue() / 255.0;
				colorSelectorUpdater.accept(true);
			});

			colorSelector.pack();
		}
		colorSelectorUpdater.accept(false);
		Point p = MouseInfo.getPointerInfo().getLocation();
		colorSelector.setLocation(p.x, p.y);
		colorSelector.setVisible(true);
		colorSelector.setVisible(true); // get Focus
	}

	private static class PixelDisplay extends Panel implements Monitor
	{
		private static final long	serialVersionUID	= 9074953198580438950L;
		public int					x;
		public int					y;
		private BufferedImage		bi;

		public PixelDisplay(int x, int y)
		{
			this.x = x;
			this.y = y;
			setPreferredSize(new Dimension(256, 256));
			setMinimumSize(new Dimension(128, 128));
		}

		@Override
		public int getMouseMonitorInterval()
		{
			return 0;
		}

		@Override
		public int getPixelMonitorInterval()
		{
			return 200;
		}

		@Override
		public void onConnect()
		{

		}

		@Override
		public void onMouseMonitor(PointerInfo pi)
		{

		}

		@Override
		public void onPixelMonitor(Robot r)
		{
			synchronized (this)
			{
				Rectangle re = new Rectangle(x - 8, y - 8, 16, 16);
				bi = r.createScreenCapture(re);
			}
			repaint();
		}

		@Override
		public void paint(Graphics g)
		{
			super.paint(g);
			Polygon p = new Polygon();
			p.addPoint((int) (getWidth() / 16.0 * 8), (int) (getHeight() / 16.0 * 8));
			p.addPoint((int) (getWidth() / 16.0 * 9), (int) (getHeight() / 16.0 * 8));
			p.addPoint((int) (getWidth() / 16.0 * 9), (int) (getHeight() / 16.0 * 9));
			p.addPoint((int) (getWidth() / 16.0 * 8), (int) (getHeight() / 16.0 * 9));
			p.addPoint((int) (getWidth() / 16.0 * 8), (int) (getHeight() / 16.0 * 8));
			synchronized (this)
			{
				if (bi == null)
				{ return; }
				g.drawImage(bi, 0, 0, getWidth(), getHeight(), null);
				g.setColor(Color.red);
				g.drawPolygon(p);
			}
		}
	}
}
