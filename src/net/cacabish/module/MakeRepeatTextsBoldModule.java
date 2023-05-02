package net.cacabish.module;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class MakeRepeatTextsBoldModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Make Repeat Texts Bold";
	}

	@Override
	public String getModuleTooltip() {
		return "<html>If checked, the program will make any ending texts like <tt>D.C. al Coda</tt> and <tt>Fine</tt> bolded.</html>";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Making repeat texts bold...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// To make sure we are dealing with actual ending texts and not just textual
		// directions that say the words, we search for the <sound> tags first
		NodeList soundNodes = musicXMLDocument.getElementsByTagName("sound");
		for (int i = 0; i < soundNodes.getLength(); i++) {
			Element soundElement = (Element) soundNodes.item(i);

			// We're first going to check that this is inside a <direction> tag.
			Node parentNode = soundElement.getParentNode();

			// If there is no parent tag, it's not an element, or isn't a <direction> tag,
			// skip it.
			if (parentNode == null || parentNode.getNodeType() != Node.ELEMENT_NODE
					|| !((Element) parentNode).getTagName().equalsIgnoreCase("direction")) {
				continue;
			}

			// At this point, we can assume this sound tag is inside a <direction> tag.
			// Great! Now, let's check that it actually pertains to repeats.
			if (soundElement.getAttribute("dacapo").equalsIgnoreCase("yes") // Is this a "D.C. ______"?
					|| !soundElement.getAttribute("dalsegno").isEmpty() // Is this a "D.S. ______"?
					|| !soundElement.getAttribute("fine").isEmpty() // Is this a "Fine"?
					|| !soundElement.getAttribute("tocoda").isEmpty() // Is this a "To Coda"?
			) {
				// It passed one of tests! Fantastic! Almost done now. We just need the <words>
				// tag.
				NodeList wordsList = ((Element) parentNode).getElementsByTagName("words");

				for (int j = 0; j < wordsList.getLength(); j++) {
					Element wordElement = (Element) wordsList.item(j);

					// Set the font-weight to bold.
					wordElement.setAttribute("font-weight", "bold");
				}

			} else {
				// It's not something we're looking for. Continue.
				continue;
			}
		}

		System.out.println("Successfully made repeat texts bold!");
	}

}
