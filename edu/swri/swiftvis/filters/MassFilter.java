/*
 * Created on Jul 17, 2005
 */
package edu.swri.swiftvis.filters;

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
import edu.swri.swiftvis.sources.FileSourceInter;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.TextReader;

/**
 * This filter takes a binary position source as input, and will read in a pl.in file and attach
 * masses to all of the elements.  If the data is from a SyMBA run, users should go with the
 * SyMBAMassFilter.  This filter is intended for simulations where the masses of the bodies are
 * constant and particle indices don't change over the course of the simulation.  All test particles
 * (things with p[0]>0) will be given a mass of 0.
 * 
 * This filter is needed for anyone who is working in barycenter coordinates and the conversions
 * using the CoordConvertFilter require that masses be attached to the elements.  For this purpose
 * you can also select to have elements added for the Sun that contain the mass.
 * 
 * @author Mark Lewis
 */
public class MassFilter extends AbstractSingleSourceFilter implements
        FileSourceInter {
    public MassFilter() {}
    
    public MassFilter(MassFilter c,List<GraphElement> l) {
        super(c,l);
        plFile=c.plFile;
        addSun=new EditableBoolean(c.addSun.getValue());
    }
    
    @Override
    protected boolean doingInThreads() {
		return false;
	}

    @Override
    protected void redoAllElements() {
    	if(plFile==null) return;
    	float[] masses;
    	try {
    		TextReader tr=new TextReader(new BufferedReader(new FileReader(plFile)));
    		int numPl=tr.readInt();
    		masses=new float[numPl+1];
    		for(int i=0; i<numPl; ++i) {
    			masses[i+1]=(float)tr.readDouble();
    			if(i>0) tr.readDouble();
    			tr.readDouble();
    			tr.readDouble();
    			tr.readDouble();
    			tr.readDouble();
    			tr.readDouble();
    			tr.readDouble();
    		}
    		tr.close();
    		float lastTime=-1f;
    		int[] sunParam={-1};
    		float[] sunVals={0f,0f,0f,0f,0f,0f,0f,masses[1]};
    		for(int i=0; i<input.getNumElements(0); ++i) {
    			float time=input.getElement(i, 0).getValue(0);
    			if(addSun.getValue() && time!=lastTime) {
    				sunVals[0]=time;
    				dataVect.get(0).add(new DataElement(sunParam,sunVals));
    				lastTime=time;
    			}
    			DataElement de=input.getElement(i, 0);
    			if(de.getParam(0)<0) {
    				dataVect.get(0).add(new DataElement(de,masses[-de.getParam(0)]));
    			} else {
    				dataVect.get(0).add(new DataElement(de,0f));
    			}
    		}
    	} catch(FileNotFoundException e) {
    		e.printStackTrace();
    		JOptionPane.showMessageDialog(propPanel,"Couldn't open the planet file.");
    	} catch(IOException e) {
    		e.printStackTrace();
    		JOptionPane.showMessageDialog(propPanel,"Exception while reading the planet file.");
    	}
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(4,1));
        plLabel=new JLabel((plFile==null)?"No file selected.":plFile.getAbsolutePath());
        northPanel.add(plLabel);
        JButton button=new JButton("Select Planet File");
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
        northPanel.add(addSun.getCheckBox("Add elements for the Sun?",null));
        button=new JButton("Read File");
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
        return new File[]{plFile};
    }

    @Override
    public void setFile(int which, File f) {
        plFile=f;
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();
    }

    @Override
    public int getNumParameters(int stream) {
        if(input==null) return 0;
        return input.getNumParameters(0);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return input.getParameterDescription(0, which);
    }

    @Override
    public int getNumValues(int stream) {
        if(input==null) return 0;
        return input.getNumValues(0)+1;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(which<input.getNumValues(0)) return input.getValueDescription(0, which);
        return "Mass";
    }

    @Override
    public String getDescription() {
        return "Mass Filter";
    }

    public static String getTypeDescription() {
        return "Mass Filter";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new MassFilter(this,l);
    }

    private File plFile;
    private EditableBoolean addSun=new EditableBoolean(false);

    private transient JLabel plLabel;

    private static final long serialVersionUID=985698237562375l;
}
