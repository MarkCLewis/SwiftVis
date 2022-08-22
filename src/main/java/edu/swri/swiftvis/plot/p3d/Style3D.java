/*
 * Created on Dec 27, 2006
 */
package edu.swri.swiftvis.plot.p3d;

import java.io.Serializable;

import javax.swing.JComponent;

import edu.swri.swiftvis.plot.PlotArea3D;


public interface Style3D extends Serializable {
	void renderToEngine(RenderEngine engine);
    JComponent getPropertiesPanel();
    Style3D copy(PlotArea3D c);
}
