package edu.swri.swiftvis.plot.p3d.raytrace;

import java.util.*;


/*
 * Created on Dec 27, 2003
 */

/**
 * A single node in the octree.  This class is public so that special traversals
 * can be written without having to add them into the Octree class.
 * @author Mark Lewis
 */
public final class OctreeNode {
	public OctreeNode(double x,double y,double z,double s) {
		size=s;
		bSphere=new RTSphere(x,y,z,s*1.366);
	}
	
	private OctreeNode(OctreeNode p,double x,double y,double z,double s) {
		parent=p;
		size=s;
		bSphere=new RTSphere(x,y,z,s*1.366);
	}
	
    public void addObject(RTGeometry obj) {
		if(obj==null) {
			System.out.println("Something adding null object.");
			Thread.dumpStack();
            return;
		}
		double hsize=0.5*size;
		RTSphere objBSphere=obj.getBoundingSphere();
		if(objBSphere.getRadius()<hsize && objects!=null && objects.size()>=MaxUnsplit) {
			if(children==null) {
				children=new OctreeNode[8];
				for(Iterator<RTGeometry> iter=objects.iterator(); iter.hasNext(); ) {
				    RTGeometry obj2=iter.next();
			        RTSphere objBSphere2=obj2.getBoundingSphere();
				    if(objBSphere2.getRadius()<hsize) {
				        iter.remove();
			            objectToChild(obj2,hsize,objBSphere2);
				    }
				}
			}
			objectToChild(obj,hsize,objBSphere);
		} else {
		    if(objects==null) objects=new LinkedList<RTGeometry>();
            objects.add(obj);
		}
	}

    private void objectToChild(RTGeometry obj, double hsize, RTSphere objBSphere) {
        double x=objBSphere.getX();
        double y=objBSphere.getY();
        double z=objBSphere.getZ();
        int pos=getIndexByPosition(x,y,z);
        if(children[pos]==null) {
        	double qsize=0.25*size;
        	double ncx=bSphere.getX();
        	double ncy=bSphere.getY();
        	double ncz=bSphere.getZ();
        	if((pos&1)>0) ncx+=qsize; else ncx-=qsize;
        	if((pos&2)>0) ncy+=qsize; else ncy-=qsize;
        	if((pos&4)>0) ncz+=qsize; else ncz-=qsize;
        	children[pos]=new OctreeNode(this,ncx,ncy,ncz,hsize);
        }
        children[pos].addObject(obj);
    }
	
	public boolean containsPoint(double x,double y,double z) {
		double hsize=size*0.5;
		if(x<bSphere.getX()-hsize || x>bSphere.getX()+hsize) return false;
		if(y<bSphere.getY()-hsize || y>bSphere.getY()+hsize) return false;
		if(z<bSphere.getZ()-hsize || z>bSphere.getZ()+hsize) return false;
		return true;
	}
	
	public OctreeNode getChildByPosition(double x,double y,double z) {
		if(children==null) return null;
		return children[getIndexByPosition(x,y,z)];
	}
	
	public OctreeNode getChildByIndex(int index) {
		if(children==null || index>=children.length) return null;
		return children[index];
	}

    public OctreeNode getParent() {
        return parent;
    }

    public RTSphere getBoundingSphere() {
        return bSphere;
    }

	
	public int getMaxChildren() {
		return 8;
	}

    public int getNumObjects() {
        if(objects==null) return 0;
        return objects.size();
    }


	public boolean isSubDivided() {
		if(children != null) return true;
		return false;
	}
	
	public void getObjects(RTGeometry[] objs) {
	    if(objects!=null) objects.toArray(objs);
	}
	
	public Iterator<RTGeometry> objectIterator() {
	    if(objects==null) return new Iterator<RTGeometry>() {
            @Override
            public boolean hasNext() { return false; }
            @Override
            public RTGeometry next() { return null; }
            @Override
            public void remove() {}
	    };
		return objects.iterator();
	}
	
	public void getCenter(double[] center) {
		center[0]=bSphere.getX();
		center[1]=bSphere.getX();
		center[2]=bSphere.getX();
	}

    public double getSize() {
        return size;
    }

    @Override
    public void finalize() {
        RTSphere tmp=bSphere;
        bSphere=null;
        if(objects!=null) {
            if(!objects.isEmpty()) {
                bSphere=objects.get(0).getBoundingSphere();
            }
            for(int i=1; i<objects.size(); ++i) {
                bSphere=RTSphere.mutualBoundingSphere(bSphere,objects.get(i).getBoundingSphere());
            }
        }
        if(children!=null) {
            for(int i=0; i<children.length; ++i) {
                if(children[i]!=null) children[i].finalize();
            }
            for(int i=0; i<children.length; ++i) {
                if(children[i]!=null && children[i].bSphere!=null) {
                    if(bSphere==null) {
                        bSphere=children[i].bSphere;
                    } else {
                        bSphere=RTSphere.mutualBoundingSphere(bSphere,children[i].bSphere);                    
                    }
                }
            }
        }
        if(bSphere.getRadius()>tmp.getRadius()) {
            bSphere=tmp;
        }
    }
	
	private int getIndexByPosition(double x,double y,double z) {
		int pos=0;
		if(x>bSphere.getX()) pos|=1;
		if(y>bSphere.getY()) pos|=2;
		if(z>bSphere.getZ()) pos|=4;
		return pos;
	}

/*	public void visitNodeRecur(Sphere region,Octree.OctreeVisitor visitor) {
		for(ObjectListNode rover=firstObject; rover!=null; rover=rover.next) {
			double dist=Basic.distanceSqr(region.getCenter(),rover.obj.getBoundingSphere().getCenter());
			double rad=region.getRadius()+rover.obj.getBoundingSphere().getRadius();
			if(dist<=rad*rad) visitor.visitGeometry(rover.obj);
		}
		if(children!=null) {
			for(int i=0; i<children.length; ++i) {
				if(children[i]!=null) {
					double dist=Basic.distanceSqr(region.getCenter(),children[i].getBoundingSphere().getCenter());
					double rad=region.getRadius()+children[i].getBoundingSphere().getRadius();
					if(dist<=rad*rad) children[i].visitNodeRecur(region,visitor);
				}
			}
		}
	}
*/

    private double size;
    private OctreeNode parent;
    private OctreeNode[] children;
    private List<RTGeometry> objects=null;
    private RTSphere bSphere;

    private static final int MaxUnsplit = 5;
}
