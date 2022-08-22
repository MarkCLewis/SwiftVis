/*
 * Created on Mar 12, 2007
 */
package edu.swri.swiftvis;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.util.GraphLabelString;

public class GraphNote implements GraphElement {
    public GraphNote() {}
    
    public GraphNote(GraphNote c) {
        label=new GraphLabelString(c.label);
    }

    @Override
    public void clearData() {
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new GraphNote(this);
    }

    @Override
    public void drawNode(Graphics2D g) {
        label.draw(g);
    }

    @Override
    public Rectangle getBounds() {
        return label.getBounds();
    }

    @Override
    public String getDescription() {
        return "Note";
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=label.getAreaPanel("Label");
        }
        return propPanel;
    }

    @Override
    public void redo() {
    }

    @Override
    public void relink(Hashtable<GraphElement, GraphElement> linkHash) {
    }

    @Override
    public void translate(int dx, int dy) {
        label.translate(dx,dy);
    }
    
    private GraphLabelString label=new GraphLabelString(getDescription(),Color.red);
    
    private transient JPanel propPanel;

    private static final long serialVersionUID = -6574879620778684895L;
}
