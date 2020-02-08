package info.iskariot.pingger.java.streamOverlayHelper.util;

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

	/**
	 * Cancatenates a number of strings with single spaces betweeen each other
	 * 
	 * @param strings
	 *            the String to concatenate
	 * @return the oncatenated Strings
	 */
	public static String concatStrings(String... strings)
	{
		if (strings == null || strings.length == 0)
		{ return ""; }
		StringBuilder sb = new StringBuilder(strings[0]);
		for (int i = 1; i < strings.length; i++)
		{
			sb.append(" " + strings[i]);
		}
		return sb.toString();
	}
}
