package edu.swri.swiftvis.plot.p3d.cpu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

// JGPolygon.java


public class JGPolygon extends JGObject {
	public JGPolygon(int np,boolean f,Color c) {
		int i;

		p=new JGVector[np];
		fill=f;
		col=c;
		for(i=0; i<np; i++) p[i]=null;
	}

	public void setPoint(int n,JGVector v) {
		int i;

		if((n<0) || (n>=p.length)) return;
		p[n]=new JGVector(v);
		allSet=true;
		for(i=0; (i<p.length) && allSet; i++) {
			if(p[i]==null) allSet=false;
		}
		if(allSet) autoSetNorm();
	}

	public void setPoint(int n,double nx,double ny,double nz) {
		int i;

		if((n<0) || (n>=p.length)) return;
		p[n]=new JGVector(nx,ny,nz,1.0);
		allSet=true;
		for(i=0; (i<p.length) && allSet; i++) {
			if(p[i]==null) allSet=false;
		}
		if(allSet) autoSetNorm();
	}

	// This only works consistently for convex polygons.  Returns false
	// if not all points have been set.  It assumes that the points are in
	// in a clockwise direction.
	private boolean autoSetNorm() {
		JGVector v1,v2;

		if((!allSet) || (p.length<3)) return false;
		v1=p[1].add(p[0].scale(-1.0));
		v2=p[2].add(p[1].scale(-1.0));
		norm=v1.cross(v2);
		norm.normalize();

		return true;
	}

	public void setFill(boolean f) {
		fill=f;
	}

    @Override
	public JGObject applyTransform(JGTransform t) {
		JGPolygon ret=new JGPolygon(p.length,fill,col);
		int i;

		if(!allSet) return(null);
		for(i=0; i<p.length; i++) {
			ret.p[i]=p[i].multiply(t);
			ret.p[i].normalize();
		}
		ret.fill=fill;
		ret.allSet=allSet;
		ret.autoSetNorm();
        ret.drawCol=drawCol;

		return ret;
	}

    @Override
	public void draw(Graphics2D g) {
		Rectangle r=g.getClip().getBounds();
		int x[],y[];
		int i;

		if(!allSet) return;
		x=new int[p.length];
		y=new int[p.length];
		for(i=0; i<p.length; i++) {
			x[i]=r.x+(int)(r.width*(p[i].element(0)/p[i].element(3)+1.0)/2.0);
			y[i]=r.y+(int)(r.height*(p[i].element(1)/p[i].element(3)+1.0)/2.0);
		}
		g.setColor(drawCol);
		if(fill) {
			g.fillPolygon(x,y,p.length);
		} else {
			g.drawPolygon(x,y,p.length);
		}
	}

    @Override
	public boolean drawable() {
		return true;
	}
    
    @Override
    public void setDrawColor(JGLighting lighting) {
        drawCol=lighting.getLightColor(p[0],norm,col);
    }

    @Override
	public boolean cull() {
		int i;
		double tmp;
		boolean ret=false;

		if(!allSet) return false ;
		// backface cull
		if((norm==null) || (norm.element(2)>0.0)) return false;
		// Cull vs. z=0.0
		for(i=0; i<p.length; i++) {
			if((p[i].element(2)<=0.0) && (p[(i+1)%p.length].element(2)<=0.0)) {
				p[i].setElement(2,0.0);
			} else if(p[i].element(2)<0.0) {
				ret=true;
				tmp=p[i].element(0)+(p[(i+1)%p.length].element(0)-p[i].element(0))*p[i].element(2)/(p[i].element(2)-p[(i+1)%p.length].element(2));
				p[i].setElement(1,p[i].element(1)+(p[(i+1)%p.length].element(1)-p[i].element(1))*p[i].element(2)/(p[i].element(2)-p[(i+1)%p.length].element(2)));
				p[i].setElement(0,tmp);
				p[i].setElement(2,0.0);
			} else ret=true;
		}
		return ret;
	}

    @Override
	public JGBoundingBox getBoundingBox() {
		JGBoundingBox ret=new JGBoundingBox();
		int i;

		if((p.length==0) || (!allSet)) return(null);
		ret.setFront(p[0].element(2));
		ret.setBack(p[0].element(2));
		ret.setLeft(p[0].element(0));
		ret.setRight(p[0].element(0));
		ret.setTop(p[0].element(1));
		ret.setBottom(p[0].element(1));
		for(i=1; i<p.length; i++) {
			ret.setFront(Math.min(p[i].element(2),ret.getFront()));
			ret.setBack(Math.max(p[i].element(2),ret.getBack()));
			ret.setLeft(Math.min(p[i].element(0),ret.getLeft()));
			ret.setRight(Math.max(p[i].element(0),ret.getRight()));
			ret.setTop(Math.max(p[i].element(1),ret.getTop()));
			ret.setBottom(Math.min(p[i].element(1),ret.getBottom()));
		}

		return ret;
	}

    @Override
	public JGPlane getPlane() {
		JGPlane ret=new JGPlane(p[0],norm);
		return ret;
	}

    @Override
	public JGVector getPoint(int i) {
		if((i>=0) && (i<p.length)) return(p[i]);
		throw new RuntimeException("Asked for point "+i+" in a polygon.");
	}
    
    @Override
    public int getNumPoints() {
        return p.length;
    }

    @Override
	public List<JGObject> splitByPlane(JGPlane pl) {
		List<JGObject> ret=null;
		int cnt[]={0,0},side=0,inter_cnt=0;
		JGIntersectionList list=null;
		JGVector ipoint;
		int i;
		JGPolygon poly[];

		// make list of points of intersecion
		for(i=0; i<p.length; i++) {
			ipoint=pl.intersectWithSegment(p[i],p[(i+1)%p.length]);
			cnt[side]++;
			if(ipoint!=null) {
				list=new JGIntersectionList(i,ipoint,list);
				side=(side+1)%2;
				inter_cnt++;
			}
		}
//		System.out.println("split: "+cnt[0]+" "+cnt[1]+" "+inter_cnt);
		if(inter_cnt==0) return(null);

		// walk around polygons to break it into two if there are any intersections.
		poly=new JGPolygon[2];
		poly[0]=new JGPolygon(cnt[0]+inter_cnt,fill,col);
		poly[1]=new JGPolygon(cnt[1]+inter_cnt,fill,col);
		side=0;
		cnt[0]=0;
		cnt[1]=0;
		for(i=p.length-1; i>=0; i--) {
			if((list!=null) && (list.vert==i)) {
				poly[side].setPoint(cnt[side],list.point);
				cnt[side]++;
				side=(side+1)%2;
				poly[side].setPoint(cnt[side],list.point);
				cnt[side]++;
				list=list.next;
			}
			poly[side].setPoint(cnt[side],p[i]);
			cnt[side]++;
		}
		ret=new ArrayList<JGObject>();
		ret.add(poly[0]);
		ret.add(poly[1]);
		return(ret);
	}

    @Override
	public String toString() {
		return "Polygon with "+p.length+" sides";
	}

	private JGVector p[];
	private JGVector norm;
	private Color col;
	private boolean fill;
	private boolean allSet;
	private Color drawCol;

    private static class JGIntersectionList {
        public JGIntersectionList(int v,JGVector p,JGIntersectionList n) {
            vert=v;
            point=p;
            next=n;
        }

        public int vert;
        public JGVector point;
        public JGIntersectionList next;
    }
}

