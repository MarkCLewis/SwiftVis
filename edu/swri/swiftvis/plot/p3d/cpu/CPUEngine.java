/*
 * Created on Jul 19, 2008
 */
package edu.swri.swiftvis.plot.p3d.cpu;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.plot.p3d.AmbientLight;
import edu.swri.swiftvis.plot.p3d.Camera3D;
import edu.swri.swiftvis.plot.p3d.DirectionalLight;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.PointLight;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Sphere3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.util.EditableBoolean;

public class CPUEngine implements RenderEngine {
    public CPUEngine(PlotArea3D area) {
        plotArea=area;
        view=new JGView();
    }
    
    @Override
    public String toString() {
        return "CPU Based Render Engine";
    }

    @Override
    public void add(Sphere3D s, DrawAttributes attrs) {
        double rad=s.getRadius();
        Vect3D c=s.getCenter();
        for(int i=0; i<sphereLevels[sphereResolution]; ++i) {
            double phi1=Math.PI*0.5*(1-(double)i/sphereLevels[sphereResolution]);
            double phi2=Math.PI*0.5*(1-(double)(i+1)/sphereLevels[sphereResolution]);
            double zTop1=c.getZ()+rad*Math.cos(phi1);
            double zBottom1=c.getZ()-rad*Math.cos(phi1);
            double zTop2=c.getZ()+rad*Math.cos(phi2);
            double zBottom2=c.getZ()-rad*Math.cos(phi2);
            double rad1=rad*Math.sin(phi1);
            double rad2=rad*Math.sin(phi2);
            for(int j=0; j<spherePolysPerLevel[sphereResolution]; ++j) {
                double theta1=2*Math.PI*((double)j/spherePolysPerLevel[sphereResolution]);
                double theta2=2*Math.PI*((double)(j+1)/spherePolysPerLevel[sphereResolution]);
                double x11=c.getX()+rad1*Math.cos(theta1);
                double x21=c.getX()+rad1*Math.cos(theta2);
                double y11=c.getY()+rad1*Math.sin(theta1);
                double y21=c.getY()+rad1*Math.sin(theta2);
                double x12=c.getX()+rad2*Math.cos(theta1);
                double x22=c.getX()+rad2*Math.cos(theta2);
                double y12=c.getY()+rad2*Math.sin(theta1);
                double y22=c.getY()+rad2*Math.sin(theta2);
                
                JGPolygon poly=new JGPolygon(3,true,attrs.getColor());
                poly.setPoint(0,new JGVector(x11,y11,zTop1,1.0));
                poly.setPoint(1,new JGVector(x22,y22,zTop2,1.0));
                poly.setPoint(2,new JGVector(x12,y12,zTop2,1.0));
                view.addChild(poly);
                poly=new JGPolygon(3,true,attrs.getColor());
                poly.setPoint(0,new JGVector(x11,y11,zTop1,1.0));
                poly.setPoint(1,new JGVector(x21,y21,zTop1,1.0));
                poly.setPoint(2,new JGVector(x22,y22,zTop2,1.0));
                view.addChild(poly);
                poly=new JGPolygon(3,true,attrs.getColor());
                poly.setPoint(0,new JGVector(x11,y11,zBottom1,1.0));
                poly.setPoint(1,new JGVector(x12,y12,zBottom2,1.0));
                poly.setPoint(2,new JGVector(x22,y22,zBottom2,1.0));
                view.addChild(poly);
                poly=new JGPolygon(3,true,attrs.getColor());
                poly.setPoint(0,new JGVector(x11,y11,zBottom1,1.0));
                poly.setPoint(1,new JGVector(x22,y22,zBottom2,1.0));
                poly.setPoint(2,new JGVector(x21,y21,zBottom1,1.0));
                view.addChild(poly);
            }
        }
    }

    @Override
    public void add(Triangle3D t, DrawAttributes[] attrs) {
        System.out.println("Adding triangle to view");
        JGPolygon poly=new JGPolygon(3,true,attrs[0].getColor());
        for(int i=0; i<3; ++i) {
            poly.setPoint(i,new JGVector(t.getPoint(i)));
        }
        view.addChild(poly);
    }

    @Override
    public void clearScene() {
        lighting.clearLights();
        view.clearChildren();
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
        sx=imgWidth;
        sy=imgHeight;
        if(img==null || sx!=img.getWidth() || sy!=img.getHeight()) {
            img=new BufferedImage(sx,sy,BufferedImage.TYPE_INT_ARGB);
        }
        setCamera();
        renderImage();
        return img;
    }
    
    @Override
    public Image cameraMoved() {
        setCamera();
        renderImage();
        return img;
    }

    @Override
    public void addPointLight(PointLight pl) {
        lighting.addPointLight(pl);
    }

    @Override
    public void addDirectionalLight(DirectionalLight dl) {
        lighting.addDirectLight(dl);
    }

    @Override
    public void setAmbientLight(AmbientLight al) {
        lighting.setAmbientLight(al);
    }
    
    private void renderImage() {
        lighting.fixValues(plotArea.getSink());
        Graphics2D g=img.createGraphics();
        g.setClip(new Rectangle(0,0,sx,sy));
        view.endFrame(g,lighting,splitPolygons.getValue());
    }
    
    private void setCamera() {
        Camera3D cam=plotArea.getCamera();
        DataSink sink=plotArea.getSink();
        Vect3D center=cam.getCenter(sink);
        Vect3D forward=cam.getForward(sink);
        Vect3D up=cam.getUp(sink);
        view.setRRRT(new JGVector(center),new JGVector(forward.plus(center)),new JGVector(up.plus(center)));
        view.setWindow(-1.0,-1.0,1.0,1.0);
        view.setProjection(1,center.getX()+2*forward.getX(),center.getY()+2*forward.getY(),center.getZ()+2*forward.getZ());
        view.setFrontAndBackClippingDistance(0.0,100.0);
    }

    private PlotArea3D plotArea;
    private int sx,sy;
    private JGView view;
    private BufferedImage img;
    private JGLighting lighting=new JGLighting();  // TODO remember that point lights should be in tree.
    // This doesn't matter so much for SwiftVis, but would be a general consideration.
    private EditableBoolean splitPolygons=new EditableBoolean(false);
    private int sphereResolution=1;
    
    private static final int[] sphereLevels={1,2,4};
    private static final int[] spherePolysPerLevel={4,8,12};
    //private static final String[] sphereLevelDesc={"","",""};    

    private transient JPanel propPanel;
}
