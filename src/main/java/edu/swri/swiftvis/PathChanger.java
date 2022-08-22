/*
 * Created on Jun 30, 2005
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import edu.swri.swiftvis.sources.FileSourceInter;

public class PathChanger {
    public PathChanger(JFrame frame,final List<GraphElement> lst) {
        dialog=new JDialog(frame,"Source Path Change",true);
        dialog.setLayout(new BorderLayout());
        
        JPanel bigPanel=new JPanel(new GridLayout(2,1));
        
        // List of source files
        JPanel tmpPanel=new JPanel(new GridLayout(1,1));
        tmpPanel.setBorder(BorderFactory.createTitledBorder("Source Files"));
        ArrayList<File> files=compileFiles(lst);
        tmpPanel.add(new JScrollPane(new JList(files.toArray())));
        bigPanel.add(tmpPanel);
        
        // List of shared paths
        tmpPanel=new JPanel(new GridLayout(1,1));
        tmpPanel.setBorder(BorderFactory.createTitledBorder("Paths"));
        ArrayList<File> paths=compilePaths(files);
        final JList pathList=new JList(paths.toArray());
        tmpPanel.add(new JScrollPane(pathList));
        bigPanel.add(tmpPanel);
        
        dialog.add(bigPanel,BorderLayout.CENTER);
        
        // Buttons
        JPanel southPanel=new JPanel(new GridLayout(3,1));
        final JLabel baseLabel=new JLabel("No Base Selected");
        southPanel.add(baseLabel);
        JButton button=new JButton("Select Base");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser=new JFileChooser(OptionsData.instance().getLastDir());
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if(chooser.showOpenDialog(dialog)==JFileChooser.CANCEL_OPTION) return;
                baseFile=chooser.getSelectedFile();
                baseLabel.setText(baseFile.getAbsolutePath());
            }
        });
        southPanel.add(button);
        JPanel buttonPanel=new JPanel(new FlowLayout());
        button=new JButton("OK");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(baseFile==null) {
                    JOptionPane.showMessageDialog(dialog,"You must select a base directory to change to.");
                } else {
                    changePaths(lst,(File)pathList.getSelectedValue(),baseFile);
                    dialog.setVisible(false);
                    dialog.dispose();
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
        southPanel.add(buttonPanel);
        dialog.add(southPanel,BorderLayout.SOUTH);
        
        dialog.setSize(400,500);
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    private ArrayList<File> compileFiles(List<GraphElement> lst) {
        ArrayList<File> ret=new ArrayList<File>();
        for(GraphElement ge:lst) {
            if(ge instanceof FileSourceInter) {
                File[] fs=((FileSourceInter)ge).getFiles();
                for(File f:fs) if(f!=null) ret.add(f);
            }
        }
        return ret;
    }
    
    private ArrayList<File> compilePaths(ArrayList<File> files) {
        ArrayList<File> ret=new ArrayList<File>();
        File[] roots=File.listRoots();
        for(File file:files) {
            File partFile=file.getParentFile();
            while(partFile!=null && !isInArray(partFile,roots) && !ret.contains(partFile)) {
                ret.add(partFile);
                partFile=partFile.getParentFile();
            }
        }
        return ret;
    }
    
    private <F> boolean isInArray(F f,F[] arr) {
        for(F root:arr) {
            if(f.equals(root)) return true;
        }
        return false;
    }
    
    private void changePaths(List<GraphElement> lst,File startPath,File changePath) {
        for(GraphElement ge:lst) {
            if(ge instanceof FileSourceInter) {
                FileSourceInter fsi=(FileSourceInter)ge;
                File[] oldFiles=fsi.getFiles();
                boolean changed=false;
                for(int i=0; i<oldFiles.length; ++i) {
                    if(oldFiles[i]!=null && oldFiles[i].getAbsolutePath().startsWith(startPath.getAbsolutePath())) {
                        File newFile=new File(changePath,oldFiles[i].getAbsolutePath().substring(startPath.getAbsolutePath().length()));
                        fsi.setFile(i,newFile);
                        changed=true;
                    }
                }
                if(changed) {
                    fsi.rereadFiles();
                }
            }
        }
    }
    
    private JDialog dialog;
    private File baseFile;
}
