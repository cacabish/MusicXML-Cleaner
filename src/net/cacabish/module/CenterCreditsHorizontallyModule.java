package net.cacabish.module;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class CenterCreditsHorizontallyModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Center Credits Horizontally";
	}

	@Override
	public String getModuleTooltip() {
		return "<html>If checked, the program will horizontally center any credit, "
				+ "that is set as horizontally aligned, with respect to the repsective page's margins.</html>";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Centering all relevant text...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// Compute some commonly used values
		double[][] margins = musicXMLDocument.getPageMargins(); // REMINDER: left, right, top, bottom
		double pageWidth = musicXMLDocument.getPageWidth();

		if (margins == null || pageWidth == -1) {
			System.out.println("Missing either the margins or the page width. Aborting.");
			return; // We don't have what we need to do anything, so abort.
		}

		// Compute the "default-x" values for the centered credit tags
		double evenCenter = (pageWidth - margins[0][1] + margins[0][0]) / 2.0;
		double oddCenter = (pageWidth - margins[1][1] + margins[1][0]) / 2.0;

		// Find all the <credit> tags
		NodeList creditList = musicXMLDocument.getElementsByTagName("credit");

		for (int i = 0; i < creditList.getLength(); i++) {
			// Get the <credit> tag
			Element creditElement = (Element) creditList.item(i);

			int page;
			try {
				// Get the page number
				page = Integer.parseInt(creditElement.getAttribute("page"));
			} catch (NumberFormatException e) {
				// This tag does not contain a number, so skip this one
				continue;
			}
			boolean isEvenPage = (page % 2) == 0;

			// Check if there is a <credit-words> tag (there should be, and if there is,
			// there must be exactly one of them)
			NodeList creditWordsList = creditElement.getElementsByTagName("credit-words");
			if (creditWordsList.getLength() != 0) {
				Element creditWordsElement = (Element) creditWordsList.item(0);
				// Check if this credit needs to be centered.
				if (creditWordsElement.getAttribute("justify").equals("center")
						|| creditWordsElement.getAttribute("halign").equals("center")) {
					// Center it!
					creditWordsElement.setAttribute("default-x",
							String.format(Locale.US, "%.4f", isEvenPage ? evenCenter : oddCenter));
				}
			}
		}

		System.out.println("Done centering all relevant credits!");

	}

}
