/*
 * Created on Dec 8, 2006
 */
package edu.swri.swiftvis.plot.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;

/**
 * This class represents a general model of producing colors.  It encapsulates
 * multiple different ways of producing colors and handles the GUIs for them.
 * The various methods of producing colors are listed below.
 * 
 * Gradient (includes a formula for alpha)
 * Solid
 * ARGB composite
 * 
 * The GUI will use 5 slots in a JPanel with a GridLayout.  What is in those slots
 * will vary depending on what option is being used.  Other methods of producing
 * colors can be added in later as needed.  Nothing else in the code will need to
 * change as long the GUIs don't exceed 5 slots.   
 * 
 * @author Mark Lewis
 */
public class ColorModel implements Serializable {
    public ColorModel() {}
    
    public ColorModel(ColorModel c) {
        currentModel=c.currentModel;
        model=c.model.copy();
    }
    
    public ColorModel(int type,Object... init) {
        currentModel=type;
        model=modelCreators[currentModel].createModel();
        model.init(init);
    }
    
    public void addGUIToPanel(String title,JPanel panel) {
        panels=new JPanel[5];
        panels[0]=new JPanel(new BorderLayout());
        for(int i=1; i<panels.length; ++i) {
            panels[i]=new JPanel(new GridLayout(1,1));
        }
        panels[0].add(new JLabel(title),BorderLayout.WEST);
        final JComboBox comboBox=new JComboBox(modelCreators);
        comboBox.setSelectedIndex(currentModel);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(comboBox.getSelectedIndex()!=currentModel) {
                    currentModel=comboBox.getSelectedIndex();
                    model=modelCreators[currentModel].createModel();
                    for(int i=1; i<panels.length; ++i) {
                        panels[i].removeAll();
                    }
                    model.setupGUI(panels);
                    for(int i=1; i<panels.length; ++i) {
                        panels[i].validate();
                        panels[i].repaint();
                    }
                }
            }
        });
        panels[0].add(comboBox,BorderLayout.CENTER);
        model.setupGUI(panels);
        for(int i=0; i<panels.length; ++i) {
            panel.add(panels[i]);
        }
    }
    
    public void addGUIToBox(String title,Box box) {
        JPanel p=new JPanel(new GridLayout(5,1));
        addGUIToPanel(title,p);
        box.add(p);
    }
    
    public Color getColor(DataSink sink,int index) {
        return model.getColor(sink,index);
    }
    
    public int[] getSafeElementRange(DataSink sink) {
        return model.getSafeElementRange(sink);
    }
    
    public ColorGradient getGradient() {
        if(model instanceof GradientModel) {
            return ((GradientModel)model).cg;
        }
        return null;
    }
    
    public void clearGroupSelection() {
        model.clearGroupSelection();
    }
    
    public void mapSources(Map<Integer,Integer> newSources) {
        if(newSources.isEmpty()) return;
        model.mapSources(newSources);
    }

    
    private static ModelCreator modelCreators[]={new GradientCreator(),new SolidCreator(),new ARGBCreator()};
    
    private int currentModel=0;
    private SpecificModel model=modelCreators[currentModel].createModel();
    private transient JPanel[] panels=null;
    
    private static final long serialVersionUID = -7184341046080890070L;
    public static final int GRADIENT_TYPE=0;
    public static final int SOLID_TYPE=0;
    public static final int ARGB_TYPE=0;
    
    private static interface ModelCreator {
        SpecificModel createModel();
    }
    
    private static class GradientCreator implements ModelCreator {
        @Override
        public SpecificModel createModel() {
            return new GradientModel();
        }
        @Override
        public String toString() { return "Gradient Color Model"; }
    }
    
    private static class SolidCreator implements ModelCreator {
        @Override
        public SpecificModel createModel() {
            return new SolidModel();
        }
        @Override
        public String toString() { return "Solid Color Model"; }
    }
    
    private static class ARGBCreator implements ModelCreator {
        @Override
        public SpecificModel createModel() {
            return new ARGBModel();
        }
        @Override
        public String toString() { return "ARGB Color Model"; }
    }
    
    private static interface SpecificModel extends Serializable {
        Color getColor(DataSink sink,int index);
        int[] getSafeElementRange(DataSink sink);
        void setupGUI(JPanel[] panels);
        SpecificModel copy();
        void init(Object[] initVals);
        void clearGroupSelection();
        void mapSources(Map<Integer,Integer> newSources);
    }

    private static class GradientModel implements SpecificModel {
        @Override
        public Color getColor(DataSink sink, int index) {
            return cg.getColor(gradFormula.valueOf(sink,0, index),(float)alphaFormula.valueOf(sink,0, index));
        }
        
        @Override
        public int[] getSafeElementRange(DataSink sink) {
            int[] r=gradFormula.getSafeElementRange(sink, 0);
            DataFormula.mergeSafeElementRanges(r,alphaFormula.getSafeElementRange(sink, 0));
            DataFormula.checkRangeSafety(r,sink);
            return r;
        }

        @Override
        public void setupGUI(JPanel[] panels) {
            panels[1].add(gradFormula.getLabeledTextField("Color Formula",null));
            JButton button=new JButton("Change Gradient");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cg.edit();
                }
            });
            panels[2].add(button);
            panels[3].add(alphaFormula.getLabeledTextField("Alpha Formula",null));
        }
        
        @Override
        public GradientModel copy() {
            GradientModel ret=new GradientModel();
            ret.gradFormula=new DataFormula(gradFormula);
            ret.cg=new ColorGradient(cg);
            ret.alphaFormula=new DataFormula(alphaFormula);
            return ret;
        }
        
        @Override
        public void init(Object[] initVals) {
            int stringCount=0;
            for(int i=0; i<initVals.length; ++i) {
                if(initVals[i] instanceof String) {
                    if(stringCount==0) gradFormula=new DataFormula((String)initVals[i]);
                    else if(stringCount==1) alphaFormula=new DataFormula((String)initVals[i]);
                    stringCount++;
                }
                if(initVals[i] instanceof ColorGradient) {
                    cg=(ColorGradient)initVals[i];
                }
            }
        }
        
        @Override
        public void clearGroupSelection() {
            gradFormula.clearGroupSelection();
            alphaFormula.clearGroupSelection();
        }

        @Override
        public void mapSources(Map<Integer,Integer> newSources) {
            gradFormula.mapSources(newSources);
            alphaFormula.mapSources(newSources);
        }
        
        private DataFormula gradFormula=new DataFormula("0");
        private ColorGradient cg=new ColorGradient();
        private DataFormula alphaFormula=new DataFormula("1");

        private static final long serialVersionUID = 1695748357527414134L;
    }
    
    private static class SolidModel implements SpecificModel {
        @Override
        public Color getColor(DataSink sink, int index) {
            return color;
        }

        @Override
        public int[] getSafeElementRange(DataSink sink) {
            return new int[]{0,Integer.MAX_VALUE};
        }

        @Override
        public void setupGUI(JPanel[] panels) {
            final JButton button=new JButton("Change Gradient");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Color nc=JColorChooser.showDialog(button,"Solid Color",color);
                    if(nc!=null) color=nc;
                }
            });
            panels[1].add(button);
        }

        @Override
        public SolidModel copy() {
            SolidModel ret=new SolidModel();
            ret.color=color;
            return ret;            
        }

        @Override
        public void init(Object[] initVals) {
            for(int i=0; i<initVals.length; ++i) {
                if(initVals[i] instanceof Color) {
                    color=(Color)initVals[i];
                }
            }
        }

        @Override
        public void clearGroupSelection() {
        }

        @Override
        public void mapSources(Map<Integer,Integer> newSources) {}
        
        private Color color=Color.BLACK;

        private static final long serialVersionUID = -2018896226277965906L;
    }

    private static class ARGBModel implements SpecificModel {
        @Override
        public Color getColor(DataSink sink, int index) {
            float r=(float)redFormula.valueOf(sink,0, index);
            float g=(float)greenFormula.valueOf(sink,0, index);
            float b=(float)blueFormula.valueOf(sink,0, index);
            float a=(float)alphaFormula.valueOf(sink,0, index);
            if(r<0.0) r=0.0f;
            if(r>1.0) r=1.0f;
            if(g<0.0) g=0.0f;
            if(g>1.0) g=1.0f;
            if(b<0.0) b=0.0f;
            if(b>1.0) b=1.0f;
            if(a<0.0) a=0.0f;
            if(a>1.0) a=1.0f;
            return new Color(r,g,b,a);
        }

        @Override
        public int[] getSafeElementRange(DataSink sink) {
            int[] range=redFormula.getSafeElementRange(sink, 0);
            DataFormula.mergeSafeElementRanges(range,greenFormula.getSafeElementRange(sink, 0));
            DataFormula.mergeSafeElementRanges(range,blueFormula.getSafeElementRange(sink, 0));
            DataFormula.mergeSafeElementRanges(range,alphaFormula.getSafeElementRange(sink, 0));
            DataFormula.checkRangeSafety(range,sink);
            return range;
        }

        @Override
        public void setupGUI(JPanel[] panels) {
            panels[1].add(redFormula.getLabeledTextField("Red Formula",null));
            panels[2].add(greenFormula.getLabeledTextField("Green Formula",null));
            panels[3].add(blueFormula.getLabeledTextField("Blue Formula",null));
            panels[4].add(alphaFormula.getLabeledTextField("Alpha Formula",null));
        }
        
        @Override
        public ARGBModel copy() {
            ARGBModel ret=new ARGBModel();
            ret.redFormula=new DataFormula(redFormula);
            ret.greenFormula=new DataFormula(greenFormula);
            ret.blueFormula=new DataFormula(blueFormula);
            ret.alphaFormula=new DataFormula(alphaFormula);
            return ret;            
        }

        @Override
        public void init(Object[] initVals) {
            int stringCount=0;
            for(int i=0; i<initVals.length; ++i) {
                if(initVals[i] instanceof String) {
                    if(stringCount==0) redFormula=new DataFormula((String)initVals[i]);
                    else if(stringCount==1) greenFormula=new DataFormula((String)initVals[i]);
                    else if(stringCount==2) blueFormula=new DataFormula((String)initVals[i]);
                    else if(stringCount==3) alphaFormula=new DataFormula((String)initVals[i]);
                    stringCount++;
                }
            }            
        }

        @Override
        public void clearGroupSelection() {
            redFormula.clearGroupSelection();
            greenFormula.clearGroupSelection();
            blueFormula.clearGroupSelection();
            alphaFormula.clearGroupSelection();
        }

        @Override
        public void mapSources(Map<Integer,Integer> newSources) {
            redFormula.mapSources(newSources);
            greenFormula.mapSources(newSources);
            blueFormula.mapSources(newSources);
            alphaFormula.mapSources(newSources);
        }
        
        private DataFormula redFormula=new DataFormula("0");
        private DataFormula greenFormula=new DataFormula("0");
        private DataFormula blueFormula=new DataFormula("0");
        private DataFormula alphaFormula=new DataFormula("1");

        private static final long serialVersionUID = 5763363235202464019L;
    }
}
