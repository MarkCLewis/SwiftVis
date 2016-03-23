/*
 * Created on Dec 28, 2003
 */
package edu.swri.swiftvis.plot.p3d.raytrace;

import java.awt.Color;

import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.Vect3D;


/**
 * As the name implies, this class represents a 3-D sphere.  It has a number of functions
 * to help with things like detecting overlaps or intersections.
 * @author Mark Lewis
 */
public class RTSphere implements RTGeometry {
	public RTSphere(double x,double y,double z,double r) {
		this(new Vect3D(x,y,z),r);
	}
	public RTSphere(double[] cen,double r) {
		this(new Vect3D(cen[0],cen[1],cen[2]),r);
	}
	public RTSphere(Vect3D cen,double r) {
        c=cen;
        radius=r;
	}
	public double getX() {
		return c.get(0);
	}
	public double getY() {
		return c.get(1);
	}
	public double getZ() {
		return c.get(2);
	}

    public double getRadius() {
        return radius;
    }

    public Vect3D getCenter() {
        return c;
    }

	
	public boolean contains(double[] p) {
		return Basic.distanceSqr(p,c)<=radius*radius;
	}
	
	public boolean contains(Vect3D p) {
		return Basic.distanceSqr(p,c)<=radius*radius;
	}
	
	public double distanceInside(double[] p) {
		double ret=Basic.distance(p,c);
		if(ret<=radius) return radius-ret;
		return -1;
	}
	
	public double distanceInside(Vect3D p) {
		double ret=Basic.distance(p,c);
		if(ret<=radius) return radius-ret;
		return -1;
	}
	
	@Override
    public RTSphere getBoundingSphere() {
		return this;
	}
	
	@Override
    public double[] calcRayIntersect(Ray r) {
		double dc;
		double dr;
		double a=0.0,b=0.0,c2=-radius*radius;
		int i;

		for(i=0; i<3; i++) {
			dc=c.get(i)-r.start(i);
			dr=r.one(i)-r.start(i);
			a+=dr*dr;
			b+=dc*dr;
			c2+=dc*dc;
		}
		b*=-2.0;
		double rootTerm=b*b-4.0*a*c2;
		if(rootTerm<0.0) return new double[0];
		rootTerm=Math.sqrt(rootTerm);
		double[] ret=new double[2];
		ret[0]=(-b-rootTerm)/(2.0*a);
		ret[1]=(-b+rootTerm)/(2.0*a);
		if(ret[0]<0.0 && ret[1]<0.0) return new double[0];
		return ret;
	}
	
	@Override
    public double[] getNormal(double[] location) {
		double[] ret={location[0]-c.get(0),location[1]-c.get(1),location[2]-c.get(2)};
		Basic.normalize(ret);
		return ret;
	}
	
	@Override
    public Vect3D getNormal(Vect3D location) {
		double[] ret={location.get(0)-c.get(0),location.get(1)-c.get(1),location.get(2)-c.get(2)};
		Basic.normalize(ret);
		return new Vect3D(ret);
	}

	@Override
    public String toString() {
		return "Sphere at "+c+" r="+radius;
	}

    @Override
    public DrawAttributes getAttributes(double[] location) {
        return attr;
    }

    public void setAttributes(DrawAttributes da) {
        attr = da;
    }


	/**
	 * This method is intended to create a "mutual bounding sphere".  Given the two
	 * sphere it makes this new on large enough to contain both.
	 * @param s1 A sphere to be contianed in this new one.
	 * @param s2 Another sphere to be contained in this new one.
	 */
	public static RTSphere mutualBoundingSphere(RTSphere s1,RTSphere s2) {
		if(s1.radius<s2.radius) {
			RTSphere tmp=s1;
			s1=s2;
			s2=tmp;
		}
		double dist=Basic.distance(s1.c,s2.c);
		if(s1.radius>=s2.radius+dist) {
			return s1;
		}
		double c0=s1.c.get(0);
		double c1=s1.c.get(1);
		double c2=s1.c.get(2);
		double tsep=dist+s1.radius+s2.radius;
		double radius=0.5*tsep;
		double moveFact=(radius-s1.radius)/dist;
		c0+=moveFact*(s2.c.get(0)-c0);
		c1+=moveFact*(s2.c.get(1)-c1);
		c2+=moveFact*(s2.c.get(2)-c2);
		return new RTSphere(c0,c1,c2,radius);
	}

    private final Vect3D c;
    private final double radius;

    private DrawAttributes attr = DefaultAttrs;
    private static final DrawAttributes DefaultAttrs = new DrawAttributes(Color.lightGray, 0.0, 1.0); 
}
