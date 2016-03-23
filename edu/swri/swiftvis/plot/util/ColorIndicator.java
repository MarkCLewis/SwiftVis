/* Generated by Together */
package edu.swri.swiftvis.plot.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/**
* This class represents a GUI component that can be used to show a selected
* color.  It can also show if it has been selected by having the user click on it.
* Objects of this kind can be grouped together like radio buttons so that only
* one can be selected at a given time.
*/
public class ColorIndicator extends JPanel implements MouseListener,ChangeListener {
    /**
     * This constructor provides a color chooser for the indicator.  This is
     * done so that so if the indicator is selected, it can tell the chooser to
     * start showing its color.
     */
    public ColorIndicator(JColorChooser ch) {
        colorChooser=ch;
        col=Color.black;
        addMouseListener(this);
    }

    public ColorIndicator(JColorChooser ch,ColorIndicator ci) {
        colorChooser=ch;
        col=Color.white;
        addToGroup(ci);
        addMouseListener(this);
    }

    /**
     * 
     * @uml.property name="prevInGroup"
     */
    public void addToGroup(ColorIndicator ci) {
        ColorIndicator myFirst = this, theirLast = ci;
        for (; myFirst.prevInGroup != null; myFirst = myFirst.prevInGroup);
        for (; theirLast.nextInGroup != null; theirLast = theirLast.nextInGroup);
        myFirst.prevInGroup = theirLast;
        theirLast.nextInGroup = myFirst;
    }


    public void setSelected() {
        selected=true;
        for(ColorIndicator rover=prevInGroup; rover!=null; rover=rover.prevInGroup) { rover.selected=false; rover.repaint(); }
        for(ColorIndicator rover=nextInGroup; rover!=null; rover=rover.nextInGroup) { rover.selected=false; rover.repaint(); }
        repaint();
    }

    /**
     * 
     * @uml.property name="col"
     */
    public void setColor(Color c) {
        col = c;
        repaint();
    }

    /**
     * 
     * @uml.property name="col"
     */
    public Color getColor() {
        return col;
    }


    @Override
    public void paintComponent(Graphics gr) {
        Graphics2D g=(Graphics2D)gr;
        g.setPaint(Color.lightGray);
        g.fillRect(0,0,getWidth(),getHeight());
        g.setPaint(col);
        g.fillRect(2,2,getWidth()-5,getHeight()-5);
        if(selected) {
            g.setPaint(Color.black);
            g.drawRect(1,1,getWidth()-3,getHeight()-3);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(50,50);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        setSelected();
        colorChooser.setColor(col);
    }

    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void stateChanged(ChangeEvent e) {
        if(selected) {
	        col=colorChooser.getColor();
    	    repaint();
        }
    }

    /**
     * 
     * @uml.property name="colorChooser"
     * @uml.associationEnd multiplicity="(1 1)"
     */
    private JColorChooser colorChooser;

    /**
     * 
     * @uml.property name="col" 
     */
    private Color col;

    private boolean selected;

    /**
     * 
     * @uml.property name="nextInGroup"
     * @uml.associationEnd multiplicity="(0 1)" inverse="prevInGroup:edu.swri.swiftvis.plot.util.ColorIndicator"
     */
    private ColorIndicator nextInGroup;

    /**
     * 
     * @uml.property name="prevInGroup"
     * @uml.associationEnd multiplicity="(0 1)" inverse="nextInGroup:edu.swri.swiftvis.plot.util.ColorIndicator"
     */
    private ColorIndicator prevInGroup;

    private static final long serialVersionUID=43893463473274l;
}
