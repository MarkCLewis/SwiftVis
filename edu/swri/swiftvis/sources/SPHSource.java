/*
 * Created on Aug 5, 2005
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.util.BinaryInput;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;

/**
 * This source is intended to read in SPH binary dump files.  Addition requested by Robin Canup.
 * 
 * @author Mark Lewis
 */
public class SPHSource extends AbstractSource {
    public SPHSource() {}
    
    public SPHSource(SPHSource c,List<GraphElement> l) {
        super(c,l);
        directory=c.directory;
        nameTemplate=new EditableString(c.nameTemplate.getValue());
        numDigits=new EditableInt(c.numDigits.getValue());
        startValue=new EditableInt(c.startValue.getValue());
        endValue=new EditableInt(c.endValue.getValue());
    }

    @Override
    protected void redoAllElements() {
        for(int i=startValue.getValue(); i<=endValue.getValue(); ++i) {
            StringBuffer num=new StringBuffer(Integer.toString(i));
            for(int j=num.length(); j<numDigits.getValue(); ++j) {
                num.insert(0,'0');
            }
            int numLoc=nameTemplate.getValue().indexOf('#');
            String name;
            if(numLoc>=0) name=nameTemplate.getValue().substring(0,numLoc)+num+nameTemplate.getValue().substring(numLoc+1);
            else name=nameTemplate.getValue()+num;
            readFile(new File(directory,name));
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel outerPanel=new JPanel(new BorderLayout());
        JPanel panel=new JPanel(new GridLayout(6,1));
        
        dirLabel=new JLabel((directory==null)?"No File Selected":directory.getAbsolutePath());
        panel.add(dirLabel);
        JButton button=new JButton("Select Directory");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectDir();
                if(directory!=null) dirLabel.setText(directory.getAbsolutePath());
            }
        } );
        panel.add(button);
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Filename Specification"),BorderLayout.WEST);
        innerPanel.add(nameTemplate.getTextField(null),BorderLayout.CENTER);
        panel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Number of Digits"),BorderLayout.WEST);
        innerPanel.add(numDigits.getTextField(null),BorderLayout.CENTER);
        panel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Start Value"),BorderLayout.WEST);
        innerPanel.add(startValue.getTextField(null),BorderLayout.CENTER);
        panel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("End Value"),BorderLayout.WEST);
        innerPanel.add(endValue.getTextField(null),BorderLayout.CENTER);
        panel.add(innerPanel);
        outerPanel.add(panel,BorderLayout.NORTH);
        
        dataList=new JList(dataNames);
        dataList.setSelectedIndices(valIndexes);
        dataList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                valIndexes=dataList.getSelectedIndices();
            }
        });
        outerPanel.add(dataList,BorderLayout.CENTER);
        
        button=new JButton("Read Files");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        } );
        outerPanel.add(button,BorderLayout.SOUTH);
        propPanel.add("Settings",outerPanel);
    }

    @Override
    public int getNumParameters(int stream) {
        return 0;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return null;
    }

    @Override
    public int getNumValues(int stream) {
        return valIndexes.length;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return dataNames[valIndexes[which]];
    }
    
    public static String getTypeDescription() {
        return "SPH Source";
    }

    @Override
    public String getDescription() {
        return "SPH Source";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new SPHSource(this,l);
    }
    
    private void readFile(File file) {
        try {
            BinaryInput bi=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(file))));
            int numParts=bi.readIntXDR4();
            float[] svals=new float[NUM_HEADER];
            svals[0]=bi.readIntXDR4();
            for(int i=1; i<NUM_HEADER; ++i) {
                svals[i]=(float)bi.readXDR4();
            }
            float[][] pvals=new float[numParts][NUM_PER];
            for(int i=0; i<NUM_PER; ++i) {
                int np=bi.readIntXDR4();
                if(np!=numParts) {
                    JOptionPane.showMessageDialog(propPanel,"Error: array "+i+" length not equal numParts:"+np+"!="+numParts);
                }
                for(int j=0; j<numParts; ++j) {
                    pvals[j][i]=(float)bi.readXDR4();
                }
            }
            bi.close();
            int[] params=new int[0];
            float[] vals=new float[valIndexes.length];
            for(int i=0; i<numParts; ++i) {
                for(int j=0; j<valIndexes.length; ++j) {
                    if(valIndexes[j]<NUM_HEADER) {
                        vals[j]=svals[valIndexes[j]];
                    } else {
                        vals[j]=pvals[i][valIndexes[j]-NUM_HEADER];
                    }
                }
                dataVect.add(new DataElement(params,vals));
            }
            bi.close();
        } catch(FileNotFoundException e) {
            JOptionPane.showMessageDialog(propPanel,"Couldn't open file "+file.getName()+".");
        } catch(IOException e) {
            JOptionPane.showMessageDialog(propPanel,"Exception while reading "+file.getName()+".");            
            e.printStackTrace();
        }
    }
    
    private void selectDir() {
        JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
        directory=fileChooser.getSelectedFile();
        OptionsData.instance().setLastDir(directory);        
    }

    private File directory;
    private EditableString nameTemplate=new EditableString("file#");
    private EditableInt numDigits=new EditableInt(3);
    private EditableInt startValue=new EditableInt(1);
    private EditableInt endValue=new EditableInt(1);
    private int[] valIndexes={3,8,9,10,11,12,13};
    
    private transient JLabel dirLabel;
    private transient JList dataList;

    private static final int NUM_HEADER=8;
    private static final int NUM_PER=14;
    private static final String[] dataNames={"n1","udist","xmass","t","trot","tkin","tgrav","tterm",
        "x","y","z","vx","vy","vz","u","temp","pr","pmass","rho","h","alpha","matter"};
    private static final long serialVersionUID=34545897459863467l;
}
