package net.cacabish.module;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class AddPeriodsToVoltaTextsModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Add Periods to Ending Texts";
	}

	@Override
	public String getModuleTooltip() {
		return "<html>If checked, the program will add periods to the end of numbers in ending texts.</html>";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Adding periods after volta numbers...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// Fetch all the <ending> tags
		NodeList endingNodes = musicXMLDocument.getElementsByTagName("ending");
		for (int i = 0; i < endingNodes.getLength(); i++) {
			Element endingElement = (Element) endingNodes.item(i);

			// Fetch the number attribute
			String numberAttribute = endingElement.getAttribute("number");

			// Put this in a StringBuilder so it's easy to insert characters.
			StringBuilder builder = new StringBuilder(numberAttribute);

			// A flag to keep track of whether we have seen a digit or not.
			boolean hasSeenDigit = false;
			// Iterate over the string backwards so indices remain constant.
			for (int j = numberAttribute.length() - 1; j >= 0; j--) {
				char c = numberAttribute.charAt(j);
				if (Character.isDigit(c)) {
					// We are looking at a digit
					// If we haven't seen a digit whilst scanning rightwards, this must be the end
					// of the number so add a period.
					if (!hasSeenDigit) {
						// However, for forwards compatibility, if the character after is actually a
						// period, then don't add it.
						// To do this, we check so that if we're at the end of the string or it there
						// isn't a period after this character, then insert a period.
						if (j + 1 >= numberAttribute.length() || numberAttribute.charAt(j + 1) != '.') {
							builder.insert(j + 1, '.');
						}
					}

					// Update the flag because we have seen a digit.
					hasSeenDigit = true;
				} else {
					// This isn't a digit, so we have no longer seen a digit
					hasSeenDigit = false;
				}
			}

			// Set this as the <ending> tag's content.
			endingElement.setTextContent(builder.toString());
		}

		System.out.println("Successfully added periods after volta numbers!");

	}

}
