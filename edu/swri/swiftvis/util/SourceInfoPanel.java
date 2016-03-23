/* Generated by Together */

package edu.swri.swiftvis.util;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.DataSource;

/**
* This class provides an interface to quickly see the properties of the sources
* that are set as inputs to a given sink.  It shows a list of the various
* sources and when one it selected it provides a table showing the data that
* comes from that input.  This can also interface with the sink so that the
* user can remove sources here if they are no longer wanted.
*
* Most of the time people will want to put this into tabbed pane so that the
* user can just from this to the settings that the DataSink uses.
**/
public class SourceInfoPanel extends JPanel {
    public SourceInfoPanel(DataSink ds) {
        sink=ds;
        setupGUI();
    }

    public void redoSourceList() {
        String[] sources=new String[sink.getNumSources()];
        for(int i=0; i<sink.getNumSources(); i++) {
            sources[i]="d["+i+"] - "+sink.getSource(i);
        }
        sourceList.setListData(sources);
        redoStreamBox();
    }
    
    public void redoStreamBox() {
        if(sourceList.getSelectedIndex()<0) return;
        DataSource source=sink.getSource(sourceList.getSelectedIndex());
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
    }

    private void setupGUI() {
        setLayout(new BorderLayout());
        JPanel topPanel=new JPanel(new BorderLayout());
        sourceList=new JList();
        redoSourceList();
        sourceList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) { listSelectionChanged(); }
        } );
        topPanel.add(new JScrollPane(sourceList),BorderLayout.NORTH);
        JPanel southPanel=new JPanel(new GridLayout(2,1));
        JPanel buttonPanel=new JPanel(new GridLayout(1,3));
        JButton removeButton=new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { removeSelection(); }
        } );
        buttonPanel.add(removeButton);
		JButton moveUpButton=new JButton("Move Up");
        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { moveUpSelection(); }
        } );
        buttonPanel.add(moveUpButton);
        JButton updateButton=new JButton("Update");
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { redoSourceList(); }
        } );
        buttonPanel.add(updateButton);
        southPanel.add(buttonPanel);
        streamBox=new JComboBox();
        streamBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stm.setStream(streamBox.getSelectedIndex());
            }
        });
        southPanel.add(streamBox);
        topPanel.add(southPanel,BorderLayout.SOUTH);
        add(topPanel,BorderLayout.NORTH);
		viewTable=new JTable();
		stm=new SourceTableModel(viewTable,null);
		viewTable.setModel(stm);
        add(new JScrollPane(viewTable),BorderLayout.CENTER);
    }

    private void listSelectionChanged() {
        if(sourceList.getSelectedIndex()<0) return;
        redoStreamBox();
        stm.setSource(sink.getSource(sourceList.getSelectedIndex()));
    }

    private void removeSelection() {
        if(sourceList.getSelectedIndex()>=0) {
            sink.removeInput(sink.getSource(sourceList.getSelectedIndex()));
        }
    }

    private void moveUpSelection() {
        int index=sourceList.getSelectedIndex();
        if(index>0) {
            sink.moveUpInput(index);
        }
    }

    private DataSink sink;

    private JList sourceList;

    private JTable viewTable;
    private SourceTableModel stm;
    private JComboBox streamBox;

    private static final long serialVersionUID=5730987235l;
}