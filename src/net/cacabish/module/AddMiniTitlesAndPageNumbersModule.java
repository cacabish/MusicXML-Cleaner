package net.cacabish.module;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class AddMiniTitlesAndPageNumbersModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Add Mini-titles & Page Numbers";
	}

	@Override
	public String getModuleTooltip() {
		return "If checked, the program will add mini-title and page numbers to pages 2+.";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Adding page numbers and mini-titles...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// Fetch the necessary values
		int numberOfPages = musicXMLDocument.getNumberOfPages();
		double[][] margins = musicXMLDocument.getPageMargins(); // REMINDER: left, right, top, bottom
		double pageWidth = musicXMLDocument.getPageWidth();
		double pageHeight = musicXMLDocument.getPageHeight();
		String title = musicXMLDocument.getTitle();

		// Check to proceed
		if (margins == null || pageWidth == -1 || pageHeight == -1 || title == null || numberOfPages == -1) {
			System.out.println("Missing one or more pieces of information, aborting.");
			return; // We're missing some critical piece of information, so abort.
		}
		if (numberOfPages == 1) {
			return; // We don't need to do anything to a 1 page sheet, so return early
		}

		// Compute the "default-y" values for the upcoming tags
		double evenVertical = pageHeight - margins[0][2];
		double oddVertical = pageHeight - margins[1][2];

		// Compute the "default-x" values for the mini title tags
		double evenCenter = (pageWidth - margins[0][1] + margins[0][0]) / 2.0;
		double oddCenter = (pageWidth - margins[1][1] + margins[1][0]) / 2.0;

		// Compute the "default-x" values for the page number tags
		double evenHorizontal = margins[0][0];
		double oddHorizontal = pageWidth - margins[1][1];

		// Detect if page number and/or mini titles exist.
		// For both of these arrays, the index corresponds to the (page number - 1) and
		// is true if and only if the respective credit was found
		// Because we don't check credits on the first page, both of the indices at 0
		// are always false and are to be ignored.
		boolean[] pageNumberExists = new boolean[numberOfPages];
		boolean[] miniTitleExists = new boolean[numberOfPages];

		// Fetch all the credits
		NodeList creditList = musicXMLDocument.getElementsByTagName("credit");
		for (int i = 0; i < creditList.getLength(); i++) {
			Element creditTag = (Element) creditList.item(i);
			String pageAttribute = creditTag.getAttribute("page");
			if (pageAttribute.isEmpty() || pageAttribute.equals("1")) {
				// Page numbers and mini titles exist on pages 2+.
				// This means it is on page 1. Ignore.
				continue;
			}

			// Convert the attribute to an integer
			int pageNumber;
			try {
				pageNumber = Integer.parseInt(pageAttribute);
			} catch (NumberFormatException e) {
				// The attribute didn't contain a numerical value. Ignore.
				continue;
			}
			boolean isEvenPage = (pageNumber % 2) == 0;

			// Fetch the <credit-words> child tag
			NodeList creditWordsList = creditTag.getElementsByTagName("credit-words");
			if (creditWordsList.getLength() != 1) {
				// There is no <credit-words> tag, ignore.
				continue;
			}

			// Get the text content and check if it is a page number, a mini title, or
			// neither
			Element candidateTag = (Element) creditWordsList.item(0);
			String content = candidateTag.getTextContent().trim();

			if (content.equals(pageAttribute)) {
				// This is a page number. Ensure the formatting attributes.
				candidateTag.setAttribute("default-x",
						String.format(Locale.US, "%.4f", isEvenPage ? evenHorizontal : oddHorizontal));
				candidateTag.setAttribute("default-y",
						String.format(Locale.US, "%.4f", isEvenPage ? evenVertical : oddVertical));
				candidateTag.setAttribute("justify", isEvenPage ? "left" : "right");
				candidateTag.setAttribute("valign", "top");
				candidateTag.setAttribute("font-size", "14");

				// We've found a page number for this page, mark it as such.
				pageNumberExists[pageNumber - 1] = true;

			} else if (content.equals(title)) {
				// This is a mini title. Ensure the formatting attributes.
				candidateTag.setAttribute("default-x",
						String.format(Locale.US, "%.4f", isEvenPage ? evenCenter : oddCenter));
				candidateTag.setAttribute("default-y",
						String.format(Locale.US, "%.4f", isEvenPage ? evenVertical : oddVertical));
				candidateTag.setAttribute("justify", "center");
				candidateTag.setAttribute("valign", "top");
				candidateTag.setAttribute("font-size", "12");

				// We've found a page number for this page, mark it as such.
				miniTitleExists[pageNumber - 1] = true;
			} else {
				// This is neither. Ignore.
				continue;
			}
		}

		// At this point, proceed to add all page numbers and mini titles that don't
		// exist.
		// Find the <part-list> tag (which must exist according to the schema).
		// We will add the new credits before the <part-list> tag.
		NodeList partList = musicXMLDocument.getElementsByTagName("part-list");

		if (partList.getLength() == 0) {
			// Since this is required, something bad happened. Abort.
			return;
		}

		Element partListTag = (Element) partList.item(0);

		// Iterate over the second page and onward
		for (int pageNumber = 2; pageNumber <= numberOfPages; pageNumber++) {
			// Commonly used values
			String pageNumberText = Integer.toString(pageNumber);
			boolean isEvenPage = (pageNumber % 2) == 0;

			/*
			 * Add the page number to the top of the page
			 */
			{
				// Check if we need to add a page number
				if (pageNumberExists[pageNumber - 1]) {
					// There already exists a page number of this page and we've already handled it.
					// No need to add a new one.
					continue;
				}

				// Create the parent <credit> tag
				Element pageNumberCreditTag = musicXMLDocument.createElement("credit");
				pageNumberCreditTag.setAttribute("page", pageNumberText); // Set the page number of the credit

				// Add the (optional) <credit-type> child
				Element pageNumberCreditType = musicXMLDocument.createElement("credit-type");
				pageNumberCreditType.setTextContent("page number");
				pageNumberCreditTag.appendChild(pageNumberCreditType);

				// Add the mandatory <credit-words> child
				Element pageNumberCreditWords = musicXMLDocument.createElement("credit-words");
				pageNumberCreditWords.setTextContent(pageNumberText); // Set the text equal to the page number

				// Set all the attributes
				pageNumberCreditWords.setAttribute("default-x",
						String.format(Locale.US, "%.4f", isEvenPage ? evenHorizontal : oddHorizontal));
				pageNumberCreditWords.setAttribute("default-y",
						String.format(Locale.US, "%.4f", isEvenPage ? evenVertical : oddVertical));
				pageNumberCreditWords.setAttribute("justify", isEvenPage ? "left" : "right");
				pageNumberCreditWords.setAttribute("valign", "top");
				pageNumberCreditWords.setAttribute("font-size", "14");

				pageNumberCreditTag.appendChild(pageNumberCreditWords); // Add this as a child of the <credit> tag

				// Add it before the <part-list> tag
				partListTag.getParentNode().insertBefore(pageNumberCreditTag, partListTag);
			}

			/*
			 * Add the mini title to the top of the page
			 */
			{
				// Check if we need to add a mini title
				if (miniTitleExists[pageNumber - 1]) {
					// There already exists a mini title of this page and we've already handled it.
					// No need to add a new one.
					continue;
				}

				// Create the parent <credit> tag
				Element pageNumberCreditTag = musicXMLDocument.createElement("credit");
				pageNumberCreditTag.setAttribute("page", pageNumberText); // Set the page number of the credit

				// Add the (optional) <credit-type> child
				Element pageNumberCreditType = musicXMLDocument.createElement("credit-type");
				pageNumberCreditType.setTextContent("title");
				pageNumberCreditTag.appendChild(pageNumberCreditType);

				// Add the mandatory <credit-words> child
				Element pageNumberCreditWords = musicXMLDocument.createElement("credit-words");
				pageNumberCreditWords.setTextContent(title); // Set the text equal to the page number

				// Set all the attributes
				pageNumberCreditWords.setAttribute("default-x",
						String.format(Locale.US, "%.4f", isEvenPage ? evenCenter : oddCenter));
				pageNumberCreditWords.setAttribute("default-y",
						String.format(Locale.US, "%.4f", isEvenPage ? evenVertical : oddVertical));
				pageNumberCreditWords.setAttribute("justify", "center");
				pageNumberCreditWords.setAttribute("valign", "top");
				pageNumberCreditWords.setAttribute("font-size", "12");

				pageNumberCreditTag.appendChild(pageNumberCreditWords); // Add this as a child of the <credit> tag

				// Add it before the <part-list> tag
				partListTag.getParentNode().insertBefore(pageNumberCreditTag, partListTag);
			}
		}

		System.out.println("All done adding page numbers and mini-titles!");
	}

}
