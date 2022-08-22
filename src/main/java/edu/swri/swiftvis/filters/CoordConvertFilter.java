package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.LoopBodyWithGroup;
import edu.swri.swiftvis.util.ThreadHandler;

public final class CoordConvertFilter extends AbstractSingleSourceFilter {
    public CoordConvertFilter() {}

    private CoordConvertFilter(CoordConvertFilter c,List<GraphElement> l) {
        super(c,l);
        converter=c.converter.copy();
    }

    @Override
    public String getDescription(){ return "Coordinate Conversion"; }

    public static String getTypeDescription(){ return "Coordinate Conversion"; }

    /**
     * Tells you what a particular parameter is used for.
     */
    @Override
    public String getParameterDescription(int stream, int which){
        return input.getParameterDescription(stream,which);
    }

    /**
     * Tells you what a particular value is used for.
     */
    @Override
    public String getValueDescription(int stream, int which){
        return converter.getValueDescription(which,input);
    }

    @Override
    public int getNumParameters(int stream){
        if(input==null) return 0;
        return input.getNumParameters(stream);
    }

    @Override
    public int getNumValues(int stream){
        if(input==null) return 0;
        return input.getNumValues(stream);
    }

    @Override
    public CoordConvertFilter copy(List<GraphElement> l) {
        return new CoordConvertFilter(this,l);
    }

    @Override
    protected boolean doingInThreads() {
        return true;
    }

    @Override
    protected void redoAllElements() {
        if(converter==null) return;
        sizeDataVectToInputStreams();
        for(int s=0; s<input.getNumStreams(); ++s) {
            final int ss=s;
            try {
                dataVect.get(s).ensureCapacity(input.getNumElements(s));
                for (int i=0; i<input.getNumElements(s); i++) {
                    dataVect.get(s).add(null);
                }
                // parallel code
                converter.setup(input.getElement(0,s));
                ThreadHandler.instance().chunkedForLoop(this,0,input.getNumElements(s), new LoopBodyWithGroup() {
                    @Override
                    public void execute(int i,int g) {
                        dataVect.get(ss).set(i, converter.convert(input.getElement(i,ss),i,g));
                    }
                });
            } catch(ArrayIndexOutOfBoundsException e) {
                JOptionPane.showMessageDialog(propPanel,"There was an exception with a conversion.  Check to make sure your input has the proper values for this conversion.");
            }
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(converters.length,1));
        final JPanel centerPanel=new JPanel(new GridLayout(1,1));
        if(converter!=null) {
            centerPanel.add(converter.getPropPanel());
        }
        panel.add(centerPanel,BorderLayout.CENTER);
        ButtonGroup group=new ButtonGroup();
        for(int i=0; i<converters.length; ++i) {
            JRadioButton radioButton=new JRadioButton(converters[i].toString());
            final int which=i;
            radioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    converter=converters[which].copy();
                    centerPanel.removeAll();
                    centerPanel.add(converter.getPropPanel());
                    centerPanel.validate();
                    centerPanel.repaint();
                }
            });
            group.add(radioButton);
            northPanel.add(radioButton);
            if(converter!=null && converters[i].getClass().isInstance(converter)) {
                radioButton.setSelected(true);
            }
        }
        panel.add(northPanel,BorderLayout.NORTH);
        JButton button=new JButton("Process and Propagate");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        });
        panel.add(button,BorderLayout.SOUTH);
        propPanel.add("Conversion",panel);
    }

    private Converter converter;
    private Converter[] converters={new OrbEl2Helio(),new Helio2OrbEl(),new Helio2Bary(),
            new CartToGC(),new GCToCart()};

    private static final long serialVersionUID=13468146987l;
    private static final double TINY=4e-15;

    public static interface Converter extends Serializable {
        String getValueDescription(int which,DataSource input);
        void setup(DataElement de);
        DataElement convert(DataElement de,int index,int g);
        JPanel getPropPanel();
        Converter copy();
    }

    public static class OrbEl2Helio implements Converter {
        @Override
        public String toString() { return "Orbital Elements to Heliocentric xv"; }
        @Override
        public String getValueDescription(int which,DataSource input) {
            String[] desc={"Time","x","y","z","vx","vy","vz"};
            if(which>0 && which<=6) return desc[which];
            return input.getValueDescription(0, which); 
        }
        @Override
        public void setup(DataElement de) {
            if(vals==null || vals.length!=ThreadHandler.instance().getNumThreads() || vals[0].length!=de.getNumValues()) {
                vals=new float[ThreadHandler.instance().getNumThreads()][de.getNumValues()];
            }
        }
        @Override
        public DataElement convert(DataElement de,int index,int g) {
            // This code taken from SWIFT orbel_el2xv.f with modificatiosn to fit Java and values we have here.
            double a=de.getValue(1);
            if(a==0) {
                vals[g][0]=de.getValue(0);
                for(int j=1; j<7; ++j) {
                    vals[g][j]=0;
                }
                for(int j=7; j<de.getNumValues(); ++j) {
                    vals[g][j]=de.getValue(j);
                }                
            }
            double e=de.getValue(2);
            double i=de.getValue(3);
            double OMG=de.getValue(4);
            double omg=de.getValue(5);
            double M=de.getValue(6);
            if(a==0 && e==0 && i==0) {
                vals[g][0]=de.getValue(0);
                for(int j=1; j<7; ++j) vals[g][j]=0;
                for(int j=7; j<de.getNumValues(); ++j) {
                    vals[g][j]=de.getValue(j);
                }
                return DataElement.replaceValues(de,vals[g]);
            }
            double sp=Math.sin(omg);
            double cp=Math.cos(omg);
            double so=Math.sin(OMG);
            double co=Math.cos(OMG);
            double si=Math.sin(i);
            double ci=Math.cos(i);
            double d11=cp*co-sp*so*ci;
            double d12=cp*so+sp*co*ci;
            double d13=sp*si;
            double d21=-sp*co-cp*so*ci;
            double d22=-sp*so+cp*co*ci;
            double d23=cp*si;
            double xfac1,xfac2,vfac1,vfac2;
            if(Math.abs(e-1.0)<TINY) { // parabolic
                double zpara=zget(M);
                double sqgma=Math.sqrt(2*gm.getValue()*a);
                xfac1=a*(1-zpara*zpara);
                xfac2=2*a*zpara;
                double ri=1/(a*(1+zpara*zpara));
                vfac1=-ri*sqgma*zpara;
                vfac2=ri*sqgma;
            } else if(e<1.0) { // elliptic
                double E=ehybrid(e,M);
                double sE=Math.sin(E);
                double cE=Math.cos(E);
                double sqe=Math.sqrt(1-e*e);
                double sqgma=Math.sqrt(gm.getValue()*a);
                xfac1=a*(cE-e);
                xfac2=a*sqe*sE;
                double ri=1/(a*(1-e*cE));
                vfac1=-ri*sqgma*sE;
                vfac2=ri*sqgma*sqe*cE;
            } else { // hyperbolic
                double F=fhybrid(e,M);
                double sF=Math.sinh(F);
                double cF=Math.cosh(F);
                double sqe=Math.sqrt(e*e-1);
                double sqgma=Math.sqrt(gm.getValue()*a);
                xfac1=a*(e-cF);
                xfac2=a*sqe*sF;
                double ri=1/(a*(e*cF-1));
                vfac1=-ri*sqgma*sF;
                vfac2=ri*sqgma*sqe*cF;
            }
            vals[g][0]=de.getValue(0);
            for(int j=7; j<de.getNumValues(); ++j) {
                vals[g][j]=de.getValue(j);
            }
            vals[g][1]=(float)(d11*xfac1+d21*xfac2);
            vals[g][2]=(float)(d12*xfac1+d22*xfac2);
            vals[g][3]=(float)(d13*xfac1+d23*xfac2);
            vals[g][4]=(float)(d11*vfac1+d21*vfac2);
            vals[g][5]=(float)(d12*vfac1+d22*vfac2);
            vals[g][6]=(float)(d13*vfac1+d23*vfac2);
            return DataElement.replaceValues(de,vals[g]);
        }
        private double zget(double q) {
            boolean flip=false;
            double ret;
            if(q<0) {
                q*=-1;
                flip=true;
            }
            if(q<1e-3) {
                ret=q*(1-(q*q/3)*(1-q*q));
            } else {
                double x=0.5*(3*q+Math.sqrt(9*q*q+4));
                double tmp=Math.pow(x,0.333333333);
                ret=tmp-1/tmp;
            }
            if(flip) {
                ret*=-1;
            }
            return ret;
        }
        // edited: Glenn, 2 June 06
        private double ehybrid(double e,double M) {
            if (e<0.18) return esolmd(e,M);
            else {
                if(e<0.8) return eget(e,M);
                return ehi(e,M);
            }
        }
        // added: Glenn, 2 June 06
        private double esolmd(double e, double M) {
            double ret=0.0;
            double sm=Math.sin(M);
            double cm=Math.cos(M);
            double x=M+e*sm*(1+e*(cm+e*(1-1.5*sm*sm)));
            double sE=Math.sin(e);
            double cE=Math.cos(e);
            double es=e*sE;
            double ec=e*cE;
            double f=x-es-M;
            double fp=1-ec;
            double fpp=es;
            double fppp=ec;
            double dx;
            dx=-f/fp;
            dx=-f/(fp+dx*fpp/2);
            dx=-f/(fp+dx*fpp/2+dx*dx*fppp/6);
            ret=x+dx;
            return ret;
        }
        private double eget(double e,double M) {
            double sm=Math.sin(M);
            double cm=Math.cos(M);
            double x=M+e*sm*(1+e*(cm+e*(1-1.5*sm*sm)));
            double es=e*Math.sin(x);
            double ec=e*Math.cos(x);
            double f=x-es-M;
            double fp=1-ec;
            double fpp=es;
            double fppp=ec;
            double dx=-f/fp;
            dx=-f/(fp+dx*fpp*0.5);
            dx=-f/(fp+dx*fpp*0.5+dx*dx*fppp/6);
            double ret=x+dx;
            x=ret;
            es=e*Math.sin(x);
            ec=e*Math.cos(x);
            f=x-es-M;
            fp=1-ec;
            fpp=es;
            fppp=ec;
            dx=-f/fp;
            dx=-f/(fp+dx*fpp*0.5);
            dx=-f/(fp+dx*fpp*0.5+dx*dx*fppp/6);
            ret=x+dx;
            return ret;
        }
        private double ehi(double e,double M) {
            boolean iflag=false;
            int nper=(int)(M/(2*Math.PI));
            M-=nper*2*Math.PI;
            if(M<0) M+=2*Math.PI;
            if(M>Math.PI) {
                M=2*Math.PI-M;
                iflag=true;
            }
            double x=Math.pow(6*M,0.333333333)-M;
            for(int i=0; i<3; ++i) {
                double esa=e*Math.sin(x+M);
                double eca=e*Math.cos(x+M);
                double f=x-esa;
                double fp=1-eca;
                double dx=-f/fp;
                dx=-f/(fp+0.5*dx*esa);
                dx=-f/(fp+0.5*dx*(esa+0.33333333333333*eca*dx));
                x+=dx;
            }
            double ret=M+x;
            if(iflag) {
                ret=2*Math.PI-ret;
            }
            return ret;
        }
        private double fhybrid(double e,double M) {
            double abm=Math.abs(M);
            if(abm<.636*e-0.6) {
                return flon(e,M);
            } else {
                return fget(e,M);
            }
        }
        private double flon(double e,double M) {
            boolean iflag=false;
            if(M<0) {
                iflag=true;
                M=-M;
            }
            double a1=6227020800.0*(1-1/e);
            double a0=-6227020800.0*M/e;
            double b1=a1;
            double a=6*(e-1)/e;
            double b=-6*M/e;
            double sq=Math.sqrt(0.25*b*b+a*a*a/27.0);
            double biga=Math.pow(-0.5*b+sq,0.3333333333333);
            double bigb=-Math.pow((0.5*b+sq),0.3333333333333);
            double x=biga+bigb;
            double ret=x;
            if(M<TINY) return ret*(iflag?-1:1);
            final double a3=1037836800.0;
            final double a5=51891840.0;
            final double a7=1235520.0;
            final double a9=17160.0;
            final double a11=156.0;
            final double b3=3*a3;
            final double b5=5*a5;
            final double b7=7*a7;
            final double b9=9*a9;
            final double b11=11*a11;
            for(int i=0; i<10; ++i) {
                double x2=x*x;
                double f=a0+x*(a1+x2*(a3+x2*(a5+x2*(a7+x2*(a9+x2*(a11+x2))))));
                double fp=b1+x2*(b3+x2*(b5+x2*(b7+x2*(b9+x2*(b11+13*x2)))));
                double dx=-f/fp;
                ret=x+dx;
                if(Math.abs(dx)<TINY) return ret*(iflag?-1:1);
                x=ret*(iflag?-1:1);
            }
            return ret*(iflag?-1:1);
        }
        private double fget(double e,double M) {
            double tmp,x;
            if(M<0) {
                tmp=-2*M/e+1.8;
                x=-Math.log(tmp);
            } else {
                tmp=2*M/e+1.8;
                x=-Math.log(tmp);
            }
            double ret=x;
            for(int i=0; i<10; ++i) {
                double esh=e*Math.sinh(x);
                double ech=e*Math.cosh(x);
                double f=esh-x-M;
                double fp=ech-1;
                double fpp=esh;
                double fppp=ech;
                double dx=-f/fp;
                dx=-f/(fp+dx*fpp*0.5);
                dx=-f/(fp+dx*fpp*0.5+dx*dx+fppp/6);
                ret=x+dx;
                if(Math.abs(dx)<TINY) return ret;
                x=ret;
            }
            return ret;
        }
        @Override
        public JPanel getPropPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new BorderLayout());
                northPanel.add(new JLabel("GM for Central Body"),BorderLayout.WEST);
                northPanel.add(gm.getTextField(null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        @Override
        public OrbEl2Helio copy() {
            OrbEl2Helio ret=new OrbEl2Helio();
            ret.gm=new EditableDouble(gm.getValue());
            return ret;
        }

        private EditableDouble gm=new EditableDouble(3.9478417604357354E+01);
        private transient JPanel propPanel;
        private transient float[][] vals;
        private static final long serialVersionUID=13468146987l;
    }
    public static class Helio2OrbEl implements Converter {
        @Override
        public String toString() { return "xv to Orbital Elements (requires mass else 0 assumed)"; }
        @Override
        public String getValueDescription(int which,DataSource input) {
            String[] desc={"Time","a","e","i","OMEGA","omega","M"};
            if(which>0 && which<=6) return desc[which];
            return input.getValueDescription(0, which); 
        }
        @Override
        public void setup(DataElement de) {
            if(vals==null || vals.length!=ThreadHandler.instance().getNumThreads() || vals[0].length!=de.getNumValues()) {
                vals=new float[ThreadHandler.instance().getNumThreads()][de.getNumValues()];
            }
            if(params==null || params.length!=ThreadHandler.instance().getNumThreads() || params[0].length!=de.getNumParams()+1) {
                params=new int[ThreadHandler.instance().getNumThreads()][de.getNumParams()+1];
            }
        }
        @Override
        public DataElement convert(DataElement de,int index,int group) {
            // This code taken from SWIFT orbel_xv2el.f with modificatiosn to fit Java and values we have here.
            double x=de.getValue(1);
            double y=de.getValue(2);
            double z=de.getValue(3);
            double vx=de.getValue(4);
            double vy=de.getValue(5);
            double vz=de.getValue(6);
            double gmsum;
            if(useMass.getValue() && de.getNumValues()>7) {
                gmsum=g.getValue()*(mass.getValue()+de.getValue(7));
            } else {
                gmsum=g.getValue()*mass.getValue();
            }
            double hx=y*vz-z*vy;
            double hy=z*vx-x*vz;
            double hz=x*vy-y*vx;
            double h2=hx*hx+hy*hy+hz*hz;
            double h=Math.sqrt(h2);
            double a,e,i,OMG,omg,M;
            i=Math.acos(hz/h);
            double fac=Math.sqrt(hx*hx+hy*hy)/h;
            double u;
            if(fac<TINY) {
                OMG=0;
                u=Math.atan2(y,x);
                if(Math.abs(i-Math.PI)<10*TINY) u*=-1;
            } else {
                OMG=Math.atan2(hx,-hy);
                u=Math.atan2(z/Math.sin(i),x*Math.cos(OMG)+y*Math.sin(OMG));
            }
            if(OMG<0) OMG+=2*Math.PI;
            if(u<0) u+=2*Math.PI;
            double r=Math.sqrt(x*x+y*y+z*z);
            double v2=vx*vx+vy*vy+vz*vz;
            double vdotr=x*vx+y*vy+z*vz;
            double energy=0.5*v2-gmsum/r;
            int iAlpha;
            if(Math.abs(energy*r/gmsum)<Math.sqrt(TINY)) {
                iAlpha=0;
            } else {
                if(energy<0) iAlpha=-1;
                else iAlpha=1;
            }
            if(iAlpha==-1) {
                // Ellipse
                a=-0.5*gmsum/energy;
                fac=1-h2/(gmsum*a);
                double E,w;
                if(fac>TINY) {
                    e=Math.sqrt(fac);
                    double face=(a-r)/(a*e);
                    if(face>1) {
                        E=0.0;
                    } else if(face>-1) {
                        E=Math.acos(face);
                    } else {
                        E=Math.PI;
                    }
                    if(vdotr<0) E=2*Math.PI-E;
                    double cw=(Math.cos(E)-e)/(1-e*Math.cos(E));
                    double sw=Math.sqrt(1-e*e)*Math.sin(E)/(1-e*Math.cos(E));
                    w=Math.atan2(sw,cw);
                    if(w<0) w+=2*Math.PI;
                } else {
                    e=0.0;
                    w=u;
                    E=u;
                }
                M=E-e*Math.sin(E);
                omg=u-w;
                if(omg<0) omg+=2*Math.PI;
                omg-=((int)(omg/(2*Math.PI)))*2*Math.PI;
            } else if(iAlpha==1) {
                // Hyperbola
                a=0.5*gmsum/energy;
                fac=h2/(gmsum*a);
                double F,w;
                if(fac>TINY) {
                    e=Math.sqrt(1+fac);
                    double tmpf=(a+r)/(a*e);
                    if(tmpf<1) tmpf=1;
                    F=Math.log(tmpf+Math.sqrt(tmpf*tmpf-1));
                    if(vdotr<0) F*=-1;
                    double cw=(e-Math.cosh(F))/(e*Math.cosh(F)-1);
                    double sw=Math.sqrt(e*e-1)*Math.sinh(F)/(e*Math.cosh(F)-1);
                    w=Math.atan2(sw,cw);
                    if(w<0) w+=2*Math.PI;
                } else {
                    e=1;
                    double tmpf=0.5*h2/gmsum;
                    w=Math.acos(2*tmpf/r-1);
                    if(vdotr<0) w=2*Math.PI-w;
                    tmpf=(a+r)/(a*e);
                    F=Math.log(tmpf+Math.sqrt(tmpf*tmpf-1));
                }
                M=e*Math.sinh(F)-F;
                omg=u-w;
                if(omg<0) omg+=2*Math.PI;
                omg-=((int)(omg/(2*Math.PI)))*2*Math.PI;                
            } else {
                // Parabola
                a=0.5*h2/gmsum;
                e=1;
                double w=Math.acos(2*a/r-1);
                if(vdotr<0) w=2*Math.PI-w;
                double tmpf=Math.tan(0.5*w);
                M=tmpf*(1+tmpf*tmpf/3);
                omg=u-w;
                if(omg<0) omg+=2*Math.PI;
                omg-=((int)(omg/(2*Math.PI)))*2*Math.PI;                
            }
            for(int j=0; j<de.getNumParams(); ++j) params[group][j]=de.getParam(j);
            params[group][de.getNumParams()]=iAlpha;
            vals[group][0]=de.getValue(0);
            vals[group][1]=(float)a;
            vals[group][2]=(float)e;
            vals[group][3]=(float)i;
            vals[group][4]=(float)OMG;
            vals[group][5]=(float)omg;
            vals[group][6]=(float)M;
            for(int j=7; j<vals[group].length; ++j) vals[group][j]=de.getValue(j);
            return new DataElement(params[group],vals[group]);
        }
        @Override
        public JPanel getPropPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new GridLayout(3,1));
                JPanel panel=new JPanel(new BorderLayout());
                panel.add(new JLabel("G"),BorderLayout.WEST);
                panel.add(g.getTextField(null));
                northPanel.add(panel);
                panel=new JPanel(new BorderLayout());
                panel.add(new JLabel("M for Central Body"),BorderLayout.WEST);
                panel.add(mass.getTextField(null));
                northPanel.add(panel);
                northPanel.add(useMass.getCheckBox("Use particle masses if present?",null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }
        @Override
        public Helio2OrbEl copy() {
            Helio2OrbEl ret=new Helio2OrbEl();
            ret.g=new EditableDouble(g.getValue());
            ret.mass=new EditableDouble(mass.getValue());
            ret.useMass=new EditableBoolean(useMass.getValue());
            return ret;
        }

        private EditableDouble g=new EditableDouble(1.0);
        private EditableDouble mass=new EditableDouble(3.9478417604357354E+01);
        private EditableBoolean useMass=new EditableBoolean(false);
        private transient JPanel propPanel;
        private transient float[][] vals;
        private transient int[][] params;
        private static final long serialVersionUID=235784357087326l;
    }

    public class Helio2Bary implements Converter {
        @Override
        public String toString() { return "Heliocentric xv to Barycentric xv"; }

        @Override
        public String getValueDescription(int which,DataSource input) {
            String[] desc={"Time","x","y","z","vx","vy","vz","Mass"};
            if(which>0 && which<=7) return desc[which];
            return input.getValueDescription(0, which); 
        }

        @Override
        public void setup(DataElement de) {
            if(vals==null || vals.length!=ThreadHandler.instance().getNumThreads() || vals[0].length!=de.getNumValues()) {
                vals=new float[ThreadHandler.instance().getNumThreads()][de.getNumValues()];
            }
        }

        @Override
        public DataElement convert(DataElement de,int index,int g) {
            if(index==0 || index>=endGroup) {
                cmx=0.0f;
                cmy=0.0f;
                cmz=0.0f;
                cmvx=0.0f;
                cmvy=0.0f;
                cmvz=0.0f;
                cm=0.0f;
                startGroup=index;
                double time=de.getValue(0);
                boolean hasSun=false;
                int i;
                for(i=startGroup; i<input.getNumElements(0) && time==input.getElement(i, 0).getValue(0); ++i) {
                    DataElement e=input.getElement(i, 0);
                    if(e.getParam(0)==-1) hasSun=true;
                    float m=0;
                    try {
                        m=e.getValue(7);
                    } catch(ArrayIndexOutOfBoundsException ex) {
                        JOptionPane.showMessageDialog(propPanel,"v[7] must contain a mass.  No value for v[7] present.");
                        throw new ConverterException("v[7] must contain a mass.  No value for v[7] present.",ex);
                    }
                    cm+=m;
                    cmx+=e.getValue(1)*m;
                    cmy+=e.getValue(2)*m;
                    cmz+=e.getValue(3)*m;
                    cmvx+=e.getValue(4)*m;
                    cmvy+=e.getValue(5)*m;
                    cmvz+=e.getValue(6)*m;
                }
                if(!hasSun) cm+=mass.getValue();
                cmx/=cm;
                cmy/=cm;
                cmz/=cm;
                cmvx/=cm;
                cmvy/=cm;
                cmvz/=cm;
                endGroup=i;
            }
            vals[g][0]=de.getValue(0);
            vals[g][1]=de.getValue(1)-cmx;
            vals[g][2]=de.getValue(2)-cmy;
            vals[g][3]=de.getValue(3)-cmz;
            vals[g][4]=de.getValue(4)-cmvx;
            vals[g][5]=de.getValue(5)-cmvy;
            vals[g][6]=de.getValue(6)-cmvz;
            for(int i=7; i<vals[g].length; ++i) {
                vals[g][i]=de.getValue(i);
            }
            return DataElement.replaceValues(de,vals[g]);
        }

        @Override
        public JPanel getPropPanel() {
            if(propPanel==null) {
                propPanel=new JPanel(new BorderLayout());
                JPanel northPanel=new JPanel(new BorderLayout());
                northPanel.add(new JLabel("M for Central Body"),BorderLayout.WEST);
                northPanel.add(mass.getTextField(null));
                propPanel.add(northPanel,BorderLayout.NORTH);
            }
            return propPanel;
        }

        @Override
        public Converter copy() {
            Helio2Bary ret=new Helio2Bary();
            ret.mass=new EditableDouble(mass.getValue());
            return ret;
        }

        private EditableDouble mass=new EditableDouble(3.9478417604357354E+01);
        private transient JPanel propPanel;
        private transient int startGroup=-1;
        private transient int endGroup=-1;
        private transient float cmx;
        private transient float cmy;
        private transient float cmz;
        private transient float cmvx;
        private transient float cmvy;
        private transient float cmvz;
        private transient float cm;
        private transient float[][] vals;
        private static final long serialVersionUID=562087562l;
    }

    public static class CartToGC implements Converter {
        @Override
        public String toString() { return "Rings - Cartesian to Guiding Center"; }

        @Override
        public String getValueDescription(int which,DataSource input) {
            String[] desc={"X","Y","e","phi","i","ksi"};
            if(which>0 && which<=5) return desc[which];
            return input.getValueDescription(0, which); 
        }

        @Override
        public void setup(DataElement de) {
            if(vals==null || vals.length!=ThreadHandler.instance().getNumThreads() || vals[0].length!=de.getNumValues()) {
                vals=new float[ThreadHandler.instance().getNumThreads()][de.getNumValues()];
            }
        }

        @Override
        public DataElement convert(DataElement de, int index,int g) {
            vals[g][0]=(2*(2*de.getValue(0)+de.getValue(4)));
            vals[g][1]=(de.getValue(1)-de.getValue(3)*2);
            double dx=de.getValue(0)-vals[g][0];
            double dy=de.getValue(1)-vals[g][1];
            vals[g][2]=(float)(Math.sqrt(dx*dx+dy*dy/4));
            vals[g][3]=(float)(Math.atan2(dy,-2*dx));
            vals[g][5]=(float)(Math.atan2(-de.getValue(5),de.getValue(2)));
            vals[g][4]=(float)(Math.abs(de.getValue(2)/Math.cos(vals[g][5])));
            for(int i=6; i<vals[g].length; ++i) {
                vals[g][i]=de.getValue(i);
            }
            return DataElement.replaceValues(de,vals[g]);
        }

        @Override
        public JPanel getPropPanel() {
            if(propPanel==null) propPanel=new JPanel();
            return propPanel;
        }

        @Override
        public Converter copy() {
            return new CartToGC();
        }

        private transient float[][] vals;
        private transient JPanel propPanel;
        private static final long serialVersionUID=352359875734l;
    }

    public static class GCToCart implements Converter {
        @Override
        public String toString() { return "Rings - Guiding Center to Cartesian"; }

        @Override
        public String getValueDescription(int which,DataSource input) {
            String[] desc={"x","y","z","vx","vy","vz"};
            if(which>0 && which<=5) return desc[which];
            return input.getValueDescription(0, which); 
        }

        @Override
        public void setup(DataElement de) {
            if(vals==null || vals.length!=ThreadHandler.instance().getNumThreads() || vals[0].length!=de.getNumValues()) {
                vals=new float[ThreadHandler.instance().getNumThreads()][de.getNumValues()];
            }
        }

        @Override
        public DataElement convert(DataElement de, int index,int g) {
            double cosp=Math.cos(de.getValue(3));
            double sinp=Math.sin(de.getValue(3));
            vals[g][0]=(float)(de.getValue(0)-de.getValue(2)*cosp);
            vals[g][1]=(float)(de.getValue(1)+2*de.getValue(2)*sinp);
            vals[g][2]=(float)(de.getValue(4)*Math.cos(de.getValue(5)));
            vals[g][3]=(float)(de.getValue(2)*sinp);
            vals[g][4]=(float)(2*de.getValue(2)*cosp-1.5*de.getValue(0));
            vals[g][5]=(float)(-de.getValue(4)*Math.cos(de.getValue(5)));
            for(int i=6; i<vals[g].length; ++i) {
                vals[g][i]=de.getValue(i);
            }
            return DataElement.replaceValues(de,vals[g]);
        }

        @Override
        public JPanel getPropPanel() {
            if(propPanel==null) propPanel=new JPanel();
            return propPanel;
        }

        @Override
        public Converter copy() {
            return new GCToCart();
        }

        private transient float[][] vals;
        private transient JPanel propPanel;
        private static final long serialVersionUID=352359875734l;
    }

    public static class ConverterException extends RuntimeException {
        public ConverterException(String s,Exception e) {
            super(s,e);
        }
        private static final long serialVersionUID = -5403743100099359152L;
    }
}
