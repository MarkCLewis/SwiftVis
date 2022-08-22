package edu.swri.swiftvis.plot.p3d.cpu;

// JGText.java

import java.awt.*;

public class JGText extends JGObject {
	private JGText() {
	}

	public JGText(double x,double y,double z,String s,Color c) {
		p1=new JGVector(x,y,z,1.0);
		str=s;
		col=c;
	}

    @Override
    public JGObject applyTransform(JGTransform t) {
        JGText ret;

        ret=new JGText();
        ret.p1=p1.multiply(t);
		ret.col=col;
		ret.str=str;

        return(ret);
    }

    @Override
    public void draw(Graphics2D g) {
        Rectangle r=g.getClip().getBounds();
        int x1,y1;

//		if(projection==0) {
	        x1=r.x+(int)(r.width*(p1.element(0)/p1.element(3)+1.0)/2.0);
		    y1=r.y+(int)(r.height*(p1.element(1)/p1.element(3)+1.0)/2.0);
//		} else {
//	        x1=r.x+(int)(r.width*(p1.Element(0)/(p1.Element(2)*p1.Element(3))+1.0)/2.0);
//		    y1=r.y+(int)(r.height*(p1.Element(1)/(p1.Element(2)*p1.Element(3))+1.0)/2.0);
//		}
        g.setColor(col);
        g.drawString(str,x1,y1);
    }

    @Override
	public boolean drawable() {
		return(true);
	}

    @Override
	public boolean cull() {
		if(p1.element(2)<0.0) return(false);
		return(true);
	}

    @Override
	public JGBoundingBox getBoundingBox() {
		JGBoundingBox ret=new JGBoundingBox();

		ret.setFront(p1.element(2));
		ret.setBack(p1.element(2));
		ret.setLeft(p1.element(0));
		ret.setRight(p1.element(0));
		ret.setTop(p1.element(1));
		ret.setBottom(p1.element(1));

		return(ret);
	}

    @Override
	public JGVector getPoint(int i) {
		if(i==0) return(p1);
		throw new RuntimeException("Asked for point "+i+" from text.");
	}
    private JGVector p1;
	private String str;
    private Color col;
}
