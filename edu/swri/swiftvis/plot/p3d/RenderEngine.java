/*
 * Created on Dec 29, 2006
 */
package edu.swri.swiftvis.plot.p3d;

import java.awt.Image;

import javax.swing.JComponent;

/**
 * This interface represents an engine that can render a 3-D image.  I'm doing this
 * with a double dispatch so each engine will need to have methods that render each
 * of the different types of 3-D objects that can appear in any scene.
 * 
 * @author Mark Lewis
 */
public interface RenderEngine {
	void clearScene();
    void add(Sphere3D s,DrawAttributes attrs);
    void add(Triangle3D t,DrawAttributes[] attrs);
    void setAmbientLight(AmbientLight al);
    void addDirectionalLight(DirectionalLight dl);
    void addPointLight(PointLight pl);
    Image sceneComplete(int imgWidth,int imgHeight);
    Image cameraMoved();
    JComponent getPropertiesPanel();
}
