package net.cacabish.module;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class AddSystemMeasureNumbersModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Add System Measure Numbers";
	}

	@Override
	public String getModuleTooltip() {
		return "If checked, the program will add the tag that creates system measure numbers.";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Adding measure numbers...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		NodeList printList = musicXMLDocument.getElementsByTagName("print");
		if (printList.getLength() == 0) {
			System.out.println("No print tag detected. Aborting.");
			return; // We've got nothing to work with, so abort.
		}

		// Get the first <print> tag
		Element firstPrint = (Element) printList.item(0);

		// Check to see if the <measure-numbering> tag exists
		NodeList checkList = firstPrint.getElementsByTagName("measure-numbering");
		if (checkList.getLength() == 1) {
			// Ensure the measure-numbering is "system"
			checkList.item(0).setTextContent("system");

			// We're done.
			System.out.println("Measure numbers already exist.");
			return;
		}

		// Otherwise, the tag doesn't exist so we need to add it.
		Element measureNumberingTag = musicXMLDocument.createElement("measure-numbering");
		measureNumberingTag.setTextContent("system");

		// The XML sequence order must be preserved, so we need to determine where the
		// tag goes.
		// First, check if <part-name-display> tag exists.
		NodeList successorList = firstPrint.getElementsByTagName("part-name-display");
		if (successorList.getLength() == 0) {
			// <part-name-display> doesn't exist. Check if <part-abbreviation-display>
			// exists.
			successorList = firstPrint.getElementsByTagName("part-abbreviation-display");
			if (successorList.getLength() == 0) {
				// <part-abbreviation-display> doesn't exist either.
				// At this point, it is safe to append <measure-numbering> tag to the end as no
				// others exist
				firstPrint.appendChild(measureNumberingTag);

				// We've added it, so we're done.
				System.out.println("Done adding measure numbers!");
				return;
			}
		}

		// At this point, either <part-name-display> exists or <part-name-display>
		// doesn't exist but <part-abbreviation-display> does exist.
		// Either way, the tag is stored in the NodeList and so simply add the
		// <measure-numbering> tag before these tags
		firstPrint.insertBefore(measureNumberingTag, successorList.item(0));
		System.out.println("Done adding measure numbers!");
	}

}
