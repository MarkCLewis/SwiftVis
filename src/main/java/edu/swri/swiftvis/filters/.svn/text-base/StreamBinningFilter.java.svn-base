/*
 * Created on Dec 9, 2009
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableInt;

/**
 * This filter "bins" the elements of the first stream of the input into
 * different streams.  This is an efficient way to break data up into
 * separate groupings for processing.  The alternative would require a large
 * number of selection filters that pull things apart.  The selection filter
 * basically throws away the things that aren't selected.  This keeps everything,
 * but puts them all in different streams.
 * 
 * @author Mark Lewis
 */
public class StreamBinningFilter extends AbstractSingleSourceFilter {
    public StreamBinningFilter() {}
    
    private StreamBinningFilter(StreamBinningFilter c,List<GraphElement> l) {
        super(c,l);
        sourceStream=new EditableInt(c.sourceStream.getValue());
        streamNumberFormula=new DataFormula(c.streamNumberFormula);
    }

    @Override
    protected boolean doingInThreads() {
        return false;
    }

    @Override
    protected void redoAllElements() {
        streamNumberFormula.clearGroupSelection();
        dataVect.clear();
        int s=sourceStream.getValue();
        int[] range=streamNumberFormula.getSafeElementRange(this,s);
        DataFormula.checkRangeSafety(range,this);
        for(int i=range[0]; i<range[1]; ++i) {
            int stream=(int)streamNumberFormula.valueOf(this,s,i);
            while(dataVect.size()<=stream) dataVect.add(new ArrayList<DataElement>());
            dataVect.get(stream).add(input.getElement(i,s));
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(2,1));
        northPanel.add(sourceStream.getLabeledTextField("Source Stream",null));
        northPanel.add(streamNumberFormula.getLabeledTextField("Stream Number Formula",null));
        panel.add(northPanel,BorderLayout.NORTH);
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
        return getSource(0).getNumParameters(stream);
    }

    @Override
    public int getNumValues(int stream) {
        return getSource(0).getNumValues(stream);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return getSource(0).getParameterDescription(stream,which);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return getSource(0).getValueDescription(stream,which);
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new StreamBinningFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Stream Binning Filter";
    }
    
    public static String getTypeDescription() {
        return "Stream Binning Filter";
    }

    private EditableInt sourceStream=new EditableInt(0);
    private DataFormula streamNumberFormula=new DataFormula("1");
    
    private static final long serialVersionUID = 6117447440955016071L;
}
