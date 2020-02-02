package pingger.obsAutomation.monitors.mappings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JTextArea;

import pingger.obsAutomation.monitors.OverlayManager.State;

/**
 * Used for keeping unknown Mappings and not loose them, like other Programs do.
 *
 * @author Pingger
 *
 */
public class DefaultMappingFactory implements MappingFactory
{

	@Override
	public Mapping createNewMapping()
	{
		return new UnknownMapping("UnknownMapping:{}");
	}

	@Override
	public String getGeneralName()
	{
		return "Unknown Mapping Container";
	}

	@Override
	public String getTypeID()
	{
		return null;
	}

	@Override
	public Mapping loadMapping(String s)
	{
		UnknownMapping um = new UnknownMapping(s);
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
		public final String				data;

		private transient Panel			editPanel	= null;

		private final HashSet<Mapping>	subMappings	= new HashSet<>();

		public UnknownMapping(String d)
		{
			data = d;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{ return true; }
			if (obj == null)
			{ return false; }
			if (getClass() != obj.getClass())
			{ return false; }
			UnknownMapping other = (UnknownMapping) obj;
			if (data == null)
			{
				if (other.data != null)
				{ return false; }
			}
			else if (!data.equals(other.data))
			{ return false; }
			if (subMappings == null)
			{
				if (other.subMappings != null)
				{ return false; }
			}
			else if (!subMappings.equals(other.subMappings))
			{ return false; }
			return true;
		}

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
			if (editPanel == null)
			{
				editPanel = new Panel(new BorderLayout());
				JTextArea notice = new JTextArea(
						"This Mapping could not be loaded, as its Type is unknown. To prevent "
								+ "breaking it, you can't edit it. You may delete it or edit its "
								+ "Submappings (as long as those could be loaded properly). This "
								+ "mapping will not match while it is not loaded properly, but "
								+ "will be saved properly (the way it was found), so another "
								+ "Version can still work with this Mapping. \r\n"
								+ "\r\n"
								+ "The cause for this is probably, that you are using an older "
								+ "Version of this Application or this Mapping is so old, that "
								+ "it can no longer be loaded. In the latter case you can try "
								+ "upgrading step by step."
				);
				notice.setWrapStyleWord(true);
				notice.setLineWrap(true);
				notice.setEditable(false);
				editPanel.add(notice, BorderLayout.CENTER);
			}
			return editPanel;
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
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (data == null ? 0 : data.hashCode());
			result = prime * result + (subMappings == null ? 0 : subMappings.hashCode());
			return result;
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
