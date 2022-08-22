/*
 * Created on Dec 28, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.swri.swiftvis.plot.p3d.raytrace;

import edu.swri.swiftvis.plot.p3d.DrawAttributes;
import edu.swri.swiftvis.plot.p3d.Vect3D;


/**
 * This interface represents anything that should be geometry in the world. * @author Mark Lewis
 */

public interface RTGeometry {

    /**
     * This method returns a sphere that either bounds the geometry of the object or,
     * if the object doesn't have geometry, it gives the objects central location and
     * a rough notification size.
     * @return A sphere that gives position and size information for the object.
     */
    RTSphere getBoundingSphere();

	
	/**
	 * This function returns the first time >=0 at which the ray intersects or is in
	 * the geometry.
	 */
	double[] calcRayIntersect(Ray ray);
	
	/**
	 * Returns the normal vector for this geometry at that location.  If that point
	 * is not part of the surface of the geometry it can return null.  How the point is
	 * used can vary by geometry type.
	 * @param location The point we want the normal for.
	 * @return A normal 3-vector or null if that point isn't on the surface.
	 */
	double[] getNormal(double[] location);

    /**
     * Returns the normal for this element at the specified location.
     * @param location Where we want the normal for.
     * @return A Vect3D that gives the normal.
     */
    Vect3D getNormal(Vect3D location);
    
    /**
     * Return the drawing attributes for this object.
     * @return
     */
    DrawAttributes getAttributes(double[] location);
}
