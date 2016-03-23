package edu.swri.swiftvis.plot.p3d.cpu;

// JGLine.java

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class JGLine extends JGObject {
	private JGLine() {
	}

	public JGLine(double x1,double y1,double z1,double x2,double y2,double z2,Color c) {
		p1=new JGVector(x1,y1,z1,1.0);
		p2=new JGVector(x2,y2,z2,1.0);
		col=c;
	}

	public JGLine(JGVector ip1,JGVector ip2,Color c) {
		p1=ip1;
		p2=ip2;
		col=c;
	}

    @Override
	public JGObject applyTransform(JGTransform t) {
		JGLine ret;

		ret=new JGLine();
		ret.p1=p1.multiply(t);
		ret.p2=p2.multiply(t);
		ret.col=col;

		return(ret);
	}

    @Override
	public void draw(Graphics2D g) {
		Rectangle r=g.getClip().getBounds();
		int x1,y1,x2,y2;

//		if(projection==0) {
			x1=r.x+(int)(r.width*(p1.element(0)/p1.element(3)+1.0)/2.0);
			x2=r.x+(int)(r.width*(p2.element(0)/p2.element(3)+1.0)/2.0);
			y1=r.y+(int)(r.height*(p1.element(1)/p1.element(3)+1.0)/2.0);
			y2=r.y+(int)(r.height*(p2.element(1)/p2.element(3)+1.0)/2.0);
//		} else {
//			x1=r.x+(int)(r.width*(p1.Element(0)/(p1.Element(2)*p1.Element(3))+1.0)/2.0);
//			x2=r.x+(int)(r.width*(p2.Element(0)/(p2.Element(2)*p2.Element(3))+1.0)/2.0);
//			y1=r.y+(int)(r.height*(p1.Element(1)/(p1.Element(2)*p1.Element(3))+1.0)/2.0);
//			y2=r.y+(int)(r.height*(p2.Element(1)/(p2.Element(2)*p2.Element(3))+1.0)/2.0);
//		}
		g.setColor(col);
		g.drawLine(x1,y1,x2,y2);
	}

    @Override
	public boolean drawable() {
		return(true);
	}

    @Override
	public boolean cull() {
		double tmp;

		// Cull vs. z=0.0
		if((p1.element(2)<0.0) && (p2.element(2)<0.0)) return(false);
		if(p1.element(2)<0.0) {
			tmp=p1.element(0)+(p2.element(0)-p1.element(0))*p1.element(2)/(p1.element(2)-p2.element(2));
			p1.setElement(1,p1.element(1)+(p2.element(1)-p1.element(1))*p1.element(2)/(p1.element(2)-p2.element(2)));
			p1.setElement(0,tmp);
			p1.setElement(2,0.0);
		} else if(p2.element(2)<0.0) {
			tmp=p2.element(0)+(p1.element(0)-p2.element(0))*p2.element(2)/(p2.element(2)-p1.element(2));
			p2.setElement(1,p2.element(1)+(p1.element(1)-p2.element(1))*p2.element(2)/(p2.element(2)-p1.element(2)));
			p2.setElement(0,tmp);
			p2.setElement(2,0.0);
		}
		return(true);
	}

    @Override
	public JGBoundingBox getBoundingBox() {
		JGBoundingBox ret=new JGBoundingBox();

		ret.setFront(Math.min(p1.element(2),p2.element(2)));
		ret.setBack(Math.max(p1.element(2),p2.element(2)));
		ret.setLeft(Math.min(p1.element(0),p2.element(0)));
		ret.setRight(Math.max(p1.element(0),p2.element(0)));
		ret.setTop(Math.max(p1.element(1),p2.element(1)));
		ret.setBottom(Math.min(p1.element(1),p2.element(1)));

		return(ret);
	}

    @Override
	public JGVector getPoint(int i) {
		if(i==0) return(p1);
		if(i==1) return(p2);
		throw new RuntimeException("Asked for point "+i+" in a line.");
	}
    
    @Override
    public int getNumPoints() {
        return 2;
    }

    @Override
	public List<JGObject> splitByPlane(JGPlane pl) {
		List<JGObject> ret=null;
		JGVector split_point;
		
		split_point=pl.intersectWithSegment(p1,p2);
		if(split_point!=null) {
			ret=new ArrayList<JGObject>();
			ret.add(new JGLine(p1,split_point,col));
			ret.add(new JGLine(p2,split_point,col));
		}
		return(ret);
	}

    @Override
	public String toString() {
		return("Line from "+p1.toString()+" to "+p2.toString());
	}

	private JGVector p1,p2;
	private Color col;
}
