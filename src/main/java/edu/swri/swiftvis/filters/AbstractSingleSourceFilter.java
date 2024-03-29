/*
 * Created on Jul 6, 2004
 */
package edu.swri.swiftvis.filters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.Filter;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.GraphPanel;
import edu.swri.swiftvis.util.GraphLabelString;
import edu.swri.swiftvis.util.OutputInfoPanel;
import edu.swri.swiftvis.util.SourceInfoPanel;
import edu.swri.swiftvis.util.ThreadHandler;

/**
 * This abstract class is intended to help people extend SwiftVis.  Most filters
 * that can only take data from a single source should extend this class instead of
 * directly implementing the Filter interface. 
 * @author Mark Lewis
 */
public abstract class AbstractSingleSourceFilter implements Filter {
    protected AbstractSingleSourceFilter() {
        for(int i=0; i<getNumStreams(); ++i) {
            dataVect.add(new ArrayList<DataElement>());
        }
    }
    
    /**
     * This constructor is used by the copy method of subclasses.
     * @param c The filter we are copying.
     * @param l A list of other elements being copied.  Only links to these should go through.
     */
    protected AbstractSingleSourceFilter(AbstractSingleSourceFilter c,List<GraphElement> l) {
        dataVect=new ArrayList<ArrayList<DataElement>>();
        for(int i=0; i<c.dataVect.size(); ++i) {
            dataVect.add(new ArrayList<DataElement>(c.dataVect.get(i)));
        }
        if(l.contains(c.input)) input=c.input;
        sinkVector=new ArrayList<DataSink>();
        for(DataSink ds:c.sinkVector) {
            if(l.contains(ds)) sinkVector.add(ds);
        }
        label=new GraphLabelString(c.label);
    }

	@Override
    public boolean validInput(DataSource ds){ return true; }

    @Override
    public void addInput(DataSource in) {
        if (in == this)
            return;
        if (input != null) {
            input.removeOutput(this);
        }
        input = in;
        abstractRedoAllElements();
        if (sip != null)
            sip.redoSourceList();
        input.addOutput(this);
    }

	@Override
    public void removeInput(DataSource in){
		if(in!=input) return;
		input.removeOutput(this);
		input=null;
		abstractRedoAllElements();
		if(sip!=null) sip.redoSourceList();
	}

    @Override
    public void moveUpInput(int index) {
    }
        
    @Override
    public DataSource getSource(int which){ return (which==0)?input:null; }

	@Override
    public int getNumSources(){ return (input!=null)?1:0; }

	@Override
    public void sourceAltered(DataSource source){
		abstractRedoAllElements();
	}

	@Override
    public void addOutput(DataSink sink){
	    sinkVector.add(sink);
	}

	@Override
    public void removeOutput(DataSink sink){
	    sinkVector.remove(sink);
	}

    @Override
    public int getNumOutputs() {
        return sinkVector.size();
    }
    
    @Override
    public DataSink getOutput(int which) {
        return sinkVector.get(which);
    }

    @Override
    public Rectangle getBounds() {
        return label.getBounds();
    }

    @Override
    public void translate(int dx,int dy) {
        label.translate(dx,dy);
    }

    @Override
    public void clearData() {
        dataVect=new ArrayList<ArrayList<DataElement>>();
        for(int i=0; i<getNumStreams(); ++i) {
            dataVect.add(new ArrayList<DataElement>());
        }
    }

	public Paint getPaint(){ return Color.blue; }
    
    @Override
    public void drawNode(Graphics2D g) {
        label.draw(g);
    }

	@Override
    public DataElement getElement(int i,int stream) { return dataVect.get(stream).get(i); }

    @Override
    public int getNumStreams() {
        if(dataVect==null || dataVect.size()<1) return 1;
        return dataVect.size();
    }

	/**
	 * Returns the number of data elements that this source has in it.  I'm using
	 * this instead of an iterator because direct access is much more efficient
	 * when trying to make tables of data.
	 * @return The number of data elements in this source.
	 */
	@Override
    public int getNumElements(int stream){ return dataVect.get(stream).size(); }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JTabbedPane();
            setupSpecificPanelProperties();
            
			propPanel.addTab("Sources",getSourceInfoPanel());
			propPanel.addTab("Output",getOutputInfoPanel());
            propPanel.addTab("Label",label.getAreaPanel("Label")); 
        }
        return propPanel;
    }

    @Override
    public void relink(Hashtable<GraphElement,GraphElement> linkHash) {
        for(int i=0; i<sinkVector.size(); ++i) {
            sinkVector.set(i,(DataSink)linkHash.get(sinkVector.get(i)));
        }
        if(input!=null) input=(DataSource)linkHash.get(input);
    }
    
    @Override
    public void redo() {
    	localRedo();
    }

    @Override
    public String toString() {
        return label.toString();
    }
    
	protected void abstractRedoAllElements() {
	    GraphPanel.instance().newWorkOrder(this);
	}
	
    protected void localRedo() {
		if(input==null) return;
	    if(clearOnRedo()) {
            for(int i=0; i<dataVect.size(); ++i) {
                dataVect.get(i).clear();
            }
	    }
	    if (doingInThreads()) {
	    	redoAllElements();
	    } else {
	    	ThreadHandler.instance().loadWaitTask(this,new Runnable() {
	    		@Override
                public void run() {
	    			redoAllElements();
	    		}
	    	});
	    	ThreadHandler.instance().waitForAll(this);
	    }
	    /*  sink traversal done in GraphPanel nwoHelper()
		for(DataSink ds:sinkVector) {
			ds.sourceAltered(this);
		}
		*/
		if(oip!=null) oip.redoOutputTable();
	}
    
    protected void sizeDataVectToInputStreams() {
        if(dataVect.size()>input.getNumStreams()) dataVect.clear();
        while(dataVect.size()<input.getNumStreams()) dataVect.add(new ArrayList<DataElement>());
    }
    
    protected boolean clearOnRedo() {
        return true;
    }

    protected SourceInfoPanel getSourceInfoPanel() {
        if (sip == null)
            sip = new SourceInfoPanel(this);
        return sip;
    }

    protected OutputInfoPanel getOutputInfoPanel() {
        if (oip == null)
            oip = new OutputInfoPanel(this);
        return oip;
    }

	
	/**
	 * This method is intended to reprocess all the input elements to redo the
	 * output.  It must be properly implemented by all extending classes.
	 */
	protected abstract void redoAllElements();
	

	/**
	 * This method should add any tabbed panes to the propPanel that are specific to
	 * the given filter.  This is called when the propPanel is created.  After it is
	 * called, the source and output info panels are added.
	 */
	protected abstract void setupSpecificPanelProperties();
	
	/**
	 * Method implemented to inform the AbstractFilter whether the Filter is handling
	 * threading threading or if if the AbstractFilter should put the work into the 
	 * ThreadHandler itself.
	 * @return True if redoAllElements() is threaded; False if redoAllElements() should be
	 * threaded by AbstractFilter
	 */
	protected abstract boolean doingInThreads();

    protected ArrayList<ArrayList<DataElement>> dataVect=new ArrayList<ArrayList<DataElement>>();

    protected DataSource input=null;

    protected ArrayList<DataSink> sinkVector=new ArrayList<DataSink>();

    private GraphLabelString label=new GraphLabelString(getDescription(),getPaint());
	
    protected transient JTabbedPane propPanel;

    private transient SourceInfoPanel sip;

    private transient OutputInfoPanel oip;

    private static final long serialVersionUID=6106398216791133408l;
}
