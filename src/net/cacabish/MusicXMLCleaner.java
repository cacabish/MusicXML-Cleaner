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
 * @version 1.2.0
 *
 */
public final class MusicXMLCleaner {
	
	/*
	 * =============================
	 * ===== OPERATIONAL FLAGS =====
	 * =============================
	 */
	
	/**
	 * A boolean flag signaling whether to add mini-titles and page numbers, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean addMiniTitlesAndPageNumbers     = true;
	/**
	 * A boolean flag signaling whether to add system measure numbers, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean addSystemMeasureNumbers         = true;
	/**
	 * A boolean flag signaling whether to remove extraneous copyright information, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean removeDuplicateCopyrightInfo    = true;
	/**
	 * A boolean flag signaling whether to correct all tempo markings, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean correctTempoMarking             = true;
	/**
	 * A boolean flag signaling whether to horizontally center all horizontally-aligned credits, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean centerCreditsHorizontally       = true;
	/**
	 * A boolean flag signaling whether to offset the system margins to align them with the left margin, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean offsetSystemMargins             = true;
	/**
	 * A boolean flag signaling whether to make repeat texts bold, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean makeRepeatTextsBold             = true;
	/**
	 * A boolean flag signaling whether to add periods to the end of numbers in voltas, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean addPeriodsToVoltaTexts          = true;
	/**
	 * A boolean flag signaling whether to add the swung eighths symbol wherever this is a "Swing" direction, when an XML is cleaned.
	 * Default = true.
	 */
	public static boolean addSwing8thsWhereSwingDirection = true;


	/*
	 * ===============================
	 * ===== MISC. STATIC FIELDS =====
	 * ===============================
	 */
	
	/**
	 * The validated document. The document is cached here after cleaning until either saved or replaced by a new document.
	 */
	private static Document validatedDoc = null;
	
	/**
	 * The document builder factory to construct {@code DocumentBuilder} objects from. Statically initialized.
	 */
	private static final DocumentBuilderFactory DBF;
	
	
	
	/**
	 * The static method that initializes the DocumentBuilderFactory.
	 */
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
	 * Performs all operations whose boolean flags are set to true.
	 * @param file the file to clean
	 * @throws ParserConfigurationException throws if there is a problem getting a new {@code DocumentBuilder}
	 * @throws SAXException throws if there is an error when parsing or validating the file
	 * @throws IOException throws if there is an I/O error resulting from parsing the file
	 */
	public static void cleanMusicXMLFile(File file) throws ParserConfigurationException, SAXException, IOException {
		System.out.println("");
		System.out.println("===== Beginning New Cleaning Job =====");
		
		validatedDoc = MusicXMLCleaner.constructAndValidateMusicXMLDocument(file);
		
		// Do the cleaning!
		if (addMiniTitlesAndPageNumbers)
			MusicXMLCleaner.addPageNumbersAndMiniTitles(validatedDoc);
		if (addSystemMeasureNumbers)
			MusicXMLCleaner.addSystemMeasureNumbers(validatedDoc);
		if (removeDuplicateCopyrightInfo)
			MusicXMLCleaner.removeDuplicateCopyrightInfo(validatedDoc);
		if (correctTempoMarking)
			MusicXMLCleaner.correctTempoMark(validatedDoc);
		if (centerCreditsHorizontally)
			MusicXMLCleaner.centerCreditsHorizontally(validatedDoc);
		if (offsetSystemMargins)
			MusicXMLCleaner.offsetSystemMarginsToAlignWithLeftMargin(validatedDoc);
		if (makeRepeatTextsBold)
			MusicXMLCleaner.makeRepeatTextsBold(validatedDoc);
		if (addPeriodsToVoltaTexts)
			MusicXMLCleaner.addPeriodsToVoltaTexts(validatedDoc);
		if (addSwing8thsWhereSwingDirection)
			MusicXMLCleaner.addSwing8thsWhereSwingDirection(validatedDoc);
		
		System.out.println("===== End New Cleaning Job =====");
	}
	
	
	/**
	 * Constructs an XML DOM object, given a file, and validates it against the MusicXML 3.1 schema.
	 * @param file the file to be parsed
	 * @return the DOM object
	 * @throws ParserConfigurationException throws if there is a problem getting a new {@code DocumentBuilder}
	 * @throws SAXException throws if there is an error when parsing or validating the file
	 * @throws IOException throws if there is an I/O error resulting from parsing the file
	 */
	private static Document constructAndValidateMusicXMLDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		if (file == null)
			throw new IllegalArgumentException("file provided was null"); // You should know better than that. :(
		System.out.println("Loading file " + file.getName() + "...");
		
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
			System.out.println("Successfully constructed and validated the XML file!");
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
	 * Converts millimeters to inches.
	 * @param millimeters the millimeters to convert
	 * @return the number of inches equal to the provided millimeters
	 */
	private static double millimetersToInches(double millimeters) {
		return millimeters / 25.4;
	}
	
	/**
	 * Converts inches to millimeters.
	 * @param inches the inches to convert
	 * @return the number of millimeters equal to the provided inches
	 */
	private static double inchesToMillimeters(double inches) {
		return inches * 25.4;
	}
	
	/**
	 * Takes a MusicXML document and returns the number of millimeters per tenth.
	 * @param document a validated MusicXML v3.1 document
	 * @return the scaling factor of millimeters per tenth or -1 if it could not find the value
	 */
	private static double getMillimetersPerTenth(Document document) {
		if (document == null) {
			return -1; // *le sigh*
		}
		
		NodeList nodeList = document.getElementsByTagName("scaling");
		if (nodeList.getLength() == 0) {
			// We couldn't find any <scaling> tags
			return -1;
		}
		
		// We have a scaling element. Hooray!
		Element scalingElement = (Element) nodeList.item(0);
		
		// Now, we need to find the millimeters tag and the tenths tag; both MUST exist in a validated sheet.
		NodeList childNodes = scalingElement.getChildNodes();
		
		double millimeters = -1;
		double tenths = -1;
		
		// Iterate over the child nodes of the <scaling> tag
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue; // If this isn't an element node, we don't care about it.
			
			// This is an element, so cast it!
			Element e = (Element) node;
			if (e.getTagName().equals("millimeters")) {
				millimeters = Double.parseDouble(e.getTextContent()); // This is the millimeters value.
			}
			else if (e.getTagName().equals("tenths")) {
				tenths = Double.parseDouble(e.getTextContent()); // This is the tenths value.
			}
		}
		
		if (millimeters < 0 || tenths < 0)
			return -1; // Something bad happened and the tags don't exist. Abort.
		
		// Return the ratio
		return millimeters / tenths;
	}
	
	/**
	 * Takes a MusicXML document and returns the page height (in units).
	 * @param document a validated MusicXML v3.1 document
	 * @return the page height (in units) or -1 if it could not find the value
	 */
	private static double getPageHeight(Document document) {
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
	private static double getPageWidth(Document document) {
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
	private static double[][] getPageMargins(Document document) {
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
	private static String getTitle(Document document) {
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
	private static String getCopyrightInfo(Document document) {
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
	private static int getNumberOfPages(Document document) {
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
	private static void addPageNumbersAndMiniTitles(Document document) {
		System.out.println("Adding page numbers and mini-titles...");
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
		// For both of these arrays, the index corresponds to the (page number - 1) and is true if and only if the respective credit was found
		// Because we don't check credits on the first page, both of the indices at 0 are always false and are to be ignored.
		boolean[] pageNumberExists = new boolean[numberOfPages];
		boolean[] miniTitleExists = new boolean[numberOfPages];
		
		// Fetch all the credits
		NodeList creditList = document.getElementsByTagName("credit");
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
			
			// Get the text content and check if it is a page number, a mini title, or neither
			Element candidateTag = (Element) creditWordsList.item(0);
			String content = candidateTag.getTextContent().trim();
			
			if (content.equals(pageAttribute)) {
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
		
		System.out.println("All done adding page numbers and mini-titles!");
	}
	
	/**
	 * Adds {@code <measure-numbering>system</measure-numbering>} to the first {@code <print>} tag to turn on system measure numbering.
	 * <br>
	 * This method will not add the tag if it detects that it already exists, but it will ensure the content of the tag is {@code system}.
	 * <br><br>
	 * If this method is unable to find any {@code <print>} tags, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void addSystemMeasureNumbers(Document document) {
		System.out.println("Adding measure numbers...");
		if (document == null) {
			return; // Did you expect something *magical* to happen? :(
		}
		
		NodeList printList = document.getElementsByTagName("print");
		if (printList.getLength() == 0) {
			System.out.println("No print tag detected. Aborting.");
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
			System.out.println("Measure numbers already exist.");
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
				System.out.println("Done adding measure numbers!");
				return;
			}
		}
		
		// At this point, either <part-name-display> exists or <part-name-display> doesn't exist but <part-abbreviation-display> does exist.
		// Either way, the tag is stored in the NodeList and so simply add the <measure-numbering> tag before these tags
		firstPrint.insertBefore(measureNumberingTag, successorList.item(0));
		System.out.println("Done adding measure numbers!");
	}
	
	/**
	 * This method deletes any copyright information that appears on pages 2+. 
	 * It also sets the font size of the copyright information on page 1 to be 10 pt. font.
	 * <br><br>
	 * If this method is unable to fetch the copyright information, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void removeDuplicateCopyrightInfo(Document document) {
		System.out.println("Removing duplicate copyright info...");
		if (document == null) {
			return; // For real? :(
		}
		
		String copyrightInfo = getCopyrightInfo(document);
		
		if (copyrightInfo == null) {
			System.out.println("Unable to fetch copyright info. Aborting.");
			return; // We have nothing to work with, so return
		}
		
		// Get a list of all the <credit> tags
		NodeList nodeList = document.getElementsByTagName("credit");
		
		// Iterate over the list backwards since we are deleting elements and want to avoid a concurrency issue.
		for (int i = nodeList.getLength() - 1; i >= 0; i--) {
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
					if (previousNode != null && previousNode.getNodeType() == Node.TEXT_NODE) {
						previousNode.getParentNode().removeChild(previousNode);
					}
					
					// Delete the text element.
					creditElement.getParentNode().removeChild(creditElement);
				}
				else {
					// This is the primary copyright information. 
					// While we have a handle on it, add the font-size attribute.
					creditWordsElement.setAttribute("font-size", "10");
					System.out.println("Font size corrected on primary copyright information.");
				}
			}
		}
		
		System.out.println("All done removing duplicate copyright information!");
	}
	
	/**
	 * This method joins together a text direction marking with its neighboring tempo mark. 
	 * This method also fixes a bug where {@code default-x} is used instead of {@code relative-x} for either the tempo marking or the direction text.
	 * <br><br>
	 * If this method is unable to find any {@code <metronome>} tags, this method does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void correctTempoMark(Document document) {
		System.out.println("Correcting the tempo mark...");
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
					System.out.println("Unexpected element before metronome mark. Skipping.");
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
			
			System.out.println("All finished correcting tempo markings!");
		}
		
		/* 
		 * XXX
		 * This method does not address if there is text after the tempo mark or if there are other <direction-types> in the <direction> tag.
		 * It is conceivable that other directional markings, such as dynamics, could suffer from the problem that this method fixes. To be investigated.
		 */
	}
	
	/**
	 * This takes any {@code <credit>} tag with the attribute {@code "justify"="center"} or {@code "halign"="center"} and centers it 
	 * (i.e. modifies the {@code default-x} attribute} with respect to the margins.
	 * This method will modify the attribute, even if the new values are the same as the old values.
	 * <br><br>
	 * If this method is unable to fetch the page margins or page width, this method returns immediately and does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void centerCreditsHorizontally(Document document) {
		System.out.println("Centering all relevant text...");
		if (document == null) {
			return; // Are null objects ever safe to pass into a method modifying said object? :(
		}
		
		// Compute some commonly used values
		double[][] margins = getPageMargins(document); // REMINDER: left, right, top, bottom
		double pageWidth = getPageWidth(document);
		
		if (margins == null || pageWidth == -1) {
			System.out.println("Missing either the margins or the page width. Aborting.");
			return; // We don't have what we need to do anything, so abort.
		}
		
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
		
		System.out.println("Done centering all relevant credits!");
	}
	
	/**
	 * Offsets each system's left margin by the smallest left margin found. 
	 * If the offset renders the new margin relatively close to 0 (within some epsilon), then the <system-margins> tag is deleted.
	 * <br><br>
	 * The method will not touch any systems that are poorly formatted. If all systems are poorly formatted, this method does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void offsetSystemMarginsToAlignWithLeftMargin(Document document) {
		System.out.println("Left aligning systems with the left margins...");
		if (document == null) {
			return; // Bruh. :(
		}
		
		// Fetch all the <system-layout> tags
		NodeList allSystemLayoutTags = document.getElementsByTagName("system-layout");
		
		// We need to find the minimum left-margin value
		double minimumValue = Double.MAX_VALUE;
		
		// Iterate over all the <system-layout> tags
		for (int i = 0; i < allSystemLayoutTags.getLength(); i++) {
			Element systemLayoutElement = (Element) allSystemLayoutTags.item(i);
			
			// Fetch the <system-margins> child of the <system-layout> tag.
			NodeList systemMarginsTags = systemLayoutElement.getElementsByTagName("system-margins");
			
			// We will be operating on a single <system-margins> tag, so we need to use that.
			if (systemMarginsTags.getLength() != 1) {
				// We don't need to do anything because this is either a bug (if > 1) or this doesn't exist (if == 0), which can happen according to the schema
				continue;
			}
			
			// Since we know there is exactly one child, fetch it.
			Element systemMarginsElement = (Element) systemMarginsTags.item(0);
			
			// Great! Now, we need to get the <left-margin> child of the <system-margins> tag. According to the schema, it MUST exist and there is exactly one of them.
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
			}
			catch (NumberFormatException e) {
				// This tag errantly contains a non-number (why did this happen??), so ignore this.
				continue;
			}
			
		}
		
		
		// Worst case check
		if (minimumValue == Double.MAX_VALUE) {
			// Nothing changed? Strange, but we can't do anything with this. Return.
			return;
		}
		
		
		// At this point, we have determined what the smallest left margin is. Hoo-ray! 
		// Now, we're going to zero out all systems that we can and offset what should be the first system (or any other indented system)
		
		// The absolute threshold to be constituted as 0 margin and warrant deletion.
		final double epsilon = 0.1;
		
		// Iterate over all the <system-layout> tags again!
		// Iterate over it backwards since we are potentially deleting elements and want to avoid a concurrency issue.
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
			
			// Great! Now, we need to get the <left-margin> child of the <system-margins> tag. According to the schema, it MUST exist and there is exactly one of them.
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
				
				// Now, check for an approximate zero. Since we are handling floating-point numbers, we use a threshold epsilon.
				if (Math.abs(newValue) < epsilon) {
					// This tag is effectively a 0 margin, so delete the <system-margins> tag and its two subtags.
					// Delete the element.
					systemMarginsElement.getParentNode().removeChild(systemMarginsElement);
				}
				else {
					// This system is indented some, so we just change the indentation.
					leftMarginsElement.setTextContent(String.format("%.2f", newValue)); // Since MuseScore uses 2 decimal places of accuracy, so will I.
				}
			}
			catch (NumberFormatException e) {
				// This tag errantly contains a non-number (why did this happen??), so ignore this.
				continue;
			}
		}
		
		System.out.println("Done left-aligning all non-indented systems and offsetting those that are indented!");
	}
	
	/**
	 * This makes any repeat texts bold. If there aren't any, then this method will fall through.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void makeRepeatTextsBold(Document document) {
		System.out.println("Making repeat texts bold...");
		if (document == null) {
			return; // C'mon, man... :(
		}
		
		// To make sure we are dealing with actual ending texts and not just textual directions that say the words, we search for the <sound> tags first
		NodeList soundNodes = document.getElementsByTagName("sound");
		for (int i = 0; i < soundNodes.getLength(); i++) {
			Element soundElement = (Element) soundNodes.item(i);
			
			// We're first going to check that this is inside a <direction> tag.
			Node parentNode = soundElement.getParentNode();
			
			// If there is no parent tag, it's not an element, or isn't a <direction> tag, skip it.
			if (parentNode == null || parentNode.getNodeType() != Node.ELEMENT_NODE || !((Element) parentNode).getTagName().equalsIgnoreCase("direction")) {
				continue;
			}
			
			// At this point, we can assume this sound tag is inside a <direction> tag. Great! Now, let's check that it actually pertains to repeats.
			if (soundElement.getAttribute("dacapo").equalsIgnoreCase("yes")           // Is this a "D.C. ______"?
					|| soundElement.getAttribute("dalsegno").equalsIgnoreCase("yes")  // Is this a "D.S. ______"?
					|| soundElement.getAttribute("fine").equalsIgnoreCase("yes")      // Is this a "Fine"?
					|| soundElement.getAttribute("tocoda").equalsIgnoreCase("yes"))   // Is this a "To Coda"?
			{
				// It passed one of tests! Fantastic! Almost done now. We just need the <words> tag.
				NodeList wordsList = ((Element) parentNode).getElementsByTagName("words");
				
				for (int j = 0; j < wordsList.getLength(); j++) {
					Element wordElement = (Element) wordsList.item(j);
					
					// Set the font-weight to bold.
					wordElement.setAttribute("font-weight", "bold");
				}
				
			}
			else {
				// It's not something we're looking for. Continue.
				continue;
			}
		}
		
		System.out.println("Successfully made repeat texts bold!");
	}
	
	/**
	 * This adds periods to the end of every numbered ending included in a volta.
	 * If there are no voltas, then this method does nothing.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void addPeriodsToVoltaTexts(Document document) {
		System.out.println("Adding periods after volta numbers...");
		if (document == null) {
			return; // Null, null, always null... :(
		}
		
		// Fetch all the <ending> tags
		NodeList endingNodes = document.getElementsByTagName("ending");
		for (int i = 0; i < endingNodes.getLength(); i++) {
			Element endingElement = (Element) endingNodes.item(i);
			
			// Fetch the number attribute
			String numberAttribute = endingElement.getAttribute("number");
			
			// Put this in a StringBuilder so it's easy to insert characters.
			StringBuilder builder = new StringBuilder(numberAttribute);
			
			// A flag to keep track of whether we have seen a digit or not.
			boolean hasSeenDigit = false;
			// Iterate over the string backwards so indices remain constant.
			for (int j = numberAttribute.length() - 1; j >= 0; j--) {
				char c = numberAttribute.charAt(j);
				if (Character.isDigit(c)) {
					// We are looking at a digit
					// If we haven't seen a digit whilst scanning rightwards, this must be the end of the number so add a period.
					if (!hasSeenDigit) {
						// However, for forwards compatibility, if the character after is actually a period, then don't add it.
						// To do this, we check so that if we're at the end of the string or it there isn't a period after this character, then insert a period.
						if (j + 1 >= numberAttribute.length() || numberAttribute.charAt(j + 1) != '.') {
							builder.insert(j + 1, '.');
						}
					}
					
					// Update the flag because we have seen a digit.
					hasSeenDigit = true;
				}
				else {
					// This isn't a digit, so we have no longer seen a digit
					hasSeenDigit = false;
				}
			}
			
			// Set this as the <ending> tag's content.
			endingElement.setTextContent(builder.toString());
		}
		
		System.out.println("Successfully added periods after volta numbers!");
	}
	
	
	/**
	 * This method will add the symbol of two eighths notes being equal to a quarter-note-eighth-note triplet wherever there is a "Swing" direction.
	 * If there are no such swing directions, this method does nothing.
	 * Repeatedly using this method will repeatedly add more symbols.
	 * @param document a validated MusicXML v3.1 document
	 */
	private static void addSwing8thsWhereSwingDirection(Document document) {
		System.out.println("Adding swing 8ths where there is a \"Swing\" direction...");
		if (document == null) {
			return; // Surely you would know not to do this by now, right??? :(
		}
		
		// Iterate over the <words> tags.
		NodeList wordsList = document.getElementsByTagName("words");
		for (int i = 0; i < wordsList.getLength(); i++) {
			Element wordsElement = (Element) wordsList.item(i);
			
			Element directionTypeTag = (Element) wordsElement.getParentNode(); // <words> elements are always children of a <direction-type> tag
			Element nextSibling = (Element) directionTypeTag.getNextSibling(); // Get the next sibling of this tag. Will be null if doesn't exist!
			Element directionTag = (Element) directionTypeTag.getParentNode(); // <direction-type> elements are always children of a <direction> tag
			
			// Check if the text is "Swing"
			if (wordsElement.getTextContent().trim().equalsIgnoreCase("swing")) {
				// We need to add all the tags that will generate the swing rhythm notation
				
				/*
				 * Here is the full tag we need to add to the <direction> tag.
				 * 
				 * <direction-type>
				 *    <metronome>
				 *       <metronome-note>
				 *          <metronome-type>eighth</metronome-type>
				 *          <metronome-beam number="1">begin</metronome-beam>
				 *       </metronome-note>
				 *       <metronome-note>
				 *          <metronome-type>eighth</metronome-type>
				 *          <metronome-beam number="1">end</metronome-beam>
				 *       </metronome-note>
				 *       <metronome-relation>equals</metronome-relation>
				 *       <metronome-note>
				 *          <metronome-type>quarter</metronome-type>
				 *          <metronome-tuplet bracket="yes" show-number="actual" type="start">
				 *             <actual-notes>3</actual-notes>
				 *             <normal-notes>2</normal-notes>
				 *             <normal-type>eighth</normal-type>
				 *          </metronome-tuplet>
				 *       </metronome-note>
				 *       <metronome-note>
				 *          <metronome-type>eighth</metronome-type>
				 *          <metronome-tuplet type="stop">
				 *             <actual-notes>3</actual-notes>
				 *             <normal-notes>2</normal-notes>
				 *             <normal-type>eighth</normal-type>
				 *          </metronome-tuplet>
				 *       </metronome-note>
				 *    </metronome>
				 * </direction-type>
				 * 
				 * Source: https://www.w3.org/2021/06/musicxml40/musicxml-reference/examples/metronome-note-element/
				 */
				
				// Create the root <direction-type> and <metronome> tags.
				Element newDirectionTypeTag = document.createElement("direction-type");
				Element metronomeTag = document.createElement("metronome");
				{
					// Everything in this scope is a descendent of the <metronome> tag
				
					// Add the first eighth note
					Element metronomeNote1Tag = document.createElement("metronome-note");
					{
						Element metronomeTypeTag = document.createElement("metronome-type");
						metronomeTypeTag.setTextContent("eighth"); // It's an eighth note
						metronomeNote1Tag.appendChild(metronomeTypeTag);
						
						// Begin the eighth note beam that will connect the two eighth notes
						Element metronomeBeamTag = document.createElement("metronome-beam");
						metronomeBeamTag.setAttribute("number", "1");
						metronomeBeamTag.setTextContent("begin");
						metronomeNote1Tag.appendChild(metronomeBeamTag);
					}
					// Add this note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote1Tag);
					
					// Add the second eighth note
					Element metronomeNote2Tag = document.createElement("metronome-note");
					{
						
						Element metronomeTypeTag = document.createElement("metronome-type");
						metronomeTypeTag.setTextContent("eighth"); // It's another eighth note
						metronomeNote2Tag.appendChild(metronomeTypeTag);
						
						// End the eighth note beam that will connect the two eighth notes
						Element metronomeBeamTag = document.createElement("metronome-beam");
						metronomeBeamTag.setAttribute("number", "1");
						metronomeBeamTag.setTextContent("end");
						metronomeNote2Tag.appendChild(metronomeBeamTag);
					}
					// Add this note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote2Tag);
					
					// Create the "=" tag that goes in the middle
					Element metronomeRelationTag = document.createElement("metronome-relation");
					metronomeRelationTag.setTextContent("equals");
					metronomeTag.appendChild(metronomeRelationTag);
					
					// Add the first quarter note in the tuple
					Element metronomeNote3Tag = document.createElement("metronome-note");
					{
						Element metronomeTypeTag = document.createElement("metronome-type");
						metronomeTypeTag.setTextContent("quarter"); // It's a quarter note
						metronomeNote3Tag.appendChild(metronomeTypeTag);
						
						// Signify this is part of a tuplet
						Element metronomeTupletTag = document.createElement("metronome-tuplet");
						// We are starting the tuplet
						metronomeTupletTag.setAttribute("bracket", "yes");
						metronomeTupletTag.setAttribute("show-number", "actual");
						metronomeTupletTag.setAttribute("type", "start");
						{
							// 3 eights...
							Element actualNotesTag = document.createElement("actual-notes");
							actualNotesTag.setTextContent("3");
							metronomeTupletTag.appendChild(actualNotesTag);
							
							// .. are actually equal to 2.
							Element normalNotesTag = document.createElement("normal-notes");
							normalNotesTag.setTextContent("2");
							metronomeTupletTag.appendChild(normalNotesTag);
							
							// Did I mention they were eighths?
							Element normalTypeTag = document.createElement("normal-type");
							normalTypeTag.setTextContent("eighth");
							metronomeTupletTag.appendChild(normalTypeTag);
						}
						// Add this tuple to the note
						metronomeNote3Tag.appendChild(metronomeTupletTag);
					}
					// Add this tupled quarter note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote3Tag);
					
					// Add the eighth note in the tuple
					Element metronomeNote4Tag = document.createElement("metronome-note");
					{
						Element metronomeTypeTag = document.createElement("metronome-type");
						metronomeTypeTag.setTextContent("eighth"); // It's an eighth note
						metronomeNote4Tag.appendChild(metronomeTypeTag);
						
						// Signify this is part of a tuplet
						Element metronomeTupletTag = document.createElement("metronome-tuplet");
						// We are ending the tuplet with this note
						metronomeTupletTag.setAttribute("type", "stop");
						{
							// 3 eights...
							Element actualNotesTag = document.createElement("actual-notes");
							actualNotesTag.setTextContent("3");
							metronomeTupletTag.appendChild(actualNotesTag);
							
							// .. are actually equal to 2.
							Element normalNotesTag = document.createElement("normal-notes");
							normalNotesTag.setTextContent("2");
							metronomeTupletTag.appendChild(normalNotesTag);
							
							// Did I mention they were eighths?
							Element normalTypeTag = document.createElement("normal-type");
							normalTypeTag.setTextContent("eighth");
							metronomeTupletTag.appendChild(normalTypeTag);
						}
						// Add this tuple to the note
						metronomeNote4Tag.appendChild(metronomeTupletTag);
					}
					// Add this tupled eighth note to the <metronome> tag
					metronomeTag.appendChild(metronomeNote4Tag);
				}
				// Finally, add the <metronome> tag as the child of the new <direction-type> tag.
				newDirectionTypeTag.appendChild(metronomeTag);
				
				
				// Alright! Now, we just need to add this as a child. 
				// Since we are using a <direction-type> tag as a base, it can either come before or after it.
				// I'm electing to add it after, which means there are two cases.
				// Either there is another tag afterwards, in which case we add it before that...
				// ...or we just add it at the end because there is nothing to add.
				if (nextSibling == null) {
					// Just stick it at the end (i.e. right after the previous <direction-type> tag)
					directionTag.appendChild(newDirectionTypeTag);
				}
				else {
					// Add it before the next sibling (i.e. right after the previous <direction-type> tag)
					directionTag.insertBefore(newDirectionTypeTag, nextSibling);
				}
				
				// Whew! We're done.
			}
		}
		
		System.out.println("Finished adding swing 8ths where there is a \"Swing\" direction!");
	}
	
	/**
	 * Saves the last validated MusicXML document to the provided file.
	 * This will override the contents of the provided file.
	 * If the save is successful, the last validated MusicXML document is invalidated.
	 * @param destinationFile the file to save to
	 * @throws IllegalArgumentException if there was no last validated MusicXML document or the destination file is null.
	 * @throws IOException if there is an issue opening the {@code FileOutputStream}
	 * @throws TransformerException if there an issue creating or running the {@code Transformer}
	 */
	public static void writeToFile(File destinationFile) throws IOException, TransformerException {
		if (validatedDoc == null) {
			throw new IllegalArgumentException("there was no document to save"); // You are trying to save nothing? Why?! :(
		}
		else if (destinationFile == null) {
			throw new IllegalArgumentException("file provided was null"); // You are trying to save to nothing? Why?! :(
		}
		System.out.println("Writing to file " + destinationFile + "...");
		
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
		validatedDoc.setXmlStandalone(true); 
		
		// Export the data and open a file stream
		DOMSource source = new DOMSource(validatedDoc);
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destinationFile), StandardCharsets.UTF_8);
		StreamResult result = new StreamResult(writer);
		
		// Write to the file
		transformer.transform(source, result);
		
		// Close the file
		writer.close();
		
		// Now that the file has been saved, invalidate the document so the next run cannot use the old document.
		validatedDoc = null;
		
		// Fin!
		System.out.println("Write successful!");
	}
	
}
