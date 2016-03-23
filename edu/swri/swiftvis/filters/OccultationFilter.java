/*
 * Created on Jul 24, 2011
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.LoopBody;
import edu.swri.swiftvis.util.ThreadHandler;

public class OccultationFilter extends AbstractMultipleSourceFilter {
    public OccultationFilter() {
    }
    
    private OccultationFilter(OccultationFilter c,List<GraphElement> l) {
        super(c,l);
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        zFormula=new DataFormula(c.zFormula);
        radiusFormula=new DataFormula(c.radiusFormula);
        maxWraps=new EditableInt(c.maxWraps.getValue());
        photonCount=new EditableInt(c.photonCount.getValue());
        thetaBins=new EditableInt(c.thetaBins.getValue());
        phiBins=new EditableInt(c.phiBins.getValue());
        thetaMin=new DataFormula(c.thetaMin);
        thetaMax=new DataFormula(c.thetaMax);
        phiMin=new DataFormula(c.phiMin);
        phiMax=new DataFormula(c.phiMax);
        bounds=new ArrayList<Boundary>();
        for(Boundary b:c.bounds) {
            bounds.add(b.copy());
        }
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
            int[] indexBounds=xFormula.getSafeElementRange(this,s);
            DataFormula.mergeSafeElementRanges(indexBounds, yFormula.getSafeElementRange(this, s));
            DataFormula.mergeSafeElementRanges(indexBounds, zFormula.getSafeElementRange(this, s));
            DataFormula.mergeSafeElementRanges(indexBounds, radiusFormula.getSafeElementRange(this, s));
            
            List<RTSphere> geomList=new ArrayList<RTSphere>();
            final Cube cube=new Cube();
            for(int i=indexBounds[0]; i<indexBounds[1]; ++i) {
                RTSphere2 bs=new RTSphere2(new Vect3D(xFormula.valueOf(this,s,i),yFormula.valueOf(this,s,i),zFormula.valueOf(this,s,i)),radiusFormula.valueOf(this,s,i),i);
                geomList.add(bs);            
                double r=bs.getRadius();
                if(bs.getX()-r<cube.minx) cube.minx=bs.getX()-r;
                if(bs.getX()+r>cube.maxx) cube.maxx=bs.getX()+r;
                if(bs.getY()-r<cube.miny) cube.miny=bs.getY()-r;
                if(bs.getY()+r>cube.maxy) cube.maxy=bs.getY()+r;
                if(bs.getZ()-r<cube.minz) cube.minz=bs.getZ()-r;
                if(bs.getZ()+r>cube.maxz) cube.maxz=bs.getZ()+r;
            }
            for(Boundary b:bounds) {
                b.reset(cube,s);
            }
            double size=Math.max(cube.maxx-cube.minx,Math.max(cube.maxy-cube.miny,cube.maxz-cube.minz));
            final Octree tree=new Octree(0.5*(cube.maxx+cube.minx),0.5*(cube.maxy+cube.miny),0.5*(cube.maxz+cube.minz),size);
            for(RTGeometry g:geomList) {
                tree.addObject(g);
            }
            tree.getRoot().finalize();

            final AtomicIntegerArray[] bins=new AtomicIntegerArray[thetaBins.getValue()];
            for(int i=0; i<bins.length; ++i) {
                bins[i]=new AtomicIntegerArray(phiBins.getValue());
            }
            
            for(int thetaI=0; thetaI<thetaBins.getValue(); ++thetaI) {
                final int thetaIndex=thetaI;
                final double theta=thetaMin.valueOf(this,s,0)+thetaI*(thetaMax.valueOf(this,s,0)-thetaMin.valueOf(this,s,0))/thetaBins.getValue();
                if(thetaLabel!=null) thetaLabel.setText("Theta Bin: "+thetaI);
                for(int phiI=0; phiI<phiBins.getValue(); ++phiI) {
                    final int phiIndex=phiI;
                    final double phi=phiMin.valueOf(this,s,0)+phiI*(phiMax.valueOf(this,s,0)-phiMin.valueOf(this,s,0))/phiBins.getValue();
                    ThreadHandler.instance().chunkedForLoop(this,0,photonCount.getValue(),new LoopBody() {
                        @Override
                        public void execute(int i) {
                            Ray photon=getPhoton(theta,phi,ss,cube);
                            int partHit=followRay(tree,photon,0,cube);
                            if(partHit<0) bins[thetaIndex].addAndGet(phiIndex,1);
                        }
                    });                    
                    if(phiLabel!=null) phiLabel.setText("Phi Bin: "+phiI);
                }
            }
            float[] values=new float[3];
            for(int thetaI=0; thetaI<thetaBins.getValue(); ++thetaI) {
                values[0]=(float)(thetaMin.valueOf(this,s,0)+thetaI*(thetaMax.valueOf(this,s,0)-thetaMin.valueOf(this,s,0))/thetaBins.getValue());
                for(int phiI=0; phiI<phiBins.getValue(); ++phiI) {
                    values[1]=(float)(phiMin.valueOf(this,s,0)+phiI*(phiMax.valueOf(this,s,0)-phiMin.valueOf(this,s,0))/phiBins.getValue());
                    values[2]=(float)bins[thetaI].get(phiI)/photonCount.getValue();
                    dataVect.get(s).add(new DataElement(DataElement.zeroParams,values));
                }
            }
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel basicPanel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(13,1));
        northPanel.add(xFormula.getLabeledTextField("X Formula",null));
        northPanel.add(yFormula.getLabeledTextField("Y Formula",null));
        northPanel.add(zFormula.getLabeledTextField("Z Formula",null));
        northPanel.add(radiusFormula.getLabeledTextField("Radius Formula",null));
        northPanel.add(photonCount.getLabeledTextField("Photon Count",null));
        northPanel.add(thetaMin.getLabeledTextField("Theta Min",null));
        northPanel.add(thetaMax.getLabeledTextField("Theta Max",null));
        northPanel.add(thetaBins.getLabeledTextField("Theta Bins",null));
        northPanel.add(phiMin.getLabeledTextField("Phi Min",null));
        northPanel.add(phiMax.getLabeledTextField("Phi Max",null));
        northPanel.add(phiBins.getLabeledTextField("Phi Bins",null));
        northPanel.add(maxWraps.getLabeledTextField("Max Wraps",null));
        JPanel labels=new JPanel(new GridLayout(1,2));
        thetaLabel=new JLabel("Theta Bin:");
        labels.add(thetaLabel);
        phiLabel=new JLabel("Phi Bin:");
        labels.add(phiLabel);
        northPanel.add(labels);
        basicPanel.add(northPanel,BorderLayout.NORTH);
        JButton button=new JButton("Progagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        basicPanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Basic",basicPanel);
                
        JPanel boundsPanel=new JPanel(new BorderLayout());
        final JPanel boundsPropPanel=new JPanel(new GridLayout(1,1)); 
        northPanel=new JPanel(new BorderLayout());
        final JList boundsList=new JList(bounds.toArray());
        boundsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boundsPropPanel.removeAll();
                Boundary b=(Boundary)boundsList.getSelectedValue();
                if(b==null) return;
                JPanel panel=b.getPropertiesPanel();
                if(panel!=null) boundsPropPanel.add(panel);
                boundsPropPanel.validate();
                boundsPropPanel.repaint();
            }
        });
        northPanel.add(boundsList,BorderLayout.CENTER);
        JPanel buttonPanel=new JPanel(new GridLayout(1,2));
        button=new JButton("Add");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] boundsTypes={"Periodic Cube","Sliding Brick","Plane","Periodic Sphere"};
                int opt=JOptionPane.showOptionDialog(propPanel,"Select a boundary type.","Boundary Selection",JOptionPane.OK_OPTION,JOptionPane.INFORMATION_MESSAGE,null,boundsTypes,boundsTypes[0]);
                if(opt==0) {
                    bounds.add(new PeriodicCube());
                } else if(opt==1) {
                    bounds.add(new SlidingBrick());
                } else if(opt==2) {
                    bounds.add(new GeneralPlane());
                } else if(opt==3) {
                    bounds.add(new PeriodicSphere());
                } else {
                    return;
                }
                boundsList.setListData(bounds.toArray());                
            }
        });
        buttonPanel.add(button);
        button=new JButton("Remove");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=boundsList.getSelectedIndex();
                if(index<0) return;
                bounds.remove(index);
                boundsList.setListData(bounds.toArray());
                boundsPropPanel.removeAll();
                boundsPropPanel.validate();
                boundsPropPanel.repaint();
            }
        });
        buttonPanel.add(button);
        northPanel.add(buttonPanel,BorderLayout.SOUTH);
        northPanel.setPreferredSize(new Dimension(200,200));
        boundsPanel.add(northPanel,BorderLayout.NORTH);
        boundsPanel.add(new JScrollPane(boundsPropPanel));
        button=new JButton("Progagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        boundsPanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Bounds",boundsPanel);
        
    }
    
    @Override
    public int getNumParameters(int stream) {
        return 0;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return "";
    }

    @Override
    public int getNumValues(int stream) {
        return 3;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        String[] names={"Primary","Secondary","Intensity"};
        return names[which];
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new OccultationFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Occultation Filter";
    }
    
    public static String getTypeDescription() {
        return "Occultation Filter";
    }

    @Override
    protected void sizeDataVectToInputStreams() {
        if(dataVect.size()>inputVector.get(0).getNumStreams()) dataVect.clear();
        while(dataVect.size()<inputVector.get(0).getNumStreams()) dataVect.add(new ArrayList<DataElement>());
    }
    
    private Ray getPhoton(double theta,double phi,int stream,Cube b) {
        double[] dir={Math.cos(theta)*Math.cos(phi),Math.sin(theta)*Math.cos(phi),-Math.sin(phi)};
        Basic.scale(dir,b.maxEdge());
        double[] one=new double[3];
        if(dir[2]<0) {
            one[0]=b.minx+Math.random()*(b.maxx-b.minx);
            one[1]=b.miny+Math.random()*(b.maxy-b.miny);
            one[2]=b.maxz;
        } else if(dir[2]>0) {
            one[0]=b.minx+Math.random()*(b.maxx-b.minx);
            one[1]=b.miny+Math.random()*(b.maxy-b.miny);
            one[2]=b.minz;
        } else {
            one[0]=b.minx+Math.random()*(b.maxx-b.minx);
            one[1]=b.miny+Math.random()*(b.maxy-b.miny);
            one[2]=b.minz+Math.random()*(b.maxz-b.minz);
        }
        double[] zero={one[0]-dir[0],one[1]-dir[1],one[2]-dir[2]};
        return new Ray(zero,one);
    }

    private int followRay(Octree tree,Ray photon,int numWraps,Cube c) {
        if(numWraps>maxWraps.getValue()) {
            return -1;
        }
        double px=photon.startX();
        double py=photon.startY();
        double pz=photon.startZ();
        if(numWraps>1 && (px<c.minx && photon.oneX()<c.minx || px>c.maxx && photon.oneX()>c.maxx || 
                py<c.miny && photon.oneY()<c.miny || py>c.maxy && photon.oneY()>c.maxy || 
                pz<c.minz && photon.oneZ()<c.minz || pz>c.maxz && photon.oneZ()>c.maxz)) {
            return -1;
        }
        Boundary firstBound=null;
        double mint=-1;
        for(Boundary b:bounds) {
            double t=b.intersectVal(photon);
            if(t>0 && (firstBound==null || t<mint)) {
                mint=t;
                firstBound=b;
            }
        }
        RayFollower rf=new RayFollower(photon,tree);
        if(rf.follow() && (firstBound==null || mint>=rf.getIntersectTime())) {
            RTSphere2 geom=(RTSphere2)rf.getIntersectObject();
            double[] intersect=photon.point(rf.getIntersectTime()*0.999999);
            double[] norm=geom.getNormal(intersect);
            Basic.normalize(norm);
            return geom.index;
        } else if(firstBound!=null) {
            double[] intersect=photon.point(mint);
            return followRay(tree,firstBound.newRay(photon,intersect),numWraps+1,c);
        } else {
            return -1;
        }
    }

    // formulas for particle properties and tree
    private DataFormula xFormula=new DataFormula("v[0]");
    private DataFormula yFormula=new DataFormula("v[1]");
    private DataFormula zFormula=new DataFormula("v[2]");
    private DataFormula radiusFormula=new DataFormula("v[6]");
    private EditableInt photonCount=new EditableInt(1000);
    private EditableInt maxWraps=new EditableInt(10);
    private DataFormula thetaMin=new DataFormula("-PI");
    private DataFormula thetaMax=new DataFormula("PI");
    private EditableInt thetaBins=new EditableInt(10);
    private DataFormula phiMin=new DataFormula("-PI/2");
    private DataFormula phiMax=new DataFormula("PI/2");
    private EditableInt phiBins=new EditableInt(10);

    // periodic boundaries
    private List<Boundary> bounds=new ArrayList<Boundary> ();

    private transient JLabel thetaLabel;
    private transient JLabel phiLabel;
    private static final long serialVersionUID = -4077984307888423004L;
    
    private static class Cube {
        public double maxEdge() {
            return Math.max(maxx-minx,Math.max(maxy-miny,maxz-minz));
        }
        private double minx=1e100,maxx=-1e100,miny=1e100,maxy=-1e100,minz=1e100,maxz=-1e100;        
    }
    
    private interface Boundary extends Serializable {
        public void reset(Cube c,int s);
        public double intersectVal(Ray r);
        public Ray newRay(Ray r,double[] intersect);
        public JPanel getPropertiesPanel();
        public Boundary copy();
    }
    
    private class PeriodicCube implements Boundary {
        public PeriodicCube() {}
        public PeriodicCube(PeriodicCube c) {
            autoBounds=new EditableBoolean(c.autoBounds.getValue());
            xMinFormula=new DataFormula(c.xMinFormula);
            xMaxFormula=new DataFormula(c.xMaxFormula);
            yMinFormula=new DataFormula(c.yMinFormula);
            yMaxFormula=new DataFormula(c.yMaxFormula);
            zMinFormula=new DataFormula(c.zMinFormula);
            zMaxFormula=new DataFormula(c.zMaxFormula);
        }
        @Override
        public void reset(Cube c,int s) {
            if(autoBounds.getValue()) {
                xmin=c.minx;
                xmax=c.maxx;
                ymin=c.miny;
                ymax=c.maxy;
                zmin=c.minz;
                zmax=c.maxz;
            } else {
                xmin=xMinFormula.valueOf(OccultationFilter.this,s,0);
                xmax=xMaxFormula.valueOf(OccultationFilter.this,s,0);
                ymin=yMinFormula.valueOf(OccultationFilter.this,s,0);
                ymax=yMaxFormula.valueOf(OccultationFilter.this,s,0);
                zmin=zMinFormula.valueOf(OccultationFilter.this,s,0);
                zmax=zMaxFormula.valueOf(OccultationFilter.this,s,0);
            }
        }
        
        @Override
        public double intersectVal(Ray r) {
            double ret=iTime(r.startX(),r.oneX(),xmin,xmax);
            double tmp=iTime(r.startY(),r.oneY(),ymin,ymax);
            if(ret==-1 || (tmp>0 && tmp<ret)) ret=tmp;
            tmp=iTime(r.startZ(),r.oneZ(),zmin,zmax);
            if(ret==-1 || (tmp>0 && tmp<ret)) ret=tmp;
            return ret;
        }

        @Override
        public Ray newRay(Ray r, double[] intersect) {
            double[] dir=r.getDirectionVector();
            double[] sp={intersect[0],intersect[1],intersect[2]};
            if(Math.abs(intersect[0]-xmin)<1e-7*(xmax-xmin)) {
                sp[0]+=(xmax-xmin);
            } else if(Math.abs(intersect[0]-xmax)<1e-7*(xmax-xmin)) {
                sp[0]-=(xmax-xmin);
            }
            if(Math.abs(intersect[1]-ymin)<1e-7*(ymax-ymin)) {
                sp[1]+=(ymax-ymin);
            } else if(Math.abs(intersect[1]-ymax)<1e-7*(ymax-ymin)) {
                sp[1]-=(ymax-ymin);
            }
            if(Math.abs(intersect[2]-zmin)<1e-7*(zmax-zmin)) {
                sp[2]+=(zmax-zmin);
            } else if(Math.abs(intersect[2]-zmax)<1e-7*(zmax-zmin)) {
                sp[2]-=(zmax-zmin);
            }
            dir[0]+=sp[0];
            dir[1]+=sp[1];
            dir[2]+=sp[2];
            return new Ray(sp,dir);
        }

        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(7,1));
                northPanel.add(autoBounds.getCheckBox("Use Geometry Bounds?", null));
                northPanel.add(xMinFormula.getLabeledTextField("X Minimum",null));
                northPanel.add(xMaxFormula.getLabeledTextField("X Maximum",null));
                northPanel.add(yMinFormula.getLabeledTextField("Y Minimum",null));
                northPanel.add(yMaxFormula.getLabeledTextField("Y Maximum",null));
                northPanel.add(zMinFormula.getLabeledTextField("Z Minimum",null));
                northPanel.add(zMaxFormula.getLabeledTextField("Z Maximum",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        
        @Override
        public Boundary copy() {
            return new PeriodicCube(this);
        }
        
        @Override
        public String toString() {
            return "Periodic Cube";
        }
        
        private double iTime(double v1,double v2,double min,double max) {
            double ret=-1;
            if(v1<v2) {
                ret=(max-v1)/(v2-v1);
            } else if(v1>v2) {
                ret=(v1-min)/(v1-v2);                
            }
            if(ret<0) ret=-1;
            return ret;
        }
        
        private EditableBoolean autoBounds=new EditableBoolean(false);
        private DataFormula xMinFormula=new DataFormula("-1");
        private DataFormula xMaxFormula=new DataFormula("1");
        private DataFormula yMinFormula=new DataFormula("-1");
        private DataFormula yMaxFormula=new DataFormula("1");
        private DataFormula zMinFormula=new DataFormula("-1");
        private DataFormula zMaxFormula=new DataFormula("1");

        private transient double xmin,xmax,ymin,ymax,zmin,zmax;
        private transient JPanel propPanel;
        private static final long serialVersionUID = 8047530545667127338L;
    }
    
    private class SlidingBrick implements Boundary {
        public SlidingBrick() {}
        public SlidingBrick(SlidingBrick c) {
            xMinFormula=new DataFormula(c.xMinFormula);
            xMaxFormula=new DataFormula(c.xMaxFormula);
            yMinFormula=new DataFormula(c.yMinFormula);
            yMaxFormula=new DataFormula(c.yMaxFormula);
            yShearFormula=new DataFormula(c.yShearFormula);            
        }
        @Override
        public void reset(Cube c,int s) {
            xmin=xMinFormula.valueOf(OccultationFilter.this,s,0);
            xmax=xMaxFormula.valueOf(OccultationFilter.this,s,0);
            ymin=yMinFormula.valueOf(OccultationFilter.this,s,0);
            ymax=yMaxFormula.valueOf(OccultationFilter.this,s,0);
            yShear=yShearFormula.valueOf(OccultationFilter.this,s,0);
        }
        
        @Override
        public double intersectVal(Ray r) {
            double ret=iTime(r.startX(),r.oneX(),xmin,xmax);
            double tmp=iTime(r.startY(),r.oneY(),ymin,ymax);
            if(ret==-1 || (tmp>0 && tmp<ret)) ret=tmp;
            return ret;
        }

        @Override
        public Ray newRay(Ray r, double[] intersect) {
            double[] dir=r.getDirectionVector();
            double[] sp={intersect[0],intersect[1],intersect[2]};
            if(Math.abs(intersect[1]-ymin)<1e-7*(ymax-ymin)) {
                sp[1]+=(ymax-ymin);
            } else if(Math.abs(intersect[1]-ymax)<1e-7*(ymax-ymin)) {
                sp[1]-=(ymax-ymin);
            }
            if(Math.abs(intersect[0]-xmin)<1e-7*(xmax-xmin)) {
                sp[0]+=(xmax-xmin);
                sp[1]+=yShear;
                while(sp[1]>ymax) sp[1]-=(ymax-ymin); 
                while(sp[1]<ymin) sp[1]+=(ymax-ymin); 
            } else if(Math.abs(intersect[0]-xmax)<1e-7*(xmax-xmin)) {
                sp[0]-=(xmax-xmin);
                sp[1]-=yShear;
                while(sp[1]>ymax) sp[1]-=(ymax-ymin); 
                while(sp[1]<ymin) sp[1]+=(ymax-ymin); 
            }
            dir[0]+=sp[0];
            dir[1]+=sp[1];
            dir[2]+=sp[2];
            //System.out.println("Old ray is "+r);
            //System.out.println("New ray is "+new Ray(sp,dir));
            return new Ray(sp,dir);
        }

        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(5,1));
                northPanel.add(xMinFormula.getLabeledTextField("X Minimum",null));
                northPanel.add(xMaxFormula.getLabeledTextField("X Maximum",null));
                northPanel.add(yMinFormula.getLabeledTextField("Y Minimum",null));
                northPanel.add(yMaxFormula.getLabeledTextField("Y Maximum",null));
                northPanel.add(yShearFormula.getLabeledTextField("Y Shear",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        
        @Override
        public Boundary copy() {
            return new SlidingBrick(this);
        }
        
        @Override
        public String toString() {
            return "Sliding Brick";
        }
        
        private double iTime(double v1,double v2,double min,double max) {
            double ret=-1;
            //System.out.println(v1+" "+v2+"  "+min+" "+max);
            if(v1<v2) {
                ret=(max-v1)/(v2-v1);
            } else if(v1>v2) {
                ret=(v1-min)/(v1-v2);           
            }
            if(ret<0) ret=-1;
            return ret;
        }
        
        private DataFormula xMinFormula=new DataFormula("-1");
        private DataFormula xMaxFormula=new DataFormula("1");
        private DataFormula yMinFormula=new DataFormula("-1");
        private DataFormula yMaxFormula=new DataFormula("1");
        private DataFormula yShearFormula=new DataFormula("0");

        private transient double xmin,xmax,ymin,ymax,yShear;
        private transient JPanel propPanel;
        private static final long serialVersionUID = 103407905698574718L;
    }
    
    private class GeneralPlane implements Boundary {
        public GeneralPlane() {}
        public GeneralPlane(GeneralPlane c) {
            xPointFormula=new DataFormula(c.xPointFormula);
            yPointFormula=new DataFormula(c.yPointFormula);
            zPointFormula=new DataFormula(c.zPointFormula);
            xNormalFormula=new DataFormula(c.xNormalFormula);
            yNormalFormula=new DataFormula(c.yNormalFormula);
            zNormalFormula=new DataFormula(c.zNormalFormula);
            xOffsetFormula=new DataFormula(c.xOffsetFormula);
            yOffsetFormula=new DataFormula(c.yOffsetFormula);
            zOffsetFormula=new DataFormula(c.zOffsetFormula);
        }
        @Override
        public void reset(Cube c,int s) {
            p=new double[]{xPointFormula.valueOf(OccultationFilter.this,s,0),yPointFormula.valueOf(OccultationFilter.this,s,0),zPointFormula.valueOf(OccultationFilter.this,s,0)};
            n=new double[]{xNormalFormula.valueOf(OccultationFilter.this,s,0),yNormalFormula.valueOf(OccultationFilter.this,s,0),zNormalFormula.valueOf(OccultationFilter.this,s,0)};
            Basic.normalize(n);
            o=new double[]{xOffsetFormula.valueOf(OccultationFilter.this,s,0),yOffsetFormula.valueOf(OccultationFilter.this,s,0),zOffsetFormula.valueOf(OccultationFilter.this,s,0)};
        }
        
        @Override
        public double intersectVal(Ray r) {
            double[] toPlane={p[0]-r.startX(),p[1]-r.startY(),p[2]-r.startZ()};
            double tpd=Basic.dotProduct(toPlane,n);
            double rd=Basic.dotProduct(r.getDirectionVector(),n);
            return tpd/rd;
        }

        @Override
        public Ray newRay(Ray r, double[] intersect) {
            double[] s={intersect[0]+o[0],intersect[1]+o[1],intersect[2]+o[2]};
            double[] d=r.getDirectionVector();
            double[] e={s[0]+d[0],s[1]+d[1],s[2]+d[2]};
            return new Ray(s,e);
        }

        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(9,1));
                northPanel.add(xPointFormula.getLabeledTextField("X in Plane",null));
                northPanel.add(yPointFormula.getLabeledTextField("Y in Plane",null));
                northPanel.add(zPointFormula.getLabeledTextField("Z in Plane",null));
                northPanel.add(xNormalFormula.getLabeledTextField("X Normal to Plane",null));
                northPanel.add(yNormalFormula.getLabeledTextField("Y Normal to Plane",null));
                northPanel.add(zNormalFormula.getLabeledTextField("Z Normal to Plane",null));
                northPanel.add(xOffsetFormula.getLabeledTextField("X Offset",null));
                northPanel.add(yOffsetFormula.getLabeledTextField("Y Offset",null));
                northPanel.add(zOffsetFormula.getLabeledTextField("Z Offset",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }

        @Override
        public Boundary copy() {
            return new GeneralPlane(this);
        }
        
        @Override
        public String toString() {
            return "General Plane";
        }
        
        private DataFormula xPointFormula=new DataFormula("0");
        private DataFormula yPointFormula=new DataFormula("0");
        private DataFormula zPointFormula=new DataFormula("0");
        private DataFormula xNormalFormula=new DataFormula("0");
        private DataFormula yNormalFormula=new DataFormula("0");
        private DataFormula zNormalFormula=new DataFormula("1");
        private DataFormula xOffsetFormula=new DataFormula("0");
        private DataFormula yOffsetFormula=new DataFormula("0");
        private DataFormula zOffsetFormula=new DataFormula("1");
        
        private transient double[] p,n,o;
        private transient JPanel propPanel;
        private static final long serialVersionUID = -4798930803580612272L;
    }

    private class PeriodicSphere implements Boundary {
        public PeriodicSphere() {}
        public PeriodicSphere(PeriodicSphere c) {
            xPointFormula=new DataFormula(c.xPointFormula);
            yPointFormula=new DataFormula(c.yPointFormula);
            zPointFormula=new DataFormula(c.zPointFormula);
            radiusFormula=new DataFormula(c.radiusFormula);            
        }
        
        @Override
        public void reset(Cube c,int s) {
            x=xPointFormula.valueOf(OccultationFilter.this,s,0);
            y=yPointFormula.valueOf(OccultationFilter.this,s,0);
            z=zPointFormula.valueOf(OccultationFilter.this,s,0);
            r=radiusFormula.valueOf(OccultationFilter.this,s,0);
        }
        
        @Override
        public double intersectVal(Ray ray) {
            double[] rv=ray.getDirectionVector();
            double[] d={ray.startX()-x,ray.startY()-y,ray.startZ()-z};
            double a=Basic.dotProduct(rv,rv);
            double b=2*Basic.dotProduct(rv,d);
            double c=Basic.dotProduct(d,d)-r*r;
            double root=b*b-4*a*c;
            if(root<0) return -1;
            return (-b+Math.sqrt(root))/(2*a);
        }

        @Override
        public Ray newRay(Ray r, double[] intersect) {
            double[] rv=r.getDirectionVector();
            double dx=intersect[0]-x;
            double dy=intersect[1]-y;
            double dz=intersect[2]-z;
            double[] s={x-dx,y-dy,z-dz};
            double[] e={s[0]+rv[0],s[1]+rv[1],s[2]+rv[2]};
            return new Ray(s,e);
        }

        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(4,1));
                northPanel.add(xPointFormula.getLabeledTextField("Center X",null));
                northPanel.add(yPointFormula.getLabeledTextField("Center Y",null));
                northPanel.add(zPointFormula.getLabeledTextField("Center Z",null));
                northPanel.add(radiusFormula.getLabeledTextField("Radius",null));
                propPanel.add(northPanel,BorderLayout.NORTH);                
            }
            return propPanel;
        }
     
        @Override
        public Boundary copy() {
            return new PeriodicSphere(this);
        }
        
        @Override
        public String toString() {
            return "Periodic Sphere";
        }
        
        private DataFormula xPointFormula=new DataFormula("0");
        private DataFormula yPointFormula=new DataFormula("0");
        private DataFormula zPointFormula=new DataFormula("0");
        private DataFormula radiusFormula=new DataFormula("1");

        private transient double x,y,z,r;
        private transient JPanel propPanel;
        private static final long serialVersionUID = 8047530545667127338L;
    }
    
    private class RTSphere2 extends RTSphere {
        public RTSphere2(Vect3D cen,double r,int i) {
            super(cen,r);
            index=i;
        }
        @Override
        public String toString() { return index+" "+super.toString(); }
        private int index;
    }
}
