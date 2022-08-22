/*
 * Created on Jul 25, 2004
 */
package edu.swri.swiftvis.plot.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;

/**
 * This class encapsulates the information for a stroke and provides a GUI for editing
 * the stroke setting.
 * 
 * @author Mark Lewis
 */
public class StrokeOptions implements Serializable {
    public interface StrokeUser {
        void applyStroke();
    }
    
    public StrokeOptions(double w,StrokeUser su) {
        width=new EditableDouble(w);
        user=su;
    }
    
    public StrokeOptions(StrokeUser su,StrokeOptions o) {
        width=new EditableDouble(o.width.getValue());
        user=su;
        capStyle=o.capStyle;
        joinStyle=o.joinStyle;
        miterLimit=new EditableDouble(o.miterLimit.getValue());
        useDash=new EditableBoolean(o.useDash.getValue());
        dash=new float[o.dash.length];
        for(int i=0; i<dash.length; ++i) {
            dash[i]=o.dash[i];
        }
        dashPhase=new EditableDouble(dashPhase.getValue());
    }
    
    public Stroke getStroke() {
        if(useDash.getValue()) {
            return new BasicStroke((float)(width.getValue()),capStyle,joinStyle,
                (float)miterLimit.getValue(),dash,(float)dashPhase.getValue());
        } else {
            return new BasicStroke((float)(width.getValue()),capStyle,joinStyle,
                    (float)miterLimit.getValue());
        }
    }
    
    public void edit() {
        if(chooser==null) {
            chooser=new StrokeChooser();
        }
        chooser.setVisible(true);
    }

    private StrokeUser user;
    private EditableDouble width;
    private int capStyle=BasicStroke.CAP_BUTT;
    private int joinStyle=BasicStroke.JOIN_BEVEL;
    private EditableDouble miterLimit = new EditableDouble(1);
    private EditableBoolean useDash=new EditableBoolean(false);
    private float[] dash={2,2};
    private EditableDouble dashPhase = new EditableDouble(0);

    private transient StrokeChooser chooser;

    private static final long serialVersionUID=82485788234672346l;
    
    private class StrokeChooser extends JFrame {
        public StrokeChooser() {
            super("Stroke Settings");
            getContentPane().setLayout(new GridLayout(8,1));
            JPanel panel=new JPanel(new BorderLayout());
            panel.add(new JLabel("Width"),BorderLayout.WEST);
            panel.add(width.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() { if(user!=null) user.applyStroke(); }
            }),BorderLayout.CENTER);
            add(panel);
            final String[] capNames={"Cap Butt","Cap Round","Cap Square"};
            final int[] capVals={BasicStroke.CAP_BUTT,BasicStroke.CAP_ROUND,BasicStroke.CAP_SQUARE};
            final JComboBox capComboBox=new JComboBox(capNames);
            capComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    capStyle=capVals[capComboBox.getSelectedIndex()];
                    if(user!=null) user.applyStroke();
                }
            });
            add(capComboBox);
            final String[] joinNames={"Join Bevel","Join Miter","Join Round"};
            final int[] joinVals={BasicStroke.JOIN_BEVEL,BasicStroke.JOIN_MITER,BasicStroke.JOIN_ROUND};
            final JComboBox joinComboBox=new JComboBox(joinNames);
            joinComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    joinStyle=joinVals[joinComboBox.getSelectedIndex()];
                    if(user!=null) user.applyStroke();
                }
            });
            add(joinComboBox);
            panel=new JPanel(new BorderLayout());
            panel.add(new JLabel("Miter Limit"),BorderLayout.WEST);
            panel.add(miterLimit.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() { if(user!=null) user.applyStroke(); }
            }),BorderLayout.CENTER);
            add(panel);
            add(useDash.getCheckBox("Use Dash?",new EditableBoolean.Listener() {
                @Override
                public void valueChanged() { if(user!=null) user.applyStroke(); }
            }),BorderLayout.CENTER);
            final String[] dashNames={"Dotted","Dashed","Dash-Dot","Long Dash"};
            final float[][] dashVals={{2,2},{10,10},{10,5,2,5},{20,20}};
            final JComboBox dashComboBox=new JComboBox(dashNames);
            dashComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dash=dashVals[dashComboBox.getSelectedIndex()];
                    if(user!=null) user.applyStroke();
                }
            });
            add(dashComboBox);
            panel=new JPanel(new BorderLayout());
            panel.add(new JLabel("Dash Phase"),BorderLayout.WEST);
            panel.add(dashPhase.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() { if(user!=null) user.applyStroke(); }
            }),BorderLayout.CENTER);
            add(panel);
            JButton button=new JButton("Done");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            add(button);
            pack();
        }
        private static final long serialVersionUID=772356098237509l;
    }
}
