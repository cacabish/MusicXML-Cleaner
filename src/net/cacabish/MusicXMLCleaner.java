package net.cacabish;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A collection of self-contained transformative methods and helper methods that are intended to "clean" a MusicXML v3.1 file exported from MuseScore.
 * The purpose being that, once cleaned, the file could be uploaded into Finale or Finale Notepad with a lot of major changes made.
 * <br><br>
 * Every function here requires that a validated MusicXML document be passed in. If it is unvalidated, the function may do unexpected things.
 * Every method should ensure that if a document is valid before, then it is valid after the function is complete.
 * If a function modifies a document and it cannot complete for any reason, it should quietly fail.
 * 
 * @author cacabish
 * @version 1.0.0
 *
 */
public final class MusicXMLCleaner {

	/**
	 * The document builder factory to construct {@code DocumentBuilder} objects from. Statically initialized.
	 */
	private static final DocumentBuilderFactory DBF;
	
	static {
		// Create the factory one time
		DBF = DocumentBuilderFactory.newInstance();
		DBF.setValidating(true);
		DBF.setIgnoringElementContentWhitespace(true);
	}
	
	/**
	 * There should be no reason to instantiate this class.
	 */
	private MusicXMLCleaner() {}
	
	/**
	 * Constructs an XML DOM object, given a file, and validates it against the MusicXML 3.1 schema.
	 * @param file the file to be parsed
	 * @return the DOM object
	 * @throws ParserConfigurationException throws if there is a problem getting a new {@code DocumentBuilder}
	 * @throws SAXException throws if there is an error when parsing or validating the file
	 * @throws IOException throws if there is an I/O error resulting from parsing the file
	 */
	public static Document constructAndValidateMusicXMLDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		if (file == null)
			throw new IllegalArgumentException("file provided was null"); // You should know better than that. :(
		
		DocumentBuilder builder = DBF.newDocumentBuilder();
		
		// Construct an instance of a personal error handler to see if anything goes wrong when the file gets validated
		XMLValidationErrorHandler errorHandler = new XMLValidationErrorHandler();
		builder.setErrorHandler(errorHandler);
		
		// Redirect the entity resolver to the local directory instead of online
		builder.setEntityResolver(new EntityResolver() {
			
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				// Get the files from the local directory
				InputStream url = getClass().getResourceAsStream("/musicxml-3.1/schema/" + systemId.substring(systemId.lastIndexOf("/") + 1));
				
				if (url != null) {
					// Return pointer to the file
					return new InputSource(url);
				}
				else {
					// Things went wrong
					throw new SAXException("Unrecognized System ID: " + systemId + ". This probably means this isn't a MusicXML file.");
				}
				
			}
		});
		
		// Actually parse the file now
		Document primaryDoc = builder.parse(file);
		
		// Check if all went well
		if (!errorHandler.isValid()) {
			// All did not go well
			throw errorHandler.getException();
		}
		else {
			// All is well
			return primaryDoc;
		}
	}
	
	/*
	 * ======================================
	 * =============== HELPER ===============
	 * ============= FUNCTIONS ==============
	 * ======================================
	 */
	
	/**
	 * Takes a MusicXML document and returns the page height (in units).
	 * @param document a validated MusicXML v3.1 document
	 * @return the page height (in units) or -1 if it could not find the value
	 */
	public static double getPageHeight(Document document) {
		if (document == null) {
			return -1; // Shame on you! :(
		}
		
		NodeList nodeList = document.getElementsByTagName("page-height");
		if (nodeList.getLength() == 0) {
			// We couldn't find any <page-height> tags
			return -1;
		}
		
		try {
			// Return the text content as a double
			double height = Double.parseDouble(nodeList.item(0).getTextContent());
			return height;
		} catch (NumberFormatException e) {
			// This number is not a double
			return -1;
		} 
	}
	
	/**
	 * Takes a MusicXML document and returns the page width (in units).
	 * @param document a validated MusicXML v3.1 document
	 * @return the page width (in units) or -1 if it could not find the value
	 */
	public static double getPageWidth(Document document) {
		if (document == null) {
			return -1; // You monster. :(
		}
		
		NodeList nodeList = document.getElementsByTagName("page-width");
		if (nodeList.getLength() == 0) {
			// We couldn't find any <page-width> tags
			return -1;
		}
		
		try {
			// Return the text content as a double
			double width = Double.parseDouble(nodeList.item(0).getTextContent());
			return width;
		} catch (NumberFormatException e) {
			// This number is not a double
			return -1;
		} 
	}
	
	/**
	 * Takes a MusicXML document and returns the page margins (in units). 
	 * @param document a validated MusicXML v3.1 document
	 * @return an array of two double arrays or {@code null} if unsuccessful for any reason. 
	 *         The first double array corresponds to the even-paged margins 
	 *         and the second double array corresponds to the odd-paged margins. 
	 *         The margins are in the following order for both: left, right, top, bottom.
	 */
	public static double[][] getPageMargins(Document document) {
		if (document == null) {
			return null; // You disgust me. :(
		}
		
		double[] evenMargins = null;
		double[] oddMargins = null;
		
		// Run the search
		NodeList nodeList = document.getElementsByTagName("page-margins");
		
		// Iterate over all the <page-margins> tags
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element element = (Element) nodeList.item(i);
			// Order will be: left, right, top, bottom. This is according to the schema.
			double[] currentMargins = new double[4];
			int index = 0;
			
			// Get the children
			NodeList children = element.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node n = children.item(j);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					// This is an element node, not a text node (id: 3)
					try {
						double value = Double.parseDouble(n.getTextContent());
						currentMargins[index++] = value;
					} catch (NumberFormatException e) {
						// This didn't contain a number
						continue;
					}
				}
			}
			
			if (index < 4) {
				continue; // We didn't fill the array, so passing it along would be a mistake
			}
			
			if (element.hasAttribute("type") && !element.getAttribute("type").equals("both")) {
				// These margins correspond to either even pages or odd pages. The order is not specified in the schema.
				switch (element.getAttribute("type")) {
				case "even":
					evenMargins = currentMargins;
					break;
				case "odd":
					oddMargins = currentMargins;
					break;
				}
			}
			else {
				// Page Margins is either "type"=both or not provided, in which it is assumed to be "type"=both
				evenMargins = currentMargins;
				oddMargins = currentMargins;
				break;
			}
		}
		
		// If either set of margins is uninitialized, there will be problems down the line, so return null.
		if (evenMargins == null || oddMargins == null) {
			return null;
		}
		
		return new double[][] {evenMargins, oddMargins};
	}
	
	/**
	 * Takes a MusicXML document and returns the title of the score. This includes any quotation marks, if they are present.
	 * <br><br>
	 * Note: this method checks that the title is either contained within the first {@code <work-title>} or {@code <movement-title>} tags. 
	 * If neither of these are found, the method returns {@code null}, regardless of whether the title exists elsewhere.
	 * @param document a validated MusicXML v3.1 document
	 * @return the title of the score or {@code null} if unable to find either the {@code <work-title>} or {@code <movement-title>} tags
	 */
	public static String getTitle(Document document) {
		if (document == null) {
			return null; // Why would you do such a horrible thing? :(
		}
		
		// Fetch the <work-title> tag (which is what MuseScore uses by default)
		NodeList nodeList = document.getElementsByTagName("work-title");
		if (nodeList.getLength() == 0) {
			// Finale uses this tag instead. Added for future-proofing.
			nodeList = document.getElementsByTagName("movement-title");
			if (nodeList.getLength() == 0) {
				return null;
			}
		}
		
		// There (technically) should only be one element in this node list
		// The title should be the text content of the first node.
		return nodeList.item(0).getTextContent();
	}
	
	/**
	 * Takes a MusicXML document and returns the copyright information of the score. This includes any whitespace or newline characters.
	 * <br><br>
	 * Note: this method checks that the copyright is either within the {@code <rights>} tags. 
	 * If this tag doesn't exist, this method returns {@code null}, regardless of whether the copyright exists elsewhere.
	 * @param document a validated MusicXML v3.1 document
	 * @return the copyright information or {@code null} if unable to find
	 */
	public static String getCopyrightInfo(Document document) {
		if (document == null) {
			return null; // Seriously? No one's impressed. :(
		}
		
		// Fetches the <rights> tag
		NodeList nodeList = document.getElementsByTagName("rights");
		if (nodeList.getLength() == 0) {
			return null;
		}
		
		// There (technically) should only be one element in this node list
		// The copyright info should be the text content of the first node.
		return nodeList.item(0).getTextContent();
	}
	
	/**
	 * Takes a MusicXML document and returns the number of pages of the score.
	 * @param document a validated MusicXML v3.1 document
	 * @return the number of pages. Guaranteed to be at least 1 unless the document is {@code null}, then it returns -1.
	 */
	public static int getNumberOfPages(Document document) {
		if (document == null) {
			return -1; // *eyeroll* :(
		}
		
		// Gets all the <print> tags
		NodeList nodeList = document.getElementsByTagName("print");
		
		int numberOfPages = 1; // There is at least one page
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element printElement = (Element) nodeList.item(i);
			
			// If this <print> tag creates a new page, increment the count
			if (printElement.getAttribute("new-page").equals("yes")) {
				numberOfPages++;
			}
		}
		
		return numberOfPages;
	}
	
	
	/*
	 * ======================================
	 * =========== TRANSFORMATIVE ===========
	 * ============= FUNCTIONS ==============
	 * ======================================
	 */
	
	/**
	 * For scores containing 2+ pages, this method checks if a page number and/or mini title already exists for a page after the first. 
	 * If so, it ensures the attributes are correct. If not, it adds them. 
	 * <br><br>
	 * This method aligns page numbers to the left on even-numbered pages and to the right on odd-numbered pages.
	 * It also sets the font size for page numbers to be 14 pt. font and 12 pt. font for the mini titles. 
	 * <br><br>
	 * This method calls other methods to get the title, margins, page width, and page height. 
	 * If this method is unable to get any of these items, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	public static void addPageNumbersAndMiniTitles(Document document) {
		if (document == null) {
			return; // Wow. Just. Wow. :(
		}
		
		// Fetch the necessary values
		int numberOfPages = getNumberOfPages(document);
		double[][] margins = getPageMargins(document); // REMINDER: left, right, top, bottom
		double pageWidth = getPageWidth(document);
		double pageHeight = getPageHeight(document);
		String title = getTitle(document);
		
		// Check to proceed
		if (margins == null || pageWidth == -1 || pageHeight == -1 || title == null || numberOfPages == -1) {
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
		// For both of these arrays, the index corresponds to the (page number - 1) and is true if and only if the respective credit was found
		// Because we don't check credits on the first page, both of the indices at 0 are always false and are to be ignored.
		boolean[] pageNumberExists = new boolean[numberOfPages];
		boolean[] miniTitleExists = new boolean[numberOfPages];
		
		// Fetch all the credits
		NodeList creditList = document.getElementsByTagName("credit");
		for (int i = 0; i < creditList.getLength(); i++) {
			Element creditTag = (Element) creditList.item(i);
			String page = creditTag.getAttribute("page");
			if (page.isEmpty() || page.equals("1")) {
				// Page numbers and mini titles exist on pages 2+.
				// This means it is on page 1. Ignore.
				continue;
			}
			
			// Convert the attribute to an integer
			int pageNumber;
			try {
				pageNumber = Integer.parseInt(page);
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
			
			// Get the text content and check if it is a page number, a mini title, or neither
			Element candidateTag = (Element) creditWordsList.item(0);
			String content = candidateTag.getTextContent().trim();
			
			if (content.equals(page)) {
				// This is a page number. Ensure the formatting attributes.
				candidateTag.setAttribute("default-x", String.format("%.4f", isEvenPage ? evenHorizontal : oddHorizontal));
				candidateTag.setAttribute("default-y", String.format("%.4f", isEvenPage ? evenVertical : oddVertical));
				candidateTag.setAttribute("justify", isEvenPage ? "left" : "right");
				candidateTag.setAttribute("valign", "top");
				candidateTag.setAttribute("font-size", "14");
				
				// We've found a page number for this page, mark it as such.
				pageNumberExists[pageNumber - 1] = true;
				
			}
			else if (content.equals(title)) {
				// This is a mini title. Ensure the formatting attributes.
				candidateTag.setAttribute("default-x", String.format("%.4f", isEvenPage ? evenCenter : oddCenter));
				candidateTag.setAttribute("default-y", String.format("%.4f", isEvenPage ? evenVertical : oddVertical));
				candidateTag.setAttribute("justify", "center");
				candidateTag.setAttribute("valign", "top");
				candidateTag.setAttribute("font-size", "12");
				
				// We've found a page number for this page, mark it as such.
				miniTitleExists[pageNumber - 1] = true;
			}
			else {
				// This is neither. Ignore.
				continue;
			}
		}
		
		// At this point, proceed to add all page numbers and mini titles that don't exist.
		// Find the <part-list> tag (which must exist according to the schema). 
		// We will add the new credits before the <part-list> tag.
		NodeList partList = document.getElementsByTagName("part-list");
		
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
					// There already exists a page number of this page and we've already handled it. No need to add a new one.
					continue; 
				}
				
				// Create the parent <credit> tag
				Element pageNumberCreditTag = document.createElement("credit");
				pageNumberCreditTag.setAttribute("page", pageNumberText); // Set the page number of the credit
				
				// Add the (optional) <credit-type> child
				Element pageNumberCreditType = document.createElement("credit-type");
				pageNumberCreditType.setTextContent("page number");
				pageNumberCreditTag.appendChild(pageNumberCreditType);
				
				// Add the mandatory <credit-words> child
				Element pageNumberCreditWords = document.createElement("credit-words");
				pageNumberCreditWords.setTextContent(pageNumberText); // Set the text equal to the page number
				
				// Set all the attributes
				pageNumberCreditWords.setAttribute("default-x", String.format("%.4f", isEvenPage ? evenHorizontal : oddHorizontal));
				pageNumberCreditWords.setAttribute("default-y", String.format("%.4f", isEvenPage ? evenVertical : oddVertical));
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
					// There already exists a mini title of this page and we've already handled it. No need to add a new one.
					continue; 
				}
				
				// Create the parent <credit> tag
				Element pageNumberCreditTag = document.createElement("credit");
				pageNumberCreditTag.setAttribute("page", pageNumberText); // Set the page number of the credit
				
				// Add the (optional) <credit-type> child
				Element pageNumberCreditType = document.createElement("credit-type");
				pageNumberCreditType.setTextContent("title");
				pageNumberCreditTag.appendChild(pageNumberCreditType);
				
				// Add the mandatory <credit-words> child
				Element pageNumberCreditWords = document.createElement("credit-words");
				pageNumberCreditWords.setTextContent(title); // Set the text equal to the page number
				
				// Set all the attributes
				pageNumberCreditWords.setAttribute("default-x", String.format("%.4f", isEvenPage ? evenCenter : oddCenter));
				pageNumberCreditWords.setAttribute("default-y", String.format("%.4f", isEvenPage ? evenVertical : oddVertical));
				pageNumberCreditWords.setAttribute("justify", "center");
				pageNumberCreditWords.setAttribute("valign", "top");
				pageNumberCreditWords.setAttribute("font-size", "12");
				
				pageNumberCreditTag.appendChild(pageNumberCreditWords); // Add this as a child of the <credit> tag
				
				// Add it before the <part-list> tag
				partListTag.getParentNode().insertBefore(pageNumberCreditTag, partListTag);
			}
		}
	}
	
	/**
	 * Adds {@code <measure-numbering>system</measure-numbering>} to the first {@code <print>} tag to turn on system measure numbering.
	 * <br>
	 * This method will not add the tag if it detects that it already exists, but it will ensure the content of the tag is {@code system}.
	 * <br><br>
	 * If this method is unable to find any {@code <print>} tags, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	public static void addSystemMeasureNumbers(Document document) {
		if (document == null) {
			return; // Did you expect something *magical* to happen? :(
		}
		
		NodeList printList = document.getElementsByTagName("print");
		if (printList.getLength() == 0) {
			return; // We've got nothing to work with, so abort.
		}
		
		// Get the first <print> tag
		Element firstPrint = (Element) printList.item(0);
		
		// Check to see if the <measure-numbering> tag exists
		NodeList checkList = firstPrint.getElementsByTagName("measure-numbering");
		if (checkList.getLength() == 1) {
			// Ensure the measure-numbering is "system"
			checkList.item(0).setTextContent("system");
			
			// We're done.
			return; 
		}
		
		// Otherwise, the tag doesn't exist so we need to add it.
		Element measureNumberingTag = document.createElement("measure-numbering");
		measureNumberingTag.setTextContent("system");
		
		// The XML sequence order must be preserved, so we need to determine where the tag goes.
		// First, check if <part-name-display> tag exists.
		NodeList successorList = firstPrint.getElementsByTagName("part-name-display");
		if (successorList.getLength() == 0) {
			// <part-name-display> doesn't exist. Check if <part-abbreviation-display> exists.
			successorList = firstPrint.getElementsByTagName("part-abbreviation-display");
			if (successorList.getLength() == 0) {
				// <part-abbreviation-display> doesn't exist either.
				// At this point, it is safe to append <measure-numbering> tag to the end as no others exist
				firstPrint.appendChild(measureNumberingTag);
				
				// We've added it, so we're done.
				return;
			}
		}
		
		// At this point, either <part-name-display> exists or <part-name-display> doesn't exist but <part-abbreviation-display> does exist.
		// Either way, the tag is stored in the NodeList and so simply add the <measure-numbering> tag before these tags
		firstPrint.insertBefore(measureNumberingTag, successorList.item(0));
	}
	
	/**
	 * This method deletes any copyright information that appears on pages 2+. 
	 * It also sets the font size of the copyright information on page 1 to be 10 pt. font.
	 * <br><br>
	 * If this method is unable to fetch the copyright information, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	public static void removeDuplicateCopyrightInfo(Document document) {
		if (document == null) {
			return; // For real? :(
		}
		
		String copyrightInfo = getCopyrightInfo(document);
		
		if (copyrightInfo == null) {
			return; // We have nothing to work with, so return
		}
		
		// Get a list of all the <credit> tags
		NodeList nodeList = document.getElementsByTagName("credit");
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element creditElement = (Element) nodeList.item(i);
			String pageNumber = creditElement.getAttribute("page"); // Gets the page number of the credit. NOTE: This can be "" if assumed to be page 1
			
			// Fetch the <credit-words> child of the <credit> tag.
			// Unless there is an image here, there should be one and only one of these.
			NodeList subList = creditElement.getElementsByTagName("credit-words");
			
			// Get the text inside the <credit-words> tag
			Element creditWordsElement = (Element) subList.item(0);
			String content = creditWordsElement.getTextContent().trim();
			// Check if the content contains the copyright info and see if the associated page is not the first page
			if (content.equals(copyrightInfo)) {
				if (!pageNumber.isEmpty() && !pageNumber.equals("1")) {
					// This is redundant copyright information. Delete it.
					
					// If there is a newline and whitespace before this element, delete it.
					Node previousNode = creditElement.getPreviousSibling();
					if (previousNode.getNodeType() == Node.TEXT_NODE) {
						previousNode.getParentNode().removeChild(previousNode);
					}
					
					// Delete the text element.
					creditElement.getParentNode().removeChild(creditElement);
				}
				else {
					// This is the primary copyright information. 
					// While we have a handle on it, add the font-size attribute.
					creditWordsElement.setAttribute("font-size", "10");
				}
			}
		}
	}
	
	/**
	 * This method joins together a text direction marking with its neighboring tempo mark. 
	 * This method also fixes a bug where {@code default-x} is used instead of {@code relative-x} for either the tempo marking or the direction text.
	 * <br><br>
	 * If this method is unable to find any {@code <metronome>} tags, this method does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	public static void correctTempoMark(Document document) {
		if (document == null) {
			return; // Shocking. :(
		}
		
		// Search for all the <metronome> tags
		NodeList nodeList = document.getElementsByTagName("metronome");
		
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element metronomeTag = (Element) nodeList.item(i);
			Element directionTypeTag = (Element) metronomeTag.getParentNode();
			
			// Look for a previous <direction-type> that may contain the textual directions, such as a <words> direction
			Node previous = directionTypeTag.getPreviousSibling();
			while (previous != null && previous.getNodeType() != Node.ELEMENT_NODE) {
				previous = previous.getPreviousSibling();
			}
			
			if (previous == null) {
				// The metronome marking is first; there is no musical direction marking before it
				// Thus, we only need to replace the "default-x" attribute, if it exists, with "relative-x"
				// However, ensure there isn't a "relative-x" already that we would override
				// It seems that Finale will always ignore the "default-x" attribute, so it should be future-proof to replace
				String x = metronomeTag.getAttribute("default-x");
				if (!x.isEmpty() && !metronomeTag.hasAttribute("relative-x")) {
					metronomeTag.removeAttribute("default-x");
					metronomeTag.setAttribute("relative-x", x);
				}
				
				// And we're done.
			}
			else {
				// Check if there are words in this direction
				Element previousElement = (Element) previous;
				NodeList wordsList = previousElement.getElementsByTagName("words");
				
				if (wordsList.getLength() == 0) {
					// This is something else. I'm not sure what would be here, but I don't know how to handle it. Abort.
					continue;
				}
				else {
					// There is a directional marking with <words>. We've got stuff to do. 
					// First, get the first <words> tag - this should contain all the attributes we care about
					Element firstWords = (Element) wordsList.item(0);
					
					// Replace the "default-x" attribute with "relative-x", if it exists.
					// Also make sure there isn't a "relative-x" already that we would override
					String x = firstWords.getAttribute("default-x");
					if (!x.isEmpty() && !metronomeTag.hasAttribute("relative-x")) {
						firstWords.removeAttribute("default-x");
						firstWords.setAttribute("relative-x", x);
					}
					
//					firstWords.removeAttribute("font-size"); // Optional. Remove the font-size tag and let Finale decide its size
					
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
		}
		
		/* 
		 * XXX
		 * This method does not address if there is text after the tempo mark or if there are other <direction-types> in the <direction> tag.
		 * It is conceivable that other directional markings, such as dynamics, could suffer from the problem that this method fixes. To be investigated.
		 */
	}
	
	/**
	 * This takes any {@code <credit>} tag with the attribute {@code "justify"="center"} and centers it 
	 * (i.e. modifies the {@code default-x} attribute} with respect to the margins.
	 * This method will modify the attribute, even if the new values are the same as the old values.
	 * <br><br>
	 * If this method is unable to fetch the page margins or page width, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	public static void centerCreditsHorizontally(Document document)  {
		if (document == null) {
			return; // Are null objects ever safe to pass into a method modifying said object? :(
		}
		
		// Compute some commonly used values
		double[][] margins = getPageMargins(document); // REMINDER: left, right, top, bottom
		double pageWidth = getPageWidth(document);
		
		if (margins == null || pageWidth == -1)
			return; // We don't have what we need to do anything, so abort.
		
		// Compute the "default-x" values for the centered credit tags
		double evenCenter = (pageWidth - margins[0][1] + margins[0][0]) / 2.0;
		double oddCenter = (pageWidth - margins[1][1] + margins[1][0]) / 2.0;
		
		// Find all the <credit> tags
		NodeList creditList = document.getElementsByTagName("credit");
		
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
			
			// Check if there is a <credit-words> tag (there should be, and if there is, there must be exactly one of them)
			NodeList creditWordsList = creditElement.getElementsByTagName("credit-words");
			if (creditWordsList.getLength() != 0) {
				Element creditWordsElement = (Element) creditWordsList.item(0);
				// Check if this credit needs to be centered.
				if (creditWordsElement.getAttribute("justify").equals("center") || creditWordsElement.getAttribute("halign").equals("center")) {
					// Center it!
					creditWordsElement.setAttribute("default-x", String.format("%.4f", isEvenPage ? evenCenter : oddCenter));
				}
			}
		}
	}
	
	/**
	 * Saves the document to a file. This will override the contents of the provided file.
	 * @param document a validated MusicXML v3.1 document
	 * @param destinationFile the file to save to
	 * @throws IOException if there is an issue opening the {@code FileOutputStream}
	 * @throws TransformerException if there an issue creating or running the {@code Transformer}
	 */
	public static void writeToFile(Document document, File destinationFile) throws IOException, TransformerException {
		if (document == null) {
			throw new IllegalArgumentException("document provided was null"); // You are trying to save nothing? Why?! :(
		}
		else if (destinationFile == null) {
			throw new IllegalArgumentException("file provided was null"); // You are trying to save to nothing? Why?! :(
		}
		
		// Create a new factory and transformer
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		
		// Add 2 space indentation (will indent newly added tags)
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		// Set UTF-8 Encoded
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		// Add the <!doctype>
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Recordare//DTD MusicXML 3.1 Partwise//EN");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.musicxml.org/dtds/partwise.dtd");
		// Remove "standalone" attribute from <xml> tag
		document.setXmlStandalone(true); 
		
		// Export the data and open a file stream
		DOMSource source = new DOMSource(document);
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destinationFile), StandardCharsets.UTF_8);
		StreamResult result = new StreamResult(writer);
		
		// Write to the file
		transformer.transform(source, result);
		
		// Close the file
		writer.close();
		
		// Fin!
	}
	
}
