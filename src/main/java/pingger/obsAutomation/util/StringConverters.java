package pingger.obsAutomation.util;

import java.awt.Color;

/**
 * Class for general conversion methods of Objects to Strings and back.
 *
 * @author Pingger
 *
 */
public class StringConverters
{
	/**
	 * Converts a "colorString" to a {@link Color}. A colorString is just
	 * 
	 * @param colorString
	 *            the rgba integer as a numerical string
	 * @return the resulting {@link Color}
	 */
	public static Color colorFromString(String colorString)
	{
		return new Color(Integer.parseInt(colorString));
	}

	/**
	 * @param c
	 *            the {@link Color} to convert
	 * @return the {@link Color#getRGB()} as {@link String}
	 */
	public static String colorToString(Color c)
	{
		return "" + c.getRGB();
	}
}
