/*
 * Created on Jun 8, 2007
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.scripting.ScriptUtil;

/**
 * This filter is intended to work with the scripting abilities of Java that were introduced
 * in Java 6.  This code is intended to work with any of the scripting languages that are
 * developed for Java as long as the user has the proper JAR files for them.
 * 
 * The real trick to doing scripting is how to get information into and out of the scripts.
 * We register a number of standard variables with the script engine before the script is
 * run and have to decode some variables that are passed back.  The decoding is the most
 * challenging part of this filter because it is designed to handle all the different scripting
 * languages.  Technically that isn't possible, but code is provided that can work with the
 * languages we have tried and more could be added in the future as people bring in other
 * languages.
 * 
 * @author Mark Lewis
 */
public class ScriptFilter extends AbstractMultipleSourceFilter {
    
    public ScriptFilter() {
        
    }
    
    public ScriptFilter(ScriptFilter c,List<GraphElement> l) {
        super(c,l);
        code=c.code;
        language=c.language;
        valueNames=Arrays.copyOf(c.valueNames,valueNames.length);
        paramNames=Arrays.copyOf(c.paramNames,valueNames.length);
    }

    @Override
    protected boolean doingInThreads() {
        return false;
    }

    @Override
    protected void redoAllElements() {
        ScriptEngineManager manager=new ScriptEngineManager();
        ScriptEngine engine=manager.getEngineByName(language);
        if(engine==null) {
            JOptionPane.showMessageDialog(propPanel,"Could not find the engine for your script.");
            return;
        }
        int[][][] params=new int[inputVector.size()][][];
        float[][][] vals=new float[inputVector.size()][][];
        for(int i=0; i<params.length; ++i) {
            DataSource ds=inputVector.get(i);
            params[i]=new int[ds.getNumElements(0)][];
            vals[i]=new float[ds.getNumElements(0)][];
            for(int j=0; j<ds.getNumElements(0); ++j) {
                DataElement de=ds.getElement(j, 0);
                params[i][j]=new int[de.getNumParams()];
                vals[i][j]=new float[de.getNumValues()];
                for(int k=0; k<de.getNumParams(); ++k) {
                    params[i][j][k]=de.getParam(k);
                }
                for(int k=0; k<de.getNumValues(); ++k) {
                    vals[i][j][k]=de.getValue(k);
                }
            }
        }
        engine.put("params",params);
        engine.put("values",vals);
        try {
            Object returnVal=engine.eval(OptionsData.instance().getScriptBaseCode(language)+code);
            Object vNames=engine.get("valueNames");
            String[] tmp=ScriptUtil.parseToStringArray(vNames);
            if(tmp!=null) valueNames=tmp;
            Object pNames=engine.get("paramNames");
            tmp=ScriptUtil.parseToStringArray(pNames);
            if(tmp!=null) paramNames=tmp;
            Object outParams=engine.get("p");
            Object outValues=engine.get("v");
            if(outParams==null && outValues==null) outValues=returnVal;
            ScriptUtil.buildElements(outParams,outValues,dataVect.get(0));
        } catch (ScriptException e) {
            JOptionPane.showMessageDialog(propPanel,"There was an exception running the script.");
            e.printStackTrace();
        }
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

        final JTextArea codeArea=new JTextArea(code);
        codeArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                code=codeArea.getText();
            }
        });
        panel.add(codeArea,BorderLayout.CENTER);
        
        JPanel southPanel=new JPanel(new GridLayout(2,1));
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
                codeArea.setText(OptionsData.instance().launchEditor(codeArea.getText(),ext));
            }
        });
        southPanel.add(editorButton,BorderLayout.NORTH);
        JButton button=new JButton("Run Code/Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        southPanel.add(button);
        panel.add(southPanel,BorderLayout.SOUTH);
        
        propPanel.addTab("Script",panel);
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
        return new ScriptFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Script Filter";
    }
    
    public static String getTypeDescription() {
        return "Script Filter";
    }
    
    private String code="";
    private String language="ECMAScript";
    private String[] valueNames=new String[0];
    private String[] paramNames=new String[0];
    
    private static final long serialVersionUID = -5955467625818145175L;
        
}
