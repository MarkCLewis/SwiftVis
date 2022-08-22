package edu.swri.swiftvis.plot.p3d.styles;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Sphere3D;
import edu.swri.swiftvis.plot.p3d.Style3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;


public class TestStyle implements Style3D {

    public TestStyle(PlotArea3D p3d) {
        
    }
    
    @Override
    public String toString() {
        return "Test Style";
    }

	@Override
    public void renderToEngine(RenderEngine engine) {
        DrawAttributes attrs=new DrawAttributes(Color.white,0,1);
        DrawAttributes attrs2=new DrawAttributes(Color.orange,1,1);
		engine.add(new Triangle3D(0,0,0,5,0,0,0,5,0),new DrawAttributes[]{attrs2,attrs,attrs});
        engine.add(new Triangle3D(5,0,0,5,5,0,0,5,0),new DrawAttributes[]{attrs,attrs2,attrs});
        attrs=new DrawAttributes(Color.red,0,0.4);
		engine.add(new Sphere3D(0.5,1,1.5,0.3),attrs);
        attrs=new DrawAttributes(Color.green,0.0,1);
        engine.add(new Sphere3D(0,3,2,0.5),attrs);
        attrs=new DrawAttributes(Color.blue,0.7,1);
        engine.add(new Sphere3D(2,3,2,1.5),attrs);
	}

    @Override
    public JComponent getPropertiesPanel() {
        return new JPanel();
    }
    
    @Override
    public Style3D copy(PlotArea3D p) {
        return new TestStyle(p);
    }
    
    public static String getTypeDescription() {
        return "Test Style";
    }

    private static final long serialVersionUID = -3966567870129560964L;
}
