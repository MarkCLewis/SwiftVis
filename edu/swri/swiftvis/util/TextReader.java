/* Generated by Together */

package edu.swri.swiftvis.util;

import java.io.*;

public class TextReader extends Reader {
    public TextReader(Reader r) {
        wrapped=new BufferedReader(r);
    }

    public String readWord() throws IOException {
        StringBuffer buf=new StringBuffer();
        int ret=wrapped.read();
        while(ret!=-1 && Character.isWhitespace((char)ret)) ret=wrapped.read();
		if(ret<0) throw new EOFException("End of stream reached.");
        while(ret>=0) {
            buf.append((char)ret);
	        ret=wrapped.read();
	        //if(ret<0) throw new EOFException("End of stream reached.");
            if(Character.isWhitespace((char)ret)) ret=-1;
        }
        return new String(buf);
    }

    public String readLine() throws IOException {
        StringBuffer buf=new StringBuffer();
        int ret=wrapped.read();
        while(Character.isWhitespace((char)ret)) ret=wrapped.read();
//        System.out.println("First "+ret+" "+Character.getType((char)ret)+" "+Character.LINE_SEPARATOR);
		if(ret==-1) throw new EOFException();
        while(ret!=-1) {
            buf.append((char)ret);
	        ret=wrapped.read();
            //if(ret==-1) throw new EOFException();
//            System.out.println("      "+ret+" "+Character.getType((char)ret)+" "+Character.LINE_SEPARATOR);
            if(ret=='\n') ret=-1;
        }
        return new String(buf);
    }

    public double readDouble() throws IOException {
        return Double.parseDouble(readWord());
    }

    public int readInt() throws IOException {
        return Integer.parseInt(readWord());
    }

    @Override
    public int read() throws IOException {
        return wrapped.read();
    }

    @Override
    public int read(char[] buf) throws IOException {
        return wrapped.read(buf);
    }

    @Override
    public int read(char[] buf,int offset,int len) throws IOException {
        return wrapped.read(buf,offset,len);
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public void mark(int ral) throws IOException {
        wrapped.mark(ral);
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public boolean ready() throws IOException {
        return wrapped.ready();
    }

    @Override
    public void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public long skip(long num) throws IOException {
        return wrapped.skip(num);
    }

    private Reader wrapped;
}
