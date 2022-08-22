/*
 * Created on Jul 8, 2008
 */
package edu.swri.swiftvis.util;

/**
 * This class has the required methods to do optimization.
 * 
 * @author Mark Lewis
 */
public class MatrixPack {
    // J^T*J*s=J^T*R
    public Matrix GaussNewtonStep(Matrix xc,Matrix J,Matrix R) {
        Matrix right=J.transpose().multiply(R);
        Matrix left=J.transpose().multiply(J);
        Matrix L=choleskyDecomp(left);
        Matrix ret;

        ret=choleskySolve(L,right);
        return(ret.scale(-1.0));
    }

    private Matrix choleskyDecomp(Matrix M) {
        Matrix L=new Matrix(M.rows(),M.columns());
        int i,j,k;
        double min,max,tmp=0.0;
        double maxdiag=0.0;
        boolean phase1=true;
        double root_eps=4.8e-6;

        for(i=0; i<M.rows(); i++)
            if(Math.abs(M.get(i,i))>maxdiag) maxdiag=Math.abs(M.get(i,i));
        if(M.get(0,0)<=root_eps*maxdiag) phase1=false;
        for(j=0; j<M.columns(); j++) {
            if(phase1) {
                if(j<M.rows()-1) {
                    min=M.get(j+1,j+1)-Math.pow(M.get(j,j+1),2.0)/M.get(j,j);
                    for(i=j+2; i<M.rows(); i++) {
                        tmp=M.get(i,i)-Math.pow(M.get(j,i),2.0)/M.get(j,j);
                        if(tmp<min) min=tmp;
                    }
                    if(min<root_eps*maxdiag) phase1=false;
                }
            }
            if(phase1) {
                L.set(j,j,Math.sqrt(M.get(j,j)));
            } else {
                max=maxdiag*root_eps;;
                for(i=j; i<M.rows(); i++) {
                    tmp=Math.abs(M.get(j,i));
                    if(tmp>max) max=tmp;
                }
                L.set(j,j,Math.sqrt(max));
            }
            for(i=j+1; i<M.rows(); i++) {
                L.set(i,j,M.get(j,i)/L.get(j,j));
            }
            for(i=j+1; i<M.rows(); i++) {
                for(k=i; k<M.rows(); k++) {
                    M.set(i,k,M.get(i,k)-L.get(i,j)*L.get(k,j));
                }
            }
        }

        return(L);
    }

    private Matrix choleskySolve(Matrix L,Matrix b) {
        return(uSolve(L.transpose(),lSolve(L,b)));
    }

    private Matrix uSolve(Matrix U,Matrix b) {
        Matrix ret=new Matrix(b);
        int i,j;
        double sum;

        for(i=b.rows()-1; i>=0; i--) {
            sum=0.0;
            for(j=i+1; j<U.columns(); j++) sum+=U.get(i,j)*ret.get(j,0);
            ret.set(i,0,(b.get(i,0)-sum)/U.get(i,i));
        }

        return(ret);
    }

    private Matrix lSolve(Matrix L,Matrix b) {
        Matrix ret=new Matrix(b);
        int i,j;
        double sum;

        for(i=0; i<b.rows(); i++) {
            sum=0.0;
            for(j=0; j<i; j++) sum+=L.get(i,j)*ret.get(j,0);
            ret.set(i,0,(b.get(i,0)-sum)/L.get(i,i));
        }

        return(ret);
    }

}
