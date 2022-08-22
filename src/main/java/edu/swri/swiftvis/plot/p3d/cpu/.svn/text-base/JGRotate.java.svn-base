package edu.swri.swiftvis.plot.p3d.cpu;

// JGRotate.java

public class JGRotate extends JGTransform {
	public JGRotate(double ux,double uy,double uz,double theta) {
		axis_x=ux;
		axis_y=uy;
		axis_z=uz;
		angle=theta;
		setMatrix();
	}

	public void incrementAngle(double delta_ang) {
		angle+=delta_ang;
		if(angle>=Math.PI) angle-=2.0*Math.PI;
		if(angle<=-Math.PI) angle+=2.0*Math.PI;
		setMatrix();
	}

	public void setRotation(double ux,double uy,double uz,double theta) {
		axis_x=ux;
		axis_y=uy;
		axis_z=uz;
		angle=theta;
		setMatrix();
	}

	private void setMatrix() {
		double mag;
		double cost=Math.cos(angle),sint=Math.sin(angle);

		mag=Math.sqrt(axis_x*axis_x+axis_y*axis_y+axis_z*axis_z);
		if(mag==0.0) {
			for(int i=0; i<4; i++) setElement(i,i,1.0);
			return;
		}
		if(mag!=1.0) {
			axis_x/=mag;
			axis_y/=mag;
			axis_z/=mag;
		}
		setElement(0,0,axis_x*axis_x+cost*(1.0-axis_x*axis_x));
		setElement(0,1,axis_x*axis_y*(1-cost)-axis_z*sint);
		setElement(0,2,axis_x*axis_z*(1-cost)+axis_y*sint);
		setElement(0,3,0.0);
		setElement(1,0,axis_x*axis_y*(1-cost)+axis_z*sint);
		setElement(1,1,axis_y*axis_y+cost*(1.0-axis_y*axis_y));
		setElement(1,2,axis_y*axis_z*(1-cost)-axis_x*sint);
		setElement(1,3,0.0);
		setElement(2,0,axis_x*axis_z*(1-cost)-axis_y*sint);
		setElement(2,1,axis_y*axis_z*(1-cost)+axis_x*sint);
		setElement(2,2,axis_z*axis_z+cost*(1.0-axis_z*axis_z));
		setElement(2,3,0.0);
		setElement(3,0,0.0);
		setElement(3,1,0.0);
		setElement(3,2,0.0);
		setElement(3,3,1.0);
	}

	private double axis_x,axis_y,axis_z,angle;
}
