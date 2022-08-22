/*
 * Created on Oct 10, 2006
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.OptionsData;
import edu.swri.swiftvis.util.BinaryInput;
import edu.swri.swiftvis.util.EditableBoolean;
import edu.swri.swiftvis.util.EditableDouble;
import edu.swri.swiftvis.util.EditableInt;

public class MercurySource extends AbstractSource {
    public MercurySource() {
    }
    
    public MercurySource(MercurySource c,List<GraphElement> l) {
        super(c,l);
        oneIn=new EditableInt(c.oneIn.getValue());
        useRange=new EditableBoolean(c.useRange.getValue());
        startRange=new EditableDouble(c.startRange.getValue());
        endRange=new EditableDouble(c.endRange.getValue());
        maxAllowed=c.maxAllowed;
        offset=c.offset;
        fileToRead=c.fileToRead;
        totalElements=c.totalElements;
    }

    @Override
    protected void redoAllElements() {
        if(fileToRead==null) return;
        int[] precisionArray={2,4,7};
        int lenin=0;
        try {
            bis=new BinaryInput(new DataInputStream(new FileInputStream(fileToRead)));
            double[] el_18,el_21;
            double[][] spin;
            double mcen;
            while(true) {
                byte check=bis.readByte(),style=bis.readByte(),type=bis.readByte();
                if((char)type=='a') {
                    int algor=bis.readFortranInt(2);
                    byte[] cc=new byte[62];
                    bis.readFully(cc);
                    int precision=bis.readFortranInt(1);
                    System.out.println(algor+" "+precision);
                    double time=convertArray(cc,0);
                    int nbig=(int)(0.5+convertBoundArray(cc,8,3,0,11239424));
                    int nsml=(int)(0.5+convertBoundArray(cc,11,3,0,11239424));
                    System.out.println(time+" "+nbig+" "+nsml);
                    mcen=convertArray(cc,14);
                    double[] jcen={convertArray(cc,22),convertArray(cc,30),convertArray(cc,38)};
                    double rcen=convertArray(cc,46);
                    double rmax=convertArray(cc,54);
                    double rfac=Math.log10(rmax/rcen);
                    System.out.println(mcen+" "+jcen[0]+" "+jcen[1]+" "+jcen[2]+" "+rcen+" "+rmax);
                    byte[][] c=new byte[nbig+nsml][51];
                    for(int i=0; i<nbig+nsml; ++i) {
                        bis.readFully(c[i]);
                    }
                    int nchar=precisionArray[precision-1];
                    lenin=3+6*nchar;
                    String[] id=new String[nbig+nsml];
                    el_18=new double[nbig+nsml];
                    el_21=new double[nbig+nsml];
                    spin=new double[nbig+nsml][3];
                    for(int i=0; i<nbig+nsml; ++i) {
                        int k=(int)(0.5+convertBoundArray(c[i],8,3,0,11239424));
                        id[k]=new String(c[i],3,7);
                        el_18[i]=convertArray(c[i],11);
                        spin[i][0]=convertArray(c[i],19);
                        spin[i][1]=convertArray(c[i],27);
                        spin[i][2]=convertArray(c[i],35);
                        el_21[i]=convertArray(c[i],43);
                    }                
                } else if((char)type=='b') {
                    byte[] cc=new byte[14];
                    bis.readFully(cc);
                    double time=convertArray(cc,0);
                    int nbig=(int)(0.5+convertBoundArray(cc,8,3,0,11239424));
                    int nsml=(int)(0.5+convertBoundArray(cc,11,3,0,11239424));
                    int nbod=nbig+nsml;
                    byte[][] c=new byte[nbig+nsml][lenin];
                    for(int i=0; i<nbod; ++i) {
                        bis.readFully(c[i]);
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }        
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel outerPanel=new JPanel(new BorderLayout());
        JPanel panel=new JPanel(new GridLayout(3,1));
        fileLabel=new JLabel((fileToRead==null)?"No File Selected":fileToRead.getAbsolutePath());
        panel.add(fileLabel);
        JButton button=new JButton("Select File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFile();
                if(fileToRead!=null) fileLabel.setText(fileToRead.getAbsolutePath());
            }
        } );
        panel.add(button);
        button=new JButton("Read File");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        } );
        panel.add(button);
        outerPanel.add(panel,BorderLayout.NORTH);
        propPanel.add("Settings",outerPanel);
        
        outerPanel=new JPanel(new BorderLayout());
        panel=new JPanel(new GridLayout(5,1));
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Take 1 timestep in"),BorderLayout.WEST);
        innerPanel.add(oneIn.getTextField(null),BorderLayout.CENTER);
        panel.add(innerPanel);        
        panel.add(useRange.getCheckBox("Select time range?",null));
        EditableDouble.Listener rangeListener=new EditableDouble.Listener() {
            @Override
            public void valueChanged() { useRange.setValue(true); }
        };
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Selection Start Time"),BorderLayout.WEST);
        innerPanel.add(startRange.getTextField(rangeListener),BorderLayout.CENTER);
        panel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Selection End Time"),BorderLayout.WEST);
        innerPanel.add(endRange.getTextField(rangeListener),BorderLayout.CENTER);
        panel.add(innerPanel);
        panel.add(new JLabel("You must read the file for changes to take effect."));
        outerPanel.add(panel,BorderLayout.NORTH);
        propPanel.add("Thinning",outerPanel);
    }

    @Override
    public int getNumParameters(int stream) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumValues(int stream) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getValueDescription(int stream, int which) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        return "Mercury Source";
    }
    
    public static String getTypeDescription() {
        return "Mercury Source";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new MercurySource(this,l);
    }
    
    private void selectFile() {
        JFileChooser fileChooser=new JFileChooser(OptionsData.instance().getLastDir());
        if(fileChooser.showOpenDialog(propPanel)==JFileChooser.CANCEL_OPTION) return;
        fileToRead=fileChooser.getSelectedFile();
        OptionsData.instance().setLastDir(fileToRead.getParentFile());        
    }

    private double convertArray(byte[] c,int offset) {
        double mant=convertBoundArray(c,offset,7,0,1);
        mant=mant*2-1;
        double exp=(c[offset+7]&0xff)-32-112;
        //System.out.println("exp="+exp);
        return mant*Math.pow(10,exp);
    }

    private double convertBoundArray(byte[] c,int offset,int len,double min,double max) {
        double ret=0;
        for(int i=len-1; i>=0; --i) {
            ret=(ret+(c[i+offset]&0xff)-32)/224;
            //System.out.println(ret+" "+((char)(c[i+offset]&0xff))+" "+c[i+offset]+" "+((c[i+offset]&0xff)-32));
        }
        return min+ret*(max-min);
    }

    private EditableInt oneIn=new EditableInt(1);
    private EditableBoolean useRange=new EditableBoolean(false);
    private EditableDouble startRange=new EditableDouble(0.0);
    private EditableDouble endRange=new EditableDouble(1e10);
    
    // Data for handling large files.
    
    /** The number of elements to hold in memory at one time.  Will round up to get
     * a full timestep. */
    private int maxAllowed;

    private int offset;

    private File fileToRead;

    private int totalElements;

    private transient BinaryInput bis;
    private transient RandomAccessFile raf;
    private transient JLabel fileLabel;
    
    private static final double K2=2.959122082855911e-4;

    private static final long serialVersionUID = -6482272093888928741L;
}
