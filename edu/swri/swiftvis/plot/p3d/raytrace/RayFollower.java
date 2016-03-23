package edu.swri.swiftvis.plot.p3d.raytrace;
/*
 * Created on Dec 27, 2003
 */

import java.util.Iterator;


/**
 * @author Mark Lewis
 *
 * This class provides functionality for following a ray through space given and
 * octree to help optimize the search for what it can run into.  In order for this
 * to work, objects in the tree must be in nodes that are large enough to encapsulate
 * them and their centers must lie in the node.
 */
public final class RayFollower {
    public RayFollower(Ray r,Octree t) {
        ray=r;
        tree=t;
    }

    public RTGeometry getIntersectObject() {
        return intersectObject;
    }

    public double getIntersectTime() {
        return intersectTime;
    }

    public Ray getRay() {
        return ray;
    }


    /**
     * This function follows the ray using the octree and finds the first thing
     * that it intersects with.
     * @return This returns true if it did hit something and false if it didn't.
     */
    public boolean follow() {
        return follow(defaultVisitor);
    }

    public boolean follow(OctreeNode node) {
        return follow(defaultVisitor,node);
    }

    public boolean follow(Visitor v) {
        OctreeNode node;
        node=tree.getRoot();
        intersectObject=null;
        followRecur(node,node,v);
        return intersectObject!=null;
    }

    public boolean follow(Visitor v,OctreeNode node) {
        if(node==null) return follow(v);
        intersectObject=null;
        followRecur(node,node,v);
        return intersectObject!=null;
    }

    private void followRecur(OctreeNode node,OctreeNode ignore,Visitor v) {
        checkContents(node,v);
        NodeIntersectData[] nid=new NodeIntersectData[node.getMaxChildren()];
        int childCnt=0;
        //System.out.println(node.getBoundingSphere().getCenter()+"  "+node.getBoundingSphere().getRadius());
        for(int i=0; i<node.getMaxChildren(); ++i) {
            OctreeNode cnode=node.getChildByIndex(i);
            if(cnode!=null && cnode!=ignore) {
                double[] times=cnode.getBoundingSphere().calcRayIntersect(ray);
                if(times.length!=0 && (intersectObject==null || times[0]<intersectTime)) {
                    int j;
                    for(j=childCnt; j>0 && nid[j-1].time>times[0]; --j) {
                        nid[j]=nid[j-1];
                    }
                    nid[j]=new NodeIntersectData(cnode,times[0]);
                    childCnt++;
//                  followRecur(cnode,null,v);
                }
            }
        }
//      java.util.Arrays.sort(nid,0,childCnt);
        for(int i=0; i<childCnt && (intersectObject==null || nid[i].time<intersectTime); ++i) {
            followRecur(nid[i].node,null,v);
        }
        if(ignore!=null && node.getParent()!=null) followRecur(node.getParent(),node,v);
    }

    private void checkContents(OctreeNode node,Visitor v) {
        for(Iterator<RTGeometry> iter=node.objectIterator(); iter.hasNext(); ) {
            RTGeometry geom=iter.next();
            if(geom!=null) {
                double[] times=v.intersectTimes(geom,ray);
                if(times!=null && times.length!=0) {
                    boolean flag=true;
                    for(int j=0; j<times.length && flag; ++j) {
                        if(times[j]>=0 &&(intersectObject==null || times[j]<intersectTime)) {
                            intersectObject=geom;
                            intersectTime=times[j];
                            flag=false;
                        }
                    }
                }
            }
        }
    }

    private Ray ray;

    private Octree tree;

    private double intersectTime;

    private RTGeometry intersectObject;

    private static final DefaultVisitor defaultVisitor=new DefaultVisitor();

    public static interface Visitor {
        double[] intersectTimes(RTGeometry wo,Ray r);
    }

    private static class DefaultVisitor implements Visitor {
        @Override
        public double[] intersectTimes(RTGeometry wo,Ray r) {
            return wo.calcRayIntersect(r);
        }
    }

    private static class NodeIntersectData implements Comparable<NodeIntersectData> {
        public NodeIntersectData(OctreeNode n,double t) {
            node=n;
            time=t;
        }
        @Override
        public int compareTo(NodeIntersectData nid) {
            double dif=time-nid.time;
            if(dif<0) return -1;
            if(dif>0) return 1;
            return 0;
        }
        @Override
        public boolean equals(Object o) {
            if(o instanceof NodeIntersectData) return compareTo((NodeIntersectData)o)==0;
            return false;
        }
        
        public double time;

        public OctreeNode node;

    }
}
