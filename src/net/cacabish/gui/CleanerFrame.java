package net.cacabish.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import net.cacabish.MusicXMLCleaner;

import org.w3c.dom.Document;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

/**
 * The main GUI for processing MusicXML files.
 * @author cacabish
 * @version 1.0.0
 *
 */
public class CleanerFrame extends JFrame {

	private static final long serialVersionUID = 7520073032182546710L;
	
	public static final String VERSION = "1.0.0";
	public static final String MUSESCORE_VERSION = "3.5.2";

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
		setTitle("MusicXML Cleaner v" + VERSION + " for MuseScore v" + MUSESCORE_VERSION);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 525, 350); // Set initial size parameters
		setResizable(false); // No need to resize
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
						"Next, load up this program and either drag-and-drop the newly exported file onto the window or click the button below and select the file." 
							+ System.lineSeparator() +
						"Finally, let the program work its magic and then it will prompt you to save the cleaned file. "
							+ System.lineSeparator() +
						"You may override the old file or create a new one."
							+ System.lineSeparator() + System.lineSeparator() +
						"That's it! You can now import into Finale with many annoying bugs fixed!"
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
				JOptionPane.showMessageDialog(contentPane, 
						"A tool for NinSheetMusic.org arrangers who use MuseScore."
								+ System.lineSeparator() + System.lineSeparator() +
						"Copyright © 2020 cacabish" 
								+ System.lineSeparator() +
						"MusicXML 3.1 by W3C Music Notation Community Group"
								+ System.lineSeparator() +
						"Published under the MIT License"
				, "About", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menuHelp.add(menuItemAbout);

		
		
		
		
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
		
		JLabel lblDragAndDrop = new JLabel("Drag and drop a .musicxml file to convert here or use the button below.");
		lblDragAndDrop.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblDragAndDrop, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);
		
		// Open File Button
		JButton btnOpenFile = new JButton("Choose .musicxml File...");
		btnOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showFileChooserAndLoad();
			}
		});
		panel.add(btnOpenFile);
		
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
			// Parse the document
			Document validatedDoc = MusicXMLCleaner.constructAndValidateMusicXMLDocument(fileToLoad);
			
			// Do the cleaning!
			MusicXMLCleaner.addPageNumbersAndMiniTitles(validatedDoc);
			MusicXMLCleaner.addSystemMeasureNumbers(validatedDoc);
			MusicXMLCleaner.removeDuplicateCopyrightInfo(validatedDoc);
			MusicXMLCleaner.correctTempoMark(validatedDoc);
			MusicXMLCleaner.centerCreditsHorizontally(validatedDoc);
			
			System.out.println("All corrections successful. Prompting to save...");
			
			// Still here? Great! Save!
			saveFile(fileToLoad, validatedDoc);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Unable to parse " + fileToLoad.getName() 
					+ ". Reason: " + e.getMessage(), "Failed to Parse File", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
	}
	
	/**
	 * Saves the XML document to a file after prompting the user for a save location.
	 * @param parentFile the original file that the document came from. Gives quick access to simply override the original file.
	 * @param document the cleaned XML document to be saved to
	 */
	private void saveFile(File parentFile, Document document) {
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
				MusicXMLCleaner.writeToFile(document, chosenFile);
				
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
			return;
		}
	}

}
