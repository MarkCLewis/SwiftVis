/*
 * Created on Jan 26, 2006
 */
package edu.swri.swiftvis.util;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorEnumeration<E> implements Enumeration<E> {
    public IteratorEnumeration(Iterator<E> i) {
        iter=i;
    }

    @Override
    public boolean hasMoreElements() {
        return iter.hasNext();
    }

    @Override
    public E nextElement() {
        return iter.next();
    }

    private Iterator<E> iter;
}
