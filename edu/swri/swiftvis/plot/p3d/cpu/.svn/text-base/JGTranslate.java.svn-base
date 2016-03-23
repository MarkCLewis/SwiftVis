package edu.swri.swiftvis.plot.p3d.cpu;

// JGTranslate.java

public class JGTranslate extends JGTransform {
	public JGTranslate(double dx,double dy,double dz) {
		int i;

		for(i=0; i<4; i++) setElement(i,i,1.0);
		setElement(0,3,dx);
		setElement(1,3,dy);
		setElement(2,3,dz);
	}

	public JGTranslate(JGVector v) {
		int i;

		for(i=0; i<4; i++) setElement(i,i,1.0);
		if(v.element(3)==0.0) return;
		setElement(0,3,v.element(0)/v.element(3));
		setElement(1,3,v.element(1)/v.element(3));
		setElement(2,3,v.element(2)/v.element(3));
	}

	public JGTranslate(JGVector v,double scale) {
		int i;

		for(i=0; i<4; i++) setElement(i,i,1.0);
		if(v.element(3)==0.0) return;
		setElement(0,3,scale*v.element(0)/v.element(3));
		setElement(1,3,scale*v.element(1)/v.element(3));
		setElement(2,3,scale*v.element(2)/v.element(3));
	}
}
