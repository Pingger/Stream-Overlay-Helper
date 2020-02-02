package pingger.obsAutomation.monitors.mappings;

import java.awt.Color;
import java.awt.Panel;
import java.util.Set;

import pingger.obsAutomation.monitors.OverlayManager.State;

/**
 * Interface for Mappings
 *
 * @author Pingger
 *
 */
public interface Mapping
{
	/**
	 * this method should be fast as it might be called in fast succession.
	 *
	 * @return the Background {@link Color} for displaying this Mapping
	 */
	public Color getBackgroundColor();

	/**
	 * This method should start with the user defined Label!
	 * this method should be fast as it might be called in fast succession.
	 *
	 * @return the String to display String for this Mapping
	 */
	public String getDisplayString();

	/**
	 * Should return the Panel for Editing this Mapping. This Panel should be 128px
	 * to 256px wide. and at most 512px high. If more Space is needed, a Button
	 * should be contained in this Panel, that opens a new Window on its own. The
	 * Panel should be regareded as visible upon return.
	 *
	 * @return the Panel containing the Elements for Editing this Mapping.
	 */
	public Panel getEditPanel();

	/**
	 * @return the user-defined Label
	 */
	public String getLabel();

	/**
	 * @return the Sub Mappings, that should be evaluated, when this mapping matches
	 */
	public Set<Mapping> getSubMappings();

	/**
	 * @return the {@link State} to switch to when the {@link Mapping} matches
	 */
	public State getTargetState();

	/**
	 * this method should be fast as it might be called in fast succession.
	 *
	 * @return javax.swing Tooltip String (aka HTML formatted Tooltip)
	 */
	public String getTooltipString();

	/**
	 * This is called, when the Panel returned by {@link #getEditPanel()} is no
	 * longer visible on Screen
	 */
	public void hideEditPanel();

	/**
	 * Checks if this mapping matches at the current moment. Should be fast!
	 *
	 * @return <code>true</code> if it matches, <code>false</code> otherwise.
	 */
	public boolean matches();

	/**
	 * Should set the user defined Label
	 *
	 * @param label
	 *            the user defined Label to set
	 */
	public void setLabel(String label);

	/**
	 * @return the TypeID to identify the correct {@link MappingFactory}
	 */
	public String getTypeID();

}
