/*
 * Created on Jun 12, 2006
 */
package edu.swri.swiftvis;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;

public class AboutDialog {
    public AboutDialog(JFrame frame) {
        dialog=new JDialog(frame,"About SwiftVis",true);
        JEditorPane pane=new JEditorPane("text/html",message);
        pane.setEditable(false);
        dialog.add(pane,BorderLayout.CENTER);
        JButton button=new JButton("Done");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        dialog.add(button,BorderLayout.SOUTH);
        dialog.pack();
        dialog.setVisible(true);
    }
    
    private JDialog dialog;
    private static final String message=
        "<html>"+
        "<body>"+
        "<p><center><h1>SwiftVis</h1></center></p><hr>"+
        "<p>Welcome to SwiftVis by Mark Lewis.</p>"+
        "<p>SwiftVis was produced with funding from NASA AIS.  Hal Levison grant PI.<p>"+
        "<p>Threading and scheduling code by Glenn Kavanagh.</p>"+
        "<p>Version "+OptionsData.version+"</p>"+
        "</body></html>";
}
