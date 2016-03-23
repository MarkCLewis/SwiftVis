/*
 * Created on Oct 23, 2007
 */
package edu.swri.swiftvis.sources;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.EditableInt;
import edu.swri.swiftvis.util.EditableString;

/**
 * This source is used to read information from a socket.  This allows other programs
 * to communicate to a SwiftVis session in real time.  The communication occurs over a
 * DataInputStream.
 * 
 * The format of the communication is as follows.  It begins with the number of elements
 * in the data set as an int.  Each elements is sent as a number of parameters (int), the
 * parameters (ints), a number of values (int), and the values (floats).
 * 
 * @author Mark Lewis
 */
public class SocketSource extends AbstractSource {
    public SocketSource() {}
    
    private SocketSource(SocketSource c,List<GraphElement> l) {
        super(c,l);
        for(String s:c.paramNames) {
            paramNames.add(s);
        }
        for(String s:c.valueNames) {
            valueNames.add(s);
        }
        port=new EditableInt(c.port.getValue());
    }

    @Override
    protected void redoAllElements() {
        dataVect.addAll(readList);
        calcNumParamsAndValue();
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel socketPanel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(5,1));
        northPanel.add(host.getLabeledTextField("Socket Host",null));
        northPanel.add(port.getLabeledTextField("Socket Port",null));
        northPanel.add(booleanSelection.getLabeledTextField("Selection Expression",new EditableString.Listener() {
            @Override
            public void valueChanged() {
                if(dos==null) return;
                try {
                    dos.writeInt(1);
                    byte[] buf=booleanSelection.getValue().getBytes();
                    dos.writeInt(buf.length);
                    dos.write(buf);
                    dos.flush();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        northPanel.add(updateInterval.getLabeledTextField("Update Interval",new EditableInt.Listener() {
            @Override
            public void valueChanged() {
                if(dos==null) return;
                try {
                    dos.writeInt(0);
                    dos.writeInt(updateInterval.getValue());
                    dos.flush();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        JButton button=new JButton("Start Socket");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                socketPortChanged();
            }
        });
        northPanel.add(button);
        socketPanel.add(northPanel,BorderLayout.NORTH);
        propPanel.add("Socket",socketPanel);
        
        JPanel descPanel=new JPanel(new GridLayout(2,1));
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Parameter Descriptions"));
        final JList paramList=new JList(paramNames.toArray());
        panel.add(paramList,BorderLayout.CENTER);
        JPanel buttonPanel=new JPanel(new GridLayout(1,3));
        button=new JButton("Add");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str=JOptionPane.showInputDialog(propPanel,"Enter the new parameter description");
                if(str!=null) {
                    paramNames.add(str);
                    paramList.setListData(paramNames.toArray());
                }
            }
        });
        buttonPanel.add(button);
        button=new JButton("Remove");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=paramList.getSelectedIndex();
                if(index>=0) {
                    paramNames.remove(index);
                    paramList.setListData(paramNames.toArray());
                }
            }
        });
        buttonPanel.add(button);
        button=new JButton("Move Up");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index=paramList.getSelectedIndex();
                if(index>0) {
                    String elem=paramNames.remove(index);
                    paramNames.add(index-1,elem);
                    paramList.setListData(paramNames.toArray());
                }
            }
        });
        buttonPanel.add(button);
        panel.add(buttonPanel,BorderLayout.SOUTH);
        descPanel.add(panel);
        propPanel.add("Descriptions",descPanel);
    }

    @Override
    public int getNumParameters(int stream) {
        return maxParams;
    }

    @Override
    public int getNumValues(int stream) {
        return maxValues;
    }

    @Override
    public String getParameterDescription(int stream, int which) {
        if(paramNames.size()>which) return paramNames.get(which);
        return "Unspecified";
    }

    @Override
    public String getValueDescription(int stream, int which) {
        if(valueNames.size()>which) return valueNames.get(which);
        return "Unspecified";
    }

    @Override
    public GraphElement copy(List<GraphElement> l) {
        return new SocketSource(this,l);
    }

    @Override
    public String getDescription() {
        return "Socket Source";
    }
    
    public static String getTypeDescription() {
        return "Socket Source";
    }
    
    private void socketPortChanged() {
        try {
            if(sock!=null) {
                sock.close();
            }
            startServer();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void startServer() throws IOException {
        sock=new Socket(host.getValue(), port.getValue());
        dos=new DataOutputStream(sock.getOutputStream());
        dos.writeInt(0);
        dos.writeInt(updateInterval.getValue());
        dos.writeInt(1);
        byte[] buf=booleanSelection.getValue().getBytes();
        dos.writeInt(buf.length);
        dos.write(buf);
        dos.flush();
        new Thread(new Runnable() {
            @Override
            public void run() {
                watchSocket();
            }
        }).start();
    }
    
    private void watchSocket() {
        try {
            DataInputStream dis=new DataInputStream(sock.getInputStream());
            while(true) {
                boolean first=true;
                while(dis.readInt() == 1) {
                    if(first) {
                        readList.clear();
                        first=false;
                    }
                    int numParams=dis.readInt();
                    int[] params=new int[numParams];
                    for(int j=0; j<numParams; ++j) {
                        params[j]=dis.readInt();
                    }
                    int numValues=dis.readInt();
                    float[] vals=new float[numValues];
                    for(int j=0; j<numValues; ++j) {
                        vals[j]=(float)dis.readDouble();
                    }
                    int numStrings=dis.readInt();
                    for(int j=0; j<numStrings; ++j) {
                        dis.readUTF();
                    }
                    readList.add(new DataElement(params,vals));
                }
                abstractRedoAllElements();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void calcNumParamsAndValue() {
        maxParams=0;
        maxValues=0;
        for(DataElement de:dataVect) {
            if(de.getNumParams()>maxParams) maxParams=de.getNumParams();
            if(de.getNumValues()>maxValues) maxValues=de.getNumValues();
        }
    }

    private List<String> paramNames=new ArrayList<String>();
    private List<String> valueNames=new ArrayList<String>();
    private int maxParams=0;
    private int maxValues=0;
    private EditableInt port=new EditableInt(4001);
    private EditableString host=new EditableString("localhost");
    private EditableString booleanSelection=new EditableString("1=1");
    private EditableInt updateInterval=new EditableInt(5);
    private ArrayList<DataElement> readList=new ArrayList<DataElement>();
    
    private transient Socket sock;
    private transient DataOutputStream dos;

    private static final long serialVersionUID = -8920467424478443184L;
}
