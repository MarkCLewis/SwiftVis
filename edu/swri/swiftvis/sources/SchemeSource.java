/*
 * Created on Jul 5, 2005
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.scheme.SVSchemeUtil;
import edu.swri.swiftvis.scheme.SchemeConsole;
import edu.swri.swiftvis.scheme.SchemeElement;
import edu.swri.swiftvis.scheme.SchemeEnvironment;
import edu.swri.swiftvis.scheme.SchemeException;

/**
 * You must provide a method called (readElements) that will return the list of elements.
 * 
 * @author Mark Lewis
 */
public class SchemeSource extends AbstractSource implements FileSourceInter {
    public SchemeSource() {}
    
    private SchemeSource(SchemeSource c,List<GraphElement> l) {
        super(c,l);
        codeString=c.codeString;
        dataFiles=new ArrayList<File>(c.dataFiles);
        numValues=c.numValues;
        numParams=c.numParams;
    }

    @Override
    protected void redoAllElements() {
        try {
            FileInputStream[] fileStreams=new FileInputStream[dataFiles.size()];
            for(int i=0; i<fileStreams.length; ++i) {
                fileStreams[i]=new FileInputStream(dataFiles.get(i));
            }
            env=SVSchemeUtil.sourceEnvironment(fileStreams);
            SchemeConsole.parseAndExecuteMany(codeString,env);
            SchemeElement func=env.getNameValue("readElements");
            if(func==null) {
                JOptionPane.showMessageDialog(propPanel,"You must define a readElements function.");
                return;
            }
            SchemeElement newList=SchemeConsole.parse("(readElements "+dataFiles.size()+")",env).eval(env);
            SVSchemeUtil.fillListWithSchemeList(newList,dataVect);
            numValues=0;
            numParams=0;
            for(DataElement de:dataVect) {
                if(numValues<de.getNumValues()) numValues=de.getNumValues();
                if(numParams<de.getNumParams()) numParams=de.getNumParams();
            }
        } catch(FileNotFoundException e) {
            JOptionPane.showMessageDialog(propPanel,"The file could not be opened. "+e.getMessage());
            e.printStackTrace();            
        } catch(IOException e) {
            JOptionPane.showMessageDialog(propPanel,"There was an exception reading the file. "+e.getMessage());
            e.printStackTrace();
        } catch(SchemeException e) {
            JOptionPane.showMessageDialog(propPanel,"There was an exception reading the file. "+e.getMessage());
            e.printStackTrace();            
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        final JTextArea textArea=new JTextArea(codeString);
        panel.add(textArea,BorderLayout.CENTER);
        JPanel southPanel=new JPanel(new BorderLayout());
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.setBorder(BorderFactory.createTitledBorder("Files"));
        fileList=new JList(dataFiles.toArray());
        fileList.setPreferredSize(new Dimension(100,100));
        innerPanel.add(fileList,BorderLayout.CENTER);
        JPanel buttonPanel=new JPanel(new GridLayout(1,2));
        JButton button=new JButton("Add File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFile();
            }
        });
        buttonPanel.add(button);
        button=new JButton("Remove File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeFile();
            }
        });
        buttonPanel.add(button);
        innerPanel.add(buttonPanel,BorderLayout.SOUTH);
        southPanel.add(innerPanel,BorderLayout.CENTER);
        button=new JButton("Process Code/Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                codeString=textArea.getText();
                abstractRedoAllElements();
            }
        });
        southPanel.add(button,BorderLayout.SOUTH);
        panel.add(southPanel,BorderLayout.SOUTH);
        propPanel.add("Scheme Code",panel);
    }

    @Override
    public int getNumParameters(int stream) {
        return numParams;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        SchemeElement func=env.getNameValue("parameterDescription");
        if(func==null) {
            return "Scheme Param "+which;
        }
        SchemeElement name=SchemeConsole.parse("(parameterDescription "+which+")",env).eval(env);
        return name.toString();
    }

    @Override
    public int getNumValues(int stream) {
        return numValues;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        SchemeElement func=env.getNameValue("valueDescription");
        if(func==null) {
            return "Scheme Value "+which;
        }
        SchemeElement name=SchemeConsole.parse("(valueDescription "+which+")",env).eval(env);
        return name.toString();
    }
    
    public static String getTypeDescription() {
        return "Scheme Source";
    }

    @Override
    public String getDescription() {
        return "Scheme Source";
    }

    @Override
    public SchemeSource copy(List<GraphElement> l) {
        return new SchemeSource(this,l);
    }

    @Override
    public File[] getFiles() {
        File[] fs=new File[dataFiles.size()];
        return dataFiles.toArray(fs);
    }

    @Override
    public void setFile(int which,File f) {
        dataFiles.set(which,f);
    }

    @Override
    public void rereadFiles() {
        abstractRedoAllElements();        
    }
    
    private void addFile() {
        JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
        dataFiles.add(fileChooser.getSelectedFile());
        OptionsData.instance().setLastDir(fileChooser.getSelectedFile().getParentFile());
        fileList.setListData(dataFiles.toArray());
    }
    
    private void removeFile() {
        if(fileList.getSelectedIndex()<0) return;
        dataFiles.remove(fileList.getSelectedIndex());
        fileList.setListData(dataFiles.toArray());
    }

    private String codeString="(define (readElements numFiles) ())\n";
    private ArrayList<File> dataFiles=new ArrayList<File>();
    private int numValues;
    private int numParams;
    private transient SchemeEnvironment env;
    private transient JList fileList;
    private static final long serialVersionUID=57872309672392876l;
}
