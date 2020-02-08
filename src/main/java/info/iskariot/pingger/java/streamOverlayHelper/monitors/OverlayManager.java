package info.iskariot.pingger.java.streamOverlayHelper.monitors;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import info.iskariot.pingger.java.streamOverlayHelper.Main;
import info.iskariot.pingger.java.streamOverlayHelper.monitors.mappings.MappingEditor;
import info.iskariot.pingger.java.streamOverlayHelper.monitors.mappings.PixelMapping;
import info.iskariot.pingger.java.streamOverlayHelper.util.ColorSelector;

/**
 * FIXME rework this class to be a bit more than a Proof of Concept!
 *
 * @author Pingger
 *
 */
public class OverlayManager implements Monitor
{
	/**
	 * currently used for debugging the {@link ColorSelector} in the
	 * {@link PixelMapping}
	 * FIXME -&gt; solve this issue
	 */
	public static JFrame		frm					= null;
	private JButton				btn_editPM;
	private JButton				btn_faded;
	private JButton				btn_loadPM;
	private JButton				btn_savePM;
	private Map<State, JButton>	buttons				= new HashMap<>();
	private int					cursorUnderOverlay	= 0;
	private boolean				faded				= false;
	private boolean				fadeLocked			= false;
	private PixelMapping		mapping				= new PixelMapping(0, 0, Color.black);
	private State				state				= State.TOP_RIGHT;
	private boolean				stateLocked			= false;

	/**
	 * Creates a new {@link OverlayManager}
	 */
	public OverlayManager()
	{
		if (frm != null)
		{ throw new IllegalStateException("Only one OverlayManager is allowed!"); }
		frm = new JFrame("Player.me Overlay");
		JPanel pan = new JPanel(new GridLayout(0, 2));
		frm.setContentPane(pan);
		for (State s : State.values())
		{
			JButton jb = new JButton(s.name());
			jb.addActionListener((e) -> setButtonState(s));
			buttons.put(s, jb);
		}
		pan.add(buttons.get(State.TOP_LEFT));
		pan.add(buttons.get(State.TOP_RIGHT));
		pan.add(buttons.get(State.BOTTOM_LEFT));
		pan.add(buttons.get(State.BOTTOM_RIGHT));
		btn_faded = new JButton("FADE");
		btn_faded.addActionListener(e -> {
			if (faded)
			{
				faded = false;
				fadeLocked = true;
				notify("layout unfade");
			}
			else
			{
				if (fadeLocked)
				{
					fadeLocked = false;
				}
				else
				{
					faded = true;
					notify("layout fade");
					fadeLocked = true;
				}
			}
			updateButtons();
		});
		pan.add(btn_faded);
		pan.add(buttons.get(State.HIDDEN));
		btn_savePM = new JButton("SAVE");
		btn_savePM.addActionListener(e -> {
			String r = PixelMapping.storeToString(mapping);
			try (PrintStream p = new PrintStream(new File("PixelMapping.cfg"), StandardCharsets.UTF_8))
			{
				p.println(r);
				p.flush();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		});
		btn_loadPM = new JButton("LOAD");
		btn_loadPM.addActionListener(e -> {
			try (
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("PixelMapping.cfg")), (int) Math.max(new File("PixelMapping.cfg").length(), 1024 * 1024))
			)
			{
				mapping = PixelMapping.loadFromString(br.readLine());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
		});
		btn_editPM = new JButton("Edit Mappings");
		btn_editPM.addActionListener(e -> new MappingEditor(mapping));
		pan.add(btn_savePM);
		pan.add(btn_loadPM);
		pan.add(btn_editPM);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.setAlwaysOnTop(true);
		frm.pack();
		updateButtons();
		frm.setVisible(true);
	}

	@Override
	public int getMouseMonitorInterval()
	{
		return 10;
	}

	@Override
	public int getPixelMonitorInterval()
	{
		return 100;
	}

	@Override
	public void onConnect()
	{
		Main.main.broadcast("layout " + state.cmd);
		Main.main.broadcast("layout " + (faded ? "fade" : "unfade"));
	}

	@Override
	public void onMouseMonitor(PointerInfo pi)
	{
		if (fadeLocked)
		{ return; }
		cursorUnderOverlay = Math.max(0, Math.min(state.area.contains(pi.getLocation()) ? cursorUnderOverlay + 3 : cursorUnderOverlay - 10, 200));
		if (!faded && cursorUnderOverlay > 10)
		{
			faded = true;
			notify("layout fade");
		}
		else if (faded && cursorUnderOverlay < 10)
		{
			faded = false;
			notify("layout unfade");
		}
	}

	@Override
	public void onPixelMonitor(Robot r)
	{

	}

	private void notify(String msg)
	{
		System.out.println("Sending: " + msg);
		Main.main.broadcast(msg);
	}

	private void setButtonState(State state)
	{
		if (stateLocked && this.state == state)
		{
			stateLocked = false;
		}
		else
		{
			this.state = state;
			notify("layout " + state.cmd);
			stateLocked = true;
		}
		updateButtons();
	}

	private void updateButtons()
	{
		buttons.forEach((s, b) -> {
			if (state == s)
			{
				b.setBackground(new Color(220, 255, 220));
				b.setText(stateLocked ? "[" + s.name() + "]" : s.name());
			}
			else
			{
				b.setBackground(Color.LIGHT_GRAY);
				b.setText(s.name());
			}
		});
		btn_faded.setBackground(faded ? new Color(255, 220, 220) : new Color(220, 255, 220));
		btn_faded.setText((fadeLocked ? "<" : "") + (faded ? "UNFADE" : "FADE") + (fadeLocked ? ">" : ""));
		if (fadeLocked && !faded)
		{
			btn_faded.setText("UNLOCK FADE");
		}
	}

	/**
	 * The possible Overlay states
	 *
	 * @author Pingger
	 *
	 */
	public enum State
	{
		/**
		 * The Overlay is at the bottom left
		 */
		BOTTOM_LEFT(8, 1080 - 450 - 8, 459, 450, "bl"),
		/**
		 * The Overlay is at the bottom right
		 */
		BOTTOM_RIGHT(1920 - 459 - 8, 1080 - 450 - 8, 459, 450, "br"),
		/**
		 * The Overlay is hidden (moved just of view on the right side of the screen)
		 */
		HIDDEN(1920, 8, 459, 450, "hide"),
		/**
		 * Special State, used in Mappings to prevent any change (e.g. when more than
		 * one Mapping needs to match to actually cause a State transition.
		 */
		NO_CHANGE(0, 0, 0, 0, "unchanged"),
		/**
		 * The Overlay is positioned at the top left.
		 */
		TOP_LEFT(8, 240, 459, 450, "left"),
		/**
		 * The Overlay is positioned at the top right. (usually the default)
		 */
		TOP_RIGHT(1920 - 459 - 8, 240, 459, 450, "right");

		/**
		 * The area occupied by Overlay in this State
		 */
		public final Rectangle	area;
		/**
		 * The command to send to the connected WebSockets to perform the State
		 * transition
		 */
		public final String		cmd;

		State(int x, int y, int w, int h, String cmd)
		{
			area = new Rectangle(x, y, w, h);
			this.cmd = cmd;
		}
	}
}
