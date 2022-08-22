/*
 * Created on Sep 20, 2008
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.plot.p3d.raytrace.Octree;
import edu.swri.swiftvis.plot.p3d.raytrace.RTGeometry;
import edu.swri.swiftvis.plot.p3d.raytrace.RTSphere;
import edu.swri.swiftvis.plot.p3d.raytrace.Ray;
import edu.swri.swiftvis.plot.p3d.raytrace.RayFollower;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.LoopBody;
import edu.swri.swiftvis.util.ThreadHandler;

public class RayTraceBinningFilter extends AbstractMultipleSourceFilter {
    
    public RayTraceBinningFilter() {}
    
    private RayTraceBinningFilter(RayTraceBinningFilter c,List<GraphElement> l) {
        super(c,l);
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        zFormula=new DataFormula(c.zFormula);
        radiusFormula=new DataFormula(c.radiusFormula);
        viewCenterXFormula=new DataFormula(c.viewCenterXFormula);
        viewCenterYFormula=new DataFormula(c.viewCenterYFormula);
        viewCenterZFormula=new DataFormula(c.viewCenterZFormula);
        viewForwardXFormula=new DataFormula(c.viewForwardXFormula);
        viewForwardYFormula=new DataFormula(c.viewForwardYFormula);
        viewForwardZFormula=new DataFormula(c.viewForwardZFormula);
        viewUpXFormula=new DataFormula(c.viewUpXFormula);
        viewUpYFormula=new DataFormula(c.viewUpYFormula);
        viewUpZFormula=new DataFormula(c.viewUpZFormula);
        viewRightXFormula=new DataFormula(c.viewUpXFormula);
        viewRightYFormula=new DataFormula(c.viewUpYFormula);
        viewRightZFormula=new DataFormula(c.viewUpZFormula);
        lightXFormula=new DataFormula(c.lightXFormula);
        lightYFormula=new DataFormula(c.lightYFormula);
        lightZFormula=new DataFormula(c.lightZFormula);
        maxRecursions=new EditableInt(c.maxRecursions.getValue());
        numHorizontalBins=new EditableInt(c.numHorizontalBins.getValue());
        horizontalViewSize=new EditableDouble(c.horizontalViewSize.getValue());
        numVerticalBins=new EditableInt(c.numVerticalBins.getValue());
        verticalViewSize=new EditableDouble(c.verticalViewSize.getValue());
        raysPerBin=new EditableInt(c.raysPerBin.getValue());
    }

    @Override
    protected boolean doingInThreads() {
        return true;
    }

    @Override
    protected void redoAllElements() {
        sizeDataVectToInputStreams();
        for(int s=0; s<getSource(0).getNumStreams(); ++s) {
            final int ss=s;
            int[] range=xFormula.getSafeElementRange(this,s);
            DataFormula.mergeSafeElementRanges(range,yFormula.getSafeElementRange(this,s));
            DataFormula.mergeSafeElementRanges(range,zFormula.getSafeElementRange(this,s));
            DataFormula.mergeSafeElementRanges(range,radiusFormula.getSafeElementRange(this,s));
            DataFormula.checkRangeSafety(range,this);
            List<RTSphere> geomList=new ArrayList<RTSphere>();
            double minx=1e100,maxx=-1e100,miny=1e100,maxy=-1e100,minz=1e100,maxz=-1e100;
            for(int i=range[0]; i<range[1]; ++i) {
                RTSphere bs=new RTSphere(new Vect3D(xFormula.valueOf(this,s,i),yFormula.valueOf(this,s,i),zFormula.valueOf(this,s,i)),radiusFormula.valueOf(this,s,i));
                geomList.add(bs);            
                double r=bs.getRadius();
                if(bs.getX()-r<minx) minx=bs.getX()-r;
                if(bs.getX()+r>maxx) maxx=bs.getX()+r;
                if(bs.getY()-r<miny) miny=bs.getY()-r;
                if(bs.getY()+r>maxy) maxy=bs.getY()+r;
                if(bs.getZ()-r<minz) minz=bs.getZ()-r;
                if(bs.getZ()+r>maxz) maxz=bs.getZ()+r;
            }
            double size=Math.max(maxx-minx,Math.max(maxy-miny,maxz-minz));
            tree=new Octree(0.5*(maxx+minx),0.5*(maxy+miny),0.5*(maxz+minz),size);
            for(RTGeometry g:geomList) {
                tree.addObject(g);
            }
            tree.getRoot().finalize();
            
            final double[] forward=new double[]{viewForwardXFormula.valueOf(this,s,0),viewForwardYFormula.valueOf(this,s,0),viewForwardZFormula.valueOf(this,s,0)};
            final double[] up=new double[]{viewUpXFormula.valueOf(this,s,0),viewUpYFormula.valueOf(this,s,0),viewUpZFormula.valueOf(this,s,0)};
            final double[] screenCenter=new double[]{viewCenterXFormula.valueOf(this,s,0),viewCenterYFormula.valueOf(this,s,0),viewCenterZFormula.valueOf(this,s,0)};
            lightDir=new Vect3D(lightXFormula.valueOf(this,s,0),lightYFormula.valueOf(this,s,0),lightZFormula.valueOf(this,s,0));
            Basic.normalize(up);
            final double[] left=new double[]{-viewRightXFormula.valueOf(this,s,0),-viewRightYFormula.valueOf(this,s,0),-viewRightZFormula.valueOf(this,s,0)};
            Basic.normalize(left);
            final double horizontalResolution=horizontalViewSize.getValue()/numHorizontalBins.getValue();
            final double verticalResolution=verticalViewSize.getValue()/numVerticalBins.getValue();
            ThreadHandler.instance().dynamicForLoop(null, 0, numHorizontalBins.getValue(), new LoopBody() {
                @Override
                public void execute(int i) {
                    int[] params=new int[0];
                    float[] vals=new float[6];
                    vals[0]=i;
                    double[] eye=new double[3];
                    for(int j=0; j<numVerticalBins.getValue(); ++j) {
                        vals[1]=j;
                        double[] loc=new double[3];
                        vals[5]=0.0f;
                        float factor=1.0f/raysPerBin.getValue();
                        for(int k=0; k<raysPerBin.getValue(); ++k) {
                            double ri=0;
                            double rj=0;
                            if(k>0) {
                                ri=Math.random();
                                rj=Math.random();
                            }
                            double u=((i+ri)-0.5*numHorizontalBins.getValue());
                            double v=((j+rj)-0.5*numVerticalBins.getValue());
                            loc[0]=screenCenter[0]-horizontalResolution*left[0]*u-verticalResolution*up[0]*v;
                            loc[1]=screenCenter[1]-horizontalResolution*left[1]*u-verticalResolution*up[1]*v;
                            loc[2]=screenCenter[2]-horizontalResolution*left[2]*u-verticalResolution*up[2]*v;
                            if(k==0) {
                                vals[2]=(float)loc[0];
                                vals[3]=(float)loc[1];
                                vals[4]=(float)loc[2];                            
                            }
                            eye[0]=loc[0]-forward[0];
                            eye[1]=loc[1]-forward[1];
                            eye[2]=loc[2]-forward[2];
                            Ray r=new Ray(eye,loc);
                            vals[5]+=rayIntensity(r,0)*factor;
                        }
                        dataVect.get(ss).add(new DataElement(params,vals));
                    }
                }
            });
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel settingPanel=new JPanel(new BorderLayout());
        Box northPanel=new Box(BoxLayout.Y_AXIS);
        JPanel panel=new JPanel(new GridLayout(4,1));
        panel.setBorder(BorderFactory.createTitledBorder("Particle Settings"));
        panel.add(xFormula.getLabeledTextField("X formula",null));
        panel.add(yFormula.getLabeledTextField("Y formula",null));
        panel.add(zFormula.getLabeledTextField("Z formula",null));
        panel.add(radiusFormula.getLabeledTextField("Radius formula",null));
        northPanel.add(panel);
        panel=new JPanel(new GridLayout(3,1));
        panel.setBorder(BorderFactory.createTitledBorder("Light Settings"));
        panel.add(lightXFormula.getLabeledTextField("X formula",null));
        panel.add(lightYFormula.getLabeledTextField("Y formula",null));
        panel.add(lightZFormula.getLabeledTextField("Z formula",null));
        northPanel.add(panel);
        panel=new JPanel(new GridLayout(12,1));
        panel.setBorder(BorderFactory.createTitledBorder("View Settings"));
        panel.add(viewForwardXFormula.getLabeledTextField("Forward X formula",null));
        panel.add(viewForwardYFormula.getLabeledTextField("Forward Y formula",null));
        panel.add(viewForwardZFormula.getLabeledTextField("Forward Z formula",null));
        panel.add(viewCenterXFormula.getLabeledTextField("Center X formula",null));
        panel.add(viewCenterYFormula.getLabeledTextField("Center Y formula",null));
        panel.add(viewCenterZFormula.getLabeledTextField("Center Z formula",null));
        panel.add(viewUpXFormula.getLabeledTextField("Up X formula",null));
        panel.add(viewUpYFormula.getLabeledTextField("Up Y formula",null));
        panel.add(viewUpZFormula.getLabeledTextField("Up Z formula",null));
        panel.add(viewRightXFormula.getLabeledTextField("Right X formula",null));
        panel.add(viewRightYFormula.getLabeledTextField("Right Y formula",null));
        panel.add(viewRightZFormula.getLabeledTextField("Right Z formula",null));
        northPanel.add(panel);
        panel=new JPanel(new GridLayout(6,1));
        panel.setBorder(BorderFactory.createTitledBorder("Render Settings"));
        panel.add(raysPerBin.getLabeledTextField("Rays per bin",null));
        panel.add(numHorizontalBins.getLabeledTextField("Num horizontal bins",null));
        panel.add(horizontalViewSize.getLabeledTextField("Horizontal view size",null));
        panel.add(numVerticalBins.getLabeledTextField("Num vertical bins",null));
        panel.add(verticalViewSize.getLabeledTextField("Vertical view size",null));
        panel.add(maxRecursions.getLabeledTextField("Max recursions",null));
        northPanel.add(panel);
        settingPanel.add(northPanel,BorderLayout.NORTH);
        JButton button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        settingPanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Settings",settingPanel);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return paramDesc[which];
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return valueDesc[which];
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new RayTraceBinningFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Ray Trace Binning Filter";
    }
    
    public static String getTypeDescription() {
        return "Ray Trace Binning Filter";
    }

    private float rayIntensity(Ray r,int cnt) {
        if(cnt>=maxRecursions.getValue()) return 0.0f;
        RayFollower rf=new RayFollower(r,tree);
        //System.out.println("Ray "+r);
        if(rf.follow()) {
            RTGeometry geom=rf.getIntersectObject();
            //System.out.println("Hit "+geom);
            double[] intersect=r.point(rf.getIntersectTime()*0.999999);
            //System.out.println("Hit val "+rf.getIntersectTime()+" point "+intersect[0]+" "+intersect[1]+" "+intersect[2]);
            double[] norm=geom.getNormal(intersect);
            Ray normRay=new Ray(new double[]{0,0,0},norm);
            Basic.normalize(norm);
            float ret=0;
            Ray toLight=new Ray(intersect,new Vect3D(intersect).minus(lightDir.mult(10000)).get());
            RayFollower followLight=new RayFollower(toLight,tree);
            if(!followLight.follow()) {
                float lightMag=(float)(toLight.dotProduct(normRay));
                if(lightMag>0) {
                    ret+=lightMag;
                }
            }
            return ret;
        }
        return 0.0f;
    }
    
    private DataFormula xFormula=new DataFormula("v[0]");
    private DataFormula yFormula=new DataFormula("v[1]");
    private DataFormula zFormula=new DataFormula("v[2]");
    private DataFormula radiusFormula=new DataFormula("v[6]");
    
    private DataFormula viewCenterXFormula=new DataFormula("0");
    private DataFormula viewCenterYFormula=new DataFormula("0");
    private DataFormula viewCenterZFormula=new DataFormula("0");

    private DataFormula viewForwardXFormula=new DataFormula("0");
    private DataFormula viewForwardYFormula=new DataFormula("0");
    private DataFormula viewForwardZFormula=new DataFormula("-1");

    private DataFormula viewUpXFormula=new DataFormula("1");
    private DataFormula viewUpYFormula=new DataFormula("0");
    private DataFormula viewUpZFormula=new DataFormula("0");

    private DataFormula viewRightXFormula=new DataFormula("0");
    private DataFormula viewRightYFormula=new DataFormula("1");
    private DataFormula viewRightZFormula=new DataFormula("0");

    private DataFormula lightXFormula=new DataFormula("-1");
    private DataFormula lightYFormula=new DataFormula("0");
    private DataFormula lightZFormula=new DataFormula("0");
    private EditableInt maxRecursions=new EditableInt(1);

    private EditableInt numHorizontalBins=new EditableInt(100);
    private EditableDouble horizontalViewSize=new EditableDouble(1);
    private EditableInt numVerticalBins=new EditableInt(100);
    private EditableDouble verticalViewSize=new EditableDouble(1);
    private EditableInt raysPerBin=new EditableInt(20);
    
    private transient Octree tree;
    private transient Vect3D lightDir;

    private static String[] paramDesc={};
    private static String[] valueDesc={"i","j","x","y","z","Intensity"};

    private static final long serialVersionUID = 3086661642568736381L;
}
