/*
 * Created on Feb 25, 2007
 */
package edu.swri.swiftvis.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JFileChooser;

public class BinaryFileHelper {

    /**
     * @param args
     */
    public static void main(String[] args) {
        JFileChooser chooser=new JFileChooser();
        chooser.showOpenDialog(null);
        try {
            DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(chooser.getSelectedFile())));
            for(int i=0; i<10; ++i) {
                int[] vals=new int[8];
                for(int j=0; j<8; ++j) {
                    vals[j]=in.read();
                    System.out.print(Integer.toHexString(vals[j])+"\t");
                }
                System.out.print(" | ");
                for(int j=0; j<8; ++j) {
                    System.out.print(vals[j]+"\t");
                }
                System.out.println();
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
