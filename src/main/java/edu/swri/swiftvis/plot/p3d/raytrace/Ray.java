/*
 * Created on Dec 27, 2003
 */
package edu.swri.swiftvis.plot.p3d.raytrace;

import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.Vect3D;

/**
 * This interface is used to represent a ray passing through the world.
 * @author Mark Lewis
 */
public final class Ray {
	public static final double NO_INTERSECT=-1e32;
	public Ray(double[] start,double[] one) {
		s[0]=start[0];
		s[1]=start[1];
		s[2]=start[2];
		o[0]=one[0];
		o[1]=one[1];
		o[2]=one[2];
	}
    public Ray(Vect3D start,Vect3D one) {
        s=start.get();
        o=one.get();
    }
	public double startX() {
		return s[0];
	}
	public double startY() {
		return s[1];
	}
	public double startZ() {
		return s[2];
	}
	public double start(int dim) {
		return s[dim];
	}
	public double oneX() {
		return o[0];
	}
	public double oneY() {
		return o[1];
	}
	public double oneZ() {
		return o[2];
	}
	public double one(int dim) {
		return o[dim];
	}
	
	public double pointX(double t) {
		return s[0]+t*(o[0]-s[0]);
	}
	public double pointY(double t) {
		return s[1]+t*(o[1]-s[1]);
	}
	public double pointZ(double t) {
		return s[2]+t*(o[2]-s[2]);
	}
	public double point(int dim,double t) {
		return s[dim]+t*(o[dim]-s[dim]);
	}
	public double[] point(double t) {
		double[] ret={pointX(t),pointY(t),pointZ(t)};
		return ret;
	}
	
	public double[] getDirectionVector() {
		double[] ret={o[0]-s[0],o[1]-s[1],o[2]-s[2]};
		return ret;
	}
	
	public double dotProduct(Ray r) {
	    return (s[0]-o[0])*(r.s[0]-r.o[0])+(s[1]-o[1])*(r.s[1]-r.o[1])+(s[2]-o[2])*(r.s[2]-r.o[2]);
	}
	
	public double mainLength() {
	    return Basic.distance(s,o);
	}
	
	@Override
    public String toString() {
	    return s[0]+" "+s[1]+" "+s[2]+" to "+o[0]+" "+o[1]+" "+o[2];
	}
	
	private double[] s=new double[3];
	private double[] o=new double[3];
}
