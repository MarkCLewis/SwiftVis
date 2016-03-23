/*
 * Created on Jul 8, 2005
 */
package edu.swri.swiftvis.filters;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.PlotListener;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.ReduceLoopBody;
import edu.swri.swiftvis.util.ThreadHandler;

/**
 * This is a listener that applies transforms to some of the elements based on some formulas.
 * It is also a listener and takes input from the user as to where the click or drag on the plot.
 * To make it easier for people to use, I'm putting in "wizard" tabs that will automatically set
 * formulas for rotating (and possibly translating) points.  If they set things in the wizard and
 * click to apply wizard settings, it will update the actual values in another tab where users have
 * full control over the transforms that are used, how many things are transformed, etc.
 * 
 *  In order to make this work, it has its own private source type that will provide 5 different
 *  values and act as the second input to this filter.  Those values are the x and y click location
 *  (d[1].v[0], d[1].v[1]), the compiled rotation angle (d[1].v[2]), and the compiled linear drag in
 *  x and y (d[1].v[3], d[1].v[4]).
 * 
 * @author Mark Lewis
 */
public class RotationFilter extends AbstractSingleSourceFilter implements PlotListener {
    public RotationFilter() {}
    
    public RotationFilter(RotationFilter c,List<GraphElement> l) {
        super(c,l);
        x=new EditableDouble(c.x.getValue());
        y=new EditableDouble(c.y.getValue());
        ang=new EditableDouble(c.ang.getValue());
        dx=new EditableDouble(c.dx.getValue());
        dy=new EditableDouble(c.dy.getValue());
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        xValue=c.xValue;
        yValue=c.yValue;
        axisThickness=new EditableDouble(c.axisThickness.getValue());
        vars=new HashMap<String, Double>(c.vars);
        axisSource=new AxisSource(c.axisSource);
    }
    
    @Override
    protected boolean doingInThreads() {
		return true;
	}
    
    @Override
    protected void redoAllElements() {
        setVars();
        double minx1=0.0,miny1=0.0,maxx1=0.0,maxy1=0.0;
        final int[] indexRange=xFormula.getSafeElementRange(this, 0);
        int[] tmp=yFormula.getSafeElementRange(this, 0);
        if(tmp[0]>indexRange[0]) indexRange[0]=tmp[0];
        if(tmp[1]<indexRange[1]) indexRange[1]=tmp[1];
        DataFormula.checkRangeSafety(indexRange,this);
        minx=new double[ThreadHandler.instance().getNumThreads()];
        miny=new double[ThreadHandler.instance().getNumThreads()];
        maxx=new double[ThreadHandler.instance().getNumThreads()];
        maxy=new double[ThreadHandler.instance().getNumThreads()];
        
        //  parallel
        for (int i=indexRange[0]; i<indexRange[1]; i++) {
        	dataVect.get(0).add(null);
        }
        ReduceLoopBody[] loops=new ReduceLoopBody[ThreadHandler.instance().getNumThreads()];
        for (int i=0; i<loops.length; i++) {
        	final int index=i;
        	loops[i]=new ReduceLoopBody() {
        		@Override
                public void execute(int start,int end) {
        			DataFormula xForm=xFormula.getParallelCopy();
        			DataFormula yForm=yFormula.getParallelCopy();
                    float[] vals=new float[input.getNumValues(0)];
        			for (int j=start; j<end; j++) {
        	    		DataElement de=input.getElement(j, 0);
        	            for(int k=0; k<vals.length; ++k) {
        	                vals[k]=de.getValue(k);
        	            }
        	            if(j==indexRange[0] || vals[xValue]<minx[index]) {
        	                minx[index]=vals[xValue];
        	            }
        	            if(j==indexRange[0] || vals[xValue]>maxx[index]) {
        	                maxx[index]=vals[xValue];
        	            }
        	            if(j==indexRange[0] || vals[yValue]<miny[index]) {
        	                miny[index]=vals[yValue];
        	            }
        	            if(j==indexRange[0] || vals[yValue]>maxy[index]) {
        	                maxy[index]=vals[yValue];
        	            }
        	            vals[xValue]=(float)xForm.valueOf(RotationFilter.this,0,j,null,vars);
        	            vals[yValue]=(float)yForm.valueOf(RotationFilter.this,0,j,null,vars);
        	            dataVect.get(0).set(j,DataElement.replaceValues(de,vals));
        			}
        		}
        	};
        }
        ThreadHandler.instance().chunkedForLoop(this,indexRange[0],indexRange[1],loops);
        minx1=minx[0];
        for (int i=1; i<minx.length; i++) {
        	if (minx[i]<minx1) minx1=minx[i];
        }
        maxx1=maxx[0];
        for (int i=1; i<maxx.length; i++) {
        	if (maxx[i]>maxx1) maxx1=maxx[i];
        }
        miny1=miny[0];
        for (int i=1; i<miny.length; i++) {
        	if (miny[i]<miny1) miny1=miny[i];
        }
        maxy1=maxy[0];
        for (int i=1; i<maxy.length; i++) {
        	if (maxy[i]>maxy1) maxy1=maxy[i];
        }
        axisSource.redoElements(indexRange,minx1,maxx1,miny1,maxy1);
    }

    @Override
    protected void setupSpecificPanelProperties() {
        EditableDouble.Listener edl=new EditableDouble.Listener() {
            @Override
            public void valueChanged() {
                setVars();
            }
        };
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(15,1));
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Primary Point (x)"),BorderLayout.WEST);
        innerPanel.add(x.getTextField(edl),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Secondary Point (y)"),BorderLayout.WEST);
        innerPanel.add(y.getTextField(edl),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Angle [radians] (ang)"),BorderLayout.WEST);
        innerPanel.add(ang.getTextField(edl),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Axis Ratio"),BorderLayout.WEST);
        innerPanel.add(axisRatio.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Primary Offset (dx)"),BorderLayout.WEST);
        innerPanel.add(dx.getTextField(edl),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Secondary Offset (dy)"),BorderLayout.WEST);
        innerPanel.add(dy.getTextField(edl),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        JButton button=new JButton("Set Formulas for Rotation");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRotationFormulas();
            }
        });
        northPanel.add(button);
        button=new JButton("Set Formulas for Translation");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setTranslationFormulas();
            }
        });
        northPanel.add(button);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Primary Formula"),BorderLayout.WEST);
        innerPanel.add(xFormula.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Secondary Formula"),BorderLayout.WEST);
        innerPanel.add(yFormula.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Primary Value"),BorderLayout.WEST);
        String[] xValNames;
        if(input!=null) {
            xValNames=new String[input.getNumValues(0)];
            for(int i=0; i<input.getNumValues(0); ++i) xValNames[i]=input.getValueDescription(0, i);
        } else {
            xValNames=new String[0];
        }
        xValueBox=new JComboBox(xValNames);
        if(xValNames.length>0) xValueBox.setSelectedIndex(xValue);
        xValueBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xValue=xValueBox.getSelectedIndex();
            }
        });
        innerPanel.add(xValueBox,BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Secondary Value"),BorderLayout.WEST);
        String[] yValNames;
        if(input!=null) {
            yValNames=new String[input.getNumValues(0)];
            for(int i=0; i<input.getNumValues(0); ++i) yValNames[i]=input.getValueDescription(0, i);
        } else {
            yValNames=new String[0];
        }
        yValueBox=new JComboBox(yValNames);
        yValueBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                yValue=yValueBox.getSelectedIndex();
            }
        });
        if(yValNames.length>0) yValueBox.setSelectedIndex(yValue);
        innerPanel.add(yValueBox,BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Drawn Axis Thickness"),BorderLayout.WEST);
        innerPanel.add(axisThickness.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        northPanel.add(button);
        panel.add(northPanel,BorderLayout.NORTH);
        innerPanel=new JPanel(new GridLayout(1,2));
        v1Label=new JLabel("0.0");
        innerPanel.add(v1Label);
        v2Label=new JLabel("0.0");
        innerPanel.add(v2Label);
        panel.add(innerPanel,BorderLayout.SOUTH);
        propPanel.add("Settings",panel);
    }
    
    @Override
    public DataSource getSource(int which) {
        return input;
    }
    
    @Override
    public void addInput(DataSource in) {
        super.addInput(in);
        xValue=0;
        yValue=1;
        if(xValueBox!=null) {
            xValueBox.removeAllItems();
            for(int i=0; i<input.getNumValues(0); ++i) xValueBox.addItem(input.getValueDescription(0, i));
            if(xValueBox.getItemCount()>xValue) xValueBox.setSelectedIndex(xValue);
            yValueBox.removeAllItems();
            for(int i=0; i<input.getNumValues(0); ++i) yValueBox.addItem(input.getValueDescription(0, i));
            if(yValueBox.getItemCount()>yValue) yValueBox.setSelectedIndex(yValue);
        }
    }

    @Override
    public void mousePressed(double v1, double v2, MouseEvent e) {
        startx=v1;
        starty=v2;
    }

    @Override
    public void mouseReleased(double v1, double v2, MouseEvent e) {
        if(v1!=startx || v2!=starty) {
            dx.setValue(dx.getValue()+(v1-startx));
            dy.setValue(dy.getValue()+(v2-starty));
            double sang=Math.atan2(starty-y.getValue(),startx-x.getValue());
            double fang=Math.atan2(v2-y.getValue(),v1-x.getValue());
            ang.setValue(ang.getValue()+(fang-sang));
            setVars();
            abstractRedoAllElements();
        }
    }

    @Override
    public void mouseClicked(double v1, double v2, MouseEvent e) {
        if(v1==startx && v2==starty && e.getButton()!=MouseEvent.BUTTON1) {
            x.setValue(v1);
            y.setValue(v2);
            setVars();
            abstractRedoAllElements();
        }
    }

    @Override
    public void mouseMoved(double v1, double v2, MouseEvent e) {
        if(v1Label!=null) {
            v1Label.setText(Double.toString(v1));
            v2Label.setText(Double.toString(v2));
        }
    }

    @Override
    public void mouseDragged(double v1, double v2, MouseEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public Shape getSelectionRegion(PlotTransform trans) {
        AxisSink as=new AxisSink();
        GeneralPath ret=new GeneralPath();
        BasicStroke stroke=new BasicStroke((float)axisThickness.getValue());
        Point2D p1=trans.transform(xFormula.valueOf(as,0,0,null,vars),yFormula.valueOf(as,0,0,null,vars));
        Point2D p2=trans.transform(xFormula.valueOf(as,0,1,null,vars),yFormula.valueOf(as,0,1,null,vars));
        ret.append(stroke.createStrokedShape(new Line2D.Double(p1,p2)),false);
        p1=trans.transform(xFormula.valueOf(as,0,2,null,vars),yFormula.valueOf(as,0,2,null,vars));
        p2=trans.transform(xFormula.valueOf(as,0,3,null,vars),yFormula.valueOf(as,0,3,null,vars));
        ret.append(stroke.createStrokedShape(new Line2D.Double(p1,p2)),false);
        return ret;
    }

    public static String getTypeDescription() {
        return "Rotation Filter (Listener)";
    }
    
    @Override
    public String getDescription() {
        return "Rotation Filter (Listener)";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new RotationFilter(this,l);
    }

    @Override
    public int getNumParameters(int stream) {
        if(input==null) return 0;
        return input.getNumParameters(0);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return input.getParameterDescription(0, which);
    }

    @Override
    public int getNumValues(int stream) {
        if(input==null) return 0;
        return input.getNumValues(0);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(which==xValue && which==yValue) return "Transformed "+input.getValueDescription(0, which);
        return input.getValueDescription(0, which);
    }
    
    private void setRotationFormulas() {
        xFormula.setFormula("(v["+xValue+"]-x)*cos(ang)-"+axisRatio.getValue()+"*(v["+yValue+"]-y)*sin(ang)+x");
        yFormula.setFormula("(v["+yValue+"]-y)*cos(ang)+(v["+xValue+"]-x)*sin(ang)+y/"+axisRatio.getValue());
    }
    
    private void setTranslationFormulas() {
        xFormula.setFormula("v["+xValue+"]+dx");
        yFormula.setFormula("v["+yValue+"]+dy");
    }
    
    private void setVars() {
        vars.put("x",x.getValue());
        vars.put("y",y.getValue());
        vars.put("ang",ang.getValue());
        vars.put("dx",dx.getValue());
        vars.put("dy",dy.getValue());
    }
    
    private EditableDouble x=new EditableDouble(0.0);
    private EditableDouble y=new EditableDouble(0.0);
    private EditableDouble ang=new EditableDouble(0.0);
    private EditableDouble axisRatio=new EditableDouble(1.0);
    private EditableDouble dx=new EditableDouble(0.0);
    private EditableDouble dy=new EditableDouble(0.0);
    private DataFormula xFormula=new DataFormula("(v[0]-x)*cos(ang)-(v[1]-y)*sin(ang)+x");
    private DataFormula yFormula=new DataFormula("(v[1]-y)*cos(ang)+(v[0]-y)*sin(ang)+y");
    private int xValue=0;
    private int yValue=1;
    private double[] maxx,minx,maxy,miny;
    private EditableDouble axisThickness=new EditableDouble(1);
    
    private HashMap<String,Double> vars=new HashMap<String, Double>();
    private AxisSource axisSource=new AxisSource();
    
    private transient JComboBox xValueBox;
    private transient JComboBox yValueBox;
    private transient double startx,starty;
    private transient JLabel v1Label;
    private transient JLabel v2Label;
    
    private static final long serialVersionUID=5296807295862375l;
    
    private class AxisSource implements DataSource {
        public AxisSource() {}
        public AxisSource(AxisSource c) {
            for(int i=0; i<elements.length; ++i) elements[i]=c.elements[i];
        }
        @Override
        public void addOutput(DataSink sink) {}
        @Override
        public void removeOutput(DataSink sink) {}
        @Override
        public int getNumOutputs() {
            return RotationFilter.this.getNumOutputs(); 
        }
        @Override
        public DataSink getOutput(int which) {
            return RotationFilter.this.getOutput(which);
        }
        @Override
        public int getNumElements(int stream) {
            return 4;
        }
        @Override
        public DataElement getElement(int i, int stream) {
            return elements[i];
        }
        @Override
        public int getNumStreams() {
            return 1;
        }
        @Override
        public int getNumParameters(int stream) {
            return elements[0].getNumParams();
        }
        @Override
        public String getParameterDescription(int stream, int which) { return null; }
        @Override
        public int getNumValues(int stream) {
            return elements[0].getNumValues();
        }
        @Override
        public void redo() { /* TODO implement? */ }
        @Override
        public String getValueDescription(int stream, int which) { return null; }
        @Override
        public String getDescription() { return null; }
        @Override
        public Rectangle getBounds() { return null; }
        @Override
        public void translate(int dx,int dy) {}
        @Override
        public void drawNode(Graphics2D g) {}
        @Override
        public JComponent getPropertiesPanel() { return null; }
        @Override
        public GraphElement copy(List<GraphElement> l) { return null; }
        @Override
        public void relink(Hashtable<GraphElement, GraphElement> linkHash) {}
        @Override
        public void clearData() {}
        private void redoElements(int[] indexRange,double minx,double maxx,double miny,double maxy) {
            DataElement de=input.getElement(indexRange[0], 0);
            float[] vals=new float[input.getNumValues(0)];
            for(int j=0; j<vals.length; ++j) {
                vals[j]=de.getValue(j);
            }
            vals[xValue]=(float)minx;
            vals[yValue]=0;
            elements[0]=DataElement.replaceValues(de,vals);
            vals[xValue]=(float)maxx;
            vals[yValue]=0;
            elements[1]=DataElement.replaceValues(de,vals);
            vals[xValue]=0;
            vals[yValue]=(float)miny;
            elements[2]=DataElement.replaceValues(de,vals);
            vals[xValue]=0;
            vals[yValue]=(float)maxy;
            elements[3]=DataElement.replaceValues(de,vals);
        }

        private DataElement[] elements=new DataElement[4];
        private static final long serialVersionUID=57348632457458l;
    }
    
    private class AxisSink implements DataSink {
        @Override
        public boolean validInput(DataSource ds) {
            return false;
        }
        @Override
        public void addInput(DataSource input) {
        }
        @Override
        public void removeInput(DataSource input) {
        }
        @Override
        public void moveUpInput(int index) {
        }
        @Override
        public int getNumSources() {
            return 2;
        }
        @Override
        public void redo() { /* TODO implement? */ }
        @Override
        public DataSource getSource(int which) {
            return axisSource;
        }
        @Override
        public void sourceAltered(DataSource source) {
        }
        @Override
        public String getDescription() {
            return null;
        }
        @Override
        public Rectangle getBounds() {
            return null;
        }
        @Override
        public void translate(int dx,int dy) {
        }
        @Override
        public void drawNode(Graphics2D g) {
        }
        @Override
        public JComponent getPropertiesPanel() {
            return null;
        }
        @Override
        public GraphElement copy(List<GraphElement> l) {
            return null;
        }
        @Override
        public void relink(Hashtable<GraphElement, GraphElement> linkHash) {}
        @Override
        public void clearData() {}
        
        private static final long serialVersionUID=72323540988346837l;
    }
}
