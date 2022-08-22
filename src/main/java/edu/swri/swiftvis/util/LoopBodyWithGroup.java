/*
 * Created on Feb 15, 2006
 */
package edu.swri.swiftvis.util;

/**
 * This functor should hold the body of a loop for processing in parallel. 
 * 
 * @author Mark Lewis
 */
public interface LoopBodyWithGroup {
    /**
     * This is called with the index of the loop.  Basically, this shoudl have the
     * code you would normally put inside of a for(int i=start; i<end; ++i) loop.
     * @param i The index to process.
     */
    void execute(int i,int g);
}
