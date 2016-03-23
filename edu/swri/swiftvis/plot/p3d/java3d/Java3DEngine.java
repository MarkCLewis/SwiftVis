/*
 * Created on Jul 19, 2008
 */
package edu.swri.swiftvis.plot.p3d.java3d;

import java.awt.BorderLayout;
import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.plot.p3d.AmbientLight;
import edu.swri.swiftvis.plot.p3d.DirectionalLight;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.PointLight;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Sphere3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;

public class Java3DEngine implements RenderEngine {
    public Java3DEngine(PlotArea3D area) {
        // TODO
    }
    
    @Override
    public String toString() {
        return "Java 3D Engine";
    }

    @Override
    public void add(Sphere3D s, DrawAttributes attrs) {
        // TODO Auto-generated method stub

    }

    @Override
    public void add(Triangle3D t, DrawAttributes[] attrs) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addDirectionalLight(DirectionalLight dl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPointLight(PointLight pl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearScene() {
        // TODO Auto-generated method stub

    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
        }
        return propPanel;
    }

    @Override
    public Image sceneComplete(int imgWidth, int imgHeight) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Image cameraMoved() {
        // TODO
        return null;
    }

    @Override
    public void setAmbientLight(AmbientLight al) {
        // TODO Auto-generated method stub

    }

    private transient JPanel propPanel;
}
