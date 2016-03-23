/*
 * Created on Jul 21, 2004
 */
package edu.swri.swiftvis.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JCheckBox;

/**
 * This class can be used to store booleans that need to be edited in a GUI.  Using this class
 * prevents the programmer from having to write event handlers.  It also makes it so that
 * when the value is changed the GUI component is updates.
 * 
 * @author Mark Lewis
 */
public final class EditableBoolean implements Serializable {
    public EditableBoolean(boolean val) { value=val; }
    
    /**
     * 
     * @uml.property name="value"
     */
    public boolean getValue() {
        return value;
    }

    /**
     * 
     * @uml.property name="value"
     */
    public void setValue(boolean val) {
        value = val;
        if (field != null)
            field.setSelected(val);
    }

    
    public JCheckBox getCheckBox(String prompt,Listener ebl) {
        if(field==null) {
            listener=ebl;
            field=new JCheckBox(prompt,value);
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { valueChanged(); }
        	} );
        }
        return field;
    }
    
    public interface Listener {
        void valueChanged();
    }
    
    private void valueChanged() {
        value=field.isSelected();
        if(listener!=null) {
            listener.valueChanged();
        }
    }

    /**
     * 
     * @uml.property name="value" 
     */
    private boolean value;

    /**
     * 
     * @uml.property name="field"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private transient JCheckBox field;

    /**
     * 
     * @uml.property name="listener"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private transient Listener listener;
    private static final long serialVersionUID=487347234754l;
}
