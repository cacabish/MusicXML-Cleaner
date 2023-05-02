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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A class that houses a parsed MusicXML document and various document-wide
 * utility functions.
 */
public final class MusicXMLDocument {

	private static final XPath XPATH = XPathFactory.newInstance().newXPath();
	private static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();

	/** The internal XML document */
	private final Document document;
	private final File originalFile;

	public MusicXMLDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		if (file == null) {
			throw new IllegalArgumentException("File provided was null"); // You should know better than that. :(
		}
		System.out.println("Loading file " + file.getName() + "...");

		this.originalFile = file;

		DBF.setValidating(true);
		DBF.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = DBF.newDocumentBuilder();

		// Construct an instance of a personal error handler to see if anything goes
		// wrong when the file gets validated
		XMLValidationErrorHandler errorHandler = new XMLValidationErrorHandler();
		builder.setErrorHandler(errorHandler);

		// Redirect the entity resolver to the local directory instead of online
		builder.setEntityResolver(new EntityResolver() {

			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				// Get the files from the local directory
				InputStream url = getClass().getResourceAsStream(
						"/musicxml-3.1/schema/" + systemId.substring(systemId.lastIndexOf("/") + 1));

				if (url != null) {
					// Return pointer to the file
					return new InputSource(url);
				} else {
					// Things went wrong
					throw new SAXException("Unrecognized System ID: " + systemId
							+ ". This probably means this isn't a MusicXML file.");
				}

			}
		});

		// Actually parse the file now
		Document document = builder.parse(file);

		// Check if all went well
		if (!errorHandler.isValid()) {
			// All did not go well
			throw errorHandler.getException();
		} else {
			// All is well
			System.out.println("Successfully constructed and validated the XML file!");
			this.document = document;
		}
	}

	/**
	 * Takes a MusicXML document and returns the number of millimeters per tenth.
	 * 
	 * @return the scaling factor of millimeters per tenth or -1 if it could not
	 *         find the value
	 */
	public double getMillimetersPerTenth() {
		NodeList nodeList = document.getElementsByTagName("scaling");
		if (nodeList.getLength() == 0) {
			// We couldn't find any <scaling> tags
			return -1;
		}

		// We have a scaling element. Hooray!
		Element scalingElement = (Element) nodeList.item(0);

		// Now, we need to find the millimeters tag and the tenths tag; both MUST exist
		// in a validated sheet.
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
			} else if (e.getTagName().equals("tenths")) {
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
	 * 
	 * @return the page height (in units) or -1 if it could not find the value
	 */
	public double getPageHeight() {
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
	 * 
	 * @return the page width (in units) or -1 if it could not find the value
	 */
	public double getPageWidth() {
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
	 * 
	 * @return an array of two double arrays or {@code null} if unsuccessful for any
	 *         reason. The first double array corresponds to the even-paged margins
	 *         and the second double array corresponds to the odd-paged margins. The
	 *         margins are in the following order for both: left, right, top,
	 *         bottom.
	 */
	public double[][] getPageMargins() {
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
				// These margins correspond to either even pages or odd pages. The order is not
				// specified in the schema.
				switch (element.getAttribute("type")) {
				case "even":
					evenMargins = currentMargins;
					break;
				case "odd":
					oddMargins = currentMargins;
					break;
				}
			} else {
				// Page Margins is either "type"=both or not provided, in which it is assumed to
				// be "type"=both
				evenMargins = currentMargins;
				oddMargins = currentMargins;
				break;
			}
		}

		// If either set of margins is uninitialized, there will be problems down the
		// line, so return null.
		if (evenMargins == null || oddMargins == null) {
			return null;
		}

		return new double[][] { evenMargins, oddMargins };
	}

	/**
	 * Takes a MusicXML document and returns the title of the score. This includes
	 * any quotation marks, if they are present. <br>
	 * <br>
	 * Note: this method checks that the title is either contained within the first
	 * {@code <work-title>} or {@code <movement-title>} tags. If neither of these
	 * are found, the method returns {@code null}, regardless of whether the title
	 * exists elsewhere.
	 * 
	 * @return the title of the score or {@code null} if unable to find either the
	 *         {@code <work-title>} or {@code <movement-title>} tags
	 */
	public String getTitle() {
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
	 * Takes a MusicXML document and returns the copyright information of the score.
	 * This includes any whitespace or newline characters. <br>
	 * <br>
	 * Note: this method checks that the copyright is either within the
	 * {@code <rights>} tags. If this tag doesn't exist, this method returns
	 * {@code null}, regardless of whether the copyright exists elsewhere.
	 * 
	 * @return the copyright information or {@code null} if unable to find
	 */
	public String getCopyrightInfo() {
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
	 * 
	 * @return the number of pages. Guaranteed to be at least 1 unless there are no
	 *         parts, then it returns -1.
	 */
	public int getNumberOfPages() {
		// Get all the parts
		NodeList partsList = document.getElementsByTagName("part");
		if (partsList.getLength() == 0) {
			return -1; // No parts, no dice.
		}

		// Get the first part
		Element firstPart = (Element) partsList.item(0);

		// Get all the <print> tags of the first part
		NodeList printTagList = firstPart.getElementsByTagName("print");

		int numberOfPages = 1; // There is at least one page
		for (int i = 0; i < printTagList.getLength(); i++) {
			Element printElement = (Element) printTagList.item(i);

			// If this <print> tag creates a new page, increment the count
			if (printElement.getAttribute("new-page").equals("yes")) {
				numberOfPages++;
			}
		}

		return numberOfPages;
	}

	/**
	 * Saves the last validated MusicXML document to the provided file. This will
	 * override the contents of the provided file. If the save is successful, the
	 * last validated MusicXML document is invalidated.
	 * 
	 * @param destinationFile the file to save to
	 * @throws IllegalArgumentException if there was no last validated MusicXML
	 *                                  document or the destination file is null.
	 * @throws IOException              if there is an issue opening the
	 *                                  {@code FileOutputStream}
	 * @throws TransformerException     if there an issue creating or running the
	 *                                  {@code Transformer}
	 */
	public void saveToFile(File destinationFile) throws IOException, TransformerException {
		if (destinationFile == null) {
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
		document.setXmlStandalone(true);

		// Export the data and open a file stream
		DOMSource source = new DOMSource(document);
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destinationFile),
				StandardCharsets.UTF_8);
		StreamResult result = new StreamResult(writer);

		// Write to the file
		transformer.transform(source, result);

		// Close the file
		writer.close();

		// Fin!
		System.out.println("Write successful!");
	}

	/**
	 * Returns a NodeList of all the Elements in document order with a given tag
	 * name and are contained in the document.
	 */
	public NodeList getElementsByTagName(String tagname) {
		return document.getElementsByTagName(tagname);
	}

	/** Creates an element of the type specified. */
	public Element createElement(String tagName) {
		return document.createElement(tagName);
	}

	/**
	 * Evaluates an XPath expression against the internal document and returns the
	 * result.
	 */
	public NodeList xPathEvaluation(String xPathExpression) throws XPathExpressionException {
		return (NodeList) XPATH.compile(xPathExpression).evaluate(document, XPathConstants.NODESET);
	}

}
