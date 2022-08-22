/*
 * Created on Jul 7, 2004
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;

/**
 * This method goes through and adds two new parameters to the elements of the
 * single source.  The first parameter is a group number telling you which group
 * this element is in.  The second paramter is a the number for where it is in
 * the group beginning with 0 for the first element found in each group.
 * @author Mark Lewis
 */
public class GroupNumberingFilter extends AbstractSingleSourceFilter {
    public GroupNumberingFilter() {
        abstractRedoAllElements();
    }

    private GroupNumberingFilter(GroupNumberingFilter c,List<GraphElement> l) {
        super(c,l);
        groupFormula=new DataFormula(c.groupFormula);
    }

    @Override
    public int getNumParameters(int stream) {
        return input.getNumParameters(stream)+2;
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.DataSource#getParameterDescription(int)
     */
    @Override
    public String getParameterDescription(int stream, int which) {
        if(which<input.getNumParameters(stream)) {
            return input.getParameterDescription(stream,which);
        } else if(which==input.getNumParameters(stream)) {
            return "Group";
        } else {
            return "Element in Group";
        }
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.DataSource#getNumValues()
     */
    @Override
    public int getNumValues(int stream) {
        return input.getNumValues(stream);
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.DataSource#getValueDescription(int)
     */
    @Override
    public String getValueDescription(int stream, int which) {
        return input.getValueDescription(stream,which);
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.GraphElement#getDescription()
     */
    @Override
    public String getDescription() {
        return "Group Numbering Filter";
    }

    public static String getTypeDescription(){ return "Group Numbering Filter"; }

    @Override
    public GroupNumberingFilter copy(List<GraphElement> l) {
        return new GroupNumberingFilter(this,l);
    }

    @Override
    protected boolean doingInThreads() {
        return false;
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.filters.AbstractSingleSourceFilter#redoAllElements()
     */
    @Override
    protected void redoAllElements() {
        if(input==null) return;
        sizeDataVectToInputStreams();
        for(int s=0; s<getSource(0).getNumStreams(); ++s) {
            int[] newPars=new int[2];
            newPars[0]=0;
            int[] range=groupFormula.getSafeElementRange(this,s);
            DataFormula.checkRangeSafety(range,this);
            for(int i=range[0]; i<range[1]; ++i) {
                newPars[1]=0;
                double groupValue=groupFormula.valueOf(this,s,i);
                double curValue=groupValue;
                int j=i;
                while(curValue==groupValue && j<range[1]) {
                    dataVect.get(s).add(new DataElement(input.getElement(j,s),newPars));
                    newPars[1]++;
                    ++j;
                    if(j<range[1]) curValue=groupFormula.valueOf(this,s,j);
                }
                i=j-1;
                newPars[0]++;
            }
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel formPanel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new BorderLayout());
        northPanel.add(new JLabel("Grouping Formula"),BorderLayout.WEST);
        JTextField field=new JTextField(groupFormula.getFormula());
        field.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFormula((JTextField)e.getSource());
            }
        } );
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                setFormula((JTextField)e.getSource());
            }
        } );
        northPanel.add(field,BorderLayout.CENTER);
        formPanel.add(northPanel,BorderLayout.NORTH);
        propPanel.add("Settings",formPanel);
    }

    private void setFormula(JTextField field) {
        try {
            DataFormula df=new DataFormula(field.getText());
            if(!df.equals(groupFormula)) {
                groupFormula=df;
                abstractRedoAllElements();
            }
        } catch(IllegalArgumentException e) {
            field.setText(groupFormula.getFormula());
            JOptionPane.showMessageDialog(field,"That was not a correct formula.");
        }
    }

    private DataFormula groupFormula = new DataFormula("v[0]");

    private static final long serialVersionUID=3409686436098246l;
}
