/* Generated by Together */

package edu.swri.swiftvis.plot;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.tree.TreeNode;

import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.plot.util.FillOptions;
import edu.swri.swiftvis.plot.util.FillUser;
import edu.swri.swiftvis.plot.util.FontOptions;
import edu.swri.swiftvis.plot.util.FontUser;
import edu.swri.swiftvis.plot.util.FormattedString;


import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

public class PlotText implements PlotObject,FontUser,FillUser {
    public PlotText(PlotSpec spec) {
        parent=spec;
        text=new FormattedString("Title");
        fontOpts=new FontOptions(this,OptionsData.instance().getLabelFont());
        fillOpts=new FillOptions(this,Color.black);
    }
    
    private PlotText(PlotText c,PlotSpec spec) {
        parent=spec;
        text=new FormattedString(c.text.getValue());
        fontOpts=new FontOptions(this,c.fontOpts.getFont());
        fillOpts=new FillOptions(this,c.fillOpts.getColor());
        rotAngle=c.rotAngle;
        x=c.x;
        y=c.y;
    }

    @Override
    public String toString() {
        return "Text: "+text.getValue();
    }

    /**
     * Draw this object on the provided graphics.
     */
    @Override
    public void draw(Graphics2D g,Rectangle2D bounds,float fontScale){
        g.setFont(fontOpts.getFont(fontScale));
        g.setPaint(fillOpts.getColor());
        AffineTransform oldTrans=g.getTransform();
        g.rotate(rotAngle);
		text.draw(g,(float)(x*bounds.getWidth()*0.01),(float)(y*bounds.getHeight()*0.01),parent.getPlot());
        g.setTransform(oldTrans);
    }

	@Override
    public void print(Graphics2D g,Rectangle2D bounds) {
		draw(g,bounds,1);
	}
	
    /**
     * Returns a panel that can be used to set the properties of this plot
     * object.
     */
    @Override
    public JComponent getPropertiesPanel(){
        if(propPanel==null) {
			propPanel=new JPanel(new BorderLayout());
            JPanel tmpPanel=new JPanel(new GridLayout(6,1));
			JPanel tmpPanel2=new JPanel(new BorderLayout());
            tmpPanel2.add(new JLabel("Text"),BorderLayout.WEST);
            tmpPanel2.add(text.getTextField(new FormattedString.Listener() {
                @Override
                public void valueChanged() {
                    fireRedraw();
                }
            }),BorderLayout.CENTER);
            tmpPanel.add(tmpPanel2);
			// Position
			tmpPanel2=new JPanel(new BorderLayout());
            tmpPanel2.add(new JLabel("X position"),BorderLayout.WEST);
            JTextField field=new JTextField(Double.toString(x));
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { setX(e); }
            } );
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { setX(e); }
            } );
            tmpPanel2.add(field,BorderLayout.CENTER);
            tmpPanel.add(tmpPanel2);
			tmpPanel2=new JPanel(new BorderLayout());
            tmpPanel2.add(new JLabel("Y position"),BorderLayout.WEST);
            field=new JTextField(Double.toString(y));
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { setY(e); }
            } );
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { setY(e); }
            } );
            tmpPanel2.add(field,BorderLayout.CENTER);
            
            tmpPanel.add(tmpPanel2);
			tmpPanel2=new JPanel(new BorderLayout());
			tmpPanel2.add(new JLabel("Angle"),BorderLayout.WEST);
			field=new JTextField(Double.toString(rotAngle));
			field.addActionListener(new ActionListener() {
				@Override
                public void actionPerformed(ActionEvent e) { setAngle(e); }
			} );
			field.addFocusListener(new FocusAdapter() {
				@Override
                public void focusLost(FocusEvent e) { setAngle(e); }
			} );
			tmpPanel2.add(field,BorderLayout.CENTER);
			tmpPanel.add(tmpPanel2);
			
            // Font Options
            JButton button=new JButton("Select Font");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { fontOpts.edit(); }
            } );
            tmpPanel.add(button);
            // Color
            button=new JButton("Select Color");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { fillOpts.edit(); }
            } );
            tmpPanel.add(button);
            propPanel.add(tmpPanel,BorderLayout.NORTH);
        }
        return propPanel;
    }

    @Override
    public void fireRedraw() {
        parent.fireRedraw();
    }

    /**
     * Send a message down the tree that current draw buffers are invalid.
     * This generally happens because data has changed.
     */
    @Override
    public void forceRedraw(){ }

    @Override
    public PlotText copy(PlotSpec p) {
        return new PlotText(this,p);
    }
    
    @Override
    public void relink(Hashtable<GraphElement,GraphElement> linkHash) {}

    @Override
    public void mousePressed(MouseEvent e,double mx,double my) {}
    @Override
    public void mouseReleased(MouseEvent e,double mx,double my) {}
    @Override
    public void mouseClicked(MouseEvent e,double mx,double my) {}
    @Override
    public void mouseMoved(MouseEvent e,double mx,double my) {}
    @Override
    public void mouseDragged(MouseEvent e,double mx,double my) {}
    @Override
    public void keyPressed(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

    //-------------------------------------------------------------------------
    // Methods from TreeNode
    //-------------------------------------------------------------------------

    @Override
    public Enumeration<TreeNode> children() { return null; }

	@Override
    public boolean getAllowsChildren() { return false; }

    @Override
    public TreeNode getChildAt(int index) { return null; }

    @Override
    public int getChildCount() { return 0; }

	@Override
    public int getIndex(TreeNode node) { return -1; }

    @Override
    public TreeNode getParent() { return parent; }

    @Override
    public boolean isLeaf() { return true; }
    
    //-------------------------------------------------------------------------
    // From FontUser
    //-------------------------------------------------------------------------

    @Override
    public void applyFont(FontOptions fo) {
        fireRedraw();
    }

    //-------------------------------------------------------------------------
    // From FontUser
    //-------------------------------------------------------------------------

    @Override
    public void applyFill(FillOptions fo) {
        fireRedraw();
    }

    //-------------------------------------------------------------------------
    // Private methods
    //-------------------------------------------------------------------------

    private void setX(AWTEvent e) {
        JTextField field=(JTextField)e.getSource();
        try {
	        x=Float.parseFloat(field.getText());
        } catch(NumberFormatException ex) {
            field.setText(Double.toString(x));
        }
        fireRedraw();
    }

    private void setY(AWTEvent e) {
        JTextField field=(JTextField)e.getSource();
        try {
	        y=Float.parseFloat(field.getText());
        } catch(NumberFormatException ex) {
            field.setText(Double.toString(y));
        }
        fireRedraw();
    }

	private void setAngle(AWTEvent e) {
		JTextField field=(JTextField)e.getSource();
		try {
			rotAngle=Float.parseFloat(field.getText());
		} catch(NumberFormatException ex) {
			field.setText(Double.toString(rotAngle));
		}
		fireRedraw();
	}

    private FormattedString text;

    private FontOptions fontOpts;

    private FillOptions fillOpts;

    private float rotAngle;
    private float x = 10.0f ;
    private float y = 10.0f ;

    private PlotSpec parent;

    private transient JPanel propPanel;

    private static final long serialVersionUID=45709823746236l;
}
