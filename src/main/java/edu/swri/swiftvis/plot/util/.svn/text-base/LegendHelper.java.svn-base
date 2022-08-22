/*
 * Created on Jul 2, 2006
 */
package edu.swri.swiftvis.plot.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.plot.AxisOptions;

public class LegendHelper {
    public static void drawTextSymbolAndGradient(Graphics2D g,Rectangle2D bounds,FormattedString legendName,
            DataSink sink,Paint symbolColor,ShapeCreator symbolShape,Stroke stroke,AxisOptions axisOptions,
            ColorGradient colorGradients,boolean drawVertical) {
        Shape clipShape=g.getClip();
        g.setClip(bounds);
        double textFractionHeight;
        if(drawVertical) {
            textFractionHeight=0.15;
        } else {
            textFractionHeight=0.2;                
        }
        drawTextAndSymbol(g,new Rectangle2D.Double(bounds.getMinX(),bounds.getMinY(),bounds.getWidth(),bounds.getHeight()*textFractionHeight),legendName,sink,axisOptions.getFont(),symbolColor,symbolShape,stroke);
        drawGradient(g,new Rectangle2D.Double(bounds.getMinX(),bounds.getMinY()+textFractionHeight*bounds.getHeight(),bounds.getWidth(),bounds.getHeight()*(1-textFractionHeight)),axisOptions,colorGradients,drawVertical);
        g.setClip(clipShape);
    }
    
    public static void drawTextAndGradient(Graphics2D g,Rectangle2D bounds,FormattedString legendName,
            DataSink sink,AxisOptions axisOptions,ColorGradient colorGradients,boolean drawVertical) {
        Shape clipShape=g.getClip();
        g.setClip(bounds);
        double textFraction;
        if(legendName.getValue().length()>0) {
            if(drawVertical) {
                textFraction=0.15;
            } else {
                textFraction=0.2;                
            }
            drawText(g,new Rectangle2D.Double(bounds.getMinX(),bounds.getMinY(),bounds.getWidth(),bounds.getHeight()*textFraction),legendName,sink,axisOptions.getFont());
        } else {
            textFraction=0.0;            
        }
        drawGradient(g,new Rectangle2D.Double(bounds.getMinX(),bounds.getMinY()+textFraction*bounds.getHeight(),bounds.getWidth(),bounds.getHeight()*(1-textFraction)),axisOptions,colorGradients,drawVertical);
        g.setClip(clipShape);
    }
    
    public static void drawTextAndSymbol(Graphics2D g,Rectangle2D bounds,FormattedString legendName,
            DataSink sink,Font font,Paint symbolColor,ShapeCreator symbolShape,Stroke stroke) {
        Shape clipShape=g.getClip();
        g.setClip(bounds);
        if(legendName.getValue().length()>0) {
            drawSymbol(g,new Rectangle2D.Double(bounds.getMinX(),bounds.getMinY(),0.3*bounds.getWidth(),bounds.getHeight()),symbolColor,symbolShape,stroke);
            drawText(g,new Rectangle2D.Double(bounds.getMinX()+0.3*bounds.getWidth(),bounds.getMinY(),0.7*bounds.getWidth(),bounds.getHeight()),legendName,sink,font);
        } else {
            drawSymbol(g,bounds,symbolColor,symbolShape,stroke);            
        }
        g.setClip(clipShape);
    }
    
    public static void drawSymbol(Graphics2D g,Rectangle2D bounds,Paint symbolColor,
            ShapeCreator symbolShape,Stroke stroke) {
        Shape clipShape=g.getClip();
        g.setClip(bounds);
        g.setPaint(symbolColor);
        if(stroke!=null) {
            g.setStroke(stroke);
            g.draw(new Line2D.Double(bounds.getMinX()+1,bounds.getCenterY(),bounds.getMaxX()-1,bounds.getCenterY()));
        }
        double size=0.25*Math.min(bounds.getWidth(),bounds.getHeight());
        g.fill(symbolShape.makeShape(bounds.getCenterX(),bounds.getCenterY(),size,size));
        g.setClip(clipShape);
    }
    
    public static void drawText(Graphics2D g,Rectangle2D bounds,FormattedString legendName,
            DataSink sink,Font font) {
        Shape clipShape=g.getClip();
        g.setClip(bounds);
        g.setPaint(Color.black);
        Rectangle2D stringBounds=legendName.getBounds(font,sink);
        double widthFactor=stringBounds.getWidth()/bounds.getWidth();
        double heightFactor=stringBounds.getHeight()/bounds.getHeight();
        double maxFactor=Math.max(widthFactor,heightFactor);
        if(maxFactor>0.96 || maxFactor<0.94) {
            double fontSize=0.95*font.getSize2D()/maxFactor;
            font=font.deriveFont((float)fontSize);
        }
        stringBounds=legendName.getBounds(font,sink);
        g.setFont(font);
        legendName.draw(g,(float)(bounds.getCenterX()-0.5*stringBounds.getWidth()),(float)(bounds.getCenterY()-stringBounds.getMinY()-0.5*stringBounds.getHeight()),sink);
        g.setClip(clipShape);
    }
    
    public static void drawGradient(Graphics2D g,Rectangle2D bounds,AxisOptions axisOptions,
            ColorGradient colorGradients,boolean drawVertical) {
        Shape clipShape=g.getClip();
        g.setClip(bounds);
        AxisOptions.AxisBounds axisBounds=axisOptions.getAxisBounds(1);
        if(drawVertical) {
            float scale=(float)((bounds.getWidth()*0.5-2)/axisBounds.getOutside());
            axisBounds=axisOptions.getAxisBounds(scale);
            double ymin=bounds.getMinY()+axisBounds.getHeight()*0.5;
            double ymax=bounds.getMaxY()-axisBounds.getHeight()*0.5;
            if(axisOptions.getAxisLocation()==AxisOptions.MIN_SIDE) {
                bounds=new Rectangle2D.Double(bounds.getCenterX(),ymin,bounds.getWidth()*0.5,ymax-ymin);
            } else {
                bounds=new Rectangle2D.Double(bounds.getMinX(),ymin,bounds.getWidth()*0.5,ymax-ymin);
            }
            // Draw gradient
            double[] gradBounds=colorGradients.getBounds();
            for(double i=bounds.getMinY(); i<=bounds.getMaxY(); ++i) {
                g.setPaint(colorGradients.getColor(gradBounds[0]+(bounds.getMaxY()-i)*(gradBounds[1]-gradBounds[0])/bounds.getHeight()));
                g.fill(new Rectangle2D.Double(bounds.getMinX(),i,bounds.getWidth(),2));
            }
            // Draw bounds labels
            g.setPaint(Color.black);
            axisOptions.drawAxis(g,bounds,axisOptions.getAxisBounds(scale),AxisOptions.VERTICAL_AXIS,0,1,true,scale);
            axisOptions.drawAxisLabel(g,bounds,axisOptions.getAxisBounds(scale),AxisOptions.VERTICAL_AXIS,scale);            
        } else {
            float scale=(float)(bounds.getHeight()*0.5/axisBounds.getOutside());
            axisBounds=axisOptions.getAxisBounds(scale);
            double xmin=bounds.getMinX()+axisBounds.getHeight()*0.5;
            double xmax=bounds.getMaxX()-axisBounds.getHeight()*0.5;
            if(axisOptions.getAxisLocation()==AxisOptions.MIN_SIDE) {
                bounds=new Rectangle2D.Double(xmin,bounds.getMinY(),xmax-xmin,bounds.getHeight()*0.5);
            } else {
                bounds=new Rectangle2D.Double(xmin,bounds.getCenterY(),xmax-xmin,bounds.getHeight()*0.5);
            }
            // Draw gradient
            double[] gradBounds=colorGradients.getBounds();
            for(double i=bounds.getMinX(); i<=bounds.getMaxX(); ++i) {
                g.setPaint(colorGradients.getColor(gradBounds[0]+(i-bounds.getMinX())*(gradBounds[1]-gradBounds[0])/bounds.getWidth()));
                g.fill(new Rectangle2D.Double(i,bounds.getMinY(),2,bounds.getHeight()));
            }
            // Draw bounds labels
            g.setPaint(Color.black);
            axisOptions.drawAxis(g,bounds,axisOptions.getAxisBounds(scale),AxisOptions.HORIZONTAL_AXIS,0,1,true,scale);
            axisOptions.drawAxisLabel(g,bounds,axisOptions.getAxisBounds(scale),AxisOptions.HORIZONTAL_AXIS,scale);
        }
        g.setClip(clipShape);
    }    
}
