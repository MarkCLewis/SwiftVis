/*
 * Created on Aug 17, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.swri.swiftvis.filters;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;

import java.util.Comparator;
import java.util.Arrays;
import java.util.List;

/**
 * Sorts elements based on some formula.
 * @author Mark Lewis
 */
public class SortFilter extends AbstractSingleSourceFilter {
	public SortFilter() {
		map=new Integer[0][0];
		sortValue=new DataFormula("v[0]");
	}
    
    private SortFilter(SortFilter c,List<GraphElement> l) {
        super(c,l);
        map=new Integer[c.map.length][];
        for(int i=0; i<map.length; ++i) {
            map[i]=Arrays.copyOf(c.map[i],c.map[i].length);
        }
        sortValue=new DataFormula(c.sortValue);
    }

	@Override
    public int getNumElements(int stream) {
		return map[stream].length;
	}

	@Override
    public DataElement getElement(int i, int stream) {
		return input.getElement(map[stream][i],stream);
	}

	@Override
    public int getNumParameters(int stream) {
		if(input==null) return 0;
		return input.getNumParameters(0);
	}

	@Override
    public String getParameterDescription(int stream, int which) {
		if(input==null) return "No Inputs";
		return input.getParameterDescription(0, which);
	}

	@Override
    public int getNumValues(int stream) {
		if(input==null) return 0;
		return input.getNumValues(0);
	}

	@Override
    public String getValueDescription(int stream, int which) {
		if(input==null) return "No Inputs";
		return input.getValueDescription(0, which);
	}

	@Override
    public String getDescription() {
		return "Sort Filter";
	}

	public static String getTypeDescription(){ return "Sort Filter"; }

    @Override
    public SortFilter copy(List<GraphElement> l) {
        return new SortFilter(this,l);
    }

	@Override
    protected void setupSpecificPanelProperties() {
		JPanel formPanel=new JPanel(new BorderLayout());
		JPanel tmpPanel=new JPanel(new BorderLayout());
		tmpPanel.add(new JLabel("Sort Expression"),BorderLayout.WEST);
		tmpPanel.add(sortValue.getTextField(null),BorderLayout.CENTER);
		formPanel.add(tmpPanel,BorderLayout.NORTH);
		JButton button=new JButton("Propagate Changes");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
		} );
		formPanel.add(button,BorderLayout.SOUTH);
		propPanel.add("Expression",formPanel);
	}
	
	@Override
    protected boolean doingInThreads() {
		return false;
	}
	
	@Override
    protected void redoAllElements() {
		if(input!=null) {
		    sizeDataVectToInputStreams();
            map=new Integer[getSource(0).getNumStreams()][];
		    for(int s=0; s<getSource(0).getNumStreams(); ++s) {
		        final int ss=s;
	            map[s]=new Integer[input.getNumElements(s)];
    			for(int i=0; i<map[s].length; ++i) map[s][i]=i;
    			Arrays.sort(map[s],new Comparator<Integer>() {
    				@Override
                    public int compare(Integer o1,Integer o2) {
    					double v1=sortValue.valueOf(SortFilter.this,ss,o1);
    					double v2=sortValue.valueOf(SortFilter.this,ss,o2);
    					return Double.compare(v1,v2);
    				}
    			});
		    }
		}
	}
	
	private Integer[][] map;

    private DataFormula sortValue;

    private static final long serialVersionUID=32690872335725l;
}
