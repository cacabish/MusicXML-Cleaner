package net.cacabish.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import net.cacabish.MusicXMLDocument;

/**
 * The main controller class that interfaces between the view and cleaning
 * modules.
 */
public final class CleanerModuleController {

	/** A list containing each cleaning module. */
	private ArrayList<CleanerModule> allModules = new ArrayList<>();

	/** A set containing any cleaning module that has been disabled. */
	private HashSet<CleanerModule> disabledModules = new HashSet<>();

	/** Storing the last MusicXML Document for saving. */
	private MusicXMLDocument lastDocument = null;

	public CleanerModuleController() {
		// Add all the modules so we only create one copy of them.
		allModules.add(new AddMiniTitlesAndPageNumbersModule());
		allModules.add(new AddSystemMeasureNumbersModule());
		allModules.add(new RemoveDuplicateCopyrightInfoModule());
		allModules.add(new CorrectTempoMarkModule());
		allModules.add(new CenterCreditsHorizontallyModule());
		allModules.add(new OffsetSystemMarginsToAlignWithLeftMarginModule());
		allModules.add(new MakeRepeatTextsBoldModule());
		allModules.add(new AddPeriodsToVoltaTextsModule());
		allModules.add(new AddSwing8thsWhereSwingDirectionModule());
		allModules.add(new ReplaceEdwinAndFreeSerifWithTimesNewRomanModule());
		allModules.add(new FormatOssias());

		// Disable any modules by default
		for (CleanerModule module : allModules) {
			if (!module.isEnabledByDefault()) {
				disabledModules.add(module);
			}
		}
	}

	/** Returns a list of all available modules to the view. */
	public Iterable<CleanerModuleInfo> getAllModuleInfo() {
		ArrayList<CleanerModuleInfo> moduleInfo = new ArrayList<>();

		for (int i = 0; i < allModules.size(); i++) {
			CleanerModule module = allModules.get(i);
			moduleInfo.add(new CleanerModuleInfo(i, module.getModuleName(), module.getFormattedModuleName(),
					module.getModuleTooltip(), !disabledModules.contains(module)));
		}

		return moduleInfo;
	}

	/** Same as ArrayList.get, but with controlled error handling. */
	private CleanerModule getModuleFromIndex(int index) {
		if (index < 0 || index >= allModules.size()) {
			throw new IndexOutOfBoundsException("There is no module with index " + index);
		}
		return allModules.get(index);
	}

	/**
	 * Enables a cleaning module, given its index
	 * 
	 * @param moduleIndex the index of the module to enable
	 * @return true if the module was successfully enabled, false otherwise
	 */
	public boolean enableModule(int moduleIndex) {
		try {
			disabledModules.remove(getModuleFromIndex(moduleIndex));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Disable a cleaning module, given its index
	 * 
	 * @param moduleIndex the index of the module to disable
	 * @return true if the module was successfully disabled, false otherwise
	 */
	public boolean disableModule(int moduleIndex) {
		try {
			disabledModules.add(getModuleFromIndex(moduleIndex));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Takes the file and cleans it! Will only run enabled modules.
	 * 
	 * @param file the file to process
	 * @return an itemized list of the modules' successes/failures
	 */
	public List<CleaningResult> cleanFile(File file) throws ParserConfigurationException, SAXException, IOException {
		lastDocument = new MusicXMLDocument(file);

		List<CleaningResult> results = new ArrayList<>();

		for (CleanerModule module : allModules) {
			if (!disabledModules.contains(module)) {
				try {
					module.execute(lastDocument);
					results.add(new CleaningResult(module.getModuleName(), true,
							module.getModuleName() + " ran successfully."));
				} catch (CleanerException e) {
					results.add(new CleaningResult(module.getModuleName(), false, e.getMessage()));
				}
			} else {
				results.add(
						new CleaningResult(module.getModuleName(), true, module.getModuleName() + " was disabled."));
			}
		}

		return results;
	}

	/**
	 * Saves the last cleaned document. If there is no last cleaned document, this
	 * method errors.
	 * 
	 * @param saveDestination
	 */
	public void saveLastCleanedDocumentToFile(File saveDestination) throws IOException, TransformerException {
		if (lastDocument == null) {
			throw new IllegalStateException("There is no MusicXML document to save");
		}

		lastDocument.saveToFile(saveDestination);
	}

	/**
	 * A simple class that houses information about the various cleaner modules for
	 * the view.
	 */
	public final class CleanerModuleInfo {

		/**
		 * The internal index that the view uses to talk to the controller about a
		 * specific module.
		 */
		public final int moduleIndex;
		/** The unformatted name of the module. */
		public final String moduleName;
		/** The formatted name of the module. Fit for HTML text. */
		public final String moduleFormattedName;
		/** The formatted tooltip of the module. Fit for HTML text. */
		public final String moduleTooltip;
		/** Whether this module is currently enabled or not. */
		public final boolean isEnabled;

		public CleanerModuleInfo(int moduleIndex, String moduleName, String moduleFormattedName, String moduleTooltip,
				boolean isEnabled) {
			this.moduleIndex = moduleIndex;
			this.moduleName = moduleName;
			this.moduleFormattedName = moduleFormattedName;
			this.moduleTooltip = moduleTooltip;
			this.isEnabled = isEnabled;
		}

	}

	/**
	 * A simple class that houses results about the last cleaning operation for the
	 * view.
	 */
	public final class CleaningResult {

		/** The unformatted name of the module. */
		public final String moduleName;
		/** Whether the cleaning was a success or not. */
		public final boolean success;
		/** Either a success or error message. Can be null. */
		public final String message;

		public CleaningResult(String moduleName, boolean success, String errorMessage) {
			this.moduleName = moduleName;
			this.success = success;
			this.message = errorMessage;
		}

	}

}
