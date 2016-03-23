/*
 * Created on Jul 3, 2005
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.scheme.SVSchemeUtil;
import edu.swri.swiftvis.scheme.SchemeConsole;
import edu.swri.swiftvis.scheme.SchemeElement;
import edu.swri.swiftvis.scheme.SchemeEnvironment;

/**
 * This is a filter that processes elements based on Scheme code that the user can input.
 * The code written here has access to all of the functions in the Option Scheme code and
 * can call on a function (elements [source=0]) which returns all of the elements of the
 * specified source as a list.  Each element of the list has the form ((p0 p1 ...) (v0 v1 ...))
 * and gives the parameters and values of the elements of the specified source.
 * 
 * The Scheme code must have a function (filterElements numSources) that will be passed the
 * number of sources for this filter.  This function should return a list of elements of the
 * same format as what the elements function returns.  That will be used as the output.  By
 * default you are provided with a method that returns a null list so no elements go throughs.
 * 
 * To do a transformation, you can write a function that transforms an element list and apply
 * it to the elements using the map function that is given by default in the Options pane.  To select
 * and transform use the selectMap with two functions that is provided for you.
 * 
 * You can also write functions of the form (parameterDescription pnum) and
 * (valueDescription vnum) that should return quoted names for the specified parameters and values.
 * What is passed in will be integers that are 0 referenced.  If these functions are not written,
 * default names will be provided.  Note the SVScheme does not have string processing so the quoted
 * names can only be single words. 
 * 
 * An example for a possible valueDescription function might be the following:
 * (define (valueDescription vnum)
 *    (cond
 *       ((= vnum 0) 'time)
 *       ((= vnum 1) 'a)
 *       ((= vnum 2) 'e)
 *       (else 'error)))
 * 
 * @author Mark Lewis
 */
public class SchemeFilter extends AbstractMultipleSourceFilter {
    public SchemeFilter() {}

    private SchemeFilter(SchemeFilter c,List<GraphElement> l) {
        super(c,l);
        codeString=c.codeString;
        numParams=c.numParams;
        numValues=c.numValues;
    }
    
    @Override
    protected boolean doingInThreads() {
		return false;
	}
        
    @Override
    protected void redoAllElements() {
        env=SVSchemeUtil.filterEnvironment(this);
        SchemeConsole.parseAndExecuteMany(codeString,env);
        SchemeElement func=env.getNameValue("filterElements");
        if(func==null) {
            JOptionPane.showMessageDialog(propPanel,"You must define a filterElements function.");
            return;
        }
        SchemeElement newList=SchemeConsole.parse("(filterElements "+inputVector.size()+")",env).eval(env);
        SVSchemeUtil.fillListWithSchemeList(newList,dataVect.get(0));
        numValues=0;
        numParams=0;
        for(DataElement de:dataVect.get(0)) {
            if(numValues<de.getNumValues()) numValues=de.getNumValues();
            if(numParams<de.getNumParams()) numParams=de.getNumParams();
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        final JTextArea textArea=new JTextArea(codeString);
        panel.add(textArea,BorderLayout.CENTER);
        JButton button=new JButton("Process Code/Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                codeString=textArea.getText();
                abstractRedoAllElements();
            }
        });
        panel.add(button,BorderLayout.SOUTH);
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
        return "Scheme Filter";        
    }

    @Override
    public String getDescription() {
        return "Scheme Filter";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new SchemeFilter(this,l);
    }
    
    private String codeString="(define (filterElements numSources) ())";
    private int numValues;
    private int numParams;
    private transient SchemeEnvironment env;

    private static final long serialVersionUID=72346457459760l;
}
