package net.cacabish.module;

import net.cacabish.MusicXMLDocument;

/**
 * The base class for all modules that clean the MusicXML Document
 */
interface CleanerModule {

	/** The unformatted name of this module. */
	public String getModuleName();

	/** The HTML formatted name of this module. */
	public default String getFormattedModuleName() {
		return this.getModuleName();
	}

	/** The HTML formatted tooltip of this module. */
	public String getModuleTooltip();

	/**
	 * The main execution method that takes a MusicXML Document and cleans it.
	 * 
	 * @param musicXMLDocument the constructed MusicXML Document to clean
	 * @throws CleanerException if the cleaning was "unsuccessful" in any way
	 */
	public default void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		throw new CleanerException("Module not yet implemented");
	}

	/** Whether this module should run by default or needs to be enabled first */
	public default boolean isEnabledByDefault() {
		return true;
	}

}
