package edu.swri.swiftvis.plot;


import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeNode;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.GraphPanel;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.PlotListener;
import edu.swri.swiftvis.plot.styles.ScatterStyle;
import edu.swri.swiftvis.plot.util.FillOptions;
import edu.swri.swiftvis.plot.util.FillUser;
import edu.swri.swiftvis.plot.util.FontOptions;
import edu.swri.swiftvis.plot.util.FontUser;
import edu.swri.swiftvis.plot.util.FormattedString;
import edu.swri.swiftvis.plot.util.StrokeOptions;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.ThreadHandler;

/**
 * This class is intended to represent the area that a plot or set of
 * related plots should occupy.  You get to specify how many different plots
 * you want in the area and how they are arranged as well as if there should be
 * a legend.  Basically, you get to say how many rows and columns you want.  You
 * can specify if the horizontal or vertical is primary and specify axis options
 * for each one.  For the secondary direction you can specify one or more axis
 * options for each row or column.  Each secondary axis can have one or more data
 * specifications under it.
 */
public final class PlotArea2D implements PlotObject,AxisOptions.AxisOptionUser {
    public PlotArea2D(PlotSpec spec) {
        parent=spec;
        x=new EditableDouble(0.0);
        y=new EditableDouble(0.0);
        width=new EditableDouble(100.0);
        height=new EditableDouble(100.0);
        axisList.add(new AxisOptions(this,"X axis"));
        axisList.add(new AxisOptions(this,"Y axis"));
        plotDataList.add(new ScatterStyle(this));
        data=new DataSetInfo[1][1][1];
        data[0][0][0]=new DataSetInfo(plotDataList.get(0),axisList.get(0),axisList.get(1));
        legendX=new EditableDouble(80.0);
        legendY=new EditableDouble(45.0);
        legendWidth=new EditableDouble(18.0);
        legendHeight=new EditableDouble(10.0);
        setDataOnAxes();
    }

    private PlotArea2D(PlotArea2D c,PlotSpec spec) {
        parent=spec;
        x=new EditableDouble(c.x.getValue());
        y=new EditableDouble(c.y.getValue());
        width=new EditableDouble(c.width.getValue());
        height=new EditableDouble(c.height.getValue());
        data=new DataSetInfo[c.data.length][][];
        for(AxisOptions ao:c.axisList) {
            axisList.add(new AxisOptions(ao,this));
        }
        for(DataPlotStyle dps:c.plotDataList) {
            plotDataList.add(dps.copy(this));
        }
        for(int i=0; i<data.length; ++i) {
            data[i]=new DataSetInfo[c.data[i].length][];
            for(int j=0; j<data[i].length; ++j) {
                data[i][j]=new DataSetInfo[c.data[i][j].length];
                for(int k=0; k<data[i][j].length; ++k) {
                    data[i][j][k]=new DataSetInfo(plotDataList.get(c.plotDataList.indexOf(c.data[i][j][k].style)),
                            axisList.get(c.axisList.indexOf(c.data[i][j][k].xAxis)),
                            axisList.get(c.axisList.indexOf(c.data[i][j][k].yAxis)));
                }
            }
        }
        xRelativeSize=new double[c.xRelativeSize.length];
        for(int i=0; i<xRelativeSize.length; ++i) xRelativeSize[i]=c.xRelativeSize[i];
        yRelativeSize=new double[c.yRelativeSize.length];
        for(int i=0; i<yRelativeSize.length; ++i) yRelativeSize[i]=c.yRelativeSize[i];
        legendX=new EditableDouble(c.legendX.getValue());
        legendY=new EditableDouble(c.legendY.getValue());
        legendWidth=new EditableDouble(c.legendWidth.getValue());
        legendHeight=new EditableDouble(c.legendHeight.getValue());
        for(ListenerCellPair lcp:c.listeners) {
            listeners.add(new ListenerCellPair(lcp));
        }
        for(PlotLabel pl:c.labels) {
            labels.add(copyLabel(pl));
        }
        setDataOnAxes();
    }

    @Override
    public String toString() {
        return "Plot Area";
    }

    @Override
    public DataSink getSink() {
        return parent.getPlot();
    }

    public boolean hasData() {
        return parent.getPlot().getNumSources()>0;
    }

    @Override
    public void syncGUI() {
        synchingGUI=true;

        int selection=dataJList.getSelectedIndex();
        dataJList.setListData(data[mini.getSelectedX()][mini.getSelectedY()]);
        if(selection<0 && data[mini.getSelectedX()][mini.getSelectedY()].length>0) selection=0;
        if(selection>=0 && selection<data[mini.getSelectedX()][mini.getSelectedY()].length) {
            dataJList.setSelectedIndex(selection);
            xAxisJList.setListData(axisList.toArray());
            xAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][selection].xAxis,true);
            yAxisJList.setListData(axisList.toArray());
            yAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][selection].yAxis,true);
        } else {
            xAxisJList.setListData(new Object[0]);
            yAxisJList.setListData(new Object[0]);
        }

        relativeWidthField.setText(Double.toString(xRelativeSize[mini.getSelectedX()]));
        relativeHeightField.setText(Double.toString(yRelativeSize[mini.getSelectedY()]));

        selection=allDataJList.getSelectedIndex();
        allDataJList.setListData(plotDataList.toArray());
        if(selection>=0 && selection<plotDataList.size()) {
            allDataJList.setSelectedIndex(selection);
            dataPropertiesPanel.removeAll();
            dataPropertiesPanel.add(plotDataList.get(selection).getPropertiesPanel());
        }

        selection=allAxisJList.getSelectedIndex();
        allAxisJList.setListData(axisList.toArray());
        if(selection>=0) {
            allAxisJList.setSelectedIndex(selection);
            axisPropertiesPanel.removeAll();
            axisPropertiesPanel.add(axisList.get(selection).getPropertiesPanel());
        }

        selection=legendDataJList.getSelectedIndex();
        legendDataJList.setListData(plotDataList.toArray());
        if(selection>=0 && selection<plotDataList.size()) {
            legendDataJList.setSelectedIndex(selection);
            legendPropertiesPanel.removeAll();
            PlotLegend legendInfo=plotDataList.get(selection).getLegendInformation();
            if(legendInfo!=null) legendPropertiesPanel.add(legendInfo.getPropertiesPanel());
        }

        List<ListenerCellPair> llcp=new LinkedList<ListenerCellPair>();
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cx==listenerMini.getSelectedX() && lcp.cy==listenerMini.getSelectedY()) {
                llcp.add(lcp);
            }
        }
        listenerJList.setListData(llcp.toArray());

        List<PlotLabel> lpl=new LinkedList<PlotLabel>();
        for(PlotLabel pl:labels) {
            if(pl.cx==labelMini.getSelectedX() && pl.cy==labelMini.getSelectedY()) {
                lpl.add(pl);
            }
        }
        labelJList.setListData(lpl.toArray());
        labelJList.setSelectedIndex(lpl.size()-1);

        propPanel.validate();
        propPanel.repaint();
        synchingGUI=false;
    }

    //-------------------------------------------------------------------------
    // Methods from PlotObject
    //-------------------------------------------------------------------------

    /**
     * Draw this object on the provided graphics.
     */
    @Override
    public synchronized void draw(Graphics2D g,Rectangle2D bounds,float fontScale){
        int imgX=(int)(bounds.getWidth()*width.getValue()*0.01);
        int imgY=(int)(bounds.getHeight()*height.getValue()*0.01);
        if(img==null || imgX!=img.getWidth(null) || imgY!=img.getHeight(null)) {
//          img=parent.getSurface().createImage(imgX,imgY);
            img=new BufferedImage(imgX,imgY,BufferedImage.TYPE_INT_ARGB);
            useBuffer=false;
        }
        if(!useBuffer || fontScale!=1) {
            Graphics2D imgG=(Graphics2D)img.getGraphics();
            imgG.setPaint(Color.white);
            imgG.fill(new Rectangle2D.Double(0,0,imgX,imgY));
            //System.out.println("Drawing to graphics: size="+img.getWidth(parent.getPlot().getSurface())+","+img.getHeight(parent.getPlot().getSurface()));
            drawToGraphics(imgG,new Rectangle2D.Double(0,0,imgX,imgY),fontScale,true);
            useBuffer=true;
        }
        imgLeft=x.getValue()*bounds.getWidth()*0.01;
        imgTop=y.getValue()*bounds.getHeight()*0.01;
        g.drawImage(img,AffineTransform.getTranslateInstance(imgLeft,imgTop),parent.getSurface());
        drawLegendToGraphics(g,new Rectangle2D.Double(bounds.getMinX()+legendX.getValue()*bounds.getWidth()*0.01,
                bounds.getMinY()+legendY.getValue()*bounds.getHeight()*0.01,legendWidth.getValue()*bounds.getWidth()*0.01,legendHeight.getValue()*bounds.getHeight()*0.01));
    }

    @Override
    public void print(Graphics2D g,Rectangle2D bounds) {
        AffineTransform oldTrans=g.getTransform();
        drawToGraphics(g,new Rectangle2D.Double(bounds.getMinX()+x.getValue()*bounds.getWidth()*0.01,
                bounds.getMinY()+y.getValue()*bounds.getHeight()*0.01,width.getValue()*bounds.getWidth()*0.01,height.getValue()*bounds.getHeight()*0.01),1,false);
        g.setTransform(oldTrans);
        drawLegendToGraphics(g,new Rectangle2D.Double(bounds.getMinX()+legendX.getValue()*bounds.getWidth()*0.01,
                bounds.getMinY()+legendY.getValue()*bounds.getHeight()*0.01,legendWidth.getValue()*bounds.getWidth()*0.01,legendHeight.getValue()*bounds.getHeight()*0.01));
        g.setTransform(oldTrans);		
    }

    /**
     * Returns a panel that can be used to set the properties of this plot
     * object.  I'm not yet certain what this will do for the Spec.  The plot
     * itself also has a properties panel so I'm not certain which one to use.
     * Right now I'm thinking that I'll use both.  The plot one will have a tree
     * at the top and other stuff below.  This one will have buttons to add new
     * things into the plot.
     */
    @Override
    public JComponent getPropertiesPanel() {
        if (propPanel == null) {
            propPanel = new JTabbedPane();

            // Data Pane
            JPanel panel = new JPanel(new BorderLayout());
            JPanel innerPanel = new JPanel(new BorderLayout());
            allDataJList = new JList();
            allDataJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    allDataListSelectionMade();
                }
            });
            innerPanel.add(new JScrollPane(allDataJList), BorderLayout.CENTER);
            JPanel innerPanel2 = new JPanel(new GridLayout(1,3));
            JButton button = new JButton("Create");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createData();
                }
            });
            innerPanel2.add(button);
            button = new JButton("Delete");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteData();
                }
            });
            innerPanel2.add(button);
            button = new JButton("Duplicate");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    duplicateData();
                }
            });
            innerPanel2.add(button);
            innerPanel.add(innerPanel2, BorderLayout.SOUTH);
            panel.add(innerPanel, BorderLayout.NORTH);
            dataPropertiesPanel = new JPanel(new GridLayout(1, 1));
            panel
            .add(new JScrollPane(dataPropertiesPanel), BorderLayout.CENTER);
            propPanel.add("Data Sets", panel);

            // Layout Pane
            panel = new JPanel(new BorderLayout());
            Box box = new Box(BoxLayout.Y_AXIS);
            innerPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Add X");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addPrimary();
                }
            });
            innerPanel.add(button);
            button=new JButton("Remove X");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removePrimary();
                }
            });
            innerPanel.add(button);
            box.add(innerPanel);
            innerPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Add Y");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addSecondary();
                }
            });
            innerPanel.add(button);
            button=new JButton("Remove Y");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeSecondary();
                }
            });
            innerPanel.add(button);
            box.add(innerPanel);
            innerPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Swap Up");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    swapLayoutUp();
                }
            });
            innerPanel.add(button);
            button=new JButton("Swap Right");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    swapLayoutRight();
                }
            });
            innerPanel.add(button);
            box.add(innerPanel);
            panel.add(box, BorderLayout.NORTH);            

            mini = new PlotAreaMiniature();
            panel.add(mini, BorderLayout.CENTER);

            box = new Box(BoxLayout.Y_AXIS);
            innerPanel=new JPanel(new GridLayout(1,2));
            innerPanel.setBorder(BorderFactory.createTitledBorder("Relative Size"));
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Width"),BorderLayout.WEST);
            relativeWidthField=new JTextField(Double.toString(xRelativeSize[mini.getSelectedX()]));
            relativeWidthField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        xRelativeSize[mini.getSelectedX()]=Double.parseDouble(relativeWidthField.getText());
                        forceRedraw();
                        fireRedraw();
                    } catch(NumberFormatException ex) {
                        JOptionPane.showMessageDialog(relativeWidthField,"You must enter a number.");
                    }
                }
            });
            relativeWidthField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    try {
                        xRelativeSize[mini.getSelectedX()]=Double.parseDouble(relativeWidthField.getText());
                        forceRedraw();
                        fireRedraw();
                    } catch(NumberFormatException ex) {
                        JOptionPane.showMessageDialog(relativeWidthField,"You must enter a number.");
                        relativeWidthField.requestFocus();
                    }
                }
            });
            innerPanel2.add(relativeWidthField,BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            innerPanel2=new JPanel(new BorderLayout());
            innerPanel2.add(new JLabel("Height"),BorderLayout.WEST);
            relativeHeightField=new JTextField(Double.toString(yRelativeSize[mini.getSelectedY()]));
            relativeHeightField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        yRelativeSize[mini.getSelectedY()]=Double.parseDouble(relativeHeightField.getText());
                        forceRedraw();
                        fireRedraw();
                    } catch(NumberFormatException ex) {
                        JOptionPane.showMessageDialog(relativeHeightField,"You must enter a number.");
                    }
                }
            });
            relativeHeightField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    try {
                        yRelativeSize[mini.getSelectedY()]=Double.parseDouble(relativeHeightField.getText());
                        forceRedraw();
                        fireRedraw();
                    } catch(NumberFormatException ex) {
                        JOptionPane.showMessageDialog(relativeHeightField,"You must enter a number.");
                        relativeHeightField.requestFocus();
                    }
                }
            });
            innerPanel2.add(relativeHeightField,BorderLayout.CENTER);
            innerPanel.add(innerPanel2);
            box.add(innerPanel);
            JPanel tmpPanel = new JPanel(new GridLayout(1, 1));
            tmpPanel.setBorder(BorderFactory
                    .createTitledBorder("Data Sets Used"));
            innerPanel2 = new JPanel(new BorderLayout());
            dataJList = new JList(data[mini.getSelectedX()][mini.getSelectedY()]);
            dataJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            dataJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    dataListSelectionMade();
                }
            });
            innerPanel2.add(new JScrollPane(dataJList),
                    BorderLayout.CENTER);
            innerPanel = new JPanel(new GridLayout(1, 3));
            button = new JButton("Add");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addToDataList();
                }
            });
            innerPanel.add(button);
            button = new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeFromDataList();
                }
            });
            innerPanel.add(button);
            button = new JButton("Move Up");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveUpOnDataList();
                }
            });
            innerPanel.add(button);
            innerPanel2.add(innerPanel, BorderLayout.SOUTH);
            tmpPanel.add(innerPanel2);
            box.add(tmpPanel);
            innerPanel = new JPanel(new GridLayout(1, 1));
            innerPanel.setBorder(BorderFactory.createTitledBorder("X Axis"));
            xAxisJList = new JList();
            xAxisJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            xAxisJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    xAxisSelectionMade();
                }
            });
            innerPanel.add(new JScrollPane(xAxisJList));
            box.add(innerPanel);
            innerPanel = new JPanel(new GridLayout(1, 1));
            innerPanel.setBorder(BorderFactory.createTitledBorder("Y Axis"));
            yAxisJList = new JList();
            yAxisJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            yAxisJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    yAxisSelectionMade();
                }
            });
            innerPanel.add(new JScrollPane(yAxisJList));
            box.add(innerPanel);
            panel.add(box, BorderLayout.SOUTH);
            propPanel.add("Layout", panel);

            // Axis Pane
            panel = new JPanel(new BorderLayout());
            innerPanel = new JPanel(new BorderLayout());
            allAxisJList = new JList();
            allAxisJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    allAxisListSelectionMade();
                }
            });
            innerPanel.add(new JScrollPane(allAxisJList),
                    BorderLayout.CENTER);
            innerPanel2 = new JPanel(new GridLayout(1, 3));
            button = new JButton("Create");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createAxis();
                }
            });
            innerPanel2.add(button);
            button = new JButton("Delete");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteAxis();
                }
            });
            innerPanel2.add(button);
            button = new JButton("Duplicate");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    duplicateAxis();
                }
            });
            innerPanel2.add(button);
            innerPanel.add(innerPanel2, BorderLayout.SOUTH);
            panel.add(innerPanel, BorderLayout.NORTH);
            axisPropertiesPanel = new JPanel(new GridLayout(1, 1));
            panel.add(new JScrollPane(axisPropertiesPanel),
                    BorderLayout.CENTER);
            propPanel.add("Axes", panel);

            // Geometry Pane
            panel = new JPanel(new BorderLayout());
            JPanel outerPanel = new JPanel(new BorderLayout());
            innerPanel = new JPanel(new GridLayout(4, 1));
            innerPanel.add(new JLabel("X Position"));
            innerPanel.add(new JLabel("Y Position"));
            innerPanel.add(new JLabel("Width"));
            innerPanel.add(new JLabel("Height"));
            outerPanel.add(innerPanel, BorderLayout.WEST);
            innerPanel = new JPanel(new GridLayout(4, 1));
            innerPanel.add(x.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(x.getValue());
                }
            }));
            innerPanel.add(y.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(y.getValue());
                }
            }));
            innerPanel.add(width.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(width.getValue());
                }
            }));
            innerPanel.add(height.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(height.getValue());
                }
            }));
            outerPanel.add(innerPanel, BorderLayout.CENTER);
            panel.add(outerPanel, BorderLayout.NORTH);
            propPanel.add("Geometry", panel);

            // Legend Pane
            panel = new JPanel(new BorderLayout());
            outerPanel = new JPanel(new BorderLayout());
            innerPanel2 = new JPanel(new BorderLayout());
            innerPanel = new JPanel(new GridLayout(4, 1));
            innerPanel.add(new JLabel("X Position"));
            innerPanel.add(new JLabel("Y Position"));
            innerPanel.add(new JLabel("Width"));
            innerPanel.add(new JLabel("Height"));
            innerPanel2.add(innerPanel, BorderLayout.WEST);
            innerPanel = new JPanel(new GridLayout(4, 1));
            innerPanel.add(legendX.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(x.getValue());
                }
            }));
            innerPanel.add(legendY.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(y.getValue());
                }
            }));
            innerPanel.add(legendWidth.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(width.getValue());
                }
            }));
            innerPanel.add(legendHeight.getTextField(new EditableDouble.Listener() {
                @Override
                public void valueChanged() {
                    checkLocation(height.getValue());
                }
            }));
            innerPanel2.add(innerPanel, BorderLayout.CENTER);
            outerPanel.add(innerPanel2, BorderLayout.NORTH);
            innerPanel = new JPanel(new BorderLayout());
            legendDataJList = new JList();
            legendDataJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    legendDataListSelectionMade();
                }
            });
            innerPanel.add(new JScrollPane(legendDataJList),BorderLayout.CENTER);
            outerPanel.add(innerPanel, BorderLayout.CENTER);
            panel.add(outerPanel, BorderLayout.NORTH);
            legendPropertiesPanel = new JPanel(new GridLayout(1, 1));
            panel.add(new JScrollPane(legendPropertiesPanel),BorderLayout.CENTER);
            propPanel.add("Legend", panel);

            // Listener Pane
            panel=new JPanel(new BorderLayout());
            JPanel northPanel=new JPanel(new BorderLayout());
            final JPanel centerPanel=new JPanel(new GridLayout(1,1));
            listenerMini=new PlotAreaMiniature();
            northPanel.add(listenerMini,BorderLayout.NORTH);
            listenerJList=new JList(listeners.toArray());
            listenerJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    centerPanel.removeAll();
                    if(listenerJList.getSelectedIndex()<0) return;
                    centerPanel.add(((ListenerCellPair)listenerJList.getSelectedValue()).getPropertiesPanel());
                    centerPanel.validate();
                    centerPanel.repaint();
                }
            });
            northPanel.add(new JScrollPane(listenerJList),BorderLayout.CENTER);
            innerPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Add");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addPlotListener();
                }
            });
            innerPanel.add(button);
            button=new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removePlotListener();
                }
            });
            innerPanel.add(button);
            northPanel.add(innerPanel,BorderLayout.SOUTH);
            panel.add(northPanel,BorderLayout.NORTH);
            panel.add(centerPanel,BorderLayout.CENTER);
            propPanel.add("Listeners",panel);

            // Label Pane
            panel=new JPanel(new BorderLayout());
            northPanel=new JPanel(new BorderLayout());
            final JPanel centerLabelPanel=new JPanel(new GridLayout(1,1));
            labelMini=new PlotAreaMiniature();
            northPanel.add(labelMini,BorderLayout.NORTH);
            labelJList=new JList(labels.toArray());
            labelJList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    centerLabelPanel.removeAll();
                    if(labelJList.getSelectedIndex()<0) return;
                    centerLabelPanel.add(((PlotLabel)labelJList.getSelectedValue()).getPropertiesPanel());
                    centerLabelPanel.validate();
                    centerLabelPanel.repaint();
                }
            });
            northPanel.add(new JScrollPane(labelJList),BorderLayout.CENTER);
            outerPanel=new JPanel(new GridLayout(3,1));
            innerPanel=new JPanel(new GridLayout(1,3));
            button=new JButton("Add");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addPlotLabel();
                }
            });
            innerPanel.add(button);
            button=new JButton("Remove");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removePlotLabel();
                    forceRedraw();
                    fireRedraw();
                }
            });
            innerPanel.add(button);
            button=new JButton("Duplicate");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    duplicatePlotLabel();
                }
            });
            innerPanel.add(button);
            outerPanel.add(innerPanel);
            innerPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Move Up");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveUpPlotLabel();
                }
            });
            innerPanel.add(button);
            button=new JButton("Move Down");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveDownPlotLabel();
                }
            });
            innerPanel.add(button);
            outerPanel.add(innerPanel);
            innerPanel=new JPanel(new GridLayout(1,2));
            button=new JButton("Move Left");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveLeftPlotLabel();
                }
            });
            innerPanel.add(button);
            button=new JButton("Move Right");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveRightPlotLabel();
                }
            });
            innerPanel.add(button);
            outerPanel.add(innerPanel);
            northPanel.add(outerPanel,BorderLayout.SOUTH);
            panel.add(northPanel,BorderLayout.NORTH);
            panel.add(new JScrollPane(centerLabelPanel),BorderLayout.CENTER);
            propPanel.add("Labels",panel);

            syncGUI();
        }
        return propPanel;
    }

    @Override
    public void fireRedraw() {
        useBuffer=false;
        parent.fireRedraw();
    }

    /**
     * Send a message down the tree that current draw buffers are invalid.
     * This generally happens because data has changed.
     */
    @Override
    public void forceRedraw(){
        if(!hasData()) return;
        useBuffer=false;
        for(DataPlotStyle dps:plotDataList) {
            dps.redoBounds();
        }
        setDataOnAxes();
    }

    @Override
    public PlotArea2D copy(PlotSpec p) {
        return new PlotArea2D(this,p);
    }

    @Override
    public void relink(Hashtable<GraphElement,GraphElement> linkHash) {
        List<ListenerCellPair> newListeners=new LinkedList<ListenerCellPair>();
        for(ListenerCellPair lcp:listeners) {
            GraphElement mapTo=linkHash.get(lcp.pl);
            if(mapTo!=null) newListeners.add(new ListenerCellPair((PlotListener)mapTo,lcp.cx,lcp.cy));
        }
        listeners=newListeners;
    }

    @Override
    public void mousePressed(MouseEvent e,double mx,double my) {
        if(cellBounds==null || cellBounds[0][0]==null) return;
        if(mx>=x.getValue() && mx<=x.getValue()+width.getValue() &&
                my>=y.getValue() && my<=y.getValue()+height.getValue()) {
            ClickPosition cp=new ClickPosition(e.getX(),e.getY());
            lastInside=cp.inside;
            if(!cp.inside) return;
            for(ListenerCellPair lcp:listeners) {
                if(lcp.cx==cp.primaryCell && lcp.cy==cp.secondaryCell && e.getButton()==lcp.buttonUsed) lcp.pl.mousePressed(cp.v1,cp.v2,e);
            }
        }
    }
    @Override
    public void mouseReleased(MouseEvent e,double mx,double my) {
        if(cellBounds==null || cellBounds[0][0]==null) return;
        if(mx>=x.getValue() && mx<=x.getValue()+width.getValue() &&
                my>=y.getValue() && my<=y.getValue()+height.getValue()) {
            ClickPosition cp=new ClickPosition(e.getX(),e.getY());
            lastInside=cp.inside;
            if(!cp.inside) return;
            for(ListenerCellPair lcp:listeners) {
                if(lcp.cx==cp.primaryCell && lcp.cy==cp.secondaryCell && e.getButton()==lcp.buttonUsed) lcp.pl.mouseReleased(cp.v1,cp.v2,e);
            }
        }
    }
    @Override
    public void mouseClicked(MouseEvent e,double mx,double my) {
        if(cellBounds==null || cellBounds[0][0]==null) return;
        if(mx>=x.getValue() && mx<=x.getValue()+width.getValue() &&
                my>=y.getValue() && my<=y.getValue()+height.getValue()) {
            ClickPosition cp=new ClickPosition(e.getX(),e.getY());
            lastInside=cp.inside;
            if(!cp.inside) return;
            for(ListenerCellPair lcp:listeners) {
                if(lcp.cx==cp.primaryCell && lcp.cy==cp.secondaryCell && e.getButton()==lcp.buttonUsed) lcp.pl.mouseClicked(cp.v1,cp.v2,e);
            }
        }
    }
    @Override
    public void mouseMoved(MouseEvent e,double mx,double my) {
        if(cellBounds==null || cellBounds[0][0]==null) return;
        if(mx>=x.getValue() && mx<=x.getValue()+width.getValue() &&
                my>=y.getValue() && my<=y.getValue()+height.getValue()) {
            ClickPosition cp=new ClickPosition(e.getX(),e.getY());
            lastInside=cp.inside;
            if(!cp.inside) return;
            for(ListenerCellPair lcp:listeners) {
                if(lcp.cx==cp.primaryCell && lcp.cy==cp.secondaryCell) lcp.pl.mouseMoved(cp.v1,cp.v2,e);
            }
        }
    }
    @Override
    public void mouseDragged(MouseEvent e,double mx,double my) {
        if(cellBounds==null || cellBounds[0][0]==null) return;
        if(mx>=x.getValue() && mx<=x.getValue()+width.getValue() &&
                my>=y.getValue() && my<=y.getValue()+height.getValue()) {
            ClickPosition cp=new ClickPosition(e.getX(),e.getY());
            lastInside=cp.inside;
            if(!cp.inside) return;
            for(ListenerCellPair lcp:listeners) {
                if(lcp.cx==cp.primaryCell && lcp.cy==cp.secondaryCell && e.getButton()==lcp.buttonUsed) lcp.pl.mouseDragged(cp.v1,cp.v2,e);
            }
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if(lastInside) {
            for(ListenerCellPair lcp:listeners) {
                lcp.pl.keyPressed(e);
            }
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
        if(lastInside) {
            for(ListenerCellPair lcp:listeners) {
                lcp.pl.keyReleased(e);
            }
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {
        if(lastInside) {
            for(ListenerCellPair lcp:listeners) {
                lcp.pl.keyReleased(e);
            }
        }
    }

    //-------------------------------------------------------------------------
    // Methods from TreeNode
    //-------------------------------------------------------------------------

    @Override
    public Enumeration<TreeNode> children() { return null; }

    @Override
    public boolean getAllowsChildren() { return false; }

    @Override
    public TreeNode getChildAt(int index) { return null; }

    @Override
    public int getChildCount() { return 0; }

    @Override
    public int getIndex(TreeNode node) { return -1; }

    @Override
    public TreeNode getParent() { return parent; }

    @Override
    public boolean isLeaf() { return true; }

    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

    /**
     * This function will actually draw in the image that will be put to screen.
     * The drawing is done in three parts.  First, the primary axis figures out how 
     * much space it will take up on the size of the plot where it will be drawn.  
     * Second, the same is done for all the secondary axes.  They should each save this 
     * because they will be asked to draw in their space later.  Once the exact plot area 
     * has been decided on, the calls will be made for each of the PlotData elements to
     * draw themselves to that part of the image (clipping must be set).  After
     * all the data has been drawn, the axes and their marks can be drawn over it.
     */
    private void drawToGraphics(Graphics2D g,final Rectangle2D bounds,float fontScale,boolean doInThreadPool) {
        Font defaultFont=g.getFont();
        g.setPaint(Color.white);
        g.fill(bounds);
        if(OptionsData.instance().getAntialias()) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        AxisOptions.AxisBounds[] axisBounds=new AxisOptions.AxisBounds[axisList.size()];
        cellBounds=new Rectangle2D[data.length][data[0].length];
        ArrayList<ArrayList<Integer>> xAxes=new ArrayList<ArrayList<Integer>>();
        for(int i=0; i<data.length; ++i) xAxes.add(new ArrayList<Integer>());
        ArrayList<ArrayList<Integer>> yAxes=new ArrayList<ArrayList<Integer>>();
        for(int i=0; i<data[0].length; ++i) yAxes.add(new ArrayList<Integer>());        

        boolean exceptionOccured=false;

        try {
            gridBounds=findGridBounds(axisBounds,bounds,fontScale,cellBounds,xAxes,yAxes);
        } catch(Exception e) {
            e.printStackTrace();
            exceptionOccured=true;
        }

        try {
            if(hasData()) {
                drawDataSets(g, bounds, fontScale, doInThreadPool, cellBounds);
            }
        } catch(Exception e) {
            e.printStackTrace();
            exceptionOccured=true;
        }

        try {
            optimizeAxes(xAxes);
            optimizeAxes(yAxes);

            g.setStroke(new BasicStroke((float)(0.001*Math.min(bounds.getWidth(),bounds.getHeight()))));
            double[][] minSideBufferX=new double[data.length][];
            double[][] maxSideBufferX=new double[data.length][];
            double[][] minSideBufferY=new double[data[0].length][];
            double[][] maxSideBufferY=new double[data[0].length][];
            drawAxisLines(g, fontScale, axisBounds, cellBounds, xAxes, yAxes,minSideBufferX,maxSideBufferX,minSideBufferY,maxSideBufferY);
            drawAxisLabels(g, fontScale, axisBounds, cellBounds, xAxes, yAxes, minSideBufferX, maxSideBufferX, minSideBufferY, maxSideBufferY);
        } catch(Exception e) {
            e.printStackTrace();
            exceptionOccured=true;
        }

        if(exceptionOccured) {
            g.setClip(bounds);
            g.setPaint(Color.red);
            g.setFont(defaultFont);
            g.drawString("Exception occured during plot draw. Plot may be inaccurate.", 10, 50);
        }
    }

    private Rectangle2D findGridBounds(AxisOptions.AxisBounds[] axisBounds,Rectangle2D bounds,float fontScale,
            Rectangle2D[][] cellBounds,ArrayList<ArrayList<Integer>> xAxes,ArrayList<ArrayList<Integer>> yAxes) {
        for(int i=0; i<axisBounds.length; i++) {
            axisBounds[i]=axisList.get(i).getAxisBounds(fontScale);
        }
        double primaryMinBuffer=0.0;
        double primaryMaxBuffer=0.0;
        double secondaryMinBuffer=0.0;
        double secondaryMaxBuffer=0.0;
        for(int i=0; i<data.length; i++) {
            for(int j=0; j<data[0].length; ++j) {
                for(int k=0; k<data[i][j].length; ++k) {
                    int xIndex=axisList.indexOf(data[i][j][k].xAxis);
                    int yIndex=axisList.indexOf(data[i][j][k].yAxis);
                    if(!xAxes.get(i).contains(xIndex)) xAxes.get(i).add(xIndex);
                    if(!yAxes.get(j).contains(yIndex)) yAxes.get(j).add(yIndex);
                }
            }
        }
        for(int i=0; i<xAxes.size(); ++i) {
            double thisSizeMin=0.0;
            double thisSizeMax=0.0;
            for(int j=0; j<xAxes.get(i).size(); ++j) {
                int axisIndex=xAxes.get(i).get(j);
                if(axisList.get(axisIndex).getAxisLocation()==AxisOptions.MIN_SIDE) {
                    if(thisSizeMin==0.0) {
                        thisSizeMin+=axisBounds[axisIndex].getOutside();
                    } else {
                        thisSizeMin+=axisBounds[axisIndex].getOutside()+axisBounds[axisIndex].getInside()+2;
                    }
                } else {
                    if(thisSizeMax==0.0) {
                        thisSizeMax+=axisBounds[axisIndex].getOutside();
                    } else {
                        thisSizeMax+=axisBounds[axisIndex].getOutside()+axisBounds[axisIndex].getInside()+2;
                    }
                }
            }
            if(thisSizeMin>primaryMinBuffer) primaryMinBuffer=thisSizeMin;
            if(thisSizeMax>primaryMaxBuffer) primaryMaxBuffer=thisSizeMax;
        }
        for(int i=0; i<yAxes.size(); ++i) {
            double thisSizeMin=0.0;
            double thisSizeMax=0.0;
            for(int j=0; j<yAxes.get(i).size(); ++j) {
                int axisIndex=yAxes.get(i).get(j);
                if(axisList.get(axisIndex).getAxisLocation()==AxisOptions.MIN_SIDE) {
                    if(thisSizeMin==0.0) {
                        thisSizeMin+=axisBounds[axisIndex].getOutside();
                    } else {
                        thisSizeMin+=axisBounds[axisIndex].getOutside()+axisBounds[axisIndex].getInside()+2;
                    }
                } else {
                    if(thisSizeMax==0.0) {
                        thisSizeMax+=axisBounds[axisIndex].getOutside();
                    } else {
                        thisSizeMax+=axisBounds[axisIndex].getOutside()+axisBounds[axisIndex].getInside()+2;
                    }
                }
            }
            if(thisSizeMin>secondaryMinBuffer) secondaryMinBuffer=thisSizeMin;
            if(thisSizeMax>secondaryMaxBuffer) secondaryMaxBuffer=thisSizeMax;
        }

        double fontHeight=0.0;
        for(int i=0; i<xAxes.get(0).size(); ++i) {
            if(fontHeight<axisBounds[xAxes.get(0).get(i)].getHeight()) {
                fontHeight=axisBounds[xAxes.get(0).get(i)].getHeight(); 
            }
        }
        if(secondaryMinBuffer<fontHeight/2+2) secondaryMinBuffer=fontHeight/2+2; 
        fontHeight=0.0;
        for(int i=0; i<xAxes.get(xAxes.size()-1).size(); ++i) {
            if(fontHeight<axisBounds[xAxes.get(xAxes.size()-1).get(i)].getHeight()) {
                fontHeight=axisBounds[xAxes.get(xAxes.size()-1).get(i)].getHeight(); 
            }
        }
        if(secondaryMaxBuffer<fontHeight/2+2) secondaryMaxBuffer=fontHeight/2+2; 

        fontHeight=0.0;
        for(int i=0; i<yAxes.get(0).size(); ++i) {
            if(fontHeight<axisBounds[yAxes.get(0).get(i)].getHeight()) {
                fontHeight=axisBounds[yAxes.get(0).get(i)].getHeight(); 
            }
        }
        if(primaryMinBuffer<fontHeight/2+2) primaryMinBuffer=fontHeight/2+2; 
        fontHeight=0.0;
        for(int i=0; i<yAxes.get(yAxes.size()-1).size(); ++i) {
            if(fontHeight<axisBounds[yAxes.get(yAxes.size()-1).get(i)].getHeight()) {
                fontHeight=axisBounds[yAxes.get(yAxes.size()-1).get(i)].getHeight(); 
            }
        }
        if(primaryMaxBuffer<fontHeight/2+2) primaryMaxBuffer=fontHeight/2+2; 

        Rectangle2D ret=new Rectangle2D.Double(bounds.getMinX()+secondaryMinBuffer,bounds.getMinY()+primaryMaxBuffer,
                bounds.getWidth()-secondaryMinBuffer-secondaryMaxBuffer,bounds.getHeight()-primaryMinBuffer-primaryMaxBuffer);
        double xTot=0.0;
        double[] xCumm=new double[xRelativeSize.length];
        for(int i=0; i<xRelativeSize.length; ++i) {
            xCumm[i]=xTot;
            xTot+=xRelativeSize[i];
        }
        double yTot=0.0;
        double[] yCumm=new double[yRelativeSize.length];
        for(int i=0; i<yRelativeSize.length; ++i) {
            yTot+=yRelativeSize[i];
            yCumm[i]=yTot;
        }
        for(int i=0; i<cellBounds.length; ++i) {
            for(int j=0; j<cellBounds[i].length; ++j) {
                cellBounds[i][j]=new Rectangle2D.Double(ret.getMinX()+ret.getWidth()*xCumm[i]/xTot,ret.getMaxY()-ret.getHeight()*yCumm[j]/yTot,
                        ret.getWidth()*xRelativeSize[i]/xTot,ret.getHeight()*yRelativeSize[j]/yTot);
            }
        }
        return ret;
    }

    private void drawDataSets(Graphics2D g, final Rectangle2D bounds, float fontScale, boolean doInThreadPool, Rectangle2D[][] cellBounds) throws Exception {
        Exception ex=null;
        Stroke stroke=g.getStroke();
        Shape clipShape=g.getClip();
        for(int i=0; i<data.length; i++) {  // loop over primary r/c
            final int ii=i;
            for(int j=0; j<data[i].length; j++) {  // loop over secondary r/c
                if(data[i][j].length<1) {
                    JOptionPane.showMessageDialog(propPanel,"Plot cell ("+i+","+j+") does not have a data set.");
                } else {
                    final int jj=j;
                    g.setClip(cellBounds[i][j]);
                    final PlotTransform trans0=
                        new PlotTransform(data[i][j][0].xAxis,data[i][j][0].yAxis,cellBounds[i][j]);
                    for(PlotLabel pl:labels) {
                        if(pl.cx==i && pl.cy==j && pl.plotBefore.getValue()) {
                            pl.draw(g,cellBounds[i][j],fontScale,trans0);
                        }
                    }
                    if(doInThreadPool && OptionsData.instance().getPlottingThreaded()) {
                        final BufferedImage[] imgs=new BufferedImage[data[i][j].length];
                        for(int k=0; k<data[i][j].length; k++) {  // loop over data list
                            final PlotTransform trans;
                            AxisOptions primaryAxis=data[i][j][k].xAxis;
                            AxisOptions secondaryAxis=data[i][j][k].yAxis;
                            trans=(k==0)?trans0:new PlotTransform(primaryAxis,secondaryAxis,cellBounds[i][j]);
                            final int kk=k;
                            imgs[k]=new BufferedImage((int)bounds.getWidth(),(int)bounds.getHeight(),BufferedImage.TYPE_INT_ARGB);
                            ThreadHandler.instance().loadWaitTask(parent.getPlot(),new Runnable() {
                                @Override
                                public void run() {
                                    Graphics2D g2d=imgs[kk].createGraphics(); //image.createGraphics();
                                    if(OptionsData.instance().getAntialias()) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                                    data[ii][jj][kk].style.drawToGraphics(g2d,trans);
                                }
                            });
                        }
                        ThreadHandler.instance().waitForAll(parent.getPlot());
                        for (int a=0; a<imgs.length; a++) {
                            g.drawImage(imgs[a],null,0,0);
                        }
                    } else {
                        for(int k=0; k<data[i][j].length; k++) {  // loop over data list
                            AxisOptions primaryAxis=data[i][j][k].xAxis;
                            AxisOptions secondaryAxis=data[i][j][k].yAxis;
                            PlotTransform trans;
                            trans=(k==0)?trans0:new PlotTransform(primaryAxis,secondaryAxis,cellBounds[i][j]);
                            try {
                                data[i][j][k].style.drawToGraphics(g,trans);
                            } catch(Exception e) {
                                ex=e;
                            }
                        }
                    }
                    for(ListenerCellPair lcp:listeners) {
                        Shape region=lcp.pl.getSelectionRegion(trans0);
                        if(lcp.cx==i && lcp.cy==j && lcp.colorRegion.getValue() && region!=null) {
                            g.setPaint(lcp.fill.getColor());
                            g.fill(region);
                        }
                    }
                    for(PlotLabel pl:labels) {
                        if(pl.cx==i && pl.cy==j && !pl.plotBefore.getValue()) {
                            pl.draw(g,cellBounds[i][j],fontScale,trans0);
                        }
                    }
                }
            }
        }
        g.setClip(clipShape);
        g.setStroke(stroke);
        if(ex!=null) throw ex;
    }

    private void drawAxisLines(Graphics2D g,float fontScale,AxisOptions.AxisBounds[] axisBounds,Rectangle2D[][] cellBounds,
            ArrayList<ArrayList<Integer>> xAxes,ArrayList<ArrayList<Integer>> yAxes,
            double[][] minSideBufferX,double[][] maxSideBufferX,double[][] minSideBufferY,double[][] maxSideBufferY) {
        for(int i=0; i<data.length; ++i) {
            for(int j=0; j<data[i].length; ++j) {
                g.setPaint(Color.black);
                g.draw(cellBounds[i][j]);
                if(!xAxes.get(i).isEmpty()) {
                    int xa=xAxes.get(i).get(0);
                    axisList.get(xa).drawAxis(g,cellBounds[i][j],axisBounds[xa],AxisOptions.HORIZONTAL_AXIS,j,data[0].length,i==data.length-1,fontScale);
                }
                if(!yAxes.get(j).isEmpty()) {
                    int ya=yAxes.get(j).get(0);
                    axisList.get(ya).drawAxis(g,cellBounds[i][j],axisBounds[ya],AxisOptions.VERTICAL_AXIS,i,data.length,j==data[0].length-1,fontScale);
                }
            }
        }

        for(int i=0; i<xAxes.size(); i++) {
            minSideBufferX[i]=new double[xAxes.get(i).size()];
            maxSideBufferX[i]=new double[xAxes.get(i).size()];
            double minSideSum=0.0;
            double maxSideSum=0.0;
            boolean hadMin=false;
            boolean hadMax=false;
            if(!xAxes.get(i).isEmpty()) {
                if(axisList.get(xAxes.get(i).get(0)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                    minSideSum+=axisBounds[xAxes.get(i).get(0)].getOutside();
                    hadMin=true;
                } else {
                    maxSideSum+=axisBounds[xAxes.get(i).get(0)].getOutside();
                    hadMax=true;
                }
                for(int j=1; j<xAxes.get(i).size(); j++) {
                    if(axisList.get(xAxes.get(i).get(j)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                        if(hadMin) minSideSum+=axisBounds[xAxes.get(i).get(j)].getInside();
                    } else {
                        if(hadMax) maxSideSum+=axisBounds[xAxes.get(i).get(j)].getInside();
                    }
                    Rectangle2D plotBounds=new Rectangle2D.Double(cellBounds[i][0].getMinX(),gridBounds.getMinY()-maxSideSum,
                            cellBounds[i][0].getWidth(),gridBounds.getHeight()+minSideSum+maxSideSum);
                    axisList.get(xAxes.get(i).get(j)).drawAxis(g,plotBounds,axisBounds[xAxes.get(i).get(j)],AxisOptions.HORIZONTAL_AXIS,0,1,i==data.length-1,fontScale);
                    minSideBufferX[i][j]=minSideSum;
                    maxSideBufferX[i][j]=maxSideSum;
                    if(axisList.get(xAxes.get(i).get(j)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                        minSideSum+=axisBounds[xAxes.get(i).get(j)].getOutside();
                        hadMin=true;
                    } else {
                        maxSideSum+=axisBounds[xAxes.get(i).get(j)].getOutside();
                        hadMax=true;
                    }
                }
            }
        }

        for(int i=0; i<yAxes.size(); i++) {
            minSideBufferY[i]=new double[yAxes.get(i).size()];
            maxSideBufferY[i]=new double[yAxes.get(i).size()];
            double minSideSum=0.0;
            double maxSideSum=0.0;
            boolean hadMin=false;
            boolean hadMax=false;
            if(!yAxes.get(i).isEmpty()) {
                if(axisList.get(yAxes.get(i).get(0)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                    minSideSum+=axisBounds[yAxes.get(i).get(0)].getOutside();
                    hadMin=true;
                } else {
                    maxSideSum+=axisBounds[yAxes.get(i).get(0)].getOutside();
                    hadMax=true;
                }
                for(int j=1; j<yAxes.get(i).size(); j++) {
                    if(axisList.get(yAxes.get(i).get(j)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                        if(hadMin) minSideSum+=axisBounds[yAxes.get(i).get(j)].getInside();
                    } else {
                        if(hadMax) maxSideSum+=axisBounds[yAxes.get(i).get(j)].getInside();
                    }
                    Rectangle2D plotBounds=new Rectangle2D.Double(gridBounds.getMinX()-minSideSum,cellBounds[0][i].getMinY(),
                            gridBounds.getWidth()+minSideSum+maxSideSum,cellBounds[0][i].getHeight());
                    axisList.get(yAxes.get(i).get(j)).drawAxis(g,plotBounds,axisBounds[yAxes.get(i).get(j)],AxisOptions.VERTICAL_AXIS,0,1,i==data[0].length-1,fontScale);
                    minSideBufferY[i][j]=minSideSum;
                    maxSideBufferY[i][j]=maxSideSum;
                    if(axisList.get(yAxes.get(i).get(j)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                        minSideSum+=axisBounds[yAxes.get(i).get(j)].getOutside();
                        hadMin=true;
                    } else {
                        maxSideSum+=axisBounds[yAxes.get(i).get(j)].getOutside();
                        hadMax=true;
                    }
                }
            }
        }    
    }

    private void drawAxisLabels(Graphics2D g, float fontScale, AxisOptions.AxisBounds[] axisBounds, Rectangle2D[][] cellBounds, ArrayList<ArrayList<Integer>> xAxes, ArrayList<ArrayList<Integer>> yAxes, double[][] minSideBufferX, double[][] maxSideBufferX, double[][] minSideBufferY, double[][] maxSideBufferY) {
        int maxXCnt=0;
        for(int i=0; i<xAxes.size(); i++) {
            if(xAxes.get(i).size()>maxXCnt) maxXCnt=xAxes.get(i).size();
        }
        for(int j=0; j<maxXCnt; j++) {
            Rectangle2D plotBounds=null;
            for(int i=0; i<xAxes.size(); i++) {
                if(j<xAxes.get(i).size()) {
                    if(plotBounds==null) {
                        plotBounds=new Rectangle2D.Double(cellBounds[i][0].getMinX(),gridBounds.getMinY()-maxSideBufferX[i][j],
                                cellBounds[i][0].getWidth(),gridBounds.getHeight()+minSideBufferX[i][j]+maxSideBufferX[i][j]);
                    } else {
                        plotBounds.add(new Rectangle2D.Double(cellBounds[i][0].getMinX(),gridBounds.getMinY()-maxSideBufferX[i][j],
                                cellBounds[i][0].getWidth(),gridBounds.getHeight()+minSideBufferX[i][j]+maxSideBufferX[i][j]));
                    }
                    boolean endSharing=false;
                    if(i>=xAxes.size()-1 || xAxes.get(i+1).size()<j+1 || !axisList.get(xAxes.get(i).get(j)).mergeLabelWith(axisList.get(xAxes.get(i+1).get(j)))) {
                        endSharing=true;
                    }
                    if(!endSharing) {
                        if(axisList.get(xAxes.get(i).get(j)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                            endSharing=(minSideBufferX[i][j]!=minSideBufferX[i+1][j]);
                        } else {
                            endSharing=(maxSideBufferX[i][j]!=maxSideBufferX[i+1][j]);
                        }
                    }
                    if(endSharing) {
                        axisList.get(xAxes.get(i).get(j)).drawAxisLabel(g,plotBounds,axisBounds[xAxes.get(i).get(j)],AxisOptions.HORIZONTAL_AXIS,fontScale);
                        plotBounds=null;
                    }
                } else plotBounds=null;
            }
        }
        int maxYCnt=0;
        for(int i=0; i<yAxes.size(); i++) {
            if(yAxes.get(i).size()>maxYCnt) maxYCnt=yAxes.get(i).size();
        }
        for(int j=0; j<maxYCnt; j++) {
            Rectangle2D plotBounds=null;
            for(int i=0; i<yAxes.size(); i++) {
                if(j<yAxes.get(i).size()) {
                    if(plotBounds==null) {
                        plotBounds=new Rectangle2D.Double(gridBounds.getMinX()-minSideBufferY[i][j],cellBounds[0][i].getMinY(),
                                gridBounds.getWidth()+minSideBufferY[i][j]+maxSideBufferY[i][j],cellBounds[0][i].getHeight());						
                    } else {
                        plotBounds.add(new Rectangle2D.Double(gridBounds.getMinX()-minSideBufferY[i][j],cellBounds[0][i].getMinY(),
                                gridBounds.getWidth()+minSideBufferY[i][j]+maxSideBufferY[i][j],cellBounds[0][i].getHeight()));
                    }
                    boolean endSharing=false;
                    if(i>=yAxes.size()-1 || yAxes.get(i+1).size()<j+1 || !axisList.get(yAxes.get(i).get(j)).mergeLabelWith(axisList.get(yAxes.get(i+1).get(j)))) {
                        endSharing=true;
                    }
                    if(!endSharing) {
                        if(axisList.get(yAxes.get(i).get(j)).getAxisLocation()==AxisOptions.MIN_SIDE) {
                            endSharing=(minSideBufferY[i][j]!=minSideBufferY[i+1][j]);
                        } else {
                            endSharing=(maxSideBufferY[i][j]!=maxSideBufferY[i+1][j]);
                        }
                    }
                    if(endSharing) {
                        axisList.get(yAxes.get(i).get(j)).drawAxisLabel(g,plotBounds,axisBounds[yAxes.get(i).get(j)],AxisOptions.VERTICAL_AXIS,fontScale);
                        plotBounds=null;
                    }
                } else plotBounds=null;
            }
        }
    }

    private void optimizeAxes(ArrayList<ArrayList<Integer>> axes) {
        // TODO: reorder the axes to maximize the mergers.
    }

    private void drawLegendToGraphics(Graphics2D g,Rectangle2D bounds) {
        ArrayList<PlotLegend> legendInfo=new ArrayList<PlotLegend>();
        double totalSize=0.0;
        for(int i=0; i<plotDataList.size(); ++i) {
            PlotLegend pl=plotDataList.get(i).getLegendInformation();
            if(pl!=null && pl.isDrawn()) {
                legendInfo.add(pl);
                totalSize+=pl.relativeVerticalSize();
            }
        }
        if(legendInfo.isEmpty()) return;
        g.setColor(Color.white);
        g.fill(bounds);
        g.setColor(Color.black);
        g.draw(bounds);
        bounds=new Rectangle2D.Double(bounds.getMinX()+bounds.getWidth()*0.01,
                bounds.getMinY()+bounds.getHeight()*0.01,bounds.getWidth()*0.98,bounds.getHeight()*0.98);
        double downPos=0.0;
        for(int i=0; i<legendInfo.size(); ++i) {
            PlotLegend pl=legendInfo.get(i);
            Rectangle2D smallBounds=new Rectangle2D.Double(bounds.getMinX(),bounds.getMinY()+downPos/totalSize*bounds.getHeight(),
                    bounds.getWidth(),bounds.getHeight()*pl.relativeVerticalSize()/totalSize);
            pl.drawToGraphics(g,smallBounds);
            downPos+=pl.relativeVerticalSize();
        }
    }

    private void setDataOnAxes() {
        ArrayList<ArrayList<DataPlotStyle>> dps=new ArrayList<ArrayList<DataPlotStyle>>();
        ArrayList<ArrayList<Integer>> orient=new ArrayList<ArrayList<Integer>>();
        for(int i=0; i<axisList.size(); ++i) {
            dps.add(new ArrayList<DataPlotStyle>());
            orient.add(new ArrayList<Integer>());
        }
        for(int i=0; i<data.length; ++i) {
            for(int j=0; j<data[i].length; ++j) {
                for(int k=0; k<data[i][j].length; ++k) {
                    int xIndex=axisList.indexOf(data[i][j][k].xAxis);
                    dps.get(xIndex).add(data[i][j][k].style);
                    orient.get(xIndex).add(0);
                    int yIndex=axisList.indexOf(data[i][j][k].yAxis);
                    dps.get(yIndex).add(data[i][j][k].style);
                    orient.get(yIndex).add(1);
                }
            }
        }
        for(int i=0; i<axisList.size(); ++i) {
            axisList.get(i).setDataPlots(dps.get(i),orient.get(i));
        }
    }

    private void checkLocation(double val) {
        if(val<0.0 || val>100.0) {
            JOptionPane.showMessageDialog(propPanel,"Warning: this field uses values scaled between 0.0 and 100.0.");
        }
        fireRedraw();
    }

    private void addPrimary() {
        DataSetInfo[][][] newDS=new DataSetInfo[data.length+1][][];
        double[] newRS=new double[xRelativeSize.length+1];
        int selX=mini.getSelectedX();
        for(int i=0; i<newDS.length; i++) {
            if(i<selX+1) {
                newDS[i]=data[i];
                newRS[i]=xRelativeSize[i];
            } else if(i>selX+1) {
                newDS[i]=data[i-1];
                newRS[i]=xRelativeSize[i-1];
            } else {
                newDS[i]=new DataSetInfo[data[0].length][];
                for(int j=0; j<newDS[i].length; j++) {
                    newDS[i][j]=new DataSetInfo[data[selX][j].length];
                    for(int k=0; k<newDS[i][j].length; ++k) {
                        newDS[i][j][k]=new DataSetInfo(data[selX][j][k].style,data[selX][j][k].xAxis,data[selX][j][k].yAxis);                        
                    }
                }
                newRS[i]=1.0;
            }
        }
        data=newDS;        
        xRelativeSize=newRS;
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cx>mini.getSelectedX()) lcp.cx++;
        }
        for(PlotLabel pl:labels) {
            if(pl.cx>mini.getSelectedX()) pl.cx++;
        }
        mini.repaint();
        syncGUI();
        forceRedraw();
        fireRedraw();
    }

    private void removePrimary() {
        if(data.length==1) {
            JOptionPane.showMessageDialog(propPanel,"You can't delete the last x axis.");
            return;
        }
        DataSetInfo[][][] newDS=new DataSetInfo[data.length-1][][];
        double[] newRS=new double[xRelativeSize.length-1];
        for(int i=0; i<data.length; i++) {
            if(i<mini.getSelectedX()) {
                newDS[i]=data[i];
                newRS[i]=xRelativeSize[i];
            } else if(i>mini.getSelectedX()) {
                newDS[i-1]=data[i];       
                newRS[i-1]=xRelativeSize[i];
            }
        }
        data=newDS;
        xRelativeSize=newRS;
        List<ListenerCellPair> lcpToRemove=new LinkedList<ListenerCellPair>();
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cx==mini.getSelectedX()) lcpToRemove.add(lcp);
            if(lcp.cx>mini.getSelectedX()) lcp.cx--;
        }
        listeners.removeAll(lcpToRemove);
        List<PlotLabel> plToRemove=new LinkedList<PlotLabel>();
        for(PlotLabel pl:labels) {
            if(pl.cx==mini.getSelectedX()) plToRemove.add(pl);
            if(pl.cx>mini.getSelectedX()) pl.cx--;
        }
        labels.removeAll(plToRemove);
        if(mini.getSelectedX()>=data.length) mini.selectedX=0;
        mini.repaint();
        syncGUI();
        forceRedraw();
        fireRedraw();
    }

    private void addSecondary() {
        int numY=data[0].length;
        int selY=mini.getSelectedY();
        for(int i=0; i<data.length; i++) {
            DataSetInfo[][] newDS2=new DataSetInfo[numY+1][];
            for(int j=0; j<newDS2.length; j++) {
                if(j<selY+1) {
                    newDS2[j]=data[i][j];
                } else if(j>selY+1) {
                    newDS2[j]=data[i][j-1];
                } else {
                    newDS2[j]=new DataSetInfo[data[i][selY].length];
                    for(int k=0; k<newDS2[j].length; ++k) {
                        newDS2[j][k]=new DataSetInfo(data[i][selY][k].style,data[i][selY][k].xAxis,data[i][selY][k].yAxis);                        
                    }
                }
            }
            data[i]=newDS2;
        }
        double[] newRS=new double[numY+1];
        for(int i=0; i<newRS.length; i++) {
            if(i<mini.getSelectedY()+1) {
                newRS[i]=yRelativeSize[i];
            } else if(i>mini.getSelectedY()+1) {
                newRS[i]=yRelativeSize[i-1];
            } else {
                newRS[i]=1;
            }
        }
        yRelativeSize=newRS;
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cy>mini.getSelectedY()) lcp.cy++;
        }
        for(PlotLabel pl:labels) {
            if(pl.cy>mini.getSelectedY()) pl.cy++;
        }
        mini.repaint();
        syncGUI();
        forceRedraw();
        fireRedraw();        
    }

    private void removeSecondary() {
        if(data[0].length==1) {
            JOptionPane.showMessageDialog(propPanel,"You can't delete the last y axis.");
            return;
        }
        int numY=data[0].length;
        for(int i=0; i<data.length; i++) {
            DataSetInfo[][] newDS2=new DataSetInfo[numY-1][];
            for(int j=0; j<data[i].length; j++) {
                if(j<mini.getSelectedY()) {
                    newDS2[j]=data[i][j];
                } else if(j>mini.getSelectedY()) {
                    newDS2[j-1]=data[i][j];
                }
            }
            data[i]=newDS2;
        }
        double[] newRS=new double[numY-1];
        for(int i=0; i<yRelativeSize.length; i++) {
            if(i<mini.getSelectedY()) {
                newRS[i]=yRelativeSize[i];
            } else if(i>mini.getSelectedY()) {
                newRS[i-1]=yRelativeSize[i];
            }
        }
        yRelativeSize=newRS;
        List<ListenerCellPair> lcpToRemove=new LinkedList<ListenerCellPair>();
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cy==mini.getSelectedY()) lcpToRemove.add(lcp);
            if(lcp.cy>mini.getSelectedY()) lcp.cy--;
        }
        listeners.removeAll(lcpToRemove);
        List<PlotLabel> plToRemove=new LinkedList<PlotLabel>();
        for(PlotLabel pl:labels) {
            if(pl.cy==mini.getSelectedY()) plToRemove.add(pl);
            if(pl.cy>mini.getSelectedY()) pl.cy--;
        }
        labels.removeAll(plToRemove);
        if(mini.getSelectedY()>=data[0].length) mini.selectedY=0;
        mini.repaint();
        syncGUI();
        forceRedraw();
        fireRedraw();        
    }
    
    private void swapLayoutUp() {
        int sel=mini.getSelectedY();
        if(sel>=data[0].length-1) return;
        for(int i=0; i<data.length; ++i) {
            DataSetInfo[] tmp=data[i][sel];
            data[i][sel]=data[i][sel+1];
            data[i][sel+1]=tmp;
        }
        double tmp=yRelativeSize[sel];
        yRelativeSize[sel]=yRelativeSize[sel+1];
        yRelativeSize[sel+1]=tmp;
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cy==sel) lcp.cy=sel+1;
            else if(lcp.cy==sel+1) lcp.cy=sel;
        }
        for(PlotLabel pl:labels) {
            if(pl.cy==sel) pl.cy=sel+1;
            else if(pl.cy==sel+1) pl.cy=sel;
        }
        syncGUI();
        forceRedraw();
        fireRedraw();        
    }
    
    private void swapLayoutRight() {
        int sel=mini.getSelectedX();
        if(sel>=data.length-1) return;
        DataSetInfo[][] tmp=data[sel];
        data[sel]=data[sel+1];
        data[sel+1]=tmp;
        double tmp2=xRelativeSize[sel];
        xRelativeSize[sel]=xRelativeSize[sel+1];
        xRelativeSize[sel+1]=tmp2;
        for(ListenerCellPair lcp:listeners) {
            if(lcp.cx==sel) lcp.cx=sel+1;
            else if(lcp.cx==sel+1) lcp.cx=sel;
        }
        for(PlotLabel pl:labels) {
            if(pl.cx==sel) pl.cx=sel+1;
            else if(pl.cx==sel+1) pl.cx=sel;
        }
        syncGUI();
        forceRedraw();
        fireRedraw();        
    }

    private void xAxisSelectionMade() {
        if(synchingGUI || dataJList.getSelectedIndex()<0 || xAxisJList.getSelectedIndex()<0) return;
        data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].xAxis=
            axisList.get(xAxisJList.getSelectedIndex());
        setDataOnAxes();
        forceRedraw();
        fireRedraw();
    }

    private void yAxisSelectionMade() {
        if(synchingGUI || dataJList.getSelectedIndex()<0 || yAxisJList.getSelectedIndex()<0) return;
        data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].yAxis=
            axisList.get(yAxisJList.getSelectedIndex());
        setDataOnAxes();
        forceRedraw();
        fireRedraw();
    }

    private void addToDataList() {
        Object select=JOptionPane.showInputDialog(dataJList,"Select the data set you want to add.","Data Set Selection",
                JOptionPane.QUESTION_MESSAGE,null,plotDataList.toArray(),plotDataList.get(0));
        int toAdd=plotDataList.indexOf(select);
        if(toAdd<0) return;
        int sx=mini.getSelectedX();
        int sy=mini.getSelectedY();
        DataSetInfo[] newSS=Arrays.copyOf(data[sx][sy],data[sx][sy].length+1);
        //DataSetInfo[] newSS=new DataSetInfo[data[sx][sy].length+1];
        //for(int i=0; i<data[sx][sy].length; ++i) newSS[i]=data[sx][sy][i];
        AxisOptions xAxis;
        AxisOptions yAxis;
        if(data[sx][sy].length>0) {
            xAxis=data[sx][sy][0].xAxis;
            yAxis=data[sx][sy][0].yAxis;
        } else {
            xAxis=axisList.get(0);
            yAxis=axisList.get((axisList.size()>1)?1:0);
        }
        newSS[newSS.length-1]=new DataSetInfo((DataPlotStyle)select,xAxis,yAxis);
        data[sx][sy]=newSS;
        syncGUI();
        setDataOnAxes();
        forceRedraw();
        fireRedraw();
    }

    private void removeFromDataList() {
        int toRemove=dataJList.getSelectedIndex();
        if(toRemove<0) return;
        int sx=mini.getSelectedX();
        int sy=mini.getSelectedY();
        DataSetInfo[] newSS=new DataSetInfo[data[sx][sy].length-1];
        for(int j=0; j<newSS.length; j++) {
            if(j<toRemove) newSS[j]=data[sx][sy][j];
            else newSS[j]=data[sx][sy][j+1];
        }
        data[sx][sy]=newSS;
        syncGUI();
        setDataOnAxes();
        forceRedraw();
        fireRedraw();
    }

    private void moveUpOnDataList() {
        int toMove=dataJList.getSelectedIndex();
        if(toMove<1) return;
        int sx=mini.getSelectedX();
        int sy=mini.getSelectedY();
        DataSetInfo tmp=data[sx][sy][toMove];
        data[sx][sy][toMove]=data[sx][sy][toMove-1];
        data[sx][sy][toMove-1]=tmp;
        syncGUI();
        forceRedraw();
        fireRedraw();
    }

    private void dataListSelectionMade() {
        if(synchingGUI) return;
        synchingGUI=true;
        int selected=dataJList.getSelectedIndex();
        if(selected<0) return;
        xAxisJList.setListData(axisList.toArray());
        xAxisJList.setSelectedIndex(axisList.indexOf(data[mini.getSelectedX()][mini.getSelectedY()][selected].xAxis));
        yAxisJList.setListData(axisList.toArray());
        yAxisJList.setSelectedIndex(axisList.indexOf(data[mini.getSelectedX()][mini.getSelectedY()][selected].yAxis));
        synchingGUI=false;
        forceRedraw();
        fireRedraw();
    }

    private void allAxisListSelectionMade() {
        if(synchingGUI) return;
        int which=allAxisJList.getSelectedIndex();
        axisPropertiesPanel.removeAll();
        if(which>=0) axisPropertiesPanel.add(axisList.get(which).getPropertiesPanel());
        axisPropertiesPanel.validate();
        axisPropertiesPanel.repaint();
    }

    private void createAxis() {
        axisList.add(new AxisOptions(this));
        allAxisJList.setListData(axisList.toArray());
        allAxisJList.setSelectedIndex(axisList.size()-1);
        if(dataJList.getSelectedIndex()>=0) {
            xAxisJList.setListData(axisList.toArray());
            xAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].xAxis,true);
        }
        if(dataJList.getSelectedIndex()>=0) {
            yAxisJList.setListData(axisList.toArray());
            yAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].yAxis,true);
        }
    }

    private void deleteAxis() {
        if(axisList.size()<=1) {
            JOptionPane.showMessageDialog(allAxisJList,"You can't delete the last axis.");
            return;
        }
        int which=allAxisJList.getSelectedIndex();
        if(which<0) return;
        AxisOptions removed=axisList.remove(which);
        for(int i=0; i<data.length; ++i) {
            for(int j=0; j<data[i].length; ++j) {
                for(int k=0; k<data[i][j].length; ++k) {
                    if(data[i][j][k].xAxis==removed) {
                        data[i][j][k].xAxis=axisList.get(0);
                    }
                    if(data[i][j][k].yAxis==removed) {
                        data[i][j][k].yAxis=axisList.get((axisList.size()>1)?1:0);
                    }
                }
            }
        }
        allAxisJList.setListData(axisList.toArray());
        allAxisJList.setSelectedIndex((which>0)?(which-1):0);
        if(dataJList.getSelectedIndex()>=0) {
            xAxisJList.setListData(axisList.toArray());
            xAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].xAxis,true);
        }
        if(dataJList.getSelectedIndex()>=0) {
            yAxisJList.setListData(axisList.toArray());
            yAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].yAxis,true);
        }
    }

    private void duplicateAxis() {
        int which=allAxisJList.getSelectedIndex();
        if(which<0) return;
        axisList.add(new AxisOptions(axisList.get(which),this));
        allAxisJList.setListData(axisList.toArray());
        allAxisJList.setSelectedIndex(which);
        if(dataJList.getSelectedIndex()>=0) {
            xAxisJList.setListData(axisList.toArray());
            xAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].xAxis,true);
        }
        if(dataJList.getSelectedIndex()>=0) {
            yAxisJList.setListData(axisList.toArray());
            yAxisJList.setSelectedValue(data[mini.getSelectedX()][mini.getSelectedY()][dataJList.getSelectedIndex()].yAxis,true);
        }
    }

    private void allDataListSelectionMade() {
        if(synchingGUI) return;
        int which=allDataJList.getSelectedIndex();
        dataPropertiesPanel.removeAll();
        if(which>=0) dataPropertiesPanel.add(plotDataList.get(which).getPropertiesPanel());
        dataPropertiesPanel.validate();
        dataPropertiesPanel.repaint();
    }

    private void createData() {
        Class<DataPlotStyle> dataClass=OptionsData.instance().selectPlotType(allDataJList);
        if(dataClass==null) return;
        try {
            Object[] constArgs={this};
            Class<?>[] constClasses={this.getClass()};
            Constructor<DataPlotStyle> constructor=dataClass.getConstructor(constClasses);
            plotDataList.add(constructor.newInstance(constArgs));
            allDataJList.setListData(plotDataList.toArray());
            allDataJList.setSelectedIndex(plotDataList.size()-1);
            legendDataJList.setListData(plotDataList.toArray());
        } catch(ClassCastException e) {
            JOptionPane.showMessageDialog(allDataJList,"The selected class is not of the proper type.  "+dataClass.getName()+" is not a DataPlotStyle.","Incorrect Type",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class is not a DataPlotStyle: "+dataClass.getName());
            e.printStackTrace();
        } catch(InstantiationException e) {
            JOptionPane.showMessageDialog(allDataJList,"The selected class does not have the proper constructor.  See Documentation.","Constructor Missing",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class does not have the proper constructor.  See Documentation.");
            e.printStackTrace();
        } catch(NoSuchMethodException e) {
            JOptionPane.showMessageDialog(allDataJList,"The selected class does not have the proper constructor.  See Documentation.","Constructor Missing",JOptionPane.ERROR_MESSAGE);
            System.err.println("The selected class does not have the proper constructor.  See Documentation.");
            e.printStackTrace();
        } catch(InvocationTargetException e) {
            JOptionPane.showMessageDialog(allDataJList,"The constructor threw an exception.","Exception in Constructor",JOptionPane.ERROR_MESSAGE);
            System.err.println("The constructor for the selected class threw an exception.");
            e.printStackTrace();
            System.err.println("Was wrapped around.");
            e.getTargetException().printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void deleteData() {
        if(plotDataList.size()<=1) {
            JOptionPane.showMessageDialog(allDataJList,"You can't delete the last data plot.");
            return;
        }
        int which=allDataJList.getSelectedIndex();
        if(which<0) return;
        DataPlotStyle removed=plotDataList.remove(which);
        for(int i=0; i<data.length; i++) {
            for(int j=0; j<data[i].length; j++) {
                int killCount=0;
                for(int k=0; k<data[i][j].length; k++) {
                    if(data[i][j][k].style==removed) {
                        killCount++;
                    }
                }
                if(killCount>0) {
                    DataSetInfo[] newDS=new DataSetInfo[data[i][j].length-killCount];
                    int dest=0;
                    for(int k=0; k<data[i][j].length; k++) {
                        if(data[i][j][k].style!=removed) {
                            newDS[dest]=data[i][j][k];
                            dest++;
                        } 
                    }
                    data[i][j]=newDS;
                }
            }
        }
        setDataOnAxes();
        syncGUI();
    }

    private void duplicateData() {
        int which=allDataJList.getSelectedIndex();
        if(which<0) return;
        plotDataList.add(plotDataList.get(which).copy(this));
        allDataJList.setListData(plotDataList.toArray());
        allDataJList.setSelectedIndex(plotDataList.size()-1);
        dataPropertiesPanel.removeAll();
        dataPropertiesPanel.add(plotDataList.get(plotDataList.size()-1).getPropertiesPanel());
    }

    private void legendDataListSelectionMade() {
        if(synchingGUI) return;
        int which=legendDataJList.getSelectedIndex();
        legendPropertiesPanel.removeAll();
        if(which>=0) {
            PlotLegend legendInfo=plotDataList.get(which).getLegendInformation();
            if(legendInfo!=null) legendPropertiesPanel.add(legendInfo.getPropertiesPanel());
        }
        legendPropertiesPanel.getParent().validate();
        legendPropertiesPanel.getParent().repaint();
        legendPropertiesPanel.validate();
        legendPropertiesPanel.repaint();
    }

    private void addPlotListener() {
        List<PlotListener> lpl=new LinkedList<PlotListener>();
        List<GraphElement> elements=GraphPanel.instance().getElements();
        for(GraphElement ge:elements) {
            if(ge instanceof PlotListener && !listeners.contains(ge)) {
                lpl.add((PlotListener)ge);
            }
        }
        if(lpl.isEmpty()) {
            JOptionPane.showMessageDialog(propPanel,"There are no listeners to add.");
            return;
        }
        PlotListener pl=(PlotListener)JOptionPane.showInputDialog(propPanel,"Which plot listener do you want to add?","Add Plot Listener",JOptionPane.QUESTION_MESSAGE,null,lpl.toArray(),lpl.get(0));
        if(pl!=null) {
            listeners.add(new ListenerCellPair(pl,listenerMini.getSelectedX(),listenerMini.getSelectedY()));
        }
        syncGUI();
    }

    private void removePlotListener() {
        int index=listenerJList.getSelectedIndex();
        if(index<0) return;
        listeners.remove(listenerJList.getSelectedValue());
        syncGUI();
    }

    private void addPlotLabel() {
        if(labelTypes==null) initLabelTypes();
        PlotLabel type=(PlotLabel)JOptionPane.showInputDialog(propPanel,"What type of label do you want to add?","Add Label",
                JOptionPane.QUESTION_MESSAGE,null,labelTypes,labelTypes[0]);
        if(type==null) return;
        labels.add(type.create(labelMini.getSelectedX(),labelMini.getSelectedY()));
        syncGUI();
    }

    private void removePlotLabel() {
        int index=labelJList.getSelectedIndex();
        if(index<0) return;
        labels.remove(labelJList.getSelectedValue());
        syncGUI();
    }
    
    private void duplicatePlotLabel() {
        int index=labelJList.getSelectedIndex();
        if(index<0) return;
        labels.add(copyLabel((PlotLabel)labelJList.getSelectedValue()));
        syncGUI();
    }

    private void moveUpPlotLabel() {
        int index=labelJList.getSelectedIndex();
        if(index<0) return;
        PlotLabel label=(PlotLabel)labelJList.getSelectedValue();
        if(label.cy<data[0].length-1) {
            label.cy++;
            syncGUI();
        }
    }

    private void moveDownPlotLabel() {
        int index=labelJList.getSelectedIndex();
        if(index<0) return;
        PlotLabel label=(PlotLabel)labelJList.getSelectedValue();
        if(label.cy>0) {
            label.cy--;
            syncGUI();
        }
    }

    private void moveLeftPlotLabel() {
        int index=labelJList.getSelectedIndex();
        if(index<0) return;
        PlotLabel label=(PlotLabel)labelJList.getSelectedValue();
        if(label.cx>0) {
            label.cx--;
            syncGUI();
        }
    }

    private void moveRightPlotLabel() {
        int index=labelJList.getSelectedIndex();
        if(index<0) return;
        PlotLabel label=(PlotLabel)labelJList.getSelectedValue();
        if(label.cx<data.length-1) {
            label.cx++;
            syncGUI();
        }
    }

    // I had put a copy method in the PlotLabel class, but this caused problems because it is an
    // inner class.  Instead I'm using reflection to call a copy constructor.
    private PlotLabel copyLabel(PlotLabel label) {
        try {
            Constructor<? extends PlotLabel> con=label.getClass().getConstructor(PlotArea2D.class,label.getClass());
            return con.newInstance(this,label);
        } catch(NoSuchMethodException e) {
            e.printStackTrace();
        } catch(InstantiationException e) {
            e.printStackTrace();
        } catch(InvocationTargetException e) {
            e.printStackTrace();
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Couldn't match label type.");
    }

    private void initLabelTypes() {
        labelTypes=new PlotLabel[]{new PlotText(),new PlotLine(),new PlotRect(),new PlotOval()};        
    }

    private PlotSpec parent;

    private EditableDouble x;
    private EditableDouble y;
    private EditableDouble width;
    private EditableDouble height;

    private DataSetInfo[][][] data;
    private double[] xRelativeSize={1};
    private double[] yRelativeSize={1};

    private ArrayList<AxisOptions> axisList = new ArrayList<AxisOptions>();
    private ArrayList<DataPlotStyle> plotDataList = new ArrayList<DataPlotStyle>();

    private EditableDouble legendX;
    private EditableDouble legendY;
    private EditableDouble legendWidth;
    private EditableDouble legendHeight;

    private List<ListenerCellPair> listeners=new LinkedList<ListenerCellPair>();
    private List<PlotLabel> labels=new LinkedList<PlotLabel>();

    private transient JComponent propPanel;
    private transient Image img;
    private transient PlotAreaMiniature mini;
    private transient JTextField relativeWidthField;
    private transient JTextField relativeHeightField;
    private transient boolean useBuffer=false;
    private transient JList dataJList;
    private transient JList xAxisJList;
    private transient JList yAxisJList;
    private transient JList allAxisJList;
    private transient JList allDataJList;
    private transient JList legendDataJList;
    private transient JList listenerJList;
    private transient PlotAreaMiniature listenerMini;
    private transient JList labelJList;
    private transient PlotAreaMiniature labelMini;
    private transient JPanel axisPropertiesPanel;
    private transient JPanel dataPropertiesPanel;
    private transient JPanel legendPropertiesPanel;

    private transient double imgLeft,imgTop;
    private transient Rectangle2D gridBounds;
    private transient Rectangle2D[][] cellBounds;
    private transient boolean lastInside=false;

    private transient boolean synchingGUI=false;

    private static final long serialVersionUID=4609871246285l;

    private static class DataSetInfo implements Serializable {
        public DataSetInfo(DataPlotStyle s,AxisOptions x,AxisOptions y) {
            style=s;
            xAxis=x;
            yAxis=y;
        }
        @Override
        public String toString() {
            return style.toString();
        }
        private DataPlotStyle style;
        private AxisOptions xAxis,yAxis;
        private static final long serialVersionUID = -401205553990383048L;
    }

    private class PlotAreaMiniature extends JPanel implements MouseListener,MouseMotionListener {
        public PlotAreaMiniature() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100,100);
        }

        public int getSelectedX() {
            return selectedX;
        }

        public int getSelectedY() {
            return selectedY;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            selectedX=e.getX()*data.length/getWidth();
            selectedY=(data[0].length-1)-e.getY()*data[0].length/getHeight();
            if(selectedX<0) selectedX=0;
            if(selectedX>=data.length) selectedX=data.length;
            if(selectedY<0) selectedY=0;
            if(selectedY>=data[0].length) selectedY=data[0].length;
            repaint();
            syncGUI();
        }

        @Override
        public void mouseReleased(MouseEvent e) { }

        @Override
        public void mouseClicked(MouseEvent e) { }

        @Override
        public void mouseEntered(MouseEvent e) { }

        @Override
        public void mouseExited(MouseEvent e) { }

        @Override
        public void mouseMoved(MouseEvent e) { }

        @Override
        public void mouseDragged(MouseEvent e) { }

        @Override
        protected void paintComponent(Graphics gr) {
            if(selectedX<0) selectedX=0;
            if(selectedX>=data.length) selectedX=data.length;
            if(selectedY<0) selectedY=0;
            if(selectedY>=data[0].length) selectedY=data[0].length;
            Graphics2D g=(Graphics2D)gr;
            int numX,numY;
            numX=data.length;
            numY=data[0].length;
            double cellWidth=(double)getWidth()/numX;
            double cellHeight=(double)getHeight()/numY;
            for(int i=0; i<numX; i++) {
                for(int j=0; j<numY; j++) {
                    Rectangle2D rect=new Rectangle2D.Double(i*cellWidth,(numY-1-j)*cellHeight,cellWidth,cellHeight);
                    int selectDir=0;
                    if(i==selectedX) selectDir++;
                    if(j==selectedY) selectDir++;
                    if(selectDir==0) {
                        g.setPaint(Color.white);
                    } else if(selectDir==1) {
                        g.setPaint(Color.lightGray);
                    } else {
                        g.setPaint(Color.darkGray);
                    }
                    g.fill(rect);
                    g.setPaint(Color.black);
                    g.draw(rect);
                }
            }
        }

        private int selectedX;
        private int selectedY;
        private static final long serialVersionUID=2574673683568l;
    }

    private class ClickPosition {
        public ClickPosition(int cx,int cy) {
            if(gridBounds==null) return;
            inside=false;
            for(int i=0; i<cellBounds.length && !inside; ++i) {
                for(int j=0; j<cellBounds[i].length && !inside; ++j) {
                    if(cellBounds[i][j].contains(cx,cy)) {
                        primaryCell=i;
                        secondaryCell=j;
                        inside=true;
                    }
                }
            }
            if(!inside || data[primaryCell][secondaryCell].length<1) {
                return;
            }
            double offsetPrimary,offsetSecondary;
            offsetPrimary=cx-cellBounds[primaryCell][secondaryCell].getMinX();
            offsetSecondary=cellBounds[primaryCell][secondaryCell].getMaxY()-cy;
            PlotTransform trans=new PlotTransform(data[primaryCell][secondaryCell][0].xAxis,data[primaryCell][secondaryCell][0].yAxis,cellBounds[primaryCell][secondaryCell]);
            Point2D p=trans.inverseTransform(offsetPrimary,offsetSecondary);
            v1=p.getX();
            v2=p.getY();
        }
        public boolean inside=true;
        public double v1,v2;
        public int primaryCell,secondaryCell;
    }

    private class ListenerCellPair implements Serializable,FillUser {
        public ListenerCellPair(PlotListener pListener,int cellX,int cellY) {
            pl=pListener;
            cx=cellX;
            cy=cellY;
        }
        public ListenerCellPair(ListenerCellPair c) {
            pl=c.pl;
            cx=c.cx;
            cy=c.cy;
            colorRegion=new EditableBoolean(c.colorRegion.getValue());
            fill=new FillOptions(this,c.fill.getColor());
        }
        @Override
        public String toString() {
            return pl.toString();
        }
        @Override
        public void applyFill(FillOptions fs) {
            fireRedraw();
        }
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel innerPanel=new JPanel(new GridLayout(3,1));
                innerPanel.add(colorRegion.getCheckBox("Color in selected region?",null));
                JButton button=new JButton("Select Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fill.edit();
                    }
                });
                innerPanel.add(button);
                String[] options={"Left","Center","Right"};
                final int[] buttonNumbers={MouseEvent.BUTTON1,MouseEvent.BUTTON2,MouseEvent.BUTTON3};
                final JComboBox buttonCombo=new JComboBox(options);
                int bIndex=0;
                while(buttonUsed!=buttonNumbers[bIndex] && bIndex<buttonNumbers.length) bIndex++;
                if(bIndex==buttonNumbers.length) {
                    bIndex=0;
                    buttonUsed=MouseEvent.BUTTON1;
                }
                buttonCombo.setSelectedItem(bIndex);
                buttonCombo.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonUsed=buttonNumbers[buttonCombo.getSelectedIndex()];
                    }
                });
                innerPanel.add(buttonCombo);
                propPanel.add(innerPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        public PlotListener pl;
        public int cx,cy;
        public EditableBoolean colorRegion=new EditableBoolean(true);
        public FillOptions fill=new FillOptions(this,new Color(1.0f,0.3f,0.3f,0.2f));
        public int buttonUsed=MouseEvent.BUTTON1;
        private static final long serialVersionUID=698273561462437853l;

        private transient JPanel propPanel;
    }

    private transient PlotLabel[] labelTypes;
    private abstract class PlotLabel implements Serializable {
        public abstract PlotLabel create(int x,int y);
        public abstract JPanel getPropertiesPanel();
        public abstract void draw(Graphics2D g,Rectangle2D bounds, float fontScale,PlotTransform trans);
        public int cx,cy;
        public EditableBoolean plotBefore=new EditableBoolean(false);
        private static final long serialVersionUID=2670619833533401488l;
    }


    private class PlotText extends PlotLabel implements FillUser,FontUser {
        public PlotText() {}
        public PlotText(int x,int y) {
            cx=x;
            cy=y;
        }
        @SuppressWarnings("unused") public PlotText(PlotText c) {
            text=new FormattedString(c.text.getValue());
            cx=c.cx;
            cy=c.cy;
            plotBefore=new EditableBoolean(c.plotBefore.getValue());
            positionStyle=c.positionStyle;
            xValue=new DataFormula(c.xValue);
            yValue=new DataFormula(c.yValue);
            fill=new FillOptions(this,c.fill.getColor());
            font=new FontOptions(this,c.font.getFont());
        }
        @Override
        public PlotLabel create(int x,int y) {
            return new PlotText(x,y);
        }
        @Override
        public String toString() {
            return text.getValue();
        }
        @Override
        public void applyFill(FillOptions fs) {
            fireRedraw();
        }
        @Override
        public void applyFont(FontOptions options) {
            fireRedraw();
        }
        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(8,1));
                JPanel innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Label Text"),BorderLayout.WEST);
                innerPanel.add(text.getTextField(new FormattedString.Listener() {
                    @Override
                    public void valueChanged() { if(labelJList!=null) labelJList.repaint(); }
                }),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                northPanel.add(plotBefore.getCheckBox("Draw under data plot?",null));
                if(styles==null) initStyles();
                final JComboBox styleComboBox=new JComboBox(styles);
                styleComboBox.setSelectedIndex(positionStyle);
                styleComboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        positionStyle=styleComboBox.getSelectedIndex();
                    }
                });
                northPanel.add(styleComboBox);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("X Offset/Position"),BorderLayout.WEST);
                innerPanel.add(xValue.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Y Offset/Position"),BorderLayout.WEST);
                innerPanel.add(yValue.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                JButton button=new JButton("Edit Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fill.edit(); }
                });
                northPanel.add(button);
                button=new JButton("Edit Font");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { font.edit(); }
                });
                northPanel.add(button);
                button=new JButton("Apply Changes");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fireRedraw(); }
                });
                northPanel.add(button);
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        @Override
        public void draw(Graphics2D g,Rectangle2D bounds, float fontScale,PlotTransform trans) {
            if(styles==null) initStyles();
            styles[positionStyle].draw(g,text,xValue.valueOf(parent.getPlot(),0, 0),yValue.valueOf(parent.getPlot(),0, 0),cx,cy,
                    bounds,fill.getColor(),font.getFont(fontScale),trans);
        }
        private void initStyles() {
            styles=new PositionStyle[]{new FloatingStyle(),new TopLeftStyle(),new TopCenterStyle(),new TopRightStyle(),
                    new CenterLeftStyle(),new CenterStyle(),new CenterRightStyle(),
                    new BottomLeftStyle(),new BottomCenterStyle(),new BottomRightStyle()};
        }

        private FormattedString text=new FormattedString("Text");
        private int positionStyle;
        private DataFormula xValue=new DataFormula("0.0");
        private DataFormula yValue=new DataFormula("0.0");
        private FillOptions fill=new FillOptions(this,Color.BLACK);
        private FontOptions font=new FontOptions(this,OptionsData.instance().getLabelFont());
        private static final long serialVersionUID=436098237690837237l;

        private transient JPanel propPanel;
    }

    private class PlotLine extends PlotLabel implements FillUser {
        public PlotLine() {}
        public PlotLine(int x,int y) {
            cx=x;
            cy=y;
        }
        @SuppressWarnings("unused") public PlotLine(PlotLine c) {
            cx=c.cx;
            cy=c.cy;
            plotBefore=new EditableBoolean(c.plotBefore.getValue());
            xValue1=new DataFormula(c.xValue1);
            yValue1=new DataFormula(c.yValue1);
            xValue2=new DataFormula(c.xValue2);
            yValue2=new DataFormula(c.yValue2);
            fill=new FillOptions(this,c.fill.getColor());
            strokeOptions=new StrokeOptions(null,c.strokeOptions);
            drawArrow=new EditableBoolean(c.drawArrow.getValue());
            headSize=new EditableDouble(c.headSize.getValue());
        }
        @Override
        public PlotLabel create(int x,int y) {
            return new PlotLine(x,y);
        }
        @Override
        public String toString() {
            return "Line";
        }
        @Override
        public void applyFill(FillOptions fs) {
            fireRedraw();
        }
        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(10,1));
                JPanel innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("X1 Position"),BorderLayout.WEST);
                innerPanel.add(xValue1.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Y1 Position"),BorderLayout.WEST);
                innerPanel.add(yValue1.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("X2 Position"),BorderLayout.WEST);
                innerPanel.add(xValue2.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Y2 Position"),BorderLayout.WEST);
                innerPanel.add(yValue2.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                northPanel.add(plotBefore.getCheckBox("Draw under data plot?",null));
                JButton button=new JButton("Edit Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fill.edit(); }
                });
                northPanel.add(button);
                button=new JButton("Edit Stroke");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { strokeOptions.edit(); }
                });
                northPanel.add(button);
                northPanel.add(drawArrow.getCheckBox("Draw arrow?",null));
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Arrow Head Size"),BorderLayout.WEST);
                innerPanel.add(headSize.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                button=new JButton("Apply Changes");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fireRedraw(); }
                });
                northPanel.add(button);
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        @Override
        public void draw(Graphics2D g,Rectangle2D bounds, float fontScale,PlotTransform trans) {
            Point2D p1=trans.transform(xValue1.valueOf(parent.getPlot(),0, 0),yValue1.valueOf(parent.getPlot(),0, 0));
            Point2D p2=trans.transform(xValue2.valueOf(parent.getPlot(),0, 0),yValue2.valueOf(parent.getPlot(),0, 0));
            double dx1=p1.getX();
            double dy1=p1.getY();
            double dx2=p2.getX();
            double dy2=p2.getY();
            g.setPaint(fill.getColor());
            g.setStroke(strokeOptions.getStroke());
            Line2D line=new Line2D.Double(dx1,dy1,dx2,dy2);
            g.draw(line);
            if(drawArrow.getValue()) {
                double dx=line.getX2()-line.getX1(); 
                double dy=line.getY2()-line.getY1();
                double len=Math.sqrt(dx*dx+dy*dy);
                dx*=-headSize.getValue()/len;
                dy*=-headSize.getValue()/len;
                double nx=dy*0.5;
                double ny=-dx*0.5;
                GeneralPath gp=new GeneralPath();
                gp.moveTo((float)line.getX2(),(float)line.getY2());
                gp.lineTo((float)(line.getX2()+dx+nx),(float)(line.getY2()+dy+ny));
                gp.lineTo((float)(line.getX2()+dx-nx),(float)(line.getY2()+dy-ny));
                gp.closePath();
                g.fill(gp);
            }
        }

        private DataFormula xValue1=new DataFormula("0.0");
        private DataFormula yValue1=new DataFormula("0.0");
        private DataFormula xValue2=new DataFormula("1.0");
        private DataFormula yValue2=new DataFormula("1.0");
        private FillOptions fill=new FillOptions(this,Color.BLACK);
        private StrokeOptions strokeOptions=new StrokeOptions(1,null);
        private EditableBoolean drawArrow=new EditableBoolean(false);
        private EditableDouble headSize=new EditableDouble(10.0);
        private static final long serialVersionUID=436098237690837237l;

        private transient JPanel propPanel;
    }

    private class PlotRect extends PlotLabel implements FillUser {
        public PlotRect() {}
        public PlotRect(int x,int y) {
            cx=x;
            cy=y;
        }
        @SuppressWarnings("unused") public PlotRect(PlotRect c) {
            cx=c.cx;
            cy=c.cy;
            plotBefore=new EditableBoolean(c.plotBefore.getValue());
            xValue=new DataFormula(c.xValue);
            yValue=new DataFormula(c.yValue);
            width=new DataFormula(c.width);
            height=new DataFormula(c.height);
            fill=new FillOptions(this,c.fill.getColor());
            line=new FillOptions(this,c.line.getColor());
            strokeWidth=new EditableDouble(c.strokeWidth.getValue());
        }
        @Override
        public PlotLabel create(int x,int y) {
            return new PlotRect(x,y);
        }
        @Override
        public String toString() {
            return "Rectangle";
        }
        @Override
        public void applyFill(FillOptions fs) {
            fireRedraw();
        }
        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(9,1));
                JPanel innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Min X Position"),BorderLayout.WEST);
                innerPanel.add(xValue.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Min Y Position"),BorderLayout.WEST);
                innerPanel.add(yValue.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Width"),BorderLayout.WEST);
                innerPanel.add(width.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Height"),BorderLayout.WEST);
                innerPanel.add(height.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                northPanel.add(plotBefore.getCheckBox("Draw under data plot?",null));
                JButton button=new JButton("Edit Line Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { line.edit(); }
                });
                northPanel.add(button);
                button=new JButton("Edit Fill Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fill.edit(); }
                });
                northPanel.add(button);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Stroke Width"),BorderLayout.WEST);
                innerPanel.add(strokeWidth.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                button=new JButton("Apply Changes");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fireRedraw(); }
                });
                northPanel.add(button);
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        @Override
        public void draw(Graphics2D g,Rectangle2D bounds, float fontScale,PlotTransform trans) {
            Point2D p1=trans.transform(xValue.valueOf(parent.getPlot(),0, 0),yValue.valueOf(parent.getPlot(),0, 0));
            Point2D p2=trans.transform(xValue.valueOf(parent.getPlot(),0, 0)+width.valueOf(parent.getPlot(),0, 0),yValue.valueOf(parent.getPlot(),0, 0)+height.valueOf(parent.getPlot(),0, 0));
            double dx=Math.min(p1.getX(),p2.getX());
            double dy=Math.min(p1.getY(),p2.getY());
            double w=Math.max(p1.getX(),p2.getX())-dx;
            double h=Math.max(p1.getY(),p2.getY())-dy;
            Rectangle2D rect=new Rectangle.Double(dx,dy,w,h);
            g.setPaint(fill.getColor());
            g.fill(rect);
            g.setPaint(line.getColor());
            g.setStroke(new BasicStroke((float)strokeWidth.getValue()));
            g.draw(rect);
        }

        private DataFormula xValue=new DataFormula("0.0");
        private DataFormula yValue=new DataFormula("0.0");
        private DataFormula width=new DataFormula("1.0");
        private DataFormula height=new DataFormula("1.0");
        private FillOptions fill=new FillOptions(this,new Color(0,0,0,0));
        private FillOptions line=new FillOptions(this,Color.BLACK);
        private EditableDouble strokeWidth=new EditableDouble(1.0);
        private static final long serialVersionUID=436098237690837237l;

        private transient JPanel propPanel;
    }

    private class PlotOval extends PlotLabel implements FillUser {
        public PlotOval() {}
        public PlotOval(int x,int y) {
            cx=x;
            cy=y;
        }
        @SuppressWarnings("unused") public PlotOval(PlotOval c) {
            cx=c.cx;
            cy=c.cy;
            plotBefore=new EditableBoolean(c.plotBefore.getValue());
            xValue=new DataFormula(c.xValue);
            yValue=new DataFormula(c.yValue);
            width=new DataFormula(c.width);
            height=new DataFormula(c.height);
            fill=new FillOptions(this,c.fill.getColor());
            line=new FillOptions(this,c.line.getColor());
            strokeWidth=new EditableDouble(c.strokeWidth.getValue());
        }
        @Override
        public PlotLabel create(int x,int y) {
            return new PlotOval(x,y);
        }
        @Override
        public String toString() {
            return "Oval";
        }
        @Override
        public void applyFill(FillOptions fs) {
            fireRedraw();
        }
        @Override
        public JPanel getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(9,1));
                JPanel innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Min X Position"),BorderLayout.WEST);
                innerPanel.add(xValue.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Min Y Position"),BorderLayout.WEST);
                innerPanel.add(yValue.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Width"),BorderLayout.WEST);
                innerPanel.add(width.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Height"),BorderLayout.WEST);
                innerPanel.add(height.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                northPanel.add(plotBefore.getCheckBox("Draw under data plot?",null));
                JButton button=new JButton("Edit Line Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { line.edit(); }
                });
                northPanel.add(button);
                button=new JButton("Edit Fill Color");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fill.edit(); }
                });
                northPanel.add(button);
                innerPanel=new JPanel(new BorderLayout());
                innerPanel.add(new JLabel("Stroke Width"),BorderLayout.WEST);
                innerPanel.add(strokeWidth.getTextField(null),BorderLayout.CENTER);
                northPanel.add(innerPanel);
                button=new JButton("Apply Changes");
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { fireRedraw(); }
                });
                northPanel.add(button);
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        @Override
        public void draw(Graphics2D g,Rectangle2D bounds, float fontScale,PlotTransform trans) {
            Point2D p1=trans.transform(xValue.valueOf(parent.getPlot(),0, 0),yValue.valueOf(parent.getPlot(),0, 0));
            Point2D p2=trans.transform(xValue.valueOf(parent.getPlot(),0, 0)+width.valueOf(parent.getPlot(),0, 0),yValue.valueOf(parent.getPlot(),0, 0)+height.valueOf(parent.getPlot(),0, 0));
            double dx=Math.min(p1.getX(),p2.getX());
            double dy=Math.min(p1.getY(),p2.getY());
            double w=Math.max(p1.getX(),p2.getX())-dx;
            double h=Math.max(p1.getY(),p2.getY())-dy;
            Ellipse2D ell=new Ellipse2D.Double(dx,dy,w,h);
            g.setPaint(fill.getColor());
            g.fill(ell);
            g.setPaint(line.getColor());
            g.setStroke(new BasicStroke((float)strokeWidth.getValue()));
            g.draw(ell);
        }

        private DataFormula xValue=new DataFormula("0.0");
        private DataFormula yValue=new DataFormula("0.0");
        private DataFormula width=new DataFormula("1.0");
        private DataFormula height=new DataFormula("1.0");
        private FillOptions fill=new FillOptions(this,new Color(0,0,0,0));
        private FillOptions line=new FillOptions(this,Color.BLACK);
        private EditableDouble strokeWidth=new EditableDouble(1.0);
        private static final long serialVersionUID=436098237690837237l;

        private transient JPanel propPanel;
    }

    private transient PositionStyle styles[];
    private interface PositionStyle extends Serializable {
        void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans);
    }
    private class TopLeftStyle implements PositionStyle {
        @Override
        public String toString() { return "Top Left Corner"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getMinX()+x),(float)(bounds.getMinY()+y-strBnds.getMinY()),getSink());
        }
        private static final long serialVersionUID=259087237348235732l;
    }
    private class TopCenterStyle implements PositionStyle {
        @Override
        public String toString() { return "Top Center Side"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getCenterX()-strBnds.getWidth()*0.5),(float)(bounds.getMinY()+y-strBnds.getMinY()),getSink());
        }
        private static final long serialVersionUID=2235498475609757l;
    }
    private class TopRightStyle implements PositionStyle {
        @Override
        public String toString() { return "Top Right Corner"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getMaxX()-strBnds.getWidth()-x),(float)(bounds.getMinY()+y-strBnds.getMinY()),parent.getPlot());
        }
        private static final long serialVersionUID=9459722776688l;
    }
    private class CenterLeftStyle implements PositionStyle {
        @Override
        public String toString() { return "Center Left Side"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getMinX()+x),(float)(bounds.getCenterY()),getSink());
        }
        private static final long serialVersionUID=119422334680l;
    }
    private class CenterStyle implements PositionStyle {
        @Override
        public String toString() { return "Center"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getCenterX()-strBnds.getWidth()*0.5),(float)(bounds.getCenterY()),getSink());
        }
        private static final long serialVersionUID=565356745789467l;
    }
    private class CenterRightStyle implements PositionStyle {
        @Override
        public String toString() { return "Center Right Side"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getMaxX()-strBnds.getWidth()-x),(float)(bounds.getCenterY()),getSink());
        }
        private static final long serialVersionUID=4574723467348786l;
    }
    private class BottomLeftStyle implements PositionStyle {
        @Override
        public String toString() { return "Bottom Left Corner"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getMinX()+x),(float)(bounds.getMaxY()-strBnds.getMaxY()-y),getSink());
        }
        private static final long serialVersionUID=768458346262347358l;
    }
    private class BottomCenterStyle implements PositionStyle {
        @Override
        public String toString() { return "Bottom Center Side"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getCenterX()-strBnds.getWidth()*0.5),(float)(bounds.getMaxY()-strBnds.getMaxY()-y),parent.getPlot());
        }
        private static final long serialVersionUID=149082375427579l;
    }
    private class BottomRightStyle implements PositionStyle {
        @Override
        public String toString() { return "Bottom Right Corner"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            Rectangle2D strBnds=text.getBounds(font,getSink());
            g.setFont(font);
            g.setPaint(fill);
            text.draw(g,(float)(bounds.getMaxX()-strBnds.getWidth()-x),(float)(bounds.getMaxY()-strBnds.getMaxY()-y),parent.getPlot());
        }
        private static final long serialVersionUID=9032587620983762375l;
    }
    private class FloatingStyle implements PositionStyle {
        @Override
        public String toString() { return "Point Position"; }
        @Override
        public void draw(Graphics2D g,FormattedString text,double x,double y,int cx,int cy,Rectangle2D bounds,Paint fill,Font font,PlotTransform trans) {
            if(data[cx][cy].length<1) return;
            g.setFont(font);
            g.setPaint(fill);
            Point2D p1=trans.transform(x,y);
            double dx=p1.getX();
            double dy=p1.getY();
            text.draw(g,(float)dx,(float)dy,getSink());
        }
        private static final long serialVersionUID=9459722776688l;
    }
}
