/* Generated by Together */

package edu.swri.swiftvis;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
* This class is intended to do a simple display of an image.  It can be used to
* help with the debugging of pieces of code that just do drawings of things.
**/
public class GraphicsTester extends JPanel {
    public GraphicsTester(int width,int height) {
        img=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        JFrame frame=new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new JScrollPane(this),BorderLayout.CENTER);
        JButton button=new JButton("Repaint");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { repaint(); }
        } );
        frame.getContentPane().add(button,BorderLayout.SOUTH);
        frame.setSize(width+20,height+50);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { System.exit(0); }
        } );
        frame.setVisible(true);
    }

    public Image getImage() {
        return img;
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(img.getWidth(),img.getHeight()); }

    @Override
    protected void paintComponent(Graphics g) {
        ((Graphics2D)g).drawRenderedImage(img,new AffineTransform());
    }

    private BufferedImage img;
    private static final long serialVersionUID=8234609867l;
}