/*
 * Created on Jun 12, 2007
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.scripting.ScriptUtil;
import edu.swri.swiftvis.util.BinaryInput;

public class ScriptSource extends AbstractSource implements FileSourceInter {
    public ScriptSource() {}
    
    private ScriptSource(ScriptSource c,List<GraphElement> l) {
        super(c,l);
        codeString=c.codeString;
        language=c.language;
        dataFiles=new ArrayList<File>(c.dataFiles);
        valueNames=Arrays.copyOf(c.valueNames,valueNames.length);
        paramNames=Arrays.copyOf(c.paramNames,valueNames.length);
    }

    @Override
    protected void redoAllElements() {
        ScriptEngineManager manager=new ScriptEngineManager();
        ScriptEngine engine=manager.getEngineByName(language);
        if(engine==null) {
            JOptionPane.showMessageDialog(propPanel,"Could not find the engine for your script.");
            return;
        }
        engine.put("files",dataFiles);
        engine.put("filesArray",dataFiles.toArray(new File[dataFiles.size()]));
        FileAccessor fileAccessor=new FileAccessor();
        engine.put("fileAccess",fileAccessor);
        engine.put("valueNames",valueNames);
        engine.put("paramNames",paramNames);
        try {
            engine.eval(OptionsData.instance().getScriptBaseCode(language)+codeString);
        } catch (ScriptException e) {
            JOptionPane.showMessageDialog(propPanel,"There was an exception running the script.");
            e.printStackTrace();
        }
        Object vNames=engine.get("valueNames");
        String[] tmp=ScriptUtil.parseToStringArray(vNames);
        if(tmp!=null) valueNames=tmp;
        Object pNames=engine.get("paramNames");
        tmp=ScriptUtil.parseToStringArray(pNames);
        if(tmp!=null) paramNames=tmp;
        Object outParams=engine.get("p");
        Object outValues=engine.get("v");
        ScriptUtil.buildElements(outParams,outValues,dataVect);
        fileAccessor.closeOut();
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());

        ScriptEngineManager manager=new ScriptEngineManager();
        List<ScriptEngineFactory> factories=manager.getEngineFactories();
        String[] languages=new String[factories.size()];
        int selected=-1;
        for(int i=0; i<factories.size(); ++i) {
            ScriptEngineFactory sef=factories.get(i);
            languages[i]=sef.getLanguageName();
            if(language.equals(languages[i])) selected=i;
        }
        if(selected==-1) {
            selected=0;
            language=factories.get(0).getLanguageName();
        }
        final JComboBox engineBox=new JComboBox(languages);
        engineBox.setSelectedIndex(selected);
        engineBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                language=(String)engineBox.getSelectedItem();
            }
        });
        panel.add(engineBox,BorderLayout.NORTH);
        
        final JTextArea textArea=new JTextArea(codeString);
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                codeString=textArea.getText();
            }
        });
        panel.add(textArea,BorderLayout.CENTER);

        JPanel southPanel=new JPanel(new BorderLayout());
        JButton editorButton=new JButton("Launch External Editor");
        editorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScriptEngineManager manager=new ScriptEngineManager();
                List<ScriptEngineFactory> factories=manager.getEngineFactories();
                String ext=".txt";
                for(ScriptEngineFactory sef:factories) {
                    if(sef.getLanguageName().equals(language)) ext=sef.getExtensions().get(0);
                }
                textArea.setText(OptionsData.instance().launchEditor(textArea.getText(),ext));
            }
        });
        southPanel.add(editorButton,BorderLayout.NORTH);
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
        propPanel.add("Script Code",panel);
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
    
    @Override
    public int getNumParameters(int stream) {
        if(!dataVect.isEmpty()) return dataVect.get(0).getNumParams();
        return paramNames.length;
    }

    @Override
    public int getNumValues(int stream) {
        if(!dataVect.isEmpty()) return dataVect.get(0).getNumValues();
        return valueNames.length;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(paramNames==null || which>=paramNames.length) return "Unspecified";
        return paramNames[which];
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(valueNames==null || which>=valueNames.length) return "Unspecified";
        return valueNames[which];
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new ScriptSource(this,l);
    }

    @Override
    public String getDescription() {
        return "Script Source";
    }

    public static String getTypeDescription() {
        return "Script Source";
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

    private String codeString="";
    private String language="ECMAScript";
    private ArrayList<File> dataFiles=new ArrayList<File>();
    private String[] valueNames=new String[0];
    private String[] paramNames=new String[0];
    private transient JList fileList;
    private static final long serialVersionUID = 4507871631685113003L;
    
    private class FileAccessor {
        public Scanner getScanner(int i) {
            try {
                if(sc[i]==null) sc[i]=new Scanner(new FileInputStream(dataFiles.get(i)));
                return sc[i];
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        public BinaryInput getBinaryInput(int i) {
            try {
                if(bi[i]==null) bi[i]=new BinaryInput(new DataInputStream(new FileInputStream(dataFiles.get(i))));
                return bi[i];
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }            
        }
        
        public void closeOut() {
            for(int i=0; i<sc.length; ++i) {
                if(sc[i]!=null) sc[i].close();
                if(bi[i]!=null) {
                    try {
                        bi[i].close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        private Scanner[] sc=new Scanner[dataFiles.size()];
        private BinaryInput[] bi=new BinaryInput[dataFiles.size()];
    }
}
