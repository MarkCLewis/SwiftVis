/* Generated by Together */

package edu.swri.swiftvis.sources;

import javax.swing.table.*;

import edu.swri.swiftvis.DataElement;

public class PreviewTableModel extends AbstractTableModel {
    public PreviewTableModel(java.util.List<DataElement> v) {
        vect=v;
        pars=0;
        vals=0;
        for(DataElement de:vect) {
            if(de.getNumParams()>pars) pars=de.getNumParams();
            if(de.getNumValues()>vals) vals=de.getNumValues();
        }
    }

    @Override
    public int getRowCount() {
        return vect.size();
    }

    @Override
    public int getColumnCount() {
        return pars+vals;
    }

    @Override
    public Object getValueAt(int row,int col) {
        DataElement de=vect.get(row);
        if(col<pars) {
            if(col>=de.getNumParams()) return "";
            else return Integer.valueOf(de.getParam(col));
        } else {
            col-=pars;
            if(col>=de.getNumValues()) return "";
            else return new Double(de.getValue(col));
        }
    }

    @Override
    public boolean isCellEditable(int row,int col) { return false; }

    /**
     * 
     * @uml.property name="vect"
     * @uml.associationEnd multiplicity="(0 -1)" elementType="edu.swri.swiftvis.DataElement"
     */
    private java.util.List<DataElement> vect;

    private int pars;
    private int vals;
    private static final long serialVersionUID=62569873209856l;
}

