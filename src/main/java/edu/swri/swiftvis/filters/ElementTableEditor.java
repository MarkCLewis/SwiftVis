/*
 * Created on Jun 15, 2005
 */
package edu.swri.swiftvis.filters;



import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.GraphPanel;

/**
 * ElementTableEditor is a Filter that provides the user with the ability to
 * create their own DataSource by manually adding Parameters and Values and then
 * filling in their own data.  It can also be used to manually edit specific elements
 * of one of its DataSources.  To choose the DataSource to edit, switch to the Sources
 * tab in the properties panel and move the desired DataSource to the d[0] position.<BR>
 * <BR>
 * <b>NOTE:</b> <i>Changing which DataSource you are using in the Sources tab will override
 * any data that you have changed in the TableEditor.</i>
 * @author Mark Lewis
 */
public class ElementTableEditor extends AbstractSingleSourceFilter {
    public ElementTableEditor() {
    }
    
    public ElementTableEditor(ElementTableEditor c,List<GraphElement> l) {
        super(c,l);
        paramNames=new ArrayList<String>(c.paramNames);
        valueNames=new ArrayList<String>(c.valueNames);
    }

	@Override
    protected boolean doingInThreads() {
		return false;
	}
	
    @Override
    protected void redoAllElements() {
        paramNames.clear();
        for(int i=0; i<input.getNumParameters(0); ++i) paramNames.add(input.getParameterDescription(0, i));
        valueNames.clear();
        for(int i=0; i<input.getNumValues(0); ++i) valueNames.add(input.getValueDescription(0, i));
        for(int i=0; i<input.getNumElements(0); ++i) {
            dataVect.get(0).add(input.getElement(i, 0));
        }
        if(model!=null) model.fireTableDataChanged();
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel editPanel=new JPanel(new BorderLayout());
        
        JPanel northPanel=new JPanel(new GridLayout(2,1));
        
        JPanel tmp=new JPanel();
        JButton button=new JButton("Add Parameter");
        button.addActionListener(new ActionListener() {
        	@Override
            public void actionPerformed(ActionEvent e) { newParam(); }
        });
        tmp.add(button);
        button=new JButton("Add Value");
        button.addActionListener(new ActionListener() {
        	@Override
            public void actionPerformed(ActionEvent e) { newValue(); }
        });
        tmp.add(button);
        button=new JButton("Remove Element");
        button.addActionListener(new ActionListener() {
        	@Override
            public void actionPerformed(ActionEvent e) { removeElement(); }
        });
        tmp.add(button);
        button=new JButton("Remove Column");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { removeColumn(); }
        });
        tmp.add(button);
        northPanel.add(tmp);
        
        tmp=new JPanel(new BorderLayout());
        tmp.add(new JLabel("Column Name"));
        final JTextField field=new JTextField();
        ActionListener al=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int col=editTable.getSelectedColumn();
                if(col<0) return;
                if(col<paramNames.size()) {
                    paramNames.set(col,field.getText());
                } else {
                    valueNames.set(col-paramNames.size(),field.getText());
                }
                model.fireTableStructureChanged();
            }
        };
        field.addActionListener(al);
        tmp.add(field,BorderLayout.CENTER);
        button=new JButton("Set Name");
        button.addActionListener(al);
        tmp.add(button,BorderLayout.EAST);
        northPanel.add(tmp);
        editPanel.add(northPanel, BorderLayout.NORTH);
        
        button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
        	@Override
            public void actionPerformed(ActionEvent e) {
                for(DataSink ds:sinkVector) {
                    GraphPanel.instance().newWorkOrder(ds);
                }
            }
        } );
        editPanel.add(button,BorderLayout.SOUTH);
        
        model=new EditorTableModel();
        editTable=new JTable(model);
        editTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        editTable.setRowSelectionAllowed(false);
        editTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int col=editTable.getSelectedColumn();
                if(col>=0) {
                    if(col<paramNames.size()) {
                        field.setText(paramNames.get(col));
                    } else {
                        field.setText(valueNames.get(col-paramNames.size()));
                    }
                }
            }
        });
        editPanel.add(new JScrollPane(editTable),BorderLayout.CENTER);
        
        propPanel.add("Editor",editPanel);
    }
    
    private void newParam() {
    	paramNames.add("Param");
        int[] params=new int[paramNames.size()];
        for(int i=0; i<dataVect.get(0).size(); ++i) {
            DataElement de=dataVect.get(0).get(i);
            for(int j=0; j<de.getNumParams(); ++j) {
                params[j]=de.getParam(j);
            }
            dataVect.get(0).set(i,DataElement.replaceParams(de,params));
        }
        model.fireTableStructureChanged();
    }
    
    private void newValue() {
        valueNames.add("Value");
        float[] vals=new float[valueNames.size()];
        for(int i=0; i<dataVect.get(0).size(); ++i) {
            DataElement de=dataVect.get(0).get(i);
            for(int j=0; j<de.getNumValues(); ++j) {
                vals[j]=de.getValue(j);
            }
            dataVect.get(0).set(i,DataElement.replaceValues(de,vals));
        }
        model.fireTableStructureChanged();
    }
    
    private void removeElement() {
        if(editTable==null || editTable.getSelectedRow()<0) return;
    	dataVect.get(0).remove(editTable.getSelectedRow());
        model.fireTableRowsDeleted(editTable.getSelectedRow(),editTable.getSelectedRow());
    }

    private void removeColumn() {
        if(editTable==null) return;
        int col=editTable.getSelectedColumn();
        if(col<0) return;
        if(col<paramNames.size()) {
            paramNames.remove(col);
            int[] params=new int[paramNames.size()];
            for(int i=0; i<dataVect.get(0).size(); ++i) {
                DataElement de=dataVect.get(0).get(i);
                for(int j=0; j<params.length; ++j) {
                    if(j<col) params[j]=de.getParam(j);
                    else if(j>=col) params[j-1]=de.getParam(j+1);
                }
                dataVect.get(0).set(i,DataElement.replaceParams(de,params));
            }
        } else {
            col-=paramNames.size();
            valueNames.remove(col);
            float[] vals=new float[valueNames.size()];
            for(int i=0; i<dataVect.get(0).size(); ++i) {
                DataElement de=dataVect.get(0).get(i);
                for(int j=0; j<vals.length; ++j) {
                    if(j<col) vals[j]=de.getValue(j);
                    else if(j>=col) vals[j-1]=de.getValue(j+1);
                }
                dataVect.get(0).set(i,DataElement.replaceValues(de,vals));
            }
        }
        model.fireTableStructureChanged();
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        return paramNames.get(which);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return valueNames.get(which);
    }

    @Override
    public String getDescription() {
        return "Element Table Editor";
    }

    @Override
    public int getNumParameters(int stream) {
        if(input==null) return 0;
        return input.getNumParameters(0);
    }

    @Override
    public int getNumValues(int stream) {
        if(input==null) return 0;
        return input.getNumValues(0);
    }
    
    public static String getTypeDescription(){ return "Element Table Editor"; }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new ElementTableEditor(this,l);
    }

    private List<String> paramNames=new ArrayList<String>();
    private List<String> valueNames=new ArrayList<String>();
    private transient JTable editTable;
    private transient EditorTableModel model;

    private static final long serialVersionUID=76257239807203l;

    private class EditorTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return dataVect.get(0).size()+1;
        }

        @Override
        public int getColumnCount() {
            return paramNames.size()+valueNames.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(columnIndex<paramNames.size()) {
                if(rowIndex>=dataVect.get(0).size()) return "";
                return Integer.valueOf(dataVect.get(0).get(rowIndex).getParam(columnIndex));
            } else {
                if(rowIndex>=dataVect.get(0).size()) return "";
                return new Double(dataVect.get(0).get(rowIndex).getValue(columnIndex-paramNames.size()));
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex,int columnIndex) {
            return true;
        }
        
        @Override
        public void setValueAt(Object o,int rowIndex,int columnIndex) {
            if(rowIndex<dataVect.get(0).size()) {
                DataElement de=dataVect.get(0).get(rowIndex);
                if(columnIndex<de.getNumParams()) {
                    int[] params=new int[de.getNumParams()];
                    for(int i=0; i<params.length; ++i) params[i]=de.getParam(i);
                    try {
                        params[columnIndex]=(int)(Float.parseFloat(o.toString()));
                        dataVect.get(0).set(rowIndex,DataElement.replaceParams(de,params));
                    } catch(NumberFormatException e) {}
                } else {
                    int index=columnIndex-de.getNumParams();
                    float[] vals=new float[de.getNumValues()];
                    for(int i=0; i<vals.length; ++i) vals[i]=de.getValue(i);
                    try {
                        vals[index]=Float.parseFloat(o.toString());
                        dataVect.get(0).set(rowIndex,DataElement.replaceValues(de,vals));
                    } catch(NumberFormatException e) {}
                }
                fireTableCellUpdated(rowIndex,columnIndex);
            } else {
                int[] params=new int[paramNames.size()];
                float[] vals=new float[valueNames.size()];
                if(columnIndex<params.length) {
                    try {
                        params[columnIndex]=(int)(Float.parseFloat(o.toString()));
                        dataVect.get(0).add(new DataElement(params,vals));
                    } catch(NumberFormatException e) {}
                } else {
                    int index=columnIndex-params.length;
                    try {
                        vals[index]=Float.parseFloat(o.toString());
                        dataVect.get(0).add(new DataElement(params,vals));
                    } catch(NumberFormatException e) {}
                }
                fireTableRowsInserted(dataVect.get(0).size()-1,dataVect.get(0).size()-1);
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            if(columnIndex<paramNames.size()) {
                return "p["+columnIndex+"]="+paramNames.get(columnIndex);
            } else {
                return "v["+(columnIndex-paramNames.size())+"]="+valueNames.get(columnIndex-paramNames.size());
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        private static final long serialVersionUID = -2286696501535183231L;
    }
}
