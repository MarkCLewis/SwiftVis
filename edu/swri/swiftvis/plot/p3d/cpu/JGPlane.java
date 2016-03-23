package edu.swri.swiftvis.plot.p3d.cpu;

// JGPlane.java
// This class handles things for planes.

public class JGPlane {
	public JGPlane(JGVector p,JGVector n) {
		point=p;
		norm=n;
	}

	// returns 1 for near side, 2 for far side, 3 if on the plane.
	public int nearFarTest(JGVector p) {
		JGVector tnorm,vect;
		double dot;

		if(norm.element(2)>0.0) tnorm=norm.scale(-1.0);
		else tnorm=norm;
		vect=p.add(point.scale(-1.0));
		dot=vect.dot(tnorm);
		if(dot==0.0) return(3);
		if(dot<0.0) return(2);
		return(1);
	}

	public JGVector intersectWithSegment(JGVector p1,JGVector p2) {
		JGVector ipoint;
		double t,numer,denom;
		double dx,dy,dz;

		p1.normalize();
		p2.normalize();
		dx=p2.element(0)-p1.element(0);
		dy=p2.element(1)-p1.element(1);
		dz=p2.element(2)-p1.element(2);
		denom=dx*norm.element(0)+dy*norm.element(1)+dz*norm.element(2);
		if(denom==0.0) return(null);
		numer=norm.element(0)*(point.element(0)-p1.element(0))+
			norm.element(1)*(point.element(1)-p1.element(1))+
			norm.element(2)*(point.element(2)-p1.element(2));
		t=numer/denom;
		if((t<0.0) || (t>1.0)) return(null);
//		System.out.println(t+"\n "+p1.toString()+"\n "+p2.toString());
		ipoint=new JGVector(p1.element(0)+t*dx,p1.element(1)+t*dy,p1.element(2)+t*dz,1.0);
//		System.out.println(" "+ipoint.toString());
		return(ipoint);
	}

	private JGVector point;
	private JGVector norm;
}