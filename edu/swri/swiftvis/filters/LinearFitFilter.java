/*
 * Created on Jul 3, 2006
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.GraphElement;

/**
 * This filter will do linear fits of the incoming data.  The user can type in the
 * formulas for the various terms and this filter will find the set of coefficients
 * that make a best fit for that input data in those terms.
 * 
 * Unlike most other filters this one filter will actually create two filters that
 * work together.  The primary filter will output the coefficients for the fit while
 * the secondary filter will output data points for a fit to the function.  The user
 * can specify the range and number of intervals to do the fit over. 
 * 
 * @author Mark Lewis
 */
public class LinearFitFilter extends AbstractSingleSourceFilter {
    public static void main(String[] args) {
        double[][] a={{2,0,2,0.6},
                {3,3,4,-2},
                {5,5,4,2},
                {-1,-2,3.4,-1}};
        int[] p=LUPDecompose(a);
        for(int i=0; i<a.length; ++i) {
            for(int j=0; j<a[i].length; ++j) {
                System.out.print(a[i][j]+" ");
            }
            System.out.println(" | "+p[i]);
        }
        System.out.println();
        double[][] a2={{1,2,0},{3,4,4},{5,6,3}};
        double[] b={3,7,8};
        int[] p2=LUPDecompose(a2);
        for(int i=0; i<a2.length; ++i) {
            for(int j=0; j<a2[i].length; ++j) {
                System.out.print(a2[i][j]+" ");
            }
            System.out.println(" | "+p2[i]);
        }
        double[] x=LUPSolve(a2,p2,b);
        for(int i=0; i<x.length; ++i) System.out.print(x[i]+" ");
        System.out.println();
    }

    public LinearFitFilter() {
    }

    public LinearFitFilter(LinearFitFilter c,List<GraphElement> l) {
        super(c,l);
        fitFormula=new DataFormula(c.fitFormula);
        terms=new ArrayList<DataFormula>(c.terms);
        coefs=new float[c.coefs.length];
        for(int i=0; i<coefs.length; ++i) coefs[i]=c.coefs[i];
    }

    @Override
    protected boolean doingInThreads() {
        return false;
    }

    @Override
    protected void redoAllElements() {
        sizeDataVectToInputStreams();
        for(int s=0; s<input.getNumStreams(); ++s) {
            double[][] a=new double[terms.size()][terms.size()];
            double[] b=new double[terms.size()];
            int[] range=fitFormula.getSafeElementRange(this,s);
            for(DataFormula t:terms) {
                int[] tmp=t.getSafeElementRange(this,s);
                if(tmp[0]>range[0]) range[0]=tmp[0];
                if(tmp[1]<range[1]) range[1]=tmp[1]; 
            }
            DataFormula.checkRangeSafety(range,this);
            for(int i=range[0]; i<range[1]; ++i) {
                double[] ti=new double[terms.size()];
                for(int j=0; j<ti.length; ++j) ti[j]=terms.get(j).valueOf(this,s,i);
                double fit=fitFormula.valueOf(this,s,i);
                for(int j=0; j<a.length; ++j) {
                    for(int k=0; k<a[j].length; ++k) {
                        a[j][k]+=ti[j]*ti[k];
                    }
                    b[j]+=ti[j]*fit;
                }
            }

            int[] p=LUPDecompose(a);
            double[] x=LUPSolve(a,p,b);
            for(int i=0; i<x.length; ++i) {
                coefs[i]=(float)x[i];
            }

            dataVect.get(2*s).add(new DataElement(new int[0],coefs));
            int[] params=new int[input.getNumParameters(s)];
            float[] values=new float[input.getNumValues(s)+2];
            for(int i=range[0]; i<range[1]; ++i) {
                DataElement de=input.getElement(i,s);
                for(int j=0; j<params.length; ++j) params[j]=de.getParam(j);
                for(int j=0; j<values.length-2; ++j) values[j]=de.getValue(j);
                values[values.length-2]=(float)fitFormula.valueOf(this,s,i);
                values[values.length-1]=0;
                for(int j=0; j<terms.size(); ++j) {
                    values[values.length-1]+=coefs[j]*terms.get(j).valueOf(this,s,i);
                }
                dataVect.get(2*s+1).add(new DataElement(params,values));
            }
        }
    }

    public static int[] LUPDecompose(double[][] a) {
        int n=a.length;
        int[] pi=new int[n];
        for(int i=0; i<n; ++i) {
            pi[i]=i;
        }
        for(int k=0; k<n; ++k) {
            double p=0;
            int kp=-1;
            for(int i=k; i<n; ++i) {
                if(Math.abs(a[i][k])>p) {
                    p=Math.abs(a[i][k]);
                    kp=i;
                }
            }
            if(p==0) throw new IllegalArgumentException("Singular matrix for linear fit.");
            int tmp=pi[kp];
            pi[kp]=pi[k];
            pi[k]=tmp;
            double[] tmp2=a[kp];
            a[kp]=a[k];
            a[k]=tmp2;
            for(int i=k+1; i<n; ++i) {
                a[i][k]/=a[k][k];
                for(int j=k+1; j<n; ++j) {
                    a[i][j]-=a[i][k]*a[k][j];
                }
            }
        }
        return pi;
    }

    public static double[] LUPSolve(double[][] lu,int[] p,double[] b) {
        int n=lu.length;
        double[] x=new double[n];
        double[] y=new double[n];
        for(int i=0; i<n; ++i) {
            y[i]=b[p[i]];
            for(int j=0; j<i; ++j) {
                y[i]-=lu[i][j]*y[j];
            }
        }
        for(int i=n-1; i>=0; --i) {
            x[i]=y[i];
            for(int j=i+1; j<n; ++j) x[i]-=lu[i][j]*x[j];
            x[i]/=lu[i][i];
        }
        return x;
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel outerPanel=new JPanel(new BorderLayout());
        Box box=new Box(BoxLayout.Y_AXIS);
        JPanel panel=new JPanel(new BorderLayout());
        panel.add(new JLabel("Fit Formula"),BorderLayout.WEST);
        panel.add(fitFormula.getTextField(null),BorderLayout.CENTER);
        box.add(panel);
        termList=new JList();
        termList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                lastSelected=termList.getSelectedIndex();
                if(lastSelected>=0) formulaField.setText(terms.get(lastSelected).getFormula());
            }
        });
        box.add(new JScrollPane(termList));
        panel=new JPanel(new GridLayout(1,2));
        JButton button=new JButton("Add Term");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTerm();
            }
        });
        panel.add(button);
        button=new JButton("Remove Term");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeTerm();
            }
        });
        panel.add(button);
        box.add(panel);
        panel=new JPanel(new BorderLayout());
        panel.add(new JLabel("Term Formula"),BorderLayout.WEST);
        formulaField=new JTextField();
        formulaField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formulaChanged();
            }
        });
        formulaField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                formulaChanged();
            }
        });
        panel.add(formulaField,BorderLayout.CENTER);
        box.add(panel);
        box.add(Box.createVerticalGlue());
        outerPanel.add(box,BorderLayout.NORTH);

        button=new JButton("Do Fit");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        outerPanel.add(button,BorderLayout.SOUTH);

        propPanel.add("Fit Terms",outerPanel);
    }

    @Override
    public int getNumParameters(int stream) {
        if(stream%2==0) return 0;
        return getSource(0).getNumParameters(stream/2);
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(stream%2==0) return "";
        return getSource(0).getParameterDescription(stream/2,which);
    }

    @Override
    public int getNumValues(int stream) {
        if(stream%2==0) return coefs.length;
        return getSource(0).getNumValues(stream/2)+2;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(stream%2==0) return Character.toString((char)('A'+which));
        if(which<getSource(0).getNumValues(stream/2)) return getSource(0).getValueDescription(stream/2,which);
        which-=getSource(0).getNumValues(stream/2);
        String[] desc={"Fit To","Fit Value"};
        return desc[which];
    }

    @Override
    public String getDescription() {
        return "Linear Fit Filter";
    }

    public static String getTypeDescription() {
        return "Linear Fit Filter";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new LinearFitFilter(this,l);
    }

    @Override
    protected void sizeDataVectToInputStreams() {
        if(dataVect.size()>2*input.getNumStreams()) dataVect.clear();
        while(dataVect.size()<2*input.getNumStreams()) dataVect.add(new ArrayList<DataElement>());
    }

    private void addTerm() {
        terms.add(new DataFormula("v[0]"));
        float[] nc=new float[terms.size()];
        for(int i=0; i<coefs.length; ++i) {
            nc[i]=coefs[i];
        }
        coefs=nc;
        termList.setListData(terms.toArray());
        termList.setSelectedIndex(terms.size()-1);
    }

    private void removeTerm() {
        int selected=termList.getSelectedIndex();
        if(selected<0) {
            JOptionPane.showMessageDialog(propPanel,"You must select a term to remove.");
            return;
        }
        terms.remove(selected);
        float[] tmp=new float[coefs.length-1];
        for(int i=0; i<tmp.length; ++i) {
            if(i<selected) tmp[i]=coefs[i];
            else tmp[i]=coefs[i+1];
        }
        coefs=tmp;
        lastSelected=-1;
        termList.setListData(terms.toArray());
        termList.setSelectedIndex(-1);
    }

    private void formulaChanged() {
        if(lastSelected>=0) {
            terms.set(lastSelected,new DataFormula(formulaField.getText()));
            termList.setListData(terms.toArray());
            termList.setSelectedIndex(lastSelected);
        }        
    }

    private DataFormula fitFormula=new DataFormula("v[0]");
    private ArrayList<DataFormula> terms=new ArrayList<DataFormula>();
    private float[] coefs=new float[0];

    private transient JList termList;
    private transient int lastSelected;
    private transient JTextField formulaField;

    private static final long serialVersionUID = 7580335019846891281L;

}
