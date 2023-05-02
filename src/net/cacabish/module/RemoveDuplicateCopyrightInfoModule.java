package net.cacabish.module;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class RemoveDuplicateCopyrightInfoModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Remove Extraneous Copyright";
	}

	@Override
	public String getModuleTooltip() {
		return "If checked, the program will remove copyright information from pages 2+. "
				+ "It will also correct the formatting of the copyright information on page 1.";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Removing duplicate copyright info...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		String copyrightInfo = musicXMLDocument.getCopyrightInfo();

		if (copyrightInfo == null) {
			System.out.println("Unable to fetch copyright info. Aborting.");
			return; // We have nothing to work with, so return
		}

		// Get a list of all the <credit> tags
		NodeList nodeList = musicXMLDocument.getElementsByTagName("credit");

		// Iterate over the list backwards since we are deleting elements and want to
		// avoid a concurrency issue.
		for (int i = nodeList.getLength() - 1; i >= 0; i--) {
			Element creditElement = (Element) nodeList.item(i);
			String pageNumber = creditElement.getAttribute("page"); // Gets the page number of the credit. NOTE: This
																	// can be "" if assumed to be page 1

			// Fetch the <credit-words> child of the <credit> tag.
			// Unless there is an image here, there should be one and only one of these.
			NodeList subList = creditElement.getElementsByTagName("credit-words");

			// Get the text inside the <credit-words> tag
			Element creditWordsElement = (Element) subList.item(0);
			String content = creditWordsElement.getTextContent().trim();
			// Check if the content contains the copyright info and see if the associated
			// page is not the first page
			if (content.equals(copyrightInfo)) {
				if (!pageNumber.isEmpty() && !pageNumber.equals("1")) {
					// This is redundant copyright information. Delete it.

					// If there is a newline and whitespace before this element, delete it.
					Node previousNode = creditElement.getPreviousSibling();
					if (previousNode != null && previousNode.getNodeType() == Node.TEXT_NODE) {
						previousNode.getParentNode().removeChild(previousNode);
					}

					// Delete the text element.
					creditElement.getParentNode().removeChild(creditElement);
				} else {
					// This is the primary copyright information.
					// While we have a handle on it, add the font-size attribute.
					creditWordsElement.setAttribute("font-size", "10");
					System.out.println("Font size corrected on primary copyright information.");
				}
			}
		}

		System.out.println("All done removing duplicate copyright information!");
	}

}
