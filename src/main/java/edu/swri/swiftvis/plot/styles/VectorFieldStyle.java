/*
 * Created on Jul 22, 2004
 */
package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.AxisOptions;
import edu.swri.swiftvis.plot.Bounded;
import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.PlotArea2D;
import edu.swri.swiftvis.plot.PlotLegend;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.plot.util.ColorGradient;
import edu.swri.swiftvis.plot.util.ColorModel;
import edu.swri.swiftvis.plot.util.FormattedString;
import edu.swri.swiftvis.plot.util.LegendHelper;
import edu.swri.swiftvis.plot.util.ShapeCreator;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.plot.util.SymbolOptions;
import edu.swri.swiftvis.plot.util.StrokeOptions.StrokeUser;
import edu.swri.swiftvis.plot.util.SymbolOptions.SymbolUser;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableString;

/**
 * This plot style can be used to plot a vector field.  The user gets to specify formulas
 * for the x and y start of the vectors as well as the length of the vectors in x and y.
 * 
 * @author Mark Lewis
 */
public final class VectorFieldStyle implements DataPlotStyle, StrokeUser, SymbolUser {
    public VectorFieldStyle(PlotArea2D p) {
        plotArea=p;
        bounds=new double[2][2];
        redoBounds();
    }

    private VectorFieldStyle(VectorFieldStyle c,PlotArea2D pa) {
        plotArea=pa;
        bounds=new double[c.bounds.length][c.bounds[0].length];
        for(int i=0; i<bounds.length; ++i) {
            for(int j=0; j<bounds[i].length; ++j) {
                bounds[i][j]=c.bounds[i][j];
            }
        }
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        dxFormula=new DataFormula(c.dxFormula);
        dyFormula=new DataFormula(c.dyFormula);
        sizeFormula=new DataFormula(c.sizeFormula);
        colorModel=new ColorModel(c.colorModel);
        name=new EditableString(c.name.getValue());
        symbolShape=c.symbolShape;
        symbolOptions=new SymbolOptions(this,c.symbolOptions);
        strokeOptions=new StrokeOptions(this,c.strokeOptions);
        highlightFormula=new BooleanFormula(c.highlightFormula);
        highlightColorModel=new ColorModel(c.highlightColorModel);
        legendInfo=new VectorLegend(c.legendInfo);
    }

    @Override
    public String toString() { return "Vector Field - "+name.getValue(); }

    public static String getTypeDescription() { return "Vector Field"; }

    @Override
    public double[][] getBounds() {
    	if(bounds==null) redoBounds();
        double[][] ret=new double[2][2];
        for(int i=0; i<ret.length; i++) {
            ret[i][0]=bounds[i][0];
            ret[i][1]=bounds[i][1];
        }
        return ret;
    }

    @Override
    public void redoBounds() {
        try {
            setXBounds();
    	    setYBounds();
        } catch(ArrayIndexOutOfBoundsException e) {
            // This means the formulas have a bad index.  Let user figure it out.
        }
    }

    @Override
    public PlotLegend getLegendInformation() {
        return legendInfo;
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel innerPanel=new JPanel(new GridLayout(20,1));
            
            // name
            JPanel innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Name"),BorderLayout.WEST);
            innerPanel2.add(name.getTextField(new EditableString.Listener() {
                @Override
                public void valueChanged() { plotArea.syncGUI(); }
            }),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // primary
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Primary Formula"),BorderLayout.WEST);
            innerPanel2.add(xFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // secondary
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Secondary Formula"),BorderLayout.WEST);
            innerPanel2.add(yFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // primary delta
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Primary Delta Formula"),BorderLayout.WEST);
            innerPanel2.add(dxFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // secondary delta
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Secondary Delta Formula"),BorderLayout.WEST);
            innerPanel2.add(dyFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);

            // color
            colorModel.addGUIToPanel("Standard Color",innerPanel);
            
            // symbol info
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Symbol Size Formula"),BorderLayout.WEST);
            innerPanel2.add(sizeFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Symbol Shape"),BorderLayout.WEST);
            ShapeCreator[] shapeOptions={new ShapeCreator.EmptyCreator(),new ShapeCreator.CircleCreator(),
                new ShapeCreator.RectangleCreator(),new ShapeCreator.RoundRectangleCreator()};
            JComboBox comboBox=new JComboBox(shapeOptions);
            if(symbolShape==null) {
                symbolShape=shapeOptions[1];
            }
            comboBox.setSelectedItem(symbolShape);
            comboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { symbolShapeSelected(e); }
            } );
            innerPanel2.add(comboBox,BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            JButton button=new JButton("Symbol Draw Options");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    symbolOptions.edit();
                }
            });
            innerPanel.add(button);
            
            // Stroke Options
            button=new JButton("Stroke Options");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    strokeOptions.edit();
                }
            });
            innerPanel.add(button);

            // highlight info
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Highlight Boolean Formula"),BorderLayout.WEST);
            innerPanel2.add(highlightFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            highlightColorModel.addGUIToPanel("Highlight Color",innerPanel);
            
            propPanel.add(innerPanel,BorderLayout.NORTH);
            
            button=new JButton("Apply Changes");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    plotArea.forceRedraw();
                    plotArea.fireRedraw();
                }
            } );
            propPanel.add(button,BorderLayout.SOUTH);

        }
        return propPanel;
    }

    @Override
    public void drawToGraphics(Graphics2D g,PlotTransform trans) {
        DataSink sink=plotArea.getSink();
        int[] indexBounds=xFormula.getSafeElementRange(sink, 0);
        DataFormula.mergeSafeElementRanges(indexBounds,yFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,dxFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,dyFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,sizeFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,colorModel.getSafeElementRange(sink));
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        g.setStroke(strokeOptions.getStroke());
        for(int i=indexBounds[0]; i<indexBounds[1]; i++) {
			double px=xFormula.valueOf(sink,0, i);
			double py=yFormula.valueOf(sink,0, i);
            Point2D p1=trans.transform(px,py);
            if(highlightFormula.valueOf(sink,0, i)) {
                g.setPaint(highlightColorModel.getColor(sink,i));
            } else {
                g.setPaint(colorModel.getColor(sink,i));
            }
            if(symbolShape!=null && !(symbolShape instanceof ShapeCreator.EmptyCreator)) {
                double sizeFactor=sizeFormula.valueOf(sink,0, i);
                symbolOptions.calcWidthHeight(trans,sizeFactor);
                double xSymbolSize=symbolOptions.getWidth();
                double ySymbolSize=symbolOptions.getHeight();
                g.fill(symbolShape.makeShape(p1.getX(),p1.getY(),xSymbolSize,ySymbolSize));
            }
			Point2D p2=trans.transform(px+dxFormula.valueOf(sink,0, i),py+dyFormula.valueOf(sink,0, i));
			g.draw(new Line2D.Double(p1,p2));
        }

    }

    @Override
    public VectorFieldStyle copy(PlotArea2D pa) {
        return new VectorFieldStyle(this,pa);
    }
    
    @Override
    public void applyStroke() {
        plotArea.fireRedraw();        
    }

    @Override
    public void applySymbol() {
        plotArea.fireRedraw();                
    }

    private void setXBounds() {
        if(!plotArea.hasData()) return;
        int[] indexBounds=xFormula.getSafeElementRange(plotArea.getSink(), 0);
        if(indexBounds[1]<=0) return;
        int[] tmp=dxFormula.getSafeElementRange(plotArea.getSink(), 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        bounds[0][0]=xFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
        bounds[0][1]=bounds[0][0];
        for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
            double val=xFormula.valueOf(plotArea.getSink(),0, i);
            if(val<bounds[0][0]) bounds[0][0]=val;
            if(val>bounds[0][1]) bounds[0][1]=val;
            val+=dxFormula.valueOf(plotArea.getSink(),0, i);
            if(val<bounds[0][0]) bounds[0][0]=val;
            if(val>bounds[0][1]) bounds[0][1]=val;
        }
    }

    private void setYBounds() {
        if(!plotArea.hasData()) return;
        int[] indexBounds=yFormula.getSafeElementRange(plotArea.getSink(), 0);
        if(indexBounds[1]<=0) return;
        int[] tmp=dyFormula.getSafeElementRange(plotArea.getSink(), 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        bounds[1][0]=yFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
        bounds[1][1]=bounds[0][0];
        for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
            double val=yFormula.valueOf(plotArea.getSink(),0, i);
            if(val<bounds[1][0]) bounds[1][0]=val;
            if(val>bounds[1][1]) bounds[1][1]=val;
            val+=dyFormula.valueOf(plotArea.getSink(),0, i);
            if(val<bounds[1][0]) bounds[1][0]=val;
            if(val>bounds[1][1]) bounds[1][1]=val;
        }
    }

    private void symbolShapeSelected(ActionEvent e) {
        JComboBox comboBox=(JComboBox)e.getSource();
        symbolShape=(ShapeCreator)comboBox.getSelectedItem();
    }

    private EditableString name = new EditableString("Default");
    private PlotArea2D plotArea;
    private DataFormula xFormula = new DataFormula("v[0]");
    private DataFormula yFormula = new DataFormula("v[1]");
    private DataFormula dxFormula = new DataFormula("v[2]");
    private DataFormula dyFormula = new DataFormula("v[3]");
    private DataFormula sizeFormula = new DataFormula("10");
    private ColorModel colorModel=new ColorModel();
    private ShapeCreator symbolShape = new ShapeCreator.CircleCreator();
    private SymbolOptions symbolOptions = new SymbolOptions(this);
    private StrokeOptions strokeOptions = new StrokeOptions(1,this);
    private BooleanFormula highlightFormula = new BooleanFormula("1=0");
    private ColorModel highlightColorModel=new ColorModel(ColorModel.GRADIENT_TYPE,"0",new ColorGradient(Color.red,Color.white),"1");

    private double[][] bounds;
    private VectorLegend legendInfo=new VectorLegend();

    private transient JPanel propPanel;

    private static final long serialVersionUID=2098364612347l;
    
    private class VectorLegend implements PlotLegend,Serializable,AxisOptions.AxisOptionUser,Bounded {
        public VectorLegend() {
            ArrayList<Bounded> list=new ArrayList<Bounded>();
            list.add(this);
            axisOptions.setDataPlots(list);            
        }
        public VectorLegend(VectorLegend c) {
            drawDot=new EditableBoolean(c.drawDot.getValue());
            drawGrad=new EditableBoolean(c.drawGrad.getValue());
            drawVertical=new EditableBoolean(c.drawVertical.getValue());
            vertSize=new EditableDouble(c.vertSize.getValue());
            legendName=new FormattedString(c.legendName.getValue());
            axisOptions=new AxisOptions(c.axisOptions,this);
            ArrayList<Bounded> list=new ArrayList<Bounded>();
            list.add(this);
            axisOptions.setDataPlots(list);
        }
        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel innerPanel=new JPanel(new GridLayout(5,1));
                if(legendName.getValue().length()<1) {
                    legendName.setValue(name.getValue());
                }
                JPanel innerPanel2=new JPanel(new BorderLayout());
                innerPanel2.add(new JLabel("Label"),BorderLayout.WEST);
                innerPanel2.add(legendName.getTextField(null));
                innerPanel.add(innerPanel2);
                innerPanel.add(drawDot.getCheckBox("Draw Marker in Legend?",null));
                innerPanel.add(drawGrad.getCheckBox("Draw Gradient in Legend?",null));
                innerPanel.add(drawVertical.getCheckBox("Orient Gradient Vertically?",null));
                JPanel sizePanel=new JPanel(new BorderLayout());
                sizePanel.add(new JLabel("Relative Vertical Size"),BorderLayout.WEST);
                sizePanel.add(vertSize.getTextField(null),BorderLayout.CENTER);
                innerPanel.add(sizePanel);
                propPanel.add(innerPanel,BorderLayout.NORTH);
                
                propPanel.add(axisOptions.getPropertiesPanel(),BorderLayout.CENTER);
                
                JButton applyButton=new JButton("Apply Changes");
                applyButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        plotArea.fireRedraw();
                    }
                });
                propPanel.add(applyButton,BorderLayout.SOUTH);
            }
            return propPanel;
        }
        
        @Override
        public void drawToGraphics(Graphics2D g,Rectangle2D bounds) {
            ColorGradient gradient=colorModel.getGradient();
            if(drawDot.getValue() && drawGrad.getValue() && gradient!=null) {
                LegendHelper.drawTextSymbolAndGradient(g,bounds,legendName,plotArea.getSink(),gradient.getColor(gradient.getBounds()[0]),symbolShape,null,axisOptions,gradient,drawVertical.getValue());
            } else if(drawGrad.getValue() && gradient!=null) {
                LegendHelper.drawTextAndGradient(g,bounds,legendName,plotArea.getSink(),axisOptions,gradient,drawVertical.getValue());
            } else {
                LegendHelper.drawTextAndSymbol(g,bounds,legendName,plotArea.getSink(),axisOptions.getFont(),gradient.getColor(gradient.getBounds()[0]),symbolShape,null);
            }
        }
        
        @Override
        public boolean isDrawn() {            
            return drawDot.getValue() || drawGrad.getValue();
        }
        
        @Override
        public double relativeVerticalSize() {
            return vertSize.getValue();
        }

        @Override
        public void fireRedraw() {
        }
        
        @Override
        public void forceRedraw() {
        }
        
        @Override
        public void syncGUI() {
        }
        
        @Override
        public DataSink getSink() {
            return plotArea.getSink();
        }

        @Override
        public double[][] getBounds() {
            ColorGradient gradient=colorModel.getGradient();
            if(gradient==null) return new double[][]{{0,1},{0,1}};
            return new double[][]{gradient.getBounds(),{0,1}};
        }
        
        private FormattedString legendName=new FormattedString("");
        private EditableBoolean drawDot = new EditableBoolean(false);
        private EditableBoolean drawGrad = new EditableBoolean(false);
        private EditableBoolean drawVertical=new EditableBoolean(false);
        private EditableDouble vertSize = new EditableDouble(1.0);
        private AxisOptions axisOptions=new AxisOptions(this);
        
        private transient JPanel propPanel;
        private static final long serialVersionUID=12346098257623l;
    }

}
