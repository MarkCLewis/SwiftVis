/*
 * Created on Jul 6, 2005
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.sources.FileSourceInter;
import edu.swri.swiftvis.util.BinaryInput;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.TextReader;

public class SyMBAMassFilter extends AbstractSingleSourceFilter implements FileSourceInter {
    public SyMBAMassFilter() {}
    
    private SyMBAMassFilter(SyMBAMassFilter c,List<GraphElement> l) {
        super(c,l);
        program=c.program;
        saveFormat=c.saveFormat;
        massFile=c.massFile;
        discardFile=c.discardFile;
        overrideBodies=new EditableBoolean(c.overrideBodies.getValue());
        totalBodies=new EditableInt(c.totalBodies.getValue());
    }

    @Override
    protected boolean doingInThreads() {
		return false;
	}
    
    @Override
    protected void redoAllElements() {
    	if(massFile==null && discardFile==null) {
    		return;
    	}
    	if(readers==null) initReaders();
    	Reader reader=readers[program][saveFormat];
    	try {
    		int start=0;
    		while(start<input.getNumElements(0)) {
    			double time=input.getElement(start, 0).getValue(0);
    			int end=start+1;
    			while(end<input.getNumElements(0) && input.getElement(end, 0).getValue(0)==time) end++;
    			reader.doStep(start,end);
    			start=end;
    		}
    	} catch(FileNotFoundException e) {
    		JOptionPane.showMessageDialog(propPanel,"Couldn't open one of the files.");
    		e.printStackTrace();
    	} catch(IOException e) {
    		JOptionPane.showMessageDialog(propPanel,"There was an exception reading from the files.");
    		e.printStackTrace();
    	} finally {
    		try {
    			reader.closeFiles();
    		} catch(IOException e) {
    			JOptionPane.showMessageDialog(propPanel,"Couldn't close files.  That's BAD!");
    			e.printStackTrace();
    		}
    	}
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(8,1));
        String[] programOptions={"SWIFT","SWIFTER"};
        final JComboBox programBox=new JComboBox(programOptions);
        programBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                program=programBox.getSelectedIndex();
            }
        });
        programBox.setSelectedIndex(program);
        northPanel.add(programBox);
        String[] saveOptions={"Fortran Real","XDR"};
        final JComboBox saveBox=new JComboBox(saveOptions);
        saveBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFormat=saveBox.getSelectedIndex();
            }
        });
        saveBox.setSelectedIndex(saveFormat);
        northPanel.add(saveBox);
        massFileLabel=new JLabel((massFile==null)?"No mass file selected.":massFile.getAbsolutePath());
        northPanel.add(massFileLabel);
        JButton button=new JButton("Select Mass File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
                if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
                massFile=fileChooser.getSelectedFile();
                massFileLabel.setText(massFile.getAbsolutePath());
                OptionsData.instance().setLastDir(massFile.getParentFile());        
            }
        });
        northPanel.add(button);
        discardFileLabel=new JLabel((discardFile==null)?"No discard file selected.":discardFile.getAbsolutePath());
        northPanel.add(discardFileLabel);
        button=new JButton("Select Discard File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
                if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
                discardFile=fileChooser.getSelectedFile();
                discardFileLabel.setText(discardFile.getAbsolutePath());
                OptionsData.instance().setLastDir(discardFile.getParentFile());        
            }
        });
        northPanel.add(button);
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(overrideBodies.getCheckBox("Override body count?",new EditableBoolean.Listener() {
            @Override
            public void valueChanged() {
                if(overrideBodies.getValue() && totalBodies.getValue()<1) {
                    if(input.getNumElements(0)<1) return;
                    double time0=input.getElement(0, 0).getValue(0);
                    int maxBody=0;
                    for(int i=1; input.getElement(i, 0).getValue(0)==time0; ++i) {
                        int id=input.getElement(i, 0).getParam(0);
                        if(id<0 && -id>maxBody) maxBody=-id;
                    }                    
                    totalBodies.setValue(maxBody);
                }
            }
        }),BorderLayout.WEST);
        innerPanel.add(totalBodies.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        button=new JButton("Read and Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(massFile==null && discardFile==null) {
                    JOptionPane.showMessageDialog(propPanel,"You must select either a mass file and/or a discard file.");
                    return;
                }
                abstractRedoAllElements();
            }
        });
        northPanel.add(button);
        panel.add(northPanel,BorderLayout.NORTH);
        propPanel.add("Settings",panel);
    }

    @Override
    public int getNumParameters(int stream) {
        if(input==null) return 0;
        if(discardFile==null) return input.getNumParameters(0);
        return input.getNumParameters(0)+1;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(which<input.getNumParameters(0)) return input.getParameterDescription(0, which);
        return "Current ID";
    }

    @Override
    public int getNumValues(int stream) {
        if(input==null) return 0;
        if(massFile!=null) return input.getNumValues(0)+1;
        return input.getNumValues(0);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(which<input.getNumValues(0)) return input.getValueDescription(0, which);
        return "mass";
    }
    
    public static String getTypeDescription() {
        return "SyMBA Mass Filter";
    }

    @Override
    public String getDescription() {
        return "SyMBA Mass Filter";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new SyMBAMassFilter(this,l);
    }
    
    @Override
    public File[] getFiles() {
        return new File[]{massFile,discardFile};
    }

    @Override
    public void setFile(int which, File f) {
        if(which==0) {
            massFile=f;
            if(massFileLabel!=null) massFileLabel.setText(f.getAbsolutePath());
        } else {
            discardFile=f;
            if(discardFileLabel!=null) discardFileLabel.setText(f.getAbsolutePath());
        }
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();
    }
    
    private void initReaders() {
        readers=new Reader[][]{{new SWIFTReader(),new SWIFTReaderXDR()},{}};
    }

    private int program;
    private int saveFormat;
    private EditableBoolean overrideBodies=new EditableBoolean(false);
    private EditableInt totalBodies=new EditableInt(0);
    
    private File massFile;
    private File discardFile;
    
    private transient Reader[][] readers;

    private transient JLabel massFileLabel;
    private transient JLabel discardFileLabel;

    private static final long serialVersionUID=26923596837457l;

    private interface Reader extends Serializable {
        /**
         * Reads in information from the two files and builds new elements for one timestep.
         * @param start The first element being processed.
         * @param end One after the last element being processed.
         */
        void doStep(int start,int end) throws IOException;
        void closeFiles() throws IOException;
    }
    
    private class SWIFTReader implements Reader {
        @Override
        public void doStep(int start,int end) throws IOException {
            if(actualParticle==null) {
                if(input.getNumElements(0)<1) return;
                int maxBody=0;
                for(int i=start+1; i<end; ++i) {
                    int id=input.getElement(i, 0).getParam(0);
                    if(id<0 && -id>maxBody) maxBody=-id;
                }
                if(massFile!=null) {
                    massIn=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(massFile))));
                    massIn.readInt4();  // record header
                    massIn.readReal4();
                    maxBody=massIn.readInt2();
                    massIn.close();
                    massIn=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(massFile))));
                } else if(overrideBodies.getValue()){
                    maxBody=totalBodies.getValue();
                }
                if(discardFile!=null) {
                    discardIn=new TextReader(new BufferedReader(new FileReader(discardFile)));
                    nextDiscardTime=(float)discardIn.readDouble();
                }
                params=new int[2];
                actualParticle=new int[maxBody+1];
                for(int i=0; i<actualParticle.length; ++i) {
                    actualParticle[i]=i;
                }
                masses=new float[actualParticle.length];
                elems=new DataElement[actualParticle.length];
            }
            if(discardFile!=null) {
                while(end<input.getNumElements(0) && nextDiscardTime<input.getElement(start, 0).getValue(0)) {
                    int reason=discardIn.readInt();
                    int missing;
                    if(reason<2) {
                        discardIn.readWord();  // Better be -1
                        missing=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        for(int i=0; i<actualParticle.length; ++i) {
                            if(actualParticle[i]==missing) actualParticle[i]=-1;
                        }
                    } else {
                        discardIn.readWord();  // Better be -1
                        int id1=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        discardIn.readWord();  // Better be -1
                        int id2=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        discardIn.readWord();  // Better be +1
                        int id3=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        int was,is=id3;
                        if(id3==id1) {
                            was=id2;
                            missing=id2;
                        } else {
                            was=id1;
                            missing=id1;
                        }
                        for(int i=0; i<actualParticle.length; ++i) {
                            if(actualParticle[i]==was) actualParticle[i]=is;
                        }
                    }
                    for(int i=0; i<actualParticle.length; ++i) {
                        if(actualParticle[i]>missing) actualParticle[i]--;
                    }
                    try {
                        nextDiscardTime=(float)discardIn.readDouble();
                    } catch(EOFException e) {
                        nextDiscardTime=Float.MAX_VALUE;
                    }
                }
            }
            if(massFile!=null) {
                float sourceTime=input.getElement(start, 0).getValue(0);
                boolean keepReading=true;
                int nbod=0;
                while(keepReading) {
                    massIn.readInt4();  // record header
                    float time=(float)massIn.readReal4();
                    if(Math.abs(time-sourceTime)<1e-8) {
                        keepReading=false;
                    }
                    nbod=massIn.readInt2();
                    if(nbod>masses.length) {
                        masses=new float[nbod];
                        DataElement[] tmp=new DataElement[nbod];
                        for(int i=0; i<elems.length; ++i) {
                            tmp[i]=elems[i];
                        }
                        elems=tmp;
                    }
                    massIn.readInt4();  // record footer
                    massIn.readInt4();  // mass record header
                    for(int i=0; i<nbod; ++i) {
                        masses[i]=(float)massIn.readReal4();
                    }
                    massIn.readInt4();  // mass record footer
                }
                if(nbod!=end-start+1) {
                    JOptionPane.showMessageDialog(propPanel,"Number of bodies in mass file at time "+sourceTime+" didn't match source. "+nbod+" "+start+" "+end);
                    throw new RuntimeException("Mass times and bin.dat nbod didn't match.");                
                }
                for(int i=start; i<end; ++i) {
                    elems[i-start]=new DataElement(input.getElement(i, 0),masses[i-start+1]);
                }
            } else {
                for(int i=start; i<end; ++i) {
                    elems[i-start]=input.getElement(i, 0);
                }
            }
            if(discardFile!=null) {
                for(int i=2; i<actualParticle.length; ++i) {
                    if(actualParticle[i]!=-1) {
                        if(actualParticle[i]>end-start+1) {
                            throw new RuntimeException("Found particle index too large. "+i+" "+actualParticle[i]+" "+start+" "+end);
                        }
                        params[0]=-i;
                        params[1]=-actualParticle[i];
                        dataVect.get(0).add(DataElement.replaceParams(elems[actualParticle[i]-2],params));
                    }
                }
            } else {
                for(int i=start; i<end; ++i) {
                    dataVect.get(0).add(elems[i-start]);
                }                
            }
        }
        @Override
        public void closeFiles() throws IOException {
            if(massIn!=null) massIn.close();
            if(discardIn!=null) discardIn.close();
            actualParticle=null;
        }
        private int[] actualParticle;
        private float[] masses;
        private float nextDiscardTime;
        private int[] params;
        private DataElement[] elems;
        private BinaryInput massIn;
        private TextReader discardIn;
        private static final long serialVersionUID=2497468346348659l;
    }
    private class SWIFTReaderXDR implements Reader {
        @Override
        public void doStep(int start,int end) throws IOException {
            if(actualParticle==null) {
                if(input.getNumElements(0)<1) return;
                int maxBody=0;
                for(int i=start+1; i<end; ++i) {
                    int id=input.getElement(i, 0).getParam(0);
                    if(id<0 && -id>maxBody) maxBody=-id;
                }
                if(massFile!=null) {
                    massIn=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(massFile))));
                    massIn.readXDR8();
                    maxBody=massIn.readIntXDR4();
                    massIn.close();
                    massIn=new BinaryInput(new DataInputStream(new BufferedInputStream(new FileInputStream(massFile))));
                } else if(overrideBodies.getValue()){
                    maxBody=totalBodies.getValue();
                }
                if(discardFile!=null) {
                    discardIn=new TextReader(new BufferedReader(new FileReader(discardFile)));
                    nextDiscardTime=(float)discardIn.readDouble();
                }
                params=new int[2];
                actualParticle=new int[maxBody+1];
                for(int i=0; i<actualParticle.length; ++i) {
                    actualParticle[i]=i;
                }
                masses=new float[actualParticle.length];
                elems=new DataElement[actualParticle.length];
            }
            if(discardFile!=null) {
                while(end<input.getNumElements(0) && nextDiscardTime<input.getElement(start, 0).getValue(0)) {
                    int reason=discardIn.readInt();
                    int missing;
                    if(reason<2) {
                        discardIn.readWord();  // Better be -1
                        missing=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        for(int i=0; i<actualParticle.length; ++i) {
                            if(actualParticle[i]==missing) actualParticle[i]=-1;
                        }
                    } else {
                        discardIn.readWord();  // Better be -1
                        int id1=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        discardIn.readWord();  // Better be -1
                        int id2=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        discardIn.readWord();  // Better be +1
                        int id3=discardIn.readInt();
                        discardIn.readLine();  // Skip m and r
                        discardIn.readLine();  // Skip x
                        discardIn.readLine();  // Skip v
                        int was,is=id3;
                        if(id3==id1) {
                            was=id2;
                            missing=id2;
                        } else {
                            was=id1;
                            missing=id1;
                        }
                        for(int i=0; i<actualParticle.length; ++i) {
                            if(actualParticle[i]==was) actualParticle[i]=is;
                        }
                    }
                    for(int i=0; i<actualParticle.length; ++i) {
                        if(actualParticle[i]>missing) actualParticle[i]--;
                    }
                    try {
                        nextDiscardTime=(float)discardIn.readDouble();
                    } catch(EOFException e) {
                        nextDiscardTime=Float.MAX_VALUE;
                    }
                }
            }
            if(massFile!=null) {
                float sourceTime=input.getElement(start, 0).getValue(0);
                boolean keepReading=true;
                int nbod=0;
                while(keepReading) {
                    float time=(float)massIn.readXDR4();
                    if(Math.abs(time-sourceTime)<1e-8) {
                        keepReading=false;
                    }
                    nbod=massIn.readIntXDR4();
                    if(nbod>masses.length) {
                        masses=new float[nbod];
                        DataElement[] tmp=new DataElement[nbod];
                        for(int i=0; i<elems.length; ++i) {
                            tmp[i]=elems[i];
                        }
                        elems=tmp;
                    }
                    massIn.readIntXDR4();
                    for(int i=0; i<nbod; ++i) {
                        masses[i]=(float)massIn.readXDR4();
                    }
                }
                if(nbod!=end-start+1) {
                    JOptionPane.showMessageDialog(propPanel,"Number of bodies in mass file at time "+sourceTime+" didn't match source. "+nbod+" "+start+" "+end);
                    throw new RuntimeException("Mass times and bin.dat nbod didn't match.");                
                }
                for(int i=start; i<end; ++i) {
                    elems[i-start]=new DataElement(input.getElement(i, 0),masses[i-start+1]);
                }
            } else {
                for(int i=start; i<end; ++i) {
                    elems[i-start]=input.getElement(i, 0);
                }
            }
            if(discardFile!=null) {
                for(int i=2; i<actualParticle.length; ++i) {
                    if(actualParticle[i]!=-1) {
                        if(actualParticle[i]>end-start+1) {
                            throw new RuntimeException("Found particle index too large. "+i+" "+actualParticle[i]+" "+start+" "+end);
                        }
                        params[0]=-i;
                        params[1]=-actualParticle[i];
                        dataVect.get(0).add(DataElement.replaceParams(elems[actualParticle[i]-2],params));
                    }
                }
            } else {
                for(int i=start; i<end; ++i) {
                    dataVect.get(0).add(elems[i-start]);
                }                
            }
        }
        @Override
        public void closeFiles() throws IOException {
            if(massIn!=null) massIn.close();
            if(discardIn!=null) discardIn.close();
            actualParticle=null;
        }
        private int[] actualParticle;
        private float[] masses;
        private float nextDiscardTime;
        private int[] params;
        private DataElement[] elems;
        private BinaryInput massIn;
        private TextReader discardIn;
        private static final long serialVersionUID=38239087457348l;
    }
}
