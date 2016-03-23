/*
 * Created on Feb 18, 2006
 */
package edu.swri.swiftvis.util;

public interface ReduceLoopBody {
    /**
     * This method should have a loop that does whatever work is required running start to end.
     * @param start The beginning index.
     * @param end One beyond the last index.
     */
    public void execute(int start,int end);
}
