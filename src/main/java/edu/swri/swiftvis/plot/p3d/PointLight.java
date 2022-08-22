/*
 * Created on Oct 26, 2004
 */
package edu.swri.swiftvis.plot.p3d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;


/**
 * This class represents a light in the rendering.
 * @author Mark Lewis
 */
public class PointLight implements Serializable {

    public PointLight(Vect3D pos,Color col) {
        xFormula=new DataFormula(String.valueOf(pos.getX()));
        yFormula=new DataFormula(String.valueOf(pos.getY()));
        zFormula=new DataFormula(String.valueOf(pos.getZ()));
        color=col;
    }
    
    public PointLight(PointLight c) {
        xFormula=new DataFormula(c.xFormula.getFormula());
        yFormula=new DataFormula(c.yFormula.getFormula());
        zFormula=new DataFormula(c.zFormula.getFormula());
        color=c.color;
    }
    
    public void fixPosition(DataSink sink) {
        pos=new Vect3D(xFormula.valueOf(sink,0, 0),yFormula.valueOf(sink,0, 0),zFormula.valueOf(sink,0, 0));
    }

    public Vect3D getPosition() {
        return pos;
    }

    public Color getColor() {
        return color;
    }
    
    @Override
    public String toString() {
        return "Point Light at ("+xFormula.getFormula()+","+yFormula.getFormula()+","+zFormula.getFormula()+") "+color;
    }
    
    public void edit(Component p) {
        while(p!=null && !(p instanceof Window)) p=p.getParent();
        if(p!=null) new EditDialog((Window)p);
    }

    private Vect3D pos;
    private DataFormula xFormula;
    private DataFormula yFormula;
    private DataFormula zFormula;
    private Color color;

    private static final long serialVersionUID = 5063574737852606224L;

    private class EditDialog extends JDialog {
        public EditDialog(Window p) {
            super(p,"Point Light",ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(3,1));
            northPanel.add(xFormula.getLabeledTextField("X",null));
            northPanel.add(yFormula.getLabeledTextField("Y",null));
            northPanel.add(zFormula.getLabeledTextField("Z",null));
            add(northPanel,BorderLayout.NORTH);
            chooser=new JColorChooser(color);
            add(chooser,BorderLayout.CENTER);
            JButton button=new JButton("Done");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    color=chooser.getColor();
                    setVisible(false);
                }
            });
            add(button,BorderLayout.SOUTH);
            pack();
            setVisible(true);
        }
        
        private JColorChooser chooser;
        
        private static final long serialVersionUID = 8328110817139232789L;        
    }
}
