/*
 * Created on Aug 2, 2008
 */
package edu.swri.swiftvis.plot.p3d.styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.plot.Plot;
import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.RenderEngine;
import edu.swri.swiftvis.plot.p3d.Style3D;
import edu.swri.swiftvis.plot.p3d.Triangle3D;
import edu.swri.swiftvis.plot.p3d.Vect3D;
import edu.swri.swiftvis.plot.util.ColorGradient;
import edu.swri.swiftvis.plot.util.ColorModel;
import edu.swri.swiftvis.util.EditableString;

public class SurfaceStyle3D implements Style3D {
    public SurfaceStyle3D(PlotArea3D p) {
        plotArea=p;
    }

    public SurfaceStyle3D(SurfaceStyle3D c,PlotArea3D p) {
        plotArea=p;
        name.setValue(c.name.getValue());
        xFormula.setFormula(c.xFormula.getFormula());
        yFormula.setFormula(c.yFormula.getFormula());
        zFormula.setFormula(c.zFormula.getFormula());
        columnMatchFormula.setFormula(c.columnMatchFormula.getFormula());
        opacityFormula.setFormula(c.opacityFormula.getFormula());
        reflectFormula.setFormula(c.reflectFormula.getFormula());
        colorModel=new ColorModel(c.colorModel);
    }
    
    @Override
    public Style3D copy(PlotArea3D c) {
        return new SurfaceStyle3D(this,c);
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            Box northPanel=new Box(BoxLayout.Y_AXIS);
            JPanel outerPanel=new JPanel(new GridLayout(11,1));
            outerPanel.setBorder(BorderFactory.createTitledBorder("Data Set "));

            outerPanel.add(name.getLabeledTextField("Name",null));
            outerPanel.add(xFormula.getLabeledTextField("X Formula",null));
            outerPanel.add(yFormula.getLabeledTextField("Y Formula",null));
            outerPanel.add(zFormula.getLabeledTextField("Z Formula",null));
            
            colorModel.addGUIToPanel("Color Model",outerPanel);
            outerPanel.add(opacityFormula.getLabeledTextField("Opacity Formula",null));
            outerPanel.add(reflectFormula.getLabeledTextField("Reflectivity Formula",null));

            northPanel.add(outerPanel);

            northPanel.add(columnMatchFormula.getLabeledTextField("Column Match Formula",null));
            
            propPanel.add(northPanel,BorderLayout.NORTH);
        }
        return propPanel;
    }

    @Override
    public void renderToEngine(RenderEngine engine) {
        Plot sink=plotArea.getSink();
        int[] range=xFormula.getSafeElementRange(sink, 0);
        DataFormula.mergeSafeElementRanges(range,yFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,zFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,columnMatchFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,opacityFormula.getSafeElementRange(sink, 0));
        DataFormula.mergeSafeElementRanges(range,reflectFormula.getSafeElementRange(sink, 0));
        DataFormula.checkRangeSafety(range,sink);

        if(range[0]>=range[1]) return;
        int rowCount=1;
        double colVal=columnMatchFormula.valueOf(sink,0, range[0]);
        for(int i=range[0]+1; i<range[1] && columnMatchFormula.valueOf(sink,0, i)==colVal; i++,rowCount++);
        if(rowCount<2) return;
        
        int columnCount=(range[1]-range[0]+1)/rowCount;
        Vect3D[][] pnts=new Vect3D[columnCount][rowCount];
        DrawAttributes[][] attrs=new DrawAttributes[columnCount][rowCount];
        float[][][] colorVals=new float[columnCount][rowCount][];
        double avZ=0;
        int index=range[0];
        for(int i=0; i<columnCount; ++i) {
            for(int j=0; j<rowCount; ++j) {
                pnts[i][j]=new Vect3D(xFormula.valueOf(sink,0, index),yFormula.valueOf(sink,0, index),zFormula.valueOf(sink,0, index));
                attrs[i][j]=new DrawAttributes(colorModel.getColor(sink,index),reflectFormula.valueOf(sink,0, index),opacityFormula.valueOf(sink,0, index));
                colorVals[i][j]=attrs[i][j].getColor().getColorComponents(null);
                avZ+=pnts[i][j].getZ();
                index++;
            }
        }
        avZ/=columnCount*rowCount;
        for(int i=0; i<columnCount-1; ++i) {
            for(int j=0; j<rowCount-1; ++j) {
                // points
                double cx=(pnts[i][j].getX()+pnts[i+1][j].getX()+pnts[i][j+1].getX()+pnts[i+1][j+1].getX())/4;
                double cy=(pnts[i][j].getY()+pnts[i+1][j].getY()+pnts[i][j+1].getY()+pnts[i+1][j+1].getY())/4;
                double cz=(pnts[i][j].getZ()+pnts[i+1][j].getZ()+pnts[i][j+1].getZ()+pnts[i+1][j+1].getZ())/4;
                Vect3D cPnt=new Vect3D(cx,cy,cz);
                
                // attributes
                float[] cComp=new float[3];
                cComp[0]=(colorVals[i][j][0]+colorVals[i+1][j][0]+colorVals[i][j+1][0]+colorVals[i+1][j+1][0])/4;
                cComp[1]=(colorVals[i][j][1]+colorVals[i+1][j][1]+colorVals[i][j+1][1]+colorVals[i+1][j+1][1])/4;
                cComp[2]=(colorVals[i][j][2]+colorVals[i+1][j][2]+colorVals[i][j+1][2]+colorVals[i+1][j+1][2])/4;
                double cOpacity=(attrs[i][j].getOpacity()+attrs[i+1][j].getOpacity()+attrs[i][j+1].getOpacity()+attrs[i+1][j+1].getOpacity())/4;
                double cReflect=(attrs[i][j].getReflectivity()+attrs[i+1][j].getReflectivity()+attrs[i][j+1].getReflectivity()+attrs[i+1][j+1].getReflectivity())/4;
                DrawAttributes cAttrs=new DrawAttributes(new Color(cComp[0],cComp[1],cComp[2]),cReflect,cOpacity);
                
                // normals
                Vect3D n1=calcPointNormal(pnts,i,j);
                Vect3D n2=calcPointNormal(pnts,i+1,j);
                Vect3D n3=calcPointNormal(pnts,i+1,j+1);
                Vect3D n4=calcPointNormal(pnts,i,j+1);
                Vect3D cn=calcCentralNormal(pnts,i,j,cPnt);
                
                // geom
                Triangle3D t1=new Triangle3D(pnts[i][j],pnts[i+1][j],cPnt,n1,n2,cn);
                Triangle3D t2=new Triangle3D(pnts[i+1][j],pnts[i+1][j+1],cPnt,n2,n3,cn);
                Triangle3D t3=new Triangle3D(pnts[i+1][j+1],pnts[i][j+1],cPnt,n3,n4,cn);
                Triangle3D t4=new Triangle3D(pnts[i][j+1],pnts[i][j],cPnt,n4,n1,cn);
                engine.add(t1,new DrawAttributes[]{attrs[i][j],attrs[i+1][j],cAttrs});
                engine.add(t2,new DrawAttributes[]{attrs[i+1][j],attrs[i+1][j+1],cAttrs});
                engine.add(t3,new DrawAttributes[]{attrs[i+1][j+1],attrs[i][j+1],cAttrs});
                engine.add(t4,new DrawAttributes[]{attrs[i][j+1],attrs[i][j],cAttrs});
            }
        }
    }
    
    private Vect3D calcPointNormal(Vect3D[][] pnts,int i,int j) {
        int[] offsetX={0,1,0,-1};
        int[] offsetY={1,0,-1,0};
        double[] norm=new double[3];
        double[] v1=new double[3];
        double[] v2=new double[3];
        for(int k=0; k<offsetX.length; ++k) {
            int oi=i+offsetX[k];
            int oj=j+offsetY[k];
            if(oi>=0 && oi<pnts.length && oj>=0 && oj<pnts[oi].length) {
                int oi2=i+offsetX[(k+1)%offsetX.length];
                int oj2=j+offsetY[(k+1)%offsetX.length];
                if(oi2>=0 && oi2<pnts.length && oj2>=0 && oj2<pnts[oi2].length) {
                    v1[0]=pnts[oi][oj].getX()-pnts[i][j].getX();
                    v1[1]=pnts[oi][oj].getY()-pnts[i][j].getY();
                    v1[2]=pnts[oi][oj].getZ()-pnts[i][j].getZ();
                    v2[0]=pnts[oi2][oj2].getX()-pnts[i][j].getX();
                    v2[1]=pnts[oi2][oj2].getY()-pnts[i][j].getY();
                    v2[2]=pnts[oi2][oj2].getZ()-pnts[i][j].getZ();
                    double[] lnorm=Basic.crossProduct(v2,v1);
                    norm[0]+=lnorm[0];
                    norm[1]+=lnorm[1];
                    norm[2]+=lnorm[2];
                }
            }
        }
        double len=Basic.magnitude(norm);
        norm[0]/=len;
        norm[1]/=len;
        norm[2]/=len;
        return new Vect3D(norm);
    }

    private Vect3D calcCentralNormal(Vect3D[][] pnts,int i,int j,Vect3D cPnt) {
        int[] offsetX={0,1,1,0};
        int[] offsetY={0,0,1,1};
        double[] norm=new double[3];
        double[] v1=new double[3];
        double[] v2=new double[3];
        for(int k=0; k<offsetX.length; ++k) {
            int oi=i+offsetX[k];
            int oj=j+offsetY[k];
            int oi2=i+offsetX[(k+1)%offsetX.length];
            int oj2=j+offsetY[(k+1)%offsetX.length];
            v1[0]=pnts[oi][oj].getX()-cPnt.getX();
            v1[1]=pnts[oi][oj].getY()-cPnt.getY();
            v1[2]=pnts[oi][oj].getZ()-cPnt.getZ();
            v2[0]=pnts[oi2][oj2].getX()-cPnt.getX();
            v2[1]=pnts[oi2][oj2].getY()-cPnt.getY();
            v2[2]=pnts[oi2][oj2].getZ()-cPnt.getZ();
            double[] lnorm=Basic.crossProduct(v1,v2);
            norm[0]+=lnorm[0];
            norm[1]+=lnorm[1];
            norm[2]+=lnorm[2];
        }
        double len=Basic.magnitude(norm);
        norm[0]/=len;
        norm[1]/=len;
        norm[2]/=len;
        return new Vect3D(norm);
    }

    @Override
    public String toString() {
        return "Surface 3D - "+name.getValue();
    }
    
    public static String getTypeDescription() {
        return "Surface Plot 3D";
    }

    private PlotArea3D plotArea;
    private EditableString name=new EditableString("Default");

    private DataFormula columnMatchFormula = new DataFormula("v[0]");
    private DataFormula xFormula = new DataFormula("v[0]");
    private DataFormula yFormula = new DataFormula("v[1]");
    private DataFormula zFormula = new DataFormula("v[2]");
    private ColorModel colorModel=new ColorModel(ColorModel.GRADIENT_TYPE,"0",new ColorGradient(Color.black,Color.white),"1");
    private DataFormula opacityFormula=new DataFormula("1");
    private DataFormula reflectFormula=new DataFormula("0");

    private transient JPanel propPanel;

    private static final long serialVersionUID = -8339750049905520188L;
}
