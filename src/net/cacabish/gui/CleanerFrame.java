package net.cacabish.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import net.cacabish.MusicXMLCleaner;

/**
 * The main GUI for processing MusicXML files.
 * @author cacabish
 * @version v1.5.0
 *
 */
public class CleanerFrame extends JFrame {

	private static final long serialVersionUID = 7520073032182546710L;
	
	public static final String VERSION = "v1.5.0";
	public static final String MUSESCORE_VERSION = "v4.5.1";

	private final JPanel contentPane;
	
	/**
	 * The {@code FileFilter} which accepts files ending in .musicxml or .xml.
	 */
	private final FileFilter fileFilter = new FileFilter() {
		
		@Override
		public String getDescription() {
			return "Uncompressed MusicXML files (*.musicxml, *.xml)";
		}
		
		/**
		 * Fetch the file extension of the file.
		 * @param f the file
		 * @return the file extension of the file. If unsuccessful, this returns an empty string.
		 */
		public String getExtension(File f) {
	        String ext = "";
	        String s = f.getName();
	        int i = s.lastIndexOf('.'); // Find the last period

	        // Check if it's in bounds
	        if (i > 0 && i < s.length() - 1) {
	            ext = s.substring(i + 1).toLowerCase(); // Get the substring after the last period
	        }
	        return ext;
	    }
		
		@Override
		public boolean accept(File f) {
			if (f.isDirectory())
				return true; // Always accept directories
			
			// Get the extension and see if it's what we want
			String ext = getExtension(f);
			if (ext != null && (ext.equalsIgnoreCase("musicxml") || ext.equalsIgnoreCase("xml"))) {
				return true;
			}
			else {
				return false; // It's not.
			}
		}
	};

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		System.out.println("Hello world!");
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CleanerFrame frame = new CleanerFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public CleanerFrame() {
		/* 
		 * ==========================
		 * ===== BASIC SETTINGS =====
		 * ==========================
		 */
		setTitle("MusicXML Cleaner " + VERSION + " for MuseScore " + MUSESCORE_VERSION);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 550, 450); // Set initial size parameters
		setLocationRelativeTo(null); // Centers to the screen
		
		
		
		
		
		/* 
		 * ====================
		 * ===== MENU BAR =====
		 * ====================
		 */
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu menuFile = new JMenu("File");
		menuBar.add(menuFile);
		
		JMenuItem menuItemOpenFile = new JMenuItem("Open .musicxml File...");
		menuItemOpenFile.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				showFileChooserAndLoad();
			}
		});
		menuFile.add(menuItemOpenFile);
		
		JSeparator separator = new JSeparator();
		menuFile.add(separator);
		
		JMenuItem menuItemExit = new JMenuItem("Exit");
		menuItemExit.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuFile.add(menuItemExit);
		
		JMenu menuHelp = new JMenu("Help");
		menuBar.add(menuHelp);
		
		JMenuItem menuItemHowTo = new JMenuItem("How to Use");
		menuItemHowTo.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(contentPane, 
						"First, export your MuseScore sheet as a .musicxml file." 
								+ System.lineSeparator() +
						"This can be loading your sheet in MuseScore. Go to File => Export." 
								+ System.lineSeparator() +
						"Then change the \"Save as type\" to \"Uncompressed MusicXML File (*.musicxml)\" and Save." 
							+ System.lineSeparator() + System.lineSeparator() +
						"Next, load up this program."
							+ System.lineSeparator() +
						"Check the boxes corresponding to the operations you'd like done on the sheet."
							+ System.lineSeparator() +
						"Then, either drag-and-drop the newly exported file onto the window or click the button below and select the file." 
							+ System.lineSeparator() +
						"Finally, let the program work its magic and then it will prompt you to save the cleaned file. "
							+ System.lineSeparator() +
						"You may override the old file or create a new one."
							+ System.lineSeparator() + System.lineSeparator() +
						"That's it! You can now import into Finale with many annoying bugs fixed! :D"
				, "How To Use", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menuHelp.add(menuItemHowTo);
		
		JSeparator separator1 = new JSeparator();
		menuHelp.add(separator1);
		
		JMenuItem menuItemAbout = new JMenuItem("About");
		menuItemAbout.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(contentPane, new AboutPanel()
				, "About", JOptionPane.PLAIN_MESSAGE);
			}
		});
		menuHelp.add(menuItemAbout);
		
		JButton newVersionButton = new JButton("New Version Available!");
		newVersionButton.setVisible(false);
		menuBar.add(newVersionButton);

		
		
		
		
		/* 
		 * ==========================
		 * ===== WINDOW CONTENT =====
		 * ==========================
		 */
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		contentPane.setDropTarget(new DropTarget() {
			
			private static final long serialVersionUID = 3597214483598747225L;

			@Override
			public synchronized void drop(DropTargetDropEvent event) {
				event.acceptDrop(DnDConstants.ACTION_COPY);
				try {
					@SuppressWarnings("unchecked")
					List<File> droppedFiles = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					
					// Process all the files that were dropped on the GUI
					for (File file : droppedFiles) {
						loadFile(file);
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(contentPane, "Drag and Drop was unsuccessful. Reason: " 
							+ e.getMessage() + System.lineSeparator() +
							"Try using the button instead.", "Drag and Drop Failed", JOptionPane.ERROR_MESSAGE);
				} 
			}
		});
		
		setContentPane(contentPane);
		
		JLabel lblDragAndDrop = new JLabel("<html>Drag and drop a .musicxml file to convert here<br> or use the button below.</html>");
		lblDragAndDrop.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblDragAndDrop, BorderLayout.CENTER);
		
		JPanel southPanel = new JPanel();
		contentPane.add(southPanel, BorderLayout.SOUTH);
		
		// Open File Button
		JButton btnOpenFile = new JButton("Choose .musicxml File...");
		btnOpenFile.setToolTipText("Opens a file to be processed.");
		btnOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showFileChooserAndLoad();
			}
		});
		southPanel.add(btnOpenFile);
		
		
		
		/* 
		 * ============================
		 * ===== OPERATIONS PANEL =====
		 * ============================
		 */
		
		JPanel operationsPanel = new JPanel();
//		operationsPanel.setBorder(
//				new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Operations to Perform", TitledBorder.LEADING, TitledBorder.TOP, null, null)
//				);
//		contentPane.add(operationsPanel, BorderLayout.WEST);
		operationsPanel.setLayout(new BoxLayout(operationsPanel, BoxLayout.Y_AXIS));
		
		/*
		 * Add mini-titles and page numbers checkbox
		 */
		JCheckBox chckbxAddMiniTitles = new JCheckBox("Add Mini-titles & Page Numbers");
		chckbxAddMiniTitles.setToolTipText("If checked, the program will add mini-title and page numbers to pages 2+.");
		chckbxAddMiniTitles.setSelected(MusicXMLCleaner.addMiniTitlesAndPageNumbers); // Set the default
		chckbxAddMiniTitles.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.addMiniTitlesAndPageNumbers = chckbxAddMiniTitles.isSelected();
			}
		});
		operationsPanel.add(chckbxAddMiniTitles);
		
		/*
		 * Add system measure numbers checkbox
		 */
		JCheckBox chckbxAddSystemMeasureNumbers = new JCheckBox("Add System Measure Numbers");
		chckbxAddSystemMeasureNumbers.setToolTipText("If checked, the program will add the tag that creates system measure numbers.");
		chckbxAddSystemMeasureNumbers.setSelected(MusicXMLCleaner.addSystemMeasureNumbers); // Set the default
		chckbxAddSystemMeasureNumbers.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.addSystemMeasureNumbers = chckbxAddSystemMeasureNumbers.isSelected();
			}
		});
		operationsPanel.add(chckbxAddSystemMeasureNumbers);
		
		/*
		 * Remove extraneous copyright checkbox
		 */
		JCheckBox chckbxRemoveExtraneousCopyright = new JCheckBox("Remove Extraneous Copyright");
		chckbxRemoveExtraneousCopyright.setToolTipText("If checked, the program will remove copyright information from pages 2+. "
				+ "It will also correct the formatting of the copyright information on page 1.");
		chckbxRemoveExtraneousCopyright.setSelected(MusicXMLCleaner.removeDuplicateCopyrightInfo); // Set the default
		chckbxRemoveExtraneousCopyright.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.removeDuplicateCopyrightInfo = chckbxRemoveExtraneousCopyright.isSelected();
			}
		});
		operationsPanel.add(chckbxRemoveExtraneousCopyright);
		
		/*
		 * Correct tempo marking checkbox
		 */
		JCheckBox chckbxCorrectTempoMarking = new JCheckBox("Correct Tempo Marking");
		chckbxCorrectTempoMarking.setToolTipText("If checked, the program will make necessary adjustments so tempo directional texts "
				+ "and their associated metrnonome marks are one object.");
		chckbxCorrectTempoMarking.setSelected(MusicXMLCleaner.correctTempoMarking); // Set the default
		chckbxCorrectTempoMarking.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.correctTempoMarking = chckbxCorrectTempoMarking.isSelected();
			}
		});
		operationsPanel.add(chckbxCorrectTempoMarking);
		
		/*
		 * Center credits horizontally checkbox
		 */
		JCheckBox chckbxCenterCreditsHorizontally = new JCheckBox("Center Credits Horizontally");
		chckbxCenterCreditsHorizontally.setToolTipText("<html>If checked, the program will horizontally center any credit, "
				+ "that is set as horizontally aligned, with respect to the repsective page's margins.</html>");
		chckbxCenterCreditsHorizontally.setSelected(MusicXMLCleaner.centerCreditsHorizontally); // Set the default
		chckbxCenterCreditsHorizontally.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.centerCreditsHorizontally = chckbxCenterCreditsHorizontally.isSelected();
			}
		});
		operationsPanel.add(chckbxCenterCreditsHorizontally);
		
		/*
		 * Align systems checkbox
		 */
		JCheckBox chckbxAlignSystemsWithLeftMargin = new JCheckBox("Align Systems w/ Left Margin");
		chckbxAlignSystemsWithLeftMargin.setToolTipText("<html>If checked, the program will reduce the system margins of the left-most systems so that they align with the left margin."
				+ "<br>The first system's margin is also reduced by the same amount so that the relative positioning of the systems is preserved."
				+ "<br>If there is only one system in the sheet, this doesn't do anything.</html>");
		chckbxAlignSystemsWithLeftMargin.setSelected(MusicXMLCleaner.offsetSystemMargins); // Set the default
		chckbxAlignSystemsWithLeftMargin.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.offsetSystemMargins = chckbxAlignSystemsWithLeftMargin.isSelected();
			}
		});
		operationsPanel.add(chckbxAlignSystemsWithLeftMargin);
		
		/*
		 * Make repeat text bold checkbox
		 */
		JCheckBox chckbxMakeRepeatTexts = new JCheckBox("Make Repeat Texts Bold");
		chckbxMakeRepeatTexts.setToolTipText("<html>If checked, the program will make any ending texts like <tt>D.C. al Coda</tt> and <tt>Fine</tt> bolded.</html>");
		chckbxMakeRepeatTexts.setSelected(MusicXMLCleaner.makeRepeatTextsBold); // Set the default
		chckbxMakeRepeatTexts.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.makeRepeatTextsBold = chckbxMakeRepeatTexts.isSelected();
			}
		});
		operationsPanel.add(chckbxMakeRepeatTexts);
		
		/*
		 * Add periods to voltas checkbox
		 */
		JCheckBox chckbxAddPeriodsToVoltas = new JCheckBox("Add Periods to Ending Texts");
		chckbxAddPeriodsToVoltas.setToolTipText("<html>If checked, the program will add periods to the end of numbers in ending texts.</html>");
		chckbxAddPeriodsToVoltas.setSelected(MusicXMLCleaner.addPeriodsToVoltaTexts); // Set the default
		chckbxAddPeriodsToVoltas.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.addPeriodsToVoltaTexts = chckbxAddPeriodsToVoltas.isSelected();
			}
		});
		operationsPanel.add(chckbxAddPeriodsToVoltas);
		
		/*
		 * Add swing 8ths wherever there is a "Swing" direction checkbox
		 */
		JCheckBox chckbxAddSwingths8ths = new JCheckBox("<html>Add Swing 8ths wherever <br>a \"Swing\" Direction</html>");
		chckbxAddSwingths8ths.setToolTipText("<html>If checked, the program will add a symbol that will turn on swung 8th notes "
				+ "<br>wherever a direction (i.e Staff Text) is placed labeled \"Swing\".</html>");
		chckbxAddSwingths8ths.setSelected(MusicXMLCleaner.addSwing8thsWhereSwingDirection); // Set the default
		chckbxAddSwingths8ths.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.addSwing8thsWhereSwingDirection = chckbxAddSwingths8ths.isSelected();
			}
		});
		operationsPanel.add(chckbxAddSwingths8ths);
		
		/*
		 * Replace uses of the fonts Edwin and FreeSerif with Times New Roman
		 */
		JCheckBox chckbxReplaceFonts= new JCheckBox("<html>Replace uses of Edwin and"
				+ "<br>FreeSerif with Times New Roman </html>");
		chckbxReplaceFonts.setToolTipText("<html>If checked, the program will set the font to Times New Roman for any"
				+ "<br>element that uses Edwin or FreeSerif fonts.</html>");
		chckbxReplaceFonts.setSelected(MusicXMLCleaner.replaceEdwinAndFreeSerifWithTimesNewRoman); // Set the default
		chckbxReplaceFonts.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.replaceEdwinAndFreeSerifWithTimesNewRoman = chckbxReplaceFonts.isSelected();
			}
		});
		operationsPanel.add(chckbxReplaceFonts);
		
		/*
		 * Replace uses of the fonts Edwin and FreeSerif with Times New Roman
		 */
		JCheckBox chckbxFormatOssias= new JCheckBox("Format Ossias");
		chckbxFormatOssias.setToolTipText("If checked, the program will attempt to format any ossias that exist.");
		chckbxFormatOssias.setSelected(MusicXMLCleaner.formatOssias); // Set the default
		chckbxFormatOssias.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// When the checkbox is ticked or unticked, update the flag corresponding to the operation.
				MusicXMLCleaner.formatOssias = chckbxFormatOssias.isSelected();
			}
		});
		operationsPanel.add(chckbxFormatOssias);
		
		
		
		// House all the operations in a scroll pane, so as to prevent the need from expanding the window size
		JScrollPane scrollPane = new JScrollPane(operationsPanel);
		scrollPane.setBorder(
					new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Operations to Perform", TitledBorder.LEADING, TitledBorder.TOP, null, null)
				);
		contentPane.add(scrollPane, BorderLayout.WEST);
		
		
		
		// We want to check if there is a new version available.
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					System.out.println("Checking for latest version...");
					URL url = new URL("https://api.github.com/repos/cacabish/MusicXML-Cleaner/releases/latest");
					HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(5000); // Timeout after 5 seconds
					connection.setReadTimeout(5000); // Timeout after 5 seconds
					
					int status = connection.getResponseCode();
					if (status == 200) {
						// Open a buffered reader to read the input stream
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						
						// Variable to store the read
						String input = "";
						// The StringBuilder to build the string with
						StringBuilder stringBuilder = new StringBuilder();
						
						// Read all the input lines
						while ((input = bufferedReader.readLine()) != null) {
						    stringBuilder.append(input);
						}
						
						// CLose the reader
						bufferedReader.close();
						
						// Find the JSON string(s) we're interested in
						String outputString = stringBuilder.toString();

						// Regex to get the tag name.
						Matcher tagMatcher = Pattern.compile("\"tag_name\": *\"(.*?)\"").matcher(outputString);
						
						// Search for a max in the string
						if (tagMatcher.find()) {
							// The tag is between the parentheses
							String versionTag = tagMatcher.group(1);
							
							if (versionTag.compareTo(VERSION) > 0) {
								// There is a new version! Make the button appear and construct the window to appear!
								System.out.println("New version found: " + versionTag);
								EventQueue.invokeLater(new Runnable() {
									
									@Override
									public void run() {
										// Create the action handler that will process when a person clicks the button.
										newVersionButton.addActionListener(new ActionListener() {
											
											@Override
											public void actionPerformed(ActionEvent e) {
												JOptionPane.showMessageDialog(contentPane, new NewVersionPanel(versionTag), "New Version Available!", JOptionPane.PLAIN_MESSAGE);
												
											}
										});
										// Make the button visible
										newVersionButton.setVisible(true);
									}
								});
							}
							else {
								// No new version.
								System.out.println("Current version is up-to-date.");
								return;
							}
							
						}
					}
					else {
						System.out.println("Got bad response code: " + status);
					}
					
					
				} catch (Exception e) {
					// Oh well, we won't check the version.
					System.out.println("Version check failed: " + e.getMessage());
				}
				
			}
		}).start();
		
		
		
		System.out.println("GUI Initialized.");
	}
	
	/**
	 * Shows a JFileChooser with the MusicXML FileFilter and then loads the file selected, if one is chosen.
	 */
	private void showFileChooserAndLoad() {
		// Open the file chooser
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter(fileFilter);
		
		// Show the file chooser and get the response
		int response = fileChooser.showOpenDialog(contentPane);
		if (response == JFileChooser.APPROVE_OPTION) {
			// They chose to load the file, so accept it
			loadFile(fileChooser.getSelectedFile());
		}
		else {
			return;
		}
	}
	
	/**
	 * Loads a .musicxml file, preparatory to be run through the cleaner methods
	 * @param fileToLoad the file to load
	 */
	private void loadFile(File fileToLoad) {
		if (!fileFilter.accept(fileToLoad)) {
			JOptionPane.showMessageDialog(this, fileToLoad.getName() + " is not a .musicxml or .xml file." 
					+ System.lineSeparator() + "Double check the file extension on the file.", 
					"Invalid File Type", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try {
			// Do the cleaning
			MusicXMLCleaner.cleanMusicXMLFile(fileToLoad);
			
			System.out.println("All corrections performed. Prompting to save...");
			
			// Still here? Great! Save!
			saveFile(fileToLoad);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Unable to parse " + fileToLoad.getName() 
					+ ". Please let cacabish know about this!"
					+ " Reason: " + e.toString(), "Failed to Parse File", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
	}
	
	/**
	 * Saves the XML document (cached in the cleaner) to a file after prompting the user for a save location.
	 * @param parentFile the original file that the document came from. Gives quick access to simply override the original file.
	 */
	private void saveFile(File parentFile) {
		// Create the save file chooser
		final JFileChooser fileChooser = new JFileChooser(parentFile) {

			private static final long serialVersionUID = -6037465542637020728L;

			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				if (f.exists()) {
					// Confirm if they want to overwrite the file
					int confirmation = JOptionPane.showConfirmDialog(contentPane, "This file exists. Do you want to overwrite this file?", "Overwrite Existing File?", JOptionPane.YES_NO_CANCEL_OPTION);
					
					switch (confirmation) {
					case JOptionPane.YES_OPTION:
						super.approveSelection(); // Do the normal thing and return
						return;
					case JOptionPane.NO_OPTION:
						return; // Do not accept and abort
					case JOptionPane.CLOSED_OPTION:
						return; // Do not accept and abort
					case JOptionPane.CANCEL_OPTION:
						cancelSelection(); // Cancel the selection, but return to the file chooser
						return;
					default:
						return; // Abort, in all other cases
					}
				}
				else {
					// All is normal. Proceed as normal.
					super.approveSelection();
				}
			}
		};
		fileChooser.setMultiSelectionEnabled(false); // You should only be able to save a single file
		fileChooser.setFileFilter(fileFilter); // Set the file filter
		fileChooser.setSelectedFile(parentFile); // Set the original file so as to prompt an override by default
		
		// Show the file chooser
		int response = fileChooser.showSaveDialog(contentPane);
		if (response == JFileChooser.APPROVE_OPTION) {
			// They chose to accept the save
			File chosenFile = fileChooser.getSelectedFile();
			if (chosenFile == null) {
				// Don't know how we got here, but it's bad news if this happens. Abort for safety.
				JOptionPane.showMessageDialog(contentPane, "Save was unsuccessful. Reason: file was null", "Save Failed", JOptionPane.ERROR_MESSAGE);
				return;
			}
			else if (!chosenFile.getName().toLowerCase().endsWith(".musicxml")) {
				// If the file extension is wrong, make it right!
				chosenFile = new File(chosenFile.getParentFile(), chosenFile.getName() + ".musicxml");
			}
			
			// Save the file!
			try {
				MusicXMLCleaner.writeToFile(chosenFile);
				
				// Optional: Notify the user of the success
				JOptionPane.showMessageDialog(contentPane, "Successfully saved to " + chosenFile.getName(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				// If anything goes wrong, report the failure.
				JOptionPane.showMessageDialog(contentPane, "Save was unsuccessful. Reason: " + e.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		else {
			// They chose cancel, no, or closed the window. In all these cases, and any others unaccounted for, abort.
			System.out.println("Save cancelled.");
			return;
		}
	}
	
	/**
	 * A nested class that displays information when "About" is selected.
	 * @author cacabish
	 */
	private static class AboutPanel extends JEditorPane {
		
		/**
		 * This thing. Yeah, this thing.
		 */
		private static final long serialVersionUID = 6582724507797400236L;

		/**
		 * The constructor.
		 */
		public AboutPanel() {
			super("text/html", 
					"<html>"
					+ "<body style='font-family: Tahoma; font-size: 14;'>"
					+ "<b>MusicXML Cleaner " + VERSION + " for MuseScore " + MUSESCORE_VERSION + "</b>"
					+ "<br>"
					+ "<i>A tool for <a href='http://www.ninsheetmusic.org/'>NinSheetMusic.org</a> arrangers who use MuseScore.</i>"
					+ "<br><br>"
					+ "GitHub Repo: <a href='https://github.com/cacabish/MusicXML-Cleaner'>https://github.com/cacabish/MusicXML-Cleaner</a>"
					+ "<br><br>"
					+ "Copyright (c) 2020-25 cacabish" 
					+ "<br>"
					+ "MusicXML 4.0 by W3C Music Notation Community Group"
					+ "<br>"
					+ "Published under the MIT License"
					+ "</body>"
					+ "</html>"
				);
			
			// This adds a listener so that when a link is clicked, it attempts to browse that link.
			addHyperlinkListener(new HyperlinkListener() {
				
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
						try {
							if (e.getURL() != null) {
								Desktop.getDesktop().browse(e.getURL().toURI());
							}
						} catch (Exception e1) {
							// Do nothing. This is all just extra anyway.
						}
					}
				}
			});
			
			setEditable(false); // We don't want this to be editable
		}
		
	}
	
	
	/**
	 * A nested class that displays information when a new version has been found.
	 * @author cacabish
	 */
	private static class NewVersionPanel extends JEditorPane {
		
		/**
		 * This thing. Yeah, this thing.
		 */
		private static final long serialVersionUID = 6582724507797400236L;

		/**
		 * The constructor.
		 */
		public NewVersionPanel(String newVersion) {
			super("text/html", 
					"<html>"
					+ "<body style='font-family: Tahoma; font-size: 14;'>"
					+ "A new version is available!"
					+ "<br><br>"
					+ "<b>Current Version:</b> " + VERSION
					+ "<br>"
					+ "<b>New Version:</b> " + newVersion
					+ "<br><br>"
					+ "<a href=https://github.com/cacabish/MusicXML-Cleaner/releases/latest>[Download]</a>"
					+ "</body>"
					+ "</html>"
				);
			
			// This adds a listener so that when a link is clicked, it attempts to browse that link.
			addHyperlinkListener(new HyperlinkListener() {
				
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
						try {
							if (e.getURL() != null) {
								Desktop.getDesktop().browse(e.getURL().toURI());
							}
						} catch (Exception e1) {
							// Do nothing. This is all just extra anyway.
						}
					}
				}
			});
			
			setEditable(false); // We don't want this to be editable
		}
		
	}

}
