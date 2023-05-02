package net.cacabish.module;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class CorrectTempoMarkModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Correct Tempo Marking";
	}

	@Override
	public String getModuleTooltip() {
		return "If checked, the program will make necessary adjustments so tempo directional texts "
				+ "and their associated metrnonome marks are one object.";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Correcting the tempo mark...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// Search for all the <metronome> tags
		NodeList nodeList = musicXMLDocument.getElementsByTagName("metronome");

		for (int i = 0; i < nodeList.getLength(); i++) {
			Element metronomeTag = (Element) nodeList.item(i);
			Element directionTypeTag = (Element) metronomeTag.getParentNode();

			// Look for a previous <direction-type> that may contain the textual directions,
			// such as a <words> direction
			Node previous = directionTypeTag.getPreviousSibling();
			while (previous != null && previous.getNodeType() != Node.ELEMENT_NODE) {
				previous = previous.getPreviousSibling();
			}

			if (previous == null) {
				// The metronome marking is first; there is no musical direction marking before
				// it
				// Thus, we only need to replace the "default-x" attribute, if it exists, with
				// "relative-x"
				// However, ensure there isn't a "relative-x" already that we would override
				// It seems that Finale will always ignore the "default-x" attribute, so it
				// should be future-proof to replace
				String x = metronomeTag.getAttribute("default-x");
				if (!x.isEmpty() && !metronomeTag.hasAttribute("relative-x")) {
					metronomeTag.removeAttribute("default-x");
					metronomeTag.setAttribute("relative-x", x);
				}

				// And we're done.
			} else {
				// Check if there are words in this direction
				Element previousElement = (Element) previous;
				NodeList wordsList = previousElement.getElementsByTagName("words");

				if (wordsList.getLength() == 0) {
					// This is something else. I'm not sure what would be here, but I don't know how
					// to handle it. Abort.
					System.out.println("Unexpected element before metronome mark. Skipping.");
					continue;
				} else {
					// There is a directional marking with <words>. We've got stuff to do.
					// First, get the first <words> tag - this should contain all the attributes we
					// care about
					Element firstWords = (Element) wordsList.item(0);

					// Replace the "default-x" attribute with "relative-x", if it exists.
					// Also make sure there isn't a "relative-x" already that we would override
					String x = firstWords.getAttribute("default-x");
					if (!x.isEmpty() && !metronomeTag.hasAttribute("relative-x")) {
						firstWords.removeAttribute("default-x");
						firstWords.setAttribute("relative-x", x);
					}

					// firstWords.removeAttribute("font-size"); // Optional. Remove the font-size
					// tag and let Finale decide its size

					// Next, remove all coordinate attributes from the metronome mark
					metronomeTag.removeAttribute("default-x");
					metronomeTag.removeAttribute("default-y");
					metronomeTag.removeAttribute("relative-x");
					metronomeTag.removeAttribute("relative-y");
					metronomeTag.removeAttribute("font-size");

					// Finally, ensure there is a "font-weight"=normal attribute
					metronomeTag.setAttribute("font-weight", "normal");
				}
			}

			System.out.println("All finished correcting tempo markings!");
		}

		/*
		 * XXX This method does not address if there is text after the tempo mark or if
		 * there are other <direction-types> in the <direction> tag. It is conceivable
		 * that other directional markings, such as dynamics, could suffer from the
		 * problem that this method fixes. To be investigated.
		 */

	}

}
