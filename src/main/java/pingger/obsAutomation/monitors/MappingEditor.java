package pingger.obsAutomation.monitors;

import java.awt.Window;

/**
 * A generalized Mapping editor
 *
 * @author Pingger
 *
 */
public class MappingEditor
{
	private final Mapping m;

	public MappingEditor(Mapping m)
	{
		this.m = m;
	}

	private static class NewMappingDialog extends Window
	{

		NewMappingDialog()
		{
			super(null);
		}

	}
}
