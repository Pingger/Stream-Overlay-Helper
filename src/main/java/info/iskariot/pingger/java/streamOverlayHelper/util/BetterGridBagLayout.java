package info.iskariot.pingger.java.streamOverlayHelper.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.LayoutManager2;
import java.util.Hashtable;

/**
 *
 * @author Pingger
 *
 */
public class BetterGridBagLayout implements LayoutManager2
{
	private Hashtable<Component, LayoutComponent> comps = new Hashtable<>();

	@Override
	public void addLayoutComponent(Component comp, Object constraints)
	{
		if (constraints == null || !(constraints instanceof GridBagConstraints))
		{ throw new IllegalArgumentException("You need specify GridBagConstrains for this Component for this Layout!"); }
		comps.put(comp, new LayoutComponent(comp, (GridBagConstraints) constraints));
	}

	@Override
	public void addLayoutComponent(String name, Component comp)
	{
		throw new RuntimeException("Method not implmented! Use addLayoutComponent(Component, Object)");
	}

	@Override
	public float getLayoutAlignmentX(Container target)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getLayoutAlignmentY(Container target)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void invalidateLayout(Container target)
	{
		comps.values().forEach(lc -> lc.invalidate());
	}

	@Override
	public void layoutContainer(Container parent)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Dimension maximumLayoutSize(Container target)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dimension minimumLayoutSize(Container parent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dimension preferredLayoutSize(Container parent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeLayoutComponent(Component comp)
	{
		comps.remove(comp);
	}

	private static class LayoutComponent
	{
		public final Component			c;
		public Dimension				computedMaximum		= new Dimension();
		public Dimension				computedMinimum		= new Dimension();
		public Dimension				computedPreferred	= new Dimension();
		public final GridBagConstraints	gbc;
		public Dimension				maximum				= new Dimension();
		public Dimension				minimum				= new Dimension();
		public Dimension				preferred			= new Dimension();

		public LayoutComponent(Component comp, GridBagConstraints gbcons)
		{
			c = comp;
			gbc = (GridBagConstraints) gbcons.clone();
		}

		public void invalidate()
		{
			computedMaximum = new Dimension();
			computedMinimum = new Dimension();
			computedPreferred = new Dimension();
			maximum = new Dimension();
			minimum = new Dimension();
			preferred = new Dimension();
		}
	}
}
