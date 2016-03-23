/*
 * Created on Sep 24, 2005
 */
package edu.swri.swiftvis.sinks;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import edu.swri.swiftvis.DataSink;
import edu.swri.swiftvis.DataSource;
import edu.swri.swiftvis.GraphElement;
import edu.swri.swiftvis.util.GraphLabelString;
import edu.swri.swiftvis.util.SourceInfoPanel;

public abstract class AbstractSink implements DataSink {
    public AbstractSink() {}
    
    protected AbstractSink(AbstractSink c,List<GraphElement> l) {
        label=new GraphLabelString(c.label);
        for(DataSource ds:c.sources) {
            if(l.contains(ds)) sources.add(ds);
        }        
    }
    
    @Override
    public boolean validInput(DataSource ds) {
        return true;
    }

    @Override
    public void addInput(DataSource input) {
        sources.add(input);
        input.addOutput(this);
        if(sip!=null) sip.redoSourceList();
        abstractRedo();
    }

    @Override
    public void removeInput(DataSource input) {
        sources.remove(input);
        input.removeOutput(this);
        if(sip!=null) sip.redoSourceList();
        abstractRedo();
    }

    @Override
    public void moveUpInput(int index) {
        DataSource ds=sources.get(index);
        sources.set(index,sources.get(index-1));
        sources.set(index-1,ds);
        if(sip!=null) sip.redoSourceList();        
        abstractRedo();
    }

    @Override
    public int getNumSources() {
        return sources.size();
    }

    @Override
    public DataSource getSource(int which) {
        return sources.get(which);
    }

    @Override
    public void redo() {
    	sourceAltered(null);
    }
    @Override
    public void sourceAltered(DataSource source) {
        abstractRedo();
    }
    
    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JTabbedPane();
            setupSpecificPanelProperties();
            
            propPanel.addTab("Sources",getSourceInfoPanel());
            propPanel.addTab("Label",label.getAreaPanel("Label"));
        }
        return propPanel;
    }

    @Override
    public Rectangle getBounds() {
        return label.getBounds();
    }

    @Override
    public void translate(int dx,int dy) {
        label.translate(dx,dy);
    }

    public Paint getPaint() {
        return Color.black;
    }
    
    @Override
    public String toString() {
        return label.toString();
    }
    
    @Override
    public void drawNode(Graphics2D g) {
        label.draw(g);
    }

    @Override
    public void relink(Hashtable<GraphElement, GraphElement> linkHash) {
        for(int i=0; i<sources.size(); ++i) {
            sources.set(i,(DataSource)linkHash.get(sources.get(i)));
        }
    }

    @Override
    public void clearData() {
    }
    
    protected abstract void abstractRedo();
    protected abstract void setupSpecificPanelProperties();

    protected SourceInfoPanel getSourceInfoPanel() {
        if (sip == null)
            sip = new SourceInfoPanel(this);
        return sip;
    }

    private GraphLabelString label=new GraphLabelString(getDescription(),getPaint());
    protected List<DataSource> sources=new ArrayList<DataSource>();
    protected transient JTabbedPane propPanel;
    private transient SourceInfoPanel sip;
    private static final long serialVersionUID=35639872357l;
}
