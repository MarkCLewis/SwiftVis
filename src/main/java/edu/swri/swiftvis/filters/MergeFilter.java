/*
 * Created on Aug 17, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.swri.swiftvis.filters;

import java.util.List;

import edu.swri.swiftvis.*;
import edu.swri.swiftvis.DataElement;

/**
 * This class gives the user a way to merge two or more data sets together in a serial
 * manner.  They can use a FunctionFilter to merge things in parallel so that the
 * elements are twice as large.  If two or more data sets are related such that they can
 * be put end to end, this is the method for putting them together.
 * @author Mark Lewis
 */
public class MergeFilter extends AbstractMultipleSourceFilter {
    public MergeFilter() {}
    
    private MergeFilter(MergeFilter c,List<GraphElement> l) {
        super(c,l);
    }
    
	@Override
    public String getDescription(){ return "Merge Filter"; }

	public static String getTypeDescription(){ return "Merge Filter"; }

	@Override
    protected void setupSpecificPanelProperties(){
	}

	/**
	 * Tells you what a particular parameter is used for.
	 */
	@Override
    public String getParameterDescription(int stream, int which){
		return inputVector.get(0).getParameterDescription(stream,which);
	}

	/**
	 * Tells you what a particular value is used for.
	 */
	@Override
    public String getValueDescription(int stream, int which){
		return inputVector.get(0).getValueDescription(stream,which);
	}

	@Override
    public int getNumParameters(int stream){
		if(inputVector.size()<1) return 0;
		return inputVector.get(0).getNumParameters(stream);
	}

	@Override
    public int getNumValues(int stream){
		if(inputVector.size()<1) return 0;
		return inputVector.get(0).getNumValues(stream);
	}

	@Override
    public DataElement getElement(int i, int stream){
		int whichSource=0;
		DataSource source=inputVector.get(whichSource);
		while(i>=source.getNumElements(stream)) {
			i-=source.getNumElements(stream);
			whichSource++;
			source=inputVector.get(whichSource);
		}
		return source.getElement(i,stream);
	}

	/**
	 * Returns the number of data elements that this source has in it.  I'm using
	 * this instead of an iterator because direct access is much more efficient
	 * when trying to make tables of data.
	 * @return The number of data elements in this source.
	 */
	@Override
    public int getNumElements(int stream){
		int ret=0;
		for(int i=0; i<inputVector.size(); i++) {
			ret+=inputVector.get(i).getNumElements(stream);
		}
		return ret;
	}

    @Override
    public MergeFilter copy(List<GraphElement> l) {
        return new MergeFilter(this,l);
    }

    @Override
    protected boolean doingInThreads() {
		return false;
	}
	@Override
    protected void redoAllElements() {
	}

    private static final long serialVersionUID=14698076363146l;
}
