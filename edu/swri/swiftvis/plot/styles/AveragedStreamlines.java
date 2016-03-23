package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.PlotArea2D;
import edu.swri.swiftvis.plot.PlotLegend;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.plot.util.StrokeOptions.StrokeUser;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.SourceMapDialog;

public final class AveragedStreamlines implements DataPlotStyle,StrokeUser {
    public AveragedStreamlines(PlotArea2D pa) {
        plotArea=pa;
        bounds=new double[2][2];
        redoBounds();
    }
    
    public AveragedStreamlines(AveragedStreamlines c,PlotArea2D pa) {
        bounds=new double[c.bounds.length][c.bounds[0].length];
        for(int i=0; i<bounds.length; ++i) {
            for(int j=0; j<bounds[i].length; ++j) {
                bounds[i][j]=c.bounds[i][j];
            }
        }
        plotArea=pa;
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        groupSize=c.groupSize;
        drawResonances=c.drawResonances;
        drawSynodicPeriodBounds=c.drawSynodicPeriodBounds;
        strokeOptions=new StrokeOptions(this,c.strokeOptions);
    }

    @Override
    public String toString() { return "Averaged Streamline"; }

    public static String getTypeDescription() { return "Streamline Plot"; }

    @Override
    public void redoBounds(){
        if(!plotArea.hasData()) return;
        try {
	        int[] indexBounds=xFormula.getSafeElementRange(plotArea.getSink(), 0);
	        if(indexBounds[1]>0) {
				bounds[1][0]=xFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
				bounds[1][1]=bounds[1][0];
				for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
					double val=xFormula.valueOf(plotArea.getSink(),0, i);
					if(val<bounds[1][0]) bounds[1][0]=val;
					if(val>bounds[1][1]) bounds[1][1]=val;
				}
	        } else {
	            bounds[1][0]=0;
	            bounds[1][1]=10;
	        }
        } catch(Exception e) {
            bounds[1][0]=0;
            bounds[1][1]=10;
        }
        try {
        	int[] indexBounds=yFormula.getSafeElementRange(plotArea.getSink(), 0);
	        if(indexBounds[1]>0) {
				bounds[0][0]=yFormula.valueOf(plotArea.getSink(),0, indexBounds[0]);
				bounds[0][1]=bounds[0][0];
				for(int i=indexBounds[0]+1; i<indexBounds[1]; i++) {
					double val=yFormula.valueOf(plotArea.getSink(),0, i);
					if(val<bounds[0][0]) bounds[0][0]=val;
					if(val>bounds[0][1]) bounds[0][1]=val;
				}
	        } else {
	            bounds[0][0]=0;
	            bounds[0][1]=10;
	        }
        } catch(Exception e) {
            bounds[0][0]=0;
            bounds[0][1]=10;
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
        int lineCount=1;
        DataSink sink=plotArea.getSink();
        int[] indexBounds=xFormula.getSafeElementRange(sink, 0);
        int[] tmp=yFormula.getSafeElementRange(sink, 0);
        indexBounds[0]=Math.max(indexBounds[0],tmp[0]);
        indexBounds[1]=Math.min(indexBounds[1],tmp[1]);
        if(indexBounds[1]<=indexBounds[0]) return;
        DataFormula.checkRangeSafety(indexBounds,plotArea.getSink());
        g.setPaint(Color.black);
        double Yval=sink.getSource(0).getElement(indexBounds[0], 0).getValue(0);
        for(int i=indexBounds[0]+1; i<indexBounds[1] && sink.getSource(0).getElement(i, 0).getValue(0)==Yval; i++) {
            lineCount++;
        }
        double[] xValue=new double[lineCount/groupSize.getValue()+1];
        double[] yValue=new double[lineCount/groupSize.getValue()+1];
        g.setStroke(strokeOptions.getStroke());
        for(int i=indexBounds[0]; i<indexBounds[1]; ) {
            for(int j=0; j<lineCount && i<indexBounds[1]; ) {
                double xSum=0.0;
                double ySum=0.0;
                int k=0;
                for(k=0; k<groupSize.getValue() && i<indexBounds[1] && j<lineCount; k++) {
                    xSum+=xFormula.valueOf(sink,0, i);
                    ySum+=yFormula.valueOf(sink,0, i);
                    i++;
                    j++;
                }
                if(k==groupSize.getValue()) {
					int num=j/groupSize.getValue();
					double x=xSum/groupSize.getValue();
					double y=ySum/groupSize.getValue();
					if(i>lineCount) {
                        Point2D p1=trans.transform(yValue[num],xValue[num]);
                        Point2D p2=trans.transform(y,x);
                        g.draw(new Line2D.Double(p1,p2));
						//PlottingHelper.drawLine(g,yValue[num],xValue[num],y,x,xSize*lineWidthFactor,ySize*lineWidthFactor);
					}
					xValue[num]=x;
					yValue[num]=y;
                }
            }
        }
        if(drawResonances.getValue()) {
            g.setColor(Color.black);
            int resNum=(int)(2.0/(3.0*bounds[1][0]));
            double resLoc=2.0/(3.0*resNum);
            while(resLoc<bounds[1][1]) {
                Point2D p1=trans.transform(bounds[0][0],resLoc);
                Point2D p2=trans.transform(bounds[0][1],resLoc);
                g.draw(new Line2D.Double(p1,p2));
                //PlottingHelper.drawLine(g,bounds[0][0],resLoc,bounds[0][1],resLoc,xSize*lineWidthFactor,ySize*lineWidthFactor);
                resNum--;
	            resLoc=2.0/(3.0*resNum);
            }
        }
        if(drawSynodicPeriodBounds.getValue()) {
            g.setColor(Color.blue);
            int markNum=(int)(bounds[0][0]/(2.0*Math.PI));
            while(markNum*2.0*Math.PI<bounds[0][1]) {
                Point2D p1=trans.transform(markNum*2.0*Math.PI,bounds[1][0]);
                Point2D p2=trans.transform(markNum*2.0*Math.PI,bounds[1][1]);
                g.draw(new Line2D.Double(p1,p2));
                //PlottingHelper.drawLine(g,markNum*2.0*Math.PI,bounds[1][0],markNum*2.0*Math.PI,bounds[1][1],xSize*lineWidthFactor,ySize*lineWidthFactor);
                markNum++;
            }
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
        return null;
    }

    @Override
    public JComponent getPropertiesPanel(){
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(7,1));
            JButton mapButton=new JButton("Remap Sources");
            mapButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    Map<Integer,Integer> newSources=SourceMapDialog.showDialog(propPanel);
                    if(newSources!=null) mapSources(newSources);
                }
            });
            northPanel.add(mapButton);
            
            JPanel tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Radial Formula"),BorderLayout.WEST);
            tmpPanel.add(xFormula.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

            tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Azimuthal Formula"),BorderLayout.WEST);
            tmpPanel.add(yFormula.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

            tmpPanel=new JPanel(new BorderLayout());
            tmpPanel.add(new JLabel("Group Size"),BorderLayout.WEST);
            tmpPanel.add(groupSize.getTextField(null),BorderLayout.CENTER);
            northPanel.add(tmpPanel);

            northPanel.add(drawResonances.getCheckBox("Draw Resonance Markers?",null));

            northPanel.add(drawSynodicPeriodBounds.getCheckBox("Draw Orbit Markers?",null));

            JButton button=new JButton("Stroke Options");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    strokeOptions.edit();
                }
            });
            northPanel.add(button);

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
    public AveragedStreamlines copy(PlotArea2D pa) {
        return new AveragedStreamlines(this,pa);
    }

    @Override
    public void applyStroke() {
        plotArea.forceRedraw();
        plotArea.fireRedraw();
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

    private double[][] bounds;

    private PlotArea2D plotArea;

    private DataFormula xFormula = new DataFormula("v[1]-v[3]*cos(v[4])");

    private DataFormula yFormula = new DataFormula("v[2]+2*v[3]*cos(v[4])");

    private EditableInt groupSize=new EditableInt(10);
    private EditableBoolean drawResonances=new EditableBoolean(true);
    private EditableBoolean drawSynodicPeriodBounds=new EditableBoolean(true);
    private StrokeOptions strokeOptions=new StrokeOptions(1,this);

    private transient JPanel propPanel;

    private static final long serialVersionUID=57984745623l;
}
