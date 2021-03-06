/*
 * Created on Oct 25, 2007
 */
package edu.swri.swiftvis.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketTester {
    public static void main(String[] args) {
        try {
            final Socket sock=new Socket("localhost",4000);
            DataOutputStream dos=new DataOutputStream(sock.getOutputStream());
            for(int i=0; i<20; ++i) {
                dos.writeInt(i+10);
                for(int j=0; j<i+10; ++j) {
                    dos.writeInt(1);
                    dos.writeInt(j);
                    dos.writeInt(2);
                    dos.writeFloat((float)j/i);
                    dos.writeFloat((j*i));
                }
                Thread.sleep(5000);
            }
            dos.close();
            sock.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
