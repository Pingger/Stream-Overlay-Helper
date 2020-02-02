package info.iskariot.pingger.java.streamOverlayHelper.monitors.mappings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A Factory for {@link Mapping}s
 *
 * @author Pingger
 *
 */
public interface MappingFactory
{
	/**
	 * @return a newly created Mapping with default Values
	 */
	public Mapping createNewMapping();

	/**
	 * @return a General Name for the UI
	 */
	public String getGeneralName();

	/**
	 * @return the TypeID of the
	 */
	public String getTypeID();

	/**
	 * @param s
	 *            the String-Representation of this mapping (without its
	 *            submappings).
	 * @return the resulting Mapping (without its submappings)
	 */
	public Mapping loadMapping(String s);

	/**
	 * Should return a String representation of this Mapping, that can be restored
	 * using this {@link MappingFactory}
	 *
	 * The String MUST NOT contain 'curly brackets' aka 'braces' aka '{' and '}'.
	 * Those are used for creating a recursive mapping representation. Use
	 * {@link #toB64(String)} and {@link #fromB64(String)} if you are unsure.
	 *
	 * @param m
	 *            the Mapping to Store
	 * @return the String Representation of this {@link Mapping}, without its TypeID
	 *         and SubMappings
	 */
	public String storeToString(Mapping m);

	/**
	 * Converts a Base64 encoded String back to its original String
	 *
	 * @param s
	 *            the Base64 encoded UTF-8 String
	 * @return the original String
	 */
	default String fromB64(String s)
	{
		return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
	}

	/**
	 * Converts a String to a Base64 encoded UTF-8 String
	 *
	 * @param s
	 *            the String to encode
	 * @return the Base64 UTF-8 String
	 */
	default String toB64(String s)
	{
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}
}
