/*
 * Created on Jun 11, 2005
 */
package edu.swri.swiftvis.filters;


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.PlotListener;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.ReduceLoopBody;
import edu.swri.swiftvis.util.ThreadHandler;

/**
 * This is a filter that takes a single source and selects elements in a certain region.
 * This filter type implements PlotListener so it takes inputs from the user clicking on
 * plots or key presses on the plots.  These are used to relocate the the region that is
 * selected.  The filter uses two formulas and uses the values of those as Cartesian
 * coordinates for doing selection on rectangular regions.  The clicks can be
 * set to either relocate the view region, or to resize it.
 * 
 * @author Mark Lewis
 */
public class RegionSelectionFilter extends AbstractSingleSourceFilter implements PlotListener {
    public RegionSelectionFilter() {
    }
    
    public RegionSelectionFilter(RegionSelectionFilter c,List<GraphElement> l) {
        super(c,l);
        f1=new DataFormula(c.f1);
        f2=new DataFormula(c.f2);
        center1=new EditableDouble(c.center1.getValue());
        size1=new EditableDouble(c.size1.getValue());
        center2=new EditableDouble(c.center2.getValue());
        size2=new EditableDouble(c.size2.getValue());
        zoomFactor=new EditableDouble(c.zoomFactor.getValue());
        clickZoom=new EditableBoolean(c.clickZoom.getValue());
        redoOnDrag=new EditableBoolean(c.redoOnDrag.getValue());
    }
    
    @Override
    public void addInput(DataSource in) {
        super.addInput(in);
        redoDataBounds();
    }
    
    @Override
    public void removeInput(DataSource in) {
        super.removeInput(in);
        redoDataBounds();        
    }
    
    @Override
    protected boolean doingInThreads() {
		return true;
	}

    @Override
    protected void redoAllElements() {
        if(input==null || input.getNumElements(0)==0) return;
        int[] indexBounds=f1.getSafeElementRange(this, 0);
        int[] b2=f2.getSafeElementRange(this, 0);
        if(b2[0]>indexBounds[0]) indexBounds[0]=b2[0];
        if(b2[1]<indexBounds[1]) indexBounds[1]=b2[1];
        DataFormula.checkRangeSafety(indexBounds,this);
        try {
            f1.valueOf(this,0, indexBounds[0]);
        } catch(ArrayIndexOutOfBoundsException e) {
            f1.setFormula("1");
        }
        try {
            f2.valueOf(this,0, indexBounds[0]);
        } catch(ArrayIndexOutOfBoundsException e) {
            f2.setFormula("1");
        }
        //  parallel
        final ArrayList<ArrayList<DataElement>> vects=new ArrayList<ArrayList<DataElement>>();
        for (int i=0; i<ThreadHandler.instance().getNumThreads(); i++) {
        	vects.add(new ArrayList<DataElement>());
        }
        final DataFormula[] f1s=new DataFormula[ThreadHandler.instance().getNumThreads()];
        final DataFormula[] f2s=new DataFormula[ThreadHandler.instance().getNumThreads()];
        // create ReduceLoopBody array
        ReduceLoopBody[] loops=new ReduceLoopBody[vects.size()];
        for (int i=0; i<loops.length; i++) {
        	final int index=i;
        	f1s[i]=f1.getParallelCopy();
        	f2s[i]=f2.getParallelCopy();
        	loops[i]=new ReduceLoopBody() {
        		@Override
                public void execute(int start, int end) {
        			ArrayList<DataElement> data=vects.get(index);
        			DataFormula ff1=f1s[index];
        			DataFormula ff2=f2s[index];
        			for (int j=start; j<end; j++) {
        				double v1=ff1.valueOf(RegionSelectionFilter.this,0, j);
                        double v2=ff2.valueOf(RegionSelectionFilter.this,0, j);
                        double s1=size1.getValue()*0.5;
                        double s2=size2.getValue()*0.5;
                        if(v1>=center1.getValue()-s1 && v1<=center1.getValue()+s1 &&
                                v2>=center2.getValue()-s2 && v2<=center2.getValue()+s2) {
                            data.add(input.getElement(j, 0));
                        }
        			}
        		}
        	};
        }
        ThreadHandler.instance().chunkedForLoop(this,indexBounds[0],indexBounds[1],loops);
        // merge lists
        int size=0;
        for (int i=0; i<vects.size(); i++) {
        	size+=vects.get(i).size();
        }
        dataVect.get(0).ensureCapacity(size);
        for (int i=0; i<vects.size(); i++) {
        	dataVect.get(0).addAll(vects.get(i));
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel innerPanel=new JPanel(new GridLayout(11,1));
        JPanel innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Primary Formula"),BorderLayout.WEST);
        innerPanel2.add(f1.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Primary Center"),BorderLayout.WEST);
        innerPanel2.add(center1.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Primary Size"),BorderLayout.WEST);
        innerPanel2.add(size1.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Secondary Formula"),BorderLayout.WEST);
        innerPanel2.add(f2.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Secondary Center"),BorderLayout.WEST);
        innerPanel2.add(center2.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Secondary Size"),BorderLayout.WEST);
        innerPanel2.add(size2.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        JButton button=new JButton("Apply Bounds");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        innerPanel.add(button);
        button=new JButton("Reset Bounds");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redoDataBounds();
            }
        });
        innerPanel.add(button);
        innerPanel.add(clickZoom.getCheckBox("Zoom region on click?",null));
        innerPanel2=new JPanel(new BorderLayout());
        innerPanel2.add(new JLabel("Zoom Factor"),BorderLayout.WEST);
        innerPanel2.add(zoomFactor.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(innerPanel2);
        innerPanel.add(redoOnDrag.getCheckBox("Redo data on drag?",null));
        panel.add(innerPanel,BorderLayout.NORTH);
        innerPanel=new JPanel(new GridLayout(1,2));
        v1Label=new JLabel("0.0");
        innerPanel.add(v1Label);
        v2Label=new JLabel("0.0");
        innerPanel.add(v2Label);
        panel.add(innerPanel,BorderLayout.SOUTH);
        propPanel.add("Region Options",panel);
    }

    @Override
    public void mousePressed(double v1, double v2, MouseEvent e) {
        startV1=v1;
        startV2=v2;
    }

    @Override
    public void mouseReleased(double v1, double v2, MouseEvent e) {
        if(v1!=startV1 && v2!=startV2) {
            center1.setValue((v1+startV1)*0.5);
            size1.setValue(Math.abs(startV1-v1));
            center2.setValue((v2+startV2)*0.5);
            size2.setValue(Math.abs(startV2-v2));
            abstractRedoAllElements();                
        }
    }

    @Override
    public void mouseClicked(double v1, double v2, MouseEvent e) {
    	if(v1==startV1 || v2==startV2) {
    		center1.setValue(v1);
    		center2.setValue(v2);
	    	if(clickZoom.getValue()) {
	            if((e.getModifiers()&InputEvent.BUTTON1_MASK)>0) {
	                size1.setValue(size1.getValue()/zoomFactor.getValue());
	                size2.setValue(size2.getValue()/zoomFactor.getValue());
	            } else {
	                size1.setValue(size1.getValue()*zoomFactor.getValue());
	                size2.setValue(size2.getValue()*zoomFactor.getValue());                    
	            }
	        }
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
    public void mouseDragged(double v1, double v2, MouseEvent e) {
        if(v1Label!=null) {
            v1Label.setText(Double.toString(v1));
            v2Label.setText(Double.toString(v2));
        }
        if(redoOnDrag.getValue() && (v1!=startV1 && v2!=startV2)) {
            center1.setValue((v1+startV1)*0.5);
            size1.setValue(Math.abs(startV1-v1));
            center2.setValue((v2+startV2)*0.5);
            size2.setValue(Math.abs(startV2-v2));
            abstractRedoAllElements();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode()==KeyEvent.VK_R) {
            redoDataBounds();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public Shape getSelectionRegion(PlotTransform trans) {
        double s1=size1.getValue()*0.5;
        double s2=size2.getValue()*0.5;
        Point2D p1=trans.transform(center1.getValue()-s1,center2.getValue()-s2);
        Point2D p2=trans.transform(center1.getValue()+s1,center2.getValue()+s2);
        Rectangle2D.Double ret=new Rectangle2D.Double(p1.getX(),p1.getY(),0,0);
        ret.add(p2);
        return ret;
    }

    @Override
    public String getDescription() {
        return "Region Selection Filter";
    }
    
    public static String getTypeDescription() { return "Region Selection Filter (Listener)"; }

    @Override
    public RegionSelectionFilter copy(List<GraphElement> l) {
        return new RegionSelectionFilter(this,l);
    }

    @Override
    public int getNumParameters(int stream) {
        if(input==null) return 0;
        return input.getNumParameters(0);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(input==null) return "";
        return input.getParameterDescription(0, which);
    }

    @Override
    public int getNumValues(int stream) {
        if(input==null) return 0;
        return input.getNumValues(0);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(input==null) return "";
        return input.getValueDescription(0, which);
    }
    
    private void redoDataBounds() {
        if(input==null) return;
        double min,max;
        try {
            int[] indexBounds=f1.getSafeElementRange(this, 0);
            DataFormula.checkRangeSafety(indexBounds,this);
            min=f1.valueOf(this,0, indexBounds[0]);
            max=min;
            for(int i=indexBounds[0]+1; i<indexBounds[1]; ++i) {
                double v=f1.valueOf(this,0, i);
                if(v<min) min=v;
                if(v>max) max=v;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            f1.setFormula("1");
            min=0;
            max=2;
        }
        center1.setValue((max+min)*0.5);
        size1.setValue(max-min);
        try {
            int[] indexBounds=f2.getSafeElementRange(this, 0);
            DataFormula.checkRangeSafety(indexBounds,this);
            min=f2.valueOf(this,0, indexBounds[0]);
            max=min;
            for(int i=indexBounds[0]+1; i<indexBounds[1]; ++i) {
                double v=f2.valueOf(this,0, i);
                if(v<min) min=v;
                if(v>max) max=v;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            f2.setFormula("1");
            min=0;
            max=2;
        }
        center2.setValue((max+min)*0.5);
        size2.setValue(max-min);
        abstractRedoAllElements();
    }

    private DataFormula f1=new DataFormula("v[0]");
    private DataFormula f2=new DataFormula("v[1]");
    private EditableDouble center1=new EditableDouble(0.0);
    private EditableDouble size1=new EditableDouble(0.0);
    private EditableDouble center2=new EditableDouble(0.0);
    private EditableDouble size2=new EditableDouble(0.0);
    private EditableDouble zoomFactor=new EditableDouble(2.0);
    private EditableBoolean clickZoom=new EditableBoolean(true);
    private EditableBoolean redoOnDrag=new EditableBoolean(false);
    
    private transient double startV1,startV2;
    private transient JLabel v1Label;
    private transient JLabel v2Label;
    
    private static final long serialVersionUID=146134718724698l;
}
