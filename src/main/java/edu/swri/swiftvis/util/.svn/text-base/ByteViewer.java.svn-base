/*
 * Created on Jul 9, 2005
 */
package edu.swri.swiftvis.util;

import java.io.FileInputStream;

import javax.swing.JFileChooser;

public class ByteViewer {
    public static void main(String[] args) {
        try {
            JFileChooser chooser=new JFileChooser();
            chooser.showOpenDialog(null);
            FileInputStream fis=new FileInputStream(chooser.getSelectedFile());
            for(int i=0; i<100; ++i) {
                System.out.print(i+" ");
                for(int j=0; j<8; ++j) {
                    System.out.print(fis.read()+" ");
                }
                System.out.println();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
