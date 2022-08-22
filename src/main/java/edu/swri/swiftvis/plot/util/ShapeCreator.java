/*
 * Created on Aug 30, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.swri.swiftvis.plot.util;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * @author Mark Lewis
 */
public interface ShapeCreator extends java.io.Serializable {
	Shape makeShape(double centerX,double centerY,double width,double height);
    
	public class EmptyCreator implements ShapeCreator {
		@Override
        public String toString() { return "No Symbol"; }
		@Override
        public Shape makeShape(double centerX,double centerY,double width,double height) {
			return null;
		}
		@Override
        public boolean equals(Object o) {
		    return o instanceof EmptyCreator;
		}
        private static final long serialVersionUID=9875609823765l;
	}
	
	public class CircleCreator implements ShapeCreator {
		@Override
        public String toString() { return "Circle"; }
		@Override
        public Shape makeShape(double centerX,double centerY,double width,double height) {
			return new Ellipse2D.Double(centerX-0.5*width,centerY-0.5*height,width,height);
		}
		@Override
        public boolean equals(Object o) {
		    return o instanceof CircleCreator;
		}
        private static final long serialVersionUID=1436987235098623l;
	}
	
	public class RectangleCreator implements ShapeCreator {
		@Override
        public String toString() { return "Rectangle"; }
		@Override
        public Shape makeShape(double centerX,double centerY,double width,double height) {
			return new Rectangle2D.Double(centerX-0.5*width,centerY-0.5*height,width,height);
		}
		@Override
        public boolean equals(Object o) {
		    return o instanceof RectangleCreator;
		}
        private static final long serialVersionUID=34609823740698l;
	}

	public class RoundRectangleCreator implements ShapeCreator {
		@Override
        public String toString() { return "Round Rectangle"; }
		@Override
        public Shape makeShape(double centerX,double centerY,double width,double height) {
			return new RoundRectangle2D.Double(centerX-0.5*width,centerY-0.5*height,width,height,width*0.2,height*0.2);
		}
		@Override
        public boolean equals(Object o) {
		    return o instanceof RoundRectangleCreator;
		}
        private static final long serialVersionUID=112498346834763l;
	}

    public class TriangleCreator implements ShapeCreator {
        @Override
        public String toString() { return "Triangle"; }
        @Override
        public Shape makeShape(double centerX,double centerY,double width,double height) {
            GeneralPath ret=new GeneralPath();
            ret.moveTo((float)(centerX),(float)(centerY+0.5*height));
            ret.lineTo((float)(centerX-0.5*width),(float)(centerY-0.5*height));
            ret.lineTo((float)(centerX+0.5*width),(float)(centerY-0.5*height));
            ret.closePath();
            return ret;
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof TriangleCreator;
        }
        private static final long serialVersionUID=94582343573245346l;
    }

    public class PlusCreator implements ShapeCreator {
        @Override
        public String toString() { return "Plus Sign"; }
        @Override
        public Shape makeShape(double centerX,double centerY,double width,double height) {
            GeneralPath ret=new GeneralPath();
//            BasicStroke stroke=new BasicStroke((float)(0.1*Math.min(width,height)));
//            ret.append(stroke.createStrokedShape(new Line2D.Double(centerX-0.5*width,centerY,centerX+0.5*width,centerY)),false);
//            ret.append(stroke.createStrokedShape(new Line2D.Double(centerX,centerY-0.5*height,centerX,centerY+0.5*height)),false);
            ret.moveTo((float)(centerX-0.05*width),(float)(centerY+0.5*height));
            ret.lineTo((float)(centerX+0.05*width),(float)(centerY+0.5*height));
            ret.lineTo((float)(centerX+0.05*width),(float)(centerY+0.05*height));
            ret.lineTo((float)(centerX+0.5*width),(float)(centerY+0.05*height));
            ret.lineTo((float)(centerX+0.5*width),(float)(centerY-0.05*height));
            ret.lineTo((float)(centerX+0.05*width),(float)(centerY-0.05*height));
            ret.lineTo((float)(centerX+0.05*width),(float)(centerY-0.5*height));
            ret.lineTo((float)(centerX-0.05*width),(float)(centerY-0.5*height));
            ret.lineTo((float)(centerX-0.05*width),(float)(centerY-0.05*height));
            ret.lineTo((float)(centerX-0.5*width),(float)(centerY-0.05*height));
            ret.lineTo((float)(centerX-0.5*width),(float)(centerY+0.05*height));
            ret.lineTo((float)(centerX-0.05*width),(float)(centerY+0.05*height));
            ret.closePath();
            return ret;
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof PlusCreator;
        }
        private static final long serialVersionUID=23546346666865798l;
    }
}
