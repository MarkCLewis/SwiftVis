/*
 * Created on Jul 25, 2004
 */
package edu.swri.swiftvis.plot;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

/**
 * This interface encapsulates the functionality that is need to adjust legends that are * put onto plots. *   * @author Mark Lewis
 */

public interface PlotLegend {

    /**
     * As the name implies, this method returns a panel that can be used to edit the
     * properties of a legend object.
     * 
     * @return A Swing component setup to allow modification of this object.
     * 
     * @uml.property name="propertiesPanel"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    JComponent getPropertiesPanel();

    
    /**
     * This method will attempt to draw the legend information to the provided graphics
     * object inside the provided bounds.
     * 
     * @param g The Graphics2D object that is being drawn to.
     * @param bounds A rectangle in which the drawing should be confined.  Things drawn outside may be clipped.
     */
    void drawToGraphics(Graphics2D g,Rectangle2D bounds);
    
    /**
     * This method tells you whether this legend element is to be drawn based on user
     * settings.
     * @return A boolean for whether it should be drawn.
     */
    boolean isDrawn();
    
    /**
     * This method returns a double for the relative amount of space that this
     * legend component should take.  This can be a constant determined by the author or
     * something settable in the GUI.
     * @return A proportional amount of space this legend element should take up.
     */
    double relativeVerticalSize();
}
