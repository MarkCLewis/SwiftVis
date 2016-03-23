package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.util.EditableBoolean;

/**
 * This class is a data source that can be used to load in almost any type of data
 * file, whether it is text or binary in format.
 * 
 * @author Mark Lewis
 */
public class GeneralData extends AbstractSource implements FileSourceInter {
    public GeneralData() {
        parameterDescription=new ArrayList<String>();
        valueDescription=new ArrayList<String>();
    }
    
    private GeneralData(GeneralData c,List<GraphElement> l) {
        super(c,l);
        dataFile=c.dataFile;
        numParameters=c.numParameters;
        numValues=c.numValues;
        parameterDescription=new ArrayList<String>(c.parameterDescription);
        valueDescription=new ArrayList<String>(c.valueDescription);
        readType=c.readType;
        maxAllowed=c.maxAllowed;
        offset=c.offset;
        totalElements=c.totalElements;
        format=c.format;
        binary=c.binary;
    }

    @Override
    public String getDescription(){ return "General Data"; }

    public static String getTypeDescription(){ return "General Data"; }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        
		JPanel innerPanel=new JPanel(new GridLayout(3,1));
        String[] ssOptions={"Simple Text","Special Format"};
        final JComboBox ssComboBox=new JComboBox(ssOptions);
        ssComboBox.setSelectedIndex(readType);
        innerPanel.add(ssComboBox);
        String[] binaryOptions={"Text","Binary"};
        final JComboBox binaryComboBox=new JComboBox(binaryOptions);
        binaryComboBox.setSelectedIndex(binary?1:0);
        innerPanel.add(binaryComboBox);
        innerPanel.add(intsAsParams.getCheckBox("Make text ints be parameters. (Only simple text.)",null));
        panel.add(innerPanel,BorderLayout.NORTH);

        final JTextArea specialFormatArea=new JTextArea(format);
        specialFormatArea.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                format=specialFormatArea.getText();
            }
        });
        panel.add(new JScrollPane(specialFormatArea),BorderLayout.CENTER);
        
        if(readType==TEXT_TYPE) {
            binaryComboBox.setEnabled(false);
            specialFormatArea.setEditable(false);
        }
        ssComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readType=ssComboBox.getSelectedIndex();
                if(readType==TEXT_TYPE) {
                    binaryComboBox.setEnabled(false);
                    specialFormatArea.setEditable(false);
                } else {
                    binaryComboBox.setEnabled(true);
                    specialFormatArea.setEditable(true);
                }
            }
        });
        binaryComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                binary=binaryComboBox.getSelectedIndex()==1;
            }
        });
        
        innerPanel=new JPanel(new GridLayout(3,1));
        fileLabel=new JLabel((dataFile==null)?"No File Selected":dataFile.getAbsolutePath());
        innerPanel.add(fileLabel);
        JButton button=new JButton("Select File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFile();
                if(dataFile!=null) fileLabel.setText(dataFile.getAbsolutePath());
            }
        } );
        innerPanel.add(button);
        button=new JButton("Read File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        } );
        innerPanel.add(button);
        panel.add(innerPanel,BorderLayout.SOUTH);
        
		propPanel.add("Settings",panel);
    }

    /**
     * Tells you what a particular parameter is used for.
     */
    @Override
    public String getParameterDescription(int stream, int which){
        String ret=null;
        if(which<parameterDescription.size()) ret=parameterDescription.get(which);
        if(ret==null) return "Not Specified";
        return ret;
    }

    public void setParameterDescription(int which,String desc) {
		while(parameterDescription.size()<=which) {
            parameterDescription.add("");
        }
        parameterDescription.set(which,desc);
    }

    /**
     * Tells you what a particular value is used for.
     */
    @Override
    public String getValueDescription(int stream, int which){
        String ret=null;
        if(which<valueDescription.size()) ret=valueDescription.get(which);
        if(ret==null) return "Not Specified";
        return ret;
    }

    public void setValueDescription(int which,String desc) {
		while(valueDescription.size()<=which) {
            valueDescription.add("");
        }
        valueDescription.set(which,desc);
    }

    @Override
    public int getNumParameters(int stream) {
        return numParameters;
    }

    @Override
    public int getNumValues(int stream) {
        return numValues;
    }


    @Override
    public DataElement getElement(int i, int stream){
		if(i<offset || i>=offset+dataVect.size()) {
			if(readType==TEXT_TYPE) {
                if(br==null) {
                    readText();
                }
				if(i<offset || i>=offset+dataVect.size()) redoBufferText(i);
			} else {
                if(sfr==null) {
                    readSpecial();
                }
				redoBufferSpecial(i);
			}
		}
		return dataVect.get(i-offset);
    }

    /**
     * Returns the number of data elements that this source has in it.  I'm using
     * this instead of an iterator because direct access is much more efficient
     * when trying to make tables of data.
     * @return The number of data elements in this source.
     * 
     * @uml.property name="totalElements"
     */
    @Override
    public int getNumElements(int stream) {
        return totalElements;
    }

    @Override
    public GeneralData copy(List<GraphElement> l) {
        return new GeneralData(this,l);
    }

	/**
	 * This method is called by the SpecialFormatReader if it tries to read something
	 * when the stream it is pulling from is null.  This behavior is expected when
	 * the object has been nuserialized from disk. 
	 */
	public void specialFormatReset() {
		offset=1;
		redoBufferSpecial(0);
	}
    
    @Override
    public File[] getFiles() {
        return new File[]{dataFile};
    }

    @Override
    public void setFile(int which,File f) {
        dataFile=f;
        if(dataFile!=null && fileLabel!=null) fileLabel.setText(dataFile.getAbsolutePath());
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();   
    }
    
    @Override
    protected void redoAllElements() {
        readFile();
    }

	private void selectFile() {
		JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
		if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
		dataFile=fileChooser.getSelectedFile();
        OptionsData.instance().setLastDir(dataFile.getParentFile());
    }
        
    private void readFile() {
        if(dataFile==null) {
            JOptionPane.showMessageDialog(propPanel,"You must select a file to read first.");
            return;
        }
        if(readType==TEXT_TYPE) {
            readText();
        } else {
            readSpecial();
        }
	}

    private void readText() {
    	readType=TEXT_TYPE;
    	totalElements=0;
        numParameters=0;
        numValues=0;
        try {
        	if(br!=null) br.close();
			br=new BufferedReader(new FileReader(dataFile));
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }
        try {
			DataElement de=readTextLine();
        	while(de!=null) {
				totalElements++;
                if(de.getNumParams()>numParameters) numParameters=de.getNumParams();
                if(de.getNumValues()>numValues) numValues=de.getNumValues();
				de=readTextLine();
			}
		} catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(propPanel,"IOException before end of file.");
		} catch(NumberFormatException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(propPanel,"NumberFormatException before end of file.");
		}
        int size=8+4*numParameters+4*numValues;
        maxAllowed=OptionsData.instance().getSourceSize()/size;
        offset=1;
        redoBufferText(0);
    }

    private void readSpecial() {
    	readType=SPECIAL_TYPE;
    	totalElements=0;
        numParameters=0;
        numValues=0;
        try {
	        sfr=new SpecialFormatReader(this,dataFile,format,binary);
            while(true) {
                DataElement de=sfr.readNextElement();
                totalElements++;
                if(de.getNumParams()>numParameters) numParameters=de.getNumParams();
                if(de.getNumValues()>numValues) numValues=de.getNumValues();
            }
        } catch(EOFException e) {
        } catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(propPanel,"IOException before end of file.");
        } catch(NumberFormatException e) {
        	e.printStackTrace();
			JOptionPane.showMessageDialog(propPanel,"NumberFormatException before end of file.");
        }
		int size=8+4*numParameters+4*numValues;
		maxAllowed=OptionsData.instance().getSourceSize()/size;
		offset=1;
		redoBufferSpecial(0);
    }

    private void redoBufferSpecial(int elemNum) {
        int cnt;
        if (elemNum < offset) {
            cnt = 0;
            offset = cnt;
            try {
                sfr.resetStream();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            dataVect.clear();
        } else {
            cnt = offset + dataVect.size();
            if (elemNum < cnt + 5) {
                int overlap = (int)(maxAllowed / 10);
                if (overlap > dataVect.size())
                    overlap = dataVect.size();
                for (int i = 0; i < overlap; i++) {
                    dataVect.set(i, dataVect.get(dataVect.size() - overlap + i));
                }
                while(dataVect.size()>overlap) {
                    dataVect.remove(dataVect.size()-1);
                }
//                dataVect.setSize(overlap);
                offset = cnt - overlap;
            } else {
                offset = cnt;
                dataVect.clear();
            }
        }
        try {
            while (dataVect.size() < maxAllowed) {
                DataElement de = sfr.readNextElement();
                dataVect.add(de);
            }
        } catch (EOFException e) {
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                propPanel,
                "IOException while buffering.  Vector size is "
                    + dataVect.size());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                propPanel,
                "NumberFormatException while buffering.  Vector size is "
                    + dataVect.size());
        }
    }

    
	private void redoBufferText(int elemNum) {
		int cnt;
		if(elemNum<offset || br==null) {
			cnt=0;
			offset=cnt;
			try {
				if(br!=null) br.close();
				br=new BufferedReader(new FileReader(dataFile));
			} catch(IOException e) {
				e.printStackTrace();
				return;
			}
			dataVect.clear();
		} else {
			cnt=offset+dataVect.size();
			if(elemNum<=cnt) {
				int overlap=(int)(maxAllowed/10);
				if(overlap>dataVect.size()) overlap=dataVect.size();
				for(int i=0; i<overlap; i++) {
					dataVect.set(i,dataVect.get(dataVect.size()-overlap+i));
				}
                while(dataVect.size()>overlap) {
                    dataVect.remove(dataVect.size()-1);
                }
//				dataVect.setSize(overlap);
				offset=cnt-overlap;
			} else {
				offset=elemNum;
				dataVect.clear();
			}
		}
		boolean flag=true;
		while(flag && dataVect.size()<maxAllowed) {
			try {
				DataElement de=readTextLine();
				if(de==null) flag=false;
				else {
					if(cnt>=elemNum) dataVect.add(de);
					cnt++;
				}
			} catch(IOException e) {
				e.printStackTrace();
				flag=false;
			}
		}
	}
    
    private DataElement readTextLine() throws IOException {
    	String line=br.readLine();
    	if(line==null || line.length()<1) return null;
		StringTokenizer st=new StringTokenizer(line," \t,");
		DataElement de=new DataElement(new int[0],new float[0]);
        if(intsAsParams.getValue()) {
    		while(st.hasMoreTokens()) {
    			String token=st.nextToken();
                try {
                    de=de.addParam(Integer.parseInt(token));
                } catch(NumberFormatException e) {
                    try {
                        de=de.addValue(Float.parseFloat(token));
                    } catch(NumberFormatException e2) {
                        System.err.println("Non-number found: "+token);
                        e.printStackTrace();
                    }
                }
    		}
        } else {
            while(st.hasMoreTokens()) {
                String token=st.nextToken();
                try {
                    de=de.addValue(Float.parseFloat(token));
                } catch(NumberFormatException e) {
                    System.err.println("Non-number found: "+token);
                    e.printStackTrace();
                }
            }            
        }
		return de;
    }

    private File dataFile;
    private int numParameters;
    private int numValues;
    private EditableBoolean intsAsParams=new EditableBoolean(true);

    private ArrayList<String> parameterDescription;
    private ArrayList<String> valueDescription;

    private transient SpecialFormatReader sfr;

	// Data for handling large files.
	private int readType;
    private String format;
    private boolean binary;
	private long maxAllowed;

    private int offset;

	private transient BufferedReader br;
    private transient JLabel fileLabel;

    private int totalElements;
	
	private static final int TEXT_TYPE=0;
	private static final int SPECIAL_TYPE=1;
    private static final long serialVersionUID=134698237569l;
}
