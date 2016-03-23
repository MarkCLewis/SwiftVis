package edu.swri.swiftvis.plot.p3d.jogl;

//import javax.media.opengl.GL;



public class JOGLAmbientLight {} //implements JOGLObject, JOGLLight {
//
//	private AmbientLight light;
//	private float[] color;
//	
//	JOGLAmbientLight(AmbientLight am) {
//		light = am;
//		
//		color = new float[4];
//		color[0] = light.getColor().getRed()/255.0f;
//		color[1] = light.getColor().getGreen()/255.0f;
//		color[2] = light.getColor().getBlue()/255.0f;
//		color[3] = 1.0f;
//	}
//	
//	@Override
//	public void draw(GL gl) {
//		gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, color, 0);
//	}
//
//	@Override
//	public int getType() {
//		return JOGLObject.AmbientLight;
//	}
//
//	@Override
//	public int getLight() {
//		return 0;
//	}
//
//	@Override
//	public void fixValues(DataSink sink) {
//		// EMPTY ON PURPOSE
//	}
//	
//	@Override
//	public String toString() {
//		return "ambient light with color " + color[0] + " " + color[1] + " " + color[2];
//	}
//
//}
