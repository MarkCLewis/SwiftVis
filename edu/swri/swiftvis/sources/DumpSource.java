/* Generated by Together */

package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.TextReader;

public class DumpSource extends AbstractSource implements FileSourceInter {
    public DumpSource() {}
    
    private DumpSource(DumpSource c,List<GraphElement> l) {
        super(c,l);
        paramFile=c.paramFile;
        plFile=c.plFile;
        tpFile=c.tpFile;
        time=new EditableDouble(c.time.getValue());
        nstat=new EditableInt(c.nstat.getValue());
    }
    
    @Override
    protected void redoAllElements() {
        int[] params=new int[2];
        float[] vals=new float[8];
        if(paramFile==null) {
            vals[0]=(float)time.getValue();
        } else {
            try {
                TextReader tr=new TextReader(new BufferedReader(new FileReader(paramFile)));
                vals[0]=(float)tr.readDouble();
                tr.close();
            } catch(FileNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(propPanel,"Couldn't open the parameter file.");
            } catch(IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(propPanel,"Exception while reading the parameter file.");
            }
        }
        params[1]=0;
        if(plFile!=null) {
            try {
                TextReader tr=new TextReader(new BufferedReader(new FileReader(plFile)));
                int numPl=tr.readInt();
                for(int i=0; i<numPl; ++i) {
                    params[0]=-(i+1);
                    String line=tr.readLine();
                    String[] nums=line.split("[ ]+");
                    vals[7]=Float.parseFloat(nums[0]);
                    vals[1]=(float)tr.readDouble();
                    vals[2]=(float)tr.readDouble();
                    vals[3]=(float)tr.readDouble();
                    vals[4]=(float)tr.readDouble();
                    vals[5]=(float)tr.readDouble();
                    vals[6]=(float)tr.readDouble();
                    dataVect.add(new DataElement(params,vals));
                }
                tr.close();
            } catch(FileNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(propPanel,"Couldn't open the planet file.");
            } catch(IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(propPanel,"Exception while reading the planet file.");
            }            
        }
        if(tpFile!=null) {
            try {
                TextReader tr=new TextReader(new BufferedReader(new FileReader(tpFile)));
                int numTp=tr.readInt();
                vals[7]=0f;
                for(int i=0; i<numTp; ++i) {
                    params[0]=i+1;
                    vals[1]=(float)tr.readDouble();
                    vals[2]=(float)tr.readDouble();
                    vals[3]=(float)tr.readDouble();
                    vals[4]=(float)tr.readDouble();
                    vals[5]=(float)tr.readDouble();
                    vals[6]=(float)tr.readDouble();
                    for(int j=0; j<nstat.getValue(); ++j) {
                        if(j==0) params[1]=tr.readInt();
                        else tr.readInt();
                    }
                    for(int j=0; j<nstat.getValue(); ++j) {
                        tr.readDouble();
                    }
                    dataVect.add(new DataElement(params,vals));
                }
                tr.close();
            } catch(FileNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(propPanel,"Couldn't open the test particle file.");
            } catch(IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(propPanel,"Exception while reading the test particle file.");
            }            
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(9,1));
        paramLabel=new JLabel((paramFile==null)?"No file selected.":paramFile.getAbsolutePath());
        northPanel.add(paramLabel);
        JButton button=new JButton("Select Param File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
                if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
                paramFile=fileChooser.getSelectedFile();
                OptionsData.instance().setLastDir(paramFile.getParentFile());
                if(paramFile!=null) paramLabel.setText(paramFile.getAbsolutePath());                
            }
        });
        northPanel.add(button);
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Default Time"),BorderLayout.WEST);
        innerPanel.add(time.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        plLabel=new JLabel((plFile==null)?"No file selected.":plFile.getAbsolutePath());
        northPanel.add(plLabel);
        button=new JButton("Select Planet File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
                if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
                plFile=fileChooser.getSelectedFile();
                OptionsData.instance().setLastDir(plFile.getParentFile());
                if(plFile!=null) plLabel.setText(plFile.getAbsolutePath());                
            }
        });
        northPanel.add(button);
        tpLabel=new JLabel((tpFile==null)?"No file selected.":tpFile.getAbsolutePath());
        northPanel.add(tpLabel);
        button=new JButton("Select Test Particle File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
                if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
                tpFile=fileChooser.getSelectedFile();
                OptionsData.instance().setLastDir(tpFile.getParentFile());
                if(tpFile!=null) tpLabel.setText(tpFile.getAbsolutePath());                
            }
        });
        northPanel.add(button);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("NSTAT"),BorderLayout.WEST);
        innerPanel.add(nstat.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        button=new JButton("Read Files");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        northPanel.add(button);
        panel.add(northPanel,BorderLayout.NORTH);
        propPanel.add("Settings",panel);
    }

    @Override
    public File[] getFiles() {
        return new File[]{paramFile,plFile,tpFile};
    }

    @Override
    public void setFile(int which, File f) {
        switch(which) {
        case 0: paramFile=f; break;
        case 1: plFile=f; break;
        case 2: tpFile=f; break;
        }
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();
    }

    @Override
    public int getNumParameters(int stream) {
        return 2;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(which==0) return "Particle ID";
        else return "istat(0)";
    }

    @Override
    public int getNumValues(int stream) {
        return 8;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return names[which];
    }

    @Override
    public String getDescription() {
        return "Dump Source";
    }

    public static String getTypeDescription() {
        return "Dump Source";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new DumpSource(this,l);
    }
    
    private File paramFile;
    private EditableDouble time=new EditableDouble(0);
    private File plFile;
    //private int program;
    private File tpFile;
    private EditableInt nstat=new EditableInt(53);
    
    private transient JLabel paramLabel;
    private transient JLabel plLabel;
    private transient JLabel tpLabel;
    
    //private static final int SWIFT=0;

    private static final String[] names={"Time","x","y","z","vx","vy","vz","mass"};
    private static final long serialVersionUID=83262373689548l;
}