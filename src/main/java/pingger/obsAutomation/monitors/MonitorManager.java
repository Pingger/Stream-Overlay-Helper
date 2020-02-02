package pingger.obsAutomation.monitors;

import java.awt.Color;
import java.awt.Panel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import pingger.obsAutomation.monitors.OverlayManager.State;
import pingger.obsAutomation.monitors.mappings.Mapping;
import pingger.obsAutomation.monitors.mappings.MappingFactory;

/**
 * Manages {@link Monitor}s and {@link Mapping}s
 *
 * @author Pingger
 *
 */
public class MonitorManager
{
	/**
	 * Used to "load" and store Mapping of an Unknown Type, to preserve them instead
	 * of loosing them.
	 */
	protected static final MappingFactory<Mapping>								defaultMappingFactory	= new DefaultMappingFactory();
	/**
	 * MappingFactories by TypeID
	 */
	protected static final HashMap<String, MappingFactory<?>>					factories				= new HashMap<>();

	/**
	 * MappingFactories by {@link Mapping} type
	 */
	protected static final HashMap<Class<? extends Mapping>, MappingFactory<?>>	factories2				= new HashMap<>();

	/**
	 * Adds a new {@link MappingFactory}
	 *
	 * @param fac
	 *            the {@link MappingFactory} to add
	 */
	public static void addMappingFactory(MappingFactory<?> fac)
	{
		factories.put(fac.getTypeID(), fac);
		factories2.put(fac.createNewMapping().getClass(), fac);
	}

	/**
	 * Tries to parse the String to a Mapping with all its submappings
	 *
	 * @param stringRepresentation
	 *            of a single {@link Mapping} or a Mapping Tree (a Mapping with
	 *            Submappings, ...)
	 * @return the resulting Mapping
	 */
	public static Mapping loadMapping(String stringRepresentation)
	{
		// TODO implement
		return null;
	}

	/**
	 * Stores a Mapping with all its submappings
	 *
	 * @param m
	 *            the {@link Mapping} to store
	 * @return the resulting String
	 */
	public static String storeMapping(Mapping m)
	{
		StringBuilder sb = new StringBuilder();
		if (Thread.currentThread().getStackTrace().length > 200)
		{
			Thread t = new Thread(() -> sb.append(storeMapping(m)));
			t.start();
			while (t.isAlive())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ignore)
				{
				}
			}
		}
		else
		{
			// Mapping: Type:{Mapping}[{SubMapping}{SubMapping}...]
			MappingFactory<? extends Mapping> mf = factories2.getOrDefault(m.getClass(), defaultMappingFactory);
			if (mf != defaultMappingFactory)
			{
				sb.append(mf.toB64(mf.getTypeID()) + ":");
				sb.append("{" + mf.storeToString(m) + "}");
			}
			else
			{
				// The DefaultMapping Factory keeps the Type inside the storeToString!
				sb.append(mf.storeToString(m));
			}
			sb.append("[");
			for (Mapping sm : m.getSubMappings())
			{
				sb.append("{" + storeMapping(sm) + "}");
			}
			sb.append("]");

		}
		return sb.toString();
	}

	private static class DefaultMappingFactory implements MappingFactory<Mapping>
	{

		@Override
		public Mapping createNewMapping()
		{
			return null;
		}

		@Override
		public String getGeneralName()
		{
			return null;
		}

		@Override
		public String getTypeID()
		{
			return null;
		}

		@Override
		public Mapping loadMapping(String s)
		{
			UnknownMapping um = new UnknownMapping();
			um.data = s;
			return um;
		}

		@Override
		public String storeToString(Mapping m)
		{
			if (!(m instanceof UnknownMapping))
			{ throw new IllegalArgumentException("Can only store \"Unknown Mapping\"s"); }
			return ((UnknownMapping) m).data;
		}

		private static class UnknownMapping implements Mapping
		{
			public String					data		= "";
			private final HashSet<Mapping>	subMappings	= new HashSet<>();

			@Override
			public Color getBackgroundColor()
			{
				return new Color(255, 0, 0);
			}

			@Override
			public String getDisplayString()
			{
				return "UNKNOWN MAPPING TYPE";
			}

			@Override
			public Panel getEditPanel()
			{
				return new Panel();
			}

			@Override
			public String getLabel()
			{
				return "UNKNOWN MAPPING TYPE";
			}

			@Override
			public Set<Mapping> getSubMappings()
			{
				return subMappings;
			}

			@Override
			public State getTargetState()
			{
				return State.NO_CHANGE;
			}

			@Override
			public String getTooltipString()
			{
				return "This Mapping Type is unknown and is only loaded in compatiblity mode and CAN NOT BE EDITED!";
			}

			@Override
			public String getTypeID()
			{
				return null;
			}

			@Override
			public void hideEditPanel()
			{

			}

			@Override
			public boolean matches()
			{
				return false;
			}

			@Override
			public void setLabel(String label)
			{
			}
		}
	}
}
