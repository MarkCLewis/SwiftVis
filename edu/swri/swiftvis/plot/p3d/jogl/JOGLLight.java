package edu.swri.swiftvis.plot.p3d.jogl;

//import javax.media.opengl.GL;

import edu.swri.swiftvis.DataSink;

public interface JOGLLight {
//	public void draw(GL gl);
	public int getLight();
	public void fixValues(DataSink sink);
}
