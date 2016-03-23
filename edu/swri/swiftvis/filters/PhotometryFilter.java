/*
 * Created on Jun 13, 2009
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.plot.p3d.raytrace.Octree;
import edu.swri.swiftvis.plot.p3d.raytrace.RTGeometry;
import edu.swri.swiftvis.plot.p3d.raytrace.RTSphere;
import edu.swri.swiftvis.plot.p3d.raytrace.Ray;
import edu.swri.swiftvis.plot.p3d.raytrace.RayFollower;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.LoopBody;
import edu.swri.swiftvis.util.ThreadHandler;

public class PhotometryFilter extends AbstractMultipleSourceFilter {
    public PhotometryFilter() {
        lightSources.add(new DirectionSource());
    }
    
    private PhotometryFilter(PhotometryFilter c,List<GraphElement> l) {
        super(c,l);
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        zFormula=new DataFormula(c.zFormula);
        radiusFormula=new DataFormula(c.radiusFormula);
        albedoFormula=new DataFormula(c.albedoFormula);
        maxScatters=new EditableInt(c.maxScatters.getValue());
        maxWraps=new EditableInt(c.maxWraps.getValue());
        suppressTreeRebuild=new EditableBoolean(c.suppressTreeRebuild.getValue());
        selections=new ArrayList<ParticleSelection>();
        for(ParticleSelection ps:c.selections) {
            selections.add(new ParticleSelection(ps));
        }
        scatterStyle=c.scatterStyle;
        bounds=new ArrayList<Boundary>();
        for(Boundary b:c.bounds) {
            bounds.add(b.copy());
        }
        lightSources=new ArrayList<LightSource>();
        for(LightSource ls:c.lightSources) {
            lightSources.add(ls.copy());
        }
        binner=c.binner.copy();
    }

    @Override
    protected boolean doingInThreads() {
        return true;
    }

    @Override
    protected void redoAllElements() {
        sizeDataVectToInputStreams();
        if(suppressTreeRebuild==null) suppressTreeRebuild=new EditableBoolean(false);
        for(int s=0; s<getSource(0).getNumStreams(); ++s) {
            final int ss=s;
            int[] indexBounds=xFormula.getSafeElementRange(this,s);
            DataFormula.mergeSafeElementRanges(indexBounds, yFormula.getSafeElementRange(this, s));
            DataFormula.mergeSafeElementRanges(indexBounds, zFormula.getSafeElementRange(this, s));
            DataFormula.mergeSafeElementRanges(indexBounds, radiusFormula.getSafeElementRange(this, s));
            DataFormula.mergeSafeElementRanges(indexBounds, albedoFormula.getSafeElementRange(this, s));

            binner.reset(s);
            selectedParticles=new boolean[getSource(0).getNumElements(s)][selections.size()];
            
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
                for(int j=0; j<selections.size(); ++j) {
                    if(selections.get(j).selectFormula.valueOf(this,s,i)) {
                        selectedParticles[i][j]=true;
                    }
                }
            }
            for(Boundary b:bounds) {
                b.reset(cube,s);
            }
            double size=Math.max(cube.maxx-cube.minx,Math.max(cube.maxy-cube.miny,cube.maxz-cube.minz));
            if(tree==null || !suppressTreeRebuild.getValue()) {
                System.out.println("Building tree : "+tree+" and "+suppressTreeRebuild.getValue());
                tree=new Octree(0.5*(cube.maxx+cube.minx),0.5*(cube.maxy+cube.miny),0.5*(cube.maxz+cube.minz),size);
                for(RTGeometry g:geomList) {
                    tree.addObject(g);
                }
                tree.getRoot().finalize();
            }
            
            for(final LightSource ls:lightSources) {
                ThreadHandler.instance().chunkedForLoop(this,0,ls.getNumPhotons(),new LoopBody() {
                    @Override
                    public void execute(int i) {
                        PhotonPacket photon=ls.getPhoton(PhotometryFilter.this,ss,cube);
                        LinkedList<PhotonHit> path=new LinkedList<PhotonHit>();
                        followRay(photon,albedoFormula.valueOf(PhotometryFilter.this,ss,0),0,0,path,cube);
                    }
                });
            }
            binner.addData(dataVect.get(s*2),dataVect.get(s*2+1));
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel basicPanel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(9,1));
        northPanel.add(xFormula.getLabeledTextField("X Formula",null));
        northPanel.add(yFormula.getLabeledTextField("Y Formula",null));
        northPanel.add(zFormula.getLabeledTextField("Z Formula",null));
        northPanel.add(radiusFormula.getLabeledTextField("Radius Formula",null));
        northPanel.add(albedoFormula.getLabeledTextField("Albedo Formula",null));
        northPanel.add(maxScatters.getLabeledTextField("Max Scatters",null));
        northPanel.add(maxWraps.getLabeledTextField("Max Wraps",null));
        final JComboBox scatterBox=new JComboBox(scatterStyles);
        scatterBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(scatterBox.getSelectedIndex()>=0) scatterStyle=scatterBox.getSelectedIndex();
            }
        });
        northPanel.add(scatterBox);
        if(suppressTreeRebuild==null) suppressTreeRebuild=new EditableBoolean(false);
        northPanel.add(suppressTreeRebuild.getCheckBox("Supress tree rebuild?",null));
        basicPanel.add(northPanel,BorderLayout.NORTH);
        JButton button=new JButton("Progagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        basicPanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Basic",basicPanel);
        
        JPanel sourcePanel=new JPanel(new BorderLayout());
        final JPanel sourcePropPanel=new JPanel(new GridLayout(1,1)); 
        northPanel=new JPanel(new BorderLayout());
        final JList sourceList=new JList(lightSources.toArray());
        sourceList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                sourcePropPanel.removeAll();
                LightSource ls=(LightSource)sourceList.getSelectedValue();
                if(ls==null) return;
                JPanel panel=ls.getPropertiesPanel();
                if(panel!=null) sourcePropPanel.add(panel);
                sourcePropPanel.validate();
                sourcePropPanel.repaint();
            }
        });
        northPanel.add(sourceList,BorderLayout.CENTER);
        JPanel buttonPanel=new JPanel(new GridLayout(1,2));
        button=new JButton("Add");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] sourceTypes={"Direction Source","Sphere Source"};
                int opt=JOptionPane.showOptionDialog(propPanel,"Select a source type.","Source Selection",JOptionPane.OK_OPTION,JOptionPane.INFORMATION_MESSAGE,null,sourceTypes,sourceTypes[0]);
                if(opt==0) {
                    lightSources.add(new DirectionSource());
                } else if(opt==1) {
                    lightSources.add(new SphereSource());
                } else {
                    return;
                }
                sourceList.setListData(lightSources.toArray());                
            }
        });
        buttonPanel.add(button);
        button=new JButton("Remove");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=sourceList.getSelectedIndex();
                if(index<0) return;
                lightSources.remove(index);
                sourceList.setListData(lightSources.toArray());
                sourcePropPanel.removeAll();
                sourcePropPanel.validate();
                sourcePropPanel.repaint();
            }
        });
        buttonPanel.add(button);
        northPanel.add(buttonPanel,BorderLayout.SOUTH);
        northPanel.setPreferredSize(new Dimension(200,200));
        sourcePanel.add(northPanel,BorderLayout.NORTH);
        sourcePanel.add(new JScrollPane(sourcePropPanel));
        button=new JButton("Progagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        sourcePanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Photon Sources",sourcePanel);
        
        JPanel selectionPanel=new JPanel(new BorderLayout());
        final JPanel selPropPanel=new JPanel(new GridLayout(1,1)); 
        northPanel=new JPanel(new BorderLayout());
        final JList selList=new JList(selections.toArray());
        selList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                selPropPanel.removeAll();
                ParticleSelection ps=(ParticleSelection)selList.getSelectedValue();
                if(ps==null) return;
                JPanel panel=ps.getPropertiesPanel();
                if(panel!=null) selPropPanel.add(panel);
                selPropPanel.validate();
                selPropPanel.repaint();
            }
        });
        northPanel.add(selList,BorderLayout.CENTER);
        buttonPanel=new JPanel(new GridLayout(1,2));
        button=new JButton("Add");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selections.add(new ParticleSelection());
                selList.setListData(selections.toArray());                
            }
        });
        buttonPanel.add(button);
        button=new JButton("Remove");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=selList.getSelectedIndex();
                if(index<0) return;
                selections.remove(index);
                selList.setListData(selections.toArray());
                selPropPanel.removeAll();
                selPropPanel.validate();
                selPropPanel.repaint();
            }
        });
        buttonPanel.add(button);
        northPanel.add(buttonPanel,BorderLayout.SOUTH);
        northPanel.setPreferredSize(new Dimension(200,200));
        selectionPanel.add(northPanel,BorderLayout.NORTH);
        selectionPanel.add(new JScrollPane(selPropPanel));
        button=new JButton("Progagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        selectionPanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Selections",selectionPanel);
        
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
        buttonPanel=new JPanel(new GridLayout(1,2));
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
        
        JPanel binningPanel=new JPanel(new BorderLayout());
        String[] binStyleNames={"Camera Binner","Cylinder Binner","Sphere Binner"};
        if(binStyles==null) {
            binStyles=new DataBinner[binStyleNames.length];
            binStyles[binner.typeIndex()]=binner;
        }
        final JComboBox binBox=new JComboBox(binStyleNames);
        binBox.setSelectedIndex(binner.typeIndex());
        binningPanel.add(binBox,BorderLayout.NORTH);
        final JPanel binPropPanel=new JPanel(new GridLayout(1,1));
        binPropPanel.add(binner.getPropertiesPanel());
        binningPanel.add(new JScrollPane(binPropPanel),BorderLayout.CENTER);
        binBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=binBox.getSelectedIndex();
                if(index<0) return;
                binPropPanel.removeAll();
                if(binStyles[index]==null) {
                    if(index==0) {
                        binner=new CameraBinner();
                    } else if(index==1) {
                        binner=new CylinderBinner();
                    } else if(index==2) {
                        binner=new SphereBinner();
                    }
                    binStyles[index]=binner;
                } else {
                    binner=binStyles[index];
                }
                binPropPanel.add(binner.getPropertiesPanel());
                binPropPanel.validate();
                binPropPanel.repaint();
            }
        });
        button=new JButton("Progagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        binningPanel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Binning",binningPanel);
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
        if(stream%2==0) {
            return 3+2*selections.size();
        } else {
            return 2;
        }
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(stream%2==0) {
            String[] names={"Primary","Secondary","Intensity"};
            if(which<names.length) return names[which];
            which-=names.length;
            return selections.get(which/2).name+((which%2==0)?" Direct":" Scattered");
        } else {
            if(which==0) {
                return "Direct Intensity";
            } else {
                return "Scattered Intensity";
            }
        }
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new PhotometryFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Photometry Filter";
    }
    
    public static String getTypeDescription() {
        return "Photometry Filter";
    }

    @Override
    protected void sizeDataVectToInputStreams() {
        if(dataVect.size()>2*inputVector.get(0).getNumStreams()) dataVect.clear();
        while(dataVect.size()<2*inputVector.get(0).getNumStreams()) dataVect.add(new ArrayList<DataElement>());
    }
    
    private void followRay(PhotonPacket photon,double albedo,int numScatters,int numWraps,List<PhotonHit> path,Cube c) {
        if(numScatters>maxScatters.getValue() || numWraps>maxWraps.getValue()) return;
        double px=photon.ray.startX();
        double py=photon.ray.startY();
        double pz=photon.ray.startZ();
        if((numScatters>1 || numWraps>1) && (px<c.minx || px>c.maxx || py<c.miny || py>c.maxy || pz<c.minz || pz>c.maxz)) return;
        Boundary firstBound=null;
        double mint=-1;
        for(Boundary b:bounds) {
            double t=b.intersectVal(photon.ray);
            if(t>0 && (firstBound==null || t<mint)) {
                mint=t;
                firstBound=b;
            }
        }
        RayFollower rf=new RayFollower(photon.ray,tree);
        if(rf.follow() && (firstBound==null || mint>=rf.getIntersectTime())) {
            RTSphere2 geom=(RTSphere2)rf.getIntersectObject();
            double[] intersect=photon.ray.point(rf.getIntersectTime()*0.999999);
            double[] norm=geom.getNormal(intersect);
            Basic.normalize(norm);
            path.add(new PhotonHit(geom.index,photon,intersect,norm));
            binner.addPath(path);
            Ray scatterRay=scatterStyles[scatterStyle].scatter(photon.ray,intersect,norm);
            PhotonPacket pp=new PhotonPacket(scatterRay,photon.strength*albedo);
            followRay(pp,albedo,numScatters+1,numWraps,path,c);
        } else if(firstBound!=null) {
            double[] intersect=photon.ray.point(mint);
            PhotonPacket pp=new PhotonPacket(firstBound.newRay(photon.ray,intersect),photon.strength);
            followRay(pp,albedo,numScatters,numWraps+1,path,c);
        }
    }

    // formulas for particle properties and tree
    private DataFormula xFormula=new DataFormula("v[0]");
    private DataFormula yFormula=new DataFormula("v[1]");
    private DataFormula zFormula=new DataFormula("v[2]");
    private DataFormula radiusFormula=new DataFormula("v[6]");
    private DataFormula albedoFormula=new DataFormula("0.9");
    private EditableInt maxScatters=new EditableInt(10);
    private EditableInt maxWraps=new EditableInt(10);
    private EditableBoolean suppressTreeRebuild=new EditableBoolean(false);

    // special particle selections
    private List<ParticleSelection> selections=new ArrayList<ParticleSelection>();
    
    // scattering settings
    private int scatterStyle;
    
    // periodic boundaries
    private List<Boundary> bounds=new ArrayList<Boundary> ();
    
    // data for light sources
    private List<LightSource> lightSources=new ArrayList<LightSource>();
    
    // binning specifications
    private DataBinner binner=new CameraBinner();
    
    private transient Octree tree;
    private transient boolean[][] selectedParticles;
    private transient DataBinner[] binStyles;

    private static final ScatterStyle[] scatterStyles={new LambertScattering()};
    private static final long serialVersionUID = -4077984307888423004L;
    
    private static class Cube {
        public double maxEdge() {
            return Math.max(maxx-minx,Math.max(maxy-miny,maxz-minz));
        }
        private double minx=1e100,maxx=-1e100,miny=1e100,maxy=-1e100,minz=1e100,maxz=-1e100;        
    }
    
    private static class ParticleSelection implements Serializable {
        public ParticleSelection() {}
        public ParticleSelection(ParticleSelection c) {
            name=new EditableString(c.name.getValue());
            selectFormula=new BooleanFormula(c.selectFormula);
        }
        @Override
        public String toString() {
            return name.getValue()+" : "+selectFormula.getFormula();
        }
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(2,1));
                northPanel.add(name.getLabeledTextField("Name",null));
                northPanel.add(selectFormula.getLabeledTextField("Selection Formula",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        private EditableString name=new EditableString("Selection");
        private BooleanFormula selectFormula=new BooleanFormula("1=0");
        
        private transient JPanel propPanel;
        private static final long serialVersionUID = 886122397347811649L;
    }
    
    private static interface ScatterStyle {
        public Ray scatter(Ray in,double[] intersect,double[] normal);
        public double directionalIntensity(Ray in,double[] intersect,double[] normal,Ray out);
    }
    
    private static class LambertScattering implements ScatterStyle {
        @Override
        public Ray scatter(Ray in, double[] intersect, double[] n) {
            double phi=Math.random()/(2*Math.PI);
            double cosphi=Math.cos(phi);
            double sinphi=Math.sin(phi);
            double r2=Math.random();
            double coseps=Math.sqrt(r2);
            double sineps=Math.sqrt(1-r2);
            double[] e=new double[3];
            if(n[2]==1) {
                e[2]=n[2]*coseps;
                e[0]=sineps*cosphi;
                e[1]=sineps*sinphi;
            } else {
                double root=Math.sqrt(1-n[2]*n[2]);
                e[2]=n[2]*coseps+sineps*cosphi*root;
                e[0]=(n[0]*(coseps-n[2]*e[2])-sineps*sinphi*n[1]*root)/(1-n[2]*n[2]);
                e[1]=(n[1]*(coseps-n[2]*e[2])-sineps*sinphi*n[0]*root)/(1-n[2]*n[2]);
            }
            e[0]+=intersect[0];
            e[1]+=intersect[1];
            e[2]+=intersect[2];
            return new Ray(intersect,e);
        }

        @Override
        public double directionalIntensity(Ray in,double[] intersect,double[] normal,Ray out) {
            double[] dir=out.getDirectionVector();
            Basic.normalize(dir);
            double dot=Basic.dotProduct(dir,normal);
            if(dot<0) return 0;
            return dot;
        }
        
        @Override
        public String toString() { return "Lambert Scattering"; }
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
                xmin=xMinFormula.valueOf(PhotometryFilter.this,s,0);
                xmax=xMaxFormula.valueOf(PhotometryFilter.this,s,0);
                ymin=yMinFormula.valueOf(PhotometryFilter.this,s,0);
                ymax=yMaxFormula.valueOf(PhotometryFilter.this,s,0);
                zmin=zMinFormula.valueOf(PhotometryFilter.this,s,0);
                zmax=zMaxFormula.valueOf(PhotometryFilter.this,s,0);
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
            xmin=xMinFormula.valueOf(PhotometryFilter.this,s,0);
            xmax=xMaxFormula.valueOf(PhotometryFilter.this,s,0);
            ymin=yMinFormula.valueOf(PhotometryFilter.this,s,0);
            ymax=yMaxFormula.valueOf(PhotometryFilter.this,s,0);
            yShear=yShearFormula.valueOf(PhotometryFilter.this,s,0);
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
            p=new double[]{xPointFormula.valueOf(PhotometryFilter.this,s,0),yPointFormula.valueOf(PhotometryFilter.this,s,0),zPointFormula.valueOf(PhotometryFilter.this,s,0)};
            n=new double[]{xNormalFormula.valueOf(PhotometryFilter.this,s,0),yNormalFormula.valueOf(PhotometryFilter.this,s,0),zNormalFormula.valueOf(PhotometryFilter.this,s,0)};
            Basic.normalize(n);
            o=new double[]{xOffsetFormula.valueOf(PhotometryFilter.this,s,0),yOffsetFormula.valueOf(PhotometryFilter.this,s,0),zOffsetFormula.valueOf(PhotometryFilter.this,s,0)};
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
            x=xPointFormula.valueOf(PhotometryFilter.this,s,0);
            y=yPointFormula.valueOf(PhotometryFilter.this,s,0);
            z=zPointFormula.valueOf(PhotometryFilter.this,s,0);
            r=radiusFormula.valueOf(PhotometryFilter.this,s,0);
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
    
    private static class PhotonPacket {
        public PhotonPacket(Ray r,double s) {
            ray=r;
            strength=s;
        }
        private Ray ray;
        private double strength;
    }

    /**
     * The light sources will send rays through either the z=maxz or z=minz planes. 
     */
    private static interface LightSource extends Serializable {
        public int getNumPhotons();
        public PhotonPacket getPhoton(DataSink sink,int stream,Cube b);
        public JPanel getPropertiesPanel();
        public LightSource copy();
    }
    
    private static class DirectionSource implements LightSource {
        public DirectionSource() {}
        public DirectionSource(DirectionSource c) {
            numPhotons=new EditableInt(c.numPhotons.getValue());
            xDirFormula=new DataFormula(c.xDirFormula);
            yDirFormula=new DataFormula(c.yDirFormula);
            zDirFormula=new DataFormula(c.zDirFormula);
            intensityFormula=new DataFormula(c.intensityFormula);
        }
        
        @Override
        public int getNumPhotons() {
            return numPhotons.getValue();
        }

        @Override
        public PhotonPacket getPhoton(DataSink sink,int stream,Cube b) {
            double[] dir={xDirFormula.valueOf(sink,stream,0),yDirFormula.valueOf(sink,stream,0),zDirFormula.valueOf(sink,stream,0)};
            Basic.normalize(dir);
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
            Ray ray=new Ray(zero,one);
            return new PhotonPacket(ray,intensityFormula.valueOf(sink,stream,0)/numPhotons.getValue());
        }
        
        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(5,1));
                northPanel.add(numPhotons.getLabeledTextField("Number of Photons",null));
                northPanel.add(xDirFormula.getLabeledTextField("X Component",null));
                northPanel.add(yDirFormula.getLabeledTextField("Y Component",null));
                northPanel.add(zDirFormula.getLabeledTextField("Z Component",null));
                northPanel.add(intensityFormula.getLabeledTextField("Intensity",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        
        @Override
        public LightSource copy() {
            return new DirectionSource(this);
        }
        
        @Override
        public String toString() {
            return "Direction Source";
        }
        
        private EditableInt numPhotons=new EditableInt(10000);
        private DataFormula xDirFormula=new DataFormula("-1");
        private DataFormula yDirFormula=new DataFormula("0");
        private DataFormula zDirFormula=new DataFormula("-1");
        private DataFormula intensityFormula=new DataFormula("1");
        
        private transient JPanel propPanel;
        private static final long serialVersionUID = -2135931105773550211L;
    }

    private static class SphereSource implements LightSource {
        public SphereSource() {}
        public SphereSource(SphereSource c) {
            numPhotons=new EditableInt(c.numPhotons.getValue());
            xPosFormula=new DataFormula(c.xPosFormula);
            yPosFormula=new DataFormula(c.yPosFormula);
            zPosFormula=new DataFormula(c.zPosFormula);
            radiusFormula=new DataFormula(c.radiusFormula);
            intensityFormula=new DataFormula(c.intensityFormula);            
        }
        
        @Override
        public int getNumPhotons() {
            return numPhotons.getValue();
        }

        @Override
        public PhotonPacket getPhoton(DataSink sink,int stream,Cube b) {
            double cx=xPosFormula.valueOf(sink,stream,0);
            double cy=yPosFormula.valueOf(sink,stream,0);
            double cz=zPosFormula.valueOf(sink,stream,0);
            double radius=radiusFormula.valueOf(sink,stream,0);
            double intensity=intensityFormula.valueOf(sink,stream,0);
            double[] zero=new double[3];
            double[] one=new double[3];
            double[] dir=new double[3];
            boolean flag=true;
            while(flag) {
                // f(phi)=cos(phi)/2  [-pi/2<phi<pi/2]
                // F(phi)=0.5*(sin(phi)-sin(-pi/2))=0.5*(sin(phi)+1)
                // F^-1(x)=asin(2x-1)=phi
                double phi=Math.asin(2*Math.random()-1);
                double theta=2*Math.PI*Math.random();
                zero[0]=cx+radius*Math.cos(theta)*Math.cos(phi);
                zero[1]=cy+radius*Math.sin(theta)*Math.cos(phi);
                zero[2]=cz+radius*Math.sin(phi);
                double[] rv={zero[0]-cx,zero[1]-cy,zero[2]-cz};
                if(zero[2]>b.maxz) {
                    one[0]=b.minx+Math.random()*(b.maxx-b.minx);
                    one[1]=b.miny+Math.random()*(b.maxy-b.miny);
                    one[2]=b.maxz;
                } else if(zero[2]<b.minz) {
                    one[0]=b.minx+Math.random()*(b.maxx-b.minx);
                    one[1]=b.miny+Math.random()*(b.maxy-b.miny);
                    one[2]=b.minz;
                } else {
                    one[0]=b.minx+Math.random()*(b.maxx-b.minx);
                    one[1]=b.miny+Math.random()*(b.maxy-b.miny);
                    one[2]=b.minz+Math.random()*(b.maxz-b.minz);
                }
                dir[0]=one[0]-zero[0];
                dir[1]=one[1]-zero[1];
                dir[2]=one[2]-zero[2];
                flag=Basic.dotProduct(rv,dir)>0;
            }
            return new PhotonPacket(new Ray(zero,one),intensity/numPhotons.getValue());
        }

        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(6,1));
                northPanel.add(numPhotons.getLabeledTextField("Number of Photons",null));
                northPanel.add(xPosFormula.getLabeledTextField("Center X",null));
                northPanel.add(yPosFormula.getLabeledTextField("Center Y",null));
                northPanel.add(zPosFormula.getLabeledTextField("Center Z",null));
                northPanel.add(radiusFormula.getLabeledTextField("Radius",null));
                northPanel.add(intensityFormula.getLabeledTextField("Intensity",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        
        @Override
        public LightSource copy() {
            return new SphereSource(this);
        }

        @Override
        public String toString() {
            return "Sphere Source";
        }
        
        private EditableInt numPhotons=new EditableInt(10000);
        private DataFormula xPosFormula=new DataFormula("-1");
        private DataFormula yPosFormula=new DataFormula("0");
        private DataFormula zPosFormula=new DataFormula("0");
        private DataFormula radiusFormula=new DataFormula("0.5");
        private DataFormula intensityFormula=new DataFormula("1");
        
        private transient JPanel propPanel;
        private static final long serialVersionUID = -5011851160576435527L;
    }
    
    private static class PhotonHit {
        public PhotonHit(int e,PhotonPacket p,double[] inter,double[] n) {
            elemHit=e;
            photon=p;
            intersect=inter;
            normal=n;
        }
        private int elemHit;
        private PhotonPacket photon;
        private double[] intersect;
        private double[] normal;
    }

    private abstract class DataBinner implements Serializable {
        public DataBinner() {}
        public DataBinner(DataBinner c) {
            min1Value=new EditableDouble(c.min1Value.getValue());
            max1Value=new EditableDouble(c.max1Value.getValue());
            numBins1=new EditableInt(c.numBins1.getValue());
            min2Value=new EditableDouble(c.min2Value.getValue());
            max2Value=new EditableDouble(c.max2Value.getValue());
            numBins2=new EditableInt(c.numBins2.getValue());
        }
        public void reset(int s) {
            particleIntensity=new double[getSource(0).getNumElements(s)][2];
            intensityBins=new double[numBins1.getValue()][numBins2.getValue()][2*selections.size()+1];
            axis1Values=new double[numBins1.getValue()];
            axis2Values=new double[numBins2.getValue()];
            for(int i=0; i<axis1Values.length; ++i) {
                axis1Values[i]=min1Value.getValue()+i*(max1Value.getValue()-min1Value.getValue())/(axis1Values.length-1);
            }
            for(int i=0; i<axis2Values.length; ++i) {
                axis2Values[i]=min2Value.getValue()+i*(max2Value.getValue()-min2Value.getValue())/(axis2Values.length-1);
            }
            abstractReset(s);
        }
        public abstract void addPath(List<PhotonHit> path);
        public abstract int typeIndex();
        public abstract DataBinner copy();
        public void addData(List<DataElement> binStream,List<DataElement> intensityStream) {
            int[] params=new int[0];
            float[] vals=new float[3+2*selections.size()];
            for(int i=0; i<intensityBins.length; ++i) {
                vals[0]=(float)axis1Values[i];
                for(int j=0; j<intensityBins[i].length; ++j) {
                    vals[1]=(float)axis2Values[j];
                    for(int k=0; k<intensityBins[i][j].length; ++k) {
                        vals[k+2]=(float)intensityBins[i][j][k];
                    }
                    binStream.add(new DataElement(params,vals));
                }
            }
            vals=new float[2];
            for(int i=0; i<particleIntensity.length; ++i) {
                vals[0]=(float)particleIntensity[i][0];
                vals[1]=(float)particleIntensity[i][1];
                intensityStream.add(new DataElement(params,vals));
            }
        }
        public JTabbedPane getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JTabbedPane();
                JPanel binningPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(6,1));
                northPanel.add(min1Value.getLabeledTextField(primaryName()+" Minimum Bin Value",null));
                northPanel.add(max1Value.getLabeledTextField(primaryName()+" Maximum Bin Value",null));
                northPanel.add(numBins1.getLabeledTextField("Number of "+primaryName()+" Bins",null));
                northPanel.add(min2Value.getLabeledTextField(secondaryName()+" Minimum Bin Value",null));
                northPanel.add(max2Value.getLabeledTextField(secondaryName()+" Maximum Bin Value",null));
                northPanel.add(numBins2.getLabeledTextField("Number of "+secondaryName()+" Bins",null));
                binningPanel.add(northPanel,BorderLayout.NORTH);
                propPanel.addTab("Basic Settings",binningPanel);
                addSpecificTabs();
            }
            return propPanel;
        }
        protected abstract void abstractReset(int s);
        protected abstract void addSpecificTabs();
        protected abstract String primaryName();
        protected abstract String secondaryName();
        protected void addPathIntensity(List<PhotonHit> path,double intensity) {
            int p=path.get(path.size()-1).elemHit;
            particleIntensity[p][0]+=intensity;
            for(PhotonHit ph:path) {
                particleIntensity[ph.elemHit][1]+=intensity;
            }
        }
        protected void addPathToBin(List<PhotonHit> path,int b1,int b2,double intensity) {
            if(b1>=0 && b1<intensityBins.length && b2>=0 && b2<intensityBins[b1].length) {
                intensityBins[b1][b2][0]+=intensity;
                for(int i=0; i<selections.size(); ++i) {
                    if(selectedParticles[path.get(path.size()-1).elemHit][i]) {
                        intensityBins[b1][b2][i*2+1]+=intensity;                        
                        intensityBins[b1][b2][i*2+2]+=intensity;                        
                    } else {
                        boolean onPath=false;
                        for(PhotonHit ph:path) {
                            if(selectedParticles[ph.elemHit][i]) {
                                onPath=true;
                            }
                        }
                        if(onPath) intensityBins[b1][b2][i*2+2]+=intensity;                        
                    }
                }
            }            
        }
        
        protected EditableDouble min1Value=new EditableDouble(-1);
        protected EditableDouble max1Value=new EditableDouble(1);
        protected EditableInt numBins1=new EditableInt(100);
        protected EditableDouble min2Value=new EditableDouble(-1);
        protected EditableDouble max2Value=new EditableDouble(1);
        protected EditableInt numBins2=new EditableInt(100);
        
        private transient double[][] particleIntensity;
        protected transient double[][][] intensityBins;   // x,y,selector
        protected transient double[] axis1Values;
        protected transient double[] axis2Values;
        protected transient JTabbedPane propPanel;
        private static final long serialVersionUID = 2419339575885496557L;
    }
    
    private class CameraBinner extends DataBinner {
        public CameraBinner() {}
        public CameraBinner(CameraBinner c) {
            super(c);
            xPosFormula=new DataFormula(c.xPosFormula);
            yPosFormula=new DataFormula(c.yPosFormula);
            zPosFormula=new DataFormula(c.zPosFormula);
            xForwardFormula=new DataFormula(c.xForwardFormula);
            yForwardFormula=new DataFormula(c.yForwardFormula);
            zForwardFormula=new DataFormula(c.zForwardFormula);
            xUpFormula=new DataFormula(c.xUpFormula);
            yUpFormula=new DataFormula(c.yUpFormula);
            zUpFormula=new DataFormula(c.zUpFormula);
        }
        @Override
        protected void abstractReset(int s) {
            camPos=new double[]{xPosFormula.valueOf(PhotometryFilter.this,s,0),yPosFormula.valueOf(PhotometryFilter.this,s,0),zPosFormula.valueOf(PhotometryFilter.this,s,0)};
            forward=new double[]{xForwardFormula.valueOf(PhotometryFilter.this,s,0),yForwardFormula.valueOf(PhotometryFilter.this,s,0),zForwardFormula.valueOf(PhotometryFilter.this,s,0)};
            Basic.normalize(forward);
            up=new double[]{xUpFormula.valueOf(PhotometryFilter.this,s,0),yUpFormula.valueOf(PhotometryFilter.this,s,0),zUpFormula.valueOf(PhotometryFilter.this,s,0)};
            Basic.normalize(up);
            right=Basic.crossProduct(forward,up);
            Basic.normalize(right);
        }
        
        @Override
        public void addPath(List<PhotonHit> path) {
            PhotonHit lastHit=path.get(path.size()-1);
            Ray toCam=new Ray(lastHit.intersect,camPos);
            RayFollower rf=new RayFollower(toCam,tree);
            if(rf.follow()) return;
            double intensity=lastHit.photon.strength*scatterStyles[scatterStyle].directionalIntensity(lastHit.photon.ray,lastHit.intersect,lastHit.normal,toCam);
            addPathIntensity(path,intensity);
            double[] tl=toCam.getDirectionVector();
            double upDist=Basic.dotProduct(tl,up);
            double rightDist=Basic.dotProduct(tl,right);
            int upBin=(int)(numBins2.getValue()*(upDist-min2Value.getValue())/(max2Value.getValue()-min2Value.getValue()));
            int rightBin=(int)(numBins1.getValue()*(rightDist-min1Value.getValue())/(max1Value.getValue()-min1Value.getValue()));
            addPathToBin(path,rightBin,upBin,intensity);
        }
        
        @Override
        public int typeIndex() {
            return 0;
        }

        @Override
        protected void addSpecificTabs() {
            JPanel camPosPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(9,1));
            northPanel.add(xPosFormula.getLabeledTextField("X Position Formula",null));
            northPanel.add(yPosFormula.getLabeledTextField("Y Position Formula",null));
            northPanel.add(zPosFormula.getLabeledTextField("Z Position Formula",null));
            northPanel.add(xForwardFormula.getLabeledTextField("X Forward Formula",null));
            northPanel.add(yForwardFormula.getLabeledTextField("Y Forward Formula",null));
            northPanel.add(zForwardFormula.getLabeledTextField("Z Forward Formula",null));
            northPanel.add(xUpFormula.getLabeledTextField("X Up Formula",null));
            northPanel.add(yUpFormula.getLabeledTextField("Y Up Formula",null));
            northPanel.add(zUpFormula.getLabeledTextField("Z Up Formula",null));
            camPosPanel.add(northPanel,BorderLayout.NORTH);
            propPanel.addTab("Camera Settings",camPosPanel);
        }
        
        @Override
        protected String primaryName() {
            return "Right";
        }
        
        @Override
        protected String secondaryName() {
            return "Up";
        }
        
        @Override
        public DataBinner copy() {
            return new CameraBinner(this);
        }
        
        private DataFormula xPosFormula=new DataFormula("1");
        private DataFormula yPosFormula=new DataFormula("0");
        private DataFormula zPosFormula=new DataFormula("1");
        private DataFormula xForwardFormula=new DataFormula("-1");
        private DataFormula yForwardFormula=new DataFormula("0");
        private DataFormula zForwardFormula=new DataFormula("-1");
        private DataFormula xUpFormula=new DataFormula("-1");
        private DataFormula yUpFormula=new DataFormula("0");
        private DataFormula zUpFormula=new DataFormula("1");

        private transient double[] camPos;
        private transient double[] forward;
        private transient double[] up;
        private transient double[] right;
        private static final long serialVersionUID = 6959970386412208470L;
    }

    private class CylinderBinner extends DataBinner {
        public CylinderBinner() {}
        public CylinderBinner(CylinderBinner c) {
            super(c);
            xCenterFormula=new DataFormula(c.xCenterFormula);
            yCenterFormula=new DataFormula(c.yCenterFormula);
            radiusFormula=new DataFormula(c.radiusFormula);
        }
        
        @Override
        protected void abstractReset(int s) {
            x=xCenterFormula.valueOf(PhotometryFilter.this,s,0);
            y=yCenterFormula.valueOf(PhotometryFilter.this,s,0);
            r=radiusFormula.valueOf(PhotometryFilter.this,s,0);
        }
        
        @Override
        public void addPath(List<PhotonHit> path) {
            PhotonHit lastHit=path.get(path.size()-1);
            double[] binPos=new double[3];
            for(int i=0; i<axis1Values.length; ++i) {
                binPos[0]=r*Math.cos(axis1Values[i])+x;
                binPos[1]=r*Math.sin(axis1Values[i])+y;
                for(int j=0; j<axis2Values.length; ++j) {
                    binPos[2]=axis2Values[j];
                    Ray toBin=new Ray(lastHit.intersect,binPos);
                    RayFollower rf=new RayFollower(toBin,tree);
                    if(!rf.follow()) {
                        double intensity=lastHit.photon.strength*scatterStyles[scatterStyle].directionalIntensity(lastHit.photon.ray,lastHit.intersect,lastHit.normal,toBin);
                        addPathIntensity(path,intensity);
                        addPathToBin(path,i,j,intensity);
                    }
                }
            }
        }

        @Override
        public int typeIndex() {
            return 1;
        }

        @Override
        protected void addSpecificTabs() {
            JPanel cylPosPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(3,1));
            northPanel.add(xCenterFormula.getLabeledTextField("X Center Formula",null));
            northPanel.add(yCenterFormula.getLabeledTextField("Y Center Formula",null));
            northPanel.add(radiusFormula.getLabeledTextField("Radius Formula",null));
            cylPosPanel.add(northPanel,BorderLayout.NORTH);
            propPanel.addTab("Cylinder Settings",cylPosPanel);            
        }

        @Override
        protected String primaryName() {
            return "Theta";
        }
        
        @Override
        protected String secondaryName() {
            return "Z";
        }
        
        @Override
        public DataBinner copy() {
            return new CylinderBinner(this);
        }
        
        private DataFormula xCenterFormula=new DataFormula("0");
        private DataFormula yCenterFormula=new DataFormula("0");
        private DataFormula radiusFormula=new DataFormula("1");
        
        private transient double x,y,r;
        private static final long serialVersionUID = -9064556326522410442L;
    }

    private class SphereBinner extends DataBinner {
        public SphereBinner() {}
        public SphereBinner(SphereBinner c) {
            super(c);
            xCenterFormula=new DataFormula(c.xCenterFormula);
            yCenterFormula=new DataFormula(c.yCenterFormula);
            zCenterFormula=new DataFormula(c.zCenterFormula);
            radiusFormula=new DataFormula(c.radiusFormula);
        }
        
        @Override
        protected void abstractReset(int s) {
            x=xCenterFormula.valueOf(PhotometryFilter.this,s,0);
            y=yCenterFormula.valueOf(PhotometryFilter.this,s,0);
            z=zCenterFormula.valueOf(PhotometryFilter.this,s,0);
            r=radiusFormula.valueOf(PhotometryFilter.this,s,0);            
        }
        
        @Override
        public void addPath(List<PhotonHit> path) {
            PhotonHit lastHit=path.get(path.size()-1);
            double[] binPos=new double[3];
            for(int i=0; i<axis1Values.length; ++i) {
                double cos1=Math.cos(axis1Values[i]);
                double sin1=Math.sin(axis1Values[i]);
                for(int j=0; j<axis2Values.length; ++j) {
                    double cos2=Math.cos(axis2Values[j]);
                    binPos[0]=r*cos1*cos2+x;
                    binPos[1]=r*sin1*cos2+y;
                    binPos[2]=r*Math.sin(axis2Values[j])+z;
                    Ray toBin=new Ray(lastHit.intersect,binPos);
                    RayFollower rf=new RayFollower(toBin,tree);
                    if(!rf.follow()) {
                        double intensity=lastHit.photon.strength*scatterStyles[scatterStyle].directionalIntensity(lastHit.photon.ray,lastHit.intersect,lastHit.normal,toBin);
                        intensity/=Math.cos(axis2Values[j]);
                        addPathIntensity(path,intensity);
                        addPathToBin(path,i,j,intensity);
                    }
                }
            }
        }

        @Override
        public int typeIndex() {
            return 2;
        }

        @Override
        protected void addSpecificTabs() {
            JPanel spherePosPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(4,1));
            northPanel.add(xCenterFormula.getLabeledTextField("X Center Formula",null));
            northPanel.add(yCenterFormula.getLabeledTextField("Y Center Formula",null));
            northPanel.add(zCenterFormula.getLabeledTextField("Z Center Formula",null));
            northPanel.add(radiusFormula.getLabeledTextField("Radius Formula",null));
            spherePosPanel.add(northPanel,BorderLayout.NORTH);
            propPanel.addTab("Sphere Settings",spherePosPanel);                        
        }

        @Override
        protected String primaryName() {
            return "Theta";
        }
        
        @Override
        protected String secondaryName() {
            return "Phi";
        }
        
        @Override
        public DataBinner copy() {
            return new SphereBinner(this);
        }
        
        private DataFormula xCenterFormula=new DataFormula("0");
        private DataFormula yCenterFormula=new DataFormula("0");
        private DataFormula zCenterFormula=new DataFormula("0");
        private DataFormula radiusFormula=new DataFormula("1");
        
        private transient double x,y,z,r;
        private static final long serialVersionUID = 2746106577235037756L;
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
