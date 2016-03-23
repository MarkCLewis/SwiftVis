/* Generated by Together */

package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import edu.swri.swiftvis.plot.util.FormattedString;
import edu.swri.swiftvis.plot.util.LegendHelper;
import edu.swri.swiftvis.plot.util.PlottingHelper;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.plot.util.StrokeOptions.StrokeUser;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.SourceMapDialog;

public final class IsoLineSurface implements DataPlotStyle,StrokeUser {
    public IsoLineSurface(PlotArea2D pa) {
        plotArea=pa;
        bounds=new double[2][2];
        try {
            xFormula=new DataFormula("v[0]");
            xFormula.valueOf(plotArea.getSink(),0, 0);
        } catch(Exception e) {
            xFormula=new DataFormula("1");
        }
        try {
	        yFormula=new DataFormula("v[1]");
            yFormula.valueOf(plotArea.getSink(),0, 0);
        } catch(Exception e) {
	        yFormula=new DataFormula("1");
        }
        valueFormula=new DataFormula("1");
        redoBounds();
    }
    
    private IsoLineSurface(IsoLineSurface c,PlotArea2D pa) {
        plotArea=pa;
        bounds=new double[c.bounds.length][c.bounds[0].length];
        for(int i=0; i<bounds.length; ++i) {
            for(int j=0; j<bounds[i].length; ++j) {
                bounds[i][j]=c.bounds[i][j];
            }
        }
        name=new EditableString(c.name.getValue());
        columnMatchFormula=new DataFormula(c.columnMatchFormula);
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        valueFormula=new DataFormula(c.valueFormula);
        colorGradient=new ColorGradient(c.colorGradient);
        legendInfo=new ILLegend(c.legendInfo);
    }
    
    @Override
    public String toString() { return "Iso Line Surface - "+name.getValue(); }

    public static String getTypeDescription() { return "Iso Line Surface Plot"; }

    @Override
    public void redoBounds(){
        if(!plotArea.hasData()) return;
		int[] indexBounds=xFormula.getSafeElementRange(plotArea.getSink(), 0);
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
		if(indexBounds[1]>0) {
			bounds[0][0]=xFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
			bounds[0][1]=bounds[0][0];
			for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
				double val=xFormula.valueOf(plotArea.getSink(),0, i);
				if(val<bounds[0][0]) bounds[0][0]=val;
				if(val>bounds[0][1]) bounds[0][1]=val;
			}
		} else {
			bounds[0][0]=0;
			bounds[0][1]=10;
		}
		indexBounds=yFormula.getSafeElementRange(plotArea.getSink(), 0);
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
		if(indexBounds[1]>indexBounds[0]) {
			bounds[1][0]=yFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
			bounds[1][1]=bounds[1][0];
			for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
				double val=yFormula.valueOf(plotArea.getSink(),0, i);
				if(val<bounds[1][0]) bounds[1][0]=val;
				if(val>bounds[1][1]) bounds[1][1]=val;
			}
		} else {
			bounds[1][0]=0;
			bounds[1][1]=10;
		}
    }

    /**
     * This draws the plot into the specified Graphics object.  It assumes that
     * the transform and clipping for that Graphics object have all been set up
     * so that the markers can be drawn at their normal locations.  The xSize
     * and ySize are passed in so that it can figure out how large to make the
     * markers if needed.
     */
    @Override
    public void drawToGraphics(Graphics2D g,PlotTransform trans){
        DataSink sink=plotArea.getSink();
		int[] indexBounds=xFormula.getSafeElementRange(sink, 0);
		int[] tmp=yFormula.getSafeElementRange(sink, 0);
		if(tmp[0]>indexBounds[0]) indexBounds[0]=tmp[0];
		if(tmp[1]<indexBounds[1]) indexBounds[1]=tmp[1];
		tmp=valueFormula.getSafeElementRange(sink, 0);
		if(tmp[0]>indexBounds[0]) indexBounds[0]=tmp[0];
		if(tmp[1]<indexBounds[1]) indexBounds[1]=tmp[1];
		if(indexBounds[1]<=indexBounds[0]) return;
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
		double Yval=columnMatchFormula.valueOf(sink,0, indexBounds[0]);
        int lineCount=1;
		for(int i=indexBounds[0]+1; i<indexBounds[1] && columnMatchFormula.valueOf(sink,0, i)==Yval; i++) {
			lineCount++;
		}
        g.setStroke(strokeOptions.getStroke());
		double[] xValue=new double[lineCount];
		double[] yValue=new double[lineCount];
        double[] vValue=new double[lineCount];
        double[] quadX=new double[4];
        double[] quadY=new double[4];
        double[] quadV=new double[4];
		for(int i=indexBounds[0]; i<indexBounds[1]; ) {
			double lastX=0.0,lastY=0.0,lastV=0.0;
			for(int j=0; j<lineCount && i<indexBounds[1]; j++,i++) {
				double x=xFormula.valueOf(sink,0, i);
				double y=yFormula.valueOf(sink,0, i);
                double v=valueFormula.valueOf(sink,0, i);
				if(i>lineCount && j>0) {
                    quadX[0]=xValue[j];
                    quadX[1]=xValue[j-1];
                    quadX[2]=lastX;
                    quadX[3]=x;
                    quadY[0]=yValue[j];
                    quadY[1]=yValue[j-1];
                    quadY[2]=lastY;
                    quadY[3]=y;
                    quadV[0]=vValue[j];
                    quadV[1]=vValue[j-1];
                    quadV[2]=lastV;
                    quadV[3]=v;
                    processQuad(g,trans,quadX,quadY,quadV);
				}
				if(j>0) {
					xValue[j-1]=lastX;
					yValue[j-1]=lastY;
                    vValue[j-1]=lastV;
				}
				lastX=x;
				lastY=y;
                lastV=v;
			}
			xValue[lineCount-1]=lastX;
			yValue[lineCount-1]=lastY;
            vValue[lineCount-1]=lastV;
		}
    }

    /**
     * Returns the min and max values for each dimension that this style
     * supports.  The first index tells which dimension we are looking at and
     * the second index is 0 for min and 1 for max.
     * @return The bounds for this data.
     */
    @Override
    public double[][] getBounds(){
        double[][] ret=new double[bounds.length][2];
        for(int i=0; i<bounds.length; i++) {
            ret[i][0]=bounds[i][0];
            ret[i][1]=bounds[i][1];
        }
        return ret;
    }

    @Override
    public PlotLegend getLegendInformation() {
        return legendInfo;
    }

    @Override
    public JComponent getPropertiesPanel(){
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(9,1));
            northPanel.setBorder(BorderFactory.createTitledBorder("Data Set"));

            JButton mapButton=new JButton("Remap Sources");
            mapButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    Map<Integer,Integer> newSources=SourceMapDialog.showDialog(propPanel);
                    if(newSources!=null) mapSources(newSources);
                }
            });
            northPanel.add(mapButton);
            
			JPanel tmpPanel=new JPanel(new BorderLayout());
			tmpPanel.add(new JLabel("Name"),BorderLayout.WEST);
			tmpPanel.add(name.getTextField(null),BorderLayout.CENTER);
			northPanel.add(tmpPanel);

            tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Primary Formula"),BorderLayout.WEST);
            tmpPanel.add(xFormula.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

			tmpPanel=new JPanel(new BorderLayout());
			tmpPanel.add(new JLabel("Secondary Formula"),BorderLayout.WEST);
			tmpPanel.add(yFormula.getTextField(null),BorderLayout.CENTER);
			northPanel.add(tmpPanel);

            tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Value Formula"),BorderLayout.WEST);
            tmpPanel.add(valueFormula.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

            tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Line Spacing"),BorderLayout.WEST);
            tmpPanel.add(lineSpacing.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

            JButton button=new JButton("Change Gradient");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { colorGradient.edit(); }
            } );
            northPanel.add(button);

            button=new JButton("Change Stroke");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { strokeOptions.edit(); }
            } );
            northPanel.add(button);

            tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Column Match Formula"),BorderLayout.WEST);
            tmpPanel.add(columnMatchFormula.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

            propPanel.add(northPanel,BorderLayout.NORTH);

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

    @Override
    public IsoLineSurface copy(PlotArea2D pa) {
        return new IsoLineSurface(this,pa);
    }
    
    @Override
    public void applyStroke() {
    }
    
    private void processQuad(Graphics2D g,PlotTransform trans,double[] quadX,double[] quadY,double[] quadV) {
        int[] lineCnt={(int)Math.floor(quadV[0]/lineSpacing.getValue()),(int)Math.floor(quadV[1]/lineSpacing.getValue()),
                (int)Math.floor(quadV[2]/lineSpacing.getValue()),(int)Math.floor(quadV[3]/lineSpacing.getValue())};
        if(lineCnt[0]==lineCnt[1] && lineCnt[0]==lineCnt[2] && lineCnt[0]==lineCnt[3]) return;
        int min=lineCnt[0],max=lineCnt[0];
        for(int i=1; i<lineCnt.length; ++i) {
            if(lineCnt[i]<min) min=lineCnt[i];
            if(lineCnt[i]>max) max=lineCnt[i];
        }
        for(int i=min; i<=max; ++i) {
            double sx=0,sy=0;
            boolean sfound=false;
            double lv=i*lineSpacing.getValue();
            Color col=colorGradient.getColor(lv);
            g.setPaint(col);
            for(int k=0; k<5; ++k) {
                int j=(k+1)%4;
                int o=(j+1)%4;
                if((quadV[j]<lv && quadV[o]>lv) || (quadV[j]>lv && quadV[o]<lv)) {
                    double x=(quadX[j]*(quadV[o]-lv)+quadX[o]*(lv-quadV[j]))/(quadV[o]-quadV[j]);
                    double y=(quadY[j]*(quadV[o]-lv)+quadY[o]*(lv-quadV[j]))/(quadV[o]-quadV[j]);
                    if(sfound) {
                        PlottingHelper.drawLine(g,trans,x,y,sx,sy);
                    }
                    sx=x;
                    sy=y;
                    sfound=true;
                }
            }
        }
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
    }

    private EditableString name=new EditableString("Default");
    private double[][] bounds;
    private PlotArea2D plotArea;
    private DataFormula columnMatchFormula = new DataFormula("v[0]");
    private DataFormula xFormula = new DataFormula("v[0]");
    private DataFormula yFormula = new DataFormula("v[1]");
    private DataFormula valueFormula = new DataFormula("1");
    private EditableDouble lineSpacing=new EditableDouble(1);
    private ColorGradient colorGradient = new ColorGradient();
    private StrokeOptions strokeOptions=new StrokeOptions(1,this);
    private ILLegend legendInfo=new ILLegend();

    private transient JPanel propPanel;

    private static final long serialVersionUID=32567029872364l;
    
    private class ILLegend implements PlotLegend,Serializable,AxisOptions.AxisOptionUser,Bounded {
        public ILLegend() {
        }
        public ILLegend(ILLegend c) {
            drawn=new EditableBoolean(c.drawn.getValue());
            drawVertical=new EditableBoolean(c.drawVertical.getValue());
            vertSize=new EditableDouble(c.vertSize.getValue());
            legendName=new FormattedString(c.legendName.getValue());
            axisOptions=new AxisOptions(c.axisOptions,this);
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
                innerPanel.add(drawn.getCheckBox("Draw in Legend?",null));
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
                
                ArrayList<Bounded> list=new ArrayList<Bounded>();
                list.add(this);
                axisOptions.setDataPlots(list);
            }
            return propPanel;
        }
        
        @Override
        public void drawToGraphics(Graphics2D g,Rectangle2D bounds) {
            LegendHelper.drawTextAndGradient(g,bounds,legendName,plotArea.getSink(),axisOptions,colorGradient,drawVertical.getValue());
        }
        
        @Override
        public boolean isDrawn() {
            return drawn.getValue();
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
            return new double[][]{colorGradient.getBounds(),{0,1}};
        }
        
        private FormattedString legendName=new FormattedString("");
        private EditableBoolean drawn = new EditableBoolean(false);
        private EditableBoolean drawVertical=new EditableBoolean(false);
        private EditableDouble vertSize = new EditableDouble(3.0);
        private AxisOptions axisOptions=new AxisOptions(this);
        
        private transient JPanel propPanel;
        private static final long serialVersionUID=487235690873246l;
    }    
}
