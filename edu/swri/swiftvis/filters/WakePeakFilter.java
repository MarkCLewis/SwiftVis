/*
 * Created on Jul 6, 2004
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableDouble;

/**
 * This is a simple filter intended to isolate the location of wake peaks in the
 * two types of output data that I use for ring simulations.  The input should be
 * either a spatial binned file or a particle binned file.  The output will be
 * data elements with one value for the Y location of the slice and one value for the
 * X location of the peak.
 * 
 * This filter supports a single input.
 * @author Mark Lewis
 */
public final class WakePeakFilter extends AbstractSingleSourceFilter {
	public WakePeakFilter() {
		try {
			xFormula.valueOf(this,0, 0);
		} catch(Exception e) {
			xFormula=new DataFormula("0");
		}
		try {
			yFormula.valueOf(this,0, 0);
		} catch(Exception e) {
			yFormula=new DataFormula("0");
		}
		try {
			valueFormula.valueOf(this,0, 0);
		} catch(Exception e) {
			valueFormula=new DataFormula("0");
		}
	}
    
    private WakePeakFilter(WakePeakFilter c,List<GraphElement> l) {
        super(c,l);
        inputStyle=c.inputStyle;
        threshold=new EditableDouble(c.threshold.getValue());
        xFormula=new DataFormula(c.xFormula);
        yFormula=new DataFormula(c.yFormula);
        valueFormula=new DataFormula(c.valueFormula);
    }
    
	@Override
    public String getDescription(){ return "Wake Peak Filter"; }

	public static String getTypeDescription(){ return "Wake Peak Filter"; }

	/**
	 * Tells you what a particular parameter is used for.
	 */
	@Override
    public String getParameterDescription(int stream, int which){
		if(input==null) return "None";
		return input.getParameterDescription(0, which);
	}

	/**
	 * Tells you what a particular value is used for.
	 */
	@Override
    public String getValueDescription(int stream, int which){
	    return valueDescription[which];
	}

	@Override
    public int getNumParameters(int stream){
	    return 0;
	}

	@Override
    public int getNumValues(int stream){
	    return 3;
	}

    @Override
    public WakePeakFilter copy(List<GraphElement> l) {
        return new WakePeakFilter(this,l);
    }
    
    @Override
    protected boolean doingInThreads() {
    	return false;
    }

	/**
	 * For this class, this method runs through all of the input elements and
	 * for each column it locates the wake peaks above the threshold and 
	 */
	@Override
    protected void redoAllElements() {
		sizeDataVectToInputStreams();
		for(int s=0; s<getSource(0).getNumStreams(); ++s) {
	        if(input.getNumElements(s)>3) {
        		int[] yRange=yFormula.getSafeElementRange(this,s);
        		int[] xRange=xFormula.getSafeElementRange(this,s);
        		if(xRange[0]>yRange[0]) yRange[0]=xRange[0];
        		if(xRange[1]<yRange[1]) yRange[1]=xRange[1];
        		if(inputStyle.useValueFormula()) {
        			int[] valRange=valueFormula.getSafeElementRange(this,s);
        			if(valRange[0]>yRange[0]) yRange[0]=valRange[0];
        			if(valRange[1]<yRange[1]) yRange[1]=valRange[1];
        		}
        		DataFormula.checkRangeSafety(yRange,this);
        		int i=yRange[0];
        		double yMatch=yFormula.valueOf(this,s,i);
        		double base=0.0;
        		double yVal=yMatch;
                if(baseLine.getValue()<0) {
                    double cnt=0.0;
            		while(yVal==yMatch && i<yRange[1]) {
            			double val=inputStyle.getHeightValue(i,s);
            			if(val!=0) {
            				base+=val;
            				cnt++;
            			}
            			++i;
            			yVal=yFormula.valueOf(this,s,i);
            		}
            		if(cnt!=0.0) base/=cnt;
                } else {
                    base=baseLine.getValue();
                }
        		i=yRange[0];
        		while(i<yRange[1]) {
        			int start=i;
        			yMatch=yFormula.valueOf(this,s,i);
        			yVal=yMatch;
        			while(yVal==yMatch && i<yRange[1]-1) {
        				++i;
        				yVal=yFormula.valueOf(this,s,i);
        			}
        			dataVect.get(s).addAll(inputStyle.getPeaks(yMatch,base,start,i-1,s));
        			++i;
        		}
    		}
		}
	}
	
	@Override
    protected void setupSpecificPanelProperties() {
		JPanel formPanel=new JPanel(new BorderLayout());
		JPanel northPanel=new JPanel(new GridLayout(6,1));
		BinningStyle[] options={new ParticleBinStyle(),new SpaceBinStyle()};
		JComboBox comboBox=new JComboBox(options);
		comboBox.addActionListener(new ActionListener() {
		    @Override
            public void actionPerformed(ActionEvent e) {
		        inputStyle=(BinningStyle)(((JComboBox)e.getSource()).getSelectedItem());
		        valueField.setEnabled(inputStyle.useValueFormula());
		        redoAllElements();
		    }
		} );
		if(inputStyle instanceof ParticleBinStyle) comboBox.setSelectedIndex(0);
		else comboBox.setSelectedIndex(1);
		northPanel.add(comboBox);
		JPanel innerPanel=new JPanel(new BorderLayout());
		innerPanel.add(new JLabel("Threshold Multiplier"),BorderLayout.WEST);
		innerPanel.add(threshold.getTextField(new EditableDouble.Listener() {
			@Override
            public void valueChanged() { abstractRedoAllElements(); }
		}),BorderLayout.CENTER);
		northPanel.add(innerPanel);
        northPanel.add(baseLine.getLabeledTextField("Base tau (-1 for auto)",null));
		innerPanel=new JPanel(new BorderLayout());
		innerPanel.add(new JLabel("Radial Formula"),BorderLayout.WEST);
		innerPanel.add(xFormula.getTextField(new DataFormula.Listener() {
			@Override
            public void formulaChanged() { abstractRedoAllElements(); }
		}),BorderLayout.CENTER);
		northPanel.add(innerPanel);
		innerPanel=new JPanel(new BorderLayout());
		innerPanel.add(new JLabel("Azimuthal Formula"),BorderLayout.WEST);
		innerPanel.add(yFormula.getTextField(new DataFormula.Listener() {
			@Override
            public void formulaChanged() { abstractRedoAllElements(); }
		}),BorderLayout.CENTER);
		northPanel.add(innerPanel);
		innerPanel=new JPanel(new BorderLayout());
		innerPanel.add(new JLabel("Value Formula"),BorderLayout.WEST);
		valueField=valueFormula.getTextField(new DataFormula.Listener() {
			@Override
            public void formulaChanged() { abstractRedoAllElements(); }
		});
		if(!inputStyle.useValueFormula()) {
		    valueField.setEnabled(false);
		}
		innerPanel.add(valueField,BorderLayout.CENTER);
		northPanel.add(innerPanel);
		formPanel.add(northPanel,BorderLayout.NORTH);

		JButton button=new JButton("Propagate Changes");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
		} );
		formPanel.add(button,BorderLayout.SOUTH);
		propPanel.add("Settings",formPanel);
	}

	private DataElement buildElement(double y,double x,double val) {
	    float[] valArray={(float)y,(float)x,(float)val};
	    return new DataElement(new int[0],valArray);
	}
	
	private static final String[] valueDescription={"Y","X","Value"};
	
	private BinningStyle inputStyle=new SpaceBinStyle();
	private EditableDouble threshold=new EditableDouble(1.2);
    private EditableDouble baseLine=new EditableDouble(-1);
	private DataFormula xFormula=new DataFormula("v[1]");
	private DataFormula yFormula=new DataFormula("v[0]");
	private DataFormula valueFormula=new DataFormula("v[2]");

	private transient JTextField valueField;
    private static final long serialVersionUID=12436987390468l;
	
	private interface BinningStyle extends Serializable {
	    List<DataElement> getPeaks(double y,double base,int start,int end,int stream);
	    boolean useValueFormula();
	    double getHeightValue(int element,int stream);
	}
	
	private class ParticleBinStyle implements BinningStyle {
	    @Override
        public String toString() { return "Particle Binned Data"; }
	    
	    @Override
        public List<DataElement> getPeaks(double y,double base,int start,int end,int stream) {
	        List<DataElement> ret=new ArrayList<DataElement>(4);
	        for(int i=start+1; i<end-1; ++i) {
	            double height=getHeightValue(i,stream);
	            if(height>base*threshold.getValue()) {
	                int j=i+1;
	                int maxIndex=i;
	                double maxHeight=height;
	                double jHeight=getHeightValue(j,stream);
	                while(j<end-1 && jHeight>base) {
	                    if(jHeight>maxHeight) {
	                        maxHeight=jHeight;
	                        maxIndex=j;
	                    }
	                    j++;
	                    jHeight=getHeightValue(j,stream);
	                }
	                double loc=getPeakLocation(maxIndex,stream);
	                ret.add(buildElement(y,loc,maxHeight));
	                i=j-1;
	            }
	        }
	        return ret;
	    }
	    @Override
        public boolean useValueFormula() {
	        return false;
	    }
	    @Override
        public double getHeightValue(int element,int stream) {
	        return 1.0/(xFormula.valueOf(WakePeakFilter.this,stream,element+1)-xFormula.valueOf(WakePeakFilter.this,stream,element));
	    }
	    /**
	     * This method fits a quadratic to the points in a window three wide centered
	     * on the elements passed in.
	     */
	    private double getPeakLocation(int element,int stream) {
	        double[] x=new double[4];
	        x[0]=xFormula.valueOf(WakePeakFilter.this,stream,element-1);
	        x[1]=xFormula.valueOf(WakePeakFilter.this,stream,element);
	        x[2]=xFormula.valueOf(WakePeakFilter.this,stream,element+1);
	        x[3]=xFormula.valueOf(WakePeakFilter.this,stream,element+2);
	        double[] h={x[1]-x[0],x[2]-x[1],x[3]-x[2]};
	        x[0]=(x[0]+x[1])*0.5;
	        x[1]=(x[1]+x[2])*0.5;
	        x[2]=(x[2]+x[3])*0.5;
	        double a=((h[0]-h[1])-(h[0]-h[2])*(x[0]-x[1])/(x[0]-x[2]))/
	        	((x[0]*x[0]-x[1]*x[1])-(x[0]*x[0]-x[2]*x[2])*(x[0]-x[1])/(x[0]-x[2]));
	        double b=((h[0]-h[2])-a*(x[0]*x[0]-x[1]*x[1]))/(x[0]-x[1]);
	        return -b/(2.0*a);
	    }

        private static final long serialVersionUID=34689720536l;
	}

	private class SpaceBinStyle implements BinningStyle {
	    @Override
        public String toString() { return "Spatial Binned Data"; }

	    @Override
        public List<DataElement> getPeaks(double y,double base,int start,int end,int stream) {
	        List<DataElement> ret=new ArrayList<DataElement>(4);
	        for(int i=start+1; i<end; ++i) {
	            double height=getHeightValue(i,stream);
	            if(height>base*threshold.getValue()) {
	                int j=i+1;
	                int maxIndex=i;
	                double maxHeight=height;
	                double jHeight=getHeightValue(j,stream);
	                while(j<end-1 && jHeight>base*threshold.getValue()) {
	                    if(jHeight>maxHeight) {
	                        maxHeight=jHeight;
	                        maxIndex=j;
	                    }
	                    j++;
	                    jHeight=getHeightValue(j,stream);
	                }
	                double loc=getPeakLocation(maxIndex,stream);
	                ret.add(buildElement(y,loc,maxHeight));
	                i=j-1;
	            }
	        }
	        return ret;
	    }
	    @Override
        public boolean useValueFormula() {
	        return true;
	    }
	    @Override
        public double getHeightValue(int element,int stream) {
	        return valueFormula.valueOf(WakePeakFilter.this,stream,element);
	    }
	    /**
	     * This method fits a quadratic to the points in a window three wide centered
	     * on the elements passed in.
	     */
	    private double getPeakLocation(int element,int stream) {
	        double[] x=new double[3];
	        x[0]=xFormula.valueOf(WakePeakFilter.this,0, element-1);
	        x[1]=xFormula.valueOf(WakePeakFilter.this,0, element);
	        x[2]=xFormula.valueOf(WakePeakFilter.this,0, element+1);
	        double[] h={getHeightValue(element-1,stream),getHeightValue(element,stream),
	                getHeightValue(element+1,stream)};
	        double a=((h[0]-h[1])-(h[0]-h[2])*(x[0]-x[1])/(x[0]-x[2]))/
	        	((x[0]*x[0]-x[1]*x[1])-(x[0]*x[0]-x[2]*x[2])*(x[0]-x[1])/(x[0]-x[2]));
	        double b=((h[0]-h[2])-a*(x[0]*x[0]-x[1]*x[1]))/(x[0]-x[1]);
	        return -b/(2.0*a);
	    }

        private static final long serialVersionUID=93847651987465l;
	}
}
