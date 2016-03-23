package edu.swri.swiftvis.plot.p3d.jogl;

//import javax.media.opengl.GL;

public interface JOGLObject {
	public int getType();
//	public void draw(GL gl);
	
	// public enum Type { Triangle, Sphere, PointLight, DirectionLight }
	
	
	public final static int Triangle = 1;
	public final static int Sphere = 2;
	public final static int PointLight = 3;
	public final static int DirectionLight = 4;
	public final static int AmbientLight = 4;
	
}
