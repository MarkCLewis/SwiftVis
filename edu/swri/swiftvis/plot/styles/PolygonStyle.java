package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.AxisOptions;
import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.PlotArea2D;
import edu.swri.swiftvis.plot.PlotLegend;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.plot.util.ColorGradient;
import edu.swri.swiftvis.plot.util.FormattedString;
import edu.swri.swiftvis.plot.util.LegendHelper;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.plot.util.StrokeOptions.StrokeUser;
import edu.swri.swiftvis.plot.util.SymbolOptions.SymbolUser;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.SourceMapDialog;

public final class PolygonStyle implements DataPlotStyle, StrokeUser, SymbolUser {
    public PolygonStyle(PlotArea2D pa) {
        plotArea=pa;
        bounds=new double[4][2];
        try {
	        setPrimaryBounds();
        } catch(Exception e) {
            primaryFormula=new DataFormula("1");
        }
        try {
            setSecondaryBounds();
        } catch(Exception e) {
            secondaryFormula=new DataFormula("1");
        }
    }
    
    private PolygonStyle(PolygonStyle c,PlotArea2D pa) {
        plotArea=pa;
        name=new EditableString(c.name.getValue());
        primaryFormula=new DataFormula(c.primaryFormula);
        secondaryFormula=new DataFormula(c.secondaryFormula);
        connectSelector=new DataFormula(c.connectSelector);
        fillColorFormula=new DataFormula(c.fillColorFormula);
        fillGradient=new ColorGradient(c.fillGradient);
        fillCombineStyle=c.fillCombineStyle;
        drawStroke=new EditableBoolean(c.drawStroke.getValue());
        strokeColorFormula=new DataFormula(c.strokeColorFormula);
        strokeGradient=new ColorGradient(c.strokeGradient);
        strokeCombineStyle=c.strokeCombineStyle;
        strokeOptions=new StrokeOptions(this,c.strokeOptions);
        legendInfo=new PolygonLegend(c.legendInfo);
    }

    @Override
    public String toString() { return "Polygon Plot - "+name.getValue(); }

    public static String getTypeDescription() { return "Polygon Plot"; }

    @Override
    public PlotLegend getLegendInformation() {
        return legendInfo;
    }


    @Override
    public JComponent getPropertiesPanel(){
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel innerPanel=new JPanel(new GridLayout(13,1));
            
            JButton mapButton=new JButton("Remap Sources");
            mapButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    Map<Integer,Integer> newSources=SourceMapDialog.showDialog(propPanel);
                    if(newSources!=null) mapSources(newSources);
                }
            });
            innerPanel.add(mapButton);
            
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
            innerPanel2.add(primaryFormula.getTextField(new DataFormula.Listener() {
                @Override
                public void formulaChanged() { setPrimaryBounds(); }
            }),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // secondary
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Secondary Formula"),BorderLayout.WEST);
            innerPanel2.add(secondaryFormula.getTextField(new DataFormula.Listener() {
                @Override
                public void formulaChanged() { setSecondaryBounds(); }
            }),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // line selector
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Connection Selector"),BorderLayout.WEST);
            innerPanel2.add(connectSelector.getTextField(new DataFormula.Listener() {
                @Override
                public void formulaChanged() { plotArea.fireRedraw(); }
            }),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // fill color
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Fill Color Formula"),BorderLayout.WEST);
            innerPanel2.add(fillColorFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            JButton button=new JButton("Change Fill Gradient");
			button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { fillGradient.edit(); }
            } );
            innerPanel.add(button);
            final JComboBox fillComboBox=new JComboBox(combineStyles);
            fillComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fillCombineStyle=fillComboBox.getSelectedIndex();
                }
            });
            innerPanel.add(fillComboBox);
            
            // stroke
            innerPanel.add(drawStroke.getCheckBox("Draw Bounding Stroke?",null));
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Stroke Color Formula"),BorderLayout.WEST);
            innerPanel2.add(strokeColorFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            button=new JButton("Change Stroke Gradient");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { strokeGradient.edit(); }
            } );
            innerPanel.add(button);
            button=new JButton("Change Stroke Options");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { strokeOptions.edit(); }
            } );
            innerPanel.add(button);
            final JComboBox strokeComboBox=new JComboBox(combineStyles);
            strokeComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    strokeCombineStyle=strokeComboBox.getSelectedIndex();
                }
            });
            innerPanel.add(strokeComboBox);
            
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

    /**
     * Returns the min and max values for each dimension that this style
     * supports.  The first index tells which dimension we are looking at and
     * the second index is 0 for min and 1 for max.
     * @return The bounds for this data.
     */
    @Override
    public double[][] getBounds(){
    	if(bounds==null) redoBounds();
        double[][] ret=new double[bounds.length][2];
        for(int i=0; i<bounds.length; i++) {
            ret[i][0]=bounds[i][0];
            ret[i][1]=bounds[i][1];
        }
        return ret;
    }

    @Override
    public void drawToGraphics(Graphics2D g,PlotTransform trans){
        DataSink sink=plotArea.getSink();
        int[] indexBounds=primaryFormula.getSafeElementRange(sink, 0);
        int[] tmp=secondaryFormula.getSafeElementRange(sink, 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        tmp=connectSelector.getSafeElementRange(sink, 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        tmp=fillColorFormula.getSafeElementRange(sink, 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        tmp=strokeColorFormula.getSafeElementRange(sink, 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        DataFormula.checkRangeSafety(indexBounds,sink);
		g.setStroke(strokeOptions.getStroke());
		Hashtable<Double,PolygonSettings> hash=new Hashtable<Double,PolygonSettings>(1000);
        for(int i=indexBounds[0]; i<indexBounds[1]; i++) {
			double px=primaryFormula.valueOf(sink,0, i);
			double py=secondaryFormula.valueOf(sink,0, i);
            Point2D p=trans.transform(px,py);
            double fillVal=fillColorFormula.valueOf(sink,0, i);
            double strokeVal=0;
            if(drawStroke.getValue()) strokeVal=fillColorFormula.valueOf(sink,0, i);
    		Double key=new Double(connectSelector.valueOf(sink,0, i));
    		if(hash.containsKey(key)) {
    			PolygonSettings poly=hash.get(key);
                poly.addPoint(p,fillCombineStyle,strokeCombineStyle,fillVal,strokeVal);
    		} else {
                PolygonSettings poly=new PolygonSettings(p,fillCombineStyle,strokeCombineStyle,fillVal,strokeVal);
    		    hash.put(key,poly);
            }
        }
        for(PolygonSettings poly:hash.values()) {
            g.setPaint(fillGradient.getColor(combineStyles[fillCombineStyle].getValue(poly.fillColorCombine)));
            poly.path.closePath();
            g.fill(poly.path);
            if(drawStroke.getValue()) {
                g.setPaint(strokeGradient.getColor(combineStyles[strokeCombineStyle].getValue(poly.strokeColorCombine)));
                g.draw(poly.path);                
            }
        }
    }

    @Override
    public void redoBounds(){
        if(!plotArea.hasData()) return;
        if(bounds==null) bounds=new double[2][2];
        try {
            int[] indexBounds=primaryFormula.getSafeElementRange(plotArea.getSink(), 0);
            DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
            if(indexBounds[1]>0) {
    			bounds[0][0]=primaryFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
    			bounds[0][1]=bounds[0][0];
    			for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
    				double val=primaryFormula.valueOf(plotArea.getSink(),0, i);
    				if(val<bounds[0][0]) bounds[0][0]=val;
    				if(val>bounds[0][1]) bounds[0][1]=val;
    			}
            } else {
                bounds[0][0]=0;
                bounds[0][1]=10;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            bounds[0][0]=0;
            bounds[0][1]=10;
        }
        try {
            int[] indexBounds=secondaryFormula.getSafeElementRange(plotArea.getSink(), 0);
            DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
            if(indexBounds[1]>0) {
    			bounds[1][0]=secondaryFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
    			bounds[1][1]=bounds[1][0];
    			for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
    				double val=secondaryFormula.valueOf(plotArea.getSink(),0, i);
    				if(val<bounds[1][0]) bounds[1][0]=val;
    				if(val>bounds[1][1]) bounds[1][1]=val;
    			}
            } else {
                bounds[1][0]=0;
                bounds[1][1]=10;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            bounds[1][0]=0;
            bounds[1][1]=10;
        }
    }

    @Override
    public PolygonStyle copy(PlotArea2D pa) {
        return new PolygonStyle(this,pa);
    }
    
    @Override
    public void applyStroke() {
        plotArea.fireRedraw();
    }

    @Override
    public void applySymbol() {
        plotArea.fireRedraw();
    }

    private void setPrimaryBounds() {
        if(!plotArea.hasData()) return;
		if(bounds==null) redoBounds();
        int[] indexBounds=primaryFormula.getSafeElementRange(plotArea.getSink(), 0);
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        if(indexBounds[1]<=0) return;
        bounds[0][0]=primaryFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
        bounds[0][1]=bounds[0][0];
        for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
            double val=primaryFormula.valueOf(plotArea.getSink(),0, i);
            if(val<bounds[0][0]) bounds[0][0]=val;
            if(val>bounds[0][1]) bounds[0][1]=val;
        }
    }

    private void setSecondaryBounds() {
        if(!plotArea.hasData()) return;
		if(bounds==null) redoBounds();
        int[] indexBounds=secondaryFormula.getSafeElementRange(plotArea.getSink(), 0);
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        if(indexBounds[1]<=0) return;
        bounds[1][0]=secondaryFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
        bounds[1][1]=bounds[1][0];
        for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
            double val=secondaryFormula.valueOf(plotArea.getSink(),0, i);
            if(val<bounds[1][0]) bounds[1][0]=val;
            if(val>bounds[1][1]) bounds[1][1]=val;
        }
    }

    private PlotArea2D plotArea;

    private void mapSources(Map<Integer,Integer> newSources) {
        if(newSources.isEmpty()) return;
        Field[] fields=this.getClass().getDeclaredFields();
        for(Field f:fields) {
            if(DataFormula.class.isAssignableFrom(f.getType())) {
                try {
                    ((DataFormula)f.get(this)).mapSources(newSources);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if(BooleanFormula.class.isAssignableFrom(f.getType())) {
                try {
                    ((BooleanFormula)f.get(this)).mapSources(newSources);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }                
            }
        }
    }

    private EditableString name = new EditableString("Default");
    private DataFormula primaryFormula = new DataFormula("v[0]");
    private DataFormula secondaryFormula = new DataFormula("v[1]");
    private DataFormula connectSelector = new DataFormula("0");
    private DataFormula fillColorFormula=new DataFormula("0");
    private ColorGradient fillGradient=new ColorGradient();
    private int fillCombineStyle;
    private EditableBoolean drawStroke=new EditableBoolean(false);
    private DataFormula strokeColorFormula=new DataFormula("0");
    private ColorGradient strokeGradient=new ColorGradient();
    private StrokeOptions strokeOptions = new StrokeOptions(1,this);
    private int strokeCombineStyle;
    
    private PolygonLegend legendInfo = new PolygonLegend();

    private transient double[][] bounds;
    private transient JPanel propPanel;
    private static final long serialVersionUID=86723490823656l;
    private static final CombineMethod[] combineStyles={new AverageMethod(),new SumMethod(),new AngleAverageMethod()};
    
    private static class PolygonSettings {
        public PolygonSettings(Point2D sp,int fillCombStyle,int strokeCombStyle,double fillVal,double strokeVal) {
            path=new GeneralPath();
            path.moveTo((float)sp.getX(),(float)sp.getY());
            fillColorCombine=combineStyles[fillCombStyle].init();
            strokeColorCombine=combineStyles[strokeCombStyle].init();
            combineStyles[fillCombStyle].addValue(fillColorCombine,1,fillVal);
            combineStyles[strokeCombStyle].addValue(strokeColorCombine,1,strokeVal);
        }
        public void addPoint(Point2D sp,int fillCombStyle,int strokeCombStyle,double fillVal,double strokeVal) {
            path.lineTo((float)sp.getX(),(float)sp.getY());
            combineStyles[fillCombStyle].addValue(fillColorCombine,1,fillVal);
            combineStyles[strokeCombStyle].addValue(strokeColorCombine,1,strokeVal);            
        }
        private GeneralPath path;
        private double[] fillColorCombine;
        private double[] strokeColorCombine;
    }
    
    private static interface CombineMethod {
        double[] init();
        void addValue(double[] cur,double count,double v);
        double getValue(double[] cur);
    }
    
    private static class SumMethod implements CombineMethod {
        @Override
        public double[] init() {
            return new double[1];
        }
        @Override
        public void addValue(double[] cur,double count, double v) {
            cur[0]+=v*count;
        }
        @Override
        public double getValue(double[] cur) {
            return cur[0];
        }
        @Override
        public String toString() { return "Sum"; }
    }
    private static class AverageMethod implements CombineMethod {
        @Override
        public double[] init() {
            return new double[2];
        }
        @Override
        public void addValue(double[] cur,double count, double v) {
            cur[0]+=v*count;
            cur[1]+=count;
        }
        @Override
        public double getValue(double[] cur) {
            return cur[0]/cur[1];
        }
        @Override
        public String toString() { return "Average"; }        
    }
    private static class AngleAverageMethod implements CombineMethod {
        @Override
        public double[] init() {
            return new double[2];
        }
        @Override
        public void addValue(double[] cur,double count, double v) {
            if(cur[1]!=0) {
                double curVal=cur[0]/cur[1];
                while(v<curVal-Math.PI) v+=2*Math.PI;
                while(v>curVal+Math.PI) v-=2*Math.PI;
            }
            cur[0]+=v*count;
            cur[1]+=count;
        }
        @Override
        public double getValue(double[] cur) {
            return cur[0]/cur[1];
        }
        @Override
        public String toString() { return "Angle Average"; }        
    }

    private class PolygonLegend implements PlotLegend,Serializable,AxisOptions.AxisOptionUser {
        public PolygonLegend() {}
        public PolygonLegend(PolygonLegend c) {
            drawGrad=new EditableBoolean(c.drawGrad.getValue());
            vertSize=new EditableDouble(c.vertSize.getValue());
            legendName=new FormattedString(c.legendName.getValue());
            axisOptions=new AxisOptions(c.axisOptions,this);
            drawVertical=new EditableBoolean(c.drawVertical.getValue());
        }
        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel innerPanel=new JPanel(new GridLayout(4,1));
                if(legendName.getValue().length()<1) {
                    legendName.setValue(name.getValue());
                }
                JPanel innerPanel2=new JPanel(new BorderLayout());
                innerPanel2.add(new JLabel("Label"),BorderLayout.WEST);
                innerPanel2.add(legendName.getTextField(null));
                innerPanel.add(innerPanel2);
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
            LegendHelper.drawTextAndGradient(g,bounds,legendName,plotArea.getSink(),axisOptions,fillGradient,drawVertical.getValue());
        }
        
        @Override
        public boolean isDrawn() {
            return drawGrad.getValue();
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

        private FormattedString legendName=new FormattedString("");
        private EditableBoolean drawGrad = new EditableBoolean(false);
        private EditableBoolean drawVertical=new EditableBoolean(false);
        private EditableDouble vertSize = new EditableDouble(1.0);
        private AxisOptions axisOptions=new AxisOptions(this);
        
        private transient JPanel propPanel;
        private static final long serialVersionUID=12346098257623l;
    }
}
