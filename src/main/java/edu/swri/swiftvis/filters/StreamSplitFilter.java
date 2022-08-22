/*
 * Created on May 29, 2009
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;

public class StreamSplitFilter extends AbstractSingleSourceFilter {
    public StreamSplitFilter() {}
    
    private StreamSplitFilter(StreamSplitFilter c,List<GraphElement> l) {
        super(c,l);
        streamSelection=new BooleanFormula(c.streamSelection);
        selected=new ArrayList<Integer>(c.selected);
    }

    @Override
    protected boolean doingInThreads() {
        return false;
    }

    @Override
    protected void redoAllElements() {
        selected.clear();
        HashMap<String,Double> vars=new HashMap<String,Double>();
        for(int i=0; i<getSource(0).getNumStreams(); ++i) {
            vars.put("i",(double)i);
            if(streamSelection.valueOf(this,i,0,null, vars)) {
                selected.add(i);
            }
        }
        if(selected.isEmpty()) selected.add(0);
    }
    
    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        panel.add(streamSelection.getLabeledTextField("Selction Formula (use i for stream number)",null),BorderLayout.NORTH);
        JButton button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        panel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Selection",panel);
    }
    
    @Override
    public DataElement getElement(int i,int stream) {
        return getSource(0).getElement(i,selected.get(stream));
    }
    
    @Override
    public int getNumElements(int stream) {
        if(getNumSources()<1) return 0;
        return getSource(0).getNumElements(selected.get(stream));
    }

    @Override
    public int getNumStreams() {
        if(getNumSources()<1) return 1;
        return selected.size();
    }

    @Override
    public int getNumParameters(int stream) {
        return getSource(0).getNumParameters(selected.get(stream));
    }

    @Override
    public int getNumValues(int stream) {
        return getSource(0).getNumValues(selected.get(stream));
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return getSource(0).getParameterDescription(selected.get(stream),which);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return getSource(0).getValueDescription(selected.get(stream),which);
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new StreamSplitFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Stream Split Filter";
    }
    
    public static String getTypeDescription() {
        return "Stream Split Filter";
    }

    private BooleanFormula streamSelection=new BooleanFormula("i=0");
    private List<Integer> selected=new ArrayList<Integer>();

    private static final long serialVersionUID = 2143891343185546480L;
}
