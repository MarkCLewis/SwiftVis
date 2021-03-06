/*
 * Created on Jul 20, 2004
 */
package edu.swri.swiftvis.filters;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.swri.swiftvis.BooleanFormula;
import edu.swri.swiftvis.DataElement;
import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.ReduceLoopBody;
import edu.swri.swiftvis.util.ThreadHandler;

/**
 * This filter is intended to be used with multiple inputs to do selections.  It was born
 * from a desire by users to be able to have a discard file and select all of the particles
 * that collide with a particular body.  For example, you want to take a discard file and
 * a bin.dat file and select from the bin.dat only the particles that wound up colliding
 * with Jupiter.
 * 
 * The way this works is that you enter a boolean formula and a data formula for the data
 * file you are using for the selection.  In this case, the discard file.  The boolean
 * expression tells if that index in the selecting input should be used.  In this example,
 * it would be an expression asking if the body that was hit was Jupiter.  If it is true, then
 * the data formula is evaluated to produce a "key".  For this example, the "key" would
 * be the particle number that ran into Jupiter.  The first step is that the entire selecting
 * input has is run through and all the keys are collected.  The user also provides a formula
 * for the selected input that needs to match the keys.  The filter then passes through the
 * selected input and every element for which the selected formula matches a stored key, that
 * element is kept.  In the example, the selected data formula would simply be p[0], the number
 * of the particle in the bin.dat input.
 * 
 * The user specifies which input is the selecting input just with the formulas.  The one that
 * is being selected must also be selected with a drop box.  If the first input is for
 * selecting, then the boolean formula and the key formula should use d[0] while the key
 * matching formula should use d[1].
 *  
 * @author Mark Lewis
 */
public class KeySelectionFilter extends AbstractMultipleSourceFilter {
    public KeySelectionFilter() {}

    private KeySelectionFilter(KeySelectionFilter c,List<GraphElement> l) {
        super(c,l);
        boolFormula=new BooleanFormula(c.boolFormula);
        keyFormula=new DataFormula(c.keyFormula);
        pullSource=c.pullSource;
        keyMatchFormula=new DataFormula(c.keyMatchFormula);
    }

    @Override
    public void addInput(DataSource ds) {
        super.addInput(ds);
        if(comboBox!=null) {
            comboBox.setModel(new DefaultComboBoxModel(inputVector.toArray()));
            comboBox.setSelectedIndex(pullSource);
        }
    }

    @Override
    public void removeInput(DataSource ds) {
        super.removeInput(ds);
        if(comboBox!=null) comboBox.removeItem(ds);
        if(pullSource>=inputVector.size()) {
            pullSource=inputVector.size()-1;
            if(comboBox!=null) {
                comboBox.setSelectedIndex(pullSource);
            }
        }
    }

    @Override
    protected boolean doingInThreads() {
        return true;
    }

    @Override
    protected void redoAllElements() {
        if(inputVector.isEmpty()) return;
        sizeDataVectToInputStreams();
        try {
            if(pullSource>=inputVector.size()) {
                pullSource=inputVector.size()-1;
                if(comboBox!=null) {
                    comboBox.setSelectedIndex(pullSource);
                }
            }
            for(int s=0; s<inputVector.get(pullSource).getNumStreams(); ++s) {
                final int ss=s;
                int[] range1=boolFormula.getSafeElementRange(this,s);
                int[] tmp=keyFormula.getSafeElementRange(this,s);
                if(tmp[0]>range1[0]) range1[0]=tmp[0];
                if(tmp[1]<range1[1]) range1[1]=tmp[1];
                DataFormula.checkRangeSafety(range1,this);
                final Hashtable<Double,Double> hash=new Hashtable<Double,Double>((range1[1]-range1[0])/5);
                //  parallel
                final BooleanFormula[] bools=new BooleanFormula[ThreadHandler.instance().getNumThreads()];
                final DataFormula[] keys=new DataFormula[ThreadHandler.instance().getNumThreads()];
                ReduceLoopBody[] reduceLoops=new ReduceLoopBody[ThreadHandler.instance().getNumThreads()];
                for (int i=0; i<reduceLoops.length; i++) {
                    final int index=i;
                    bools[i]=boolFormula.getParallelCopy();
                    keys[i]=keyFormula.getParallelCopy();
                    reduceLoops[i]=new ReduceLoopBody() {
                        @Override
                        public void execute(int start, int end) {
                            BooleanFormula bf=bools[index];
                            DataFormula kf=keys[index];
                            for (int j=start; j<end; j++) {
                                if(bf.valueOf(KeySelectionFilter.this,ss,j)) {
                                    Double key=new Double(kf.valueOf(KeySelectionFilter.this,ss,j));
                                    hash.put(key,key);
                                }
                            }
                        }
                    };
                }
                ThreadHandler.instance().chunkedForLoop(this,range1[0],range1[1],reduceLoops);
                int[] range2=keyMatchFormula.getSafeElementRange(this, 0);
                DataFormula.checkRangeSafety(range2,this);
                //  parallel
                final ArrayList<ArrayList<DataElement>> vects=new ArrayList<ArrayList<DataElement>>();
                for (int i=0; i<ThreadHandler.instance().getNumThreads(); i++) {
                    vects.add(new ArrayList<DataElement>());
                }
                final DataFormula[] keyMatches=new DataFormula[ThreadHandler.instance().getNumThreads()];
                // create ReduceLoopBody array
                ReduceLoopBody[] loops=new ReduceLoopBody[vects.size()];
                for (int i=0; i<loops.length; i++) {
                    final int index=i;
                    keyMatches[i]=keyMatchFormula.getParallelCopy();
                    loops[i]=new ReduceLoopBody() {
                        @Override
                        public void execute(int start, int end) {
                            ArrayList<DataElement> data=vects.get(index);
                            DataFormula kmf=keyMatches[index];
                            for (int j=start; j<end; j++) {
                                Double match=new Double(kmf.valueOf(KeySelectionFilter.this,ss,j));
                                if(hash.containsKey(match)) data.add(inputVector.get(pullSource).getElement(j,ss));
                            }
                        }
                    };
                }
                ThreadHandler.instance().chunkedForLoop(this,range2[0],range2[1],loops);
                // merge lists
                int size=0;
                for (int i=0; i<vects.size(); i++) {
                    size+=vects.get(i).size();
                }
                dataVect.get(s).ensureCapacity(size);
                for (int i=0; i<vects.size(); i++) {
                    dataVect.get(s).addAll(vects.get(i));
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            // Do nothing here.  Probably waiting for second source.
        }
    }

    @Override
    protected void setupSpecificPanelProperties() {
        JPanel formPanel=new JPanel(new BorderLayout());
        JPanel northPanel=new JPanel(new GridLayout(4,1));
        JPanel innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Key Selection Formula"),BorderLayout.WEST);
        innerPanel.add(boolFormula.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Key Value Formula"),BorderLayout.WEST);
        innerPanel.add(keyFormula.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());		
        innerPanel.add(new JLabel("Selected Source"),BorderLayout.WEST);
        comboBox=new JComboBox(inputVector.toArray());
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pullSource=comboBox.getSelectedIndex();
            }
        } );
        innerPanel.add(comboBox,BorderLayout.CENTER);
        northPanel.add(innerPanel);
        innerPanel=new JPanel(new BorderLayout());
        innerPanel.add(new JLabel("Key Match Formula"),BorderLayout.WEST);
        innerPanel.add(keyMatchFormula.getTextField(null),BorderLayout.CENTER);
        northPanel.add(innerPanel);
        formPanel.add(northPanel,BorderLayout.NORTH);

        JButton button=new JButton("Propagate Changes");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { abstractRedoAllElements(); }
        } );
        formPanel.add(button,BorderLayout.SOUTH);
        propPanel.add("Selection",formPanel);
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.DataSource#getParameterDescription(int)
     */
    @Override
    public String getParameterDescription(int stream, int which) {
        if(pullSource<0) return "None";
        DataSource input=inputVector.get(pullSource);
        return input.getParameterDescription(0, which);
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.DataSource#getValueDescription(int)
     */
    @Override
    public String getValueDescription(int stream, int which) {
        if(pullSource<0) return "None";
        DataSource input=inputVector.get(pullSource);
        return input.getValueDescription(0, which);
    }

    /* (non-Javadoc)
     * @see edu.swri.swiftvis.GraphElement#getDescription()
     */
    @Override
    public String getDescription() {
        return "Key Selection Filter";
    }

    public static String getTypeDescription(){ return "Key Selection Filter"; }

    @Override
    public KeySelectionFilter copy(List<GraphElement> l) {
        return new KeySelectionFilter(this,l);
    }

    private BooleanFormula boolFormula = new BooleanFormula("1=0");

    private DataFormula keyFormula = new DataFormula("d[0].v[0]");

    private int pullSource=-1;

    private DataFormula keyMatchFormula = new DataFormula("d[1].v[0]");

    private transient JComboBox comboBox;

    private static final long serialVersionUID=8124609186456l;
}
