/*
 * Created on Sep 24, 2005
 */
package edu.swri.swiftvis.sinks;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.PlotListener;
import edu.swri.swiftvis.plot.PlotTransform;

public class StatSinkListener extends AbstractSink implements PlotListener {
    public StatSinkListener() {}
    
    public StatSinkListener(StatSinkListener c,List<GraphElement> l) {
        super(c,l);
        stats=new Object[c.stats.length][c.stats[0].length][c.stats[0][0].length];
        for(int i=0; i<stats.length; ++i) {
            for(int j=0; j<stats[i].length; ++j) {
                for(int k=0; k<stats[i][j].length; ++i) {
                    stats[i][j][k]=c.stats[i][j][k];
                }
            }
        }
    }

    @Override
    protected void abstractRedo() {
        stats=new Object[sources.size()][][];
        for(int i=0; i<stats.length; ++i) {
            int totRows=0;
            for(int s=0; s<sources.get(i).getNumStreams(); ++s) {
                totRows+=sources.get(i).getNumParameters(s)+sources.get(i).getNumValues(s);
            }
            stats[i]=new Object[totRows][colNames.length];
            int row=0;
            for(int stream=0; stream<sources.get(i).getNumStreams(); ++stream) {
                int p=sources.get(i).getNumParameters(stream);
                DataSource s=sources.get(i);
                for(int j=0; j<p; ++j) {
                    stats[i][row][0]=s.toString();
                    stats[i][row][1]=stream;
                    stats[i][row][2]="p["+j+"]";
                    stats[i][row][3]=s.getNumElements(stream);
                    float min=1e30f,max=-1e30f,mean=0,rms=0;
                    for(int k=0; k<s.getNumElements(0); ++k) {
                        int par=s.getElement(k,stream).getParam(j);
                        if(par<min) min=par;
                        if(par>max) max=par;
                        mean+=par;                    
                    }
                    if(s.getNumElements(stream)>0) mean/=s.getNumElements(stream);
                    for(int k=0; k<s.getNumElements(stream); ++k) {
                        int par=s.getElement(k,stream).getParam(j);
                        float diff=par-mean;
                        rms+=diff*diff;
                    }
                    if(s.getNumElements(stream)>0) rms=(float)Math.sqrt(rms/s.getNumElements(stream));
                    stats[i][row][4]=min;
                    stats[i][row][5]=max;
                    stats[i][row][6]=mean;
                    stats[i][row][7]=rms;
                    row++;
                }
                for(int j=0; j<s.getNumValues(stream); ++j) {
                    stats[i][row][0]=s.toString();
                    stats[i][row][1]=stream;
                    stats[i][row][2]="v["+j+"]";
                    stats[i][row][3]=s.getNumElements(stream);
                    float min=1e30f,max=-1e30f,mean=0,rms=0;
                    for(int k=0; k<s.getNumElements(stream); ++k) {
                        float val=s.getElement(k,stream).getValue(j);
                        if(val<min) min=val;
                        if(val>max) max=val;
                        mean+=val;                    
                    }
                    if(s.getNumElements(stream)>0) mean/=s.getNumElements(stream);
                    for(int k=0; k<s.getNumElements(stream); ++k) {
                        float val=s.getElement(k,stream).getValue(j);
                        float diff=val-mean;
                        rms+=diff*diff;
                    }
                    if(s.getNumElements(stream)>0) rms=(float)Math.sqrt(rms/s.getNumElements(stream));
                    stats[i][row][4]=min;
                    stats[i][row][5]=max;
                    stats[i][row][6]=mean;
                    stats[i][row][7]=rms;
                    row++;
                }
            }
        }
        int index=-1;
        if(inputList!=null) index=inputList.getSelectedIndex();
        if(statTable!=null && index>=0 && stats.length>0) {
            statTable.setModel(new DefaultTableModel(stats[index],colNames));
        }
        if(inputList!=null) {
            inputList.setListData(sources.toArray());
            if(index>=0 && index<sources.size()) inputList.setSelectedIndex(index);
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(1,2));
        xLabel=new JLabel("Primary: None");
        northPanel.add(xLabel);
        yLabel=new JLabel("Secondary: None");
        northPanel.add(yLabel);
        panel.add(northPanel,BorderLayout.NORTH);
        JPanel centerPanel=new JPanel(new GridLayout(3,1));
        inputList=new JList(sources.toArray());
        inputList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index=inputList.getSelectedIndex();
                if(statTable!=null && index>=0 && stats.length>0) {
                    statTable.setModel(new DefaultTableModel(stats[index],colNames));
                }                
            }
        });
        centerPanel.add(new JScrollPane(inputList));
        if(stats!=null && stats.length>0) {
            inputList.setSelectedIndex(0);
            statTable=new JTable(stats[0],colNames);
        } else {
            statTable=new JTable(new Object[0][colNames.length],colNames);
        }
        centerPanel.add(new JScrollPane(statTable));
        outputArea=new JTextArea();
        setTextArea();
        centerPanel.add(new JScrollPane(outputArea));
        panel.add(centerPanel,BorderLayout.CENTER);
        JPanel southPanel=new JPanel(new GridLayout(2,1));
        JButton button=new JButton("Reprocess Inputs");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abstractRedo();
            }
        });
        southPanel.add(button);
        button=new JButton("Write Table to File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                writeToFile();
            }
        });
        southPanel.add(button);
        panel.add(southPanel,BorderLayout.SOUTH);
        propPanel.add("Stats",panel);
    }

    @Override
    public void mousePressed(double v1, double v2, MouseEvent e) {
        lastXPressed=v1;
        lastYPressed=v2;
        setTextArea();
    }

    @Override
    public void mouseReleased(double v1, double v2, MouseEvent e) {
        lastXReleased=v1;
        lastYReleased=v2;
        setTextArea();
    }

    @Override
    public void mouseClicked(double v1, double v2, MouseEvent e) {
    }

    @Override
    public void mouseMoved(double v1, double v2, MouseEvent e) {
        if(xLabel!=null) xLabel.setText("Primary: "+v1);
        if(yLabel!=null) yLabel.setText("Secondary: "+v2);
    }

    @Override
    public void mouseDragged(double v1, double v2, MouseEvent e) {
        if(xLabel!=null) xLabel.setText("Primary: "+v1);
        if(yLabel!=null) yLabel.setText("Secondary: "+v2);
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public Shape getSelectionRegion(PlotTransform trans) {
        return null;
    }

    @Override
    public String getDescription() {
        return "Statistics Sink";
    }

    public static String getTypeDescription() {
        return "Statistics Sink";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new StatSinkListener(this,l);
    }
    
    private void setTextArea() {
        if(outputArea==null)  return;
        outputArea.setText("Pressed Location: ("+lastXPressed+","+lastYPressed+")\n"+
                "Released Location: ("+lastXReleased+","+lastYReleased+")\n"
                );
    }
    
    private void writeToFile() {
        JFileChooser chooser=new JFileChooser(OptionsData.instance().getLastDir());
        if(chooser.showSaveDialog(propPanel)==JFileChooser.APPROVE_OPTION) {
            File file=chooser.getSelectedFile();
            OptionsData.instance().setLastDir(file.getParentFile());
            try {
                PrintStream ps=new PrintStream(new FileOutputStream(file));
                for(int i=0; i<colNames.length; ++i) {
                    ps.print(colNames[i]+" ");
                }
                ps.println();
                for(int i=0; i<stats.length; ++i) {
                    for(int j=0; j<stats[i].length; ++j) {
                        for(int k=0; k<stats[i][j].length; ++k) {
                            if(k>0) ps.print('\t');
                            ps.print(stats[i][j][k]);
                        }
                        ps.println();
                    }
                    ps.println();
                }
                ps.close();
            } catch(IOException e) {
                JOptionPane.showMessageDialog(propPanel,"There was an exception writing the file.");
                e.printStackTrace();
            }
        }
    }

    private Object[][][] stats;
    private transient double lastXPressed;
    private transient double lastXReleased;
    private transient double lastYPressed;
    private transient double lastYReleased;
    private transient JTable statTable; 
    private transient JTextArea outputArea;
    private transient JLabel xLabel;
    private transient JLabel yLabel;
    private transient JList inputList;
    private static final long serialVersionUID=77352353337347l;
    private static final String[] colNames={"Source","Stream","Param/Value","# elems","min","max","mean","RMS"};
}
