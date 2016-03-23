package edu.swri.swiftvis.plot.p3d.cpu;

// JGTransform.java

public class JGTransform extends JGObject {
	public JGTransform() {
		int i,j;

		mval=new double[4][4];
		for(i=0; i<4; i++)
			for(j=0; j<4; j++) mval[i][j]=0.0;
	}

	public JGTransform(int vals[][]) {
		int i,j;

		mval=new double[4][4];
		for(i=0; i<4; i++)
			for(j=0; j<4; j++) mval[i][j]=vals[i][j];
	}

	public void setTo(JGTransform t) {
		int i,j;
	
		for(i=0; i<4; i++)
			for(j=0; j<4; j++) mval[i][j]=t.element(i,j);
	}

	static public JGTransform identity() {
		int i;
		JGTransform ret=new JGTransform();
	
		for(i=0; i<4; i++) ret.setElement(i,i,1.0);

		return(ret);
	}

	public JGTransform multiply(JGTransform a) {
		JGTransform ret=new JGTransform();
		int i,j,k;
		double sum;

		for(i=0; i<4; i++) {
			for(j=0; j<4; j++) {
				sum=0.0;
				for(k=0; k<4; k++) {
					sum+=a.mval[i][k]*mval[k][j];
				}
				ret.mval[i][j]=sum;
			}
		}
		return(ret);
	}

	public static JGTransform multiply(JGTransform a,JGTransform b) {
		return(a.multiply(b));
	}

	public double element(int i, int j) {
		if((i<0) || (i>3) || (j<0) || (j>3)) {
			return(0.0);
		}
		return(mval[i][j]);
	}

	public void setElement(int i,int j,double val) {
		if((i<0) || (i>3) || (j<0) || (j>3)) {
			return;
		}
		mval[i][j]=val;
	}

    @Override
	public JGObject applyTransform(JGTransform t) {
		return(this.multiply(t));
	}

    @Override
	public String toString() {
		String ret="JGTransform\n";
		int i,j;

		for(i=0; i<4; i++) {
			for(j=0; j<4; j++) {
				ret+=Double.toString(element(i,j))+" ";
			}
			ret+="\n";
		}

		return(ret);
	}

	private double[][] mval;
}
