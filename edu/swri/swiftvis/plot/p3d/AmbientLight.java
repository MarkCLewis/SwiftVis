/*
 * Created on Jul 12, 2008
 */
package edu.swri.swiftvis.plot.p3d;

import java.awt.Color;
import java.io.Serializable;

public class AmbientLight implements Serializable {

    public AmbientLight(Color c) {
        color=c;
    }
    
    public Color getColor() {
        return color;
    }

    private Color color;

    private static final long serialVersionUID = 8301890279149314324L;
}
