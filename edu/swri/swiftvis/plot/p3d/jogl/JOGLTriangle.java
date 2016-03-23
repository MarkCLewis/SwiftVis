package edu.swri.swiftvis.plot.p3d.jogl;

//import javax.media.opengl.GL;



public class JOGLTriangle {} //implements JOGLObject {
//
//	private DrawAttributes style;
//	private Triangle3D triangle;
//	private float[] color;
//	
//	
//	public JOGLTriangle(Triangle3D t, DrawAttributes s) {
//		triangle = t;
//		style = s;
//		color = new float[4];
//		color[0] = style.getColor().getRed()/255.0f;
//		color[1] = style.getColor().getGreen()/255.0f;
//		color[2] = style.getColor().getBlue()/255.0f;
//		color[3] = (float)style.getOpacity();
//	}
//	
//	@Override
//	public void draw(GL gl) {
//		gl.glMatrixMode(GL.GL_MODELVIEW);
//		gl.glLoadIdentity();
//		Vect3D vertex;
//		gl.glColor4f(color[0],color[1],color[2],color[3]); 
//		
//		vertex = triangle.getPoint(0);
//		gl.glVertex3f((float)vertex.getX(), (float)vertex.getY(), (float)vertex.getZ());
//		vertex = triangle.getPoint(1);
//		gl.glVertex3f((float)vertex.getX(), (float)vertex.getY(), (float)vertex.getZ());
//		vertex = triangle.getPoint(2);
//		gl.glVertex3f((float)vertex.getX(), (float)vertex.getY(), (float)vertex.getZ());
//	}
//
//	public float[] getColor() {
//		return color;
//	}
//	
//	public float[] getVertex(int vertex) {
//		float[] ret = new float[3];
//		Vect3D vert = triangle.getPoint(vertex);
//		ret[0] = (float)vert.getX();
//		ret[1] = (float)vert.getY();
//		ret[2] = (float)vert.getZ();
//		return ret;
//	}
//	
//	@Override
//	public int getType() {
//		return JOGLObject.Triangle;
//	}
//	
//	@Override
//	public String toString() {
//		String ret = "";
//		for(int i = 0; i < 3; i++) {
//			ret = ret + "(";
//			float[] tmp = getVertex(i);
//			for(int j = 0; j < 3; j++) {
//				ret = ret + tmp[j];
//				if (j < 2) ret = ret + ",";
//			}
//			ret = ret + ") ";
//		}
//		
//		ret = ret + color[0] + "," + color[1] + "," + color[2];
//		return ret;
//	}
//
//}
