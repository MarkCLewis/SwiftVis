/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

public interface SchemeFunction extends SchemeElement {
    SchemeElement execute(SchemeEnvironment env,ConsCell args);
}
