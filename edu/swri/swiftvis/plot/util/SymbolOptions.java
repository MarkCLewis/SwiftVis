/*
 * Created on May 12, 2006
 */
package edu.swri.swiftvis.plot.util;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;

import edu.swri.swiftvis.plot.PlotTransform;

/**
 * This class keeps track of the options for marker shapes.  These primarily include
 * sizing options.  Markers can be sized in each direction by pixels, fraction of plot
 * size, or scaled coordinates.  There is also an option to make it so that one of the
 * axes is squared with the other.
 * 
 * @author Mark Lewis
 */
public class SymbolOptions implements Serializable {
    public interface SymbolUser {
        void applySymbol();
    }
    
    public SymbolOptions(SymbolUser u) {
        user=u;
    }
    
    public SymbolOptions(SymbolUser u,SymbolOptions o) {
        user=u;
        scaleOption=o.scaleOption;
    }
    
    public void calcWidthHeight(PlotTransform trans,double size) {
        options[scaleOption].setValues(trans,size);
    }
    
    public double getWidth() { return width; }
    
    public double getHeight() { return height; }
    
    public void edit() {
        if(chooser==null) {
            chooser=new ShapeOptionChooser();
        }
        chooser.setVisible(true);
    }
    
    private SymbolUser user;
    private int scaleOption=0;
    private Scaler[] options={new ByPixel(),new ByFractionalXY(),new ByFractionalX(),new ByFractionalY(),
            new ByScaledXY(),new ByScaledX(),new ByScaledY()};
    private transient double width,height;
    private transient ShapeOptionChooser chooser;
    private static final long serialVersionUID = -3670472612499242130L;
    
    private class ShapeOptionChooser extends JFrame {
        public ShapeOptionChooser() {
            setLayout(new GridLayout(options.length+1,1));
            ButtonGroup group=new ButtonGroup();
            for(int i=0; i<options.length; ++i) {
                final int j=i;
                JRadioButton radioButton=new JRadioButton(options[i].toString());
                radioButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        scaleOption=j;
                        user.applySymbol();
                    }
                });
                if(i==scaleOption) radioButton.setSelected(true);
                group.add(radioButton);
                this.add(radioButton);
            }
            JButton button=new JButton("Done");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            this.add(button);
            this.pack();
        }
        private static final long serialVersionUID = -3646993461534521089L;        
    }
    
    private interface Scaler extends Serializable {
        void setValues(PlotTransform trans,double size);
    }
    
    private class ByPixel implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=size;
            height=size;
        }
        @Override
        public String toString() { return "Pixel Size"; }
        private static final long serialVersionUID = 1462625950964469900L;
    }

    private class ByFractionalXY implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=trans.fractionalPrimary(size);
            height=trans.fractionalSecondary(size);
        }
        @Override
        public String toString() { return "Fractional X and Y Size"; }
        private static final long serialVersionUID = 1464675950964469900L;
    }

    private class ByFractionalX implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=trans.fractionalPrimary(size);
            height=width;
        }
        @Override
        public String toString() { return "Fractional X Size"; }
        private static final long serialVersionUID = 146262595036973256L;
    }

    private class ByFractionalY implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=trans.fractionalSecondary(size);
            height=width;
        }
        @Override
        public String toString() { return "Fractional Y Size"; }
        private static final long serialVersionUID = 146262595036973256L;
    }

    private class ByScaledXY implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=trans.scaledPrimary(size);
            height=trans.scaledSecondary(size);
        }
        @Override
        public String toString() { return "Scaled X and Y Size"; }
        private static final long serialVersionUID = 566435764465678L;
    }

    private class ByScaledX implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=trans.scaledPrimary(size);
            height=width;
        }
        @Override
        public String toString() { return "Scaled X Size"; }
        private static final long serialVersionUID = 14634532695065678L;
    }

    private class ByScaledY implements Scaler {
        @Override
        public void setValues(PlotTransform trans,double size) {
            width=trans.scaledSecondary(size);
            height=width;
        }
        @Override
        public String toString() { return "Scaled Y Size"; }
        private static final long serialVersionUID = 14473467254465678L;
    }
}
