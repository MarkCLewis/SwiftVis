/*
 * Created on Aug 6, 2008
 */
package edu.swri.swiftvis.plot.p3d.j2d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.plot.p3d.AmbientLight;
import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.Camera3D;
import edu.swri.swiftvis.plot.p3d.DirectionalLight;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.PointLight;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Sphere3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.util.EditableDouble;

/**
 * This is an alternate render engine that works on the CPU with Java2D.  It does not
 * incorporate a scene graph and is more based on the idea of having a camera position
 * and doing a projection.  The goal is that this can be more efficient than the
 * JG engine.
 * 
 * @author Mark Lewis
 */
public class J2DEngine implements RenderEngine {
    public J2DEngine(PlotArea3D area) {
        plotArea=area;
    }
    
    @Override
    public String toString() {
        return "CPU Based Render Engine";
    }

    @Override
    public void add(Sphere3D s, DrawAttributes attrs) {
        DrawAttributes[] das={attrs};
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
                
                add(new Triangle3D(x11,y11,zTop1,x21,y21,zTop1,x22,y22,zTop2),das);
                add(new Triangle3D(x11,y11,zBottom1,x22,y22,zBottom2,x21,y21,zBottom1),das);
                if(rad2>0) {
                    add(new Triangle3D(x11,y11,zTop1,x22,y22,zTop2,x12,y12,zTop2),das);
                    add(new Triangle3D(x11,y11,zBottom1,x12,y12,zBottom2,x22,y22,zBottom2),das);
                }
            }
        }
    }

    @Override
    public void add(Triangle3D t, DrawAttributes[] attrs) {
        triangles.add(new TriangleData(t,attrs));
    }

    @Override
    public void clearScene() {
        pointLights.clear();
        directLights.clear();
        triangles.clear();
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(100,1));
            final JComboBox sphereResBox=new JComboBox(sphereLevelDesc);
            sphereResBox.setSelectedIndex(sphereResolution);
            sphereResBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sphereResolution=sphereResBox.getSelectedIndex();
                }
            });
            northPanel.add(sphereResBox);
            northPanel.add(viewSize.getLabeledTextField("View Size",null));
            northPanel.add(scale.getLabeledTextField("Lighting Length Scale",null));
            propPanel.add(northPanel,BorderLayout.NORTH);
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
        pointLights.add(pl);
    }

    @Override
    public void addDirectionalLight(DirectionalLight dl) {
        directLights.add(dl);
    }

    @Override
    public void setAmbientLight(AmbientLight al) {
        ambientLight=al;
    }
    
    private void renderImage() {
        DataSink sink=plotArea.getSink();
        for(PointLight pl:pointLights) pl.fixPosition(sink);
        for(DirectionalLight dl:directLights) dl.fixDirection(sink);
        Graphics2D g=img.createGraphics();
        g.setBackground(Color.black);
        g.clearRect(0,0,sx,sy);
        g.setClip(new Rectangle(0,0,sx,sy));
        int minSize=Math.min(sx,sy);
        double widthRatio=(double)sx/minSize;
        double heightRatio=(double)sy/minSize;
        for(TriangleData td:triangles) {
            setDrawPoints(td,widthRatio,heightRatio);
            if(td.drawPnts!=null) {
                float opacity=(float)td.attrs[0].getOpacity();
                td.drawColor=getLightColor(td.tri.getPoint(0),td.tri.getNormals()[0],td.attrs[0].getColor(),opacity);
            }
        }
        Collections.sort(triangles,new Comparator<TriangleData>() {
            @Override
            public int compare(TriangleData td1,TriangleData td2) {
                if(td1.drawPnts==null) {
                    if(td2.drawPnts==null) return 0;
                    else return 1;
                } else if(td2.drawPnts==null) return -1;
                double max1=Math.max(td1.drawPnts[0].getY(),Math.max(td1.drawPnts[1].getY(),td1.drawPnts[2].getY()));
                double max2=Math.max(td2.drawPnts[0].getY(),Math.max(td2.drawPnts[1].getY(),td2.drawPnts[2].getY()));
                return -Double.compare(max1,max2);
            }
        });
        int cnt=0;
        for(TriangleData td:triangles) {
            if(td.drawPnts!=null) {
                int[] px=new int[3];
                int[] py=new int[3];
                for(int i=0; i<3; ++i) {
                    px[i]=(int)(td.drawPnts[i].getX()/td.drawPnts[i].getY()*minSize*0.5+sx*0.5);
                    py[i]=(int)(-td.drawPnts[i].getZ()/td.drawPnts[i].getY()*minSize*0.5+sy*0.5);
                }
                Polygon p=new Polygon(px,py,3);
                g.setPaint(td.drawColor);
                g.fill(p);
                cnt++;
            }
        }
    }
    
    private void setCamera() {
        Camera3D cam=plotArea.getCamera();
        DataSink sink=plotArea.getSink();
        center=cam.getCenter(sink);
        forward=cam.getForward(sink).normalize();
        up=cam.getUp(sink).normalize();
        right=Basic.crossProduct(forward,up).normalize();
    }
    
    private Color getLightColor(Vect3D pos,Vect3D norm,Color col,float alpha) {
        float red=0,green=0,blue=0;
        float[] comp=col.getColorComponents(null);
        float[] acomp=ambientLight.getColor().getColorComponents(null);
        red+=comp[0]*acomp[0];
        green+=comp[1]*acomp[1];
        blue+=comp[2]*acomp[2];
        for(PointLight pl:pointLights) {
            Vect3D lpos=pl.getPosition();
            double[] dir={lpos.getX()-pos.get(0),lpos.getY()-pos.get(1),lpos.getZ()-pos.get(2)};
            double len=Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
            dir[0]/=len;
            dir[1]/=len;
            dir[2]/=len;
            double mag=(norm.get(0)*dir[0]+norm.get(1)*dir[1]+norm.get(2)*dir[2])/(len/scale.getValue()+1);
            if(mag>0) {
                float[] dcomp=pl.getColor().getColorComponents(null);
                red+=comp[0]*dcomp[0]*mag;
                green+=comp[1]*dcomp[1]*mag;
                blue+=comp[2]*dcomp[2]*mag;
            }
        }
        for(DirectionalLight dl:directLights) {
            Vect3D dir=dl.getDirection();
            double mag=-(norm.get(0)*dir.getX()+norm.get(1)*dir.getY()+norm.get(2)*dir.getZ());
            if(mag>0) {
                float[] dcomp=dl.getColor().getColorComponents(null);
                red+=comp[0]*dcomp[0]*mag;
                green+=comp[1]*dcomp[1]*mag;
                blue+=comp[2]*dcomp[2]*mag;
            }
        }
        if(red>1) red=1;
        if(green>1) green=1;
        if(blue>1) blue=1;
        return new Color(red,green,blue,alpha);
    }
    
    private void setDrawPoints(TriangleData td,double widthRatio,double heightRatio) {
        Vect3D[] toPnts=new Vect3D[3];
        Vect3D norm=td.tri.getNormals()[0];
        
        toPnts[0]=td.tri.getPoint(0).minus(center);
        // Back face culling
        if(Basic.dotProduct(toPnts[0],norm)>0) {
            td.drawPnts=null;
            return;
        }
        toPnts[1]=td.tri.getPoint(1).minus(center);
        toPnts[2]=td.tri.getPoint(2).minus(center);
        int frontBit=1,leftBit=2,rightBit=4,upBit=8,downBit=16;
        int covers=0;
        td.drawPnts=new Vect3D[3];
        for(int i=0; i<3; ++i) {
            double dx=Basic.dotProduct(right,toPnts[i]);
            double dy=Basic.dotProduct(forward,toPnts[i])*viewSize.getValue()*0.5;
            double dz=Basic.dotProduct(up,toPnts[i]);
            td.drawPnts[i]=new Vect3D(dx,dy,dz);
            if(dy>0) {
                covers|=frontBit; 
                if(dx<dy*widthRatio) covers|=leftBit;
                if(dx>-dy*widthRatio) covers|=rightBit;
                if(dz<dy*heightRatio) covers|=downBit;
                if(dz>-dy*heightRatio) covers|=upBit;
            } else {
                td.drawPnts=null;
                return;                
            }
        }
        if((covers & (leftBit | rightBit))!=(leftBit | rightBit) || (covers & (upBit | downBit))!=(upBit | downBit)) {
            td.drawPnts=null;
            return;            
        }
    }

    private PlotArea3D plotArea;
    private int sx,sy;
    private BufferedImage img;

    private int sphereResolution=2;
    private List<TriangleData> triangles=new ArrayList<TriangleData>();
    private List<PointLight> pointLights=new ArrayList<PointLight>();
    private List<DirectionalLight> directLights=new ArrayList<DirectionalLight>();
    private AmbientLight ambientLight=new AmbientLight(new Color(20,20,20));
    private EditableDouble scale=new EditableDouble(100);
    private Vect3D center;
    private Vect3D forward;
    private Vect3D up;
    private Vect3D right;
    private EditableDouble viewSize=new EditableDouble(1.0);
    
    private static final int[] sphereLevels={1,2,4};
    private static final int[] spherePolysPerLevel={4,8,12};
    private static final String[] sphereLevelDesc={"Low Poly Sphere","Mid Poly Sphere","High Poly Sphere"};    

    private transient JPanel propPanel;
    
    private static class TriangleData {
        public TriangleData(Triangle3D t,DrawAttributes[] da) {
            tri=t;
            attrs=da;
        }
        private Triangle3D tri;
        private DrawAttributes[] attrs;
        private Vect3D[] drawPnts;
        private Color drawColor;
    }
}
