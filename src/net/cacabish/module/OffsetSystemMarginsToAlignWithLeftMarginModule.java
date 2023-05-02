package net.cacabish.module;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.cacabish.MusicXMLDocument;

public class OffsetSystemMarginsToAlignWithLeftMarginModule implements CleanerModule {

	@Override
	public String getModuleName() {
		return "Align Systems w/ Left Margin";
	}

	@Override
	public String getModuleTooltip() {
		return "<html>If checked, the program will reduce the system margins of the left-most systems so that they align with the left margin."
				+ "<br>The first system's margin is also reduced by the same amount so that the relative positioning of the systems is preserved."
				+ "<br>If there is only one system in the sheet, this doesn't do anything.</html>";
	}

	@Override
	public void execute(MusicXMLDocument musicXMLDocument) throws CleanerException {
		System.out.println("Left aligning systems with the left margins...");
		if (musicXMLDocument == null) {
			throw new CleanerException("document provided was null");
		}

		// Get all of the <part-abbreviation> tags.
		NodeList partAbbreviationTags = musicXMLDocument.getElementsByTagName("part-abbreviation");
		for (int i = 0; i < partAbbreviationTags.getLength(); i++) {
			Element partAbbreviationTag = (Element) partAbbreviationTags.item(0);

			if (!partAbbreviationTag.getTextContent().isEmpty()) {
				// We've found a part with a part abbreviation text.
				// This abbreviation will show up on Finale and it will look bad if we force
				// alignment.
				// So, we'll just do nothing.
				System.out.println("One or more parts has a shortened name. Aborting aligning.");
				return;
			}
		}

		// If we've made it to this point, none of the parts has a shortened name, so
		// we're good to align all we like!

		// Fetch all the <system-layout> tags
		NodeList allSystemLayoutTags = musicXMLDocument.getElementsByTagName("system-layout");

		// Check how many <system-layout> tags we have
		if (allSystemLayoutTags.getLength() <= 1) {
			// In this case, we don't have enough relative information to slide all the
			// systems, so we'll just do nothing
			// NOTE: we could use some standard default value in this case, but for now,
			// I'll just do nothing.
			return;
		}

		// We need to find the minimum left-margin value
		double minimumValue = Double.MAX_VALUE;

		// Iterate over all the <system-layout> tags
		for (int i = 0; i < allSystemLayoutTags.getLength(); i++) {
			Element systemLayoutElement = (Element) allSystemLayoutTags.item(i);

			// Fetch the <system-margins> child of the <system-layout> tag.
			NodeList systemMarginsTags = systemLayoutElement.getElementsByTagName("system-margins");

			// We will be operating on a single <system-margins> tag, so we need to use
			// that.
			if (systemMarginsTags.getLength() != 1) {
				// We don't need to do anything because this is either a bug (if > 1) or this
				// doesn't exist (if == 0), which can happen according to the schema
				continue;
			}

			// Since we know there is exactly one child, fetch it.
			Element systemMarginsElement = (Element) systemMarginsTags.item(0);

			// Great! Now, we need to get the <left-margin> child of the <system-margins>
			// tag. According to the schema, it MUST exist and there is exactly one of them.
			NodeList leftMarginTag = systemMarginsElement.getElementsByTagName("left-margin");

			// As a sanity check, assert there is only one
			if (leftMarginTag.getLength() != 1) {
				// Again, this shouldn't happen, but for defensive code, I'm putting this here.
				continue;
			}

			// Get the left-margins tag
			Element leftMarginsElement = (Element) leftMarginTag.item(0);
			try {
				// Read the value
				double value = Double.parseDouble(leftMarginsElement.getTextContent());

				// Take the smaller of this value and the running minimum.
				minimumValue = Math.min(value, minimumValue);
			} catch (NumberFormatException e) {
				// This tag errantly contains a non-number (why did this happen??), so ignore
				// this.
				continue;
			}

		}

		// Worst case check
		if (minimumValue == Double.MAX_VALUE) {
			// Nothing changed? Strange, but we can't do anything with this. Return.
			return;
		}

		// At this point, we have determined what the smallest left margin is. Hoo-ray!
		// Now, we're going to zero out all systems that we can and offset what should
		// be the first system (or any other indented system)

		// The absolute threshold to be constituted as 0 margin and warrant deletion.
		final double epsilon = 0.1;

		// Iterate over all the <system-layout> tags again!
		// Iterate over it backwards since we are potentially deleting elements and want
		// to avoid a concurrency issue.
		for (int i = allSystemLayoutTags.getLength() - 1; i >= 0; i--) {
			Element systemLayoutElement = (Element) allSystemLayoutTags.item(i);

			// Fetch the <system-margins> child of the <system-layout> tag.
			NodeList systemMarginsTags = systemLayoutElement.getElementsByTagName("system-margins");

			// Once again, assert the length is 1
			if (systemMarginsTags.getLength() != 1) {
				continue;
			}

			// Since we know there is exactly one child, fetch it.
			Element systemMarginsElement = (Element) systemMarginsTags.item(0);

			// Great! Now, we need to get the <left-margin> child of the <system-margins>
			// tag. According to the schema, it MUST exist and there is exactly one of them.
			NodeList leftMarginTag = systemMarginsElement.getElementsByTagName("left-margin");

			// As a sanity check, assert there is only one
			if (leftMarginTag.getLength() != 1) {
				continue;
			}

			// Get the left-margins tag
			Element leftMarginsElement = (Element) leftMarginTag.item(0);

			try {
				// Read the value
				double value = Double.parseDouble(leftMarginsElement.getTextContent());

				// Now offset the value by the minimum value
				double newValue = value - minimumValue;

				// Now, check for an approximate zero. Since we are handling floating-point
				// numbers, we use a threshold epsilon.
				if (Math.abs(newValue) < epsilon) {
					// This tag is effectively a 0 margin, so set the <left-margin> to 0.00.
					leftMarginsElement.setTextContent("0.00");
				} else {
					// This system is indented some, so we just change the indentation.
					leftMarginsElement.setTextContent(String.format(Locale.US, "%.2f", newValue)); // Since MuseScore
																									// uses 2 decimal
																									// places of
																									// accuracy, so will
																									// I.
				}
			} catch (NumberFormatException e) {
				// This tag errantly contains a non-number (why did this happen??), so ignore
				// this.
				continue;
			}
		}

		System.out.println("Done left-aligning all non-indented systems and offsetting those that are indented!");
	}

}
