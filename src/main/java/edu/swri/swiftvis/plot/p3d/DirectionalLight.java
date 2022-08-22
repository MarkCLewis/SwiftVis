/*
 * Created on Jul 12, 2008
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

public class DirectionalLight implements Serializable {

    public DirectionalLight(Vect3D d,Color c) {
        xFormula=new DataFormula(String.valueOf(d.getX()));
        yFormula=new DataFormula(String.valueOf(d.getY()));
        zFormula=new DataFormula(String.valueOf(d.getZ()));
        color=c;
    }
    
    public DirectionalLight(DirectionalLight c) {
        xFormula=new DataFormula(c.xFormula.getFormula());
        yFormula=new DataFormula(c.yFormula.getFormula());
        zFormula=new DataFormula(c.zFormula.getFormula());
        color=c.color;
    }
    
    public void fixDirection(DataSink sink) {
        d=new Vect3D(xFormula.valueOf(sink,0, 0),yFormula.valueOf(sink,0, 0),zFormula.valueOf(sink,0, 0)).normalize();
    }

    public Vect3D getDirection() {
        return d;
    }
    
    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "Directional Light at ("+xFormula.getFormula()+","+yFormula.getFormula()+","+zFormula.getFormula()+") "+color;
    }

    public void edit(Component p) {
        while(p!=null && !(p instanceof Window)) p=p.getParent();
        if(p!=null) new EditDialog((Window)p);
    }

    private Vect3D d;
    private DataFormula xFormula;
    private DataFormula yFormula;
    private DataFormula zFormula;
    private Color color;

    private static final long serialVersionUID = 5797712571693130123L;

    private class EditDialog extends JDialog {
        public EditDialog(Window p) {
            super(p,"Directional Light",ModalityType.APPLICATION_MODAL);
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
