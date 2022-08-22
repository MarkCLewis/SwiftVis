/*
 * Created on Jul 31, 2009
 */
package edu.swri.swiftvis;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import edu.swri.swiftvis.sources.FileSourceInter;

public class SourceRereader {
    public SourceRereader(JFrame frame,final List<GraphElement> lst) {
        final JDialog dialog=new JDialog(frame,"Source Path Change",false);
        dialog.setLayout(new BorderLayout());
        
        JPanel bigPanel=new JPanel(new GridLayout(1,1));
        
        // List of source files
        bigPanel.setBorder(BorderFactory.createTitledBorder("Source Files"));
        final ArrayList<SourceListing> files=compileFiles(lst);
        final JList fileList=new JList(files.toArray());
        bigPanel.add(new JScrollPane(fileList));
        int[] allIndexes=new int[files.size()];
        for(int i=0; i<allIndexes.length; ++i) allIndexes[i]=i;
        fileList.setSelectedIndices(allIndexes);
        
        dialog.add(bigPanel,BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel=new JPanel(new FlowLayout());
        JButton button=new JButton("OK");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indices=fileList.getSelectedIndices();
                for(int index:indices) {
                    files.get(index).source.rereadFiles();
                }
            }
        });
        buttonPanel.add(button);
        button=new JButton("Cancel");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        buttonPanel.add(button);
        dialog.add(buttonPanel,BorderLayout.SOUTH);
        
        dialog.setSize(400,500);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    private ArrayList<SourceListing> compileFiles(List<GraphElement> lst) {
        ArrayList<SourceListing> ret=new ArrayList<SourceListing>();
        for(GraphElement ge:lst) {
            if(ge instanceof FileSourceInter) {
                ret.add(new SourceListing((FileSourceInter)ge));
            }
        }
        return ret;
    }
    
    private static class SourceListing {
        public SourceListing(FileSourceInter fsi) {
            source=fsi;
            StringBuffer sb=new StringBuffer();
            for(File f:source.getFiles()) {
                if(f!=null) sb.append(f.getName()+" ");
            }
            flist=sb.toString();
        }
        @Override
        public String toString() {
            return source.toString()+" : "+flist;
        }
        private FileSourceInter source;
        private String flist;
    }
}
