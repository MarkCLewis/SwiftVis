/*
 * Created on Feb 25, 2004
 */
package edu.swri.swiftvis.plot.p3d;

import java.io.Serializable;

/**
 * This is an immutable class that represents a point in 3-space.
 * @author Mark Lewis
 */
public final class Vect3D implements Serializable {
    public Vect3D(double px,double py,double pz) {
		p[0]=px;
		p[1]=py;
		p[2]=pz;
	}
	public Vect3D(double[] pnt) {
		p[0]=pnt[0];
		p[1]=pnt[1];
		p[2]=pnt[2];
	}
	public double getX() { return p[0]; }
	public double getY() { return p[1]; }
	public double getZ() { return p[2]; }
	public double get(int dim) { return p[dim]; }
	public double[] get() {
		double[] ret=new double[3];
		ret[0]=p[0];
		ret[1]=p[1];
		ret[2]=p[2];
		return ret;
	}
	@Override
    public String toString() {
		return p[0]+" "+p[1]+" "+p[2];
	}
	public Vect3D minus(Vect3D v) {
		return new Vect3D(p[0]-v.p[0],p[1]-v.p[1],p[2]-v.p[2]);
	}
	public Vect3D plus(Vect3D v) {
		return new Vect3D(p[0]+v.p[0],p[1]+v.p[1],p[2]+v.p[2]);
	}
	public Vect3D mult(double scale) {
		return new Vect3D(scale*p[0],scale*p[1],scale*p[2]);
	}
    public Vect3D normalize() {
        double len=Math.sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2]);
        if(len==1 || len==0) return this;
        return this.mult(1/len);
    }
	private double[] p=new double[3];
    private static final long serialVersionUID = -6133985781664096275L;
}
