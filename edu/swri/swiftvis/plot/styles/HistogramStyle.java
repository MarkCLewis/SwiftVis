/*
 * Created on Feb 25, 2007
 */
package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.PlotArea2D;
import edu.swri.swiftvis.plot.PlotLegend;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.plot.util.ColorEditor;
import edu.swri.swiftvis.plot.util.ColorRenderer;
import edu.swri.swiftvis.plot.util.FontOptions;
import edu.swri.swiftvis.plot.util.FormattedString;
import edu.swri.swiftvis.plot.util.LegendHelper;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.SourceMapDialog;

public class HistogramStyle implements DataPlotStyle {
    public HistogramStyle(PlotArea2D pa) {
        plotArea=pa;
        bars.add(new BarValue());
    }
    
    public HistogramStyle(HistogramStyle c,PlotArea2D pa) {
        plotArea=pa;
        name=new EditableString(c.name.getValue());
        primaryFormula=new DataFormula(c.primaryFormula.getFormula());
        for(BarValue bv:c.bars) {
            bars.add(new BarValue(bv));
        }
        stroke=new StrokeOptions(null,c.stroke);
        legend=new Legend(c.legend);
    }
    
    @Override
    public DataPlotStyle copy(PlotArea2D pa) {
        return new HistogramStyle(this,pa);
    }

    @Override
    public void drawToGraphics(Graphics2D g, PlotTransform trans) {
        DataSink sink=plotArea.getSink();
        int[] indexRange=primaryFormula.getSafeElementRange(sink, 0);
        for(BarValue bv:bars) {
            DataFormula.mergeSafeElementRanges(indexRange,bv.formula.getSafeElementRange(sink, 0));
        }
        DataFormula.checkRangeSafety(indexRange,sink);
        if(indexRange[1]<=indexRange[0]) return;
        double lastPrimary=0;
        double currentPrimary=primaryFormula.valueOf(sink,0, indexRange[0]);
        double nextPrimary;
        for(int i=indexRange[0]; i<indexRange[1]; ++i) {
            if(i==indexRange[1]-1) {
                nextPrimary=currentPrimary+(currentPrimary-lastPrimary);
            } else if(i==indexRange[0]) {
                nextPrimary=primaryFormula.valueOf(sink,0, i+1);
                lastPrimary=currentPrimary-(nextPrimary-currentPrimary);
            } else {
                nextPrimary=primaryFormula.valueOf(sink,0, i+1);
            }
            drawBar(g,trans,0.5*(lastPrimary+nextPrimary),Math.abs(nextPrimary-lastPrimary)*0.5,i);
            lastPrimary=currentPrimary;
            currentPrimary=nextPrimary;
        }
    }

    @Override
    public PlotLegend getLegendInformation() {
        return legend;
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new GridLayout(3,1));
            JButton mapButton=new JButton("Remap Sources");
            mapButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    Map<Integer,Integer> newSources=SourceMapDialog.showDialog(propPanel);
                    if(newSources!=null) mapSources(newSources);
                }
            });
            northPanel.add(mapButton);
            
            JPanel panel=new JPanel(new BorderLayout());
            panel.add(new JLabel("Name"),BorderLayout.WEST);
            panel.add(name.getTextField(null),BorderLayout.CENTER);
            northPanel.add(panel);
            northPanel.add(primaryFormula.getLabeledTextField("Primary Formula",null));
            propPanel.add(northPanel,BorderLayout.NORTH);
            Box box=new Box(BoxLayout.Y_AXIS);
                        
            JPanel barValuePanel=new JPanel(new BorderLayout());
            barValuePanel.setBorder(BorderFactory.createTitledBorder("Values"));
            barValueTable=new JTable(new BarValueTableModel());
            barValueTable.setDefaultEditor(Color.class, new ColorEditor());
            barValueTable.setDefaultRenderer(Color.class,new ColorRenderer(true));
            barValuePanel.add(barValueTable,BorderLayout.CENTER);
            JButton button=new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(barValueTable.getSelectedRow()>=0 && bars.size()>1 && barValueTable.getSelectedRow()<bars.size()) {
                        bars.remove(barValueTable.getSelectedRow());
                        ((AbstractTableModel)barValueTable.getModel()).fireTableStructureChanged();
                    }
                }
            });
            barValuePanel.add(button,BorderLayout.SOUTH);
            box.add(barValuePanel);
            
            panel=new JPanel(new GridLayout(1,1));
            button=new JButton("Set Stroke");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stroke.edit();
                }
            });
            panel.add(button);
            box.add(panel);
            propPanel.add(box,BorderLayout.CENTER);
            
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
    public void redoBounds() {
        DataSink sink=plotArea.getSink();
        int[] indexRange=primaryFormula.getSafeElementRange(sink, 0);
        for(BarValue bv:bars) {
            DataFormula.mergeSafeElementRanges(indexRange,bv.formula.getSafeElementRange(sink, 0));
        }
        DataFormula.checkRangeSafety(indexRange,sink);
        if(bounds==null) bounds=new double[2][2];
        if(indexRange[0]>=0) {
            bounds[0][0]=primaryFormula.valueOf(sink,0, indexRange[0]);
            bounds[0][1]=bounds[0][0];
            bounds[1][0]=0;
            bounds[1][1]=bars.get(0).formula.valueOf(sink,0, indexRange[0]);
            for(int i=indexRange[0]; i<indexRange[1]; ++i) {
                double pv=primaryFormula.valueOf(sink,0, i);
                if(pv<bounds[0][0]) bounds[0][0]=pv;
                if(pv>bounds[0][1]) bounds[0][1]=pv;
                double v=0;
                for(BarValue bv:bars) {
                    v+=bv.formula.valueOf(sink,0, i);
                }
                if(v<bounds[1][0]) bounds[1][0]=v;
                if(v>bounds[1][1]) bounds[1][1]=v;
            }
            bounds[1][1]+=(bounds[1][1]-bounds[1][0])*0.1;
            double range=bounds[0][1]-bounds[0][0];
            bounds[0][0]-=0.5*range/(indexRange[1]-indexRange[0]);
            bounds[0][1]+=0.5*range/(indexRange[1]-indexRange[0]);
        } else {
            bounds[0][0]=1e100;
            bounds[0][1]=-1e100;
            bounds[1][0]=1e100;
            bounds[1][1]=-1e100;
        }
    }

    @Override
    public double[][] getBounds() {
        if(bounds==null) redoBounds();
        return bounds;
    }
    
    public static String getTypeDescription() { return "Histogram Plot"; }
    
    @Override
    public String toString() { return name.getValue(); }

    private double drawBar(Graphics2D g,PlotTransform trans,double center,double width,int index) {
        double bottom=0;
        for(BarValue bv:bars) {
            double val=bv.formula.valueOf(plotArea.getSink(),0, index);
            Point2D p1=trans.transform(center-width*0.5,bottom);
            Point2D p2=trans.transform(center+width*0.5,bottom+val);
            double minx,miny,maxx,maxy;
            if(p1.getX()<p2.getX()) {
                minx=p1.getX();
                maxx=p2.getX();
            } else {
                minx=p2.getX();
                maxx=p1.getX();
            }
            if(p1.getY()<p2.getY()) {
                miny=p1.getY();
                maxy=p2.getY();
            } else {
                miny=p2.getY();
                maxy=p1.getY();
            }
            Rectangle2D rect=new Rectangle2D.Double(minx,miny,maxx-minx,maxy-miny);
            g.setPaint(bv.fillColor);
            g.fill(rect);
            g.setPaint(bv.borderColor);
            g.setStroke(stroke.getStroke());
            g.draw(rect);
            bottom+=val;
        }
        return bottom;
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
        for(BarValue bv:bars) {
            bv.formula.mapSources(newSources);
        }
    }

    private PlotArea2D plotArea;
    private EditableString name=new EditableString("Histogram Plot");
    private DataFormula primaryFormula=new DataFormula("v[0]");
    private List<BarValue> bars=new ArrayList<BarValue>();
    private StrokeOptions stroke=new StrokeOptions(1,null);
    private Legend legend=new Legend();
    private double[][] bounds;
    
    private transient JPanel propPanel;
    private transient JTable barValueTable;

    private static final long serialVersionUID = -1265441533742246660L;
    
    private static class BarValue implements Serializable {
        public BarValue() {}
        public BarValue(BarValue c) {
            name=c.name;
            formula=new DataFormula(c.formula.getFormula());
            fillColor=c.fillColor;
            borderColor=c.borderColor;
        }
        private FormattedString name=new FormattedString("Value");
        private DataFormula formula=new DataFormula("1");
        private Color fillColor=Color.black;
        private Color borderColor=Color.black;
        private static final long serialVersionUID = 2274081283361473089L;
    }
    
    private class Legend implements PlotLegend,Serializable {
        public Legend() {}
        public Legend(Legend c) {
            showLegend=new EditableBoolean(c.showLegend.getValue());
            relativeSize=new EditableDouble(c.relativeSize.getValue());
        }
        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel panel=new JPanel(new GridLayout(3,1));
                panel.add(showLegend.getCheckBox("Draw Legend?",null));
                JPanel innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Relative Size"),BorderLayout.WEST);
                innerPanel.add(relativeSize.getTextField(null),BorderLayout.CENTER);
                panel.add(innerPanel);
                JButton button=new JButton("Select Font");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        font.edit();
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
                } );
                propPanel.add(button,BorderLayout.SOUTH);
            }
            return propPanel;
        }

        @Override
        public void drawToGraphics(Graphics2D g, Rectangle2D bounds) {
            int totalNum=bars.size();
            double height=bounds.getHeight()/totalNum;
            double top=bounds.getMinY();
            for(BarValue bv:bars) {
                Rectangle2D fillBox=new Rectangle2D.Double(bounds.getMinX()+0.1*bounds.getWidth(),top+0.1*height,bounds.getWidth()*0.3,height*0.8);
                Rectangle2D textBox=new Rectangle2D.Double(bounds.getMinX()+0.5*bounds.getWidth(),top+0.1*height,bounds.getWidth()*0.5,height*0.8);
                g.setPaint(bv.fillColor);
                g.fill(fillBox);
                g.setPaint(bv.borderColor);
                g.setStroke(stroke.getStroke());
                g.draw(fillBox);
                g.setPaint(Color.black);
                LegendHelper.drawText(g,textBox,bv.name,plotArea.getSink(),font.getFont());
                top+=height;
            }            
        }

        @Override
        public boolean isDrawn() {
            return showLegend.getValue();
        }

        @Override
        public double relativeVerticalSize() {
            return relativeSize.getValue();
        }

        private EditableBoolean showLegend=new EditableBoolean(false);
        private EditableDouble relativeSize=new EditableDouble(5);
        private FontOptions font=new FontOptions(null);
        
        private transient JPanel propPanel;
        private static final long serialVersionUID = -5367690001205416939L;
    }
    
    private class BarValueTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public int getRowCount() {
            return bars.size()+1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(rowIndex>=bars.size()) {
                if(columnIndex<2) return "";
                else return Color.black;
            }
            switch(columnIndex) {
            case 0: return bars.get(rowIndex).name.getValue();
            case 1: return bars.get(rowIndex).formula.getFormula();
            case 2: return bars.get(rowIndex).fillColor;
            case 3: return bars.get(rowIndex).borderColor;
            default: return "";
            }
        }
        
        @Override
        public Class<?> getColumnClass(int col) {
            if(col<2) return String.class;
            return Color.class;
        }
        
        @Override
        public String getColumnName(int col) {
            switch(col) {
            case 0: return "Label";
            case 1: return "Formula";
            case 2: return "Fill Color";
            case 3: return "Line Color";
            default: return "";
            }
        }
        
        @Override
        public boolean isCellEditable(int row,int col) {
            return true;
        }
        
        @Override
        public void setValueAt(Object o,int row,int col) {
            boolean structChange=false;
            if(row>=bars.size()) {
                bars.add(new BarValue());
                structChange=true;
            }
            switch(col) {
            case 0:
                bars.get(row).name=new FormattedString(o.toString());
                break;
            case 1:
                bars.get(row).formula=new DataFormula(o.toString());
                break;
            case 2:
                bars.get(row).fillColor=(Color)o;
                break;
            case 3:
                bars.get(row).borderColor=(Color)o;
                break;
            }
            if(structChange) {
                ((AbstractTableModel)barValueTable.getModel()).fireTableStructureChanged();
            } else {
                ((AbstractTableModel)barValueTable.getModel()).fireTableCellUpdated(row,col);
            }
        }
        private static final long serialVersionUID = -3246445606642447383L;
    }
}
