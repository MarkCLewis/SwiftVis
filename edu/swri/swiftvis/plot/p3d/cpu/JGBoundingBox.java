package edu.swri.swiftvis.plot.p3d.cpu;

// JGBoundingBox.java

public class JGBoundingBox {
    public double getFront() { return front; }
    public double getBack() { return back; }
    public double getLeft() { return left; }
    public double getRight() { return right; }
    public double getTop() { return top; }
    public double getBottom() { return bottom; }
    public void setFront(double f) { front=f; }
    public void setBack(double b) { back=b; }
    public void setLeft(double l) { left=l; }
    public void setRight(double r) { right=r; }
    public void setTop(double t) { top=t; }
    public void setBottom(double b) { bottom=b; }
	private double front,back,left,right,top,bottom;
}