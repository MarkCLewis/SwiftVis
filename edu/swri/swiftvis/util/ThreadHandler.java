/*
 * Created on Feb 13, 2006
 */
package edu.swri.swiftvis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;

/**
 * Class for handling multithreading.
 * 
 * @author Glenn Kavanagh
 */
public final class ThreadHandler {
    
    public static void makeInstance(int numThreads) {
        inst=new ThreadHandler(numThreads);  
    }
    
    public static void stop() {
    	if (inst!=null) inst.shutdown();
    }
    
    /**
     * Get the singleton.
     * @return The instance of ThreadHandler being used by this application.
     */
    public static ThreadHandler instance() {
    	if (inst==null) {
    		makeInstance(OptionsData.instance().getNumThreads());
    	}
        return inst;
    }

    protected ThreadHandler(int nt) {
        numThreads=nt;
        es = Executors.newCachedThreadPool();
    }
    
    /**
     * This method adds a task to be done in parallel.  The future for this task is
     * ignored so you can't wait for it later.
     * @param <E>
     * @param task
     */
    public <E> void loadTask(Runnable task) {
    	es.submit(task);
    }
    
    /**
     * This method will start up a task and keep track of it so that you can wait
     * for it to complete later.
     * @param <E>
     * @param task
     */
    public <E> void loadWaitTask(Runnable task) {
    	loadWaitTask(null,task);
    }
    // second version with Key
    public <E> void loadWaitTask(GraphElement key, Runnable task) {
        Future<?> f=es.submit(task);
        if(f==null) {
            System.err.println("Null future returned with task for "+key);
        } else {
            tasks.add(new Pair(key,f));
        }
    }
    
    /**
     * This method will block until all wait tasks have completed.
     */
    public void waitForAll() {
    	waitForAll(null);
    }
    // second version with key
    public void waitForAll(GraphElement key) {
    	//for(Pair p:tasks) {
    	for (int i=0; i<tasks.size(); i++) {
    		Pair p=tasks.get(i);
            try {
            	// only call get if Key matches
            	if (p.key==key || key==null) {
            		p.future.get();
            	}
            } catch(InterruptedException e) {
                e.printStackTrace();
            } catch(ExecutionException e) {
                e.printStackTrace();
            }
        }
    	for (int i=0; i<tasks.size(); i++) {
    		Pair p=tasks.get(i);
            if (p.key==key) {
            	tasks.remove(i);
            	i--;
            }
        }
    }
    
    public void shutdown() {
    	es.shutdown();
    }
    
    public void roundRobinForLoop(GraphElement key,final int start, final int end, final LoopBody body) {
        for(int t=0; t<numThreads; ++t) {
            final int thread=t;
            loadWaitTask(key,new Runnable() {
                @Override
                public void run() {
                    for(int i=start+thread; i<end; i+=numThreads) {
                        body.execute(i);
                    }
                }
            });
        }
        waitForAll(key);
    }

    public void chunkedForLoop(GraphElement key, final int start, final int end, final LoopBody body) {
        int b=start;
        int numIters=end-start;
        int s=numIters/numThreads;
        int r=numIters%numThreads;
        for(int t=0; t<numThreads; ++t) {
            final int begin=b;
            final int size=s+((t<r)?1:0);
            loadWaitTask(key, new Runnable() {
                @Override
                public void run() {
                    for(int i=begin; i<begin+size; ++i) {
                        body.execute(i);
                    }
                }
            });
            b+=size;
        }
        waitForAll(key);
    }

    public void chunkedForLoop(GraphElement key, final int start, final int end, final LoopBodyWithGroup body) {
        int b=start;
        int numIters=end-start;
        int s=numIters/numThreads;
        int r=numIters%numThreads;
        for(int t=0; t<numThreads; ++t) {
            final int begin=b;
            final int size=s+((t<r)?1:0);
            final int g=t;
            loadWaitTask(key, new Runnable() {
                @Override
                public void run() {
                    for(int i=begin; i<begin+size; ++i) {
                        body.execute(i,g);
                    }
                }
            });
            b+=size;
        }
        waitForAll(key);
    }

    public void chunkedForLoop(GraphElement key, final int start, final int end, final ReduceLoopBody[] body) {
        int b=start;
        int numIters=end-start;
        int s=numIters/numThreads;
        int r=numIters%numThreads;
        for(int t=0; t<numThreads; ++t) {
            final int th=t;
            final int begin=b;
            final int size=s+((t<r)?1:0);
            loadWaitTask(key,new Runnable() {
                @Override
                public void run() {
                    body[th].execute(begin,begin+size);
                }
            });
            b+=size;
        }
        waitForAll(key);
    }
    
    public void groupChunkedForLoop(DataSink key, DataFormula df, final int start, final int end, final LoopBody body) {
    	int b=start;
        int numIters=end-start;
        int s=numIters/numThreads;
        int r=numIters%numThreads;
//        int[] safeInds=groupSafeIndexes(key,df,start,end); 
        
        for(int t=0; t<numThreads; ++t) {
            final int begin=b;
            final int size=s+((t<r)?1:0);
            loadWaitTask(key, new Runnable() {
                @Override
                public void run() {
                    for(int i=begin; i<begin+size; ++i) {
                        body.execute(i);
                    }
                }
            });
            b+=size;
        }
        waitForAll(key);
    }

    public void dynamicForLoop(GraphElement key,final int start, final int end, final LoopBody body) {
        for(int i=start; i<end; ++i) {
            final int iter=i;
            loadWaitTask(key,new Runnable() {
                @Override
                public void run() { body.execute(iter); }
            });
        }
        waitForAll(key);
    }
    
    public int[] groupSafeIndexes(DataSink sink, DataFormula df) {
    	// Calc range
    	int[] ranges=df.getSafeElementRange(sink, 0);
    	DataFormula.checkRangeSafety(ranges,sink);
    	return groupSafeIndexes(sink,df,ranges[0],ranges[1]);
    }
    
    public int[] groupSafeIndexes(DataSink sink, DataFormula df, int from, int to) {
    	ArrayList<Integer> inds=new ArrayList<Integer>();
    	double prev=0.0;
    	for (int i=from; i<to; i++) {
    		double cur=df.valueOf(sink,0, i);
    		if (prev!=cur) {
    			inds.add(i);
    			prev=cur;
    		}
    	}
    	int[] ret=new int[inds.size()];
    	for (int i=0; i<ret.length; i++) {
    		ret[i]=inds.get(i);
    	}
    	return ret;
    }

    public int getNumThreads() {
        return numThreads;
    }
    
    protected final int numThreads;
    
    private static ThreadHandler inst;
    private ExecutorService es;
    private List<Pair> tasks = Collections.synchronizedList(new LinkedList<Pair>()); 
    
    private static class Pair {
    	private GraphElement key;
    	private Future<?> future;
    	public Pair(GraphElement key,Future<?> f) {
    		this.key=key;
    		future=f;
    	}
    }
}
