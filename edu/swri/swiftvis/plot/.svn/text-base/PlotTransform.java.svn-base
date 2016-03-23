/*
 * Created on May 12, 2006
 */
package edu.swri.swiftvis.plot;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is used to do the transformations for drawing to plots.  The idea is to
 * hide the actual size and bounds of the plot from the plotting styles without giving
 * them full knowledge of the actual coordinates. 
 * 
 * @author Mark Lewis
 */
public class PlotTransform {
    public PlotTransform(AxisOptions xAxis,AxisOptions yAxis,Rectangle2D cell) {
        logX=xAxis.isLogScale();
        logY=yAxis.isLogScale();
        if(logX) {
            primaryMin=Math.log10(xAxis.getMin());
            primaryWidth=Math.log10(xAxis.getMax())-Math.log10(xAxis.getMin());
        } else {
            primaryMin=xAxis.getMin();
            primaryWidth=xAxis.getMax()-xAxis.getMin();
        }
        if(logY) {
            secondaryMin=Math.log10(yAxis.getMin());
            secondaryWidth=Math.log10(yAxis.getMax())-Math.log10(yAxis.getMin());
        } else {
            secondaryMin=yAxis.getMin();
            secondaryWidth=yAxis.getMax()-yAxis.getMin();
        }
        plotMinX=cell.getMinX();
        plotWidthX=cell.getWidth();
        plotMinY=cell.getMinY();
        plotWidthY=cell.getHeight();
    }
    
    public PlotTransform(AxisOptions axis,double start,double end) {
        logX=axis.isLogScale();
        logY=axis.isLogScale();
        if(logX) {
            primaryMin=Math.log10(axis.getMin());
            primaryWidth=Math.log10(axis.getMax())-Math.log10(axis.getMin());
        } else {
            primaryMin=axis.getMin();
            primaryWidth=axis.getMax()-axis.getMin();
        }
        if(logY) {
            secondaryMin=Math.log10(axis.getMin());
            secondaryWidth=Math.log10(axis.getMax())-Math.log10(axis.getMin());
        } else {
            secondaryMin=axis.getMin();
            secondaryWidth=axis.getMax()-axis.getMin();
        }
        plotMinX=start;
        plotWidthX=end-start;
        plotMinY=start;
        plotWidthY=end-start;        
    }
    
    public Point2D transform(Point2D p) {
        return transform(p.getX(),p.getY());
    }
    
    public Point2D transform(double x,double y) {
        return new Point2D.Double(transformX(x),transformY(y));
    }
    
    public double fractionalPrimary(double width) {
        return width*plotWidthX;
    }
    
    public double fractionalSecondary(double height) {
        return height*plotWidthY;
    }
    
    public double scaledPrimary(double width) {
        return width*plotWidthX/primaryWidth;
    }

    public double scaledSecondary(double height) {
        return height*plotWidthY/secondaryWidth;
    }
    
    public Point2D addPixelOffset(Point2D p,double dx,double dy) {
        return new Point2D.Double(p.getX()+dx,p.getY()+dy);        
    }
    
    /**
     * This method takes a click location and converts it to plot coordinates.
     * @param cx Click x offset from the left of the plot.
     * @param cy Click t offset from the top of the plot.
     * @return The point that click represents in plot coordinates.
     */
    public Point2D inverseTransform(double cx,double cy) {
        double x;
        if(logX) x=Math.pow(10,primaryMin+cx/plotWidthX*primaryWidth);
        else x=primaryMin+cx/plotWidthX*primaryWidth;
        
        double y;
        if(logY) y=Math.pow(10,secondaryMin+cy/plotWidthY*secondaryWidth);
        else y=secondaryMin+cy/plotWidthY*secondaryWidth;
        
        return new Point2D.Double(x,y);
    }
    
    public double transformX(double x) {
        if(logX) return linearTransformX(Math.log10(x));
        else return linearTransformX(x); 
    }
    
    public double transformY(double y) {
        if(logY) return linearTransformY(Math.log10(y));
        else return linearTransformY(y); 
    }
    
    private double linearTransformX(double x) {
        return plotMinX+plotWidthX*(x-primaryMin)/primaryWidth;
    }
    
    private double linearTransformY(double y) {
        return plotMinY+plotWidthY-plotWidthY*(y-secondaryMin)/secondaryWidth;
    }
    
    private final boolean logX,logY;
    private final double primaryMin,primaryWidth;
    private final double secondaryMin,secondaryWidth;
    private final double plotMinX,plotWidthX;
    private final double plotMinY,plotWidthY;
}
