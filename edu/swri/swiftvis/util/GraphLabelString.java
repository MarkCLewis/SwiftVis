/*
 * Created on Mar 6, 2007
 */
package edu.swri.swiftvis.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.GraphPanel;
import edu.swri.swiftvis.plot.util.FontOptions;
import edu.swri.swiftvis.plot.util.FontUser;
import edu.swri.swiftvis.plot.util.FormattedString;
import edu.swri.swiftvis.plot.util.FormattedString.Listener;

/**
 * This class is used for putting labels on various graph elements.  Since there
 * is a need to have special names on all the graph elements, it is easiest to put
 * the common functionality here.  Basically this includes having a FormattedString,
 * a font to display in, and some helper functions for making it easier to put it
 * in the GUI. 
 * 
 * @author Mark Lewis
 */
public class GraphLabelString implements Serializable,FontUser {
    public GraphLabelString(String l,Paint p) {
        label=new FormattedString(l);
        font=new FontOptions(this,new Font("Serif",Font.PLAIN,15));
        Rectangle2D b=label.getBounds(font.getFont(),null);
        lead=(int)-b.getY();
        bounds=new Rectangle();
        bounds.width=(int)b.getWidth()+10;
        bounds.height=(int)b.getHeight()+10;
        color=(Color)p;
    }
    
    public GraphLabelString(GraphLabelString c) {
        label=new FormattedString(c.label.getValue());
        font=new FontOptions(this,c.font.getFont());
        bounds=new Rectangle(c.bounds);
        color=c.color;
        lead=c.lead;
    }
    
    public void setString(String s) {
        label.setValue(s);
        Rectangle2D b=label.getBounds(font.getFont(),null);
        lead=(int)-b.getY();
        bounds.width=(int)b.getWidth()+10;
        bounds.height=(int)b.getHeight()+10;
    }
    
    public Rectangle2D getBounds(DataSink sink) {
        return label.getBounds(font.getFont(),sink);
    }
    
    public void draw(Graphics2D g) {
        g.setPaint(color);
        g.draw(bounds);
        g.setFont(font.getFont());
        label.draw(g,bounds.x+5,bounds.y+lead+5,null);
    }
    
    public JPanel getFieldPanel(String prompt) {
        if(panel==null) {
            panel=new JPanel(new BorderLayout());
            panel.add(new JLabel(prompt),BorderLayout.WEST);
            panel.add(label.getTextField(new Listener() {
                @Override
                public void valueChanged() { drawOptionsAltered(); }
            }),BorderLayout.CENTER);
            JButton button=new JButton("Font");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { font.edit(); }
            });
            panel.add(button,BorderLayout.EAST);
        }
        return panel;
    }

    public JPanel getAreaPanel(String prompt) {
        if(panel==null) {
            panel=new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder(prompt));
            panel.add(label.getTextArea(new Listener() {
                @Override
                public void valueChanged() { drawOptionsAltered(); }
            }),BorderLayout.CENTER);
            JPanel southPanel=new JPanel(new GridLayout(2,1));
            JButton button=new JButton("Color");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Color tmp=JColorChooser.showDialog(panel,"Select Color",color);
                    if(tmp!=null) {
                        color=tmp;
                        drawOptionsAltered();
                    }
                }
            });
            southPanel.add(button);            
            button=new JButton("Font");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { font.edit(); }
            });
            southPanel.add(button);
            panel.add(southPanel,BorderLayout.SOUTH);
        }
        return panel;
    }
    
    @Override
    public void applyFont(FontOptions fo) {
        drawOptionsAltered();
    }
    
    public void translate(int dx,int dy) {
        bounds.translate(dx,dy);
    }
    
    public void moveTo(int x,int y) {
        bounds.x=x;
        bounds.y=y;
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    @Override
    public String toString() {
        return label.getValue();
    }
    
    private void drawOptionsAltered() {
        Rectangle2D b=label.getBounds(font.getFont(),null);
        lead=(int)-b.getY();
        bounds.width=(int)b.getWidth()+10;
        bounds.height=(int)b.getHeight()+10;
        GraphPanel.instance().getDrawPanel().repaint();
    }

    private FormattedString label;
    private FontOptions font;
    private Color color=Color.black;
    private Rectangle bounds;
    private int lead;
    
    private transient JPanel panel;
    
    private static final long serialVersionUID = -2209092904254452104L;

}
