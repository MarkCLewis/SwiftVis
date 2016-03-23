/*
 * Created on Jun 12, 2007
 */
package edu.swri.swiftvis.scripting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import edu.swri.swiftvis.plot.PlotTransform;

public class ScriptDrawer {
    public ScriptDrawer(Graphics2D graphics,PlotTransform t) {
        g=graphics;
        trans=t;
    }
    
    public double[][] getBounds() { return bounds; }
    
    public void setColor(float red,float green,float blue) {
        if(g!=null) g.setPaint(new Color(red,green,blue));
    }
    
    public void setStrokeWidth(float width) {
        if(g!=null) g.setStroke(new BasicStroke(width));
    }
    
    public void fillPoly(Object o) {
       double[][] pnts=ScriptUtil.objectToDouble2DArray(o);
       for(int i=0; i<pnts.length; ++i) checkBounds(pnts[i][0],pnts[i][1]);
       if(g!=null) {
           GeneralPath gp=new GeneralPath();
           Point2D p=trans.transform(pnts[0][0],pnts[0][1]);
           gp.moveTo(p.getX(),p.getY());
           for(int i=1; i<pnts.length; ++i) {
               p=trans.transform(pnts[i][0],pnts[i][1]);
               gp.lineTo(p.getX(),p.getY());
           }
           gp.closePath();
           g.fill(gp);
       }
    }
    
    public void drawPoly(Object o) {
        double[][] pnts=ScriptUtil.objectToDouble2DArray(o);
        for(int i=0; i<pnts.length; ++i) checkBounds(pnts[i][0],pnts[i][1]);
        if(g!=null) {
            GeneralPath gp=new GeneralPath();
            Point2D p=trans.transform(pnts[0][0],pnts[0][1]);
            gp.moveTo(p.getX(),p.getY());
            for(int i=1; i<pnts.length; ++i) {
                p=trans.transform(pnts[i][0],pnts[i][1]);
                gp.lineTo(p.getX(),p.getY());
            }
            gp.closePath();
            g.draw(gp);
        }        
    }
    
    public void drawLine(double x1,double y1,double x2,double y2) {
        checkBounds(x1,y1);
        checkBounds(x2,y2);
        if(g!=null) {
            Point2D p1=trans.transform(x1,y1);
            Point2D p2=trans.transform(x2,y2);
            g.draw(new Line2D.Double(p1,p2));
        }
    }
    
    public void fillEllipse(double x,double y,double width,double height) {
        checkMinBounds(x,y);
        checkMaxBounds(x+width,y+height);
        if(g!=null) {
            Point2D p1=trans.transform(x,y);
            Point2D p2=trans.transform(x+width,y+height);
            if(p1.getX()<p2.getX()) {
                x=p1.getX();
                width=p2.getX()-p1.getX();
            } else {
                x=p2.getX();
                width=p1.getX()-p2.getX();
            }
            if(p1.getY()<p2.getY()) {
                y=p1.getY();
                height=p2.getY()-p1.getY();
            } else {
                y=p2.getY();
                height=p1.getY()-p2.getY();
            }
            g.fill(new Ellipse2D.Double(x,y,width,height));
        }
    }
    
    public void drawEllipse(double x,double y,double width,double height) {
        checkMinBounds(x,y);
        checkMaxBounds(x+width,y+height);
        if(g!=null) {
            Point2D p1=trans.transform(x,y);
            Point2D p2=trans.transform(x+width,y+height);
            if(p1.getX()<p2.getX()) {
                x=p1.getX();
                width=p2.getX()-p1.getX();
            } else {
                x=p2.getX();
                width=p1.getX()-p2.getX();
            }
            if(p1.getY()<p2.getY()) {
                y=p1.getY();
                height=p2.getY()-p1.getY();
            } else {
                y=p2.getY();
                height=p1.getY()-p2.getY();
            }
            g.draw(new Ellipse2D.Double(x,y,width,height));
        }
    }
    
    private void checkBounds(double x,double y) {
        if(x<bounds[0][0]) bounds[0][0]=x;
        if(x>bounds[0][1]) bounds[0][1]=x;
        if(y<bounds[1][0]) bounds[1][0]=y;
        if(y>bounds[1][1]) bounds[1][1]=y;
    }
    
    private void checkMinBounds(double x,double y) {
        if(x<bounds[0][0]) bounds[0][0]=x;
        if(y<bounds[1][0]) bounds[1][0]=y;
    }
    
    private void checkMaxBounds(double x,double y) {
        if(x>bounds[0][1]) bounds[0][1]=x;
        if(y>bounds[1][1]) bounds[1][1]=y;
    }
    
    private double[][] bounds={{1e100,-1e100},{1e100,-1e100}};
    private Graphics2D g;
    private PlotTransform trans;
}
