package edu.swri.swiftvis.plot.p3d.cpu;

// JGObject.java

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

abstract public class JGObject {
	public JGObject() {
		child=new ArrayList<JGObject>();
	}

	abstract public JGObject applyTransform(JGTransform t);

	public JGObject getChild(int i) {
		return child.get(i);
	}
    
    public int getNumChildren() {
        return child.size();
    }

	public void addChild(JGObject obj) {
		child.add(obj);
	}

	public void removeChild(int i) {
		child.remove(i);
	}

	public void removeChild(JGObject obj) {
		child.remove(obj);
	}
    
    public void clearChildren() {
        child.clear();
    }

	public void draw(Graphics2D g) {
	}

	public boolean drawable() {
		return false;
	}
    
    public void setDrawColor(JGLighting lighting) {
    }

	// The Cull function returns whether it is to be drawn.  In the
	// individual classes the function will make the proper changes to
	// the object.
	public boolean cull() {
		return true;
	}

	public JGBoundingBox getBoundingBox() {
		return null;
	}

	public JGPlane getPlane() {
		return null;
	}

	public JGVector getPoint(int i) {
		throw new RuntimeException("No points in object!");
	}
    
    public int getNumPoints() {
        return 0;
    }

	public List<JGObject> splitByPlane(JGPlane pl) {
		return null;
	}

	private List<JGObject> child;
}
