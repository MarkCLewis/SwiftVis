/*
 * Created on Jul 8, 2008
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.Matrix;
import edu.swri.swiftvis.util.MatrixPack;

/**
 * This filter does non-linear fits to data.  It can do linear fits as well, but the
 * linear fit filter will be more efficient for that.  There are some built in fit
 * functions or you can select a general fit where you type in an arbitrary function
 * of x and provide values for the initial guesses.
 * 
 * @author Mark Lewis
 */
public class NonlinearFitFilter extends AbstractSingleSourceFilter {
    public NonlinearFitFilter() {}

    public NonlinearFitFilter(NonlinearFitFilter c,List<GraphElement> l) {
        super(c,l);
        fitType=c.fitType;
        xFormula=new DataFormula(c.xFormula.getFormula());
        yFormula=new DataFormula(c.yFormula.getFormula());
    }

    @Override
    protected boolean doingInThreads() {
        return false;
    }

    @Override
    protected void redoAllElements() {
        sizeDataVectToInputStreams();
        doFit();
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(4,1));
        final JPanel centerPanel=new JPanel(new GridLayout(1,1));
        northPanel.add(xFormula.getLabeledTextField("x Formula", null));
        northPanel.add(yFormula.getLabeledTextField("y Formula", null));
        if(maxIters==null) maxIters=new EditableInt(10000);
        northPanel.add(maxIters.getLabeledTextField("Max Iterations",null));
        final JComboBox dropBox=new JComboBox(functions);
        dropBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fitType=dropBox.getSelectedIndex();
                JComponent comp=functions[fitType].getPropertiesPanel();
                if(comp!=null) {
                    centerPanel.removeAll();
                    centerPanel.add(comp);
                    centerPanel.validate();
                    centerPanel.repaint();
                }
            }
        });
        dropBox.setSelectedIndex(fitType);
        northPanel.add(dropBox);
        panel.add(northPanel,BorderLayout.NORTH);
        panel.add(centerPanel,BorderLayout.CENTER);
        JPanel southPanel=new JPanel(new GridLayout(3,1));
        fitLabel=new JLabel("No Fit");
        southPanel.add(fitLabel);
        residualLabel=new JLabel("No Fit");
        southPanel.add(residualLabel);
        JButton button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedoAllElements();
            }
        });
        southPanel.add(button);
        panel.add(southPanel,BorderLayout.SOUTH);
        propPanel.addTab("Settings",panel);
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
        if(stream%2==0) return 0; // TODO: make this work coefs.length;
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
    public GraphElement copy(List<GraphElement> l) {
        return new NonlinearFitFilter(this,l);
    }

    @Override
    public String getDescription() {
        return "Nonlinear Fit Filter";
    }

    public static String getTypeDescription() {
        return "Nonlinear Fit Filter";
    }

    @Override
    protected void sizeDataVectToInputStreams() {
        if(dataVect.size()>2*input.getNumStreams()) dataVect.clear();
        while(dataVect.size()<2*input.getNumStreams()) dataVect.add(new ArrayList<DataElement>());
    }

    private void doFit() {
        for(int s=0; s<getSource(0).getNumStreams(); ++s) {
            Function ef=functions[fitType];

            if(maxIters==null) maxIters=new EditableInt(10000);

            NonlinearFitData nfd=nonLinearFit(xFormula,yFormula,this,s,ef,maxIters.getValue());

            if(fitLabel!=null) {
                fitLabel.setText(ef.getFitDescription(nfd.params));
                residualLabel.setText("Residual="+nfd.getRValue());
            }
            for(int i=nfd.range[0]; i<nfd.range[1]; ++i) {
                dataVect.get(0).add(input.getElement(i, 0).addValue((float)ef.eval(xFormula.valueOf(this,s,i),nfd.params,this,s)));
            }
        }
    }

    public static NonlinearFitData nonLinearFit(DataFormula xForm,DataFormula yForm,DataSink sink,int stream,Function ef,int maxIterations) {
        Matrix step,J;
        MatrixPack mp=new MatrixPack();
        int state=0;
        int cnt=0;
        NonlinearFitData ret=new NonlinearFitData();
        ret.params=ef.createMatrix();

        xForm.clearGroupSelection();
        yForm.clearGroupSelection();
        ret.range=xForm.getSafeElementRange(sink,stream);
        DataFormula.mergeSafeElementRanges(ret.range,yForm.getSafeElementRange(sink,stream));
        DataFormula.checkRangeSafety(ret.range,sink);
        Matrix x=new Matrix(ret.range[1]-ret.range[0],1);
        Matrix y=new Matrix(ret.range[1]-ret.range[0],1);
        for(int i=ret.range[0]; i<ret.range[1]; ++i) {
            x.set(i-ret.range[0],0,xForm.valueOf(sink,stream,i));
            y.set(i-ret.range[0],0,yForm.valueOf(sink,stream,i));
        }
        ef.initialGuess(ret.params,ret.range[0],ret.range[1]-1,xForm,yForm,sink,stream);

        while((state==0) && (cnt<maxIterations)) {
            //          exp_params.Print("exp_params");
            J=jacobian(x,y,ret.params,ef,sink,stream);
            //          J.Print("J");
            ret.R=residual(x,y,ret.params,ef,sink,stream);
            //          R.Print("R");
            step=mp.GaussNewtonStep(ret.params,J,ret.R);
            Matrix R_sqr=ret.R.transpose().multiply(ret.R);
            double val=R_sqr.get(0,0);
            R_sqr.print("Residual squared");

            Matrix test_params=ret.params.add(step);
            ret.R=residual(x,y,test_params,ef,sink,stream);
            R_sqr=ret.R.transpose().multiply(ret.R);
            for(int lcnt=0; (R_sqr.get(0,0)>=val) && (lcnt<5); lcnt++) {
                step=step.scale(0.5);
                //              step.Print("Step "+lcnt);
                test_params=ret.params.add(step);
                ret.R=residual(x,y,test_params,ef,sink,stream);
                R_sqr=ret.R.transpose().multiply(ret.R);
                //              R_sqr.Print("Residual squared "+lcnt);
            }
            System.out.println("Step "+cnt+":"+step.get(0,0)+" "+step.get(1,0));
            state=1;
            for(int i=0; i<ret.params.rows(); i++) {
                if(Math.abs(step.get(i,0)/ret.params.get(i,0))>1e-6) state=0;
            }
            ret.params=test_params;
            cnt++;
        }
        return ret;
    }

    private static Matrix jacobian(Matrix x,Matrix y,Matrix params,Function f,DataSink sink,int stream) {
        Matrix ret=new Matrix(y.rows(),params.rows());
        Matrix p2=new Matrix(params);
        Matrix res=residual(x,y,params,f,sink,stream);
        Matrix res2;
        int i,j;
        double diff;

        if(machEPS==0.0) {
            machEPS=1.0;
            while(1.0+machEPS>1.0) machEPS/=2.0;
            System.out.println("mach_eps="+machEPS);
            machEPS=Math.sqrt(machEPS);
        }
        for(j=0; j<params.rows(); j++) {
            p2.set(j,0,params.get(j,0)*(1.0+machEPS));
            diff=p2.get(j,0)-params.get(j,0);
            res2=residual(x,y,p2,f,sink,stream);
            for(i=0; i<y.rows(); i++) {
                ret.set(i,j,(res2.get(i,0)-res.get(i,0))/diff);
            }
            p2.set(j,0,params.get(j,0));
        }

        return(ret);
    }

    private static Matrix residual(Matrix x,Matrix y,Matrix params,Function f,DataSink sink,int stream) {
        Matrix ret=new Matrix(y);
        int i;

        for(i=0; i<ret.rows(); i++)
            ret.set(i,0,y.get(i,0)-f.eval(x.get(i,0),params,sink,stream));
        return(ret);
    }

    private int fitType;
    private DataFormula xFormula=new DataFormula("v[0]");
    private DataFormula yFormula=new DataFormula("v[1]");
    private EditableInt maxIters=new EditableInt(10000);

    private transient JLabel fitLabel;
    private transient JLabel residualLabel;

    private static final long serialVersionUID = -4623247459999355027L;
    private static double machEPS=0.0;

    private static Function[] functions={new GeneralFit(),new ExpFit(),new InverseFit(),new ExpInverseFit()};

    public static class NonlinearFitData {
        public Matrix getParams() { return params; }
        public Matrix getR() { return R; }
        public double getRValue() { return R.get(0,0); }
        public int[] getRange() { return range; }

        private Matrix params;
        private Matrix R;
        private int[] range;
    }

    public static interface Function extends Serializable {
        public double eval(double x,Matrix p,DataSink sink,int stream);
        public void initialGuess(Matrix p,int start,int end,DataFormula xFormula,DataFormula yFormula,DataSink sink,int stream);
        public Matrix createMatrix();
        public JComponent getPropertiesPanel();
        public String getFitDescription(Matrix p);
    }

    public static class ExpFit implements Function {
        @Override
        public double eval(double x,Matrix p,DataSink sink,int stream) {
            return(p.get(0,0)*Math.exp(p.get(1,0)*x)+base.getValue());
        }

        @Override
        public void initialGuess(Matrix p,int start,int end,DataFormula xFormula,DataFormula yFormula,DataSink sink,int stream) {
            double xs,xe,ys,ye;
            xs=xFormula.valueOf(sink,stream,start);
            ys=yFormula.valueOf(sink,stream,start);
            xe=xFormula.valueOf(sink,stream,end);
            ye=yFormula.valueOf(sink,stream,end);
            double b=Math.log((ye-base.getValue())/(ys-base.getValue()))/(xe-xs);
            p.set(0,0,(ys-base.getValue())/Math.exp(b*xs));
            p.set(1,0,b);
        }

        @Override
        public Matrix createMatrix() {
            return new Matrix(2,1);
        }

        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                propPanel.add(base.getLabeledTextField("Base Value",null),BorderLayout.NORTH);
            }
            return propPanel;
        }

        @Override
        public String getFitDescription(Matrix p) {
            return "f(x)="+p.get(0,0)+"exp("+p.get(1,0)+"*x)+"+base.getValue();            
        }

        @Override
        public String toString() {
            return "Exponential Fit f(x)=A*e^(Bx)";
        }

        private EditableDouble base=new EditableDouble(0.0);
        private transient JPanel propPanel;
        private static final long serialVersionUID = -3309734958034682621L;
    }

    public static class InverseFit implements Function {
        @Override
        public double eval(double x,Matrix p,DataSink sink,int stream) {
            return(p.get(0,0)/(x-p.get(1,0))+base.getValue());
        }

        @Override
        public void initialGuess(Matrix p,int start,int end,DataFormula xFormula,DataFormula yFormula,DataSink sink,int stream) {
            double xs,xe,ys,ye;
            xs=xFormula.valueOf(sink,stream,start);
            ys=yFormula.valueOf(sink,stream,start)-base.getValue();
            xe=xFormula.valueOf(sink,stream,end);
            ye=yFormula.valueOf(sink,stream,end)-base.getValue();
            double p2=(ys*xs-ye*xe)/(ys-ye);
            p.set(0,0,ys*(xs-p2));
            p.set(1,0,p2);
        }

        @Override
        public Matrix createMatrix() {
            return new Matrix(2,1);
        }

        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                propPanel.add(base.getLabeledTextField("Base Value",null),BorderLayout.NORTH);
            }
            return propPanel;
        }

        @Override
        public String getFitDescription(Matrix p) {
            return "f(x)="+p.get(0,0)+"/(x-"+p.get(1,0)+")+"+base.getValue();
        }

        @Override
        public String toString() {
            return "Inverse Fit f(x)=A/(x-B)";
        }

        private EditableDouble base=new EditableDouble(0.0);
        private transient JPanel propPanel;
        private static final long serialVersionUID = -6292484329011512038L;
    }

    public static class ExpInverseFit implements Function {
        @Override
        public double eval(double x,Matrix p,DataSink sink,int stream) {
            if(x<p.get(3,0)) {
                return(p.get(0,0)/((p.get(3,0)-p.get(1,0))*Math.exp(p.get(2,0)*p.get(3,0)))*Math.exp(p.get(2,0)*x)+base.getValue());
            } else {
                return(p.get(0,0)/(x-p.get(1,0))+base.getValue());
            }
        }

        @Override
        public void initialGuess(Matrix p,int start,int end,DataFormula xFormula,DataFormula yFormula,DataSink sink,int stream) {
            double xs,xe,xc,ys,ye,yc;
            xs=xFormula.valueOf(sink,stream,start);
            ys=yFormula.valueOf(sink,stream,start)-base.getValue();
            xe=xFormula.valueOf(sink,stream,end);
            ye=yFormula.valueOf(sink,stream,end)-base.getValue();
            int c=start;
            double best=xFormula.valueOf(sink,stream,c);
            for(int i=start+1; i<end; ++i) {
                double cur=xFormula.valueOf(sink,stream,i);
                if(Math.abs(cur-breakPoint.getValue())<Math.abs(best-breakPoint.getValue())) {
                    c=i;
                    best=xFormula.valueOf(sink,stream,c);
                }
            }
            xc=xFormula.valueOf(sink,stream,c);
            yc=yFormula.valueOf(sink,stream,c)-base.getValue();
            double p2=(ys*xs-yc*xc)/(ys-yc);
            p.set(0,0,ys*(xs-p2));
            p.set(1,0,p2);
            p.set(2,0,Math.log(ye*(xc-p2)/p.get(0,0))/(xe-xc));
            p.set(3,0,xc);
        }

        @Override
        public Matrix createMatrix() {
            return new Matrix(4,1);
        }

        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(2,1));
                northPanel.add(base.getLabeledTextField("Base Value",null));
                northPanel.add(breakPoint.getLabeledTextField("Break Estimate",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }

        @Override
        public String getFitDescription(Matrix p) {
            double p3=p.get(0,0)/((p.get(3,0)-p.get(1,0))*Math.exp(p.get(3,0)*p.get(2,0)));
            return "f(x>"+p.get(3,0)+")="+p.get(0,0)+"/(x-"+p.get(1,0)+")+"+base.getValue()+" else f(x)="+p3+"exp("+p.get(2,0)+"*x)+"+base.getValue();
        }

        @Override
        public String toString() {
            return "Broken Exponential and Inverse Fit f(x>D)=A/(x-B) else f(x)=#*e^(Cx)";
        }

        private EditableDouble base=new EditableDouble(0.0);
        private EditableDouble breakPoint=new EditableDouble(1.0);
        private transient JPanel propPanel;
        private static final long serialVersionUID = -7644803456191293622L;
    }

    public static class GeneralFit implements Function {
        public GeneralFit() {
            findNumParams();
        }

        @Override
        public Matrix createMatrix() {
            return new Matrix(vars.size(),1);
        }

        @Override
        public double eval(double x, Matrix p,DataSink sink,int stream) {
            if(hash==null) {
                hash=new HashMap<String,Double>();
            }
            hash.put("x",x);
            for(int i=0; i<p.rows(); ++i) {
                hash.put(Character.toString((char)('A'+i)),p.get(i,0));
            }
            return fitFormula.valueOf(sink,stream,0,null,hash);
        }

        @Override
        public String getFitDescription(Matrix p) {
            String ret="";
            for(int i=0; i<p.rows(); ++i) {
                ret+=Character.toString((char)('A'+i))+"="+p.get(i,0)+" ";
            }
            return ret;
        }

        @Override
        public JComponent getPropertiesPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                propPanel.add(fitFormula.getLabeledTextField("Fit Formula",new DataFormula.Listener() {
                    @Override
                    public void formulaChanged() {
                        findNumParams();
                    }
                }),BorderLayout.NORTH);
                JPanel innerPanel=new JPanel(new BorderLayout());
                initPanel=new JPanel(new GridLayout(vars.size(),1));
                for(int i=0; i<vars.size(); ++i) {
                    initPanel.add(initialVals.get(i).getLabeledTextField(vars.get(i),null));
                }
                innerPanel.add(initPanel,BorderLayout.NORTH);
                propPanel.add(innerPanel,BorderLayout.CENTER);
            }
            return propPanel;
        }

        @Override
        public void initialGuess(Matrix p, int start, int end,DataFormula xFormula,DataFormula yFormula,DataSink sink,int stream) {
            for(int i=0; i<vars.size(); ++i) {
                p.set(i,0,initialVals.get(i).getValue());
            }
        }

        @Override
        public String toString() {
            return "General Fit";
        }

        private void findNumParams() {
            vars=new ArrayList<String>(fitFormula.listVariables());
            vars.remove("x");
            for(int i=0; i<vars.size(); ++i) {
                initialVals.add(new EditableDouble(1.0));
            }
            if(initPanel!=null) {
                initPanel.removeAll();
                initPanel.setLayout(new GridLayout(vars.size(),1));
                for(int i=0; i<vars.size(); ++i) {
                    initPanel.add(initialVals.get(i).getLabeledTextField(vars.get(i),null));
                }
                initPanel.validate();
                initPanel.repaint();
            }
        }

        private DataFormula fitFormula=new DataFormula("A+x**B");
        private List<String> vars=new ArrayList<String>();
        private List<EditableDouble> initialVals=new ArrayList<EditableDouble>();
        private transient JPanel propPanel;
        private transient JPanel initPanel;
        private transient HashMap<String,Double> hash;
        private static final long serialVersionUID = 2100061166510689021L;
    }
}
