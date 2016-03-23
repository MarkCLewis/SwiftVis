/*
 * Created on Jul 28, 2008
 */
package edu.swri.swiftvis.sources;

import java.util.List;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;

public class AutoProcessSource extends AbstractSource {
    public AutoProcessSource() {
        setCount(0);
    }
    
    public AutoProcessSource(AutoProcessSource c,List<GraphElement> l) {
        super(c,l);
    }

    @Override
    protected void redoAllElements() {
        dataVect.add(new DataElement(param,new float[0]));
    }

    @Override
    protected void setupSpecificPanelProperties() {
    }
    
    public void setCount(int cnt) {
        param[0]=cnt;
        abstractRedoAllElements();
    }

    @Override
    public int getNumParameters(int stream) {
        return 1;
    }

    @Override
    public int getNumValues(int stream) {
        return 0;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return "Count";
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return null;
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new AutoProcessSource(this,l);
    }

    @Override
    public String getDescription() {
        return "Automatic Process Count Source";
    }
    
    public static String getTypeDescription() {
        return "Automatic Process Count Source";
    }
    
    private int[] param=new int[1];

    private static final long serialVersionUID = -5093546966122051729L;
}
