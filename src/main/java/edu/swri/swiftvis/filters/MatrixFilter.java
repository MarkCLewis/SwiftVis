package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;
import edu.swri.swiftvis.util.Matrix;
import edu.swri.swiftvis.util.ReduceLoopBody;
import edu.swri.swiftvis.util.ThreadHandler;
import edu.swri.swiftvis.util.Vector;

/**
 * This filter is used to apply matrix transformations to vectors created from input data. Since the user
 * may not necessarily like to apply a transformation to all input values, the filter allows for any number
 * of input vectors to be created from the input data, for which the user may assign a universal length and
 * a minimum index range. All input vectors have the same length, and the number of rows in the matrix will
 * always match this value. The number of columns is open to the user. If the number of columns is greater
 * or smaller than the number of rows, the output data will be sized accordingly.
 * 
 * If a square matrix is used, the filter provides options for symmetric entry fills (in this case, the user
 * inputs values on the lower half of the matrix diagonal, and the filter automatically fills in the upper
 * half accordingly) as well as rotations. Rotations can be made between any two axes by any angle (in radians).
 * Applying a rotation will cause the appropriate rotational matrix to be multiplied in to the current matrix
 * in the filter, creating a composition.
 * 
 * @author Matt Maly
 */
public class MatrixFilter extends AbstractMultipleSourceFilter
{
	private static final long serialVersionUID = 4202161898875320218L;

	//Contains the vectors to which the matrix transformation is applied.
	private List<VectorEntry> vectors;
	//Contains the length of each vector. The matrix will always have this number of rows,
	//to ensure that the transformation operation is well-defined.
	private EditableInt vectorNumElements;
	//Maps the index of each output value to the index of its corresponding source value, if it has one.
	private int[] indexMapping;
	
	private transient JList vectorList;
	private transient JPanel inputSettingsPanel;
	private transient JPanel vectorTab;
	
	private MatrixModel matrixModel;
	private transient JPanel matrixTab;
	
	public MatrixFilter() {
		vectors = new ArrayList<VectorEntry>();
		vectorNumElements = new EditableInt(0);
		indexMapping = new int[0];
		matrixModel = new MatrixModel();
	}
	
	private MatrixFilter(MatrixFilter m, List<GraphElement> l) {
		super(m, l);
		vectors = new ArrayList<VectorEntry>();
		for (VectorEntry vector : m.vectors)
			vectors.add(new VectorEntry(vector));
		vectorNumElements = new EditableInt(m.vectorNumElements.getValue());
		indexMapping = Arrays.copyOf(m.indexMapping, m.indexMapping.length);
		matrixModel = new MatrixModel(m.matrixModel);
	}
	
	@Override
    protected boolean doingInThreads() {
		return true;
	}

	@Override
    protected void redoAllElements() {
		sizeDataVectToInputStreams();
		final DataSource ds = inputVector.get(0);
		final int indexBounds[] = { 0, ds.getNumElements(0) };
		for (int row = 0; row < matrixModel.getRowCount(); row++) {
			for (int col = 0; col < matrixModel.getColumnCount(); col++)
				DataFormula.mergeSafeElementRanges(indexBounds,
						matrixModel.entries[row][col].getSafeElementRange(this, 0));
		}
		DataFormula.checkRangeSafety(indexBounds, this);
		
		dataVect.get(0).ensureCapacity(indexBounds[1] - indexBounds[0]);
		for (int i = indexBounds[0]; i < indexBounds[1]; i++)
			dataVect.get(0).add(null);
		
		final boolean[] valueInUse = getValuesInUse(ds);
		int count = 0;
		for (int k = 0; k < valueInUse.length; k++) {
			if (!valueInUse[k])
				count++;
		}
		final int numPassThrough = count;
			
		ReduceLoopBody[] threads = new ReduceLoopBody[ThreadHandler.instance().getNumThreads()];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new ReduceLoopBody() {
				@Override
                public void execute(int start, int end) {
					Matrix matrix = new Matrix(matrixModel.getRowCount(), matrixModel.getColumnCount());
					for (int j = start; j < end; j++) {
						for (int row = 0; row < matrix.rows(); row++) {
							for (int col = 0; col < matrix.columns(); col++) {
								DataFormula entry = matrixModel.entries[row][col].getParallelCopy();
								matrix.set(row, col, entry.valueOf(MatrixFilter.this, 0, j));
							}
						}
						
						DataElement dataRow = ds.getElement(j, 0);
						int[] parameters = new int[dataRow.getNumParams()];
						for (int k = 0; k < parameters.length; k++)
							parameters[k] = dataRow.getParam(k);
						
						float[] resultValues;
						if (matrixModel.getRowCount() > 0)
							resultValues = new float[vectors.size()*matrix.columns() + numPassThrough];
						else
							resultValues = new float[numPassThrough];
						
						indexMapping = new int[resultValues.length];
						for (int k = 0; k < indexMapping.length; k++)
							indexMapping[k] = -1;
						
						//Since the use of non-square matrices in this filter can cause the output elements to have
						//fewer or more values than the input elements, we use two indexing variables here.
						//The value k applies to the values from the input elements, whereas resultIndex applies
						//to the values from the output elements.
						int resultIndex = 0;
						for (int k = 0; k < valueInUse.length; k++) {
							if (!valueInUse[k]) {
								resultValues[resultIndex] = dataRow.getValue(k);
								if (j == 0)
									indexMapping[resultIndex] = k;
								resultIndex++;
							}
							else if (matrixModel.getRowCount() > 0) {
								//For each index k, we apply the matrix transformation to all nonempty vectors
								//for which k is a range minimum.
								for (VectorEntry input : vectors) {
									if (k == input.getRangeMin()) {
										Vector vector = input.getValues(dataRow);
										Matrix result = vector.multiply(matrix);
										for (int l = 0; l < result.columns(); l++) {
											resultValues[l + resultIndex] = (float) result.get(0, l);
											if (j == 0 && l < input.getNumElements())
												indexMapping[l + resultIndex] = k + l;
										}
										resultIndex += result.columns();
									}
								}
							}
						}
						dataVect.get(0).set(j - indexBounds[0], DataElement.replaceValues(dataRow, resultValues));
					}
				}
			};
		}
		ThreadHandler.instance().chunkedForLoop(this, indexBounds[0], indexBounds[1], threads);
	}
	
	//Returns an array containing whether a source value, referenced by
	//its index in the source, is included in a vector.
	private boolean[] getValuesInUse(DataSource ds) {
		boolean[] valueInUse = new boolean[ds.getNumValues(0)];
		for (int i = 0; i < valueInUse.length; i++)
			valueInUse[i] = false;
		
		for (VectorEntry input : vectors) {
			for (int i = 0; i < input.getNumElements() && i + input.getRangeMin() < valueInUse.length; i++)
				valueInUse[i + input.getRangeMin()] = true;
		}
		return valueInUse;
	}

	@Override
    protected void setupSpecificPanelProperties() {
		JButton mirrorButton = new JButton("Use All Input Values");
		mirrorButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				mirrorVectorValues();
			}
		});
		vectorList = new JList(vectors.toArray());
		vectorList.addListSelectionListener(new ListSelectionListener() {
			@Override
            public void valueChanged(ListSelectionEvent e) {
				vectorListSelection();
			}
		});
		JButton newButton = new JButton("New Vector");
		newButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				newVector();
			}
		});
		JButton remButton = new JButton("Remove Vector");
		remButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				removeVector();
			}
		});
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(newButton);
		buttonPanel.add(remButton);
		
		JPanel topPanel = new JPanel(new GridLayout(2, 1));
		topPanel.add(vectorNumElements.getLabeledTextField("Vector Length", new EditableInt.Listener() {
			@Override
            public void valueChanged() {
				if (vectorNumElements.getValue() < 0) {
					JOptionPane.showMessageDialog(vectorNumElements.getTextField(this),
							"You have to enter a non-negative integer here.");
					vectorNumElements.getTextField(this).requestFocus();
				}
				
				else {
					matrixModel.reset();
					matrixModel.fireTableStructureChanged();
				}
			}
		}));
		topPanel.add(mirrorButton);

		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(topPanel, BorderLayout.NORTH);
		listPanel.add(new JScrollPane(vectorList), BorderLayout.CENTER);
		listPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		inputSettingsPanel = new JPanel(new GridLayout(1, 1));
		JPanel vectorPanel = new JPanel(new BorderLayout());
		vectorPanel.add(listPanel, BorderLayout.NORTH);
		vectorPanel.add(inputSettingsPanel, BorderLayout.SOUTH);
		
		JButton propButton = new JButton("Propagate Changes");
		propButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				abstractRedoAllElements();
			}
		});
		
		vectorTab = new JPanel(new BorderLayout());
		vectorTab.add(vectorPanel, BorderLayout.NORTH);
		vectorTab.add(propButton, BorderLayout.SOUTH);
		
		propButton = new JButton("Propagate Changes");
		propButton.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
				abstractRedoAllElements();
			}
		});
		
		matrixTab = new JPanel(new BorderLayout());
		matrixTab.add(matrixModel.getMatrixPanel(), BorderLayout.CENTER);
		matrixTab.add(propButton, BorderLayout.SOUTH);
		
		propPanel.add("Input Vectors", vectorTab);
		propPanel.add("Matrix Settings", matrixTab);
	}

	@Override
    public String getParameterDescription(int stream, int which) {
		return inputVector.get(0).getParameterDescription(stream, which);
	}

	@Override
    public String getValueDescription(int stream, int which) {
		//Since we can't easily be sure if the matrix transformation has added or
		//removed value columns, we rely on the index map to pull the appropriate
		//value descriptions. If an output value index does not have a mapping,
		//then it is a fresh value generated by the transformation and does not have
		//a description.
		int oldIndex = indexMapping[which];
		if (oldIndex == -1)
			return "Not Specified";
		else
			return inputVector.get(0).getValueDescription(stream, oldIndex);
	}

	@Override
    public GraphElement copy(List<GraphElement> l) {
		return new MatrixFilter(this, l);
	}

	@Override
    public String getDescription() {
		return "Matrix Filter";
	}

	public static String getTypeDescription() {
		return "Matrix Filter";
	}
	
	private void mirrorVectorValues() {
		if (inputVector.size() > 0) {
			DataSource ds = inputVector.get(0);
			if (vectorNumElements.getValue() != ds.getNumValues(0)) {
				vectorNumElements.setValue(ds.getNumValues(0));
				matrixModel.reset();
				matrixModel.fireTableStructureChanged();
			}
			vectors.clear();
			vectors.add(new VectorEntry(0, ds.getDescription()));
			vectorList.setListData(vectors.toArray());
		}
	}
	
	private void vectorListSelection() {
		VectorEntry row = (VectorEntry) vectorList.getSelectedValue();
		if (row != null) {
			inputSettingsPanel.removeAll();
			inputSettingsPanel.add(row.getSettingsPanel());
			vectorTab.validate();
			vectorTab.repaint();
		}
	}
	
	private void newVector() {
		vectors.add(new VectorEntry(0, "New Vector"));
		vectorList.setListData(vectors.toArray());
	}
	
	private void removeVector() {
		int index = vectorList.getSelectedIndex();
		if (index >= 0) {
			vectors.remove(index);
			vectorList.setListData(vectors.toArray());
		}
	}
	
	private class VectorEntry implements Serializable, EditableString.Listener {
		private static final long serialVersionUID = -3882071206991271831L;
		
		private EditableInt rangeMin;
		private EditableString description;
		private transient JPanel settingsPanel;
		
		public VectorEntry(int min, String desc) {
			rangeMin = new EditableInt(min);
			description = new EditableString(desc);
		}
		
		public VectorEntry(VectorEntry v) {
			rangeMin = new EditableInt(v.rangeMin.getValue());
			description = new EditableString(v.description.getValue());
		}
		
		public JPanel getSettingsPanel() {
			if (settingsPanel == null) {
				settingsPanel = new JPanel(new GridLayout(2, 1));
				settingsPanel.add(rangeMin.getLabeledTextField("Range Minimum", null));
				settingsPanel.add(description.getLabeledTextField("Description", this));
			}
			return settingsPanel;
		}
		
		@Override
        public String toString() {
			return description.getValue();
		}
		
		public int getRangeMin() {
			return rangeMin.getValue();
		}
		
		public int getNumElements() {
			return vectorNumElements.getValue();
		}
		
		//Returns a Vector containing the values referenced by this VectorEntry.
		public Vector getValues(DataElement e) {
			int range = getNumElements();
			Vector values = new Vector(range);
			for (int i = 0; i < range; i++)
				values.set(i, e.getValue(i + getRangeMin()));
			return values;
		}
		
		@Override
        public void valueChanged() {
			vectorTab.validate();
			vectorTab.repaint();
		}
	}
	
	private class MatrixModel extends AbstractTableModel {
		private static final long serialVersionUID = 2305274670558397314L;
		
		private DataFormula[][] entries;
		private EditableInt matrixColumns;
		private EditableBoolean squareMatrix;
		private transient JTextField columnsField;
		private transient JPanel matrixPanel;
		private transient RotationMenu rotationMenu;
		
		public MatrixModel() {
			matrixColumns = new EditableInt(vectorNumElements.getValue());
			squareMatrix = new EditableBoolean(true);
			reset();
		}
		
		public MatrixModel(MatrixModel m) {
			entries = new DataFormula[m.getRowCount()][m.getColumnCount()];
			for (int i = 0; i < entries.length; i++) {
				for (int j = 0; j < entries[0].length; j++)
					entries[i][j] = new DataFormula(m.entries[i][j].getFormula());
			}
			matrixColumns = new EditableInt(m.matrixColumns.getValue());
			squareMatrix = new EditableBoolean(m.squareMatrix.getValue());
		}
		
		public void reset() {
			if (rotationMenu != null)
				rotationMenu.setVisible(false);
			if (squareMatrix.getValue() && matrixColumns.getValue() != getRowCount())
				matrixColumns.setValue(getRowCount());
			entries = new DataFormula[vectorNumElements.getValue()][matrixColumns.getValue()];
			fillIdentity();
		}
		
		private void fillIdentity() {
			for (int i = 0; i < entries.length; i++) {
				for (int j = 0; j < entries[0].length; j++)
					entries[i][j] = new DataFormula(i == j ? "1" : "0");
			}
		}
		
		private void fillSymmetrically() {
			for (int i = 0; i < entries.length; i++) {
				for (int j = i + 1; j < entries[0].length; j++)
					entries[i][j] = new DataFormula(entries[j][i].getFormula());
			}
		}
		
		@Override
        public int getRowCount() {
			return vectorNumElements.getValue();
		}
		
		@Override
        public int getColumnCount() {
			return matrixColumns.getValue();
		}
		
		@Override
        public String getColumnName(int column) {
			return null;
		}
		
		@Override
        public boolean isCellEditable(int row, int column) {
			return true;
		}

		@Override
        public Object getValueAt(int row, int column) {
			return entries[row][column].getFormula();
		}
		
		@Override
        public void setValueAt(Object o, int row, int column) {
			entries[row][column].setFormula(o.toString());
			entries[row][column].simplify();
			entries[row][column].rebuildFormulaString();
		}
		
		public JPanel getMatrixPanel() {
			if (matrixPanel == null) {
				matrixPanel = new JPanel(new BorderLayout());
				matrixPanel.setBorder(BorderFactory.createTitledBorder("Matrix Transformation"));
				rotationMenu = new RotationMenu();
				
				columnsField = matrixColumns.getTextField(new EditableInt.Listener() {
					@Override
                    public void valueChanged() {
						if (matrixColumns.getValue() < 1) {
							JOptionPane.showMessageDialog(matrixColumns.getTextField(this),
									"You have to enter a positive integer here.");
							matrixColumns.getTextField(this).requestFocus();
						}
						else {
							reset();
							fireTableStructureChanged();
							matrixTab.validate();
							matrixTab.repaint();
						}
					}
				});
				columnsField.setColumns(5);
				columnsField.setEnabled(!squareMatrix.getValue());
				JPanel columnsPanel = new JPanel(new BorderLayout());
				columnsPanel.add(new JLabel("Number of Columns"), BorderLayout.WEST);
				columnsPanel.add(columnsField, BorderLayout.CENTER);
				
				JPanel optionsPanel = new JPanel(new BorderLayout());
				optionsPanel.add(squareMatrix.getCheckBox("Use Square Matrix", new EditableBoolean.Listener() {
					@Override
                    public void valueChanged() {
						columnsField.setEnabled(!squareMatrix.getValue());
						if (squareMatrix.getValue() && matrixColumns.getValue() != getRowCount()) {
							reset();
							fireTableStructureChanged();
							matrixTab.validate();
							matrixTab.repaint();
						}
					}
				}), BorderLayout.WEST);
				optionsPanel.add(columnsPanel, BorderLayout.EAST);
				
				JButton resetButton = new JButton("Reset");
				resetButton.addActionListener(new ActionListener() {
					@Override
                    public void actionPerformed(ActionEvent e) {
						reset();
						fireTableStructureChanged();
						matrixTab.validate();
						matrixTab.repaint();
					}
				});
				JButton symmetryButton = new JButton("Fill Symmetrically");
				symmetryButton.addActionListener(new ActionListener() {
					@Override
                    public void actionPerformed(ActionEvent e) {
						if (squareMatrix.getValue() && getRowCount() > 1) {
							fillSymmetrically();
							fireTableStructureChanged();
							matrixTab.validate();
							matrixTab.repaint();
						}
						else
							JOptionPane.showMessageDialog(matrixTab, "Symmetry can only be applied to square matrices in at least two dimensions.");
					}
				});
				JButton rotateButton = new JButton("Add Rotation");
				rotateButton.addActionListener(new ActionListener() {
					@Override
                    public void actionPerformed(ActionEvent e) {
						if (squareMatrix.getValue() && getRowCount() > 1)
							rotationMenu.setVisible(true);
						else
							JOptionPane.showMessageDialog(matrixTab, "Rotations can only be done on square matrices in at least two dimensions.");
					}
				});
				
				JPanel buttonsPanel = new JPanel(new GridLayout(3, 1));
				buttonsPanel.add(resetButton);
				buttonsPanel.add(symmetryButton);
				buttonsPanel.add(rotateButton);
				optionsPanel.add(buttonsPanel, BorderLayout.SOUTH);
				
				JTable matrixTable = new JTable(this);
				matrixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				matrixTable.setRowSelectionAllowed(false);
				matrixTable.setColumnSelectionAllowed(false);
				matrixTable.setPreferredScrollableViewportSize(matrixTable.getPreferredSize());
				
				matrixPanel.add(optionsPanel, BorderLayout.NORTH);
				matrixPanel.add(new JScrollPane(matrixTable), BorderLayout.CENTER);
			}
			return matrixPanel;
		}
		
		private class RotationMenu extends JFrame {
			private static final long serialVersionUID = -1821237138097244627L;
			
			private EditableDouble angle;
			private transient JComboBox sourceAxis, destAxis;
			
			public RotationMenu() {
				super("Matrix Rotation");
				super.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
				super.setLayout(new GridLayout(3, 1));
				angle = new EditableDouble(0.0);
			}
			
			public void performRotation() {
				int s = sourceAxis.getSelectedIndex();
				int d = destAxis.getSelectedIndex();
				String theta = String.valueOf(angle.getValue());
				String[][] rotation = new String[getRowCount()][getColumnCount()];
				for (int i = 0; i < rotation.length; i++) {
					for (int j = 0; j < rotation[0].length; j++)
						rotation[i][j] = (i == j ? "1" : "0");
				}
				//Since we are doing vector*matrix instead of matrix*vector,
				//we must switch the signs of the sine terms to ensure a proper rotation.
				rotation[s][s] = "cos(" + theta + ")";
				rotation[s][d] = "sin(" + theta + ")";
				rotation[d][s] = "(-sin(" + theta + "))";
				rotation[d][d] = "cos(" + theta + ")";
				
				String[][] matrix = new String[getRowCount()][getColumnCount()];
				for (int i = 0; i < getRowCount(); i++) {
					for (int j = 0; j < getColumnCount(); j++)
						matrix[i][j] = entries[i][j].getFormula();
				}

				List<String> summands = new ArrayList<String>(getRowCount());
				for (int i = 0; i < getRowCount(); i++) {
					for (int j = 0; j < getColumnCount(); j++) {
						summands.clear();
						for (int k = 0; k < rotation.length; k++)
							summands.add("(" + matrix[i][k] + ")*" + rotation[k][j]);
						String entry = "0";
						if (summands.size() > 0) {
							entry = summands.remove(0);
							for (String summand : summands)
								entry += "+" + summand;
						}
						setValueAt(entry, i, j);
					}
				}
			}
			
			@Override
            public void setVisible(boolean visible) {
				if (visible) {
					if (sourceAxis == null) {
						final JPanel axesPanel = new JPanel(new FlowLayout());
						axesPanel.add(new JLabel("Rotate"));
						axesPanel.add(sourceAxis = new JComboBox());
						axesPanel.add(new JLabel("into"));
						axesPanel.add(destAxis = new JComboBox());
						
						JButton rotateButton = new JButton("Apply Rotation");
						rotateButton.addActionListener(new ActionListener() {
							@Override
                            public void actionPerformed(ActionEvent e) {
								if (sourceAxis.getSelectedIndex() == destAxis.getSelectedIndex())
									JOptionPane.showMessageDialog(axesPanel, "An axis cannot be rotated into itself.");
								else {
									performRotation();
									fireTableStructureChanged();
									matrixTab.validate();
									matrixTab.repaint();
									setVisible(false);
								}
							}
						});
						super.add(axesPanel);
						super.add(angle.getLabeledTextField("Angle of Rotation", null));
						super.add(rotateButton);
					}
					
					if (getRowCount() < sourceAxis.getItemCount()) {
						sourceAxis.removeAllItems();
						destAxis.removeAllItems();
						for (int i = 0; i < getRowCount(); i++) {
							sourceAxis.addItem("Axis " + i);
							destAxis.addItem("Axis " + i);
						}
					}
					else {
						//In the case that the number of rows has increased,
						//we can simply add more entries to the combo boxes.
						for (int i = sourceAxis.getItemCount(); i < getRowCount(); i++) {
							sourceAxis.addItem("Axis " + i);
							destAxis.addItem("Axis " + i);
						}
					}
					
					//We can do this because the code does not allow 1-by-1 matrix rotations,
					//and so there is always more than one axis.
					if (sourceAxis.getSelectedIndex() == destAxis.getSelectedIndex())
						destAxis.setSelectedIndex(1);
					super.pack();
				}
				super.setVisible(visible);
			}
		}
	}
}