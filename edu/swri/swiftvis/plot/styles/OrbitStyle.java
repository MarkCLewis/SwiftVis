/* Generated by Together */

package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.filters.CoordConvertFilter;
import edu.swri.swiftvis.filters.CoordConvertFilter.Converter;
import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.PlotArea2D;
import edu.swri.swiftvis.plot.PlotLegend;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.plot.util.ColorGradient;
import edu.swri.swiftvis.plot.util.ColorModel;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.plot.util.StrokeOptions.StrokeUser;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;

public final class OrbitStyle implements DataPlotStyle,StrokeUser {
    public OrbitStyle(PlotArea2D pa) {
        plotArea=pa;
    }
    
    private OrbitStyle(OrbitStyle c,PlotArea2D pa) {
        plotArea=pa;
        name=new EditableString(c.name.getValue());
        primaryAxis=c.primaryAxis;
        secondaryAxis=c.secondaryAxis;
        colorModel=new ColorModel(c.colorModel);
        stroke=new StrokeOptions(this,c.stroke);
        numSegments=new EditableInt(c.numSegments.getValue());
    }
    
    @Override
    public String toString() { return "Orbit Plot - "+name.getValue(); }

    public static String getTypeDescription() { return "Orbit Plot"; }

    @Override
    public PlotLegend getLegendInformation() {
        return null;
    }

    @Override
    public JComponent getPropertiesPanel(){
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel panel=new JPanel(new GridLayout(10,1));
            JPanel innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Name"),BorderLayout.WEST);
            innerPanel.add(name.getTextField(new EditableString.Listener() {
                @Override
                public void valueChanged() { plotArea.syncGUI(); }
            }),BorderLayout.CENTER);
            panel.add(innerPanel);
            String[] axes={"x","y","z"};
            
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Primary coordinate"),BorderLayout.WEST);
            final JComboBox primaryAxisBox=new JComboBox(axes);
            primaryAxisBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    primaryAxis=primaryAxisBox.getSelectedIndex();
                }
            });
            innerPanel.add(primaryAxisBox,BorderLayout.CENTER);
            panel.add(innerPanel);
            
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Secondary coordinate"),BorderLayout.WEST);
            final JComboBox secondaryAxisBox=new JComboBox(axes);
            secondaryAxisBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    secondaryAxis=secondaryAxisBox.getSelectedIndex();
                }
            });
            innerPanel.add(secondaryAxisBox,BorderLayout.CENTER);
            panel.add(innerPanel);
            
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Segments per Orbit"),BorderLayout.WEST);
            innerPanel.add(numSegments.getTextField(null),BorderLayout.CENTER);
            panel.add(innerPanel);
            
            colorModel.addGUIToPanel("Color Model",panel);
            
            JButton button=new JButton("Change Stroke");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stroke.edit();
                }
            });
            panel.add(button);
            propPanel.add(panel,BorderLayout.NORTH);
            button=new JButton("Apply Changes");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    plotArea.forceRedraw();
                    plotArea.fireRedraw();                    
                }
            });
            propPanel.add(button,BorderLayout.SOUTH);
        }
        return propPanel;
    }

    /**
     * Returns the min and max values for each dimension that this style
     * supports.  The first index tells which dimension we are looking at and
     * the second index is 0 for min and 1 for max.
     * @return The bounds for this data.
     */
    @Override
    public double[][] getBounds(){
        return new double[][]{{bounds[0][0],bounds[0][1]},{bounds[1][0],bounds[1][1]}};
    }

    @Override
    public void drawToGraphics(Graphics2D g,PlotTransform trans){
        int[] indexBounds=colorModel.getSafeElementRange(plotArea.getSink());
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        g.setStroke(stroke.getStroke());
        float step=(float)(2*Math.PI/numSegments.getValue());
        conv.setup(new DataElement(new int[0],new float[7]));
        for(int i=indexBounds[0]; i<indexBounds[1]; ++i) {
            DataElement de=plotArea.getSink().getSource(0).getElement(i, 0);
            if(de.getValue(2)<1) {
                float[] vals={de.getValue(0),de.getValue(1),de.getValue(2),de.getValue(3),
                        de.getValue(4),de.getValue(5),0};
                int[] params={};
                GeneralPath gp=new GeneralPath();
                DataElement xyz=conv.convert(new DataElement(params,vals),i,0);
                Point2D p=trans.transform(xyz.getValue(primaryAxis+1),xyz.getValue(secondaryAxis+1));
                gp.moveTo((float)p.getX(),(float)p.getY());
                for(float m=step; m<2*Math.PI; m+=step) {
                    vals[6]=m;
                    xyz=conv.convert(new DataElement(params,vals),i,0);
                    p=trans.transform(xyz.getValue(primaryAxis+1),xyz.getValue(secondaryAxis+1));
                    gp.lineTo((float)p.getX(),(float)p.getY());
                }
                gp.closePath();
                g.setColor(colorModel.getColor(plotArea.getSink(),i));
                g.draw(gp);
            }
        }
    }

    @Override
    public void redoBounds(){
        double maxa=0,maxe=0,maxi=0;        
        if(!plotArea.hasData()) return;
        if(bounds==null) bounds=new double[2][2];
        for(int i=0; i<plotArea.getSink().getSource(0).getNumElements(0); ++i) {
            DataElement de=plotArea.getSink().getSource(0).getElement(i, 0);
            if(de.getValue(1)>maxa) maxa=de.getValue(1);
            if(de.getValue(2)>maxe) maxe=de.getValue(2);
            if(Math.abs(de.getValue(3))>maxi) maxi=Math.abs(de.getValue(3));
        }
        if(maxe>1) maxe=0.9;
        double mag=maxa*(1+maxe);  // TODO: Make this the correct formula.
        double[] size={mag,mag,mag*Math.sin(maxi)};
        bounds[0][0]=-size[primaryAxis];
        bounds[0][1]=size[primaryAxis];
        bounds[1][0]=-size[secondaryAxis];
        bounds[1][1]=size[secondaryAxis];
    }

    @Override
    public OrbitStyle copy(PlotArea2D pa) {
        return new OrbitStyle(this,pa);
    }

    @Override
    public void applyStroke() {
    }
    
    private PlotArea2D plotArea;
    private EditableString name = new EditableString("Default");
    private int primaryAxis=0;
    private int secondaryAxis=1;
    private Converter conv=new CoordConvertFilter.OrbEl2Helio();
    private double[][] bounds;
    private ColorModel colorModel=new ColorModel(ColorModel.GRADIENT_TYPE,"0",new ColorGradient(Color.black,Color.white),"1");
    private StrokeOptions stroke=new StrokeOptions(1,this);
    private EditableInt numSegments=new EditableInt(20);
    
    private transient JPanel propPanel;
    
    private static final long serialVersionUID=2389745625827l;
}