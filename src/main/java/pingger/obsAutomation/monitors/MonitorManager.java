package pingger.obsAutomation.monitors;

import java.awt.Color;
import java.awt.Panel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
	protected static final MappingFactory										defaultMappingFactory	= new DefaultMappingFactory();
	/**
	 * MappingFactories by TypeID
	 */
	protected static final HashMap<String, MappingFactory>						factories				= new HashMap<>();

	/**
	 * MappingFactories by {@link Mapping} type
	 */
	protected static final HashMap<Class<? extends Mapping>, MappingFactory>	factories2				= new HashMap<>();

	/**
	 * Convenience Method for {@link #addMappingFactory(String, MappingFactory)},
	 * with paramters (fac.getTypeID(),fac);
	 *
	 * @param fac
	 *            the {@link MappingFactory} to add
	 */
	public static void addMappingFactory(MappingFactory fac)
	{
		addMappingFactory(fac.getTypeID(), fac);
	}

	/**
	 * Adds a new {@link MappingFactory}. The Type is determined by the first Object
	 * returned using the {@link MappingFactory#createNewMapping()} Method.
	 * Multiple TypeIDs can have the same Factory. But only the first registered
	 * Factory for a given Type ({@link Mapping} implementation) is used.<br>
	 * <br>
	 * This can be used to upgrade old Mappings to a new Version using their TypeIDs
	 *
	 * @param TypeID
	 *            the TypeID to register this MappingFactory to
	 *
	 * @param fac
	 *            the {@link MappingFactory} to add
	 */
	public static void addMappingFactory(String TypeID, MappingFactory fac)
	{
		if (factories.containsKey(TypeID))
		{ throw new IllegalArgumentException("Another Factory with this TypeID is already registered!"); }
		factories.put(TypeID, fac);
		Class<? extends Mapping> clazz = fac.createNewMapping().getClass();
		if (!factories2.containsKey(clazz))
		{
			factories2.put(clazz, fac);
		}
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
		// IDEA: Solve heavy array cloning using a Scanner or Stream?
		// stringRepresentation: {Type:{Mapping}[{Type:{Mapping}[...]}{SubMapping}...]}
		String local = stringRepresentation;
		LinkedList<Mapping> recursion = new LinkedList<>();
		Mapping parent = null; // Should be root Mapping after while-loop
		while (!local.isBlank())
		{
			// End of SubMapping array, one recursion level up
			if (local.startsWith("]"))
			{
				parent = recursion.removeLast();
				local = local.substring(1);
			}
			// Decoration between mappings: remove (NOTE: decoration exists, for easier manual config edits, as in open file with notepad)
			if (local.startsWith("}"))
			{
				local = local.substring(1);
				continue;
			}
			if (local.startsWith("{"))
			{
				local = local.substring(1);
				continue;
			}
			String[] split = local.split(":", 2); // get TypeID
			String type = defaultMappingFactory.fromB64(split[0]);
			local = split[1];
			split = local.split("}", 2); // get End of Mapping Data
			String data = split[0].substring(1);
			local = split[1];

			MappingFactory mf = factories.getOrDefault(type, defaultMappingFactory);
			Mapping m = mf == defaultMappingFactory ? mf.loadMapping(mf.toB64(type) + ":{" + data + "}") : mf.loadMapping(data);

			// if not the root Mapping: add this Mapping as a submapping of the next higher.
			if (!recursion.isEmpty())
			{
				recursion.getLast().getSubMappings().add(m);
			}
			// Start of SubMappings. Prepare this mapping as direct parent.
			if (local.startsWith("["))
			{
				recursion.addLast(m);
				local = local.substring(1);
			}
		}
		return parent;
	}

	/**
	 * Stores a Mapping with all its submappings
	 *
	 * @param mapping
	 *            the {@link Mapping} to store
	 * @return the resulting String
	 */
	public static String storeMapping(Mapping mapping)
	{
		StringBuilder sb = new StringBuilder();
		LinkedList<Set<Mapping>> recursion = new LinkedList<>();
		HashSet<Mapping> start = new HashSet<>();
		start.add(mapping);
		recursion.add(start);
		while (!recursion.isEmpty())
		{
			Set<Mapping> s = recursion.getLast();
			// Mapping: {Type:{Mapping}[{Type:{Mapping}[...]}{SubMapping}...]}
			if (s.isEmpty())
			{
				recursion.removeLast();
				sb.append("]");
				continue;
			}
			Mapping m = s.stream().findAny().get();
			s.remove(m);
			MappingFactory mf = factories2.getOrDefault(m.getClass(), defaultMappingFactory);
			if (mf == defaultMappingFactory)
			{
				// Unknown Mappings are stored specially to preserve all their information
				sb.append("{" + mf.storeToString(m) + "}");
			}
			else
			{
				sb.append("{" + mf.toB64(mf.getTypeID()) + ":{" + mf.storeToString(m) + "}");
			}
			sb.append("[");
			recursion.addLast(new HashSet<>(m.getSubMappings()));
		}

		/*
		 * OLD recursive Code, that needed a hacky workaround to prevent StackOverflows
		 * if (Thread.currentThread().getStackTrace().length > 200)
		 * {
		 * Thread t = new Thread(() -> sb.append(storeMapping(m)));
		 * t.start();
		 * while (t.isAlive())
		 * {
		 * try
		 * {
		 * Thread.sleep(1);
		 * }
		 * catch (InterruptedException ignore)
		 * {
		 * }
		 * }
		 * }
		 * else
		 * {
		 * // Mapping: Type:{Mapping}[{SubMapping}{SubMapping}...]
		 * MappingFactory<? extends Mapping> mf = factories2.getOrDefault(m.getClass(),
		 * defaultMappingFactory);
		 * if (mf != defaultMappingFactory)
		 * {
		 * sb.append(mf.toB64(mf.getTypeID()) + ":");
		 * sb.append("{" + mf.storeToString(m) + "}");
		 * }
		 * else
		 * {
		 * // The DefaultMapping Factory keeps the Type inside the storeToString!
		 * sb.append(mf.storeToString(m));
		 * }
		 * sb.append("[");
		 * for (Mapping sm : m.getSubMappings())
		 * {
		 * sb.append("{" + storeMapping(sm) + "}");
		 * }
		 * sb.append("]");
		 *
		 * }
		 */
		return sb.toString();
	}

	private static class DefaultMappingFactory implements MappingFactory
	{

		@Override
		public Mapping createNewMapping()
		{
			return new UnknownMapping();
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
