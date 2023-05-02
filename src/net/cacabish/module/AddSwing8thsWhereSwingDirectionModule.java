package net.cacabish.module;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class AddSwing8thsWhereSwingDirectionModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Add Swing 8ths wherever a \"Swing\" Direction";
	}

	@Override
	public String getFormattedModuleName() {
		return "<html>Add Swing 8ths wherever <br>a \"Swing\" Direction</html>";
	}

	@Override
	public String getModuleTooltip() {
		return "<html>If checked, the program will add a symbol that will turn on swung 8th notes "
				+ "<br>wherever a direction (i.e Staff Text) is placed labeled \"Swing\".</html>";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Adding swing 8ths where there is a \"Swing\" direction...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// Iterate over the <words> tags.
		NodeList wordsList = musicXMLDocument.getElementsByTagName("words");
		for (int i = 0; i < wordsList.getLength(); i++) {
			Element wordsElement = (Element) wordsList.item(i);

			Element directionTypeTag = (Element) wordsElement.getParentNode(); // <words> elements are always children
																				// of a <direction-type> tag
			Element nextSibling = (Element) directionTypeTag.getNextSibling(); // Get the next sibling of this tag. Will
																				// be null if doesn't exist!
			Element directionTag = (Element) directionTypeTag.getParentNode(); // <direction-type> elements are always
																				// children of a <direction> tag

			// Check if the text is "Swing"
			if (wordsElement.getTextContent().trim().equalsIgnoreCase("swing")) {
				// We need to add all the tags that will generate the swing rhythm notation

				/*
				 * Here is the full tag we need to add to the <direction> tag.
				 * 
				 * <direction-type> <metronome> <metronome-note>
				 * <metronome-type>eighth</metronome-type> <metronome-beam
				 * number="1">begin</metronome-beam> </metronome-note> <metronome-note>
				 * <metronome-type>eighth</metronome-type> <metronome-beam
				 * number="1">end</metronome-beam> </metronome-note>
				 * <metronome-relation>equals</metronome-relation> <metronome-note>
				 * <metronome-type>quarter</metronome-type> <metronome-tuplet bracket="yes"
				 * show-number="actual" type="start"> <actual-notes>3</actual-notes>
				 * <normal-notes>2</normal-notes> <normal-type>eighth</normal-type>
				 * </metronome-tuplet> </metronome-note> <metronome-note>
				 * <metronome-type>eighth</metronome-type> <metronome-tuplet type="stop">
				 * <actual-notes>3</actual-notes> <normal-notes>2</normal-notes>
				 * <normal-type>eighth</normal-type> </metronome-tuplet> </metronome-note>
				 * </metronome> </direction-type>
				 * 
				 * Source:
				 * https://www.w3.org/2021/06/musicxml40/musicxml-reference/examples/metronome-
				 * note-element/
				 */

				// Create the root <direction-type> and <metronome> tags.
				Element newDirectionTypeTag = musicXMLDocument.createElement("direction-type");
				Element metronomeTag = musicXMLDocument.createElement("metronome");
				{
					// Everything in this scope is a descendent of the <metronome> tag

					// Add the first eighth note
					Element metronomeNote1Tag = musicXMLDocument.createElement("metronome-note");
					{
						Element metronomeTypeTag = musicXMLDocument.createElement("metronome-type");
						metronomeTypeTag.setTextContent("eighth"); // It's an eighth note
						metronomeNote1Tag.appendChild(metronomeTypeTag);

						// Begin the eighth note beam that will connect the two eighth notes
						Element metronomeBeamTag = musicXMLDocument.createElement("metronome-beam");
						metronomeBeamTag.setAttribute("number", "1");
						metronomeBeamTag.setTextContent("begin");
						metronomeNote1Tag.appendChild(metronomeBeamTag);
					}
					// Add this note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote1Tag);

					// Add the second eighth note
					Element metronomeNote2Tag = musicXMLDocument.createElement("metronome-note");
					{

						Element metronomeTypeTag = musicXMLDocument.createElement("metronome-type");
						metronomeTypeTag.setTextContent("eighth"); // It's another eighth note
						metronomeNote2Tag.appendChild(metronomeTypeTag);

						// End the eighth note beam that will connect the two eighth notes
						Element metronomeBeamTag = musicXMLDocument.createElement("metronome-beam");
						metronomeBeamTag.setAttribute("number", "1");
						metronomeBeamTag.setTextContent("end");
						metronomeNote2Tag.appendChild(metronomeBeamTag);
					}
					// Add this note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote2Tag);

					// Create the "=" tag that goes in the middle
					Element metronomeRelationTag = musicXMLDocument.createElement("metronome-relation");
					metronomeRelationTag.setTextContent("equals");
					metronomeTag.appendChild(metronomeRelationTag);

					// Add the first quarter note in the tuple
					Element metronomeNote3Tag = musicXMLDocument.createElement("metronome-note");
					{
						Element metronomeTypeTag = musicXMLDocument.createElement("metronome-type");
						metronomeTypeTag.setTextContent("quarter"); // It's a quarter note
						metronomeNote3Tag.appendChild(metronomeTypeTag);

						// Signify this is part of a tuplet
						Element metronomeTupletTag = musicXMLDocument.createElement("metronome-tuplet");
						// We are starting the tuplet
						metronomeTupletTag.setAttribute("bracket", "yes");
						metronomeTupletTag.setAttribute("show-number", "actual");
						metronomeTupletTag.setAttribute("type", "start");
						{
							// 3 eights...
							Element actualNotesTag = musicXMLDocument.createElement("actual-notes");
							actualNotesTag.setTextContent("3");
							metronomeTupletTag.appendChild(actualNotesTag);

							// .. are actually equal to 2.
							Element normalNotesTag = musicXMLDocument.createElement("normal-notes");
							normalNotesTag.setTextContent("2");
							metronomeTupletTag.appendChild(normalNotesTag);

							// Did I mention they were eighths?
							Element normalTypeTag = musicXMLDocument.createElement("normal-type");
							normalTypeTag.setTextContent("eighth");
							metronomeTupletTag.appendChild(normalTypeTag);
						}
						// Add this tuple to the note
						metronomeNote3Tag.appendChild(metronomeTupletTag);
					}
					// Add this tupled quarter note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote3Tag);

					// Add the eighth note in the tuple
					Element metronomeNote4Tag = musicXMLDocument.createElement("metronome-note");
					{
						Element metronomeTypeTag = musicXMLDocument.createElement("metronome-type");
						metronomeTypeTag.setTextContent("eighth"); // It's an eighth note
						metronomeNote4Tag.appendChild(metronomeTypeTag);

						// Signify this is part of a tuplet
						Element metronomeTupletTag = musicXMLDocument.createElement("metronome-tuplet");
						// We are ending the tuplet with this note
						metronomeTupletTag.setAttribute("type", "stop");
						{
							// 3 eights...
							Element actualNotesTag = musicXMLDocument.createElement("actual-notes");
							actualNotesTag.setTextContent("3");
							metronomeTupletTag.appendChild(actualNotesTag);

							// .. are actually equal to 2.
							Element normalNotesTag = musicXMLDocument.createElement("normal-notes");
							normalNotesTag.setTextContent("2");
							metronomeTupletTag.appendChild(normalNotesTag);

							// Did I mention they were eighths?
							Element normalTypeTag = musicXMLDocument.createElement("normal-type");
							normalTypeTag.setTextContent("eighth");
							metronomeTupletTag.appendChild(normalTypeTag);
						}
						// Add this tuple to the note
						metronomeNote4Tag.appendChild(metronomeTupletTag);
					}
					// Add this tupled eighth note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote4Tag);
				}
				// Finally, add the <metronome> tag as the child of the new <direction-type>
				// tag.
				newDirectionTypeTag.appendChild(metronomeTag);

				// Alright! Now, we just need to add this as a child.
				// Since we are using a <direction-type> tag as a base, it can either come
				// before or after it.
				// I'm electing to add it after, which means there are two cases.
				// Either there is another tag afterwards, in which case we add it before
				// that...
				// ...or we just add it at the end because there is nothing to add.
				if (nextSibling == null) {
					// Just stick it at the end (i.e. right after the previous <direction-type> tag)
					directionTag.appendChild(newDirectionTypeTag);
				} else {
					// Add it before the next sibling (i.e. right after the previous
					// <direction-type> tag)
					directionTag.insertBefore(newDirectionTypeTag, nextSibling);
				}

				// Whew! We're done.
			}
		}

		System.out.println("Finished adding swing 8ths where there is a \"Swing\" direction!");
	}

}
