/*
 * Created on Jan 15, 2004
 */
package edu.swri.swiftvis.plot.p3d;

import edu.swri.swiftvis.plot.p3d.raytrace.RTTriangle;

/**
 * This class is a utility class that contains some basic methods that can be helpful
 * when doing things with the geometry in 3-space.
 * @author Mark Lewis
 */
public final class Basic {
	public static void main(String[] args) {
		Vect3D p1=new Vect3D(-1,0,0);
		Vect3D p2=new Vect3D(1,0,0);
		Vect3D r1=new Vect3D(0.5,0.5,0);
		Vect3D r2=new Vect3D(0.0,0.0,0);
		double[] params=twoSegmentClosestParams(p1,p2,r1,r2);
		System.out.println(params[0]+" "+params[1]);
	}
	
	public static double distance(double[] p1,double[] p2) {
		return Math.sqrt(distanceSqr(p1,p2));
	}

	public static double distance(Vect3D p1,Vect3D p2) {
		return Math.sqrt(distanceSqr(p1,p2));
	}

	public static double distance(double[] p1,Vect3D p2) {
		return Math.sqrt(distanceSqr(p1,p2));
	}

	public static double distanceSqr(double[] p1,double[] p2) {
		double ret=0.0;
		for(int i=0; i<p1.length; ++i) {
			double dif=p1[i]-p2[i];
			ret+=dif*dif;
		}
		return ret;
	}
	
	public static double distanceSqr(Vect3D p1,Vect3D p2) {
		double ret=0.0;
		for(int i=0; i<3; ++i) {
			double dif=p1.get(i)-p2.get(i);
			ret+=dif*dif;
		}
		return ret;
	}
	
	public static double distanceSqr(double[] p1,Vect3D p2) {
		double ret=0.0;
		for(int i=0; i<3; ++i) {
			double dif=p1[i]-p2.get(i);
			ret+=dif*dif;
		}
		return ret;
	}
	
	public static double magnitude(double[] p) {
		return Math.sqrt(magnitudeSqr(p));
	}

	public static double magnitudeSqr(double[] p) {
		double ret=0.0;
		for(int i=0; i<p.length; ++i) {
			ret+=p[i]*p[i];
		}
		return ret;
	}

	public static double magnitude(Vect3D p) {
		return Math.sqrt(magnitudeSqr(p));
	}

	public static double magnitudeSqr(Vect3D p) {
		double ret=0.0;
		for(int i=0; i<3; ++i) {
			ret+=p.get(i)*p.get(i);
		}
		return ret;
	}

	public static void normalize(double[] p) {
		double len=Math.sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2]);
		if(len==0.0 || len==1.0) return;
		p[0]/=len;
		p[1]/=len;
		p[2]/=len;
	}
	
	public static Vect3D normalize(Vect3D p) {
		double len=Math.sqrt(p.get(0)*p.get(0)+p.get(1)*p.get(1)+p.get(2)*p.get(2));
		if(len==0.0 || len==1.0) return p;
		return new Vect3D(p.get(0)/len,p.get(1)/len,p.get(2)/len);
	}
	
	public static void scale(double[] p,double factor) {
		p[0]*=factor;
		p[1]*=factor;
		p[2]*=factor;
	}

	public static Vect3D scale(Vect3D p,double factor) {
		return new Vect3D(p.get(0)*factor,p.get(1)*factor,p.get(2)*factor);
	}

	public static double dotProduct(double[] p1,double[] p2) {
		return p1[0]*p2[0]+p1[1]*p2[1]+p1[2]*p2[2];
	}

	public static double dotProduct(Vect3D p1,Vect3D p2) {
		return p1.get(0)*p2.get(0)+p1.get(1)*p2.get(1)+p1.get(2)*p2.get(2);
	}

	public static double dotProduct(Vect3D p1,double[] p2) {
		return p1.get(0)*p2[0]+p1.get(1)*p2[1]+p1.get(2)*p2[2];
	}

	public static void crossProduct(double[] p1,double[] p2,double[] prod) {
		prod[0]=p1[1]*p2[2]-p2[1]*p1[2];
		prod[1]=p1[2]*p2[0]-p2[2]*p1[0];
		prod[2]=p1[0]*p2[1]-p2[0]*p1[1];
	}
	
	public static double[] crossProduct(double[] p1,double[] p2) {
		double[] ret=new double[3];
		ret[0]=p1[1]*p2[2]-p2[1]*p1[2];
		ret[1]=p1[2]*p2[0]-p2[2]*p1[0];
		ret[2]=p1[0]*p2[1]-p2[0]*p1[1];
		return ret;
	}
	
	public static Vect3D crossProduct(Vect3D p1,Vect3D p2) {
		return new Vect3D(p1.get(1)*p2.get(2)-p2.get(1)*p1.get(2),
			p1.get(2)*p2.get(0)-p2.get(2)*p1.get(0),
			p1.get(0)*p2.get(1)-p2.get(0)*p1.get(1));
	}
	
	/**
	 * This method returns the distance of a point from a line segment.  It finds the
	 * point on the full line closest to the point.  If that is not between the
	 * endpoints of the segment it returns the distance to the nearest endpoint.  If it
	 * is, then it returns the distnace to that point.
	 * @param s1 First segment endpoint.
	 * @param s2 Second segment endpoint.
	 * @param p Point to find distance of.
	 * @return The distance between the segment and the point.
	 */
	public static double distanceFromSegment(double[] s1,double[] s2,double[] p) {
		double t=segmentNearestParameterValue(s1,s2,p);
		double dist;
		if(t<=0.0) {
			dist=Basic.distance(s1,p);
		} else if(t>=1.0) {
			dist=Basic.distance(s2,p);
		} else {
			double[] c={s1[0]+t*(s2[0]-s1[0]),s1[1]+t*(s2[1]-s1[1]),s1[2]+t*(s2[2]-s1[2])};
			dist=Basic.distance(c,p);
		}
		return dist;
	}

	/**
	 * This method returns the distance of a point from a line segment.  It finds the
	 * point on the full line closest to the point.  If that is not between the
	 * endpoints of the segment it returns the distance to the nearest endpoint.  If it
	 * is, then it returns the distnace to that point.
	 * @param s1 First segment endpoint.
	 * @param s2 Second segment endpoint.
	 * @param p Point to find distance of.
	 * @return The distance between the segment and the point.
	 */
	public static double distanceFromSegment(Vect3D s1,Vect3D s2,Vect3D p) {
		double t=segmentNearestParameterValue(s1,s2,p);
		double dist;
		if(t<=0.0) {
			dist=Basic.distance(s1,p);
		} else if(t>=1.0) {
			dist=Basic.distance(s2,p);
		} else {
			Vect3D c=new Vect3D(s1.get(0)+t*(s2.get(0)-s1.get(0)),s1.get(1)+t*(s2.get(1)-s1.get(1)),s1.get(2)+t*(s2.get(2)-s1.get(2)));
			dist=Basic.distance(c,p);
		}
		return dist;
	}

	/**
	 * This method returns the parameter at which the given point is closest to the
	 * line including the provided segment.  If it is between the endpoints it will be
	 * in the range [0,1].  If it is <0 then it is before s1.  If it is >1 then it is
	 * after s2.
	 * @param s1 First segment endpoint.
	 * @param s2 Second segment endpoint.
	 * @param p Point to find distance of.
	 * @return The parameter when it is closest.
	 */
	public static double segmentNearestParameterValue(double[] s1,double[] s2,double[] p) {
		double numer=0.0,denom=0.0;
		for(int i=0; i<3; ++i) {
			double delta=(s2[i]-s1[i]);
			numer+=delta*(s1[i]-p[i]);
			denom+=delta*delta;
		}
		if(denom==0.0) return 0.0;
		return -numer/denom;
	}

	/**
	 * This method returns the parameter at which the given point is closest to the
	 * line including the provided segment.  If it is between the endpoints it will be
	 * in the range [0,1].  If it is <0 then it is before s1.  If it is >1 then it is
	 * after s2.
	 * @param s1 First segment endpoint.
	 * @param s2 Second segment endpoint.
	 * @param p Point to find distance of.
	 * @return The parameter when it is closest.
	 */
	public static double segmentNearestParameterValue(Vect3D s1,Vect3D s2,Vect3D p) {
		double numer=0.0,denom=0.0;
		for(int i=0; i<3; ++i) {
			double delta=(s2.get(i)-s1.get(i));
			numer+=delta*(s1.get(i)-p.get(i));
			denom+=delta*delta;
		}
		if(denom==0.0) return 0.0;
		return -numer/denom;
	}
	
	/**
	 * This method returns the distance of a point from a plane that is defined by a triangle.
	 * The distance is signed and is positive on the side the normal points.  It is negative if
	 * the point is "behind" the triangle.  Keeping the sign allows the returned value to be
	 * easily used to calculate the point in the plane closest to the outside point.
	 * @param pnt The point we want the distance to.
	 * @param tri A triangle defining the plane to check against.
	 * @return The distance of the point from the plane.  Negative if it is "behind" it.
	 */
	public static double pointPlaneDistance(Vect3D pnt,RTTriangle tri) {
		Vect3D n=tri.getNormal(pnt);
		Vect3D p=pnt.minus(tri.getPoint(0));
		return Basic.dotProduct(p,n);
	}
	
	/**
	 * This method returns the parameter value along a segment for the point where it's line
	 * would cross the plane defined by the given triangle.  If that value is between 0 and 1
	 * then it is part of the segment, otherwise it is beyond an endpoint.
	 * @param s1 Start endoint of the segment.
	 * @param s2 End endpoint of the segment.
	 * @param tri The triangle the defines the plane.
	 * @return The parameter value for the corssing. 
	 */
	public static double segmentPlaneIntersectParameter(Vect3D s1,Vect3D s2,RTTriangle tri) {
		Vect3D rs1=s1.minus(tri.getPoint(0));
		Vect3D rs2=s2.minus(tri.getPoint(0));
		Vect3D sep=rs2.minus(rs1);
		double denom=Basic.dotProduct(tri.getNormal(s1),sep);
		if(denom==0.0) return 0.0;
		double numer=Basic.dotProduct(tri.getNormal(s1),rs1);
		return -numer/denom;
	}
	
	/**
	 * This simple method helps with the functions that return parameters for a segment and something
	 * else.  Given the segment and the parameter this gives the point it represents.
	 * @param s1 Start endoint of the segment.
	 * @param s2 End endpoint of the segment.
	 * @param p The parameter value.
	 * @return The Vect3D where that parameter is on the segment line.
	 */
	public static Vect3D pointFromSegmentAndParameter(Vect3D s1,Vect3D s2,double p) {
		return new Vect3D(s1.getX()+p*(s2.getX()-s1.getX()),s1.getY()+p*(s2.getY()-s1.getY()),s1.getZ()+p*(s2.getZ()-s1.getZ()));
	}
	
	/**
	 * This method will find the parameters for the closest approach between two segments.
	 * The first two arguments are the endpoints of the first segment and the second two are
	 * for the second segment.  The method returns an array with two elements.  The first
	 * element is the parameter of the first segment and the second is that of the second
	 * segment.
	 * @param p1 The first point of the first segment.
	 * @param p2 The second point of the first segment.
	 * @param r1 The first point of the second segment.
	 * @param r2 The second point of the second segment.
	 * @return An array with the two parameters for the segments.
	 */
	public static double[] twoSegmentClosestParams(Vect3D p1,Vect3D p2,Vect3D r1,Vect3D r2) {
		double[] ret=new double[2];
		Vect3D dr=r2.minus(r1);
		Vect3D dp=p2.minus(p1);
		Vect3D rp=r1.minus(p1);
		Vect3D pr=p1.minus(r1);
		double drdr=dotProduct(dr,dr);
		double dpdp=dotProduct(dp,dp);
		double drdp=dotProduct(dr,dp);
		System.out.println(dr);
		System.out.println(dp);
		System.out.println(drdr+" "+dpdp+" "+drdp);
		if(drdp==0.0) {
			ret[0]=dotProduct(pr,dp)/dpdp;
			ret[1]=dotProduct(rp,dr)/drdr;
		} else {
			double a=dotProduct(pr,dp)/drdp;
			double b=dpdp/drdp;
			double denom=dotProduct(dp.minus(dr.mult(b)),dr);
			if(denom==0) {
				ret[0]=0;
			} else {
				ret[0]=dotProduct(rp.plus(dr.mult(a)),dr)/denom;
			}
			ret[1]=dotProduct(pr.plus(dp.mult(ret[0])),dp)/drdp;
		}
		return ret;
	}
}
