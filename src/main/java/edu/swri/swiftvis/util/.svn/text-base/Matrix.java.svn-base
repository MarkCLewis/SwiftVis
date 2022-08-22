package edu.swri.swiftvis.util;

import java.io.Serializable;

// Matrix.java
// This class implements a Matrix in Java.  It could be extended to a
// Vector as well.

public class Matrix implements Serializable {
	private static final long serialVersionUID = -1582200891683408530L;
	
	public Matrix(int m,int n) {
		int i;

		rows=m;
		cols=n;
		vals=new double[m*n];
		for(i=0; i<m*n; i++) vals[i]=0.0;
	}

	public Matrix(Matrix mat) {
		int i;

		rows=mat.rows;
		cols=mat.cols;
		vals=new double[rows*cols];
		for(i=0; i<rows*cols; i++) vals[i]=mat.vals[i];
	}

	public void print() {
		int i,j;

		System.out.println("Print "+rows+" by "+cols+" matrix");
		for(i=0; i<rows; i++) {
			for(j=0; j<cols; j++) {
				System.out.print(get(i,j)+" ");
			}
			System.out.println(" ");
		}
	}

	public void print(String str) {
		int i,j;

		System.out.println(str);
		for(i=0; i<rows; i++) {
			for(j=0; j<cols; j++) {
				System.out.print(get(i,j)+" ");
			}
			System.out.println("\n");
		}
	}

	public int rows() { return(rows); }
	public int columns() { return(cols); }

	public double get(int i,int j) {
		if((i<0) || (j<0) || (i>=rows) || (j>=cols)) return(0.0);
		return(vals[j+i*cols]);
	}

	public void set(int i,int j,double val) {
		if((i<0) || (j<0) || (i>=rows) || (j>=cols)) return;
		vals[j+i*cols]=val;
	}

	public Matrix transpose() {
		Matrix ret=new Matrix(cols,rows);
		int i,j;

		for(i=0; i<rows; i++) {
			for(j=0; j<cols; j++) {
				ret.set(j,i,get(i,j));
			}
		}
		return(ret);
	}

	public Matrix multiply(Matrix m) {
		if(cols!=m.rows) return(null);
		Matrix ret=new Matrix(rows,m.cols);
		int i,j,k;

		for(i=0; i<rows; i++) {
			for(j=0; j<m.cols; j++) {
				ret.set(i,j,0.0);
				for(k=0; k<cols; k++) {
					ret.set(i,j,ret.get(i,j)+get(i,k)*m.get(k,j));
				}
			}
		}
		return(ret);
	}

	public Matrix scale(double d) {
		Matrix ret=new Matrix(this);
		int i;

		for(i=0; i<rows*cols; i++) ret.vals[i]*=d;
		return(ret);
	}

	public Matrix add(Matrix m) {
		if((rows!=m.rows) || (cols!=m.cols)) return(null);
		Matrix ret=new Matrix(this);
		int i;

		for(i=0; i<rows*cols; i++) ret.vals[i]+=m.vals[i];
		return(ret);
	}

	public double norm() {
		int i;
		double ret=0.0;

		for(i=0; i<rows*cols; i++) ret+=vals[i]*vals[i];
		return(Math.sqrt(ret));
	}

	private double vals[];
	private int rows,cols;
}
