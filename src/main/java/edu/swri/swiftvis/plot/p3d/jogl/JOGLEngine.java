package edu.swri.swiftvis.plot.p3d.jogl;

import java.awt.Image;

import javax.swing.JComponent;

import edu.swri.swiftvis.plot.p3d.AmbientLight;
import edu.swri.swiftvis.plot.p3d.DirectionalLight;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.PointLight;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Sphere3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;

public class JOGLEngine implements RenderEngine {

	@Override
    public void add(Sphere3D s,DrawAttributes attrs) {
		// TODO Auto-generated method stub

	}

	@Override
    public void add(Triangle3D t,DrawAttributes[] attrs) {
		// TODO Auto-generated method stub

	}

	@Override
    public void clearScene() {
		// TODO Auto-generated method stub

	}

	@Override
    public Image sceneComplete(int imgWidth,int imgHeight) {
		// TODO Auto-generated method stub
	    return null;
	}

    @Override
    public Image cameraMoved() {
        // TODO
        return null;
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
    public void setAmbientLight(AmbientLight al) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public JComponent getPropertiesPanel() {
        // TODO Auto-generated method stub
        return null;
    }

}
