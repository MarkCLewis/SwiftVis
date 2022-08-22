/*
 * Created on Dec 23, 2007
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.util.BinaryInput;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;

/**
 * This source is designed to read in PKDGRAV output files.  The user can either tell
 * it what format to use or it will deduce it by the extension.  Using the extension is
 * the default but is only one of the selections.
 * 
 * @author Mark Lewis
 */
public class PKDGRAVSource extends AbstractSource implements FileSourceInter {
    public PKDGRAVSource() {}
    
    public PKDGRAVSource(PKDGRAVSource c,List<GraphElement> l) {
        super(c,l);
        selectedType=c.selectedType;
        selectedFile=c.selectedFile;
        useXRange=new EditableBoolean(c.useXRange.getValue());
        minXRange=new EditableDouble(c.minXRange.getValue());
        maxXRange=new EditableDouble(c.maxXRange.getValue());
        useYRange=new EditableBoolean(c.useYRange.getValue());
        minYRange=new EditableDouble(c.minYRange.getValue());
        maxYRange=new EditableDouble(c.maxYRange.getValue());
        useThin=new EditableBoolean(c.useThin.getValue());
        oneIn=new EditableInt(c.oneIn.getValue());
    }

    @Override
    protected void redoAllElements() {
        FILE_TYPES[selectedType].redoElements(selectedFile,dataVect,this);
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel outerPanel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(4,1));
        final JComboBox comboBox=new JComboBox(FILE_TYPES);
        comboBox.setSelectedIndex(selectedType);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedType=comboBox.getSelectedIndex();
            }
        });
        northPanel.add(comboBox);
        fileLabel=new JLabel((selectedFile==null)?"No File Selected":selectedFile.getAbsolutePath());
        northPanel.add(fileLabel);
        JButton button=new JButton("Select File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFile();
                if(selectedFile!=null) fileLabel.setText(selectedFile.getAbsolutePath());
            }
        } );
        northPanel.add(button);
        button=new JButton("Read File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        } );
        northPanel.add(button);
        outerPanel.add(northPanel,BorderLayout.NORTH);
        propPanel.add("Settings",outerPanel);
        
        JPanel panel=new JPanel(new BorderLayout());
        northPanel=new JPanel(new GridLayout(8,1));
        northPanel.add(useXRange.getCheckBox("Use x Range?",null));
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("x minimum"),BorderLayout.WEST);
        innerPanel.add(minXRange.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("x maximum"),BorderLayout.WEST);
        innerPanel.add(maxXRange.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        northPanel.add(useYRange.getCheckBox("Use y Range?",null));
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("y minimum"),BorderLayout.WEST);
        innerPanel.add(minYRange.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("y maximum"),BorderLayout.WEST);
        innerPanel.add(maxYRange.getTextField(null),BorderLayout.CENTER);
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
        if(selectedFile==null) return 0;
        return FILE_TYPES[selectedType].getNumParameters(selectedFile);
    }

    @Override
    public int getNumValues(int stream) {
        if(selectedFile==null) return 0;
        return FILE_TYPES[selectedType].getNumValues(selectedFile);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return FILE_TYPES[selectedType].getParameterDescription(selectedFile,which);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return FILE_TYPES[selectedType].getValueDescription(selectedFile,which);
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new PKDGRAVSource(this,l);
    }

    @Override
    public String getDescription() {
        return "PKDGRAV Source";
    }
    
    public static String getTypeDescription() {
        return "PKDGRAV Source";
    }
    
    @Override
    public File[] getFiles() {
        return new File[]{selectedFile};
    }

    @Override
    public void setFile(int which, File f) {
        selectedFile=f;
        if(fileLabel!=null && fileLabel!=null) fileLabel.setText(selectedFile.getAbsolutePath());
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();
    }
    
    private void selectFile() {
        JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
        selectedFile=fileChooser.getSelectedFile();
        OptionsData.instance().setLastDir(selectedFile.getParentFile());        
    }
    
    private boolean acceptThinned(float x,float y,int cnt) {
        return (!useXRange.getValue() || x>=minXRange.getValue() && x<=maxXRange.getValue()) &&
            (!useYRange.getValue() || y>=minYRange.getValue() && y<=maxYRange.getValue()) &&
            (!useThin.getValue() || cnt%oneIn.getValue()==0);
    }

    private int selectedType=0;
    private File selectedFile;
    private EditableBoolean useXRange=new EditableBoolean(false);
    private EditableDouble minXRange=new EditableDouble(0.0);
    private EditableDouble maxXRange=new EditableDouble(1.0);
    private EditableBoolean useYRange=new EditableBoolean(false);
    private EditableDouble minYRange=new EditableDouble(0.0);
    private EditableDouble maxYRange=new EditableDouble(1.0);
    private EditableBoolean useThin=new EditableBoolean(false);
    private EditableInt oneIn=new EditableInt(1);
    
    private transient JLabel fileLabel;
    
    private static final FileType[] FILE_TYPES={new DetectType(),new TextFileType(),
        new BinaryFileType()};

    private static final long serialVersionUID = -5037330487792718215L;
    
    private static interface FileType {
        String getValueDescription(File file,int which);
        String getParameterDescription(File file,int which);
        int getNumValues(File file);
        int getNumParameters(File file);
        void redoElements(File file,ArrayList<DataElement> dataVect,PKDGRAVSource source);
        String[] extensionMatches();
    }
    
    private static class DetectType implements FileType {
        @Override
        public int getNumParameters(File file) {
            int match=findMatch(file.getName().substring(file.getName().lastIndexOf('.')+1));
            if(match>0) {
                return FILE_TYPES[match].getNumParameters(file);
            }
            return 0;
        }

        @Override
        public int getNumValues(File file) {
            int match=findMatch(file.getName().substring(file.getName().lastIndexOf('.')+1));
            if(match>0) {
                return FILE_TYPES[match].getNumValues(file);
            }
            return 0;
        }

        @Override
        public String getParameterDescription(File file,int which) {
            int match=findMatch(file.getName().substring(file.getName().lastIndexOf('.')+1));
            if(match>0) {
                return FILE_TYPES[match].getParameterDescription(file,which);
            }
            return null;
        }

        @Override
        public String getValueDescription(File file,int which) {
            int match=findMatch(file.getName().substring(file.getName().lastIndexOf('.')+1));
            if(match>0) {
                return FILE_TYPES[match].getValueDescription(file,which);
            }
            return null;
        }

        @Override
        public void redoElements(File file, ArrayList<DataElement> dataVect,PKDGRAVSource source) {
            int match=findMatch(file.getName().substring(file.getName().lastIndexOf('.')+1));
            if(match>0) {
                FILE_TYPES[match].redoElements(file,dataVect,source);
            } else {
                JOptionPane.showMessageDialog(null,"Unknown file extension.  Please select the format.");
            }
        }
        
        @Override
        public String[] extensionMatches() {
            return new String[0];
        }
        
        @Override
        public String toString() {
            return "Detect from file extension";
        }
        
        public int findMatch(String ext) {
            for(int i=1; i<FILE_TYPES.length; ++i) {
                for(String s:FILE_TYPES[i].extensionMatches()) {
                    if(ext.equalsIgnoreCase(s)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
    
    private static class TextFileType implements FileType {
        @Override
        public int getNumParameters(File file) {
            return paramNames.length;
        }

        @Override
        public int getNumValues(File file) {
            return valueNames.length;
        }

        @Override
        public String getParameterDescription(File file,int which) {
            return paramNames[which];
        }

        @Override
        public String getValueDescription(File file,int which) {
            return valueNames[which];
        }

        @Override
        public void redoElements(File file,ArrayList<DataElement> dataVect,PKDGRAVSource source) {
            try {
                Scanner sc=new Scanner(file);
                int[] params=new int[paramNames.length];
                float[] vals=new float[valueNames.length];
                int cnt=0;
                while(sc.hasNext()) {
                    params[0]=sc.nextInt();
                    params[1]=sc.nextInt();
                    for(int i=0; i<vals.length; ++i) {
                        vals[i]=sc.nextFloat();
                    }
                    params[2]=sc.nextInt();
                    if(source.acceptThinned(vals[2],vals[3],cnt)) {
                        dataVect.add(new DataElement(params,vals));
                    }
                    cnt++;
                }
                sc.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public String[] extensionMatches() {
            return extensions;
        }
        
        @Override
        public String toString() {
            return "Text format";
        }
        
        private static String[] paramNames={"Actual Index","Original Index","Color"};
        private static String[] valueNames={"Mass","Radius","x","y","z","vx","vy","vz",
            "wx","wy","wz"};
        private static String[] extensions={"bt","txt"};
    }
    
    private static class BinaryFileType implements FileType {
        @Override
        public String[] extensionMatches() {
            return extensions;
        }

        @Override
        public int getNumParameters(File file) {
            return paramNames.length;
        }

        @Override
        public int getNumValues(File file) {
            return valueNames.length;
        }

        @Override
        public String getParameterDescription(File file, int which) {
            return paramNames[which];
        }

        @Override
        public String getValueDescription(File file, int which) {
            return valueNames[which];
        }

        @Override
        public void redoElements(File file, ArrayList<DataElement> dataVect,PKDGRAVSource source) {
            BinaryInput bi=null;
            try {
                bi=new BinaryInput(new DataInputStream(new FileInputStream(file)));
                int[] params=new int[paramNames.length];
                float[] vals=new float[valueNames.length];
                vals[11]=(float)bi.readReal8();
                int num=bi.readIntXDR4();
                bi.readIntXDR4();
                for(int i=0; i<num; ++i) {
                    for(int j=0; j<valueNames.length-1; ++j) {
                        vals[j]=(float)bi.readXDR8();
                    }
                    params[1]=bi.readIntXDR4();
                    params[0]=bi.readIntXDR4();
                    if(source.acceptThinned(vals[2],vals[3],i)) {
                        dataVect.add(new DataElement(params,vals));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                try {
                    bi.close();
                } catch(IOException e2) {
                    e2.printStackTrace();
                }
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "Binary format";
        }
        
        private static String[] paramNames={"Original Index","Color"};
        private static String[] valueNames={"Mass","Radius","x","y","z","vx","vy","vz",
            "wx","wy","wz","Time"};
        private static String[] extensions={"ss"};
    }
}
