package edu.swri.swiftvis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import edu.swri.swiftvis.AutoProcess.ProcessSink;
import edu.swri.swiftvis.AutoProcess.ProcessSource;
import edu.swri.swiftvis.plot.Plot;
import edu.swri.swiftvis.sources.AutoProcessSource;
import edu.swri.swiftvis.sources.FileSourceInter;
import edu.swri.swiftvis.sources.SocketSource;

/**
* This is the main class for the SWIFT visualization tool.  The objective of
* this tool is to give users a faster and more convenient way to look through the
* large amounts of data that SWIFT can produce for a large simulation.  It should
* also help them sort through, cut down, and convert the data in ways that will help
* them see trends.
**/
public class SwiftVis {
    public static void main(String[] args) {
        new SwiftVis(args);
    }
    
    public static JFrame startGUI() {
        SwiftVis sv=new SwiftVis(new String[0]);
        sv.mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        return sv.mainFrame;
    }
    
    public SwiftVis(String[] args) {
        File loadFile=null;
        for(int i=0; i<args.length; ++i) {
            if(args[i].endsWith(".svf") || args[i].endsWith(".svt")) {
                loadFile=new File(args[i]);
            } else if(args[i].endsWith(".svb")) {
                String[] otherArgs=Arrays.copyOfRange(args,i+1,args.length);
                batchProcess(args[i],otherArgs,loadFile);
                return;
            } else if(args[i].equals("-help")) {
                System.out.println("SwiftVis Options:\n");
                System.out.println("\t[filename [batchfile ...]] - loads specified .svf or .svt file.");
                System.out.println("\t\tbatchfile loads specified .svb.");
                System.out.println("\t\t... is list of input files for simple batch.");
                System.out.println("\t\t... is start count, end count, and step for full format.");
                System.out.println("\t-help - prints this message.");
                System.out.println("\t-version - prints the version of SwiftVis you are using.");
                return;
            } else if(args[i].equals("-version")) {
                System.out.println("SwiftVis version "+OptionsData.version);
                return;
            } else {
                System.out.println("Invalid command line argument.  Use -help to see options.");
            }
        }
        setupGUI();
        if(loadFile!=null) {
            openFile(loadFile,true);
        }
    }
    
    public SwiftVis(){
        setupGUI();
        mainFrame.setTitle("Swift Data Visualization "+OptionsData.version);
    }
    
    public void initService(Object o) {
        top = o;
        try {
            closeMethod = top.getClass().getDeclaredMethod("windowClosed", Object.class);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        graphPanel.addDataset(new SocketSource(), 20, 20);
    }
    
    public static String getTypeDescription() {
        return "Data Analysis and Visualization";
    }
    
    public void setVisible(boolean v) {
        mainFrame.setVisible(v);
    }
    
    public boolean isVisible() {
        return mainFrame.isVisible();
    }
    
    public void selectionMade(GraphElement pe) {
        JComponent pp=pe.getPropertiesPanel();
        if(pp!=null) {
            propPanel.removeAll();
            propPanel.add(pp);
            mainFrame.getContentPane().validate();
            mainFrame.getContentPane().repaint();
        }
    }
    
    public void showPopup(Component invoker,int x,int y) {
    	popUpX=x;
    	popUpY=y;
    	popupMenu.show(invoker,x,y);
    }
    
    public JFrame getFrame() { return mainFrame; }

    private void setupGUI() {
        mainFrame=new JFrame("Swift Data Visualization "+OptionsData.version);
        mainFrame.getContentPane().setLayout(new GridLayout(1,1));
        mainFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(closeMethod != null) {
                    try {
                        closeMethod.invoke(top, SwiftVis.this);
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    System.exit(0);
                }
            }
        } );
		graphPanel=GraphPanel.instance(this);
        propPanel=new JPanel(new GridLayout(1,1));
        final JScrollPane jsp=new JScrollPane(graphPanel.getDrawPanel());
        JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,jsp,propPanel);
        mainFrame.getContentPane().add(splitPane);
        
        JMenuBar menuBar=new JMenuBar();
        popupMenu=new JPopupMenu("Insert");
        JMenu menu=new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        JMenuItem item=new JMenuItem("Open Plot File",KeyEvent.VK_O);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { openFile(null,true); }
        } );
        menu.add(item);
        item=new JMenuItem("Save Plot File",KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { saveFile(); }
        } );
        menu.add(item);
        item=new JMenuItem("Save as Template",KeyEvent.VK_T);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { saveTemplate(); }
        } );
        menu.add(item);
        menu.addSeparator();
		item=new JMenuItem("Edit Options",KeyEvent.VK_E);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,ActionEvent.CTRL_MASK));
		item.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { OptionsData.instance().getOptionsDialog(mainFrame).setVisible(true); }
		} );
		menu.add(item);
        item=new JMenuItem("Path Setter",KeyEvent.VK_P);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { new PathChanger(mainFrame,graphPanel.getElements()); }
        } );
        menu.add(item);
        item=new JMenuItem("Source Rereader",KeyEvent.VK_R);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { new SourceRereader(mainFrame,graphPanel.getElements()); }
        } );
        menu.add(item);
        item=new JMenuItem("Automatic Processing",KeyEvent.VK_A);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { new AutoProcess(graphPanel); }
        } );
        menu.add(item);
        item=new JMenuItem("Create Batch File",KeyEvent.VK_B);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { makeBatchFile(); }
        } );
        menu.add(item);
		menu.addSeparator();
        item=new JMenuItem("Exit",KeyEvent.VK_X);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { 
                if(closeMethod != null) {
                    try {
                        closeMethod.invoke(top, SwiftVis.this);
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                    mainFrame.setVisible(false); 
                } else {
                    System.exit(0);
                }
            }
        } );
        menu.add(item);
        menuBar.add(menu);

        menu=new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        item=new JMenuItem("Delete",KeyEvent.VK_D);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.deleteSelection(); }
        } );
        menu.add(item);
        menu.addSeparator();
        item=new JMenuItem("Cut",KeyEvent.VK_U);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.cutSelection(); }
        } );
        menu.add(item);
        item=new JMenuItem("Copy",KeyEvent.VK_C);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.copySelection(); }
        } );
        menu.add(item);
        item=new JMenuItem("Paste",KeyEvent.VK_P);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.pasteClipboard(); }
        } );
        menu.add(item);
        item=new JMenuItem("Select All",KeyEvent.VK_A);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.selectAll(); }
        } );
        menu.add(item);
        menuBar.add(menu);

        menu=new JMenu("Insert");
        menu.setMnemonic(KeyEvent.VK_I);
        item=new JMenuItem("Data Source",KeyEvent.VK_D);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addDataset(); }
        } );
        menu.add(item);
        item=new JMenuItem("Filter",KeyEvent.VK_F);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addFilter(); }
        } );
        menu.add(item);
        item=new JMenuItem("Plot",KeyEvent.VK_P);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addPlot(); }
        } );
        menu.add(item);
        item=new JMenuItem("Sink",KeyEvent.VK_S);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addSink(); }
        } );
        menu.add(item);
        item=new JMenuItem("Note",KeyEvent.VK_N);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addNote(); }
        } );
        menu.add(item);
        item=new JMenuItem("Connection",KeyEvent.VK_C);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.addConnection(); }
        } );
        menu.add(item);
        item=new JMenuItem("Template",KeyEvent.VK_T);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,ActionEvent.CTRL_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addPlotTemplate(); }
        } );
        menu.add(item);
        menuBar.add(menu);
        
        menu=new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        item=new JMenuItem("About",KeyEvent.VK_A);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { new AboutDialog(mainFrame); }
        } );
        menu.add(item);
        menuBar.add(menu);
        
        setupPopup();
        mainFrame.setJMenuBar(menuBar);
        mainFrame.setSize(OptionsData.instance().getMainFrameWidth(),OptionsData.instance().getMainFrameHeight());
        mainFrame.setVisible(true);
        splitPane.setDividerLocation(0.5);
    }
    
    private void setupPopup() {
    	JMenu menu=new JMenu("Insert");
    	JMenuItem item=new JMenuItem("Data Source",KeyEvent.VK_D);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addDataset(popUpX,popUpY); }
        } );
        menu.add(item);
        item=new JMenuItem("Filter",KeyEvent.VK_F);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addFilter(popUpX,popUpY); }
        } );
        menu.add(item);
        item=new JMenuItem("Plot",KeyEvent.VK_P);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addPlot(popUpX,popUpY); }
        } );
        menu.add(item);
        item=new JMenuItem("Sink",KeyEvent.VK_S);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addSink(popUpX,popUpY); }
        } );
        menu.add(item);
        item=new JMenuItem("Note",KeyEvent.VK_N);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addNote(popUpX,popUpY); }
        } );
        menu.add(item);
        item=new JMenuItem("Connection",KeyEvent.VK_C);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.addConnection(); }
        } );
        menu.add(item);
        item=new JMenuItem("Template",KeyEvent.VK_T);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { addPlotTemplate(); }
        } );
        menu.add(item);
        popupMenu.add(menu);
        
        item=new JMenuItem("Delete",KeyEvent.VK_D);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.deleteSelection(); }
        } );
        popupMenu.add(item);
        item=new JMenuItem("Cut",KeyEvent.VK_U);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.cutSelection(); }
        } );
        popupMenu.add(item);
        item=new JMenuItem("Copy",KeyEvent.VK_C);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.copySelection(); }
        } );
        popupMenu.add(item);
        item=new JMenuItem("Paste",KeyEvent.VK_P);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.pasteClipboard(); }
        } );
        popupMenu.add(item);
        item=new JMenuItem("Select All",KeyEvent.VK_A);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { graphPanel.selectAll(); }
        } );
        popupMenu.add(item);
    }
    
    private void openFile(File dataFile,boolean showPlots) {
        if(dataFile==null) {
    		JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) { return f.getName().endsWith(".svf") || f.isDirectory(); }
                @Override
                public String getDescription() { return "SwiftVis Files (*.svf)"; }
            });
    		int chooserRet=fileChooser.showOpenDialog(propPanel);
    		if(chooserRet==JFileChooser.CANCEL_OPTION) return;
    		dataFile=fileChooser.getSelectedFile();
            OptionsData.instance().setLastDir(dataFile.getParentFile());
        }
		try {
			ObjectInputStream ois=new ObjectInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
            boolean rightVersion=false;
            try {
                Scanner sc=new Scanner(ois);
                String versionString=sc.nextLine();
                if(versionString.startsWith(versionHeader)) {
                    String version=versionString.substring(versionHeader.length());
                    rightVersion=version.equals(OptionsData.version);
                } else {
                    ois.close();
                    ois=new ObjectInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
                }
            } catch(NoSuchElementException e) {
                e.printStackTrace();
                // Do nothing as this means they don't have the right version.
            }
            if(!rightVersion) {
                int opt=JOptionPane.showConfirmDialog(mainFrame,"This saved file has the wrong version number.  Do you want to try to read it anyway?");
                if(opt==JOptionPane.CANCEL_OPTION || opt==JOptionPane.NO_OPTION) {
                    ois.close();
                    return;
                }
            }
            @SuppressWarnings("unchecked") java.util.List<GraphElement> elements=(java.util.List<GraphElement>)ois.readObject();
			graphPanel.setElements(elements);
			ois.close();
            for(GraphElement ge:elements) {
                try {
                    Method meth=ge.getClass().getMethod("setGraphPanel",GraphPanel.class);
                    meth.invoke(ge,graphPanel);
                } catch(NoSuchMethodException e) {}
                catch(InvocationTargetException e) {}
                catch(IllegalAccessException e) {}
                if(showPlots) {
                    try {
                        Method meth=ge.getClass().getMethod("restoreFrame");
                        meth.invoke(ge);
                    } catch(NoSuchMethodException e) {}
                    catch(InvocationTargetException e) {}
                    catch(IllegalAccessException e) {}
                }
            }
		} catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(propPanel,"An I/O exception occured while reading the file.");
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(propPanel,"The file contained an unknown class type.");
		}
    }

    private void saveFile() {
		JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) { return f.getName().endsWith(".svf") || f.isDirectory(); }
            @Override
            public String getDescription() { return "SwiftVis Files (*.svf)"; }
        });
		int chooserRet=fileChooser.showSaveDialog(propPanel);
		if(chooserRet==JFileChooser.CANCEL_OPTION) return;
		File dataFile=fileChooser.getSelectedFile();
        if(!dataFile.getName().endsWith(".svf")) {
            dataFile=new File(dataFile.getAbsoluteFile()+".svf");
        }
        if(dataFile.exists()) {
            if(JOptionPane.showConfirmDialog(propPanel,"That file exists.  Are you sure you want to overwrite?")!=JOptionPane.YES_OPTION) return;
        }
        OptionsData.instance().setLastDir(dataFile.getParentFile());
    	java.util.List<GraphElement> elements=graphPanel.getElements();
    	try {
    		ObjectOutputStream oos=new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
            String header=versionHeader+OptionsData.version+"\n";
            oos.write(header.getBytes());
    		oos.writeObject(elements);
    		oos.close();
    	} catch(IOException e) {
    		e.printStackTrace();
			JOptionPane.showMessageDialog(propPanel,"An I/O exception occured while writing the file.");
    	}
    }
    
    private void saveTemplate() {
        JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) { return f.getName().endsWith(".svt") || f.isDirectory(); }
            @Override
            public String getDescription() { return "SwiftVis Templates (*.svt)"; }
        });
        int chooserRet=fileChooser.showSaveDialog(propPanel);
        if(chooserRet==JFileChooser.CANCEL_OPTION) return;
        File dataFile=fileChooser.getSelectedFile();
        if(!dataFile.getName().endsWith(".svt")) {
            dataFile=new File(dataFile.getAbsoluteFile()+".svt");
        }
        if(dataFile.exists()) {
            if(JOptionPane.showConfirmDialog(propPanel,"That file exists.  Are you sure you want to overwrite?")!=JOptionPane.YES_OPTION) return;
        }
        OptionsData.instance().setLastDir(dataFile.getParentFile());
        java.util.List<GraphElement> elements;
        elements=graphPanel.getSelection();
        if(elements.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,"Nothing was selected to save.");
            return;
        }
        elements=graphPanel.copyElements(elements);
        for(GraphElement ge:elements) {
            ge.clearData();
        }
        try {
            ObjectOutputStream oos=new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
            String header=versionHeader+OptionsData.version+"\n";
            oos.write(header.getBytes());
            oos.writeObject(elements);
            oos.close();
        } catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(propPanel,"An I/O exception occured while writing the file.");
        }
    }
    
    private void addDataset() {
    	addDataset(-1,-1);
    }
    
    private void addDataset(int x, int y) {
        Class<DataSource> dsClass=OptionsData.instance().selectDataSourceType(mainFrame);
        if(dsClass==null) return;
        try {
			graphPanel.addDataset(dsClass.newInstance(),x,y);
        } catch(ClassCastException e) {
            JOptionPane.showMessageDialog(mainFrame,"The selected class is not of the proper type.  "+dsClass.getName()+" is not a DataSource.","Incorrect Type",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class is not a DataSource: "+dsClass.getName());
            e.printStackTrace();
        } catch(InstantiationException e) {
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private void addFilter() {
    	addFilter(-1,-1);
    }

    private void addFilter(int x, int y) {
        Class<Filter> filtClass=OptionsData.instance().selectFilterType(mainFrame);
        if(filtClass==null) return;
        try {
            Filter filter=filtClass.newInstance();
			graphPanel.addFilter(filter,x,y);
            Method meth=filter.getClass().getMethod("setGraphPanel",GraphPanel.class);
            meth.invoke(filter,graphPanel);
        } catch(ClassCastException e) {
            JOptionPane.showMessageDialog(mainFrame,"The selected class is not of the proper type.  "+filtClass.getName()+" is not a Filter.","Incorrect Type",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class is not a Filter: "+filtClass.getName());
            e.printStackTrace();
        } catch(InstantiationException e) {
            JOptionPane.showMessageDialog(mainFrame,"The selected class does not have the proper constructor.  See Documentation.","Constructor Missing",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class does not have the proper constructor.  See Documentation.");
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
        catch(NoSuchMethodException e) {}
        catch(InvocationTargetException e) {}
    }

    private void addPlot() {
    	addPlot(-1,-1);
    }
    private void addPlot(int x,int y) {
		graphPanel.addPlot(new Plot(),x,y);
    }

    private void addNote() {
        addNote(-1,-1);
    }
    private void addNote(int x,int y) {
        graphPanel.addNote(new GraphNote(),x,y);
    }

    private void addSink() {
    	addSink(-1,-1);
    }
    private void addSink(int x,int y) {
        Class<DataSink> dsClass=OptionsData.instance().selectSinkType(mainFrame);
        if(dsClass==null) return;
        try {
            graphPanel.addSink(dsClass.newInstance(),x,y);
        } catch(ClassCastException e) {
            JOptionPane.showMessageDialog(mainFrame,"The selected class is not of the proper type.  "+dsClass.getName()+" is not a DataSource.","Incorrect Type",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class is not a DataSource: "+dsClass.getName());
            e.printStackTrace();
        } catch(InstantiationException e) {
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void addPlotTemplate() {
        JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) { return f.getName().endsWith(".svt") || f.isDirectory(); }
            @Override
            public String getDescription() { return "SwiftVis Templates (*.svt)"; }
        });
        int chooserRet=fileChooser.showOpenDialog(propPanel);
        if(chooserRet==JFileChooser.CANCEL_OPTION) return;
        File dataFile=fileChooser.getSelectedFile();
        OptionsData.instance().setLastDir(dataFile.getParentFile());
        try {
            ObjectInputStream ois=new ObjectInputStream(new BufferedInputStream(new FileInputStream(dataFile)));
            ois.mark(200);
            Scanner sc=new Scanner(ois);
            String versionString=sc.nextLine();
            boolean rightVersion=false;
            if(versionString.startsWith(versionHeader)) {
                String version=versionString.substring(versionHeader.length());
                rightVersion=version.equals(OptionsData.version);
            } else {
                ois.reset();
            }
            if(!rightVersion) {
                int opt=JOptionPane.showConfirmDialog(mainFrame,"This saved file has the wrong version number.  Do you want to try to read it anyway?");
                if(opt==JOptionPane.CANCEL_OPTION || opt==JOptionPane.NO_OPTION) {
                    ois.close();
                    return;
                }
            }
            @SuppressWarnings("unchecked") java.util.List<GraphElement> elements=(java.util.List<GraphElement>)ois.readObject();
            graphPanel.getElements().addAll(elements);
            ois.close();
            for(GraphElement ge:elements) {
                try {
                    Method meth=ge.getClass().getMethod("setGraphPanel",GraphPanel.class);
                    meth.invoke(ge,graphPanel);
                } catch(NoSuchMethodException e) {}
                catch(InvocationTargetException e) {}
                catch(IllegalAccessException e) {}
            }
            graphPanel.getDrawPanel().repaint();
        } catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(propPanel,"An I/O exception occured while reading the file.");
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(propPanel,"The file contained an unknown class type.");
        }
    }
    
    /**
     * This method will be called for when the user specifies a batch file.  There
     * are two options for processing batches.  One version works much like the menu
     * option for processing multiple files.  When using that version the file will
     * specify the start count, end count, and step to use.  Then a format is specified
     * for each of the sources and well as a file name for each of the plots.  The other
     * format is for simpler saved files.  It changes the name of only one source  (the
     * first file only for that one) and saves only one plot.  The file to read is taken
     * from the command line arguments after the name of the batch file and the saved
     * file is the same thing with the extension replaced to be .png.  
     * @param fname The name of the batch file.
     * @param otherArgs The rest of the arguments on the command line.
     */
    private void batchProcess(String fname,String[] otherArgs,File loadFile) {
        if(loadFile==null) {
            System.err.println("You must specify the file to load in before the batch file.");
            return;
        }
        graphPanel=GraphPanel.instance();
        openFile(loadFile,false);
        try {
            List<AutoProcessSource> apsList=new LinkedList<AutoProcessSource>();
            for(GraphElement ge:graphPanel.getElements()) {
                if(ge instanceof AutoProcessSource) {
                    apsList.add((AutoProcessSource)ge);
                }
            }
            Scanner sc=new Scanner(new File(fname));
            String headerLine=sc.nextLine().trim();
            if(!headerLine.startsWith("TYPE=")) {
                System.err.println("Batch file format is incorrect.  Improper header.");
                return;
            }
            if(headerLine.endsWith("full")) {
                if(otherArgs.length<3) {
                    System.err.println("You must specify the start count, end count, and step for this batch file.");
                    return;
                }
                int start=Integer.parseInt(otherArgs[0]);
                int end=Integer.parseInt(otherArgs[1]);
                int step=Integer.parseInt(otherArgs[2]);
                Pattern sourcePattern=Pattern.compile("SOURCE=(.+?):(.+)");
                Pattern plotPattern=Pattern.compile("PLOT=(.+):(.+):(\\d+)x(\\d+)");
                List<AutoProcess.FileSource> sourceList=new ArrayList<AutoProcess.FileSource>();
                List<Plot> plotList=new ArrayList<Plot>();
                List<String> plotFileList=new ArrayList<String>();
                List<BufferedImage> imgList=new ArrayList<BufferedImage>();
                while(sc.hasNext()) {
                    String line=sc.nextLine();
                    Matcher match=sourcePattern.matcher(line);
                    if(match.matches()) {
                        FileSourceInter fsi=(FileSourceInter)findElementByLabel(match.group(1));
                        if(fsi==null) {
                            System.err.println("Element not found by name in file. "+match.group(1));
                            System.exit(-1);
                        }
                        String[] parts=match.group(2).split(",");
                        for(int i=0; i<parts.length; ++i) {
                            sourceList.add(new AutoProcess.FileSource(fsi,i,new File(parts[i])));
                        }
                    } else {
                        match=plotPattern.matcher(line);
                        if(match.matches()) {
                            plotList.add((Plot)findElementByLabel(match.group(1)));
                            plotFileList.add(match.group(2));
                            int width=Integer.parseInt(match.group(3));
                            int height=Integer.parseInt(match.group(4));
                            imgList.add(new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB));
                        } else {
                            System.err.println("Poorly formatted line.  "+line);
                        }
                    }                    
                }
                sc.close();
                ProcessSource source=new ProcessSource();
                ProcessSink sink=new ProcessSink(source);
                for(AutoProcess.FileSource fs:sourceList) {
                    if(fs.getFormula()==null) {
                        String spec=fs.makeSpec(sink);
                        fs.getSource().setFile(fs.getNumber(),new File(spec));
                        fs.getSource().rereadFiles();
                    }
                }
                for(int i=start; i<=end; i+=step) {
                    source.setParamValue(i);
                    for(AutoProcess.FileSource fs:sourceList) {
                        if(fs.getFormula()!=null) {
                            String spec=fs.makeSpec(sink);
                            fs.getSource().setFile(fs.getNumber(),new File(spec));
                            fs.getSource().rereadFiles();
                        }
                    }
                    for(AutoProcessSource aps:apsList) {
                        aps.setCount(i);
                    }
                    GraphPanel.instance().blockWhileUpdatesScheduled();
                    for(int j=0; j<plotList.size(); ++j) {
                        File file=new File(plotFileList.get(j)+"."+AutoProcess.makeNum(i)+".png");
                        writeImage(plotList.get(j),file,imgList.get(j));
                    }
                }
            } else if(headerLine.endsWith("simple")) {
                Pattern sourcePattern=Pattern.compile("SOURCE=(.+)");
                Pattern plotPattern=Pattern.compile("PLOT=(.+):(\\d+)x(\\d+)");
                FileSourceInter fsi=null;
                Plot plot=null;
                BufferedImage img=null;
                while(sc.hasNext()) {
                    String line=sc.nextLine();
                    Matcher match=sourcePattern.matcher(line);
                    if(match.matches()) {
                        fsi=(FileSourceInter)findElementByLabel(match.group(1));
                    } else {
                        match=plotPattern.matcher(line);
                        if(match.matches()) {
                            plot=(Plot)findElementByLabel(match.group(1));
                            int width=Integer.parseInt(match.group(2));
                            int height=Integer.parseInt(match.group(3));
                            img=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
                        } else {
                            System.err.println("Poorly formatted line.  "+line);
                        }
                    }
                }
                sc.close();
                if(fsi==null || plot==null) {
                    System.err.println("Incomplete batch file.");
                } else {
                    for(String s:otherArgs) {
                        fsi.setFile(0,new File(s));
                        fsi.rereadFiles();
                        GraphPanel.instance().blockWhileUpdatesScheduled();
                        int dotIndex=s.lastIndexOf('.');
                        String iFileName;
                        if(dotIndex>s.length()-5) {
                            iFileName=s.substring(0,dotIndex+1)+"png";
                        } else {
                            iFileName=s+".png";
                        }
                        writeImage(plot,new File(iFileName),img);
                    }
                }
            } else {
                System.err.println("Batch file format is incorrect.  Improper header.");
            }
        } catch (FileNotFoundException e) {
            System.err.println("The specified batch file could not be opened.");
        }
    }
    
    private void writeImage(Plot plot,File file,BufferedImage img) {
        Graphics2D g=img.createGraphics();
        g.setBackground(Color.white);
        g.clearRect(0,0,img.getWidth(),img.getHeight());
        plot.getPlotSpec().draw(g,new Rectangle2D.Double(0,0,img.getWidth(),img.getHeight()), 1);
        try {
            javax.imageio.ImageIO.write(img,"png",file);
        } catch(java.io.IOException e) {
            System.out.println("There was an exception trying to write the file.");
            e.printStackTrace();
        }        
    }
    
    /**
     * This method will write out a batch file for use with the current
     * file.  The user gets to select the type of batch file and the file
     * name to save to.  Once it is saved the user should edit
     */
    private void makeBatchFile() {
        String[] options={"Full Batch Format","Simple Format"};
        int type=JOptionPane.showOptionDialog(mainFrame, "What type of batch file do you want to make?",
                "Batch Option", options.length, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if(type<0) return;
        JFileChooser chooser=new JFileChooser(OptionsData.instance().getLastDir());
        int select=chooser.showSaveDialog(mainFrame);
        if(select==JFileChooser.CANCEL_OPTION) return;
        OptionsData.instance().setLastDir(chooser.getSelectedFile().getParentFile());
        File file=chooser.getSelectedFile();
        if(!file.getName().endsWith(".svb")) {
            file=new File(file.getAbsolutePath()+".svb");
        }
        if(file.exists()) {
            int response=JOptionPane.showConfirmDialog(mainFrame, "File exists.  Are you sure you want to overwrite?");
            if(response!=JOptionPane.YES_OPTION) return;
        }
        try {
            PrintStream ps=new PrintStream(file);
            if(type==0) {
                ps.println("TYPE=full");
                int plotCount=0;
                for(GraphElement ge:graphPanel.getElements()) {
                    if(ge instanceof FileSourceInter) {
                        FileSourceInter fsi=(FileSourceInter)ge;
                        File[] files=fsi.getFiles();
                        ps.print("SOURCE="+ge+":");
                        String separator="";
                        for(File f:files) {
                            ps.print(separator+f.getAbsolutePath());
                            separator=",";
                        }
                        ps.println();
                    }
                    if(ge instanceof Plot) {
                        ps.println("PLOT="+ge+":plot."+plotCount+":500x400");
                        plotCount++;
                    }
                }
            } else {
                ps.println("TYPE=simple");
                List<FileSourceInter> sources=new ArrayList<FileSourceInter>();
                List<Plot> plots=new ArrayList<Plot>();
                for(GraphElement ge:graphPanel.getElements()) {
                    if(ge instanceof FileSourceInter) {
                        sources.add((FileSourceInter)ge);
                    }
                    if(ge instanceof Plot) {
                        plots.add((Plot)ge);
                    }
                }
                int sourceNumber=0;
                if(sources.size()>1) {
                    sourceNumber=JOptionPane.showOptionDialog(mainFrame,"Which source do you want to vary?",
                        "Source Selection",sources.size(),JOptionPane.QUESTION_MESSAGE,
                        null,sources.toArray(),sources.get(0));
                }
                int plotNumber=0;
                if(plots.size()>1) {
                    plotNumber=JOptionPane.showOptionDialog(mainFrame,"Which plot do you want to export?",
                        "Plot Selection",sources.size(),JOptionPane.QUESTION_MESSAGE,
                        null,plots.toArray(),plots.get(0));
                }
                ps.println("SOURCE="+sources.get(sourceNumber));
                ps.println("PLOT="+plots.get(plotNumber)+":500x400");
            }
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private GraphElement findElementByLabel(String label) {
        for(GraphElement ge:graphPanel.getElements()) {
            if(ge.toString().equals(label)) return ge;
        }
        return null;
    }

    private JFrame mainFrame;
    private GraphPanel graphPanel;
    private JPanel propPanel;
    private JPopupMenu popupMenu;
    private Object top;
    private Method closeMethod;
   
    private transient int popUpX,popUpY;
    private static final String versionHeader="SwiftVis_Version=";
}

/*
 * Known Bugs:
 * In KeySelectionFilter, moving sources isn't reflected in the selection panel.
 * Clicking on max/min checkbox after editing values is a bug, but fixing it
 *      would require some odd refactoring that I have to think about.  The
 *      event object knows what other object is getting the focus, but I don't
 *      pass that into my listener when the value changes.   
 * Its not good when I exceed the "Heap Space". All the screens go gray and I lose
 *      whatever I haven't saved (of course). I'm not sure it's possible, but perhaps 
 *      SwiftVis could be made to crash more gracefully?  In particular, can SwiftVis 
 *      check before opening a file that would exceed the available space and give a 
 *      warning message? That would be really nice.
 * After I've tried to do something difficult, SwiftVis would hang when trying to generate 
 *      a Plot. If there is no data input to the plot, the axes will come up fine. Even 
 *      with one data point though, it is "Drawing..." indefinitely. Inserting a new plot works.
 *
 * Planned Additions:
 * Add source mapping to orbit plot.
 * Menu options to stop current processing.  Requires flag checks in loops.
 * Get label colors to match data colors.  Could have it match an index of a plot style.
 * Add transparency to colors for histogram and bar plots.
 * Support for Mercury files.
 * In the plotting "Change Gradient", it would be nice if the values for the gradient
 *      could accept variables (e.g. min(v[3]) and max(v[3]) if the gradient measures v[3]).
 * Java3D and/or JOGL support. 
 * Labels for values and parameters coming from general data.
 * Help files (could reference the net and pull down HTML that would be viewed).
 * Clean up code so we don't have both sourceAltered and redo in things.
 * Give 3 options for the coord filter xv to orbel mass.
 *      []  use the mass of the central body (particle -1).
 *      []  use the total mass of the planets.
 *      []  use a user defined mass [________________].
 * Consider adding formulas to value and parameter indexing.  I need to find an application
 *      though because this really could slow stuff down some.
 * Correlation function filter.  Calculates the correlation function of a set of points.
 *      This is similar in many ways to the clustering filter, but a bit different.
 *      There might be other types of filters like this as well for measuring
 *      frequencies that are present in stuff or something like that.
 * I would like to be able to start a new Plot File from the File menu (instead of just
 *      Opening and Saving). Close the current Plot File would be nice too. (Just like
 *      most other applications: New, Open, Save, Close.)
 * Although the "General Data" source is awesome, another nice addition might be the 
 *      ability to input IDL save files and load the appropriate variables. (Otherwise, 
 *      they have to be written out to a file in text form before I can use SwiftVis on 
 *      the data.)
 * Provide some type of warning if an operation is going to come close to taking
 *      up all the heap space.
 * Have the print option remember the last printer used and whether 
 *      it was landscape or portrait.
 * It would be very useful if the window that allows you to move through your 
 *      directory tree would have a history of recent directories used.
 *      I often find my myself playing with data in very different places (for
 *      example when I am comparing models with observations) and I need to
 *      keep switching between the two directories.  It can be a pain.
 */