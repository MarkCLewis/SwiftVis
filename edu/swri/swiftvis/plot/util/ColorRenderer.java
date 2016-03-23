/*
 * Created on Feb 24, 2007
 */
package edu.swri.swiftvis.plot.util;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

/**
 * This code taken from the Java tutorial on tables. 
 */
public class ColorRenderer extends JLabel
        implements TableCellRenderer {        
    public ColorRenderer(boolean isBordered) {
        this.isBordered = isBordered;
        setOpaque(true); //MUST do this for background to show up.
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object color,
            boolean isSelected, boolean hasFocus,int row, int column) {
        Color newColor = (Color)color;
        setBackground(newColor);
        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                       table.getSelectionBackground());
                }
                setBorder(selectedBorder);
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,
                                   table.getBackground());
                }
                setBorder(unselectedBorder);
            }
        }
    
        setToolTipText("RGB value: " + newColor.getRed() + ", "
                  + newColor.getGreen() + ", "
                  + newColor.getBlue());
        return this;
    }
    private static final long serialVersionUID = -8076528623937749454L;
    private Border unselectedBorder = null;
    private Border selectedBorder = null;
    private boolean isBordered = true;
}
