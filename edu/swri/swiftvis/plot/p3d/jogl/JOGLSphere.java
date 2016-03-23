package edu.swri.swiftvis.plot.p3d.jogl;

//import javax.media.opengl.GL;

//import com.sun.opengl.util.GLUT;



public class JOGLSphere {} //implements JOGLObject {
//
//	private float[] center;
//	private float[] color;
//	private double radius;
//	private DrawAttributes attr;
//	private GLUT glut = new GLUT();
//	
//	public JOGLSphere(Sphere3D s, DrawAttributes a) {
//		attr = a;
//		
//		radius = s.getRadius();
//		
//		center = new float[3];
//		center[0] = (float)s.getCenter().getX();
//		center[1] = (float)s.getCenter().getY();
//		center[2] = (float)s.getCenter().getZ();
//		
//		color = new float[4];
//		color[0] = attr.getColor().getRed()/255.0f;
//		color[1] = attr.getColor().getGreen()/255.0f;
//		color[2] = attr.getColor().getBlue()/255.0f;
//		color[3] = (float)attr.getOpacity();
//		
//		
//	}
//	
//	@Override
//	public void draw(GL gl) {
//		gl.glMatrixMode(GL.GL_MODELVIEW);
//		gl.glLoadIdentity();
//		gl.glTranslatef(center[0],center[1],center[2]);
//		gl.glColor4f(color[0],color[1],color[2],color[3]);
//		gl.glMaterialfv(GL.GL_FRONT_AND_BACK,GL.GL_DIFFUSE,color,0);
//		glut.glutSolidSphere(radius,16,8);
//
//	}
//	
//	public float[] getCenter() {
//		return center;
//	}
//	
//	public double getRadius() {
//		return radius;
//	}
//
//	@Override
//	public int getType() {
//		return JOGLObject.Sphere;
//	}
//	
//	@Override
//	public String toString() {
//		return "(" + center[0] + "," + center[1] + "," + center[2] + ") " + radius;
//	}
//
//}
