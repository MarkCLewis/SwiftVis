/*
 * Created on Jul 12, 2008
 */
package edu.swri.swiftvis.plot.p3d.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.plot.Plot;
import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Style3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.plot.util.ColorModel;
import edu.swri.swiftvis.util.EditableString;

public class PolyhedraStyle3D implements Style3D {

    public PolyhedraStyle3D(PlotArea3D pa) {
        plotArea=pa;
    }

    public PolyhedraStyle3D(PolyhedraStyle3D c,PlotArea3D pa) {
        plotArea=pa;
        name.setValue(c.name.getValue());
        xFormula.setFormula(c.xFormula.getFormula());
        yFormula.setFormula(c.yFormula.getFormula());
        zFormula.setFormula(c.zFormula.getFormula());
        sizeFormula.setFormula(c.sizeFormula.getFormula());
        xOrientFormula.setFormula(c.xFormula.getFormula());
        yOrientFormula.setFormula(c.yFormula.getFormula());
        zOrientFormula.setFormula(c.zFormula.getFormula());
        opacityFormula.setFormula(c.opacityFormula.getFormula());
        reflectFormula.setFormula(c.reflectFormula.getFormula());
        colorModel=new ColorModel(c.colorModel);
    }

    @Override
    public Style3D copy(PlotArea3D c) {
        return new PolyhedraStyle3D(this,c);
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(24,1));
            northPanel.add(name.getLabeledTextField("Name",null));
            northPanel.add(xFormula.getLabeledTextField("X Formula",null));
            northPanel.add(yFormula.getLabeledTextField("Y Formula",null));
            northPanel.add(zFormula.getLabeledTextField("Z Formula",null));
            northPanel.add(xOrientFormula.getLabeledTextField("X Orientation Formula",null));
            northPanel.add(yOrientFormula.getLabeledTextField("Y Orientation Formula",null));
            northPanel.add(zOrientFormula.getLabeledTextField("Z Orientation Formula",null));
            northPanel.add(sizeFormula.getLabeledTextField("Size Formula",null));
            colorModel.addGUIToPanel("Color",northPanel);
            northPanel.add(opacityFormula.getLabeledTextField("Opacity",null));
            northPanel.add(reflectFormula.getLabeledTextField("Reflectivity",null));
            propPanel.add(northPanel,BorderLayout.NORTH);
        }
        return propPanel;
    }

    @Override
    public void renderToEngine(RenderEngine engine) {
        Plot sink=plotArea.getSink();
        int[] range=xFormula.getSafeElementRange(sink, 0);
        DataFormula.mergeSafeElementRanges(range,yFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,zFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,sizeFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,xOrientFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,yOrientFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,zOrientFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,opacityFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,reflectFormula.getSafeElementRange(sink, 0));
        DataFormula.checkRangeSafety(range,sink);
        
        for(int i=range[0]; i<range[1]; ++i) {
            Vect3D center=new Vect3D(xFormula.valueOf(sink,0, i),yFormula.valueOf(sink,0, i),zFormula.valueOf(sink,0, i));
            double radius=sizeFormula.valueOf(sink,0, i)/2;
            Color color=colorModel.getColor(sink,i);
            DrawAttributes attrs=new DrawAttributes(color,reflectFormula.valueOf(sink,0, i),opacityFormula.valueOf(sink,0, i));
            
            if (currentShape == Shape.Cube) {
            	makeCube(center, radius, engine, attrs);
            }
            // engine.add(new Sphere3D(center,radius), attrs);
        }
    }
    
    private void makeCube(Vect3D center, double r, RenderEngine engine, DrawAttributes attrs) {
    	DrawAttributes[] a = new DrawAttributes[] {attrs};

    	double[][] s = new double[][] {
    			{-1,-1,-1,	-1, 1,-1,	 1, 1,-1}, 	//1
    			{-1,-1,-1,	 1,-1,-1,	 1, 1,-1}, 	//2
    			{ 1,-1,-1,	 1,-1, 1,	 1, 1,-1},  //3
    			{ 1, 1,-1,	 1, 1, 1,	 1,-1, 1},  //4
    			{ 1,-1, 1,	 1, 1, 1,	-1,-1, 1},  //5
    			{-1,-1,1,	 -1, 1, 1,	 1, 1, 1},  //6
    			{ 1, 1,-1,	-1, 1,-1,	 1, 1, 1},	//7
    			{-1, 1,-1,	 1, 1, 1,	-1, 1, 1},	//8
    			{ 1,-1, 1,	 1,-1,-1,	-1,-1, 1},	//9
    			{ 1,-1,-1,	-1,-1,-1,	-1,-1, 1},	//10
    			{-1,-1,-1,	-1, 1,-1,	-1, 1, 1},	//11
    			{-1,-1,-1,	-1,-1, 1,	-1, 1, 1}	//12
    	};
    	
    	for(int i = 0 ; i < 12; i++) {
    		Triangle3D tri = new Triangle3D(new Vect3D(s[i][0]*r,s[i][1]*r,s[i][2]*r),new Vect3D(s[i][3]*r,s[i][4]*r,s[i][5]*r),new Vect3D(s[i][6]*r,s[i][7]*r,s[i][8]*r));
    		tri.originNormal();
    		tri.translate(center);
        	engine.add(tri,a);	
    	}
    }
    
    @Override
    public String toString() {
        return "Scatter 3D - "+name.getValue();
    }
    
    public static String getTypeDescription() {
        return "Polyhedra Plot 3D";
    }

    private PlotArea3D plotArea;
    private EditableString name=new EditableString("Default");
    
    private Shape currentShape = Shape.Cube;
    private DataFormula xFormula=new DataFormula("v[0]");
    private DataFormula yFormula=new DataFormula("v[1]");
    private DataFormula zFormula=new DataFormula("v[2]");
    private DataFormula xOrientFormula=new DataFormula("v[3]");
    private DataFormula yOrientFormula=new DataFormula("v[4]");
    private DataFormula zOrientFormula=new DataFormula("v[5]");
    private DataFormula sizeFormula=new DataFormula("1");
    private ColorModel colorModel=new ColorModel();
    private DataFormula opacityFormula=new DataFormula("1");
    private DataFormula reflectFormula=new DataFormula("0");
    
    private transient JPanel propPanel;

    private static final long serialVersionUID = -4764717067033439513L;
    
    private enum Shape {
    	Cube, Dodecahedron
    };
}
