/*
 * Created on Dec 9, 2009
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;

public class StreamUnbinningFilter extends AbstractSingleSourceFilter {
    public StreamUnbinningFilter() {}
    
    public StreamUnbinningFilter(StreamUnbinningFilter c,List<GraphElement> l) {
        super(c,l);
        streamSelectionFormula=new BooleanFormula(c.streamSelectionFormula);
    }
    
    @Override
    protected boolean doingInThreads() {
        return false;
    }

    @Override
    protected void redoAllElements() {
        if(dataVect.size()!=1) {
            dataVect.clear();
            dataVect.add(new ArrayList<DataElement>());
        }
        Map<String,Double> varHash=new HashMap<String,Double>();
        for(int i=0; i<input.getNumStreams(); ++i) {
            varHash.put("i",(double)i);
            if(streamSelectionFormula.valueOf(this,0,0,null,varHash)) {
                for(int j=0; j<input.getNumElements(i); ++j) {
                    dataVect.get(0).add(input.getElement(j,i));
                }
            }
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        panel.add(streamSelectionFormula.getLabeledTextField("Stream Selection Formula",null),BorderLayout.NORTH);
        JButton button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        panel.add(button,BorderLayout.SOUTH);
        propPanel.addTab("Settings",panel);
    }

    @Override
    public int getNumParameters(int stream) {
        return input.getNumParameters(stream);
    }

    @Override
    public int getNumValues(int stream) {
        return input.getNumValues(stream);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return input.getParameterDescription(stream,which);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return input.getValueDescription(stream,which);
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new StreamUnbinningFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Stream Unbinning Filter";
    }

    public static String getTypeDescription() {
        return "Stream Unbinning Filter";
    }

    private BooleanFormula streamSelectionFormula=new BooleanFormula("1=1");
    
    private static final long serialVersionUID = 7149645934662812279L;
}
