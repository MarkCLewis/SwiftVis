/*
 * Created on Jul 9, 2006
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;

/**
 * This class takes binned data and rebins it based on some count.  The objective is to get
 * roughly the same "count" in each bin.  This filter is being created to work with fixed
 * binned data when particle binned data is really more what is needed.  The idea is that
 * each bin in the fixed binned data has a count of how many particles were in it.  The user
 * can tell this filter how many particles he/she would like to have in each bin as an
 * output and the incomming bins will be bunched together to try to accomplish this.  The
 * values will be combined with a weighted average and they will respect normal group bounds.
 * 
 * The splitCounts boolean tells the filter whether it should effectively split counts across
 * two bins.  So if it gets to an input bin with a count of 10 and only 2 more are needed to reach the
 * desired count limit, if this is true, it will put the value in the first output bin with a weight of 2
 * and the second output bin with a weight of 8.  Otherwise it will put all 10 in the first output bin
 * and start counting again for the next one.
 * 
 * @author Mark Lewis
 */
public class CountRebinFilter extends AbstractSingleSourceFilter {
    public CountRebinFilter() {}
    
    public CountRebinFilter(CountRebinFilter c,List<GraphElement> l) {
        super(c,l);
        groupFormula=new DataFormula(c.groupFormula);
        countFormula=new DataFormula(c.countFormula);
        countInBin=new EditableDouble(c.countInBin.getValue());
        splitCounts=new EditableBoolean(c.splitCounts.getValue());
        paramMethods=new int[c.paramMethods.length];
        for(int i=0; i<paramMethods.length; ++i) {
            paramMethods[i]=c.paramMethods[i];
        }
        valueMethods=new int[c.valueMethods.length];
        for(int i=0; i<valueMethods.length; ++i) {
            valueMethods[i]=c.valueMethods[i];
        }
    }

    @Override
    protected boolean doingInThreads() {
    	return false;
    }
    
    @Override
    protected void redoAllElements() {
        synchGUI();
        try {
            int[] range=groupFormula.getSafeElementRange(this, 0);
            int[] tmp=countFormula.getSafeElementRange(this, 0);
            if(tmp[0]>range[0]) tmp[0]=range[0];
            if(tmp[1]<range[1]) tmp[1]=range[1];
            DataFormula.checkRangeSafety(range,this);
            double cnt=0;
            int[] params=new int[input.getNumParameters(0)+1];
            float[] values=new float[input.getNumValues(0)];
            double[][] pcombines=new double[params.length-1][];
            double[][] vcombines=new double[values.length][];
            for(int k=0; k<params.length-1; ++k) {
                pcombines[k]=combineMethods[paramMethods[k]].init();
            }
            for(int k=0; k<values.length; ++k) {
                vcombines[k]=combineMethods[valueMethods[k]].init();
            }
            for(int i=range[0]; i<range[1];) {
                double groupVal=groupFormula.valueOf(this,0, i);
                while(i<range[1] && groupFormula.valueOf(this,0, i)==groupVal) {
                    double thisCount=countFormula.valueOf(this,0, i);
                    DataElement de=input.getElement(i, 0);
                    if(cnt+thisCount>=countInBin.getValue()) {
                        double fullCount=thisCount;
                        if(splitCounts.getValue() && cnt+fullCount>countInBin.getValue()) {
                            thisCount=countInBin.getValue()-cnt;
                        }
                        cnt+=thisCount;
                        params[params.length-1]=(int)cnt;
                        for(int k=0; k<params.length-1; ++k) {
                            combineMethods[paramMethods[k]].addValue(pcombines[k],thisCount,de.getParam(k));
                            params[k]=(int)combineMethods[paramMethods[k]].getValue(pcombines[k]);
                        }
                        for(int k=0; k<values.length; ++k) {
                            combineMethods[valueMethods[k]].addValue(vcombines[k],thisCount,de.getValue(k));
                            values[k]=(float)combineMethods[valueMethods[k]].getValue(vcombines[k]);
                        }
                        dataVect.get(0).add(new DataElement(params,values));
                        for(int k=0; k<params.length; ++k) {
                            params[k]=0;
                            if(k<pcombines.length) pcombines[k]=combineMethods[paramMethods[k]].init();
                        }
                        for(int k=0; k<values.length; ++k) {
                            values[k]=0;
                            vcombines[k]=combineMethods[valueMethods[k]].init();
                        }
                        if(splitCounts.getValue() && thisCount<fullCount) {
                            cnt=fullCount-thisCount;
                            for(int k=0; k<params.length-1; ++k) {
                                combineMethods[paramMethods[k]].addValue(pcombines[k],cnt,de.getParam(k));
                            }
                            params[params.length-1]+=cnt;
                            for(int k=0; k<values.length; ++k) {
                                combineMethods[valueMethods[k]].addValue(vcombines[k],cnt,de.getValue(k));
                            }
                        } else {
                            cnt=0;
                        }
                    } else {
                        for(int k=0; k<params.length-1; ++k) {
                            combineMethods[paramMethods[k]].addValue(pcombines[k],thisCount,de.getParam(k));
                        }
                        params[params.length-1]+=thisCount;
                        for(int k=0; k<values.length; ++k) {
                            combineMethods[valueMethods[k]].addValue(vcombines[k],thisCount,de.getValue(k));
                        }
                        cnt+=thisCount;
                    }
                    ++i;
                }
                if(cnt>0) {
                    params[params.length-1]=(int)cnt;
                    for(int k=0; k<params.length-1; ++k) {
                        params[k]=(int)combineMethods[paramMethods[k]].getValue(pcombines[k]);
                    }
                    for(int k=0; k<values.length; ++k) {
                        values[k]=(float)combineMethods[valueMethods[k]].getValue(vcombines[k]);
                    }
                    dataVect.get(0).add(new DataElement(params,values));
                    for(int k=0; k<params.length; ++k) {
                        params[k]=0;
                        if(k<pcombines.length) pcombines[k]=combineMethods[paramMethods[k]].init();
                    }
                    for(int k=0; k<values.length; ++k) {
                        values[k]=0;
                        vcombines[k]=combineMethods[valueMethods[k]].init();
                    }
                    cnt=0;
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            // This just means the formulas are off.  The user will need to correct this.
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel innerPanel=new JPanel(new GridLayout(4,1));
        JPanel p=new JPanel(new BorderLayout());
        p.add(new JLabel("Group Formula"),BorderLayout.WEST);
        p.add(groupFormula.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(p);
        p=new JPanel(new BorderLayout());
        p.add(new JLabel("Count Formula"),BorderLayout.WEST);
        p.add(countFormula.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(p);
        p=new JPanel(new BorderLayout());
        p.add(new JLabel("Number in each bin"),BorderLayout.WEST);
        p.add(countInBin.getTextField(null),BorderLayout.CENTER);
        innerPanel.add(p);
        innerPanel.add(splitCounts.getCheckBox("Split input bins across output bins?",null));
        panel.add(innerPanel,BorderLayout.NORTH);
        
        innerPanel=new JPanel(new BorderLayout());
        pvList=new JList();
        pvList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                listSelectionMade();
            }
        });
        innerPanel.add(pvList,BorderLayout.CENTER);
        methodSelect=new JComboBox(combineMethods);
        methodSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                combineMethodChosen();
            }
        });
        innerPanel.add(methodSelect,BorderLayout.SOUTH);
        panel.add(innerPanel,BorderLayout.CENTER);
        synchGUI();
        
        JButton button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        panel.add(button,BorderLayout.SOUTH);
        propPanel.add("Settings",panel);
    }

    @Override
    public int getNumParameters(int stream) {
        return input.getNumParameters(0)+1;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(which<input.getNumParameters(0)) return input.getParameterDescription(0, which);
        return "Total Count";
    }

    @Override
    public int getNumValues(int stream) {
        return input.getNumValues(0);
    }

    @Override
    public String getValueDescription(int stream, int which) {
        return input.getValueDescription(0, which);
    }

    @Override
    public String getDescription() {
        return "Count Rebinning Filter";
    }

    public static String getTypeDescription() {
        return "Count Rebinning Filter";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new CountRebinFilter(this,l);
    }
    
    @Override
    public String toString() {
        return getDescription();
    }
    
    private void synchGUI() {
        if(input==null) {
            if(pvList!=null) pvList.setListData(new Object[0]);
        } else {
            int selected=(pvList!=null)?pvList.getSelectedIndex():-1;
            int p=input.getNumParameters(0);
            int v=input.getNumValues(0);
            int[] tmp=new int[p];
            String[] listData=new String[p+v];
            for(int i=0; i<p; ++i) {
                listData[i]="p["+i+"]";
                if(i<paramMethods.length) tmp[i]=paramMethods[i];
                else tmp[i]=0;
            }
            paramMethods=tmp;
            tmp=new int[v];
            for(int i=0; i<v; ++i) {
                listData[i+p]="v["+i+"]";
                if(i<valueMethods.length) tmp[i]=valueMethods[i];
                else tmp[i]=1;
            }
            valueMethods=tmp;
            if(pvList!=null) pvList.setListData(listData);
            if(selected>=0) pvList.setSelectedIndex(selected);
        }
    }
    
    private void listSelectionMade() {
        int selected=pvList.getSelectedIndex();
        if(selected<0) return;
        int s;
        if(selected<paramMethods.length) s=paramMethods[selected];
        else s=valueMethods[selected-paramMethods.length];
        methodSelect.setSelectedIndex(s);
    }
    
    private void combineMethodChosen() {
        int selected=pvList.getSelectedIndex();
        if(selected<0) return;
        if(selected<paramMethods.length) paramMethods[selected]=methodSelect.getSelectedIndex();
        else valueMethods[selected-paramMethods.length]=methodSelect.getSelectedIndex();
    }
    
    private DataFormula groupFormula=new DataFormula("v[0]");
    private DataFormula countFormula=new DataFormula("p[0]");
    private EditableDouble countInBin=new EditableDouble(100);
    private EditableBoolean splitCounts=new EditableBoolean(false);
    private int[] paramMethods=new int[0];
    private int[] valueMethods=new int[0];
    
    private transient JList pvList;
    private transient JComboBox methodSelect;

    private static final long serialVersionUID = 151635862939731548L;
    
    private static final CombineMethod[] combineMethods={new SumMethod(),new AverageMethod(),
        new AngleAverageMethod()};
    
    private static interface CombineMethod {
        double[] init();
        void addValue(double[] cur,double count,double v);
        double getValue(double[] cur);
    }
    
    private static class SumMethod implements CombineMethod {
        @Override
        public double[] init() {
            return new double[1];
        }
        @Override
        public void addValue(double[] cur,double count, double v) {
            cur[0]+=v*count;
        }
        @Override
        public double getValue(double[] cur) {
            return cur[0];
        }
        @Override
        public String toString() { return "Sum"; }
    }
    private static class AverageMethod implements CombineMethod {
        @Override
        public double[] init() {
            return new double[2];
        }
        @Override
        public void addValue(double[] cur,double count, double v) {
            cur[0]+=v*count;
            cur[1]+=count;
        }
        @Override
        public double getValue(double[] cur) {
            return cur[0]/cur[1];
        }
        @Override
        public String toString() { return "Average"; }        
    }
    private static class AngleAverageMethod implements CombineMethod {
        @Override
        public double[] init() {
            return new double[2];
        }
        @Override
        public void addValue(double[] cur,double count, double v) {
            if(cur[1]!=0) {
                double curVal=cur[0]/cur[1];
                while(v<curVal-Math.PI) v+=2*Math.PI;
                while(v>curVal+Math.PI) v-=2*Math.PI;
            }
            cur[0]+=v*count;
            cur[1]+=count;
        }
        @Override
        public double getValue(double[] cur) {
            return cur[0]/cur[1];
        }
        @Override
        public String toString() { return "Angle Average"; }        
    }
}
