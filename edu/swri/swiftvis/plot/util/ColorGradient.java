/* Generated by Together */

package edu.swri.swiftvis.plot.util;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.swri.swiftvis.OptionsData;

/**
* The purpose of this class is to provide a more sophisticated color gradient
* for plotting.  It also provides a convenient method of setting the options
* on the gradient as part of a GUI.
**/
public class ColorGradient implements java.io.Serializable {
    public static void main(String[] args) {
        ColorGradient cg=new ColorGradient();
        cg.edit();
    }

    public ColorGradient() {
        nodes=new ColorNode[2];
        nodes[0]=new ColorNode(Color.black,0.0);
        nodes[1]=new ColorNode(Color.white,1.0);
    }
    
    public ColorGradient(Color c1,Color c2) {
        nodes=new ColorNode[2];
        nodes[0]=new ColorNode(c1,0.0);
        nodes[1]=new ColorNode(c2,1.0);
    }
    
    public ColorGradient(ColorGradient c) {
        nodes=new ColorNode[c.nodes.length];
        for(int i=0; i<nodes.length; ++i) {
            nodes[i]=new ColorNode(c.nodes[i].color,c.nodes[i].value);
        }
    }

    public Color getColor(double value) {
        return getColor(value,1);
    }
    
    public Color getColor(double value,float alphaFactor) {
        float red,green,blue,alpha;
		if(value<=nodes[0].value) {
            red=nodes[0].color.getRed()/255.0f;
            green=nodes[0].color.getGreen()/255.0f;
            blue=nodes[0].color.getBlue()/255.0f;
            alpha=nodes[0].color.getAlpha()/255.0f;
        } else if(value>=nodes[nodes.length-1].value) {
            red=nodes[nodes.length-1].color.getRed()/255.0f;
            green=nodes[nodes.length-1].color.getGreen()/255.0f;
            blue=nodes[nodes.length-1].color.getBlue()/255.0f;
            alpha=nodes[nodes.length-1].color.getAlpha()/255.0f;
        } else {
            int below=0;
            while(nodes[below+1].value<value) below++;
            float fract=(float)((value-nodes[below].value)/(nodes[below+1].value-nodes[below].value));
            red=(nodes[below].color.getRed()*(1.0f-fract)+nodes[below+1].color.getRed()*fract)/255.0f;
            green=(nodes[below].color.getGreen()*(1.0f-fract)+nodes[below+1].color.getGreen()*fract)/255.0f;
            blue=(nodes[below].color.getBlue()*(1.0f-fract)+nodes[below+1].color.getBlue()*fract)/255.0f;
            alpha=(nodes[below].color.getAlpha()*(1.0f-fract)+nodes[below+1].color.getAlpha()*fract)/255.0f;
        }
        if(red>1.0f) red=1.0f;
        if(green>1.0f) green=1.0f;
        if(blue>1.0f) blue=1.0f;
        if(alpha>1.0f) alpha=1.0f;
        if(alphaFactor>=0 && alphaFactor<=1) alpha*=alphaFactor;
        return new Color(red,green,blue,alpha);
    }

    public void edit() {
        if(propFrame==null) {
            propFrame=new JFrame();
            propFrame.getContentPane().setLayout(new BorderLayout());
            JPanel outerPanel=new JPanel(new GridLayout(8,1));
            JPanel innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Minimum Value"),BorderLayout.WEST);
            JTextField field=new JTextField(Double.toString(nodes[0].value));
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { minSet(e); }
            } );
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { minSet(e); }
            } );
            innerPanel.add(field,BorderLayout.CENTER);
            outerPanel.add(innerPanel);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Maximum Value"),BorderLayout.WEST);
            field=new JTextField(Double.toString(nodes[nodes.length-1].value));
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { maxSet(e); }
            } );
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { maxSet(e); }
            } );
            innerPanel.add(field,BorderLayout.CENTER);
            outerPanel.add(innerPanel);
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Default Schemes"),BorderLayout.WEST);
            final JComboBox comboBox=new JComboBox(ColorScheme.getDefaults());
            comboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setToScheme((ColorScheme)comboBox.getSelectedItem());
                }
            });
            innerPanel.add(comboBox,BorderLayout.CENTER);
            outerPanel.add(innerPanel);
            JButton button=new JButton("Save Current as Scheme");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { saveScheme(comboBox); }
            } );
            outerPanel.add(button);
            button=new JButton("Remove Current Scheme");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { removeScheme(comboBox); }
            } );
            outerPanel.add(button);
            button=new JButton("Add New Color");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { addColor(); }
            } );
            outerPanel.add(button);
            button=new JButton("Remove Selected Color");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { removeColor(); }
            } );
            outerPanel.add(button);
            selected=0;
			locationLabel=new JLabel("Selected Value: "+nodes[selected].value);
            outerPanel.add(locationLabel);
            propFrame.getContentPane().add(outerPanel,BorderLayout.NORTH);
            gradChooser=new GradientChooser();
            propFrame.getContentPane().add(gradChooser,BorderLayout.CENTER);
            outerPanel=new JPanel(new BorderLayout());
            colorChooser=new JColorChooser(nodes[selected].color);
            colorChooser.getSelectionModel().addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) { setColor(colorChooser.getColor()); }
            } );
            outerPanel.add(colorChooser,BorderLayout.NORTH);
            JPanel gridPanel=new JPanel(new GridLayout(2,1));
            innerPanel=new JPanel(new BorderLayout());
            innerPanel.add(new JLabel("Transparent"),BorderLayout.WEST);
            innerPanel.add(new JLabel("Opaque"),BorderLayout.EAST);
            alphaSlider=new JSlider(0,255,nodes[selected].color.getAlpha());
            alphaSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) { alphaChanged(); }
            } );
            innerPanel.add(alphaSlider,BorderLayout.CENTER);
            gridPanel.add(innerPanel);
            innerPanel=new JPanel(new FlowLayout());
            button=new JButton("Done");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { propFrame.setVisible(false); }
            });
            innerPanel.add(button);
            gridPanel.add(innerPanel);
            outerPanel.add(gridPanel,BorderLayout.SOUTH);
            propFrame.getContentPane().add(outerPanel,BorderLayout.SOUTH);
            propFrame.setSize(300,750);
        }
        propFrame.setVisible(true);
    }

    public void setBounds(double[] minMax) {
		for(int i=1; i<nodes.length-1; i++) {
			nodes[i].value=minMax[0]+(nodes[i].value-nodes[0].value)*(minMax[1]-minMax[0])/(nodes[nodes.length-1].value-nodes[0].value);
		}
		nodes[0].value=minMax[0];
		nodes[nodes.length-1].value=minMax[1];
		if(gradChooser!=null) gradChooser.repaint();
    }

    public double[] getBounds() {
        double[] ret={nodes[0].value,nodes[nodes.length-1].value};
        return ret;
    }

    private void minSet(AWTEvent e) {
        JTextField field=(JTextField)e.getSource();
        try {
            double tmp=Double.parseDouble(field.getText());
            if(tmp>=nodes[nodes.length-1].value) {
	            field.setText(Double.toString(nodes[0].value));
                JOptionPane.showMessageDialog(field,"The minimum value must be smaller than the maximum.");
                return;
            }
            for(int i=1; i<nodes.length-1; i++) {
                nodes[i].value=tmp+(nodes[i].value-nodes[0].value)*(nodes[nodes.length-1].value-tmp)/(nodes[nodes.length-1].value-nodes[0].value);
            }
            nodes[0].value=tmp;
            gradChooser.repaint();
        } catch(NumberFormatException ex) {
            field.setText(Double.toString(nodes[0].value));
        }
    }

    private void maxSet(AWTEvent e) {
        JTextField field=(JTextField)e.getSource();
        try {
            double tmp=Double.parseDouble(field.getText());
            if(tmp<=nodes[0].value) {
	            field.setText(Double.toString(nodes[nodes.length-1].value));
                JOptionPane.showMessageDialog(field,"The maximum value must be larger than the maximum.");
                return;
            }
            for(int i=1; i<nodes.length-1; i++) {
                nodes[i].value=nodes[0].value+(nodes[i].value-nodes[0].value)*(tmp-nodes[0].value)/(nodes[nodes.length-1].value-nodes[0].value);
            }
            nodes[nodes.length-1].value=tmp;
            gradChooser.repaint();
        } catch(NumberFormatException ex) {
            field.setText(Double.toString(nodes[nodes.length-1].value));
        }
    }
    
    private void setToScheme(ColorScheme cs) {
        double min=nodes[0].value;
        double max=nodes[nodes.length-1].value;
        nodes=new ColorNode[cs.nodes.length];
        for(int i=0; i<nodes.length; ++i) {
            nodes[i]=new ColorNode(cs.nodes[i].color,min+cs.nodes[i].value*(max-min));
        }
        gradChooser.repaint();
    }
    
    private void saveScheme(JComboBox comboBox) {
        String name=JOptionPane.showInputDialog(propFrame,"What do you want to call this gradient?");
        if(name==null || name.length()<1) return;
        ColorScheme.addScheme(name,nodes);
        comboBox.setModel(new DefaultComboBoxModel(ColorScheme.getDefaults()));
        comboBox.setSelectedIndex(ColorScheme.getDefaults().length-1);
    }
    
    private void removeScheme(JComboBox comboBox) {
        int index=comboBox.getSelectedIndex();
        if(index<0) return;
        ColorScheme.removeScheme(index);
        comboBox.setModel(new DefaultComboBoxModel(ColorScheme.getDefaults()));
        comboBox.setSelectedIndex(ColorScheme.getDefaults().length-1);
    }

    private void addColor() {
		ColorNode[] newNodes=new ColorNode[nodes.length+1];
        for(int i=0; i<nodes.length; i++) newNodes[i]=nodes[i];
        double value;
        int newSelect;
        if(selected<nodes.length-1) {
            value=0.5*(nodes[selected].value+nodes[selected+1].value);
            newSelect=selected+1;
        } else {
            value=0.5*(nodes[selected].value+nodes[selected-1].value);
            newSelect=selected;
        }
        newNodes[nodes.length]=new ColorNode(Color.white,value);
        nodes=newNodes;
        Arrays.sort(nodes);
        selected=newSelect;
        gradChooser.repaint();
    }

    private void removeColor() {
        if(selected==0 || selected==nodes.length-1) {
            JOptionPane.showMessageDialog(gradChooser,"You can't remove the end colors.");
            return;
        }
		ColorNode[] newNodes=new ColorNode[nodes.length-1];
        for(int i=0; i<newNodes.length; i++) newNodes[i]=(i<selected)?nodes[i]:nodes[i+1];
        nodes=newNodes;
        gradChooser.repaint();
    }

    private void setColor(Color col) {
        Color tmp=new Color(col.getRed(),col.getGreen(),col.getBlue(),alphaSlider.getValue());
        if(nodes[selected].color.equals(tmp)) return;
        nodes[selected].color=tmp;
        gradChooser.repaint();
    }

    private void alphaChanged() {
        Color oCol=nodes[selected].color;
        Color tmp=new Color(oCol.getRed(),oCol.getGreen(),oCol.getBlue(),alphaSlider.getValue());
        if(nodes[selected].color.equals(tmp)) return;
        nodes[selected].color=tmp;
        colorChooser.setColor(nodes[selected].color);
        gradChooser.repaint();
    }

    private ColorNode[] nodes;
    
    private transient JFrame propFrame;
    private transient JColorChooser colorChooser;
    private transient GradientChooser gradChooser;
    private transient JLabel locationLabel;
    private transient JSlider alphaSlider;
    private transient int selected;

    private static final long serialVersionUID=465845683475l;

    private static class ColorNode implements Comparable<ColorNode>,java.io.Serializable {
        public ColorNode(Color c,double v) {
            color=c;
            value=v;
        }
        @Override
        public int compareTo(ColorNode o) {
			double diff=value-o.value;
            if(diff<0.0) return -1;
            if(diff>0.0) return 1;
            return 0;
        }
        @Override
        public boolean equals(Object o) {
            if(o instanceof ColorNode) return compareTo((ColorNode)o)==0;
            return false;
        }
        public Color color;
        public double value;
        private static final long serialVersionUID=1119834623758l;
    }

    private static class ColorScheme {
        public static ColorScheme[] getDefaults() {
            if(schemes==null) {
                load();
            }
            return schemes;
        }
        public static void addScheme(String name,ColorNode[] nodes) {
            ColorScheme[] tmp=new ColorScheme[schemes.length+1];
            for(int i=0; i<schemes.length; ++i) {
                tmp[i]=schemes[i];
            }
            tmp[tmp.length-1]=new ColorScheme(name,nodes);
            schemes=tmp;
            save();
        }
        public static void removeScheme(int index) {
            ColorScheme[] tmp=new ColorScheme[schemes.length-1];
            for(int i=0; i<tmp.length; ++i) {
                if(i<index) tmp[i]=schemes[i];
                else tmp[i]=schemes[i+1];
            }
            schemes=tmp;
            save();
        }
        private ColorScheme(String n,ColorNode[] cn) {
            name=n;
            nodes=cn;
        }
        private ColorScheme() {}
        private static void save() {
            try {
                PrintStream ps=new PrintStream(new FileOutputStream(new File(OptionsData.getSwiftVisDir(),"ColorGradients.txt")));
                for(int i=0; i<schemes.length; ++i) {
                    ps.print(schemes[i].name+"|");
                    for(int j=0; j<schemes[i].nodes.length; ++j) {
                        if(j>0) ps.print(" ");
                        ps.print(schemes[i].nodes[j].color.getRed()+" "+
                                schemes[i].nodes[j].color.getGreen()+" "+
                                schemes[i].nodes[j].color.getBlue()+" "+
                                schemes[i].nodes[j].value);
                    }
                    ps.println();
                }
                ps.close();
            } catch(IOException e) {
                System.out.println("Couldn't save gradients.");
                e.printStackTrace();
            }
        }
        private static void load() {
            BufferedReader br=null;
            try {
                br=new BufferedReader(new FileReader(new File(OptionsData.getSwiftVisDir(),"ColorGradients.txt")));
                java.util.List<ColorScheme> list=new ArrayList<ColorScheme>();
                String line=br.readLine();
                while(line!=null && line.length()>1) {
                    String[] nn=line.split("\\|");
                    if(nn.length!=2) {
                        JOptionPane.showMessageDialog(null,"The color gradient file had an invalid format.  It will be renamed to ColorGradients.old and a new default will be created.");
                        File oldFile=new File(OptionsData.getSwiftVisDir(),"ColorGradients.txt");
                        boolean b=oldFile.renameTo(new File(OptionsData.getSwiftVisDir(),"ColorGradients.old"));
                        if(!b) {
                            System.err.println("Couldn't rename old gradients file.");
                        }
                        createDefault();
                        return;
                    }
                    String[] parts=nn[1].split(" ");
                    if(parts.length%4!=0) {
                        System.out.println("Bad length: resetting gradients.");
                        createDefault();
                        return;
                    }
                    ColorScheme ns=new ColorScheme();
                    ns.name=nn[0];
                    ns.nodes=new ColorNode[(parts.length)/4];
                    for(int i=0; i<ns.nodes.length; ++i) {
                        ns.nodes[i]=new ColorNode(new Color(Integer.parseInt(parts[i*4]),
                                Integer.parseInt(parts[1+i*4]),Integer.parseInt(parts[2+i*4])),
                                Double.parseDouble(parts[3+i*4]));
                    }
                    list.add(ns);
                    line=br.readLine();
                }
                schemes=new ColorScheme[list.size()];
                for(int i=0; i<schemes.length; ++i) {
                    schemes[i]=list.get(i);
                }
            } catch(IOException e) {
                e.printStackTrace();
                createDefault();
            } finally {
                try {
                    if(br!=null) br.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private static void createDefault() {
            schemes=new ColorScheme[13];
            ColorNode[] c0={new ColorNode(Color.black,0.0),new ColorNode(Color.white,1.0)};
            schemes[0]=new ColorScheme("Black to White",c0);
            ColorNode[] c1={new ColorNode(Color.white,0.0),new ColorNode(Color.black,1.0)};
            schemes[1]=new ColorScheme("White to Black",c1);
            ColorNode[] c2={new ColorNode(Color.black,0.0),new ColorNode(Color.black,1.0)};
            schemes[2]=new ColorScheme("Black to Black",c2);
            ColorNode[] c3={new ColorNode(Color.black,0.0),new ColorNode(Color.red,1.0)};
            schemes[3]=new ColorScheme("Black to Red",c3);
            ColorNode[] c4={new ColorNode(Color.black,0.0),new ColorNode(Color.green,1.0)};
            schemes[4]=new ColorScheme("Black to Green",c4);
            ColorNode[] c5={new ColorNode(Color.black,0.0),new ColorNode(Color.blue,1.0)};
            schemes[5]=new ColorScheme("Black to Blue",c5);
            ColorNode[] c6={new ColorNode(Color.red,0.0),new ColorNode(Color.black,1.0)};
            schemes[6]=new ColorScheme("Red to Black",c6);
            ColorNode[] c7={new ColorNode(Color.green,0.0),new ColorNode(Color.black,1.0)};
            schemes[7]=new ColorScheme("Green to Black",c7);
            ColorNode[] c8={new ColorNode(Color.blue,0.0),new ColorNode(Color.black,1.0)};
            schemes[8]=new ColorScheme("Blue to Black",c8);
            ColorNode[] c9={new ColorNode(Color.white,0.0),new ColorNode(Color.red,0.000001),
                    new ColorNode(Color.orange,1.0/6.0),new ColorNode(Color.yellow,2.0/6.0),
                    new ColorNode(Color.green,3.0/6.0),new ColorNode(Color.blue,4.0/6.0),
                    new ColorNode(new Color(255,0,255),5.0/6.0),new ColorNode(Color.black,1.0)};
            schemes[9]=new ColorScheme("Spectrum w/ White Zero",c9);
            ColorNode[] c10={new ColorNode(Color.white,0.0),new ColorNode(Color.black,0.000001),
                    new ColorNode(new Color(255,0,255),1.0/6.0),new ColorNode(Color.blue,2.0/6.0),
                    new ColorNode(Color.green,3.0/6.0),new ColorNode(Color.yellow,4.0/6.0),
                    new ColorNode(Color.orange,5.0/6.0),new ColorNode(Color.red,1.0)};
            schemes[10]=new ColorScheme("Spectrum 2 w/ White Zero",c10);
            ColorNode[] c11={new ColorNode(Color.red,0.0),
                    new ColorNode(Color.orange,1.0/6.0),new ColorNode(Color.yellow,2.0/6.0),
                    new ColorNode(Color.green,3.0/6.0),new ColorNode(Color.blue,4.0/6.0),
                    new ColorNode(new Color(255,0,255),5.0/6.0),new ColorNode(Color.black,1.0)};
            schemes[11]=new ColorScheme("Spectrum",c11);
            ColorNode[] c12={new ColorNode(Color.black,0.0),
                    new ColorNode(new Color(255,0,255),1.0/6.0),new ColorNode(Color.blue,2.0/6.0),
                    new ColorNode(Color.green,3.0/6.0),new ColorNode(Color.yellow,4.0/6.0),
                    new ColorNode(Color.orange,5.0/6.0),new ColorNode(Color.red,1.0)};
            schemes[12]=new ColorScheme("Spectrum 2",c12);
            save();
        }
        @Override
        public String toString() { return name; }
        public String name;

        public ColorNode[] nodes;

        private static ColorScheme[] schemes;

    }

    private class GradientChooser extends JPanel implements MouseListener,MouseMotionListener {
        public GradientChooser() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e) { }

        @Override
        public void mouseEntered(MouseEvent e) {
            setSelection(e.getX());
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setSelection(e.getX());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            setSelectedPosition(e.getX());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            setSelectedPosition(e.getX());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            setSelectedPosition(e.getX());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            setSelection(e.getX());
        }

        @Override
        protected void paintComponent(Graphics gr) {
            if(img==null || img.getWidth()!=getWidth() || img.getHeight()!=getHeight()) {
                img=new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
            }
            Graphics2D g=img.createGraphics();
            
            for(int i=0; i<getWidth()/20+1; i++) {
	            for(int j=0; j<getHeight()/20+1; j++) {
					if((i+j)%2==0) {
						g.setPaint(Color.gray);
					} else {
						g.setPaint(Color.white);
					}
					g.fill(new Rectangle2D.Double(i*20,j*20,20,20));
                }
            }
			for(int i=0; i<getWidth(); i++) {
                g.setPaint(getColor(nodes[0].value+i*(nodes[nodes.length-1].value-nodes[0].value)/getWidth()));
                g.fill(new Rectangle2D.Double(i,15,i+1,getHeight()));
            }
            for(int i=0; i<nodes.length; i++) {
                double center=(nodes[i].value-nodes[0].value)*getWidth()/(nodes[nodes.length-1].value-nodes[0].value);
                Ellipse2D ellipse=new Ellipse2D.Double(center-3,1,6,13);
                g.setPaint(nodes[i].color);
                g.fill(ellipse);
                if(i==selected) {
                    g.setPaint(Color.red);
                } else {
                    g.setPaint(Color.black);
                }
                g.draw(ellipse);
            }
            
            gr.drawImage(img,0,0,null);
        }

        private double convertToValue(int pos) {
            return nodes[0].value+pos*(nodes[nodes.length-1].value-nodes[0].value)/getWidth();
        }

        private void setSelection(int pos) {
            double dPos=convertToValue(pos);
            selected=0;
            double minDist=Math.abs(dPos-nodes[0].value);
            for(int i=1; i<nodes.length; i++) {
                double dist=Math.abs(dPos-nodes[i].value);
                if(dist<minDist) {
                    minDist=dist;
                    selected=i;
                }
            }
			alphaSlider.setValue(nodes[selected].color.getAlpha());
            colorChooser.setColor(nodes[selected].color);
            locationLabel.setText("Selected Value: "+Double.toString(nodes[selected].value));
            repaint();
        }

        private void setSelectedPosition(int pos) {
            if(selected==0 || selected==nodes.length-1) return;
            nodes[selected].value=convertToValue(pos);
            Arrays.sort(nodes);
            repaint();
        }

        private BufferedImage img;
        private static final long serialVersionUID=509872305623467l;
    }
}