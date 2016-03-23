package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.KDTree;
import edu.swri.swiftvis.util.ReduceLoopBody;
import edu.swri.swiftvis.util.ThreadHandler;
import edu.swri.swiftvis.util.KDTree.TreePoint;

public class CorrelationFilter extends AbstractSingleSourceFilter {

	private static final long serialVersionUID = -3512624424692472745L;
	private transient DefaultListModel streamListModel;
	private transient JList streamList;
	private transient JPanel eqDisplayPanel;
	private transient JPanel paramDisplayPanel;
	private transient JPanel pruneDisplayPanel;
	private EditableBoolean useTree = new EditableBoolean(false);
	private EditableInt[] streamUse;
	private ArrayList<FunctionEntry> eqEntry=new ArrayList<FunctionEntry>();
	private transient JList eqList;
	private ArrayList<FunctionEntry> paramEntry=new ArrayList<FunctionEntry>();
	private transient JList paramList;
	private ArrayList<TreeEntry> pruneEntry=new ArrayList<TreeEntry>();
	private transient JList pruneList;

	public CorrelationFilter() {

	}

	public CorrelationFilter(CorrelationFilter c, List<GraphElement> l) {
		super(c,l);
		useTree.setValue(c.useTree.getValue());
		for(FunctionEntry entry:c.eqEntry) {
			eqEntry.add(new FunctionEntry(entry));
		}
		for(FunctionEntry entry:c.paramEntry) {
			paramEntry.add(new FunctionEntry(entry));
		}
		for(TreeEntry entry:c.pruneEntry) {
			pruneEntry.add(new TreeEntry(entry));
		}
	}

	@Override
	protected boolean doingInThreads() {
		return true;
	}

	private int[] genStreams(int numLoops) {
		int[] ret = new int[numLoops];
		int retPos = 0;
		for(int i = 0; i < streamUse.length; i++) {
			for(int j = 0; j < streamUse[i].getValue(); j++) {
				ret[retPos] = i;
				retPos++;
			}
		}
		return ret;
	}

	@Override
	protected void redoAllElements() {
		if(streamUse == null || input.getNumStreams() < 1) return;
		updateStreamList();

		if (useTree.getValue()) {
			List<TreeElem> toBuild = getTreeBuildPoints();

			final double[] ranges = getTreeRanges();

			final KDTree<TreeElem> tree = new KDTree<TreeElem>(toBuild,pruneEntry.size(),5,ranges);

			int numLoops = 0;
			for(EditableInt n:streamUse) numLoops+=n.getValue();
			if (numLoops < 2) return;
			final int[] streams=genStreams(numLoops);
			final int[] iters=new int[numLoops];
			int firstStream = streams[0];
			final List<TreeElem> firstStreamList=new ArrayList<TreeElem>();
			for(TreeElem te:toBuild) {
				if (te.getStream() == firstStream) firstStreamList.add(te);
			}
			final List<List<DataElement>> lists=Collections.synchronizedList(new ArrayList<List<DataElement>>());
			ReduceLoopBody[] threadBody=new ReduceLoopBody[ThreadHandler.instance().getNumThreads()];
			for(int i=0; i<ThreadHandler.instance().getNumThreads(); ++i) {
				threadBody[i]=new ReduceLoopBody() {
					@Override
                    public void execute(int start, int end) {
						HashMap<String,Double> vars = new HashMap<String,Double>();
						List<DataElement> thisList=new ArrayList<DataElement>();
						lists.add(thisList);
						float[] outputPass = new float[eqEntry.size()];
						int[] paramsPass = new int[iters.length + paramEntry.size()];
						for(int i=start; i<end; ++i) {
							TreeElem te=firstStreamList.get(i);
							List<TreeElem> data = tree.getNearPoints(te, ranges);

							DataElement de=getSource(0).getElement(te.getIndex(),te.getStream());
							for(int j=0; j<de.getNumParams(); ++j) vars.put("p"+('i')+j, (double)de.getParam(j));
							for(int j=0; j<de.getNumValues(); ++j) vars.put("v"+('i')+j, (double)de.getValue(j));

							iters[0] = te.getIndex();
							redoRecurTree(streams,iters,1,data,vars,outputPass,paramsPass,thisList);
						}
					}
				};
			}
			ThreadHandler.instance().chunkedForLoop(this, 0, getSource(0).getNumElements(streams[0]),threadBody);
			for(List<DataElement> l:lists) {
				dataVect.get(0).addAll(l);
			}

		} else {
			int numLoops=0;
			for(EditableInt n:streamUse) numLoops+=n.getValue();
			if (numLoops < 2) return;
			int[] streams=genStreams(numLoops);
			int[] iters=new int[numLoops];
			redoRecur(streams,iters,0,null,null,null,dataVect.get(0));
		}
	}

	private void redoRecurTree(int[] streams, int[] iters, int depth, List<TreeElem> data, HashMap<String,Double> vars,float[] output,int[] params,List<DataElement> list) {
		if (streams.length < depth) return;
		
		if (depth >= streams.length) {
			for (int i = 0; i < eqEntry.size(); i++) {
				output[i] = (float)eqEntry.get(i).formula.valueOf(this,0,0,null,vars);
			}

			int i = 0;
			for(; i < paramEntry.size(); i++) {
				params[i] = (int)paramEntry.get(i).formula.valueOf(this, 0, 0, null, vars);
			}
			for(; i < params.length; i++) {
				params[i] = iters[i-paramEntry.size()];
			}

			list.add(new DataElement(params,output));	
		} else {
			for(TreeElem te:data) {
				if (te.getStream() == streams[depth]) {
					DataElement de=getSource(0).getElement(te.getIndex(),te.getStream());
					for(int j=0; j<de.getNumParams(); ++j) vars.put("p"+(char)('i'+depth)+j, (double)de.getParam(j));
					for(int j=0; j<de.getNumValues(); ++j) vars.put("v"+(char)('i'+depth)+j, (double)de.getValue(j));

					iters[depth] = te.getIndex();
					redoRecurTree(streams,iters,depth+1,data,vars,output,params,list);
				}
			}
		}
	}

	private void redoRecur(final int[] streams,final int[] iters,final int depth,final HashMap<String,Double> vars,float[] output,int[] params,List<DataElement> data) {
		if (streams.length < depth) return;

		if (depth >= streams.length) {
			for (int i = 0; i < eqEntry.size(); i++) {
				output[i] = (float)eqEntry.get(i).formula.valueOf(this,0,0,null,vars);
			}

			int i = 0;
			for(; i < paramEntry.size(); i++) {
				params[i] = (int)paramEntry.get(i).formula.valueOf(this, 0, 0, null, vars);
			}
			for(; i < params.length; i++) {
				params[i] = iters[i-paramEntry.size()];
			}

			data.add(new DataElement(params,output));
		} else if (depth == 0) {
			// PARALLEL
			final List<List<DataElement>> lists=Collections.synchronizedList(new ArrayList<List<DataElement>>());
			ReduceLoopBody[] threadBody=new ReduceLoopBody[ThreadHandler.instance().getNumThreads()];
			for(int i=0; i<ThreadHandler.instance().getNumThreads(); ++i) {
				threadBody[i]=new ReduceLoopBody() {
					@Override
                    public void execute(int start, int end) {
						HashMap<String,Double> map = new HashMap<String,Double>();
						List<DataElement> thisList=new ArrayList<DataElement>();
						lists.add(thisList);
						float[] outputPass = new float[eqEntry.size()];
						int[] paramsPass = new int[iters.length + paramEntry.size()];
						for(int i=start; i<end; ++i) {
							DataElement de=getSource(0).getElement(i,streams[depth]);
							for(int j=0; j<de.getNumParams(); ++j) map.put("p"+(char)('i'+depth)+j, (double)de.getParam(j));
							for(int j=0; j<de.getNumValues(); ++j) map.put("v"+(char)('i'+depth)+j, (double)de.getValue(j));
							iters[depth] = i;
							redoRecur(streams,iters,depth+1,map,outputPass,paramsPass,thisList);
						}
					}
				};
			}
			ThreadHandler.instance().chunkedForLoop(this, 0, getSource(0).getNumElements(streams[depth]),threadBody);
			for(List<DataElement> l:lists) {
				data.addAll(l);
			}
		} else {
			for(int i = 0; i < getSource(0).getNumElements(streams[depth]); i++) {
				DataElement de=getSource(0).getElement(i,streams[depth]);
				for(int j=0; j<de.getNumParams(); ++j) vars.put("p"+(char)('i'+depth)+j, (double)de.getParam(j));
				for(int j=0; j<de.getNumValues(); ++j) vars.put("v"+(char)('i'+depth)+j, (double)de.getValue(j));
				iters[depth] = i;
				redoRecur(streams,iters,depth+1,vars,output,params,data);
			}
		}
	}

	@Override
	protected void setupSpecificPanelProperties() {
		JPanel eqPanel = new JPanel(new GridLayout(2,1));
		JPanel paramPanel = new JPanel(new GridLayout(2,1));
		JPanel prunePanel = new JPanel(new GridLayout(2,1));	
		JPanel eqListPanel = new JPanel(new BorderLayout());
		JPanel paramListPanel = new JPanel(new BorderLayout());
		JPanel pruneListPanel = new JPanel(new BorderLayout());
		JPanel formPanel=new JPanel(new BorderLayout());
		JPanel optionPanel = new JPanel(new GridLayout(3,1));
		JPanel streamListPanel = new JPanel(new GridLayout(2,1));
		final JPanel streamPanel = new JPanel(new BorderLayout());
		final JPanel topPanel = new JPanel(new GridLayout(1,1));

		streamListModel=new DefaultListModel();
		streamList=new JList(streamListModel);
		streamList.setMinimumSize(new Dimension(10,100));
		streamList.addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent e) {
				if(streamList.getSelectedIndex()<0) return;
				topPanel.removeAll();
				final int index=streamList.getSelectedIndex();
				topPanel.add(streamUse[index].getLabeledTextField("Number of Times to Use String  ",new EditableInt.Listener() {
					@Override
					public void valueChanged() {
						if(streamUse[index].getValue()<0) {
							JOptionPane.showMessageDialog(null, "You must enter a number greater than 0.");
							streamUse[index].setValue(0);

						}
						streamListModel.clear();
						for(int i=0; i<input.getNumStreams(); ++i) {
							streamListModel.add(i,"Stream "+i + " : " + streamUse[i].getValue());
						}
						streamList.setSelectedIndex(index);
					}
				}));
				streamPanel.validate();
				streamPanel.repaint();
			}
		} );
		streamListPanel.add(new JScrollPane(streamList));
		streamList.setSelectedIndex(0);

		streamPanel.add(topPanel,BorderLayout.NORTH);

		if (input == null) {

		} else if(streamListModel == null || input.getNumStreams()!=streamListModel.size()) {
			int oldStream=streamList.getSelectedIndex();
			streamListModel.clear();
			streamUse = new EditableInt[input.getNumStreams()];
			for(int i=0; i<input.getNumStreams(); ++i) {
				streamListModel.add(i,"Stream "+i + " : " + 0);
				streamUse[i] = new EditableInt(0);
			}
			if(oldStream<input.getNumStreams()) {
				streamList.setSelectedIndex(oldStream);                
			} else {
				streamList.setSelectedIndex(0);
			}
		}     

		streamListPanel.add(streamPanel);

		JButton button=new JButton("Propagate Changes");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
		} );

		optionPanel.add(new JLabel("WARNING: Do not use filter with a large data set."));
		formPanel.add(optionPanel,BorderLayout.NORTH);
		formPanel.add(streamListPanel,BorderLayout.CENTER);
		formPanel.add(button,BorderLayout.SOUTH);
		propPanel.add("Streams",formPanel);


		// ------------------------ Value Panel ----------------------	
		eqDisplayPanel = new JPanel(new GridLayout(1,1));
		formPanel=new JPanel(new BorderLayout());
		optionPanel = new JPanel(new GridLayout(1,1));

		eqList=new JList();
		eqList.addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent e) { eqListSelection(); }
		} );
		eqList.setListData(eqEntry.toArray());
		eqList.setMinimumSize(new Dimension(10,100));
		eqListPanel.add(new JScrollPane(eqList),BorderLayout.CENTER);

		JPanel tmpPanel=new JPanel(new GridLayout(1,2));
		JButton newButton=new JButton("New");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { newEq(); }
		} );
		tmpPanel.add(newButton);
		newButton=new JButton("Remove");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { removeEq(); }
		} );
		tmpPanel.add(newButton);
		eqListPanel.add(tmpPanel,BorderLayout.SOUTH);

		button = new JButton("Mirror Values");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { mirrorVals(); }
		} );
		optionPanel.add(button);

		eqListPanel.add(optionPanel,BorderLayout.NORTH);

		eqPanel.add(eqListPanel);
		eqPanel.add(eqDisplayPanel);

		button=new JButton("Propagate Changes");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
		} );

		formPanel.add(eqPanel,BorderLayout.CENTER);
		formPanel.add(button,BorderLayout.SOUTH);
		propPanel.add("Values",formPanel);


		// ------------------------ Parameter Panel ----------------------		
		paramDisplayPanel = new JPanel(new GridLayout(1,1));
		formPanel=new JPanel(new BorderLayout());
		optionPanel = new JPanel(new GridLayout(1,1));

		paramList=new JList();
		paramList.addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent e) { paramListSelection(); }
		} );
		paramList.setListData(paramEntry.toArray());
		paramList.setMinimumSize(new Dimension(10,100));
		paramListPanel.add(new JScrollPane(paramList),BorderLayout.CENTER);

		tmpPanel=new JPanel(new GridLayout(1,2));
		newButton=new JButton("New");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { newParam(); }
		} );
		tmpPanel.add(newButton);
		newButton=new JButton("Remove");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { removeParam(); }
		} );
		tmpPanel.add(newButton);
		paramListPanel.add(tmpPanel,BorderLayout.SOUTH);

		button = new JButton("Mirror Parameters");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { mirrorParams(); }
		} );
		optionPanel.add(button);

		paramListPanel.add(optionPanel,BorderLayout.NORTH);

		paramPanel.add(paramListPanel);
		paramPanel.add(paramDisplayPanel);

		button=new JButton("Propagate Changes");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
		} );

		formPanel.add(paramPanel,BorderLayout.CENTER);
		formPanel.add(button,BorderLayout.SOUTH);
		propPanel.add("Parameters",formPanel);


		// ------------------------ Pruning Panel ----------------------	
		pruneDisplayPanel = new JPanel(new GridLayout(1,1));
		formPanel=new JPanel(new BorderLayout());
		optionPanel = new JPanel(new GridLayout(1,1));

		pruneListPanel.add(eqPanel.add(useTree.getCheckBox("Use Spatial Tree for Data", new EditableBoolean.Listener() {
			@Override
			public void valueChanged() {
				if(useTree.getValue()) newPrune(true);
			}

		})),BorderLayout.NORTH);
		formPanel.add(pruneListPanel,BorderLayout.CENTER);

		pruneList=new JList();
		pruneList.addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent e) { pruneListSelection(); }
		} );
		pruneList.setListData(pruneEntry.toArray());
		pruneList.setMinimumSize(new Dimension(10,100));
		pruneListPanel.add(new JScrollPane(pruneList),BorderLayout.CENTER);

		tmpPanel=new JPanel(new GridLayout(1,2));
		newButton=new JButton("New");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { newPrune(false); }
		} );
		tmpPanel.add(newButton);
		newButton=new JButton("Remove");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { removePrune(); }
		} );
		tmpPanel.add(newButton);
		pruneListPanel.add(tmpPanel,BorderLayout.SOUTH);

		prunePanel.add(pruneListPanel);
		prunePanel.add(pruneDisplayPanel);


		button=new JButton("Propagate Changes");
		button.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
		} );

		formPanel.add(prunePanel,BorderLayout.CENTER);
		formPanel.add(button,BorderLayout.SOUTH);
		propPanel.add("Pruning",formPanel);
	}

	private void mirrorVals() {
		if(getNumSources() < 1) return;
		int letter = 0;
		for(int i = 0; i < getSource(0).getNumStreams(); ++i) {
			for(int j = 0; j < streamUse[i].getValue(); ++j) {
				System.out.println(i + "," + j);
				DataElement de=getSource(0).getElement(i,i);
				for(int k=0; k<de.getNumValues(); ++k) {
					eqEntry.add(new FunctionEntry("v"+(char)('i'+letter)+k,"v"+(char)('i'+letter)+k));
				}
				letter++;
			}
		}
		eqList.setListData(eqEntry.toArray());
	}

	private void mirrorParams() {
		if(getNumSources() < 1) return;
		int letter = 0;
		for(int i = 0; i < getSource(0).getNumStreams(); ++i) {
			for(int j = 0; j < streamUse[i].getValue(); ++j) {
				DataElement de=getSource(0).getElement(i,j);
				for(int k=0; k<de.getNumParams(); ++k) {
					paramEntry.add(new FunctionEntry("p"+(char)('i'+letter)+k,"p"+(char)('i'+letter)+k));
				}
				letter++;
			}
		}
		paramList.setListData(paramEntry.toArray());
	}


	private void newEq() {
		eqEntry.add(new FunctionEntry("New Value","1"));
		eqList.setListData(eqEntry.toArray());
	}

	private void removeEq() {
		if(eqList.getSelectedIndex()<0) return;
		eqEntry.remove(eqList.getSelectedIndex());
		eqList.setListData(eqEntry.toArray());
	}

	private void eqListSelection() {
		if(eqList.getSelectedIndex()<0) return;
		eqDisplayPanel.removeAll();
		eqDisplayPanel.add(eqEntry.get(eqList.getSelectedIndex()).getSettingsPanel());
		eqDisplayPanel.validate();
		eqDisplayPanel.repaint();
	}

	private void newParam() {
		paramEntry.add(new FunctionEntry("New Value","1"));
		paramList.setListData(paramEntry.toArray());
	}

	private void removeParam() {
		if(paramList.getSelectedIndex()<0) return;
		paramEntry.remove(paramList.getSelectedIndex());
		paramList.setListData(paramEntry.toArray());
	}

	private void paramListSelection() {
		if(paramList.getSelectedIndex()<0) return;
		paramDisplayPanel.removeAll();
		paramDisplayPanel.add(paramEntry.get(paramList.getSelectedIndex()).getSettingsPanel());
		paramDisplayPanel.validate();
		paramDisplayPanel.repaint();
	}

	private void newPrune(boolean fromBox) {
		if (fromBox) {
			if (pruneEntry.size() == 0) {
				pruneEntry.add(new TreeEntry("Axis " + pruneEntry.size(),"1"));
				pruneList.setListData(pruneEntry.toArray());
			}
			return;
		}
		useTree.setValue(true);
		pruneEntry.add(new TreeEntry("Axis " + pruneEntry.size(),"1"));
		pruneList.setListData(pruneEntry.toArray());
	}

	private void removePrune() {
		if(pruneList.getSelectedIndex()<0) return;
		pruneEntry.remove(pruneList.getSelectedIndex());
		if (pruneEntry.size() < 1) useTree.setValue(false);
		pruneList.setListData(pruneEntry.toArray());
	}

	private void pruneListSelection() {
		if(pruneList.getSelectedIndex()<0) return;
		pruneDisplayPanel.removeAll();
		pruneDisplayPanel.add(pruneEntry.get(pruneList.getSelectedIndex()).getSettingsPanel());
		pruneDisplayPanel.validate();
		pruneDisplayPanel.repaint();
	}

	@Override
	public int getNumParameters(int stream) {
		if(input==null) return 0;
		int numLoops=0;
		for(EditableInt n:streamUse) numLoops+=n.getValue();
		return numLoops + paramEntry.size();
	}

	@Override
	public int getNumValues(int stream) {
		if(input==null) return 0;
		return eqEntry.size();
	}

	@Override
	public String getParameterDescription(int stream, int which) {
		if(input==null) return "None";
		if (which < paramEntry.size()) {
			return paramEntry.get(which).toString();
		}
		return "Stream "+(char)('i'+which-paramEntry.size())+" index";
	}

	@Override
	public String getValueDescription(int stream, int which) {
		if(input==null) return "None";
		return eqEntry.get(which).toString();
	}

	@Override
	public GraphElement copy(List<GraphElement> l) {
		return new CorrelationFilter(this,l);
	}

	@Override
    public String getDescription() { return "Correlation Filter"; }

	public static String getTypeDescription(){ return "Correlation Filter"; }



	private void updateStreamList() {
		if (streamList != null && input.getNumStreams() > 0) {
			if(input.getNumStreams()!=streamListModel.size()) {
				int oldStream=streamList.getSelectedIndex();
				streamListModel.clear();
				streamUse = new EditableInt[input.getNumStreams()];
				for(int i=0; i<input.getNumStreams(); ++i) {
					streamUse[i].setValue(0);
					streamListModel.add(i,"Stream "+i + " : " + streamUse[i].getValue());
				}
				if(oldStream<input.getNumStreams()) {
					streamList.setSelectedIndex(oldStream);                
				} else {
					streamList.setSelectedIndex(0);
				}
			}
		}
	}

	private List<TreeElem> getTreeBuildPoints() {
		List<TreeElem> ret = new ArrayList<TreeElem>();
		for(int i = 0; i < streamUse.length; i++) {
			if (streamUse[i].getValue() != 0) {
				for(int j = 0; j < input.getNumElements(i); j++) {
					double[] data = new double[pruneEntry.size()];
					for(int k = 0; k < data.length; k++) {
						data[k] = pruneEntry.get(k).formula.valueOf(this, i, j);
					}
					ret.add(new TreeElem(i,j,data));
				}
			}
		}
		return ret;
	}

	private double[] getTreeRanges() {
		double[] ret = new double[pruneEntry.size()];

		for(int i = 0; i < ret.length; i++) {
			ret[i] = pruneEntry.get(i).range.getValue();
		}

		return ret;
	}

	private static class TreeElem implements TreePoint {
		private final double[] data;
		private final int index,stream;

		public TreeElem(int s, int i, double[] dat) {
			index = i;
			stream = s;
			data = dat;
		}

		public int getIndex() 			{	return index;	}
		public int getStream() 			{	return stream;	}
		@Override
        public double getVal(int val) 	{	return data[val];	}
		@Override
		public String toString() {
			String ret = "Stream: " + stream + " index: " + index + "vals: ";
			for(int i = 0; i < data.length; i++) {
				ret = ret + " " + data[i];
			}
			return ret;
		}
	}

	private class TreeEntry implements Serializable {
		public TreeEntry(String desc,String form) {
			formula=new DataFormula(form);
			range=new EditableDouble(0.0);
			description=new EditableString(desc);
		}

		public TreeEntry(TreeEntry c) {
			formula=new DataFormula(c.formula);
			description=c.description;
		}

		@Override
        public String toString() { return description.getValue(); }

		public JPanel getSettingsPanel() {
			if (settingsPanel == null) {
				settingsPanel = new JPanel(new BorderLayout());
				JPanel outerPanel = new JPanel(new BorderLayout());
				JPanel tmpPanel = new JPanel(new GridLayout(3, 1));
				tmpPanel.add(new JLabel("Description"));
				tmpPanel.add(new JLabel("Tree Axis Equation"));
				tmpPanel.add(new JLabel("Compare With Points Within  "));

				outerPanel.add(tmpPanel, BorderLayout.WEST);
				tmpPanel = new JPanel(new GridLayout(3, 1));

				tmpPanel.add(description.getTextField(new EditableString.Listener() {
					@Override
                    public void valueChanged() {
						eqList.setListData(eqEntry.toArray());        
					}
				}));

				tmpPanel.add(formula.getTextField(null));
				tmpPanel.add(range.getTextField(null));

				outerPanel.add(tmpPanel, BorderLayout.CENTER);
				settingsPanel.add(outerPanel, BorderLayout.NORTH);
			}
			return settingsPanel;
		}

		public DataFormula formula;
		public EditableString description;
		public EditableDouble range;
		private transient JPanel settingsPanel;
		private static final long serialVersionUID=3250987326l;
	}

	private class FunctionEntry implements Serializable {
		public FunctionEntry(String desc,String form) {
			formula=new DataFormula(form);
			description=new EditableString(desc);
		}

		public FunctionEntry(FunctionEntry c) {
			formula=new DataFormula(c.formula);
			description=c.description;
		}

		@Override
        public String toString() { return description.getValue(); }

		public JPanel getSettingsPanel() {
			if (settingsPanel == null) {
				settingsPanel = new JPanel(new BorderLayout());
				JPanel outerPanel = new JPanel(new BorderLayout());
				JPanel tmpPanel = new JPanel(new GridLayout(2, 2));
				tmpPanel.add(new JLabel("Description"));
				tmpPanel.add(new JLabel("Expression"));

				outerPanel.add(tmpPanel, BorderLayout.WEST);
				tmpPanel = new JPanel(new GridLayout(2, 1));

				tmpPanel.add(description.getTextField(new EditableString.Listener() {
					@Override
                    public void valueChanged() {
						eqList.setListData(eqEntry.toArray());        
					}
				}));

				tmpPanel.add(formula.getTextField(null));
				outerPanel.add(tmpPanel, BorderLayout.CENTER);
				settingsPanel.add(outerPanel, BorderLayout.NORTH);
			}
			return settingsPanel;
		}

		public DataFormula formula;
		public EditableString description;
		private transient JPanel settingsPanel;
		private static final long serialVersionUID=3250987326l;
	}
}