/*
 * Created on Sep 9, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.swri.swiftvis.util;

import javax.swing.*;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.OptionsData;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * 
 * @author Mark Lewis
 */
public class OutputInfoPanel extends JPanel {
	public OutputInfoPanel(DataSource ds) {
		source=ds;
		setupGUI();
	}
	
	public void redoOutputTable() {
	    if(source.getNumStreams()!=streamBox.getItemCount()) {
	        int oldStream=streamBox.getSelectedIndex();
            streamBox.removeAllItems();
            for(int i=0; i<source.getNumStreams(); ++i) {
                streamBox.addItem("Stream "+i);
            }
            if(oldStream<source.getNumStreams()) {
                streamBox.setSelectedIndex(oldStream);                
            } else {
                streamBox.setSelectedIndex(0);
            }
            stm.setStream(streamBox.getSelectedIndex());
	    }
	    stm.fireTableStructureChanged();
	}

	private void setupGUI() {
		setLayout(new BorderLayout());
		JPanel topPanel=new JPanel(new GridLayout(3,1));
        viewTable=new JTable();
		stm=new SourceTableModel(viewTable,source);
		streamBox=new JComboBox();
		for(int i=0; i<source.getNumStreams(); ++i) {
		    streamBox.addItem("Stream "+i);
		}
		streamBox.addActionListener(new ActionListener() {
		    @Override
            public void actionPerformed(ActionEvent e) {
		        stm.setStream(streamBox.getSelectedIndex());
		    }
		});
		topPanel.add(streamBox);
        JButton writeButton=new JButton("Write to Text File");
        writeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { writeToTextFile(); }
        } );
        topPanel.add(writeButton);
        writeButton=new JButton("Write to Java Binary File");
        writeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { writeToBinaryFile(); }
        } );
        topPanel.add(writeButton);
		add(topPanel,BorderLayout.NORTH);
		viewTable.setModel(stm);
		add(new JScrollPane(viewTable),BorderLayout.CENTER);
	}

	private void writeToTextFile() {
		String[] headerOptions={"Don't Write","Top of File","Separate File"};
		Object select=JOptionPane.showInputDialog(this,"Select header options.","Header options",JOptionPane.QUESTION_MESSAGE,null,headerOptions,headerOptions[0]);
		JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
		int chooserReturn=fileChooser.showOpenDialog(this);
		if(chooserReturn==JFileChooser.CANCEL_OPTION) return;
        OptionsData.instance().setLastDir(fileChooser.getSelectedFile().getParentFile());
		String fileNameBase=fileChooser.getSelectedFile().getAbsolutePath();
        if(!fileNameBase.endsWith(".txt")) fileNameBase+=".txt";
        int stream=streamBox.getSelectedIndex();
        if(stream<0) stream=0;
		try {
			PrintWriter mainFile=new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(fileNameBase))));
			if(select.equals(headerOptions[1])) {
				for(int i=0; i<source.getNumParameters(stream); ++i) {
					mainFile.print(source.getParameterDescription(stream, i)+" "); 
				}
				for(int i=0; i<source.getNumValues(stream); ++i) {
					mainFile.print(source.getValueDescription(stream,i)+" "); 
				}
				mainFile.println();
			} else if(select.equals(headerOptions[2])) {
				PrintWriter headerFile=new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(fileNameBase+".hdr"))));
				for(int i=0; i<source.getNumParameters(stream); ++i) {
					headerFile.print(source.getParameterDescription(stream,i)+" "); 
				}
				for(int i=0; i<source.getNumValues(stream); ++i) {
					headerFile.print(source.getValueDescription(stream,i)+" "); 
				}
				headerFile.println();
				headerFile.close();
			}
			for(int i=0; i<source.getNumElements(stream); ++i) {
				DataElement de=source.getElement(i,stream);
				for(int j=0; j<de.getNumParams(); ++j) {
					mainFile.print(de.getParam(j)+" "); 
				}
				for(int j=0; j<de.getNumValues(); ++j) {
					mainFile.print(de.getValue(j)+" "); 
				}
				mainFile.println();
			}
			mainFile.close();
		} catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,"There was an exception trying to write the file.");
		}
	}

	private void writeToBinaryFile() {
		JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
		int chooserReturn=fileChooser.showOpenDialog(this);
		if(chooserReturn==JFileChooser.CANCEL_OPTION) return;
        OptionsData.instance().setLastDir(fileChooser.getSelectedFile().getParentFile());
		String fileNameBase=fileChooser.getSelectedFile().getAbsolutePath();
        if(!fileNameBase.endsWith(".jbf")) fileNameBase+=".jbf";
        int stream=streamBox.getSelectedIndex();
        if(stream<0) stream=0;
		try {
			DataOutputStream mainFile=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileNameBase))));
			for(int i=0; i<source.getNumElements(stream); ++i) {
				DataElement de=source.getElement(i,stream);
				for(int j=0; j<de.getNumParams(); ++j) {
					mainFile.writeInt(de.getParam(j)); 
				}
				for(int j=0; j<de.getNumValues(); ++j) {
					mainFile.writeFloat(de.getValue(j)); 
				}
			}
			mainFile.close();
		} catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,"There was an exception trying to write the file.");
		}
	}

    private DataSource source;

    private JTable viewTable;
    private SourceTableModel stm;
    private JComboBox streamBox;

    private static final long serialVersionUID=34496873908673l;
}
