/*
 * Created on Jul 22, 2005
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
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
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;

/**
 * This source reads in binned data into a format that can easily be fed into sufrace plots.
 * 
 * It can read files with fixed sizes in both the primary and secondary directions, or files where
 * the size in the primary direction is variable.  The format of the file is given below.  Note that
 * it is a binary format.  The use of multiple lines below is only to help the reader of this
 * documentation.  Currently all ints and reals are read as 4 byte values.
 * 
 * binCountSecondary binCountPrimary
 * minSecondary maxSecondary
 * minPrimary maxPrimary
 * numParameters paramString0 paramString1 ...
 * numValues valueString2 valueString3 ...
 * [primaryValue] (p0 p1 ... v2 v3 ...)^binCountSecondary
 * [primaryValue] (p0 p1 ... v2 v3 ...)^binCountSecondary
 * ...
 * 
 * The bin counts and the parameters are ints, everything else is a float.  v1 and v2 are the
 * primary and secondary values for each cell and their names reflect that so they aren't read in.
 * If minPrimary and maxPrimary are both zero, then the primaryValue is read for each set of elements
 * and reading continues to the end of the file.  Otherwise, reading continues until binCountPrimary
 * sets have been read. 
 * 
 * @author Mark Lewis
 */
public class Fixed2DBinnedSource extends AbstractSource implements FileSourceInter {
    public Fixed2DBinnedSource() {}
    
    private Fixed2DBinnedSource(Fixed2DBinnedSource c,List<GraphElement> l) {
        super(c,l);
        dataFile=c.dataFile;
        paramNames=c.paramNames;
        valueNames=c.valueNames;
        selection=c.selection;
        numKeepParams=c.numKeepParams;
        numKeepVals=c.numKeepVals;
        useRange=new EditableBoolean(c.useRange.getValue());
        minRange=new EditableDouble(c.minRange.getValue());
        maxRange=new EditableDouble(c.maxRange.getValue());
        useThin=new EditableBoolean(c.useThin.getValue());
        oneIn=new EditableInt(c.oneIn.getValue());
    }

    @Override
    protected void redoAllElements() {
        if(dataFile==null) {
            return;
        }
        BinaryInput binIn=null;
        boolean variablePrimary=false;
        try {
            binIn=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile))));
            int binCountSecondary=binIn.readInt4();
            int binCountPrimary=binIn.readInt4();
            double minSecondary=binIn.readReal4();
            double maxSecondary=binIn.readReal4();
            double minPrimary=binIn.readReal4();
            double maxPrimary=binIn.readReal4();
            variablePrimary=(binCountPrimary<2);
            paramNames=new String[binIn.readInt4()];
            for(int i=0; i<paramNames.length; ++i) {
                paramNames[i]=binIn.readCString();
            }
            valueNames=new String[binIn.readInt4()];
            for(int i=0; i<valueNames.length; ++i) {
                valueNames[i]=binIn.readCString();                
            }
            double secondarySpace=(maxSecondary-minSecondary)/binCountSecondary;
            double primarySpace=(binCountPrimary>0)?(maxPrimary-minPrimary)/binCountPrimary:0;
            double primaryVal=minPrimary+0.5*primarySpace;
            int[] params=new int[numKeepParams];
            float[] vals=new float[numKeepVals+2];
            int skipCount=0;
            for(int i=0; variablePrimary || i<binCountPrimary; ++i) {
                double secondaryVal=minSecondary+0.5*secondarySpace;
                if(variablePrimary) primaryVal=binIn.readReal4();
                for(int j=0; j<binCountSecondary; ++j) {
                    int index=0;
                    int selIndex=0;
                    for(int k=0; k<paramNames.length; ++k) {
                        int param=binIn.readInt4();
                        if(index<numKeepParams) params[index]=param;
                        if(selIndex<selection.length && selection[selIndex]==k) {
                            index++;
                            selIndex++;
                        }
                    }
                    vals[0]=(float)primaryVal;
                    vals[1]=(float)secondaryVal;
                    index=2;
                    for(int k=0; k<valueNames.length; ++k) {
                        float val=(float)binIn.readReal4();
                        if(index<numKeepVals+2) vals[index]=val;
                        if(selIndex<selection.length && selection[selIndex]==k+paramNames.length) {
                            index++;
                            selIndex++;
                        }
                    }
                    if(!useRange.getValue() || (primaryVal>=minRange.getValue() && primaryVal<=maxRange.getValue())) {
                        if(!useThin.getValue() || skipCount==0) {
                            dataVect.add(new DataElement(params,vals));
                        }
                    }
                    secondaryVal+=secondarySpace;
                }
                skipCount++;
                if(!useThin.getValue() || skipCount>=oneIn.getValue()) skipCount=0;
                primaryVal+=primarySpace;
            }
            binIn.close();
        } catch(EOFException e) {
            if(!variablePrimary) {
                JOptionPane.showMessageDialog(propPanel,"There was an EOF exception reading in the data file.");
                e.printStackTrace();                
            }
        } catch(IOException e) {
            JOptionPane.showMessageDialog(propPanel,"There was an exception reading in the data file.");
            e.printStackTrace();
        } finally {
            try {
                if(binIn!=null) binIn.close();
            } catch(IOException e) {
                JOptionPane.showMessageDialog(propPanel,"There was an exception closing in the data file.");
                e.printStackTrace();                
            }
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(2,1));
        fileLabel=new JLabel((dataFile==null)?"No file selected.":dataFile.getAbsolutePath());
        northPanel.add(fileLabel);
        JButton button=new JButton("Select File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
                if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
                dataFile=fileChooser.getSelectedFile();
                if(dataFile!=null) {
                    OptionsData.instance().setLastDir(dataFile.getParentFile());
                    fileLabel.setText(dataFile.getAbsolutePath());
                    readHeader();
                }
            }
        } );
        northPanel.add(button);
        panel.add(northPanel,BorderLayout.NORTH);
        
        String[] pvs=new String[paramNames.length+valueNames.length];
        for(int i=0; i<paramNames.length; ++i) {
            pvs[i]=paramNames[i];
        }
        for(int i=0; i<valueNames.length; ++i) {
            pvs[i+paramNames.length]=valueNames[i];
        }
        pvList=new JList(pvs);
        pvList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                selection=pvList.getSelectedIndices();
                redoKeeps();
            }
        });
        pvList.setSelectedIndices(selection);
        panel.add(pvList,BorderLayout.CENTER);
        
        button=new JButton("Read File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        } );
        panel.add(button,BorderLayout.SOUTH);
        propPanel.add("Settings",panel);
        
        panel=new JPanel(new BorderLayout());
        northPanel=new JPanel(new GridLayout(5,1));
        northPanel.add(useRange.getCheckBox("Use Range?",null));
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Primary minimum"),BorderLayout.WEST);
        innerPanel.add(minRange.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Primary maximum"),BorderLayout.WEST);
        innerPanel.add(maxRange.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        northPanel.add(useThin.getCheckBox("Use Thinning?",null));
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Take one in "),BorderLayout.WEST);
        innerPanel.add(oneIn.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        panel.add(northPanel,BorderLayout.NORTH);
        propPanel.add("Thinning",panel);
    }

    @Override
    public int getNumParameters(int stream) {
        return numKeepParams;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return paramNames[selection[which]];
    }

    @Override
    public int getNumValues(int stream) {
        return numKeepVals+2;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(which==0) return "Primary";
        if(which==1) return "Secondary";
        return valueNames[selection[which-2+numKeepParams]-paramNames.length];
    }

    public static String getTypeDescription() {
        return "Fixed 2D Binned Source";        
    }
    
    @Override
    public String getDescription() {
        return "Fixed 2D Binned Source";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new Fixed2DBinnedSource(this,l);
    }

    @Override
    public File[] getFiles() {
        return new File[]{dataFile};
    }

    @Override
    public void setFile(int which, File f) {
        dataFile=f;
        if(fileLabel!=null && fileLabel!=null) fileLabel.setText(dataFile.getAbsolutePath());
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();
    }
    
    private void readHeader() {
        if(dataFile==null) {
            return;
        }
        try {
            BinaryInput binIn=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(dataFile))));
            binIn.readInt4();
            binIn.readInt4();
            binIn.readReal4();
            binIn.readReal4();
            binIn.readReal4();
            binIn.readReal4();
            paramNames=new String[binIn.readInt4()];
            for(int i=0; i<paramNames.length; ++i) {
                paramNames[i]=binIn.readCString();
            }
            valueNames=new String[binIn.readInt4()];
            for(int i=0; i<valueNames.length; ++i) {
                valueNames[i]=binIn.readCString();                
            }
            String[] pvs=new String[paramNames.length+valueNames.length];
            for(int i=0; i<paramNames.length; ++i) {
                pvs[i]=paramNames[i];
            }
            for(int i=0; i<valueNames.length; ++i) {
                pvs[i+paramNames.length]=valueNames[i];
            }
            pvList.setListData(pvs);
            selection=new int[pvs.length];
            for(int i=0; i<selection.length; ++i) {
                selection[i]=i;
            }
            pvList.setSelectedIndices(selection);
            redoKeeps();
            binIn.close();
        } catch(IOException e) {
            JOptionPane.showMessageDialog(propPanel,"There was an exception reading in the data file.");
            e.printStackTrace();
        }        
    }
    
    private void redoKeeps() {
        numKeepParams=0;
        numKeepVals=0;
        for(int i=0; i<selection.length; ++i) {
            if(selection[i]<paramNames.length) numKeepParams++;
            else numKeepVals++;
        }        
    }

    private File dataFile;
    private String[] paramNames={};
    private String[] valueNames={};
    private int[] selection={};
    private int numKeepParams=0,numKeepVals=0;
    private EditableBoolean useRange=new EditableBoolean(false);
    private EditableDouble minRange=new EditableDouble(0.0);
    private EditableDouble maxRange=new EditableDouble(1.0);
    private EditableBoolean useThin=new EditableBoolean(false);
    private EditableInt oneIn=new EditableInt(1);
    private transient JLabel fileLabel;
    private transient JList pvList;
    
    private static final long serialVersionUID=234236438547834646l;
}
