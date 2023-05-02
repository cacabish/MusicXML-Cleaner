package net.cacabish.module;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class FormatOssias implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Format Ossias";
	}

	@Override
	public String getModuleTooltip() {
		return "If checked, the program will attempt to format any ossias that exist.";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Attempting to format ossias...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// First, we get the part list to see which part corresponds to the ossia
		NodeList partList = musicXMLDocument.getElementsByTagName("part-list");
		if (partList.getLength() == 0) {
			// There were no parts. We literally cannot proceed.
			return;
		}
		// At this point, there is at least one of these, and there should only be one
		// of these, so we're good.
		Element partListElement = (Element) partList.item(0);

		// In theory, there's nothing wrong with having multiple different ossia staffs,
		// so we'll process them all.
		HashSet<String> ossiaPartIDs = new HashSet<>();

		// Get the score part list
		NodeList scorePartList = partListElement.getElementsByTagName("score-part");

		for (int i = 0; i < scorePartList.getLength(); i++) {
			if (scorePartList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element scorePartElement = (Element) scorePartList.item(i);

				// Get the part id
				String partNameID = scorePartElement.getAttribute("id");
				if (partNameID == null || partNameID.isEmpty()) {
					// Technically, this has to exist according to the spec, but we'll safety check
					// it anyway.
					continue;
				}

				// Get the part name
				NodeList partNameList = scorePartElement.getElementsByTagName("part-name");
				if (partNameList.getLength() != 1) {
					// Technically, this has to exist according to the spec, but we'll safety check
					// it anyway.
					continue;
				}

				String partName = partNameList.item(0).getTextContent().trim();
				if (partName.equalsIgnoreCase("ossia")) {
					// We'll flag this part as an ossia
					ossiaPartIDs.add(partNameID);
				}

			}
		}

		// Now, we go through all the parts
		NodeList listOfParts = musicXMLDocument.getElementsByTagName("part");
		for (int i = 0; i < listOfParts.getLength(); i++) {
			Node partNode = listOfParts.item(i);
			if (partNode.getNodeType() == Node.ELEMENT_NODE) {
				Element partElement = (Element) partNode;
				String partReferenceID = partElement.getAttribute("id");

				if (partReferenceID == null || partReferenceID.isEmpty()) {
					// Nothing we can do here.
					continue;
				}

				if (!ossiaPartIDs.contains(partReferenceID)) {
					// This isn't an ossia part. Skip this one.
					continue;
				}

				// Alright, this is an ossia part! We can proceed!
				// Get all the measures for this part (we will need all of them
				NodeList listOfMeasures = partElement.getElementsByTagName("measure");

				Element startOfSystemMeasure = null;
				boolean doesMusicHappenOnThisSystem = false;
				boolean isMusicSetToDisplayAlready = true;

				for (int j = 0; j < listOfMeasures.getLength(); j++) {
					Element measureTag = (Element) listOfMeasures.item(j); // Get the measure tag
					String measureNumber = measureTag.getAttribute("number"); // Fetch the measure number

					if (measureNumber.trim().equals("1")) {
						// This is the first measure, so we will format the staff here.
						startOfSystemMeasure = measureTag;

						// Now we need to format the staff
						NodeList attributesTags = measureTag.getElementsByTagName("attributes");
						if (attributesTags.getLength() == 0) {
							// We need to create the attributes tag
							Element attributesTag = musicXMLDocument.createElement("attributes");

							Element staffDetailsTag = musicXMLDocument.createElement("staff-details");
							attributesTag.appendChild(staffDetailsTag);

							// Format the staff to be of "ossia" type
							Element staffTypeTag = musicXMLDocument.createElement("staff-type");
							staffTypeTag.setTextContent("ossia");
							staffDetailsTag.appendChild(staffTypeTag);

							NodeList notesTags = measureTag.getElementsByTagName("note");
							if (notesTags.getLength() == 0) {
								// Just put it at the end (Finale will probably do weird stuff, but the fact is,
								// this probably shouldn't happen)
								startOfSystemMeasure.appendChild(attributesTag);
							} else {
								// Put right before the <notes> tag
								measureTag.insertBefore(attributesTag, notesTags.item(0));
							}
						} else {
							// The attributes tag already exists
							Element attributesTag = (Element) attributesTags.item(0);

							NodeList staffDetailsTags = attributesTag.getElementsByTagName("staff-details");
							if (staffDetailsTags.getLength() == 0) {
								// We need to create the staff-details tag
								Element staffDetailsTag = musicXMLDocument.createElement("staff-details");

								// Format the staff to be of "ossia" type
								Element staffTypeTag = musicXMLDocument.createElement("staff-type");
								staffTypeTag.setTextContent("ossia");
								staffDetailsTag.appendChild(staffTypeTag);

								// Since where we place this matters, we will find the first tag that's supposed
								// to come after it, and put it before it.
								String[] tagNamesThatComeAfterStaffDetailsTag = new String[] { "transpose", "for-part",
										"directive", "measure-style" };
								Element candidateTag = null;

								// Go through the tags in order
								for (String tagNameToPlaceBefore : tagNamesThatComeAfterStaffDetailsTag) {
									NodeList candidateTags = attributesTag.getElementsByTagName(tagNameToPlaceBefore);
									if (candidateTags.getLength() != 0) {
										// Got a match! Use this candidate!
										candidateTag = (Element) candidateTags.item(0);
										break; // We were just looking for the first one we came across.
									}
								}

								// Insert before its closest successor
								attributesTag.insertBefore(staffDetailsTag, candidateTag);
							} else {
								// The staff-details tag already exists
								Element staffDetailsTag = (Element) staffDetailsTags.item(0);

								NodeList staffTypeTags = staffDetailsTag.getElementsByTagName("staff-type");
								if (staffTypeTags.getLength() == 0) {
									// We need to create the staff-type tag

									// Format the staff to be of "ossia" type
									Element staffTypeTag = musicXMLDocument.createElement("staff-type");
									staffTypeTag.setTextContent("ossia");
									staffDetailsTag.appendChild(staffTypeTag);
								} else {
									// The staff-type tag already exists
									Element staffTypeTag = (Element) staffTypeTags.item(0);

									// Make sure the content is set to "ossia"
									staffTypeTag.setTextContent("ossia");
								}

							}
						}

					}

					// We're looking for a "new-system" or "new-page"
					NodeList listOfPrints = measureTag.getElementsByTagName("print");
					if (listOfPrints.getLength() != 0) {
						// Okay, there's a print tag
						Element printTag = (Element) listOfPrints.item(0);
						String newSystemAttribute = printTag.getAttribute("new-system");
						String newPageAttribute = printTag.getAttribute("new-page");

						if (newSystemAttribute.equalsIgnoreCase("yes") || newPageAttribute.equalsIgnoreCase("yes")) {
							// This is the start of a new system!
							// So, we need to resolve the previous system and see if we need to change how
							// we display it.

							if (doesMusicHappenOnThisSystem != isMusicSetToDisplayAlready) {
								// This means we need to either start displaying music on the previous system...
								// ... or we need to turn it off.

								NodeList attributesTagOfTheFirstMeasureOfThePreviousSystem = startOfSystemMeasure
										.getElementsByTagName("attributes");
								if (attributesTagOfTheFirstMeasureOfThePreviousSystem.getLength() == 0) {
									// We need to create the attributes tag.
									Element attributesTag = musicXMLDocument.createElement("attributes");

									Element staffDetailsTag = musicXMLDocument.createElement("staff-details");
									staffDetailsTag.setAttribute("print-object",
											doesMusicHappenOnThisSystem ? "yes" : "no"); // Set the staff to not display
									attributesTag.appendChild(staffDetailsTag);

									NodeList notesTags = startOfSystemMeasure.getElementsByTagName("note");
									if (notesTags.getLength() == 0) {
										// Just put it at the end (Finale will probably do weird stuff, but the fact is,
										// this probably shouldn't happen)
										startOfSystemMeasure.appendChild(attributesTag);
									} else {
										// Put right before the <notes> tag
										startOfSystemMeasure.insertBefore(attributesTag, notesTags.item(0));
									}
								} else {
									// The attributes tag already exists
									Element attributesTag = (Element) attributesTagOfTheFirstMeasureOfThePreviousSystem
											.item(0);

									NodeList staffDetailsTags = attributesTag.getElementsByTagName("staff-details");
									if (staffDetailsTags.getLength() == 0) {
										// We need to create the staff-details tag
										Element staffDetailsTag = musicXMLDocument.createElement("staff-details");
										staffDetailsTag.setAttribute("print-object",
												doesMusicHappenOnThisSystem ? "yes" : "no"); // Set the staff to not
																								// display

										// Since where we place this matters, we will find the first tag that's supposed
										// to come after it, and put it before it.
										String[] tagNamesThatComeAfterStaffDetailsTag = new String[] { "transpose",
												"for-part", "directive", "measure-style" };
										Element candidateTag = null;

										// Go through the tags in order
										for (String tagNameToPlaceBefore : tagNamesThatComeAfterStaffDetailsTag) {
											NodeList candidateTags = attributesTag
													.getElementsByTagName(tagNameToPlaceBefore);
											if (candidateTags.getLength() != 0) {
												// Got a match! Use this candidate!
												candidateTag = (Element) candidateTags.item(0);
												break; // We were just looking for the first one we came across.
											}
										}

										// Insert before its closest successor
										attributesTag.insertBefore(staffDetailsTag, candidateTag);
									} else {
										// The staff-details tag already exists
										Element staffDetailsTag = (Element) staffDetailsTags.item(0);
										staffDetailsTag.setAttribute("print-object",
												doesMusicHappenOnThisSystem ? "yes" : "no"); // Set the staff to not
																								// display
									}

								}

								// Now that the tags have been properly made/updated...
								// ... make it so the internal state of the display is up-to-date
								isMusicSetToDisplayAlready = doesMusicHappenOnThisSystem;
							}

							// RESET FOR NEXT SYSTEM //
							startOfSystemMeasure = measureTag; // Assign this measure as the new start of a system.
							doesMusicHappenOnThisSystem = false; // Reset for the new system

						}
					}

					// If we haven't found any non-rest measures on this system, we need to keep
					// looking.
					if (!doesMusicHappenOnThisSystem) {
						NodeList listOfNotes = measureTag.getElementsByTagName("note");
						for (int k = 0; k < listOfNotes.getLength(); k++) {
							Element noteTag = (Element) listOfNotes.item(k);

							// Check if there is a rest tag inside the note tag.
							if (noteTag.getElementsByTagName("rest").getLength() == 0) {
								// This note is NOT a rest, which means there is music here!
								doesMusicHappenOnThisSystem = true;
								break;
							}
						}
					}

				}

				// Finally, we need to resolve the last system of the part
				if (doesMusicHappenOnThisSystem != isMusicSetToDisplayAlready) {
					// This means we need to either start displaying music on the previous system...
					// ... or we need to turn it off.

					NodeList attributesTagOfTheFirstMeasureOfThePreviousSystem = startOfSystemMeasure
							.getElementsByTagName("attributes");
					if (attributesTagOfTheFirstMeasureOfThePreviousSystem.getLength() == 0) {
						// We need to create the attributes tag.
						Element attributesTag = musicXMLDocument.createElement("attributes");

						Element staffDetailsTag = musicXMLDocument.createElement("staff-details");
						staffDetailsTag.setAttribute("print-object", doesMusicHappenOnThisSystem ? "yes" : "no"); // Set
																													// the
																													// staff
																													// to
																													// not
																													// display
						attributesTag.appendChild(staffDetailsTag);

						NodeList notesTags = startOfSystemMeasure.getElementsByTagName("note");
						if (notesTags.getLength() == 0) {
							// Just put it at the end (Finale will probably do weird stuff, but the fact is,
							// this probably shouldn't happen)
							startOfSystemMeasure.appendChild(attributesTag);
						} else {
							// Put right before the <notes> tag
							startOfSystemMeasure.insertBefore(attributesTag, notesTags.item(0));
						}

					} else {
						// The attributes tag already exists
						Element attributesTag = (Element) attributesTagOfTheFirstMeasureOfThePreviousSystem.item(0);

						NodeList staffDetailsTags = attributesTag.getElementsByTagName("staff-details");
						if (staffDetailsTags.getLength() == 0) {
							// We need to create the staff-details tag
							Element staffDetailsTag = musicXMLDocument.createElement("staff-details");
							staffDetailsTag.setAttribute("print-object", doesMusicHappenOnThisSystem ? "yes" : "no"); // Set
																														// the
																														// staff
																														// to
																														// not
																														// display

							// Since where we place this matters, we will find the first tag that's supposed
							// to come after it, and put it before it.
							String[] tagNamesThatComeAfterStaffDetailsTag = new String[] { "transpose", "for-part",
									"directive", "measure-style" };
							Element candidateTag = null;

							// Go through the tags in order
							for (String tagNameToPlaceBefore : tagNamesThatComeAfterStaffDetailsTag) {
								NodeList candidateTags = attributesTag.getElementsByTagName(tagNameToPlaceBefore);
								if (candidateTags.getLength() != 0) {
									// Got a match! Use this candidate!
									candidateTag = (Element) candidateTags.item(0);
									break; // We were just looking for the first one we came across.
								}
							}

							// Insert before its closest successor
							attributesTag.insertBefore(staffDetailsTag, candidateTag);
						} else {
							// The staff-details tag already exists
							Element staffDetailsTag = (Element) staffDetailsTags.item(0);
							staffDetailsTag.setAttribute("print-object", doesMusicHappenOnThisSystem ? "yes" : "no"); // Set
																														// the
																														// staff
																														// to
																														// not
																														// display
						}

					}
				}
			}
		}

		// Done!
		System.out.println("Done formatting ossias!");
	}

}
