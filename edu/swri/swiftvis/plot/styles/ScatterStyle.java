package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

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
import edu.swri.swiftvis.util.SourceMapDialog;

public final class ScatterStyle implements DataPlotStyle, StrokeUser, SymbolUser {
    public ScatterStyle(PlotArea2D pa) {
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
    
    private ScatterStyle(ScatterStyle c,PlotArea2D pa) {
        plotArea=pa;
        primaryFormula=new DataFormula(c.primaryFormula);
        secondaryFormula=new DataFormula(c.secondaryFormula);
        sizeFormula=new DataFormula(c.sizeFormula);
        colorModel=new ColorModel(c.colorModel);
        name=new EditableString(c.name.getValue());
        symbolShape=c.symbolShape;
        symbolOptions=new SymbolOptions(this,c.symbolOptions);
        strokeOptions=new StrokeOptions(this,c.strokeOptions);
        drawLines=new EditableBoolean(c.drawLines.getValue());
        connectSelector=new DataFormula(c.connectSelector);
        drawXErrorBars=new EditableBoolean(c.drawXErrorBars.getValue());
        xErrorBarFormula=new DataFormula(c.xErrorBarFormula);
        diffXMaxBar=new EditableBoolean(c.diffXMaxBar.getValue());
        xMaxErrorBarFormula=new DataFormula(c.xMaxErrorBarFormula);
        drawYErrorBars=new EditableBoolean(c.drawYErrorBars.getValue());
        yErrorBarFormula=new DataFormula(c.yErrorBarFormula);
        diffYMaxBar=new EditableBoolean(c.diffYMaxBar.getValue());
        yMaxErrorBarFormula=new DataFormula(c.yMaxErrorBarFormula);
        highlightFormula=new BooleanFormula(c.highlightFormula);
        highlightColorModel=new ColorModel(c.highlightColorModel);
        drawVector=new BooleanFormula(c.drawVector);
        dxFormula=new DataFormula(c.dxFormula);
        dyFormula=new DataFormula(c.dyFormula);
        drawArrowheads=new EditableBoolean(c.drawArrowheads.getValue());
        arrowheadSize=new DataFormula(c.arrowheadSize);
        vectorStroke=new StrokeOptions(this,c.strokeOptions);
        legendInfo=new ScatterLegend(c.legendInfo);
    }

    @Override
    public String toString() { return "Scatter Plot - "+name.getValue(); }

    public static String getTypeDescription() { return "Scatter Plot"; }

    @Override
    public PlotLegend getLegendInformation() {
        return legendInfo;
    }


    @Override
    public JComponent getPropertiesPanel(){
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JTabbedPane tabbedPane=new JTabbedPane();
            
            // Standard Tab
            JPanel innerPanel=new JPanel(new GridLayout(16,1));
            
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
            
            // color
            colorModel.addGUIToPanel("Basic Color",innerPanel);
            
            // symbol info
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Symbol Size Formula"),BorderLayout.WEST);
            innerPanel2.add(sizeFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Symbol Shape"),BorderLayout.WEST);
            ShapeCreator[] shapeOptions={new ShapeCreator.EmptyCreator(),new ShapeCreator.CircleCreator(),
				new ShapeCreator.RectangleCreator(),new ShapeCreator.TriangleCreator(),
                new ShapeCreator.PlusCreator(),new ShapeCreator.RoundRectangleCreator()};
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
                public void actionPerformed(ActionEvent e) { symbolOptions.edit(); }
            });
            innerPanel.add(button);
            
            // use lines
            button=new JButton("Edit Stroke");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { strokeOptions.edit(); }
            });
            innerPanel.add(button);
            innerPanel.add(drawLines.getCheckBox("Connect with lines?",new EditableBoolean.Listener() {
			    @Override
                public void valueChanged() { plotArea.fireRedraw(); }
			}));
            
            // line selector
			innerPanel2=new JPanel(new BorderLayout());
			innerPanel2.add(new JLabel("Connection Selector"),BorderLayout.WEST);
			innerPanel2.add(connectSelector.getTextField(new DataFormula.Listener() {
                @Override
                public void formulaChanged() { plotArea.fireRedraw(); }
            }),BorderLayout.CENTER);
			innerPanel.add(innerPanel2);
            
            innerPanel.add(reversePlotOrder.getCheckBox("Reverse Plot Order?",null));
            
            tabbedPane.addTab("Standard",innerPanel);
            
            // Error Bar Tab
            innerPanel=new JPanel(new GridLayout(14,1));
            
            // X error bars
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(drawXErrorBars.getCheckBox("Show X Error Bars?; Size",null),BorderLayout.WEST);
            innerPanel2.add(xErrorBarFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(diffXMaxBar.getCheckBox("Use different size for max?; Size",null),BorderLayout.WEST);
            innerPanel2.add(xMaxErrorBarFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            // Y error bars
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(drawYErrorBars.getCheckBox("Show Y Error Bars?; Size",null),BorderLayout.WEST);
            innerPanel2.add(yErrorBarFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(diffYMaxBar.getCheckBox("Use different size for max?; Size",null),BorderLayout.WEST);
            innerPanel2.add(yMaxErrorBarFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            
            tabbedPane.addTab("Error Bars",innerPanel);
            
            // Highlighting Tab
            innerPanel=new JPanel(new GridLayout(14,1));

            // highlight info
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Highlight Boolean Formula"),BorderLayout.WEST);
            innerPanel2.add(highlightFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            highlightColorModel.addGUIToPanel("Highlight Color",innerPanel);
            
            tabbedPane.addTab("Highlighting",innerPanel);
            
            // Vector Tab
            JPanel tabPanel=new JPanel(new BorderLayout());
            innerPanel=new JPanel(new GridLayout(6,1));
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Draw vectors when"),BorderLayout.WEST);
            innerPanel2.add(drawVector.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("X length"),BorderLayout.WEST);
            innerPanel2.add(dxFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Y length"),BorderLayout.WEST);
            innerPanel2.add(dyFormula.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel.add(drawArrowheads.getCheckBox("Draw Arrowheads?",null));
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Arrowhead Size"),BorderLayout.WEST);
            innerPanel2.add(arrowheadSize.getTextField(null),BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            button=new JButton("Edit Vector Stroke");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { vectorStroke.edit(); }
            });
            innerPanel.add(button);
            tabPanel.add(innerPanel,BorderLayout.NORTH);
            tabbedPane.addTab("Vectors",tabPanel);
            
            propPanel.add(tabbedPane,BorderLayout.NORTH);
            
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
        primaryFormula.clearGroupSelection();
        secondaryFormula.clearGroupSelection();
        sizeFormula.clearGroupSelection();
        colorModel.clearGroupSelection();
        connectSelector.clearGroupSelection();
        xErrorBarFormula.clearGroupSelection();
        xMaxErrorBarFormula.clearGroupSelection();
        yErrorBarFormula.clearGroupSelection();
        yMaxErrorBarFormula.clearGroupSelection();
        highlightFormula.clearGroupSelection();
        DataSink sink=plotArea.getSink();
        int[] indexBounds=primaryFormula.getSafeElementRange(sink, 0);
        DataFormula.mergeSafeElementRanges(indexBounds,secondaryFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,sizeFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,colorModel.getSafeElementRange(sink));
        DataFormula.mergeSafeElementRanges(indexBounds,highlightColorModel.getSafeElementRange(sink));
        DataFormula.mergeSafeElementRanges(indexBounds,highlightFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,drawVector.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,dxFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,dyFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(indexBounds,arrowheadSize.getSafeElementRange(sink, 0));
        DataFormula.checkRangeSafety(indexBounds,sink);
        Stroke normalStroke=strokeOptions.getStroke();
        Stroke localVectorStroke=vectorStroke.getStroke();
		g.setStroke(normalStroke);
		Hashtable<Double,Point2D> hash=new Hashtable<Double,Point2D>(1000);
        for(int j=indexBounds[0]; j<indexBounds[1]; j++) {
            int i=j;
            if(reversePlotOrder.getValue()) i=indexBounds[1]-j-1;
			double px=primaryFormula.valueOf(sink,0, i);
			double py=secondaryFormula.valueOf(sink,0, i);
            double pxo=px;
            double pyo=py;
            Point2D p=trans.transform(px,py);
            px=p.getX();
            py=p.getY();
            if(highlightFormula.valueOf(sink,0, i)) {
                g.setPaint(highlightColorModel.getColor(sink,i));
            } else {
                g.setPaint(colorModel.getColor(sink,i));
            }
            double sizeFactor=sizeFormula.valueOf(sink,0, i);
            symbolOptions.calcWidthHeight(trans,sizeFactor);
            double xSymbolSize=symbolOptions.getWidth();
            double ySymbolSize=symbolOptions.getHeight();
            if(drawXErrorBars.getValue()) {
                double minSize=xErrorBarFormula.valueOf(sink,0, i);
                double maxSize=minSize;
                if(diffXMaxBar.getValue()) {
                    maxSize=xMaxErrorBarFormula.valueOf(sink,0, i);
                }
                Point2D p1=trans.transform(pxo-minSize,pyo);
                Point2D p2=trans.transform(pxo+maxSize,pyo);
                g.draw(new Line2D.Double(p1,p2));
                Point2D p3=trans.addPixelOffset(p1,0,-ySymbolSize*0.5);
                Point2D p4=trans.addPixelOffset(p1,0,ySymbolSize*0.5);
                g.draw(new Line2D.Double(p3,p4));
                p3=trans.addPixelOffset(p2,0,-ySymbolSize*0.5);
                p4=trans.addPixelOffset(p2,0,ySymbolSize*0.5);
                g.draw(new Line2D.Double(p3,p4));
            }
            if(drawYErrorBars.getValue()) {
                double minSize=yErrorBarFormula.valueOf(sink,0, i);
                double maxSize=minSize;
                if(diffYMaxBar.getValue()) {
                    maxSize=yMaxErrorBarFormula.valueOf(sink,0, i);
                }
                Point2D p1=trans.transform(pxo,pyo-minSize);
                Point2D p2=trans.transform(pxo,pyo+maxSize);
                g.draw(new Line2D.Double(p1,p2));
                Point2D p3=trans.addPixelOffset(p1,-xSymbolSize*0.5,0);
                Point2D p4=trans.addPixelOffset(p1,xSymbolSize*0.5,0);
                g.draw(new Line2D.Double(p3,p4));
                p3=trans.addPixelOffset(p2,-xSymbolSize*0.5,0);
                p4=trans.addPixelOffset(p2,xSymbolSize*0.5,0);
                g.draw(new Line2D.Double(p3,p4));
            }
            if(drawVector.valueOf(sink,0, i)) {
                double dx=dxFormula.valueOf(sink,0, i);
                double dy=dyFormula.valueOf(sink,0, i);
                Point2D endp=trans.transform(pxo+dx,pyo+dy);
                g.setStroke(localVectorStroke);
                g.draw(new Line2D.Double(p,endp));
                if(drawArrowheads.getValue()) {
                    dx=endp.getX()-p.getX();
                    dy=endp.getY()-p.getY();
                    double len=Math.sqrt(dx*dx+dy*dy);
                    double size=arrowheadSize.valueOf(sink,0, i);
                    symbolOptions.calcWidthHeight(trans,size);
                    dx*=-symbolOptions.getWidth()/len;
                    dy*=-symbolOptions.getHeight()/len;
                    double nx=dy*0.5;
                    double ny=-dx*0.5;
                    GeneralPath gp=new GeneralPath();
                    gp.moveTo((float)endp.getX(),(float)endp.getY());
                    gp.lineTo((float)(endp.getX()+dx+nx),(float)(endp.getY()+dy+ny));
                    gp.lineTo((float)(endp.getX()+dx-nx),(float)(endp.getY()+dy-ny));
                    gp.closePath();
                    g.fill(gp);
                }
                g.setStroke(normalStroke);
            }
        	if(symbolShape!=null && !(symbolShape instanceof ShapeCreator.EmptyCreator)) {
				g.fill(symbolShape.makeShape(px,py,xSymbolSize,ySymbolSize));
        	}
        	if(drawLines.getValue()) {
        		Point2D newPnt=p;//new Point2D.Double(px,py);
        		Double key=new Double(connectSelector.valueOf(sink,0, i));
        		if(hash.containsKey(key)) {
        			Point2D pnt=hash.remove(key);
        			g.draw(new Line2D.Double(pnt,newPnt));
        		}
        		hash.put(key,newPnt);
        	}
        }
    }

    @Override
    public void redoBounds(){
        if(bounds==null) bounds=new double[2][2];
        if(!plotArea.hasData()) return;
        try {
            primaryFormula.clearGroupSelection();
            int[] indexBounds=primaryFormula.getSafeElementRange(plotArea.getSink(), 0);
            DataFormula.mergeSafeElementRanges(indexBounds,dxFormula.getSafeElementRange(plotArea.getSink(), 0));
            DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
            if(indexBounds[1]>0) {
    			bounds[0][0]=primaryFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
    			bounds[0][1]=bounds[0][0];
    			for(int i=indexBounds[0]; i<indexBounds[1]; i++) {
    				double val=primaryFormula.valueOf(plotArea.getSink(),0, i);
    				if(val<bounds[0][0]) bounds[0][0]=val;
    				if(val>bounds[0][1]) bounds[0][1]=val;
                    if(drawVector.valueOf(plotArea.getSink(),0, i)) {
                        val+=dxFormula.valueOf(plotArea.getSink(),0, i);
                        if(val<bounds[0][0]) bounds[0][0]=val;
                        if(val>bounds[0][1]) bounds[0][1]=val;
                    }
    			}
            } else {
                bounds[0][0]=1e100;
                bounds[0][1]=-1e100;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            bounds[0][0]=1e100;
            bounds[0][1]=-1e100;
        }
        try {
            secondaryFormula.clearGroupSelection();
            int[] indexBounds=secondaryFormula.getSafeElementRange(plotArea.getSink(), 0);
            DataFormula.mergeSafeElementRanges(indexBounds,dyFormula.getSafeElementRange(plotArea.getSink(), 0));
            DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
            if(indexBounds[1]>0) {
    			bounds[1][0]=secondaryFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
    			bounds[1][1]=bounds[1][0];
    			for(int i=indexBounds[0]; i<indexBounds[1]; i++) {
    				double val=secondaryFormula.valueOf(plotArea.getSink(),0, i);
    				if(val<bounds[1][0]) bounds[1][0]=val;
    				if(val>bounds[1][1]) bounds[1][1]=val;
                    if(drawVector.valueOf(plotArea.getSink(),0, i)) {
                        val+=dyFormula.valueOf(plotArea.getSink(),0, i);
                        if(val<bounds[1][0]) bounds[1][0]=val;
                        if(val>bounds[1][1]) bounds[1][1]=val;
                    }
    			}
            } else {
                bounds[1][0]=1e100;
                bounds[1][1]=-1e100;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            bounds[1][0]=1e100;
            bounds[1][1]=-1e100;
        }
    }

    @Override
    public ScatterStyle copy(PlotArea2D pa) {
        return new ScatterStyle(this,pa);
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

    private void symbolShapeSelected(ActionEvent e) {
    	JComboBox comboBox=(JComboBox)e.getSource();
    	symbolShape=(ShapeCreator)comboBox.getSelectedItem();
    }
    
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
        colorModel.mapSources(newSources);
    }

    private PlotArea2D plotArea;

    private DataFormula primaryFormula = new DataFormula("v[0]");
    private DataFormula secondaryFormula = new DataFormula("v[1]");
    private DataFormula sizeFormula = new DataFormula("10");
    private ColorModel colorModel=new ColorModel();
    private EditableString name = new EditableString("Default");
    private EditableBoolean reversePlotOrder=new EditableBoolean(false);

    private ShapeCreator symbolShape = new ShapeCreator.CircleCreator();
    private SymbolOptions symbolOptions = new SymbolOptions(this);

    private EditableBoolean drawLines = new EditableBoolean(false);
    private DataFormula connectSelector = new DataFormula("0");
    private StrokeOptions strokeOptions = new StrokeOptions(1,this);
    
    private EditableBoolean drawXErrorBars=new EditableBoolean(false);
    private DataFormula xErrorBarFormula=new DataFormula("1");
    private EditableBoolean diffXMaxBar=new EditableBoolean(false);
    private DataFormula xMaxErrorBarFormula=new DataFormula("1");
    private EditableBoolean drawYErrorBars=new EditableBoolean(false);
    private DataFormula yErrorBarFormula=new DataFormula("1");
    private EditableBoolean diffYMaxBar=new EditableBoolean(false);
    private DataFormula yMaxErrorBarFormula=new DataFormula("1");
    private BooleanFormula highlightFormula = new BooleanFormula("1=0");
    private ColorModel highlightColorModel=new ColorModel();
    
    private BooleanFormula drawVector=new BooleanFormula("1=0");
    private DataFormula dxFormula=new DataFormula("1");
    private DataFormula dyFormula=new DataFormula("1");
    private EditableBoolean drawArrowheads=new EditableBoolean(false);
    private DataFormula arrowheadSize=new DataFormula("1");
    private StrokeOptions vectorStroke=new StrokeOptions(1,this);
    
    private ScatterLegend legendInfo = new ScatterLegend();

    private transient double[][] bounds;
    private transient JPanel propPanel;
    private static final long serialVersionUID=86723490823656l;

    private class ScatterLegend implements PlotLegend,Serializable,AxisOptions.AxisOptionUser,Bounded {
        public ScatterLegend() {
            ArrayList<ScatterLegend> list=new ArrayList<ScatterLegend>(1);
            list.add(this);
            axisOptions.setDataPlots(list);
        }
        public ScatterLegend(ScatterLegend c) {
            drawDot=new EditableBoolean(c.drawDot.getValue());
            drawGrad=new EditableBoolean(c.drawGrad.getValue());
            vertSize=new EditableDouble(c.vertSize.getValue());
            legendName=new FormattedString(c.legendName.getValue());
            axisOptions=new AxisOptions(c.axisOptions,this);
            drawVertical=new EditableBoolean(c.drawVertical.getValue());
            ArrayList<ScatterLegend> list=new ArrayList<ScatterLegend>(1);
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
                innerPanel.add(drawDot.getCheckBox("Draw marker in Legend?",null));
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
                LegendHelper.drawTextSymbolAndGradient(g,bounds,legendName,plotArea.getSink(),gradient.getColor(gradient.getBounds()[0]),symbolShape,(drawLines.getValue())?strokeOptions.getStroke():null,axisOptions,gradient,drawVertical.getValue());
            } else if(drawGrad.getValue() && gradient!=null) {
                LegendHelper.drawTextAndGradient(g,bounds,legendName,plotArea.getSink(),axisOptions,gradient,drawVertical.getValue());
            } else {
                LegendHelper.drawTextAndSymbol(g,bounds,legendName,plotArea.getSink(),axisOptions.getFont(),gradient.getColor(gradient.getBounds()[0]),symbolShape,(drawLines.getValue())?strokeOptions.getStroke():null);
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
            return new double[][]{gradient.getBounds()};
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
