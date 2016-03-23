/* Generated by Together */

package edu.swri.swiftvis.plot;

import java.io.Serializable;
import javax.swing.*;
import java.awt.Graphics2D;

public interface DataPlotStyle extends Serializable,Bounded {

    void redoBounds();

    /**
     * 
     * @uml.property name="propertiesPanel"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    JComponent getPropertiesPanel();

    /**
     * This method should return the legend information for this plot style.  If there is
     * no legend information for this type of plot, then it should return null.
     * @return The legend information or null if none.
     * 
     * @uml.property name="legendInformation"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    PlotLegend getLegendInformation();

    /**
     * This draws the plot into the specified Graphics object.  It assumes that
     * the transform and clipping for that Graphics object have all been set up
     * so that the markers can be drawn at their normal locations.  The xSize
     * and ySize are passed in so that it can figure out how large to make the
     * markers if needed.
     */
    void drawToGraphics(Graphics2D g,PlotTransform trans);
    
    DataPlotStyle copy(PlotArea2D pa);
}
