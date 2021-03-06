/*
 * Created on Jul 21, 2004
 */
package edu.swri.swiftvis.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.Serializable;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.swri.swiftvis.DataFormula;

/**
 * This class can be used to store doubles that need to be edited in a GUI.  Using this class
 * prevents the programmer from having to write handler code dealing with parsing the number
 * and handling it if it is incorrect.  It also makes it so that when the value is changed the
 * GUI component is updates.
 * 
 * @author Mark Lewis
 */
public final class EditableDouble implements Serializable {
    public EditableDouble(double val) { value=val; }

    public double getValue() {
        return value;
    }

    public void setValue(double val) {
        value = val;
        if (field != null)
            field.setText(Double.toString(val));
    }

    
    public JTextField getTextField(Listener edl) {
        if(field==null) {
            listener=edl;
            field=new JTextField(Double.toString(value));
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    valueChanged(); 
                }
        	} );
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    if(e.isTemporary()) return;
                    valueChanged();
                }
        	} );
        }
        return field;
    }
    
    public JPanel getLabeledTextField(String label,Listener edl) {
        JPanel ret=new JPanel(new BorderLayout());
        ret.add(new JLabel(label),BorderLayout.WEST);
        ret.add(getTextField(edl),BorderLayout.CENTER);
        return ret;
    }
    
    public interface Listener {
        void valueChanged();
    }
    
    private void valueChanged() {
        try {
            double tmp=Double.parseDouble(field.getText());
            if(tmp!=value) {
                value=tmp;
                if(listener!=null) {
                    listener.valueChanged();
                }
            }
        } catch(NumberFormatException e) {
            try {
                DataFormula df=new DataFormula(field.getText());
                double tmp=df.valueOf(null,0, 0);
                if(tmp!=value) {
                    value=tmp;
                    if(listener!=null) {
                        listener.valueChanged();
                    }
                }                
            } catch(Exception ex) {
                JOptionPane.showMessageDialog(field,"You have to enter a valid number here.");
                field.requestFocus();
            }
        }
    }

    private double value;

    private transient JTextField field;
    private transient Listener listener;
    private static final long serialVersionUID=8366734883473465l;
}
