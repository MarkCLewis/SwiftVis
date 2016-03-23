/*
 * Created on Jun 16, 2005
 */
package edu.swri.swiftvis.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile extends RandomAccessFile {
    public static void main(String[] args) {
        try {
            File curDir=new File(".");
            for(File f:curDir.listFiles()) {
                System.out.println(f.getAbsolutePath());
            }
            BufferedRandomAccessFile bras=new BufferedRandomAccessFile(curDir.listFiles()[6],"r");
            byte[] buf=new byte[5];
            bras.read(buf);
        } catch(IOException e) {
            e.printStackTrace();
        }
        byte b=-5;
        System.out.println(b & 0xff);
    }
    
    public BufferedRandomAccessFile(File f,String mode) throws FileNotFoundException {
        super(f,mode);
        buffer=new byte[DEFAULT_BUFFER_SIZE];
    }
    
    public BufferedRandomAccessFile(String f,String mode) throws FileNotFoundException {
        super(f,mode);
        buffer=new byte[DEFAULT_BUFFER_SIZE];
    }
    
    public BufferedRandomAccessFile(File f,String mode,int bufSize) throws FileNotFoundException {
        super(f,mode);
        buffer=new byte[bufSize];
    }
    
    @Override
    public int read() throws IOException {
        if(curOffset>=readToBuffer) {
            fillBuffer();
            if(readToBuffer<1) return -1;
        }
        ++curOffset;
        return buffer[curOffset-1] & 0xff;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        int intoOffset=0;
        int ret=0;
        while(intoOffset<buf.length) {
            if(curOffset>=readToBuffer) {
                fillBuffer();
                if(readToBuffer<1) return ret;
            }
            buf[intoOffset]=buffer[curOffset];
            intoOffset++;
            curOffset++;
            ret++;
        }
        return ret;
    }

    @Override
    public int read(byte[] buf,int offset,int len) throws IOException {
        int intoOffset=offset;
        int ret=0;
        while(intoOffset<offset+len) {
            if(curOffset>=readToBuffer) {
                fillBuffer();
                if(readToBuffer<1) return ret;
            }
            buf[intoOffset]=buffer[curOffset];
            intoOffset++;
            curOffset++;
            ret++;
        }
        return ret;
    }
    
    @Override
    public void seek(long filePos) throws IOException {
        super.seek(filePos);
        fillBuffer();
    }
    @Override
    public long getFilePointer() throws IOException {
        return super.getFilePointer()-(readToBuffer-curOffset);
    }

    private void fillBuffer() throws IOException {
        curOffset=0;
        readToBuffer=super.read(buffer);
    }
    
    private byte[] buffer;
    private int curOffset=0;
    private int readToBuffer=0;
    private static final int DEFAULT_BUFFER_SIZE=4096;
}
