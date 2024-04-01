package com.mtv.app.gui;


import com.mtv.app.Main;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;


public class MainPanel extends JPanel {

	public MainPanel() {
		core = new Core();

		readDataFile();

		initUI();
	}

	protected void initUI() {

		setLayout(new BorderLayout());

		setPreferredSize(new Dimension(1366, 768));

		headPn = createHeadPanel();
		add(headPn, BorderLayout.PAGE_START);

		list = new JList<String>();
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				index = e.getFirstIndex();
				System.out.println("source: " + methodSignatures[index]);
			}
		});
		list.setFont(new Font("Serif",Font.BOLD,14));



		//file browser

		fileRoot = new File("/");
		root = new DefaultMutableTreeNode(new FileNode(fileRoot));
		treeModel = new DefaultTreeModel(root);

		tree = new JTree(treeModel);
		tree.setShowsRootHandles(true);
		tree.setModel(null);

		JScrollPane scrollPane = new JScrollPane(tree);

		tree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getPath();
				if(tp != null){
					DefaultMutableTreeNode obj = (DefaultMutableTreeNode) tp.getLastPathComponent();
					Object file1 = obj.getUserObject();

					if(file1 instanceof FileNode){

						FileNode node = (FileNode) file1;
						file = node.getFile();

						if (file != null) {

							if (file.getParent() != null) {
								recentDirectory = file.getParent();
								writeDataFile();
							}

							try {
								loadSourceCode();

								core = new Core(file.getAbsolutePath());

								methodSignatures = core.getMethodSignatures();
								list.setListData( methodSignatures );
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainPanel.this,
										"Compile error!");
							}

						}
					}
				}
			}
		});

//        CreateChildNodes ccn =
//                new CreateChildNodes(fileRoot, root);
//        new Thread(ccn).start();
//
//		JScrollPane listScrollPane = new JScrollPane(list);
//



		JPanel constraintPanel = createContraintsPanel();



		JPanel sourcePanel = createSourceViewPanel();

		JPanel functionList = createFunctionListPanel();

		JSplitPane splitpane0 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				functionList, sourcePanel);
		splitpane0.setDividerLocation(300);


		JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				splitpane0, constraintPanel);
		splitPane2.setDividerLocation(400);

		JPanel temp = new JPanel(new BorderLayout());
		temp.setPreferredSize(new Dimension(800, 900));
		temp.add(splitPane2, BorderLayout.CENTER);

		JSplitPane splitpane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				scrollPane, temp);
		splitpane2.setDividerLocation(300);


		add(splitpane2, BorderLayout.CENTER);



		UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
		for (UIManager.LookAndFeelInfo lak: infos) {
			System.out.println("class name: " + lak.getClassName());
		}
		try {
			UIManager.setLookAndFeel(infos[3].getClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				 | UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
		SwingUtilities.updateComponentTreeUI(MainPanel.this);

	}

	private JPanel createHeadPanel() {
		JPanel head = new JPanel();
		openBtn = new JButton("Open Project/File");

		openBtn.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFileChooser chooser = new JFileChooser();

				chooser.setCurrentDirectory(new File(recentDirectory));

				chooser.setDialogTitle("Please choose a file or project");
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				FileNameExtensionFilter filter = new FileNameExtensionFilter("C/C++ file", "c");
				chooser.addChoosableFileFilter(filter);
				chooser.setAcceptAllFileFilterUsed(false);

				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					fileRoot = chooser.getSelectedFile();
				} else {
					System.out.println("No Selection ");
					return;
				}

				if (fileRoot.isDirectory())
					recentDirectory = fileRoot.getAbsolutePath();
				else
				if (fileRoot.getParent() != null)
					recentDirectory = fileRoot.getParent();
				writeDataFile();

				root = new DefaultMutableTreeNode(new FileNode(fileRoot));
				treeModel = new DefaultTreeModel(root);

				tree.setModel(treeModel);
				tree.setShowsRootHandles(true);
				//      JScrollPane scrollPane = new JScrollPane(tree);
				CreateChildNodes ccn =
						new CreateChildNodes(fileRoot, root);
				new Thread(ccn).start();
			}
		});

		head.add(openBtn);

		refreshBtn = new JButton("Refresh");
		refreshBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		head.add(refreshBtn);

		vertificationBtn = new JButton("Generate test");
		vertificationBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				generateTest();
			}
		});
		head.add(vertificationBtn);

		return head;
	}

	private JPanel createContraintsPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		resultTA = new JTextPane();
		resultTA.setEditable(false);
		resultTA.setContentType("text/html");
		resultTA.setText("<p style='font-weight:bold;font-size:14;font-family:arial'>hello</h1>");

		testDrive = new JTextArea();
		testDrive.setEditable(false);

		reducedtest = new JTextArea();
		reducedtest.setEditable(false);

		metaSMT = new JTextArea();
		metaSMT.setEditable(false);


		JTabbedPane tabbedpane = new JTabbedPane();

		try {
			Font font = new Font("Arial", Font.PLAIN, 14);
			UIManager.getDefaults().put("TabbedPane.font", new FontUIResource(font));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		//	JLabel title = new JLabel("Log");
		//	panel.add(title, BorderLayout.PAGE_START);

		//	title.setFont(new Font("Arial", Font.PLAIN, 14));
		//	JScrollPane spResult = new JScrollPane(resultTA);
		tabbedpane.add("Number Test", new JScrollPane(resultTA));
		tabbedpane.add("Test Drive", new JScrollPane(testDrive));
		tabbedpane.add("Number test reduced", new JScrollPane(reducedtest));
		//tabbedpane.add("Solver log", new JScrollPane(smtLog));
		//tabbedpane.add("MetaSMT", new JScrollPane(metaSMT));


		panel.add(tabbedpane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createFunctionListPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel title = new JLabel("Function List");
		panel.add(title, BorderLayout.PAGE_START);

		title.setFont(new Font("Serif", Font.PLAIN, 14));
		JScrollPane spList = new JScrollPane(list);

		panel.add(spList, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createLogPanel() {
		JPanel panel = new JPanel(new BorderLayout());
//		JLabel title = new JLabel("Constraints");
//		title.setFont(new Font("Arial", Font.PLAIN, 14));
//		panel.add(title, BorderLayout.PAGE_START);
//
//		JLabel label1 = new JLabel("Pre-condition:");
//		label1.setFont(new Font("Serif", Font.ITALIC, 14));
//		JLabel label2 = new JLabel("Post-condition:");
//		label2.setFont(new Font("Serif", Font.ITALIC, 14));
//		preconditionTA = new JTextArea();
//		postconditionTA = new JTextArea();
//		JScrollPane spConstraint1 = new JScrollPane(preconditionTA);
//
//		JScrollPane spConstraint = new JScrollPane(postconditionTA);

//		JSplitPane tmp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, label1, spConstraint1);
//		tmp1.setDividerLocation(30);
//		JSplitPane tmp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, label2, spConstraint);
//		tmp2.setDividerLocation(30);
//		JSplitPane tmp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tmp1, tmp2);
//		tmp.setDividerLocation(200);
//		panel.add(tmp, BorderLayout.CENTER);
//

		return panel;
	}

	private JPanel createSourceViewPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JLabel title = new JLabel("Source code");
		title.setFont(new Font("Arial", Font.PLAIN, 14));
		panel.add(title, BorderLayout.PAGE_START);

		sourceView = new JTextArea();
		//sourceView.setEditable(false);
		JScrollPane spConstraint = new JScrollPane(sourceView);

		panel.add(spConstraint, BorderLayout.CENTER);
//		panel.setPreferredSize(new Dimension(600, 400));

		return panel;
	}

	private void loadSourceCode() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String nextLine = "";
			sourceView.setText("");
			int position = 0;
			int lineNo = 100;
			int countLine = 1;
			while (true) {
				nextLine = br.readLine();
				if (nextLine != null)
					sourceView.append(nextLine + "\n");
				else
					break;

				if (countLine < lineNo) {
					countLine++;
					position = position + 1 + nextLine.length();
				}
			}
			//	sourceView.setCaretPosition(0);
			sourceView.setCaretPosition(position);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadSMTInput(String filename) {
		File file = new File("./smt/" + filename +".smt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String nextLine = "";
			smtInput.setText("");
			while (true) {
				nextLine = br.readLine();
				if (nextLine != null)
					smtInput.append(nextLine + "\n");
				else
					break;

			}
			smtInput.setCaretPosition(0);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadMetaFile() {
		File file = new File("metaSMT.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String nextLine = "";
			metaSMT.setText("");
			while (true) {
				nextLine = br.readLine();
				if (nextLine != null)
					metaSMT.append(nextLine + "\n");
				else
					break;

			}
			metaSMT.setCaretPosition(0);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readDataFile() {
		dataFile = new File(dateFilePath);
		if (dataFile == null)
			return;

		try {
			BufferedReader bf = new BufferedReader( new FileReader(dataFile) );
			recentDirectory = bf.readLine();
			bf.close();
		} catch (FileNotFoundException e) {
			//	e.printStackTrace();
		} catch (IOException e) {
			//	e.printStackTrace();
		}
	}

	private void writeDataFile() {
		dataFile = new File(dateFilePath);
		if (recentDirectory == null || dataFile == null)
			return;

		try {
			BufferedWriter bf = new BufferedWriter( new FileWriter(dataFile) );
			System.out.println(recentDirectory);
			bf.write(recentDirectory);
			bf.flush();
			bf.close();
		} catch (FileNotFoundException e) {
			//		e.printStackTrace();
		} catch (IOException e) {
			//		e.printStackTrace();
		}
	}

	private void generateTest() {
		ArrayList<String> paths = new ArrayList<>();
		Integer timeOut = 100; // Timeout value

		// Check if the selection is a directory
		if (fileRoot.isDirectory()) {
			// Optionally, iterate through the directory to add specific files to the paths list
			File[] files = fileRoot.listFiles((dir, name) -> name.endsWith(".c") || name.endsWith(".cpp"));
			if (files != null) {
				for (File file : files) {
					paths.add(file.getAbsolutePath());
				}
			}
		} else if (fileRoot.isFile() && (fileRoot.getName().endsWith(".c") || fileRoot.getName().endsWith(".cpp"))) {
			// If a single file is selected and it is a C/C++ file, add it to the list
			paths.add(fileRoot.getAbsolutePath());
		}

		// Only call GenerateTests if there are paths to process
		if (!paths.isEmpty()) {
			try {
				Main.GenerateTests(paths, timeOut);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error generating tests: " + e.getMessage());
			}
		} else {
			JOptionPane.showMessageDialog(this, "No applicable files were selected or found in the directory.");
		}
	}

	private void refresh() {
		if (file == null)
			return;
		try {
			loadSourceCode();

			core = new Core(file.getAbsolutePath());

			methodSignatures = core.getMethodSignatures();
			list.setListData( methodSignatures );

			resultTA.setText("");
			smtInput.setText("");
			smtLog.setText("");
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(MainPanel.this,
					"Compile error!");
		}
	}

	public int getLinePosition(int lineNumber){
		Vector linelength=new Vector();
		String txt=sourceView.getText();
		int width=sourceView.getWidth();
		StringTokenizer st=new StringTokenizer(txt,"\n ",true);
		String str=" ";
		int len=0;
		linelength.addElement(new Integer(0)); //position of the first line
		while(st.hasMoreTokens()){
			String token=st.nextToken();
			int w=sourceView.getGraphics().getFontMetrics(sourceView.getGraphics().getFont()).stringWidth(str+token);
			if(w>width || token.charAt(0)=='\n'){
				len=len+str.length();
				if(token.charAt(0)=='\n'){
					linelength.addElement(new Integer(len)); //positon of the line
				}
				else{
					linelength.addElement(new Integer(len-1)); //positon of the line
				}
				str=token;
			}
			else{
				str=str+token;
			}

		}

		return len;
	} //get

	public static void main(String[] args) {


		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame(title);
				frame.setLayout(new BorderLayout());
				JPanel panel = new MainPanel();

				frame.add(panel);
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

				//	frame.setUndecorated(true);		// full screen
				//	frame.setPreferredSize(new Dimension(1000, 400));
				frame.pack();
				frame.setVisible(true);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
	}

	// ham de thuc hien filebrowser
	public class FileNode {

		private File file;

		public FileNode(File file) {
			this.file = file;
		}

		public File getFile() {
			return file;
		}

		@Override
		public String toString() {
			String name = file.getName();
			if (name.equals("")) {
				return file.getAbsolutePath();
			} else {
				return name;
			}
		}
	}

	public class CreateChildNodes implements Runnable {

		private DefaultMutableTreeNode root;

		private File fileRoot;

		public CreateChildNodes(File fileRoot,
								DefaultMutableTreeNode root) {
			this.fileRoot = fileRoot;
			this.root = root;
		}

		@Override
		public void run() {
			createChildren(fileRoot, root);
		}

		private void createChildren(File fileRoot,
									DefaultMutableTreeNode node) {
			File[] files = fileRoot.listFiles();
			if (files == null) return;

			for (File file : files) {
				if ( !isCFile(file) && !file.isDirectory() )
					continue;

				DefaultMutableTreeNode childNode =
						new DefaultMutableTreeNode(new FileNode(file));
				node.add(childNode);
				if (file.isDirectory()) {
					createChildren(file, childNode);
				}
			}
		}


	}

	private boolean isJavaFile(File file) {
		if (file == null || file.isDirectory())
			return false;
		String name = file.getName();
		String extension;

		String[] temp = name.split("\\.");

		if (temp.length < 2)	// file ko co duoi
			return false;
		extension = temp[temp.length-1];
		if (extension.equals("java"))
			return true;

		return false;
	}
	private boolean isCFile(File file) {
		if (file == null || file.isDirectory())
			return false;
		String name = file.getName();
		String extension;

		String[] temp = name.split("\\.");

		if (temp.length < 2)	// file ko co duoi
			return false;
		extension = temp[temp.length-1];
		if (extension.equals("c"))
			return true;

		return false;
	}

	Core core;

	String dateFilePath = "data";
	File dataFile;
	String recentDirectory ="./benchmark";
	File file;

	JPanel headPn;
	JButton openBtn;
	JButton vertificationBtn;

	JButton GenerateBtn;
	JButton refreshBtn;

	JList<String> list;
	String[] methodSignatures;	// danh sách tên các method


	//	JTextArea preconditionTA;
//	JTextArea postconditionTA;
	JTextPane resultTA;
	JTextPane result;
	JTextArea sourceView;
	JTextArea smtInput;

	JTextArea testDrive;
	JTextArea reducedtest;


	JTextArea smtLog;
	JTextArea metaSMT;

	private DefaultMutableTreeNode root;

	private DefaultTreeModel treeModel;

	private JTree tree;
	File fileRoot;
	int index = -1;

	static String title = "Công cụ kiểm chứng tính chất của chương trình";

	static String SATLOG = " SAT(post condtion is alwways true)";
	static String UNSATLOG = "UNSAT(post condition is not always true, example: ";
}