package edu.swri.swiftvis.plot.p3d.cpu;

import edu.swri.swiftvis.plot.p3d.Vect3D;

// JGVector.java
// Class to do 4-vectors for 3D graphics.

public class JGVector {
	public JGVector() {
		component[3]=1.0;
	}

	public JGVector(double c1,double c2,double c3,double c4) {
		component[0]=c1;
		component[1]=c2;
		component[2]=c3;
		component[3]=c4;
	}

	public JGVector(JGVector v) {
		component[0]=v.component[0];
		component[1]=v.component[1];
		component[2]=v.component[2];
		component[3]=v.component[3];
	}

	public JGVector(double c[]) {
		for(int i=0; i<4; i++) component[i]=c[i];
	}
    
    public JGVector(Vect3D v) {
        component[0]=v.getX();
        component[1]=v.getY();
        component[2]=v.getZ();
        component[3]=1.0;
    }

	public JGVector multiply(JGTransform m) {
		JGVector ret=new JGVector();
		int i,j;
		double sum;

		for(i=0; i<4; i++) {
			sum=0.0;
			for(j=0; j<4; j++) {
				sum+=element(j)*m.element(i,j);
			}
			ret.setElement(i,sum);
		}

		return(ret);
	}

	public static JGVector multiply(JGTransform m,JGVector v) {
		return(v.multiply(m));
	}

	public JGVector add(JGVector v) {
		JGVector ret=new JGVector(this);

		if(v.component[3]==0.0) {
			ret.component[0]+=v.component[0];
			ret.component[1]+=v.component[1];
			ret.component[2]+=v.component[2];
		} else {
			ret.component[0]+=v.component[0]*component[3]/v.component[3];
			ret.component[1]+=v.component[1]*component[3]/v.component[3];
			ret.component[2]+=v.component[2]*component[3]/v.component[3];
		}

		return(ret);
	}

	public JGVector scale(double s) {
		JGVector ret=new JGVector(this);

		ret.component[0]*=s;
		ret.component[1]*=s;
		ret.component[2]*=s;

		return(ret);
	}

	public double dot(JGVector v) {
		return(component[0]*v.component[0]+component[1]*v.component[1]+component[2]*v.component[2]);
	}

	public JGVector cross(JGVector v) {
		JGVector ret=new JGVector();

		ret.setElement(0,element(1)*v.element(2)-element(2)*v.element(1));
		ret.setElement(1,element(2)*v.element(0)-element(0)*v.element(2));
		ret.setElement(2,element(0)*v.element(1)-element(1)*v.element(0));
		ret.setElement(3,element(3)*v.element(3));

		return(ret);
	}

	public void increment(JGVector v) {
		if(v.component[3]==0.0) {
			component[0]+=v.component[0];
			component[1]+=v.component[1];
			component[2]+=v.component[2];
		} else {
			component[0]+=v.component[0]*component[3]/v.component[3];
			component[1]+=v.component[1]*component[3]/v.component[3];
			component[2]+=v.component[2]*component[3]/v.component[3];
		}
	}

	public double element(int i) {
		if((i<0) || (i>3))  return(0.0);
		return(component[i]);
	}

	public void setElement(int i,double val) {
		if((i<0) || (i>3))  return;
		component[i]=val;
	}

	@Override
    public String toString() {
		return component[0]+" "+component[1]+" "+component[2]+" "+component[3];
	}

	public void normalize() {
		if(component[3]==0.0) return;
		component[0]/=component[3];
		component[1]/=component[3];
		component[2]/=component[3];
		component[3]=1.0;
        double len=Math.sqrt(component[0]*component[0]+component[1]*component[1]+component[2]*component[2]);
        component[0]/=len;
        component[1]/=len;
        component[2]/=len;
	}

	private double[] component=new double[4];
}
