/*
 * Created on Jun 12, 2007
 */
package edu.swri.swiftvis.scripting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.swri.swiftvis.DataElement;
// import jdk.nashorn.internal.objects.NativeArray;

public class ScriptUtil {
    public static String[] parseToStringArray(Object o) {
        if(o instanceof String[]) return (String[])o;
        // if(o instanceof NativeArray) o=new IterableNativeArray((NativeArray)o); 
        if(o instanceof Iterable<?>) {
            ArrayList<String> ret=new ArrayList<String>();
            for(Object s:(Iterable<?>)o) {
                ret.add(s.toString());
            }
            return ret.toArray(new String[ret.size()]);
        }
        return null;
    }
    
    public static void buildElements(Object p,Object v,List<DataElement> dataVect) {
        // if(p instanceof NativeArray) p=new IterableNativeArray((NativeArray)p);
        // if(v instanceof NativeArray) v=new IterableNativeArray((NativeArray)v);
        if(!(p instanceof Iterable<?>)) p=null;
        if(!(v instanceof Iterable<?>)) v=null;
        if(p==null && v==null) return;
        Iterator<?> pi=(p==null)?null:((Iterable<?>)p).iterator();
        Iterator<?> vi=(v==null)?null:((Iterable<?>)v).iterator();
        while((pi!=null && pi.hasNext()) || (vi!=null && vi.hasNext())) {
            Object po=(pi==null)?null:pi.next();
            Object vo=(vi==null)?null:vi.next();
            int[] params=null;
            float[] vals=null;
            // if(po instanceof NativeArray) po=new IterableNativeArray((NativeArray)po);
            if(po instanceof int[]) params=(int[])po;
            else if(po instanceof Iterable<?>) {
                Iterable<?> i=(Iterable<?>)po;
                int cnt=0;
                for(Iterator<?> iter=i.iterator(); iter.hasNext(); iter.next()) {
                    cnt++;
                }
                params=new int[cnt];
                cnt=0;
                for(Object o:i) {
                    params[cnt]=ScriptUtil.objectToInt(o);
                    cnt++;
                }
            } else if(po!=null) {
                params=new int[1];
                params[0]=ScriptUtil.objectToInt(po);
            }
            // if(vo instanceof NativeArray) vo=new IterableNativeArray((NativeArray)vo);
            if(vo instanceof float[]) vals=(float[])vo;
            else if(vo instanceof Iterable<?>) {
                Iterable<?> i=(Iterable<?>)vo;
                int cnt=0;
                for(Iterator<?> iter=i.iterator(); iter.hasNext(); iter.next()) {
                    cnt++;
                }
                vals=new float[cnt];
                cnt=0;
                for(Object o:i) {
                    vals[cnt]=ScriptUtil.objectToFloat(o);
                    cnt++;
                }
            } else if(vo!=null) {
                vals=new float[1];
                vals[0]=ScriptUtil.objectToFloat(vo);
            }
            if(params==null) params=new int[0];
            if(vals==null) vals=new float[0];
            dataVect.add(new DataElement(params,vals));
        }
    }
    
    public static int objectToInt(Object o) {
        if(o instanceof Number) return ((Number)o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public static float objectToFloat(Object o) {
        if(o instanceof Number) return ((Number)o).floatValue();
        try {
            return Float.parseFloat(o.toString());
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public static double objectToDouble(Object o) {
        if(o instanceof Number) return ((Number)o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    public static double[][] objectToDouble2DArray(Object o) {
        // if(o instanceof NativeArray) o=new IterableNativeArray((NativeArray)o);
        if(!(o instanceof Iterable<?>) || o==null) return null;
        ArrayList<double[]> ret=new ArrayList<double[]>();
        for(Object sub1:(Iterable<?>)o) {
            // if(sub1 instanceof NativeArray) sub1=new IterableNativeArray((NativeArray)sub1);
            if(sub1 instanceof Iterable<?>) {
                Iterable<?> i=(Iterable<?>)sub1;
                int cnt=0;
                for(Iterator<?> iter=i.iterator(); iter.hasNext(); iter.next()) {
                    cnt++;
                }
                double[] vals=new double[cnt];
                cnt=0;
                for(Object sub2:i) {
                    vals[cnt]=ScriptUtil.objectToDouble(sub2);
                    cnt++;
                }
                ret.add(vals);
            } else {
                ret.add(new double[]{objectToDouble(sub1)});
            }
        }
        double[][] realRet=new double[ret.size()][];
        ret.toArray(realRet);
        return realRet;
    }
}
