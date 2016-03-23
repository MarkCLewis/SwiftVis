package edu.swri.swiftvis.plot.p3d.cpu;

//JGView.java
//This file sets up the viewing matrix and does much of the drawing
//functionality, including culling.  It uses a perspective rendering method.

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class JGView extends JGTransform {
    
    public JGView() {
        
    }

    // This constructor sets up the RRRT matrix so that all points can be
    // moved to the proper point of view.  Later function calls set up the
    // clipping planes and calculate the complete transformation.
    public JGView(JGVector a,JGVector b,JGVector c) {
        setRRRT(a,b,c);
    }

    public void setRRRT(JGVector a,JGVector b,JGVector c) {
        int i,j;
        JGTransform tmp=new JGTransform();
        double x,y,z,cval,sval;

        RRRT=new JGTransform();

        vect_a=a;
        vect_b=b;
        vect_c=c;

        // Set up the tranlation matrix.
        for(i=0; i<4; i++) RRRT.setElement(i,i,1.0);
        for(i=0; i<3; i++) RRRT.setElement(i,3,-a.element(i)/a.element(3));
        
        // Do the y rotation.
        // 1,1=3,3=cos t_y, 
        b=b.multiply(RRRT);
        c=c.multiply(RRRT);
        System.out.println("Translated b="+b);
        System.out.println("Translated c="+c);
        for(i=0; i<4; i++) tmp.setElement(i,i,1.0);
        x=b.element(0);
        y=b.element(1);
        z=b.element(2);
        if((x!=0.0) || (z!=0.0)) {
            cval=z/Math.sqrt(x*x+z*z);
            sval=-x/Math.sqrt(x*x+z*z);
            tmp.setElement(0,0,cval);
            tmp.setElement(0,2,sval);
            tmp.setElement(2,0,-sval);
            tmp.setElement(2,2,cval);
            RRRT=RRRT.multiply(tmp);
        }

        // Do the x rotation.
        b=b.multiply(tmp);
        c=c.multiply(tmp);
        for(i=0; i<4; i++)
            for(j=0; j<4; j++) tmp.setElement(i,j,0.0);
        for(i=0; i<4; i++) tmp.setElement(i,i,1.0);
        x=b.element(0);
        y=b.element(1);
        z=b.element(2);
        if((y!=0.0) || (z!=0.0)) {
            cval=z/Math.sqrt(y*y+z*z);
            sval=y/Math.sqrt(y*y+z*z);
            tmp.setElement(1,1,cval);
            tmp.setElement(2,2,cval);
            tmp.setElement(1,2,-sval);
            tmp.setElement(2,1,sval);
            RRRT=RRRT.multiply(tmp);
        }

        // Do the z rotation.
        c=c.multiply(tmp);
        for(i=0; i<4; i++)
            for(j=0; j<4; j++) tmp.setElement(i,j,0.0);
        for(i=0; i<4; i++) tmp.setElement(i,i,1.0);
        x=c.element(0);
        y=c.element(1);
        z=c.element(2);
        if((y!=0.0) || (x!=0.0)) {
            cval=y/Math.sqrt(y*y+x*x);
            sval=x/Math.sqrt(y*y+x*x);
            tmp.setElement(1,1,cval);
            tmp.setElement(0,0,cval);
            tmp.setElement(1,0,sval);
            tmp.setElement(0,1,-sval);
            RRRT=RRRT.multiply(tmp);
        }

        calcFullTransform();
    }

    public void setWindow(double umin,double vmin,double umax,double vmax) {
        win_u_min=umin;
        win_u_max=umax;
        win_v_min=vmin;
        win_v_max=vmax;
        win_set=true;

        calcFullTransform();
    }

    public void setProjection(int type,double x,double y,double z) {
        // Sets the projection_mat.
        if(type==0) {
            projection_type=type;
            proj_vector=new JGVector(x,y,z,0.0);
        } else if(type==1) {
            projection_type=type;
            proj_vector=new JGVector(x,y,z,1.0);
        }

        calcFullTransform();
    }

    public void setFrontAndBackClippingDistance(double df,double db) {
        dist_to_front=df;
        dist_to_back=db;
        clips_set=true;

        calcFullTransform();
    }

    public void calcFullTransform() {
        JGTransform SH,T,S,S2,TOPAR;
        JGVector tmp_vect;
        int i;
        double f;

        if((RRRT==null) || (projection_type==-1) || (!win_set) || (!clips_set))
            return;
        if(projection_type==0) {						// Parallel
            SH=new JGTransform();
            for(i=0; i<4; i++) SH.setElement(i,i,1.0);
            tmp_vect=new JGVector(proj_vector);
            tmp_vect=tmp_vect.multiply(RRRT);
            if(tmp_vect.element(2)==0.0) {
                System.out.println("Bad direction of projection.");
                return;
            }
            SH.setElement(0,2,-tmp_vect.element(0)/tmp_vect.element(2));
            SH.setElement(1,2,-tmp_vect.element(1)/tmp_vect.element(2));

            T=new JGTransform();
            for(i=0; i<4; i++) T.setElement(i,i,1.0);
            T.setElement(0,3,-(win_u_max+win_u_min)/2.0);
            T.setElement(1,3,-(win_v_max+win_v_min)/2.0);
            T.setElement(2,3,dist_to_front);

            S=new JGTransform();
            for(i=0; i<4; i++) S.setElement(i,i,1.0);
            S.setElement(0,0,2.0/(win_u_max-win_u_min));
            S.setElement(1,1,2.0/(win_v_max-win_v_min));
            S.setElement(2,2,1.0/(dist_to_front+dist_to_back));

            this.setTo(RRRT.multiply(SH.multiply(T.multiply(S))));
        } else if(projection_type==1) {					// Perspective
            T=new JGTransform();
            for(i=0; i<4; i++) T.setElement(i,i,1.0);
            tmp_vect=new JGVector(proj_vector);
            tmp_vect=tmp_vect.multiply(RRRT);
            T.setElement(0,3,-tmp_vect.element(0));
            T.setElement(1,3,-tmp_vect.element(1));
            T.setElement(2,3,-tmp_vect.element(2));

            SH=new JGTransform();
            for(i=0; i<4; i++) SH.setElement(i,i,1.0);
            SH.setElement(0,2,-(win_u_max+win_u_min)/2.0/tmp_vect.element(2));
            SH.setElement(1,2,-(win_v_max+win_v_min)/2.0/tmp_vect.element(2));

            S=new JGTransform();
            for(i=0; i<4; i++) S.setElement(i,i,1.0);
            S.setElement(0,0,-tmp_vect.element(2)/((win_u_max-win_u_min)/2.0));
            S.setElement(1,1,-tmp_vect.element(2)/((win_v_max-win_v_min)/2.0));

            S2=new JGTransform();
            for(i=0; i<4; i++) S2.setElement(i,i,1.0);
            S2.setElement(0,0,1.0/(-tmp_vect.element(2)+dist_to_back));
            S2.setElement(1,1,1.0/(-tmp_vect.element(2)+dist_to_back));
            S2.setElement(2,2,1.0/(-tmp_vect.element(2)+dist_to_back));

            TOPAR=new JGTransform();
            for(i=0; i<4; i++) TOPAR.setElement(i,i,1.0);
            TOPAR.setElement(3,2,1.0);
            TOPAR.setElement(3,3,0.0);
            f=(-tmp_vect.element(2)-dist_to_front)/(-tmp_vect.element(2)+dist_to_back);
            TOPAR.setElement(2,2,1.0/(1.0-f));
            TOPAR.setElement(2,3,-f/(1.0-f));

            this.setTo(RRRT.multiply(T.multiply(SH.multiply(S.multiply(S2.multiply(TOPAR))))));
        }

    }

    public void endFrame(Graphics2D g,JGLighting lighting,boolean breakPolys) {
        List<JGObject> workList=new ArrayList<JGObject>();
        Rectangle r=g.getClip().getBounds();
        
        for(int i=0; i<getNumChildren(); ++i) {
            recursiveLighting(JGTransform.identity(),getChild(i),lighting);
        }

        // Transform
        recursiveTransform(JGTransform.identity(),this,workList);

        for(JGObject jbo:workList) if(jbo==null) System.out.println("A - Null object on work list.");
        System.out.println("Before cull "+workList.size());

        // Cull - full transform moves to 1x2x2 cube at origin
        cullScene(workList);
        for(JGObject jbo:workList) if(jbo==null) System.out.println("B - Null object on work list.");
        System.out.println("After cull "+workList.size());

        // Depth Sort
        depthSort(workList,breakPolys);
        for(JGObject jbo:workList) if(jbo==null) System.out.println("C - Null object on work list.");

        // Draw
        System.out.println("Drawing "+workList.size());
        g.setColor(Color.black);
        g.fillRect(r.x,r.y,r.width,r.height);
        for(JGObject obj:workList) obj.draw(g);
        System.out.println("Done with EndFrame!!");
    }
    
    private void recursiveLighting(JGTransform t,JGObject obj,JGLighting lighting) {
        JGTransform nt;
        JGObject transObj;

        if(obj.drawable()) {
            nt=t;
            transObj=obj.applyTransform(nt);
            if(transObj!=null) {
                obj.setDrawColor(lighting);
            }
        } else {
            nt=((JGTransform)obj).multiply(t);
        }
        
        // recurse children
        for(int i=0; i<obj.getNumChildren(); ++i) {
            recursiveLighting(nt,obj.getChild(i),lighting);            
        }
    }

    private void recursiveTransform(JGTransform t,JGObject obj,List<JGObject> list) {
        JGTransform nt;
        JGObject transObj;

        if(obj.drawable()) {
            nt=t;
            transObj=obj.applyTransform(nt);
            if(transObj!=null) list.add(transObj);
        } else {
            nt=((JGTransform)obj).multiply(t);
        }
        // recurse children
        for(int i=0; i<obj.getNumChildren(); ++i) {
            recursiveTransform(nt,obj.getChild(i),list);            
        }
    }

    private void depthSort(List<JGObject> list,boolean breakPolys) {
        int numToDraw=list.size();
        ObjBoxPair[] obArray=new ObjBoxPair[numToDraw+10];
        int last_null;//,min,max;
        int break_cond,near_far;
        List<JGObject> newObjs1,newObjs2;
        InsertReturn ir;

        for(int i=0; i<obArray.length; ++i) {
            if(i<numToDraw) obArray[i]=new ObjBoxPair(list.get(i),list.get(i).getBoundingBox());
            else obArray[i]=new ObjBoxPair();
        }
        list.clear();

        // Put all the stuff without a valid bounding box up front.
        last_null=-1;
        for(int i=0; i<numToDraw; i++) {
            if(obArray[i].box==null) {
                last_null++;
                if(last_null!=i) {
                    ObjBoxPair tmp=obArray[last_null];
                    obArray[last_null]=obArray[i];
                    obArray[i]=tmp;
                }
            }
        }

        Arrays.sort(obArray, last_null+1, numToDraw, new Comparator<ObjBoxPair>() {
            @Override
            public int compare(ObjBoxPair ob1,ObjBoxPair ob2) {
                if(ob1.box.getBack()>ob2.box.getBack()) return 1;
                else if(ob1.box.getBack()<ob2.box.getBack()) return -1;
                else return 0;
            }
        });

        if(breakPolys) {
            // Test each vs. all subsequent.  Add to list when you've found
            // something that can go on the back.
            for(int i=0; i<numToDraw; ) {
                break_cond=0;
                while((i<obArray.length) && (obArray[i].box==null)) {
                    if(obArray[i].obj!=null) {
                        list.add(obArray[i].obj);
                        obArray[i].obj=null;
                    }
                    i++;
                }
                for(int j=i+1; (j<numToDraw) && (break_cond==0); j++) {
                    while((j<obArray.length) && (obArray[j].box==null)) {
                        if(obArray[j].obj!=null) {
                            list.add(obArray[j].obj);
                            obArray[j].obj=null;
                        }
                        j++;
                    }
                    if((i<obArray.length) && (j<obArray.length) &&
                            (obArray[i].box.getFront()<obArray[j].box.getBack()) &&
                            (obArray[i].box.getBack()>obArray[j].box.getFront()) &&
                            (obArray[i].box.getLeft()<obArray[j].box.getRight()) &&
                            (obArray[i].box.getRight()>obArray[j].box.getLeft()) &&
                            (obArray[i].box.getBottom()<obArray[j].box.getTop()) &&
                            (obArray[i].box.getTop()>obArray[j].box.getBottom())) {
                        // Done with easy tests, now do harder ones tests.
                        near_far=planeTest(obArray[i].obj,obArray[j].obj);
                        if(near_far==0) {
                            near_far=planeTest(obArray[j].obj,obArray[i].obj);
                            if((near_far==0) && projTest(obArray[i].obj,obArray[j].obj)) {
                                // Split polys, add to arrays, leave loop and reset
                                // i to deal with furthest back one.

                                newObjs1=obArray[i].obj.splitByPlane(obArray[j].obj.getPlane());
                                newObjs2=obArray[j].obj.splitByPlane(obArray[i].obj.getPlane());
                                if(newObjs1!=null) {
                                    obArray[i].obj=null;
                                    if(newObjs2!=null) {
                                        obArray[j].obj=null;
                                        newObjs1.addAll(newObjs2);
                                    }
                                } else if(newObjs2!=null) {
                                    obArray[j].obj=null;
                                    newObjs1=newObjs2;
                                }
                                if(newObjs1!=null) {
//                                  System.out.println("Before:"+obj_array+" "+obj_array.length+" "+box_array+" "+new_objs1.NumElements());
                                    while(!newObjs1.isEmpty()) {
                                        ir=insertObject(obArray,newObjs1.get(0));
                                        numToDraw=ir.numToDraw;
                                        obArray=ir.objBoxArray;
                                        newObjs1.remove(0);
                                    }
                                    break_cond=1;
                                    i=0;
                                }

//                              System.out.println("After:"+obj_array+" "+obj_array.length);
                            }
                        }
                    }
                }
                if(break_cond==0) {  // They passed all test so remove from array and add to list
//                  System.out.println(i+" "+obj_array[i].toString());
                    list.add(obArray[i].obj);
                    obArray[i].obj=null;
                    obArray[i].box=null;
                    i++;
                }
//              System.out.println("loop:"+obj_array+" "+obj_array.length+" "+num_to_draw+" "+break_cond);
            }
        } else {
            for(int i=0; i<obArray.length; ++i) {
                if(obArray[i].obj!=null) list.add(obArray[i].obj);
            }
        }
    }

    // Returns the near or far condition of .  0=crossing, 1=near, 2=far, 3=N/A
    private int planeTest(JGObject o1,JGObject o2) {
        JGPlane p2;
        int ret=3;	// Start off as 11b then and in other points
        int i;

        if((o1==null) || (o2==null)) return(3);
        p2=o2.getPlane();
        if(p2==null) return(3);
        for(i=0; i<o1.getNumPoints() && ret!=0; i++) {
            ret&=p2.nearFarTest(o1.getPoint(i));
        }
        return ret;
    }

    private boolean projTest(JGObject o1,JGObject o2) {
        return true;
    }

    private InsertReturn insertObject(ObjBoxPair[] obArray,JGObject newObj) {
        int i;
        int firstOpen=-1,numFull=0,insertAt=-1;
        ObjBoxPair[] newArray;
        JGBoundingBox box;
        InsertReturn ret;

        // Check if there is an open slot.
        for(i=0; i<obArray.length; i++) {
            if(obArray[i].obj==null) {
                if(firstOpen==-1) firstOpen=i;
            } else numFull++;
        }
        if(newObj==null) {
            ret=new InsertReturn();
            ret.objBoxArray=obArray;
            ret.numToDraw=numFull+1;
            return ret;
        }
        if(firstOpen==-1) { // no open slots so resize
            System.out.println("Resizing");
            newArray=new ObjBoxPair[obArray.length+10];
            for(i=0; i<newArray.length; i++) {
                if(i<obArray.length) newArray[i]=obArray[i];
                else newArray[i]=new ObjBoxPair();
            }
            firstOpen=obArray.length;
            obArray=newArray;
        }
        box=newObj.getBoundingBox();
        if((box!=null) && (numFull>0)) {
            for(i=0; (i<obArray.length) && (insertAt==-1); i++) {
                if((obArray[i].box!=null) && ((box.getBack()>obArray[i].box.getBack()) ||
                        ((box.getBack()==obArray[i].box.getBack()) && (box.getFront()>obArray[i].box.getFront()))))
                    insertAt=i;
            }
            if(insertAt==-1) {
                for(i=obArray.length-1; (i>=0) && (obArray[i].box==null); i--);
                insertAt=i+1;
            }
        } else {
            insertAt=0;
        }
//      System.out.println("Inserting "+new_obj.toString());
//      System.out.println("first_open="+first_open+" insert at="+insert_at);
        if(insertAt!=firstOpen) {
            if(insertAt<firstOpen) {
                for(i=firstOpen; i>insertAt; i--) {
                    obArray[i]=obArray[i-1];
                }
            } else {
                insertAt--;
                for(i=firstOpen; i<insertAt; i++) {
                    obArray[i]=obArray[i+1];
                }
            }
        }
//      System.out.println("Inserted at "+insert_at);
        obArray[insertAt]=new ObjBoxPair(newObj,box);
        ret=new InsertReturn();
        ret.objBoxArray=obArray;
        ret.numToDraw=numFull+1;
//      System.out.println("Done with insert");
        return(ret);
    }

    private void cullScene(List<JGObject> list) {
        JGObject current;
        int i=0;

        while(i<list.size()) {
            current=list.get(i);
            if(!current.cull()) {
                list.remove(i);
            } else i++;
        }
    }

    public void moveForward(double step_size) {
        JGVector move=new JGVector(vect_b.add(vect_a.scale(-1.0)));
        double mag;

        mag=Math.sqrt(move.element(0)*move.element(0)+move.element(1)*move.element(1)+move.element(2)*move.element(2));
        if(mag==0.0) return;
        move=move.scale(step_size/mag);
        vect_a.increment(move);
        vect_b.increment(move);
        vect_c.increment(move);
        if(projection_type==1) {
            proj_vector.increment(move);
        }
        setRRRT(vect_a,vect_b,vect_c);
    }

    public void turnLeftRight(double angle) {
        JGVector axis,tmp;

        axis=vect_b.add(vect_a.scale(-1.0));
        tmp=vect_c.add(vect_a.scale(-1.0));
        tmp=axis.cross(tmp);
        axis=tmp.cross(axis);
        rotateView(axis.element(0),axis.element(1),axis.element(2),angle);
    }

    public void turnUpDown(double angle) {
        JGVector axis,tmp;

        axis=vect_b.add(vect_a.scale(-1.0));
        tmp=vect_c.add(vect_a.scale(-1.0));
        axis=axis.cross(tmp);
        rotateView(axis.element(0),axis.element(1),axis.element(2),angle);
    }

    public void spin(double angle) {
        rotateView(vect_b.element(0)-vect_a.element(0),vect_b.element(1)-vect_a.element(1),vect_b.element(2)-vect_a.element(2),angle);
    }

    public void rotateView(double ux,double uy,double uz,double angle) {
        JGTranslate tr,tr2;
        JGRotate rotate;
        JGTransform total;

        rotate=new JGRotate(ux,uy,uz,angle);
        if(projection_type==1) {
            tr=new JGTranslate(proj_vector,-1.0);
            tr2=new JGTranslate(proj_vector);
            total=tr.multiply(rotate.multiply(tr2));

            vect_a=vect_a.multiply(total);
            vect_b=vect_b.multiply(total);
            vect_c=vect_c.multiply(total);
            proj_vector=proj_vector.multiply(total);
        } else if(projection_type==0) {
            tr=new JGTranslate(vect_a,-1.0);
            tr2=new JGTranslate(vect_a);
            total=tr.multiply(rotate.multiply(tr2));

            vect_a=vect_a.multiply(total);
            vect_b=vect_b.multiply(total);
            vect_c=vect_c.multiply(total);
            proj_vector=proj_vector.multiply(rotate);
        }
        setRRRT(vect_a,vect_b,vect_c);
        calcFullTransform();
    }

    public double getWinBound(int which) {
        switch(which) {
        case 0: return(win_v_max);
        case 1: return(win_u_max);
        case 2: return(win_v_min);
        case 3: return(win_u_min);
        default: break;
        }
        return(0.0);
    }

    public double getFrontClip() {
        return(dist_to_front);
    }

    public double getBackClip() {
        return(dist_to_back);
    }

    public int getProjType() {
        return(projection_type);
    }

    public void alterProjVector(double dx,double dy,double dz) {
        calcFullTransform();
    }

    public void switchProjectionType() {
        if(projection_type==0) {
            projection_type=1;
            proj_vector=vect_a.add(proj_vector);
        } else {
            projection_type=0;
            proj_vector=proj_vector.add(vect_a.scale(-1.0));
        }
        calcFullTransform();
    }
    
    @Override
    public String toString() {
        return "View "+super.toString();
    }

    private JGVector vect_a=null,vect_b=null,vect_c=null;
    private JGTransform RRRT=null;
    private int projection_type=-1;
    private JGVector proj_vector=null;
    private double win_u_min,win_u_max;
    private double win_v_min,win_v_max;
    private boolean win_set=false;
    private double dist_to_front,dist_to_back;
    private boolean clips_set=false;
    public final int TOP=0,LEFT=1,RIGHT=2,BOTTOM=3;

    private static class InsertReturn {
        private ObjBoxPair[] objBoxArray;
        private int numToDraw;
    }

    private static class ObjBoxPair {
        public ObjBoxPair() {}
        public ObjBoxPair(JGObject o,JGBoundingBox b) {
            obj=o;
            box=b;
        }
        private JGObject obj;
        private JGBoundingBox box;
    }
}

