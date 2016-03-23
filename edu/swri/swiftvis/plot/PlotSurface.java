/* Generated by Together */

package edu.swri.swiftvis.plot;

import javax.swing.*;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
* The purpose of this class is to provide a standard interface and functionality
* for plotting.  This will include the ability to show axes and their labels as
* well as to have things scaled in various ways.  The most basic functionality
* for it should be to do scatter plots with various markers and colors.  However,
* additonal functionality for things like showing orbits could also be added.
* It also has the ability to do line plots so that things like streamline plots
* could be added.
*
* There are two main strengths of this class.  First, it consolidates all of the
* axis drawing code into one place.  Second, it allows many configuration
* abilities so that outside code can have things like sliders that can be used
* to modify what parameters are used in the plotting.  It is possible that not
* all plots will use this class for the plotting area, but most should be able
* to.  The different plots then will provide different options for how the
* incoming data is directed to this class and how the user can interact with
* that data.
**/
public class PlotSurface extends JPanel implements MouseListener,MouseMotionListener,KeyListener {
    public PlotSurface(PlotSpec theSpec) {
        spec=theSpec;
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
    }
    
    public void redraw() {
    	if (!spec.getPlot().getPlotFrame().isVisible()) {
    		blocked=true;
    		return;
    	}
    	blocked=false;
    	spec.getPlot().getPlotFrame().setTitle("Drawing... "+spec.getPlot());
    	if (image==null || image.getWidth()!=getWidth() || image.getHeight()!=getHeight()) {
    		image=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
    	}
    	spec.draw(image.createGraphics(),new Rectangle2D.Double(0,0,getWidth(),getHeight()),1);
    	repaint();
    	spec.getPlot().getPlotFrame().setTitle(spec.getPlot().toString());
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        requestFocus();
        spec.mousePressed(e,100.0*e.getX()/getWidth(),100.0*e.getY()/getHeight());
    }
    @Override
    public void mouseReleased(MouseEvent e) { spec.mouseReleased(e,100.0*e.getX()/getWidth(),100.0*e.getY()/getHeight()); }
    @Override
    public void mouseClicked(MouseEvent e) { spec.mouseClicked(e,100.0*e.getX()/getWidth(),100.0*e.getY()/getHeight()); }
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) { spec.mouseMoved(e,100.0*e.getX()/getWidth(),100.0*e.getY()/getHeight()); }
    @Override
    public void mouseDragged(MouseEvent e) { spec.mouseDragged(e,100.0*e.getX()/getWidth(),100.0*e.getY()/getHeight()); }
    @Override
    public void keyPressed(KeyEvent e) { spec.keyPressed(e); }
    @Override
    public void keyReleased(KeyEvent e) { spec.keyReleased(e); }
    @Override
    public void keyTyped(KeyEvent e) { spec.keyTyped(e); }

    @Override
    protected void paintComponent(Graphics gr) {
        Graphics2D g=(Graphics2D)gr;
        g.setPaint(Color.white);
        Rectangle2D bounds=new Rectangle2D.Double(0,0,getWidth(),getHeight());
        g.fill(bounds);
        if (blocked || image==null || image.getWidth()!=getWidth() || image.getHeight()!=getHeight()) {
        	if(drawingThread==null) {
	        	drawingThread=new Thread(new Runnable () {
	        		@Override
                    public void run() {
                        try {
                            redraw();
                        } finally {
    	        			synchronized(PlotSurface.this) {
    	        				drawingThread=null;
    	        				PlotSurface.this.notifyAll();
    	        			}
                        }
	        		}
	        	});
	        	drawingThread.start();
        	} else if(waitingThread==null) {
        		waitingThread=new Thread(new Runnable() {
        			@Override
                    public void run() {
        				synchronized(PlotSurface.this) {
        					while(drawingThread!=null) {
        						try {
        							PlotSurface.this.wait();
        						} catch(InterruptedException e) {
        							e.printStackTrace();
        						}
        					}
        					waitingThread=null;
        					repaint();
        				}
        			}
        		});
        		waitingThread.start();
        	}
        }
        if(image!=null) g.drawImage(image,null,0,0);
//        spec.draw(g,bounds);  // inserted into redraw() method
    }

    private PlotSpec spec;
    private BufferedImage image;
    private boolean blocked;
    private Thread drawingThread;
    private Thread waitingThread;

    private static final long serialVersionUID=6483482568097l;
}
