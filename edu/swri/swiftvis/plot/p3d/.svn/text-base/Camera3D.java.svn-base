/*
 * Created on Jul 12, 2008
 */
package edu.swri.swiftvis.plot.p3d;

import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.PlotArea3D;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;

/**
 * This class is intended to encapsulate a camera for doing 3-D visualization.
 * 
 * @author Mark Lewis
 */
public class Camera3D implements Serializable {
    public Camera3D() {}

    public Camera3D(PlotArea3D pa) {
        plotArea=pa;
    }
    
    public void setValuesFrom(Camera3D c,PlotArea3D pa) {
        plotArea=pa;
        centerX.setFormula(c.centerX.getFormula());
        centerY.setFormula(c.centerY.getFormula());
        centerZ.setFormula(c.centerZ.getFormula());
        forwardX.setFormula(c.forwardX.getFormula());
        forwardY.setFormula(c.forwardY.getFormula());
        forwardZ.setFormula(c.forwardZ.getFormula());
        upX.setFormula(c.upX.getFormula());
        upY.setFormula(c.upY.getFormula());
        upZ.setFormula(c.upZ.getFormula());
        moveDist.setValue(c.moveDist.getValue());
        rotateStep.setValue(c.rotateStep.getValue());
        rotatePointDistance.setValue(c.rotatePointDistance.getValue());
        redrawOnDrag.setValue(c.redrawOnDrag.getValue());
    }
    
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new GridLayout(13,1));
            propPanel.add(centerX.getLabeledTextField("Center X",null));
            propPanel.add(centerY.getLabeledTextField("Center Y",null));
            propPanel.add(centerZ.getLabeledTextField("Center Z",null));
            propPanel.add(forwardX.getLabeledTextField("Forward X",null));
            propPanel.add(forwardY.getLabeledTextField("Forward Y",null));
            propPanel.add(forwardZ.getLabeledTextField("Forward Z",null));
            propPanel.add(upX.getLabeledTextField("Up X",null));
            propPanel.add(upY.getLabeledTextField("Up Y",null));
            propPanel.add(upZ.getLabeledTextField("Up Z",null));
            propPanel.add(moveDist.getLabeledTextField("Move Step Size",null));
            propPanel.add(rotateStep.getLabeledTextField("Key Rotate Step Size (rads)",null));
            propPanel.add(rotatePointDistance.getLabeledTextField("Mouse Rotate Center Distance",null));
            propPanel.add(redrawOnDrag.getCheckBox("Redraw on Drag?",null));
        }
        return propPanel;
    }
    
    public Vect3D getCenter(DataSink sink) {
        return new Vect3D(centerX.valueOf(sink,0, 0),centerY.valueOf(sink,0, 0),centerZ.valueOf(sink,0, 0));
    }
    
    public Vect3D getForward(DataSink sink) {
        return new Vect3D(forwardX.valueOf(sink,0, 0),forwardY.valueOf(sink,0, 0),forwardZ.valueOf(sink,0, 0));
    }
    
    public Vect3D getUp(DataSink sink) {
        return new Vect3D(upX.valueOf(sink,0, 0),upY.valueOf(sink,0, 0),upZ.valueOf(sink,0, 0));
    }
    
    public void mousePressed(final MouseEvent e, final double mx, final double my,final DataSink sink) {
        startX=mx;
        startY=my;
    }

    public void mouseReleased(final MouseEvent e, final double mx, final double my,final DataSink sink) {
        applyMouseMove(mx,my);
    }

    public void mouseClicked(final MouseEvent e, final double mx, final double my,final DataSink sink) {
    }

    public void mouseMoved(final MouseEvent e, final double mx, final double my,final DataSink sink) {
    }

    public void mouseDragged(final MouseEvent e, final double mx, final double my,final DataSink sink) {
        if(redrawOnDrag.getValue()) {
            applyMouseMove(mx,my);
            startX=mx;
            startY=my;
        }
    }
    
    private void applyMouseMove(double ex,double ey) {
        DataSink sink=plotArea.getSink();
        Vect3D center=getCenter(sink);
        Vect3D forward=getForward(sink).normalize();
        Vect3D up=getUp(sink).normalize();
        double dx=(ex-startX)/5.0;
        double dy=-(ey-startY)/5.0;
        Vect3D rotCenter=center.plus(forward.mult(rotatePointDistance.getValue()));
        if(dy!=0) {
            double c=Math.cos(dy);
            double s=Math.sin(dy);
            Vect3D newForward=forward.mult(c).plus(up.mult(s));
            up=up.mult(c).minus(forward.mult(s));
            forwardX.setFormula(Double.toString(newForward.getX()));
            forwardY.setFormula(Double.toString(newForward.getY()));
            forwardZ.setFormula(Double.toString(newForward.getZ()));
            upX.setFormula(Double.toString(up.getX()));
            upY.setFormula(Double.toString(up.getY()));
            upZ.setFormula(Double.toString(up.getZ()));
        }
        if(dx!=0) {
            Vect3D right=Basic.crossProduct(forward,up).normalize();
            double c=Math.cos(dx);
            double s=Math.sin(dx);
            forward=forward.mult(c).plus(right.mult(s));
            forwardX.setFormula(Double.toString(forward.getX()));
            forwardY.setFormula(Double.toString(forward.getY()));
            forwardZ.setFormula(Double.toString(forward.getZ()));
        }
        center=rotCenter.minus(forward.mult(rotatePointDistance.getValue()));
        centerX.setFormula(Double.toString(center.getX()));
        centerY.setFormula(Double.toString(center.getY()));
        centerZ.setFormula(Double.toString(center.getZ()));
        plotArea.cameraMoved();        
    }

    public void keyPressed(final KeyEvent e,final DataSink sink) {
        Vect3D center=getCenter(sink);
        Vect3D forward=getForward(sink).normalize();
        Vect3D up=getUp(sink).normalize();
        Vect3D right=Basic.crossProduct(forward,up).normalize();
        if(e.getKeyCode()==KeyEvent.VK_UP) {
            center=center.plus(forward.mult(moveDist.getValue()));
            centerX.setFormula(Double.toString(center.getX()));
            centerY.setFormula(Double.toString(center.getY()));
            centerZ.setFormula(Double.toString(center.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_DOWN) {
            center=center.minus(forward.mult(moveDist.getValue()));
            centerX.setFormula(Double.toString(center.getX()));
            centerY.setFormula(Double.toString(center.getY()));
            centerZ.setFormula(Double.toString(center.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_LEFT) {
            center=center.minus(right.mult(moveDist.getValue()));
            centerX.setFormula(Double.toString(center.getX()));
            centerY.setFormula(Double.toString(center.getY()));
            centerZ.setFormula(Double.toString(center.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();            
        } else if(e.getKeyCode()==KeyEvent.VK_RIGHT) {
            center=center.plus(right.mult(moveDist.getValue()));
            centerX.setFormula(Double.toString(center.getX()));
            centerY.setFormula(Double.toString(center.getY()));
            centerZ.setFormula(Double.toString(center.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();            
        } else if(e.getKeyCode()==KeyEvent.VK_PAGE_UP) {
            center=center.plus(up.mult(moveDist.getValue()));
            centerX.setFormula(Double.toString(center.getX()));
            centerY.setFormula(Double.toString(center.getY()));
            centerZ.setFormula(Double.toString(center.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_PAGE_DOWN) {
            center=center.minus(up.mult(moveDist.getValue()));
            centerX.setFormula(Double.toString(center.getX()));
            centerY.setFormula(Double.toString(center.getY()));
            centerZ.setFormula(Double.toString(center.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_A) {
            double c=Math.cos(-rotateStep.getValue());
            double s=Math.sin(-rotateStep.getValue());
            forward=forward.mult(c).plus(right.mult(s));
            forwardX.setFormula(Double.toString(forward.getX()));
            forwardY.setFormula(Double.toString(forward.getY()));
            forwardZ.setFormula(Double.toString(forward.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_D) {
            double c=Math.cos(rotateStep.getValue());
            double s=Math.sin(rotateStep.getValue());
            forward=forward.mult(c).plus(right.mult(s));
            forwardX.setFormula(Double.toString(forward.getX()));
            forwardY.setFormula(Double.toString(forward.getY()));
            forwardZ.setFormula(Double.toString(forward.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_W) {
            double c=Math.cos(rotateStep.getValue());
            double s=Math.sin(rotateStep.getValue());
            Vect3D newForward=forward.mult(c).plus(up.mult(s));
            up=up.mult(c).minus(forward.mult(s));
            forwardX.setFormula(Double.toString(newForward.getX()));
            forwardY.setFormula(Double.toString(newForward.getY()));
            forwardZ.setFormula(Double.toString(newForward.getZ()));
            upX.setFormula(Double.toString(up.getX()));
            upY.setFormula(Double.toString(up.getY()));
            upZ.setFormula(Double.toString(up.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_S) {
            double c=Math.cos(-rotateStep.getValue());
            double s=Math.sin(-rotateStep.getValue());
            Vect3D newForward=forward.mult(c).plus(up.mult(s));
            up=up.mult(c).minus(forward.mult(s));
            forwardX.setFormula(Double.toString(newForward.getX()));
            forwardY.setFormula(Double.toString(newForward.getY()));
            forwardZ.setFormula(Double.toString(newForward.getZ()));
            upX.setFormula(Double.toString(up.getX()));
            upY.setFormula(Double.toString(up.getY()));
            upZ.setFormula(Double.toString(up.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_Q) {
            double c=Math.cos(rotateStep.getValue());
            double s=Math.sin(rotateStep.getValue());
            up=up.mult(c).plus(right.mult(s));
            upX.setFormula(Double.toString(up.getX()));
            upY.setFormula(Double.toString(up.getY()));
            upZ.setFormula(Double.toString(up.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        } else if(e.getKeyCode()==KeyEvent.VK_E) {
            double c=Math.cos(-rotateStep.getValue());
            double s=Math.sin(-rotateStep.getValue());
            up=up.mult(c).plus(right.mult(s));
            upX.setFormula(Double.toString(up.getX()));
            upY.setFormula(Double.toString(up.getY()));
            upZ.setFormula(Double.toString(up.getZ()));
            if(plotArea!=null) plotArea.cameraMoved();
        }
    }

    public void keyReleased(final KeyEvent e,final DataSink sink) {
    }

    public void keyTyped(final KeyEvent e,final DataSink sink) {
    }
    
    private PlotArea3D plotArea;

    private DataFormula centerX=new DataFormula("1.0");
    private DataFormula centerY=new DataFormula("-5.0");
    private DataFormula centerZ=new DataFormula("1.0");
    private DataFormula forwardX=new DataFormula("0.0");
    private DataFormula forwardY=new DataFormula("1.0");
    private DataFormula forwardZ=new DataFormula("0.0");
    private DataFormula upX=new DataFormula("0.0");
    private DataFormula upY=new DataFormula("0.0");
    private DataFormula upZ=new DataFormula("1.0");
    private EditableDouble moveDist=new EditableDouble(0.1);
    private EditableDouble rotateStep=new EditableDouble(0.1);
    private EditableDouble rotatePointDistance=new EditableDouble(5);
    private EditableBoolean redrawOnDrag=new EditableBoolean(true);
    
    private transient JComponent propPanel;
    private transient double startX,startY;

    private static final long serialVersionUID = -9058983672149854265L;
}
