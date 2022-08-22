/*
 * Created on Dec 29, 2003
 */
package edu.swri.swiftvis.plot.p3d.raytrace;

import java.awt.Color;

import edu.swri.swiftvis.plot.p3d.Basic;
import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.Triangle3D;
import edu.swri.swiftvis.plot.p3d.Vect3D;


/**
 * This class represents a triangle.
 * @author Mark Lewis
 */
public final class RTTriangle implements RTGeometry {
	public static void main(String[] args) {
		for(int i=0; i<1000; ++i) {
			double[][] pnts=new double[3][3];
			for(int j=0; j<3; ++j) {
				for(int k=0; k<3; ++k) {
					pnts[j][k]=Math.random();
				}
			}
			RTTriangle tri=new RTTriangle(pnts);
			for(int j=0; j<3; ++j) {
				if(Basic.distance(tri.getBoundingSphere().getCenter(),tri.getPoint(j))>tri.getBoundingSphere().getRadius()) {
					System.out.println("Error with bounding sphere.");
				}
			}
		}
	}
	
	public RTTriangle(double x1,double y1,double z1,double x2,double y2,double z2,double x3,
		double y3,double z3) {
			
		p[0]=new Vect3D(x1,y1,z1);
		p[1]=new Vect3D(x2,y2,z2);
		p[2]=new Vect3D(x3,y3,z3);
		calcNorm();		
	}

	public RTTriangle(double[] p1,double[] p2,double[] p3) {
		p[0]=new Vect3D(p1);
		p[1]=new Vect3D(p2);
		p[2]=new Vect3D(p3);
		calcNorm();		
	}
	
	public RTTriangle(Vect3D p1,Vect3D p2,Vect3D p3) {
		p[0]=p1;
		p[1]=p2;
		p[2]=p3;
		calcNorm();		
	}
	
    public RTTriangle(Vect3D p1,Vect3D p2,Vect3D p3,Vect3D n1,Vect3D n2,Vect3D n3) {
        p[0]=p1;
        p[1]=p2;
        p[2]=p3;
        calcNorm();
        pvNorm=new Vect3D[3];
        pvNorm[0]=n1;
        pvNorm[1]=n2;
        pvNorm[2]=n3;
    }
    
	public RTTriangle(Vect3D[] pnts) {
		p[0]=pnts[0];
		p[1]=pnts[1];
		p[2]=pnts[2];
		calcNorm();		
	}
	
    public RTTriangle(Vect3D[] pnts,Vect3D[] norms) {
        p[0]=pnts[0];
        p[1]=pnts[1];
        p[2]=pnts[2];
        calcNorm();
        pvNorm=new Vect3D[3];
        pvNorm[0]=norms[0];
        pvNorm[1]=norms[1];
        pvNorm[2]=norms[2];
    }
    
	public RTTriangle(double[][] pnts) {
		p[0]=new Vect3D(pnts[0]);
		p[1]=new Vect3D(pnts[1]);
		p[2]=new Vect3D(pnts[2]);
		calcNorm();		
	}
    
    public RTTriangle(Triangle3D t) {
        p[0]=t.getPoint(0);
        p[1]=t.getPoint(1);
        p[2]=t.getPoint(2);
        calcNorm();
        pvNorm=t.getNormals();
        if(pvNorm.length<3) pvNorm=null;
    }
	
    /**
     * Return a sphere that bounds this triangle.
     * @see geometry.RTGeometry#getBoundingSphere()
     */
    @Override
    public RTSphere getBoundingSphere() {
        if (bSphere == null) {
            double[] len = new double[3];
            int maxi = 0;

            // Find longest leg.
            for (int i = 0; i < 3; ++i) {
                int o = (i + 1) % 3;
                len[i] = Basic.distance(p[i], p[o]);
                if (len[i] < len[maxi])
                    maxi = i;
            }

            // Assume midpoint of longest leg.
            double[] c = new double[3];
            double rad = len[maxi] * 0.5;
            int maxo = (maxi + 1) % 3;
            c[0] = (p[maxi].get(0) + p[maxo].get(0)) * 0.5;
            c[1] = (p[maxi].get(1) + p[maxo].get(1)) * 0.5;
            c[2] = (p[maxi].get(2) + p[maxo].get(2)) * 0.5;

            // Check if third point within that radius.
            int maxnot = (maxo + 1) % 3;
            double otherDist = Basic.distance(c, p[maxnot]);
            if (otherDist > rad) {
                // If not then move towards that outside point a bit.
                double moveDist = 0.9 * (otherDist - rad) / otherDist;
                c[0] -= (c[0] - p[maxnot].get(0)) * moveDist;
                c[1] -= (c[1] - p[maxnot].get(1)) * moveDist;
                c[2] -= (c[2] - p[maxnot].get(2)) * moveDist;
                for (int i = 0; i < 3; ++i) {
                    double r = Basic.distance(c, p[i]);
                    if (r > rad)
                        rad = r;
                }
            }
            bSphere = new RTSphere(c, rad);
        }
        return bSphere;
    }


	@Override
    public double[] calcRayIntersect(Ray ray) {
		double[] d0=new double[3],d1=new double[3];
		double[] leg=new double[3],sep=new double[3];
		double[] cross=new double[3];
		double dot,dot2;
		double denom;
		int i;
		d0[0]=(ray.startX()-p[0].get(0))*norm.get(0);
		d0[1]=(ray.startY()-p[0].get(1))*norm.get(1);
		d0[2]=(ray.startZ()-p[0].get(2))*norm.get(2);
		d1[0]=(ray.oneX()-ray.startX())*norm.get(0);
		d1[1]=(ray.oneY()-ray.startY())*norm.get(1);
		d1[2]=(ray.oneZ()-ray.startZ())*norm.get(2);
		denom=d1[0]+d1[1]+d1[2];
		if(denom==0.0) {
			return new double[0];
		}
		double s=-(d0[0]+d0[1]+d0[2])/denom;
		if(s<=1e-9) return new double[0];
		double[] inter=new double[3];
		for(i=0; i<3; i++) {
			inter[i]=ray.point(i,s);
		}
		for(i=0; i<3; i++) {
			leg[i]=p[1].get(i)-p[0].get(i);
			sep[i]=inter[i]-p[0].get(i);
		}
		Basic.crossProduct(leg,sep,cross);
		double[] normA=norm.get();
		dot=Basic.dotProduct(normA,cross);
		for(i=0; i<3; i++) {
			leg[i]=p[2].get(i)-p[1].get(i);
			sep[i]=inter[i]-p[1].get(i);
		}
		Basic.crossProduct(leg,sep,cross);
		dot2=Basic.dotProduct(normA,cross);
		if(dot==0.0) dot=dot2;
		if(dot*dot2<0) return new double[0];
		for(i=0; i<3; i++) {
			leg[i]=p[0].get(i)-p[2].get(i);
			sep[i]=inter[i]-p[2].get(i);
		}
		Basic.crossProduct(leg,sep,cross);
		dot2=Basic.dotProduct(normA,cross);
		if(dot*dot2<0) return new double[0];
		double[] ret={s};
		return ret;
	}
	
    @Override
    public double[] getNormal(double[] location) {
        if(pvNorm==null) return norm.get();
        return calcLocNorm(location);
    }
    
    @Override
    public Vect3D getNormal(Vect3D location) {
        if(pvNorm==null) return norm;
        return new Vect3D(calcLocNorm(location.get()));
    }
    
    private double[] calcLocNorm(double[] loc) {
        double nx=0;
        double ny=0;
        double nz=0;
        double[] weights=calcWeights(loc);
        for(int i=0; i<3; ++i) {
            nx+=weights[i]*pvNorm[i].getX();
            ny+=weights[i]*pvNorm[i].getY();
            nz+=weights[i]*pvNorm[i].getZ();
        }
        return new double[]{nx,ny,nz};         
    }

    public OctreeNode getNode() {
        return node;
    }

    public void setNode(OctreeNode n) {
        node = n;
    }


	public double getCoord(int point,int axis) {
		return p[point].get(axis);
	}
	
	public Vect3D getPoint(int point) {
		return p[point];
	}

    @Override
    public DrawAttributes getAttributes(double[] location) {
        if(attr.length==1) return attr[0];
        return calcLocAttributes(location);
    }

    private DrawAttributes calcLocAttributes(double[] loc) {
        double reflect=0;
        double opacity=0;
        double red=0;
        double green=0;
        double blue=0;
        double[] weights=calcWeights(loc);
        for(int i=0; i<3; ++i) {
            reflect+=weights[i]*attr[i].getReflectivity();
            opacity+=weights[i]*attr[i].getOpacity();
            float[] col=attr[i].getColor().getColorComponents(null);
            red+=weights[i]*col[0];
            green+=weights[i]*col[1];
            blue+=weights[i]*col[2];
        }
        if(red<0) red=0; else if(red>1) red=1;
        if(green<0) green=0; else if(green>1) green=1;
        if(blue<0) blue=0; else if(blue>1) blue=1;
        return new DrawAttributes(new Color((float)red,(float)green,(float)blue),reflect,opacity);         
    }
    
    private double[] calcWeights(double[] loc) {
        double[] ret=new double[3];
        double[] legPnt=new double[3];
        double[] leg=new double[3];
        double[] toLoc=new double[3];
        double[] legFrac=new double[3];
        double[] legWeight=new double[3];
        double tot=0;
        for(int i=0; i<3; ++i) {
            int oi=(i+1)%3;
            leg[0]=p[oi].getX()-p[i].getX();
            leg[1]=p[oi].getY()-p[i].getY();
            leg[2]=p[oi].getZ()-p[i].getZ();
            double legLen=Basic.magnitude(leg);
            Basic.normalize(leg);
            toLoc[0]=loc[0]-p[i].getX();
            toLoc[1]=loc[1]-p[i].getY();
            toLoc[2]=loc[2]-p[i].getZ();
            double len=Basic.dotProduct(leg,toLoc);
            legFrac[i]=1-len/legLen;
            legPnt[0]=p[i].getX()+len*leg[0];
            legPnt[1]=p[i].getY()+len*leg[1];
            legPnt[2]=p[i].getZ()+len*leg[2];
            double dx=loc[0]-legPnt[0];
            double dy=loc[1]-legPnt[1];
            double dz=loc[2]-legPnt[2];
            double dist=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(dist==0) {
                ret[i]=legFrac[i];
                ret[oi]=1-legFrac[i];
                return ret;
            }
            legWeight[i]=1/dist;
            tot+=legWeight[i];
        }
        for(int i=0; i<3; ++i) {
            int oi=(i+1)%3;
            legWeight[i]/=tot;
            ret[i]+=legWeight[i]*legFrac[i];
            ret[oi]+=legWeight[i]*(1-legFrac[i]);
        }
        return ret;
    }

//    private double[] calcWeightsByPoints(double[] loc) {
//        double[] ret=new double[3];
//        double tot=0;
//        for(int i=0; i<3; ++i) {
//            double dx=loc[0]-p[i].getX();
//            double dy=loc[1]-p[i].getY();
//            double dz=loc[2]-p[i].getZ();
//            double dist=Math.sqrt(dx*dx+dy*dy+dz*dz);
//            if(dist==0) return new double[]{(i==0)?1:0,(i==1)?1:0,(i==2)?1:0};
//            ret[i]=1.0/dist;
//            tot+=ret[i];
//        }
//        ret[0]/=tot;
//        ret[1]/=tot;
//        ret[2]/=tot;
//        return ret;
//    }

    public void setAttributes(DrawAttributes[] da) {
        attr = da;
    }


	@Override
    public String toString() {
		return "Tri "+p[0]+"\n "+p[1]+"\n "+p[2];	
	}
	
	/**
	 * This method tells if a point is inside of the given triangle.  It assumes that the point
	 * is close to the plane of the triangle.
	 * @param pnt The point to check.
	 * @return Whether that point is inside.
	 */
	public boolean inside(Vect3D pnt) {
		double[] leg1=new double[3];
		double[] toPoint=new double[3];
		double[] cross=new double[3];
		double dot;
		double lastDot=0.0;
		for(int i=0; i<3; ++i) {
			int o=(i+1)%3;
			leg1[0]=p[o].get(0)-p[i].get(0);
			leg1[1]=p[o].get(1)-p[i].get(1);
			leg1[2]=p[o].get(2)-p[i].get(2);
			toPoint[0]=pnt.get(0)-p[i].get(0);
			toPoint[1]=pnt.get(1)-p[i].get(1);
			toPoint[2]=pnt.get(2)-p[i].get(2);
			Basic.crossProduct(leg1,toPoint,cross);
			dot=Basic.dotProduct(norm,cross);
			if(i>0) {
				if(dot*lastDot<0) return false;
			}
			lastDot=dot;
		}
		return true;
	}

	private void calcNorm() {
		double[] l1={p[1].get(0)-p[0].get(0),p[1].get(1)-p[0].get(1),p[1].get(2)-p[0].get(2)};
		double[] l2={p[2].get(0)-p[0].get(0),p[2].get(1)-p[0].get(1),p[2].get(2)-p[0].get(2)};
		norm=new Vect3D(Basic.crossProduct(l1,l2));
		norm=Basic.normalize(norm);
	}

    private Vect3D[] p = new Vect3D[3];
    private Vect3D norm;
    private Vect3D[] pvNorm;
    private RTSphere bSphere;
    private OctreeNode node;
    private DrawAttributes[] attr = {new DrawAttributes(Color.lightGray, 0.0, 1.0)};

}

