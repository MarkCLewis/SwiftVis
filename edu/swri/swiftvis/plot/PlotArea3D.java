/*
 * Created on Jul 28, 2005
 */
package edu.swri.swiftvis.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

//import javax.media.opengl.GLDrawableFactory;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeNode;

import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.plot.p3d.AmbientLight;
import edu.swri.swiftvis.plot.p3d.Camera3D;
import edu.swri.swiftvis.plot.p3d.DirectionalLight;
import edu.swri.swiftvis.plot.p3d.PointLight;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Style3D;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.plot.p3d.j2d.J2DEngine;
//import edu.swri.swiftvis.plot.p3d.jogl.JOGLTestEngine;
import edu.swri.swiftvis.plot.p3d.raytrace.RayTraceEngine;
import edu.swri.swiftvis.util.EditableDouble;

public class PlotArea3D implements PlotObject {
    public PlotArea3D(final PlotSpec spec) {
        parent=spec;
    }
    
    private PlotArea3D(final PlotArea3D c,final PlotSpec spec) {
        parent=spec;
        x.setValue(c.x.getValue());
        y.setValue(c.y.getValue());
        width.setValue(c.width.getValue());
        height.setValue(c.height.getValue());
        camera.setValuesFrom(c.camera,this);
        engine=c.engine;
        for(Style3D style:c.styles) {
            styles.add(style.copy(this));
        }
        ambientLight=new AmbientLight(c.ambientLight.getColor());
        for(PointLight pl:c.pointLights) {
            pointLights.add(new PointLight(pl));
        }
        for(DirectionalLight dl:c.directLights) {
            directLights.add(new DirectionalLight(dl));
        }
    }
    
    @Override
    public String toString() {
        return "Plot Area 3D";
    }

    @Override
    public void draw(final Graphics2D g, final Rectangle2D bounds,final float fontScale) {
        imgX=(int)(bounds.getWidth()*width.getValue()*0.01);
        imgY=(int)(bounds.getHeight()*height.getValue()*0.01);
        if(img==null || imgX!=img.getWidth(null) || imgY!=img.getHeight(null)) {
            useBuffer=false;
        }
        if(!useBuffer || fontScale!=1) {
            redoScene();
            //System.out.println("Drawing to graphics: size="+img.getWidth(parent.getPlot().getSurface())+","+img.getHeight(parent.getPlot().getSurface()));
//            drawToGraphics(imgG,new Rectangle2D.Double(0,0,imgX,imgY),fontScale);
            useBuffer=true;
        }
        imgLeft=x.getValue()*bounds.getWidth()*0.01;
        imgTop=y.getValue()*bounds.getHeight()*0.01;
        g.drawImage(img,AffineTransform.getTranslateInstance(imgLeft,imgTop),parent.getSurface());
//        drawLegendToGraphics(g,new Rectangle2D.Double(bounds.getMinX()+legendX.getValue()*bounds.getWidth()*0.01,
//                bounds.getMinY()+legendY.getValue()*bounds.getHeight()*0.01,legendWidth.getValue()*bounds.getWidth()*0.01,legendHeight.getValue()*bounds.getHeight()*0.01));
    }

    @Override
    public void print(final Graphics2D g, final Rectangle2D bounds) {
        final AffineTransform oldTrans=g.getTransform();
//        drawToGraphics(g,new Rectangle2D.Double(bounds.getMinX()+x.getValue()*bounds.getWidth()*0.01,
//                bounds.getMinY()+y.getValue()*bounds.getHeight()*0.01,width.getValue()*bounds.getWidth()*0.01,height.getValue()*bounds.getHeight()*0.01),1);
        g.setTransform(oldTrans);
//        drawLegendToGraphics(g,new Rectangle2D.Double(bounds.getMinX()+legendX.getValue()*bounds.getWidth()*0.01,
//                bounds.getMinY()+legendY.getValue()*bounds.getHeight()*0.01,legendWidth.getValue()*bounds.getWidth()*0.01,legendHeight.getValue()*bounds.getHeight()*0.01));
        g.setTransform(oldTrans);       
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            final JTabbedPane tabbedPane=new JTabbedPane();
            
            // Data Sets
            final JPanel dataPanel=new JPanel(new BorderLayout());
            JPanel innerPanel = new JPanel(new BorderLayout());
            allDataJList = new JList();
            allDataJList.setListData(styles.toArray());
            allDataJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(final ListSelectionEvent e) {
                    allDataListSelectionMade();
                }
            });
            innerPanel.add(new JScrollPane(allDataJList), BorderLayout.CENTER);
            JPanel innerPanel2 = new JPanel(new GridLayout(1,3));
            JButton button = new JButton("Create");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    createData();
                }
            });
            innerPanel2.add(button);
            button = new JButton("Delete");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    deleteData();
                }
            });
            innerPanel2.add(button);
            button = new JButton("Duplicate");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    duplicateData();
                }
            });
            innerPanel2.add(button);
            innerPanel.add(innerPanel2, BorderLayout.SOUTH);
            dataPanel.add(innerPanel, BorderLayout.NORTH);
            dataPropertiesPanel = new JPanel(new GridLayout(1, 1));
            dataPanel.add(new JScrollPane(dataPropertiesPanel), BorderLayout.CENTER);
            tabbedPane.addTab("Data Sets",dataPanel);
            
            // Engine
            final JPanel enginePanel=new JPanel(new BorderLayout());
            final JPanel centerPanel=new JPanel(new GridLayout(1,1));
            if(engines==null) buildEngines();
            final JComboBox engineBox=new JComboBox(engines);
            engineBox.setSelectedIndex(engine);
            engineBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    engine=engineBox.getSelectedIndex();
                    centerPanel.removeAll();
                    centerPanel.add(new JScrollPane(engines[engine].getPropertiesPanel()),BorderLayout.CENTER);
                    centerPanel.validate();
                    centerPanel.repaint();
                }
            });
            enginePanel.add(engineBox,BorderLayout.NORTH);
            centerPanel.add(engines[engine].getPropertiesPanel(),BorderLayout.CENTER);
            enginePanel.add(new JScrollPane(centerPanel),BorderLayout.CENTER);
            tabbedPane.addTab("Render Engine",enginePanel);
            
            // Lights
            final Box lightPanel=new Box(BoxLayout.Y_AXIS);
            button=new JButton("Ambient Light");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    changeAmbientLight();
                }
            });
            lightPanel.add(button);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.setBorder(BorderFactory.createTitledBorder("Point Lights"));
            final JList pointLightList=new JList(pointLights.toArray());
            innerPanel.add(new JScrollPane(pointLightList),BorderLayout.CENTER);
            innerPanel2=new JPanel(new GridLayout(1,3));
            button=new JButton("Add");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    addPointLight(pointLightList);
                }
            });
            innerPanel2.add(button);
            button=new JButton("Edit");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    editPointLight(pointLightList);
                }
            });
            innerPanel2.add(button);
            button=new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    removePointLight(pointLightList);
                }
            });
            innerPanel2.add(button);
            innerPanel.add(innerPanel2,BorderLayout.SOUTH);
            lightPanel.add(innerPanel);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.setBorder(BorderFactory.createTitledBorder("Directional Lights"));
            final JList directLightList=new JList(directLights.toArray());
            innerPanel.add(new JScrollPane(directLightList),BorderLayout.CENTER);
            innerPanel2=new JPanel(new GridLayout(1,3));
            button=new JButton("Add");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    addDirectionalLight(directLightList);
                }
            });
            innerPanel2.add(button);
            button=new JButton("Edit");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    editDirectionalLight(directLightList);
                }
            });
            innerPanel2.add(button);
            button=new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    removeDirectionalLight(directLightList);
                }
            });
            innerPanel2.add(button);
            innerPanel.add(innerPanel2,BorderLayout.SOUTH);
            lightPanel.add(innerPanel);
            tabbedPane.addTab("Lights",lightPanel);
            
            // Camera
            final JPanel camPanel=new JPanel(new BorderLayout());
            camPanel.add(new JScrollPane(camera.getPropertiesPanel()),BorderLayout.NORTH);
            tabbedPane.addTab("Camera",camPanel);
            
            // Geometry
            final JPanel geomPanel=new JPanel();
            final JPanel northPanel=new JPanel(new GridLayout(4,1));
            northPanel.add(x.getLabeledTextField("X",null));
            northPanel.add(y.getLabeledTextField("Y",null));
            northPanel.add(width.getLabeledTextField("Width",null));
            northPanel.add(height.getLabeledTextField("Height",null));            
            geomPanel.add(northPanel,BorderLayout.NORTH);
            tabbedPane.addTab("Geometry",geomPanel);
            
            button=new JButton("Apply Changes");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    forceRedraw();
                    fireRedraw();
                }
            });
            propPanel.add(button,BorderLayout.SOUTH);
            propPanel.add(tabbedPane,BorderLayout.CENTER);
        }
        return propPanel;
    }

    @Override
    public void fireRedraw() {
        parent.fireRedraw();
    }

    @Override
    public void forceRedraw() {
        useBuffer=false;
        redoScene();
    }

    @Override
    public PlotArea3D copy(final PlotSpec p) {
        return new PlotArea3D(this,p);
    }

    @Override
    public void relink(final Hashtable<GraphElement, GraphElement> linkHash) {
        // TODO if normal listeners are added to this.  I'm not sure how that would work.
//        List<ListenerCellPair> newListeners=new LinkedList<ListenerCellPair>();
//        for(ListenerCellPair lcp:listeners) {
//            GraphElement mapTo=linkHash.get(lcp.pl);
//            if(mapTo!=null) newListeners.add(new ListenerCellPair((PlotListener)mapTo,lcp.cx,lcp.cy));
//        }
//        listeners=newListeners;
    }

    @Override
    public void mousePressed(final MouseEvent e, final double mx, final double my) {
        camera.mousePressed(e,mx,my,getSink());
    }

    @Override
    public void mouseReleased(final MouseEvent e, final double mx, final double my) {
        camera.mouseReleased(e,mx,my,getSink());
    }

    @Override
    public void mouseClicked(final MouseEvent e, final double mx, final double my) {
        camera.mouseClicked(e,mx,my,getSink());
    }

    @Override
    public void mouseMoved(final MouseEvent e, final double mx, final double my) {
        camera.mouseMoved(e,mx,my,getSink());
    }

    @Override
    public void mouseDragged(final MouseEvent e, final double mx, final double my) {
        camera.mouseDragged(e,mx,my,getSink());
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        camera.keyPressed(e,getSink());
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        camera.keyReleased(e,getSink());
    }

    @Override
    public void keyTyped(final KeyEvent e) {
        camera.keyTyped(e,getSink());
    }

    @Override
    public TreeNode getChildAt(final int childIndex) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(final TreeNode node) {
        return -1;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Enumeration<TreeNode> children() {
        return null;
    }
    
    /**
     * This method will be called by the engine after it has created a new image.
     * @param newImg
     */
    public void updateImage(final Image newImg) {
        img=newImg;
        fireRedraw();
    }
    
    public Camera3D getCamera() {
        return camera;
    }
    
    public Plot getSink() {
        return parent.getPlot();
    }
    
    public void cameraMoved() {
        if(engines==null) buildEngines();
        engines[engine].cameraMoved();
        fireRedraw();
    }
    
    private void redoScene() {
        if(imgX<=0 || imgY<=0) return;
        useBuffer=true;
        if(engines==null) buildEngines();
        engines[engine].clearScene();
        for(final Style3D style:styles) {
            style.renderToEngine(engines[engine]);
        }
        engines[engine].setAmbientLight(ambientLight);
        for(final PointLight pl:pointLights) engines[engine].addPointLight(pl);
        for(final DirectionalLight dl:directLights) engines[engine].addDirectionalLight(dl);
        img=engines[engine].sceneComplete(imgX,imgY);        
    }
    
    private void allDataListSelectionMade() {
        final int which=allDataJList.getSelectedIndex();
        dataPropertiesPanel.removeAll();
        if(which>=0) dataPropertiesPanel.add(styles.get(which).getPropertiesPanel());
        dataPropertiesPanel.validate();
        dataPropertiesPanel.repaint();
    }

    private void createData() {
        final Class<Style3D> dataClass=OptionsData.instance().selectPlot3DType(allDataJList);
        if(dataClass==null) return;
        try {
            final Object[] constArgs={this};
            final Class<?>[] constClasses={this.getClass()};
            final Constructor<Style3D> constructor=dataClass.getConstructor(constClasses);
            styles.add(constructor.newInstance(constArgs));
            allDataJList.setListData(styles.toArray());
            allDataJList.setSelectedIndex(styles.size()-1);
        } catch(final ClassCastException e) {
            JOptionPane.showMessageDialog(allDataJList,"The selected class is not of the proper type.  "+dataClass.getName()+" is not a DataPlotStyle.","Incorrect Type",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class is not a DataPlotStyle: "+dataClass.getName());
            e.printStackTrace();
        } catch(final InstantiationException e) {
            JOptionPane.showMessageDialog(allDataJList,"The selected class does not have the proper constructor.  See Documentation.","Constructor Missing",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class does not have the proper constructor.  See Documentation.");
            e.printStackTrace();
        } catch(final NoSuchMethodException e) {
            JOptionPane.showMessageDialog(allDataJList,"The selected class does not have the proper constructor.  See Documentation.","Constructor Missing",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class does not have the proper constructor.  See Documentation.");
            e.printStackTrace();
        } catch(final InvocationTargetException e) {
            JOptionPane.showMessageDialog(allDataJList,"The constructor threw an exception.","Exception in Constructor",JOptionPane.ERROR_MESSAGE);
            System.err.println("The constructor for the selected class threw an exception.");
            e.printStackTrace();
            System.err.println("Was wrapped around.");
            e.getTargetException().printStackTrace();
        } catch(final IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void deleteData() {
        if(styles.size()<=1) {
            JOptionPane.showMessageDialog(allDataJList,"You can't delete the last data plot.");
            return;
        }
        final int which=allDataJList.getSelectedIndex();
        if(which<0) return;
        styles.remove(which);
    }

    private void duplicateData() {
        final int which=allDataJList.getSelectedIndex();
        if(which<0) return;
        styles.add(styles.get(which).copy(this));
        allDataJList.setListData(styles.toArray());
        allDataJList.setSelectedIndex(styles.size()-1);
        dataPropertiesPanel.removeAll();
        dataPropertiesPanel.add(styles.get(styles.size()-1).getPropertiesPanel());
    }
    
    private void changeAmbientLight() {
        final Color c=JColorChooser.showDialog(propPanel, "Ambient Color", ambientLight.getColor());
        if(c!=null) ambientLight=new AmbientLight(c);
    }
    
    private void addPointLight(final JList lightList) {
        pointLights.add(new PointLight(new Vect3D(0,0,0),Color.white));
        lightList.setListData(pointLights.toArray());
        lightList.getParent().repaint();
    }

    private void editPointLight(final JList lightList) {
        final int index=lightList.getSelectedIndex();
        if(index>=0) {
            pointLights.get(index).edit(propPanel);
            lightList.setListData(pointLights.toArray());
            lightList.getParent().repaint();
        }
    }

    private void removePointLight(final JList lightList) {
        final int index=lightList.getSelectedIndex();
        if(index>=0) {
            pointLights.remove(index);
            lightList.setListData(pointLights.toArray());
            lightList.getParent().repaint();
        }
    }

    private void addDirectionalLight(final JList lightList) {
        directLights.add(new DirectionalLight(new Vect3D(0,0,-1),Color.white));
        lightList.setListData(directLights.toArray());
        lightList.getParent().repaint();
    }

    private void editDirectionalLight(final JList lightList) {
        final int index=lightList.getSelectedIndex();
        if(index>=0) {
            directLights.get(index).edit(propPanel);
            lightList.setListData(directLights.toArray());
            lightList.getParent().repaint();
        }
    }

    private void removeDirectionalLight(final JList lightList) {
        final int index=lightList.getSelectedIndex();
        if(index>=0) {
            directLights.remove(index);
            lightList.setListData(directLights.toArray());
            lightList.getParent().repaint();
        }
    }
    
    private void buildEngines() {
        final List<RenderEngine> lre=new ArrayList<RenderEngine>();
        lre.add(new J2DEngine(this));
        lre.add(new RayTraceEngine(this));
        // TODO check if we have Java3D and add engine if we do.
        // TODO check if we have JOGL and add engine if we do.
        // lre.add(new JOGLTestEngine(this));
//        try {
////        	GLDrawableFactory.getFactory();
////        	lre.add(new JOGLTestEngine(this));
//        } catch (NoClassDefFoundError e) {
//        	System.out.println("No GL classes present.");
//        }
        	
        
        engines=new RenderEngine[lre.size()];
        engines=lre.toArray(engines);
    }

    private PlotSpec parent;
    
    private final EditableDouble x=new EditableDouble(0.0);
    private final EditableDouble y=new EditableDouble(0.0);
    private final EditableDouble width=new EditableDouble(100.0);
    private final EditableDouble height=new EditableDouble(100.0);
    
    private final Camera3D camera=new Camera3D(this);
    
    private int engine;
    private final List<Style3D> styles=new ArrayList<Style3D>();
    
    private final List<PointLight> pointLights=new ArrayList<PointLight>();
    private final List<DirectionalLight> directLights=new ArrayList<DirectionalLight>();
    private AmbientLight ambientLight=new AmbientLight(new Color(0,0,0));

    private transient boolean useBuffer=false;
    private transient RenderEngine[] engines;
    private transient JPanel propPanel;
    private transient JList allDataJList;
    private transient JPanel dataPropertiesPanel;
    private transient Image img;
    private transient int imgX;
    private transient int imgY;
    private transient double imgLeft,imgTop;

    private static final long serialVersionUID=-234623987234764368l;
}
