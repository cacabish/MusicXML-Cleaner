package net.cacabish.module;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class ReplaceEdwinAndFreeSerifWithTimesNewRomanModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Replace uses of Edwin and FreeSerif with Times New Roman";
	}

	@Override
	public String getFormattedModuleName() {
		return "<html>Replace uses of Edwin and" + "<br>FreeSerif with Times New Roman</html>";
	}

	@Override
	public String getModuleTooltip() {
		return "<html>If checked, the program will set the font to Times New Roman for any"
				+ "<br>element that uses Edwin or FreeSerif fonts.</html>";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Replacing FreeSerif and Edwin with Times New Roman...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		try {
			// This XPath expression will return all elements that contain a "font-family"
			// attribute.
			String xPathExpression = "//*[@font-family]";
			NodeList nodeList = musicXMLDocument.xPathEvaluation(xPathExpression);

			// Iterate over the list of candidates
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);

				// Just in case I've picked up something I shouldn't have, check that this is an
				// element node
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					// Great! Go ahead and cast it.
					Element element = (Element) node;

					// Get the "font-family" attribute's value
					String oldValue = element.getAttribute("font-family");

					// Replace FreeSerif (case insensitive) with Times New Roman
					String newValue = oldValue.replaceAll("(?i)FreeSerif", "Times New Roman");
					// Replace Edwin (case insensitive) with Times New Roman
					newValue = newValue.replaceAll("(?i)Edwin", "Times New Roman");

					// Set the updated "font-family" attribute's value
					// Note: since we only replaced what we needed to, anything not matching what we
					// replaced should've been left alone.
					element.setAttribute("font-family", newValue);
				}
			}

		} catch (XPathExpressionException e) {
			// Bad stuff happened. Abort.
			throw new CleanerException("An error occurred while trying to replace the fonts: " + e.getMessage());
		}

		// Done!
		System.out.println("Done replacing FreeSerif and Edwin with Times New Roman!");
	}

}
