package edu.swri.swiftvis.filters;

// edu.swri.swiftvis.filters.DistributionDifferenceFilter

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;

public class DistributionDifferenceFilter extends AbstractMultipleSourceFilter {

	private static final long serialVersionUID = -8047903353931103302L;
	private int normalize = 0;
	private int numBins = 10;
	private double binMin = 0.0;
	private double binMax = 0.0;
	private DataFormula minFormula = new DataFormula("0");
	private DataFormula numFormula = new DataFormula("10");
	private DataFormula maxFormula = new DataFormula("1");
	private DataFormula firstFormula = new DataFormula("d[0].v[0]");
	private DataFormula secondFormula = new DataFormula("d[1].v[0]");

	public DistributionDifferenceFilter() {

	}
	
	private DistributionDifferenceFilter(DistributionDifferenceFilter c,List<GraphElement> l) {
		 super(c,l);
		 normalize = c.normalize;
		 numBins = c.numBins;
		 binMax = c.binMax;
		 binMin = c.binMin;
		 minFormula.setFormula(c.minFormula.getFormula());
		 numFormula.setFormula(c.numFormula.getFormula());
		 maxFormula.setFormula(c.maxFormula.getFormula());
		 firstFormula.setFormula(c.firstFormula.getFormula());
		 secondFormula.setFormula(c.secondFormula.getFormula());
	}

	@Override
	protected boolean doingInThreads() {
		return false;
	}

	@Override
	protected void redoAllElements() {
		if (getNumSources() < 2) return;
		
		binMin = minFormula.valueOf(this, 0, 0);
		binMax = maxFormula.valueOf(this, 0, 0);
		numBins = (int)numFormula.valueOf(this, 0, 0);

		int[] firstRange=firstFormula.getSafeElementRange(this,0);
		int[] secondRange=secondFormula.getSafeElementRange(this,0);
		DataFormula.checkRangeSafety(firstRange,this);
		DataFormula.checkRangeSafety(secondRange,this);

		double binSize = (binMax - binMin) / numBins;
		List<List<Integer>> firstBins = new ArrayList<List<Integer>>(numBins);
		int[] secondBins = new int[numBins];

		for(int i = 0; i < numBins; i++) {
			firstBins.add(new LinkedList<Integer>());
			secondBins[i] = 0;
		}

		for(int i = firstRange[0]; i < firstRange[1]; i++) {
			double f = firstFormula.valueOf(this, 0, i);
			if (f >= binMin && f <= binMax) {
				int binNum = (int)(f / binSize);
				if (binNum < numBins) {
					firstBins.get(binNum).add(i);
				}
			}
		}

		for(int i = secondRange[0]; i < secondRange[1]; i++) {
			double s = secondFormula.valueOf(this, 0, i);
			if (s >= binMin && s <= binMax) {
				int binNum = (int)(s / binSize);
				if (binNum < numBins) {
					secondBins[binNum]++;
				}
			}
		}

		if (normalize == 1) { // Normalize by max
			double ratio = secondBins[0] / firstBins.get(0).size();

			for(int i = 0; i < secondBins.length; i++) {
				secondBins[i] /= ratio;
			}
		} 
		/*else if (normalize == 2) { // Normalize by count

		}*/

		for(int i = 0; i < numBins; i++) {
			if (secondBins[i] >= firstBins.get(i).size()) {
				for(int j : firstBins.get(i)) {
					DataElement d = inputVector.get(0).getElement(j, 0);
					
					d = d.addParam(i);
					d = d.addParam(0);
					
					dataVect.get(0).add(d);
				}
			} else {
				int start = 0;
				for(int j : firstBins.get(i)) {
					DataElement d = inputVector.get(0).getElement(j, 0);
					
					d = d.addParam(i);
					if(start < secondBins[i]) {
						d = d.addParam(0);
					} else {
						d = d.addParam(1);
					}
					
					dataVect.get(0).add(d);
					start++;
				}
			}
		}
	}

	@Override
	protected void setupSpecificPanelProperties() {
		JPanel outerPanel = new JPanel(new BorderLayout());

		JPanel argPanel = new JPanel(new GridLayout(6,1));

		argPanel.add(firstFormula.getLabeledTextField("Source One Formula ", null));

		argPanel.add(secondFormula.getLabeledTextField("Source Two Formula ", null));

		argPanel.add(minFormula.getLabeledTextField("Bin Minimum ", null));

		argPanel.add(maxFormula.getLabeledTextField("Bin Maximum ", null));

		argPanel.add(numFormula.getLabeledTextField("Num Bins ", null));

		JPanel dropDownPanel = new JPanel(new FlowLayout());

		// Once 'Count' is implemented, use this line.
		// JComboBox normalList = new JComboBox(new String[]{"None","Max","Count"});
		
		JComboBox normalList = new JComboBox(new String[]{"None","Max"});
		normalList.setSelectedIndex(0);
		normalList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				normalize = cb.getSelectedIndex();
			}});

		JButton button = new JButton("Propogate Changes");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				abstractRedoAllElements();
			}
		});
		dropDownPanel.add(new JLabel("Normalization "));
		dropDownPanel.add(normalList);
		argPanel.add(dropDownPanel);

		outerPanel.add(argPanel,BorderLayout.NORTH);
		outerPanel.add(button,BorderLayout.SOUTH);


		propPanel.add("Parameters",outerPanel);
	}

	@Override
	public DistributionDifferenceFilter copy(List<GraphElement> l) {
		return new DistributionDifferenceFilter(this,l);
	}

	@Override
	public String getDescription(){ return "Distribution Difference Filter"; }

	public static String getTypeDescription(){ return "Distribution Difference Filter"; }

	@Override
	public String getParameterDescription(int stream, int which) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getValueDescription(int stream, int which) {
		// TODO Auto-generated method stub
		return null;
	}

}
