/*
 * Created on Jun 11, 2005
 */
package edu.swri.swiftvis;

import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import edu.swri.swiftvis.plot.PlotTransform;

/**
 * This interface is to be implemented by all of the filters that are
 * supposed to listen for user interaction with plots.  This is used to
 * produce fast analysis tools so that users can click on plots and change
 * what data is being moved through the system.  The original filters that
 * will use this are a region selection (less powerful than a full
 * selection filter, but works as a listener) and a slice through.  Those
 * came to my mind because they would allow me to build the same types of
 * tools I have used in the past for rings.  Others can be added if people
 * find a need for them. 
 * 
 * @author Mark Lewis
 */
public interface PlotListener extends GraphElement {
    /**
     * This is called when a mouse button is pressed.
     * @param v1 The primary coordinate in the plot where it was pressed.
     * @param v2 The secondary coordinate in the plot where it was pressed.
     * @param e The event that triggered the call.
     */
    void mousePressed(double v1,double v2,MouseEvent e);

    /**
     * This is called when a mouse button is released.
     * @param v1 The primary coordinate in the plot where it was pressed.
     * @param v2 The secondary coordinate in the plot where it was pressed.
     * @param e The event that triggered the call.
     */
    void mouseReleased(double v1,double v2,MouseEvent e);
    
    /**
     * This is called when a mouse button is clicked.
     * @param v1 The primary coordinate in the plot where it was pressed.
     * @param v2 The secondary coordinate in the plot where it was pressed.
     * @param e The event that triggered the call.
     */
    void mouseClicked(double v1,double v2,MouseEvent e);
    
    /**
     * This is called when the mouse is moved.
     * @param v1 The primary coordinate in the plot where it was pressed.
     * @param v2 The secondary coordinate in the plot where it was pressed.
     * @param e The event that triggered the call.
     */
    void mouseMoved(double v1,double v2,MouseEvent e);
    
    /**
     * This is called when the mouse is dragged.
     * @param v1 The primary coordinate in the plot where it was pressed.
     * @param v2 The secondary coordinate in the plot where it was pressed.
     * @param e The event that triggered the call.
     */
    void mouseDragged(double v1,double v2,MouseEvent e);
    
    /**
     * This is called when a key is pressed.
     * @param e The event that triggered the call.
     */
    void keyPressed(KeyEvent e);
    
    /**
     * This is called when a key is released.
     * @param e The event that triggered the call.
     */
    void keyReleased(KeyEvent e);
    
    /**
     * This is called when a key is typed.
     * @param e The event that triggered the call.
     */
    void keyTyped(KeyEvent e);
    
    /**
     * This method can be used by plot listeners to tell a certain
     * plot style what region is being investigated.  This can provide
     * the user with input as to what is being seen in other plots.
     * @return The shape for the selected region.
     */
    Shape getSelectionRegion(PlotTransform trans);
}
