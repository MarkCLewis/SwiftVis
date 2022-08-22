/*
 * Created on Jul 7, 2005
 */
package edu.swri.swiftvis.plot.styles;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import edu.swri.swiftvis.plot.DataPlotStyle;
import edu.swri.swiftvis.plot.PlotArea2D;
import edu.swri.swiftvis.plot.PlotLegend;
import edu.swri.swiftvis.plot.PlotTransform;
import edu.swri.swiftvis.scheme.SVSchemeUtil;
import edu.swri.swiftvis.scheme.SchemeConsole;
import edu.swri.swiftvis.scheme.SchemeElement;
import edu.swri.swiftvis.scheme.SchemeEnvironment;
import edu.swri.swiftvis.scheme.SchemeValue;
import edu.swri.swiftvis.util.EditableString;

public final class SchemeStyle implements DataPlotStyle {
    public SchemeStyle(PlotArea2D pa) {
        plotArea=pa;
    }
    
    private SchemeStyle(SchemeStyle c,PlotArea2D pa) {
        plotArea=pa;
        name=new EditableString(c.name.getValue());
        codeString=c.codeString;
    }
    
    @Override
    public double[][] getBounds() {
        return bounds;
    }

    @Override
    public void redoBounds() {
        env=SVSchemeUtil.plotEnvironment(plotArea.getSink(),null,null);
        SchemeConsole.parseAndExecuteMany(codeString,env);
        SchemeElement func=env.getNameValue("findBounds");
        if(func==null) {
            JOptionPane.showMessageDialog(propPanel,"You must define a findBounds function.");
            return;
        }
        SchemeElement newList=SchemeConsole.parse("(findBounds)",env).eval(env);
        bounds[0][0]=((SchemeValue)newList.car().car()).numericValue();
        bounds[0][1]=((SchemeValue)newList.car().cdr().car()).numericValue();
        bounds[1][0]=((SchemeValue)newList.cdr().car().car()).numericValue();
        bounds[1][1]=((SchemeValue)newList.cdr().car().cdr().car()).numericValue();
    }

    @Override
    public JComponent getPropertiesPanel() {
        if(propPanel==null) {
            propPanel=new JPanel(new BorderLayout());
            final JTextArea textArea=new JTextArea(codeString);
            propPanel.add(textArea,BorderLayout.CENTER);
            JButton button=new JButton("Process Code/Propagate Changes");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    codeString=textArea.getText();
                    plotArea.forceRedraw();
                }
            });
            propPanel.add(button,BorderLayout.SOUTH);
        }
        return propPanel;
    }

    @Override
    public PlotLegend getLegendInformation() {
        return null;
    }

    @Override
    public void drawToGraphics(Graphics2D g,PlotTransform trans) {
        env=SVSchemeUtil.plotEnvironment(plotArea.getSink(),g,trans);
        SchemeConsole.parseAndExecuteMany(codeString,env);
        SchemeElement func=env.getNameValue("drawPlot");
        if(func==null) {
            JOptionPane.showMessageDialog(propPanel,"You must define a drawPlot function.");
            return;
        }
        SchemeConsole.parse("(drawPlot)",env).eval(env);
    }

    @Override
    public DataPlotStyle copy(PlotArea2D pa) {
        return new SchemeStyle(this,pa);
    }
    
    @Override
    public String toString() {
        return "Scheme Custom Plot - "+name.getValue();
    }

    public static String getTypeDescription() { return "Scheme Custom Plot"; }

    private PlotArea2D plotArea;
    private EditableString name=new EditableString("Default");
    private String codeString="(define (drawPlot) ())\n\n"+
        "(define (findBounds) '((0 1) (0 1)))";
    private transient SchemeEnvironment env;
    private transient double[][] bounds=new double[][]{{0,1},{0,1}};
    private transient JPanel propPanel;

    private static final long serialVersionUID=72346457459760l;
}
