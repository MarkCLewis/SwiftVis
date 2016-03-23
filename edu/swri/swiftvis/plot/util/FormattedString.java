/*
 * Created on Jul 17, 2005
 */
package edu.swri.swiftvis.plot.util;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.swri.swiftvis.DataFormula;
import edu.swri.swiftvis.DataSink;

/**
 * This class is intended to do simple formatting on strings and also allow some ability to put
 * in formulas that will be evaluated.  The primary motivation is to enable superscripts and
 * subscripts in the various text elements that are drawn on plots.
 * 
 * To denote a numerical formula to be evaluated, enclose the formula string in dollar signs ($).
 * 
 * To display a formula with superscripts and subscripts, use a carret (^) for super and an
 * underscore (_) for sub.  Then enclose the text to be super/sub scripted in curly braces ({}).
 * 
 * Superscripts:  3^{2} produces 3 raised to the 2; 3 squared
 * Subscripts:  x_{0} produces x sub 0
 * To use a simple carret or underscore, place a backslash before it; 
 * Ex. x\^2 displays x^2, k\_4 displays k_4
 * 
 * If a Superscript immediately follows a Subscript, the subscripted variable is "raised to
 * the given power."  Ex:  x_{0}^{2}  produces x sub 0 squared
 * NB.  Subscripts should always be first if you are trying to raise a subscripted character
 * to a power.
 * 
 * Greek characters: backslash followed by the character name (ex. tau, rho, pi); capitalize the 
 * first character to get the capitalized form of the letter; Ex. \tau or \Tau
 * 
 * Math Symbols:
 * \infty for infinity
 * \to for --> (arrow) (used in limits)
 * 
 * Sumation: \sum_{k=0}^{n}   the sum from k=0 to n; for just the Sigma, using \sum with no
 * trailing arguments will produce a larger Sigma; \Sigma will produce a normal sized one
 * To produce sumation in a different format, use \Sigma_{k=0}^{n}
 * 
 * Limits: \lim_{n \to \infty}   the limit from n to infinity
 * Other form: lim_{n \to \infty}  (NB. no preceding backslash)
 * 
 * Integral: \int_{a}^{\infty}   the integral from a to infinity
 * Trailing arguments are optional; must have a subscript to use the superscript
 * 
 * @author Mark Lewis
 * @author Glenn Kavanagh
 */
public class FormattedString implements Serializable {
    public FormattedString(String t) {
        setValue(t);
    }
    
    public void setValue(String t) {
        text=t;
        parse();
        if(field!=null) {
            field.setText(text);
        }
    }
    
    public String getValue() {
        return text;
    }
    
    public JTextField getTextField(Listener edl) {
        if(field==null) {
            listener=edl;
            field=new JTextField(text);
            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { valueChanged(); }
            } );
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    try {
                        valueChanged();                        
                    } catch(NullPointerException ex) {
                        JOptionPane.showMessageDialog(field,"There was an exception parsing that string.");
                        ex.printStackTrace();
                    }
                }
            } );
        }
        return field;
    }
    
    public JTextArea getTextArea(Listener edl) {
        if(area==null) {
            listener=edl;
            area=new JTextArea(text);
            area.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    try {
                        valueChanged();                        
                    } catch(NullPointerException ex) {
                        JOptionPane.showMessageDialog(field,"There was an exception parsing that string.");
                        ex.printStackTrace();
                    }
                }
            } );
        }
        return area;
    }
    
    public void draw(Graphics2D g,float x,float y,DataSink sink) {
        if(root!=null) {
            root.draw(g,x,y,sink);
        }
    }
    
    public Rectangle2D getBounds(Font font,DataSink sink) {
        if(root==null) return new Rectangle2D.Double(0,0,0,0);
        return root.calcBounds(font,sink);
    }
    
    private void parse() {
        root=parseGeneralNode(text);
    }
    
    private static Node parseGeneralNode(String s) {
        int[] count=new int[openStyle.length];
        List<String> lines=new ArrayList<String>();
        int lineBeginIndex=0;
        for(int i=0; i<s.length(); ++i) {
            for(int j=0; j<openStyle.length; ++j) {
                if(s.charAt(i)==openStyle[j]) count[j]++;
            }
            for(int j=0; j<closeStyle.length; ++j) {
                if(s.charAt(i)==closeStyle[j]) count[j]--;
            }
            boolean countsEqualZero=true;
            for(int j=0; j<count.length && countsEqualZero; ++j) {
                if(count[j]>0) countsEqualZero=false;
            }
            if(s.charAt(i)=='\n' && countsEqualZero) {
                lines.add(s.substring(lineBeginIndex,i));
                lineBeginIndex=i+1;
            }
        }
        lines.add(s.substring(lineBeginIndex));
        if(lines.size()<2) {
            return parseLineNode(lines.get(0));
        } else {
            return new MultilineNode(lines);
        }
    }
    
    private static Node parseLineNode(String s) {
        if(s==null || s.length()<1) return new PlainText("");
        if(s.charAt(0)=='$') return new Formula(s);
        if(s.charAt(0)=='^') return new Superscript(s);
        if(s.charAt(0)=='_') return new Subscript(s);
        if(s.charAt(0)=='\\') return parseEscape(s);
        for(int i=0; i<openStyle.length; ++i) {
            if(s.charAt(0)==openStyle[i]) return parseParens(s);
        }
        return new PlainText(s);
    }
    
    private static synchronized Node parseEscape(String s) {
    	if(escapeMap==null) {
    		escapeMap = new HashMap<String,NodeBuilder>();
    		escapeMap.put("frac",new FracBuilder());
    		SpecialCharBuilder gb = new SpecialCharBuilder();
            for(String greekStr:gb.charMap.keySet()) {
                escapeMap.put(greekStr,gb);
            }
    		escapeMap.put("sum",new SumBuilder());
    		escapeMap.put("lim",new LimitBuilder());
    		escapeMap.put("int",new IntegralBuilder());
    	}
    	boolean search=true;
    	int index = 1;
    	while(search) {
    		if(index>=s.length()-1) {
    			search=false;
    		} 
    		if(!Character.isLetterOrDigit(s.charAt(index))) {
    			if (index==1 && (s.charAt(index)=='^' || s.charAt(index)=='_')) {
    				index++;
    			}
    			search=false;
    		} else {
    			index++;
    		}
    	}
    	String code=s.substring(1,index);
        NodeBuilder builder=escapeMap.get(code);
        if(builder!=null) return builder.buildNode(code,s.substring(index));
        try {
            if(code.charAt(0)=='x') {
                return new PlainText(Character.toString((char)Integer.parseInt(code.substring(1),16)),s.substring(index));
            } else {
                return new PlainText(Character.toString((char)Integer.parseInt(code)),s.substring(index));
            }
        } catch(NumberFormatException e) {}
        throw new FormattedStringException("Bad escape code. "+code);
    }
    
    private static Node parseParens(String s) {
        int index;
        int[] count=new int[openStyle.length];
        for(int j=0; j<openStyle.length; ++j) {
            if(s.charAt(0)==openStyle[j]) count[j]++;
        }
        boolean countsEqualZero=false;
        for(index=1; index<s.length() && !countsEqualZero; ++index) {
            for(int j=0; j<openStyle.length; ++j) {
                if(s.charAt(index)==openStyle[j]) count[j]++;
            }
            for(int j=0; j<closeStyle.length; ++j) {
                if(s.charAt(index)==closeStyle[j]) count[j]--;
            }
            countsEqualZero=true;
            for(int j=0; j<count.length && countsEqualZero; ++j) {
                if(count[j]>0) countsEqualZero=false;
            }
        }
        return new ParensNode(s.substring(0,index),s.substring(index));
    }
    
    private void valueChanged() {
        if(field!=null) text=field.getText();
        else text=area.getText();
        parse();
        if(listener!=null) {
            listener.valueChanged();
        }
    }

    private String text;
    private Node root;
    
    private transient JTextField field;
    private transient JTextArea area;
    private transient Listener listener;
    private static HashMap<String,NodeBuilder> escapeMap;
    private static HashMap<String,String> overUnderCharMap;
    
    private static char[] openStyle={'(','{','['};
    private static char[] closeStyle={')','}',']'};
    private static final FontRenderContext frc=new FontRenderContext(new AffineTransform(),true,true);
    private static final long serialVersionUID=3452398237459834757l;
    
    public interface Listener {
        void valueChanged();
    }
    
    private static interface Node extends Serializable {
        Rectangle2D calcBounds(Font f,DataSink sink);
        void draw(Graphics2D g,double x,double y,DataSink sink);
    }
    
    private static class MultilineNode implements Node {
        public MultilineNode(List<String> s) {
            for(String str:s) {
                lines.add(parseLineNode(str));
            }
        }
        @Override
        public Rectangle2D calcBounds(Font f,DataSink sink) {
            Rectangle2D[] bounds=new Rectangle2D[lines.size()];
            for(int i=0; i<bounds.length; ++i) {
                bounds[i]=lines.get(i).calcBounds(f,sink);
            }
            double maxWidth=0,totHeight=0;
            for(Rectangle2D b:bounds) {
                if(b.getWidth()>maxWidth) maxWidth=b.getWidth();
                totHeight+=b.getHeight();
            }
            return new Rectangle2D.Double(0,0,maxWidth,totHeight);
        }

        @Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
            for(Node n:lines) {
                Rectangle2D b=n.calcBounds(g.getFont(),sink);
                y-=b.getMinY();
                n.draw(g,x,y,sink);
                y+=b.getMaxY();
            }
        }
        private List<Node> lines=new ArrayList<Node>();
        private static final long serialVersionUID = 8005576738488960664L;
    }
    
    private static class PlainText implements Node {
        public PlainText(String s) {
        	initPlainText("",s);
        }
        public PlainText(String start,String s) {
            initPlainText(start,s);
        }
        private void initPlainText(String start,String s) {
            str=start;
            char[] specialChar={'$','^','_','\\'};
            int dPos=100000;
            for(int i=0; i<specialChar.length; i++) {
                int pos=s.indexOf(specialChar[i]);
            	if(pos>=0 &&pos<dPos) {
        			dPos=pos;
            	}
            }
            for(int i=0; i<openStyle.length; i++) {
                int pos=s.indexOf(openStyle[i]);
                if(pos>=0 &&pos<dPos) {
                    dPos=pos;
                }
            }
            if(dPos==100000) {
                str+=s;
            } else {
                str+=s.substring(0,dPos);
                next=parseGeneralNode(s.substring(dPos));
            }
        }
        @Override
        public Rectangle2D calcBounds(Font f,DataSink sink) {
            Rectangle2D ret=f.getStringBounds(str,frc);
            if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
            return ret;
        }
        @Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
            g.drawString(str,(float)x,(float)y);
            if(next!=null) {
                Rectangle2D bounds=g.getFont().getStringBounds(str,frc);
                next.draw(g,(float)(x+bounds.getWidth()),y,sink);                    
            }
        }
        private String str;
        private Node next;
        private static final long serialVersionUID=3468973598672346l;
    }
    
    private static class Formula implements Node {
        public Formula(String s) {
            if(s.charAt(0)!='$') {
                throw new FormattedStringException("Got into formula parse for string that didn't start with $.");
            }
            int dPos=s.indexOf('$',1);
            if(dPos<0) {
                throw new FormattedStringException("Got into formula parse for string that have a second $.");
            }
            int startPos=1;
            if(s.charAt(1)=='%') {
                startPos=s.indexOf(':');
                if(startPos>=s.length()) {
                    throw new FormattedStringException("A formula format had a %, but no :.");                    
                }
                format=s.substring(1,startPos);
                startPos++;
            }
            formula=new DataFormula(s.substring(startPos,dPos));
            if(dPos<s.length()-1) {
                next=parseGeneralNode(s.substring(dPos+1));
            }
        }
        @Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
            double val=formula.valueOf(sink,0,formula.getSafeElementRange(sink,0)[0]);
            String str;
            if(format!=null) {
                str=NumberFormatter.format(format,val);
            } else {
                str=NumberFormatter.format("%1.2g",val);
            }
            FormattedString fs=new FormattedString(str);
            fs.draw(g,(float)x,(float)y,sink);
            if(next!=null) {
                Rectangle2D bounds=g.getFont().getStringBounds(str,frc);
                next.draw(g,(float)(x+bounds.getWidth()),y,sink);
            }
        }
        @Override
        public Rectangle2D calcBounds(Font f,DataSink sink) {
            double val=formula.valueOf(sink,0, formula.getSafeElementRange(sink, 0)[0]);
            String str;
            if(format!=null) {
                str=NumberFormatter.format(format,val);
            } else {
                str=NumberFormatter.format("%1.2g",val);
            }
            FormattedString fs=new FormattedString(str);
            Rectangle2D ret=fs.getBounds(f,sink);
            if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
            return ret;
        }
        private String format;
        private DataFormula formula;
        private Node next;
        private static final long serialVersionUID=35609823750897468l;
    }
    
    private static class Superscript implements Node {
		public Superscript(String s) {
    		if (s.charAt(0)!='^') {
    			throw new FormattedStringException("Got into superscript parse for string that didn't start with ^.");
    		}
    		if (s.charAt(1)!='{') {
    			throw new FormattedStringException("Got into superscript parse for string that didn't start with ^{.");
    		}
    		int count = 1;
    		int index = 2;
    		String str="";
    		while (count > 0) {
    			if (index>=s.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (s.charAt(index)=='}') {
    				count--;
    				if (count>0) str+=s.charAt(index);
    			} else if (s.charAt(index)=='{') {
	    			str+=s.charAt(index);
    				count++;
    			} else {
	    			str+=s.charAt(index);
    			}
    			index++;
    		}
    		superNode = parseGeneralNode(str);
    		next = parseGeneralNode(s.substring(index));
    	}

        @Override
        public Rectangle2D calcBounds(Font f,DataSink sink) {
            Font newF = new Font(f.getFontName(),f.getStyle(), (int)(f.getSize()/1.5));
            Rectangle2D ret = superNode.calcBounds(newF,sink);
            double newY = ret.getMinY()-ret.getHeight()/2;
            ret.setRect(ret.getMinX(),newY,ret.getWidth(),ret.getHeight());
            if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
            return ret;
        }

        @Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
			Font old = g.getFont();
			// determine new position of y for superscript
            Font newF = new Font(old.getFontName(),old.getStyle(), (int)(old.getSize()/1.5));
            Rectangle2D bounds = superNode.calcBounds(newF,sink);
            double newY = y-bounds.getHeight()/2;
            bounds.setRect(bounds.getMinX(),newY,bounds.getWidth(),bounds.getHeight());
			g.setFont(newF);
			superNode.draw(g,x,newY,sink);
			g.setFont(old);
			if (next!=null) {
				next.draw(g,(float)(x+bounds.getWidth()),y,sink);
			}
		}
		
		private Node superNode,next;
		private static final long serialVersionUID = -745668823823372297L;
    }
    
    private static class Subscript implements Node {
		public Subscript(String s) {
    		if (s.charAt(0)!='_') {
    			throw new FormattedStringException("Got into subscript parse for string that didn't start with _.");
    		}
    		if (s.charAt(1)!='{') {
    			throw new FormattedStringException("Got into subscript parse for string that didn't start with _{.");
    		}
    		int count = 1;
    		int index = 2;
    		String str="";
    		while (count > 0) {
    			if (index>=s.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (s.charAt(index)=='}') {
    				count--;
    				if (count>0) str+=s.charAt(index);
    			} else if (s.charAt(index)=='{') {
	    			str+=s.charAt(index);
    				count++;
    			} else {
	    			str+=s.charAt(index);
    			}
    			index++;
    		}
    		subNode = parseGeneralNode(str);
    		next = parseGeneralNode(s.substring(index));
    		if (next instanceof Superscript) {
    			superNode = next;
    			next = ((Superscript)superNode).next;
    			((Superscript)superNode).next=null;
    		}
    	}

		@Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
			Font old = g.getFont();
			// determine new position of y for superscript
			g.setFont(new Font(old.getFontName(),old.getStyle(), (int)(old.getSize()/1.5)));
            Rectangle2D subBounds=subNode.calcBounds(g.getFont(),sink);
            double newY = y+(subBounds.getHeight()/2);
			subNode.draw(g,x,newY,sink);
			
            double width=subBounds.getWidth();
			g.setFont(old);
			if (superNode!=null) {
				superNode.draw(g,x,y,sink);
	            Rectangle2D superBounds=superNode.calcBounds(g.getFont(),sink);
	            if (superBounds.getWidth()>subBounds.getWidth()) {
	            	width = superBounds.getWidth();
	            }
			}
			if (next!=null) {
				next.draw(g,(float)(x+width),y,sink);
			}
		}

		@Override
        public Rectangle2D calcBounds(Font f, DataSink sink) {
			Font newF = new Font(f.getFontName(),f.getStyle(), (int)(f.getSize()/1.5));
			Rectangle2D ret = subNode.calcBounds(newF,sink);
			double newY = ret.getMinY()+ret.getHeight()/2;
			ret.setRect(ret.getMinX(),newY,ret.getWidth(),ret.getHeight());
			if (superNode!=null) {
				Rectangle2D sBounds = superNode.calcBounds(f,sink);
                ret=ret.createUnion(sBounds);
			}
			if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
			return ret;
		}
    	
		private Node subNode,superNode,next;
		private static final long serialVersionUID = -8342341733658856946L;
    }
    
    private static class FracNode implements Node {
		public FracNode(String numerator, String denominator, String s) {
    		numNode = parseGeneralNode(numerator);
    		denNode = parseGeneralNode(denominator);
    		next = parseGeneralNode(s);
    	}

		@Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
			Font old = g.getFont();
			g.setFont(new Font(old.getFontName(),old.getStyle(), (int)(old.getSize()/1.5)));
			
			Rectangle2D nBounds=numNode.calcBounds(g.getFont(),sink);
			Rectangle2D dBounds=denNode.calcBounds(g.getFont(),sink);
            System.out.println("num "+nBounds+"\ndenom "+dBounds);
			double len=0.0f;
			double newX=0.0f;
			boolean numLonger=false;
			if (nBounds.getWidth() > dBounds.getWidth()) {
				len = nBounds.getWidth();
				newX = x + (nBounds.getWidth()/2-dBounds.getWidth()/2);
				numLonger=true;
			} else {
				len = dBounds.getWidth();
				newX = x + (dBounds.getWidth()/2-nBounds.getWidth()/2);
				numLonger=false;
			}
			double newY = y-old.getSize2D()/2;
			
			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(old.getSize2D()/20.0f+0.5f));
			double minY = nBounds.getMinY();
            g.draw(new Line2D.Double(x,y+minY/2,x+len,y+minY/2));
			if (numLonger) {
				numNode.draw(g,x,newY-nBounds.getMaxY(),sink);
				denNode.draw(g,newX,newY+dBounds.getHeight(),sink);
			} else {
				numNode.draw(g,newX,newY-nBounds.getMaxY(),sink);
				denNode.draw(g,x,newY+dBounds.getHeight(),sink);
			}
			g.setStroke(oldStroke);
			g.setFont(old);
			if (next!=null) {
				next.draw(g,(float)(x+len),y,sink);
			}
		}

		@Override
        public Rectangle2D calcBounds(Font f, DataSink sink) {
			Font newF = new Font(f.getFontName(),f.getStyle(), (int)(f.getSize()/1.5));
			Rectangle2D ret=numNode.calcBounds(newF,sink);
			Rectangle2D denBounds=denNode.calcBounds(newF,sink);
			double len = (ret.getWidth()>denBounds.getWidth())?ret.getWidth():denBounds.getWidth();
            double newY = -f.getSize2D()/2;
			
			ret.setRect(0,newY-ret.getHeight(),len,denBounds.getHeight()+ret.getHeight()+denBounds.getMaxY());
			if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
			return ret;
		}
    	private Node numNode,denNode,next;
		private static final long serialVersionUID = -4761279784293355172L;
    }
    
    private static class OverUnderNode implements Node {
		public OverUnderNode(Node subscript,Node superscript, String s) {
    		subNode = subscript;
    		superNode = superscript;
    		next = parseGeneralNode(s);
            if(overUnderCharMap==null) {
                overUnderCharMap=new HashMap<String,String>();
                overUnderCharMap.put("sum",Character.toString((char)(0x03A3)));
                overUnderCharMap.put("lim","lim");
                overUnderCharMap.put("int",Character.toString((char)(0x222B)));
            }
    	}
    	private static final int FONT_SIZE=2;
    	
		@Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
			Font oldFont = g.getFont();
            Font sigmaF = new Font(oldFont.getFontName(),oldFont.getStyle(), (oldFont.getSize()*FONT_SIZE));
            Font ssFont = new Font(oldFont.getFontName(),oldFont.getStyle(), (oldFont.getSize()/2));
            Rectangle2D ret = sigmaF.getStringBounds(Character.toString((char)(0x03A3)),frc);
            double sigmaY = y-ret.getMinY()/8;
            ret=new Rectangle2D.Double(x,sigmaY+ret.getMinY()*0.8,ret.getWidth(),ret.getHeight()*0.8);
			g.setFont(sigmaF);
			g.drawString(Character.toString((char)(0x03A3)),(float)x,(float)sigmaY);
            double center = x + ret.getWidth()/2;
            g.setFont(ssFont);
            if (superNode!=null) {
                Rectangle2D pBounds=superNode.calcBounds(ssFont,sink);
                double newY = ret.getMinY() - pBounds.getMaxY();
				double newX = center - pBounds.getWidth()/2;
				superNode.draw(g,newX,newY,sink);
            }
            if(subNode!=null) {
                Rectangle2D bBounds = subNode.calcBounds(ssFont,sink);
				double newY = ret.getMaxY() - bBounds.getMinY();
				double newX = center - bBounds.getWidth()/2;
				subNode.draw(g,newX,newY,sink);
            }
			g.setFont(oldFont);
			if(next!=null) {
                next.draw(g,(float)(x+ret.getWidth()),y,sink);
            }
		}

		@Override
        public Rectangle2D calcBounds(Font f, DataSink sink) {
			Font sigmaF = new Font(f.getFontName(),f.getStyle(), f.getSize()*FONT_SIZE);
			Font ssFont = new Font(f.getFontName(),f.getStyle(), f.getSize()/2);
			Rectangle2D ret = sigmaF.getStringBounds(Character.toString((char)(0x03A3)),frc);
            double sigmaY = -ret.getMinY()/8;
            ret=new Rectangle2D.Double(ret.getMinX(),sigmaY+ret.getMinY()*0.8,ret.getWidth(),ret.getHeight()*0.8);
			if (subNode!=null) {
				Rectangle2D bBounds=subNode.calcBounds(ssFont,sink);
				double newY=ret.getMaxY();
				bBounds.setRect(bBounds.getMinX(),newY,bBounds.getWidth(),bBounds.getHeight());
				ret=ret.createUnion(bBounds);
			}
            if(superNode!=null) {
                Rectangle2D pBounds = superNode.calcBounds(ssFont,sink);
                double newY=ret.getMinY()-pBounds.getHeight();
                pBounds.setRect(pBounds.getMinX(),newY,pBounds.getWidth(),pBounds.getHeight());
                ret=ret.createUnion(pBounds);                
            }
			if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
			return ret;
		}
    	private Node subNode,superNode,next;
		private static final long serialVersionUID = -3567675439043469761L;
    }
    
    private static class LimitNode implements Node {
		public LimitNode(Node subscript,String s) {
    		subNode = subscript;
    		next = parseGeneralNode(s);
    	}
    	// lim is 75%; sub is 50%
		@Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
			Font oldFont = g.getFont();
			g.setFont(g.getFont().deriveFont(oldFont.getSize2D()*0.75f));
			double limY = y - (oldFont.getSize2D()/2-g.getFont().getSize2D()/2);
			g.drawString("lim",(float)x,(float)limY);
			Rectangle2D bounds = g.getFont().getStringBounds("lim",frc);
			double center = x + bounds.getWidth()/2;
			g.setFont(g.getFont().deriveFont(oldFont.getSize2D()*0.5f));
			Rectangle2D sBounds = subNode.calcBounds(g.getFont(),sink);
			double newX = center - sBounds.getWidth()/2;
			double newY = limY + g.getFont().getSize2D();
			subNode.draw(g,newX,newY,sink);
			g.setFont(oldFont);
			if(next!=null) {
                next.draw(g,x+bounds.getWidth(),y,sink);
            }
		}

		@Override
        public Rectangle2D calcBounds(Font f, DataSink sink) {
			Font limFont = f.deriveFont(f.getSize2D()*0.75f);
			Font subFont = f.deriveFont(f.getSize2D()*0.5f);
			Rectangle2D ret = limFont.getStringBounds("lim",frc);
			double limY = ret.getMinY()-(f.getSize2D()/2-limFont.getSize2D()/2);
			ret.setRect(ret.getMinX(),limY,ret.getWidth(),ret.getHeight());
			Rectangle2D sBounds = subNode.calcBounds(subFont,sink);
			double newY = limY + limFont.getSize2D() + subFont.getSize2D();
			double newX = (ret.getMinX() + ret.getWidth()/2) - sBounds.getWidth()/2;
			sBounds.setRect(newX,newY,sBounds.getWidth(),sBounds.getHeight());
			if (sBounds.getWidth() > ret.getWidth()) {
				ret.setRect(sBounds.getMinX(),ret.getMinY(),sBounds.getWidth(),ret.getHeight()+sBounds.getHeight());
			}
			ret.createUnion(sBounds);
			if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
			return ret;
		}
    	private Node subNode,next;
		private static final long serialVersionUID = 8292753810613813488L;
    }
    
    private static class IntegralNode implements Node {
		public IntegralNode(Node subscript,Node superscript,String s) {
    		subNode = subscript;
    		superNode = superscript;
    		next = parseGeneralNode(s);
    	}
		//charMap.put("int",(char)(0x222B));
		@Override
        public void draw(Graphics2D g,double x,double y,DataSink sink) {
			Font oldFont = g.getFont();
			g.setFont(g.getFont().deriveFont(oldFont.getSize2D()*2.0f));
			g.drawString(Character.toString((char)(0x222B)),(float)x,(float)y);
			
//			g.drawLine((int)x,(int)y,(int)x+20,(int)y);
			
			Rectangle2D bounds = g.getFont().getStringBounds(""+(char)(0x222B),frc);
			if (subNode!=null) {
				g.setFont(g.getFont().deriveFont(oldFont.getSize2D()*0.6f));
				double newX = x+bounds.getWidth()-bounds.getWidth()*0.35;
				double newY = y+g.getFont().getSize2D()/2;
				subNode.draw(g,newX,newY,sink);
				Rectangle2D bBounds = subNode.calcBounds(g.getFont(),sink);
				double width = bBounds.getWidth();
				
				if (superNode!=null) {
					newX = x+(float)bounds.getWidth()-(float)(bounds.getWidth()*0.15);
					newY = (y-oldFont.getSize2D()*2.0f)+g.getFont().getSize2D()+g.getFont().getSize2D()*0.15f; // off a bit
					superNode.draw(g,newX,newY,sink);
					Rectangle2D pBounds = superNode.calcBounds(g.getFont(),sink);
					if (pBounds.getWidth() > bBounds.getWidth()-(bounds.getWidth()*0.35)) {
						width = pBounds.getWidth();
					}
				}

				bounds.setRect(bounds.getMinX(),bounds.getMinY(),bounds.getWidth()+width,bounds.getHeight());
			}
			g.setFont(oldFont);
			if(next!=null) {
                next.draw(g,x+bounds.getWidth(),y,sink);
            }
		}

		@Override
        public Rectangle2D calcBounds(Font f, DataSink sink) {
			Font intFont = f.deriveFont(f.getSize2D()*2.0f);
			Font smFont = f.deriveFont(f.getSize2D()*0.6f);
			Rectangle2D ret = intFont.getStringBounds(""+(char)(0x222B),frc);
			if (subNode!=null) {
				Rectangle2D bBounds = subNode.calcBounds(smFont,sink);
				double width = bBounds.getWidth();
				if (superNode!=null) {
					Rectangle2D pBounds = superNode.calcBounds(smFont,sink);
					if (pBounds.getWidth() > bBounds.getWidth()-(ret.getWidth()*0.35)) {
						width = pBounds.getWidth();
					}
				}
				ret.setRect(ret.getMinX(),ret.getMinY(),ret.getWidth()+width,ret.getHeight());
			}
			if(next!=null) {
                Rectangle2D nBounds=next.calcBounds(f,sink);
                nBounds.setRect(nBounds.getMinX()+ret.getMaxX(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
                ret=ret.createUnion(nBounds);
            }
			return ret;
		}
    	private Node subNode,superNode,next;
		private static final long serialVersionUID = -4292702461029510297L;
    }
    
    private static class ParensNode implements Node {
        public ParensNode(String c,String s) {
            c=c.trim();
            style=-1;
            for(int i=0; i<openStyle.length && style==-1; ++i) {
                if(c.charAt(0)==openStyle[i]) { style=i; }
            }
            if(c.charAt(c.length()-1)==closeStyle[style]) {
                contents=parseGeneralNode(c.substring(1,c.length()-1));
            } else {
                contents=parseGeneralNode(c.substring(1,c.length()));                
            }
            next = parseGeneralNode(s);            
        }
        @Override
        public Rectangle2D calcBounds(Font f, DataSink sink) {
            Rectangle2D cBounds=contents.calcBounds(f,sink);
            Rectangle2D pBounds=f.getStringBounds(Character.toString(openStyle[style]),frc);
            Font pFont=f;
            if(pBounds.getHeight()<cBounds.getHeight()) {
                pFont=new Font(f.getName(),f.getStyle(),(int)(f.getSize2D()*cBounds.getHeight()/(0.8*pBounds.getHeight())));
                pBounds=pFont.getStringBounds(Character.toString(openStyle[style]),frc);
                pBounds=new Rectangle2D.Double(pBounds.getMinX(),pBounds.getMinY()*0.8,pBounds.getWidth(),pBounds.getHeight()*0.8);
            }
            Rectangle2D nBounds=next.calcBounds(f,sink);
            Rectangle2D tBounds=new Rectangle2D.Double(0,pBounds.getMinY(),cBounds.getWidth()+2*pBounds.getWidth(),pBounds.getHeight());
            nBounds=new Rectangle2D.Double(tBounds.getWidth(),nBounds.getMinY(),nBounds.getWidth(),nBounds.getHeight());
            return tBounds.createUnion(nBounds);
        }

        @Override
        public void draw(Graphics2D g, double x, double y, DataSink sink) {
            Font f=g.getFont();
            Rectangle2D cBounds=contents.calcBounds(f,sink);
            Rectangle2D pBounds=f.getStringBounds(Character.toString(openStyle[style]),frc);
            Font pFont=f;
            if(pBounds.getHeight()<cBounds.getHeight()) {
                pFont=new Font(f.getName(),f.getStyle(),(int)(f.getSize2D()*cBounds.getHeight()/(0.8*pBounds.getHeight())));
                pBounds=pFont.getStringBounds(Character.toString(openStyle[style]),frc);
                pBounds=new Rectangle2D.Double(pBounds.getMinX(),pBounds.getMinY()*0.8,pBounds.getWidth(),pBounds.getHeight()*0.8);
            }
            g.setFont(pFont);
            g.drawString(Character.toString(openStyle[style]),(float)x,(float)(y));
            g.drawString(Character.toString(closeStyle[style]),(float)(x+cBounds.getWidth()+pBounds.getWidth()),(float)(y));
            g.setFont(f);
            double cy=y+pBounds.getMinY()-cBounds.getMinY();
            contents.draw(g,x+pBounds.getWidth(),cy,sink);
            next.draw(g,x+2*pBounds.getWidth()+cBounds.getWidth(),y,sink);
        }
        private int style;
        private Node contents,next;
        private static final long serialVersionUID = -5686129255579063805L;
    }
    
    public static class FormattedStringException extends RuntimeException {
        public FormattedStringException(String message) {   
        	super(message);
        	JOptionPane.showMessageDialog(null,message,"Formatted String Error",JOptionPane.ERROR_MESSAGE);
        }
        private static final long serialVersionUID=8883465324323356l;
    }
    
    private static interface NodeBuilder {
    	Node buildNode(String code,String rest);
    }
    
    private static class SpecialCharBuilder implements NodeBuilder {
    	public SpecialCharBuilder() {
    		charMap = new HashMap<String,Character>();
    		charMap.put("Alpha",(char)(0x0391));
    		charMap.put("alpha",(char)(0x03B1));
    		charMap.put("Beta",(char)(0x0392));
    		charMap.put("beta",(char)(0x03B2));
    		charMap.put("Gamma",(char)(0x0393));
    		charMap.put("gamma",(char)(0x03B3));
    		charMap.put("Delta",(char)(0x0394));
            charMap.put("delta",(char)(0x03B4));
    		charMap.put("Epsilon",(char)(0x0395));
    		charMap.put("epsilon",(char)(0x03B5));
    		charMap.put("Zeta",(char)(0x0396));
    		charMap.put("zeta",(char)(0x03B6));
    		charMap.put("Eta",(char)(0x0397));
    		charMap.put("eta",(char)(0x03B7));
    		charMap.put("Theta",(char)(0x0398));
    		charMap.put("theta",(char)(0x03B8));
    		charMap.put("Iota",(char)(0x0399));
    		charMap.put("iota",(char)(0x03B9));
    		charMap.put("Kappa",(char)(0x039A));
    		charMap.put("kappa",(char)(0x03BA));
    		charMap.put("Lambda",(char)(0x039B));
    		charMap.put("lambda",(char)(0x03BB));
    		charMap.put("Mu",(char)(0x039C));
    		charMap.put("mu",(char)(0x03BC));
    		charMap.put("Nu",(char)(0x039D));
    		charMap.put("nu",(char)(0x03BD));
    		charMap.put("Xi",(char)(0x039E));
    		charMap.put("xi",(char)(0x03BE));
    		charMap.put("Omikron",(char)(0x039F));
    		charMap.put("omikron",(char)(0x03BF));
    		charMap.put("Pi",(char)(0x03A0));
    		charMap.put("pi",(char)(0x03C0));
    		charMap.put("Rho",(char)(0x03A1));
    		charMap.put("rho",(char)(0x03C1));
    		charMap.put("Sigma",(char)(0x03A3));
    		charMap.put("sigma",(char)(0x03C3));
    		charMap.put("Tau",(char)(0x03A4));
    		charMap.put("tau",(char)(0x03C4));
    		charMap.put("Upsilon",(char)(0x03A5));
    		charMap.put("upsilon",(char)(0x03C5));
    		charMap.put("Phi",(char)(0x03A6));
    		charMap.put("phi",(char)(0x03C6));
    		charMap.put("Chi",(char)(0x03A7));
    		charMap.put("chi",(char)(0x03C7));
    		charMap.put("Psi",(char)(0x03A8));
    		charMap.put("psi",(char)(0x03C8));
    		charMap.put("Omega",(char)(0x03A9));
    		charMap.put("omega",(char)(0x03C9));
            
            // Other math stuff in LaTeX
            charMap.put("partial",(char)(0x2202));
            charMap.put("forall",(char)(0x2200));
            charMap.put("exists",(char)(0x2203));
            charMap.put("pm",(char)(0x00B1));
            charMap.put("mp",(char)(0x2213));
            charMap.put("cdot",(char)(0x2219));
            charMap.put("O",(char)(0x00D8));
            charMap.put("times",(char)(0x00D7));
            charMap.put("div",(char)(0x00F7));
            charMap.put("vee",(char)(0x2227));
            charMap.put("wedge",(char)(0x2228));
            charMap.put("oplus",(char)(0x2295));
            charMap.put("ominus",(char)(0x2296));
            charMap.put("otimes",(char)(0x2297));
            charMap.put("oslash",(char)(0x2298));
            charMap.put("odot",(char)(0x2299));
            charMap.put("cap",(char)(0x2229));
            charMap.put("cup",(char)(0x222A));
            charMap.put("uplus",(char)(0x228E));
            charMap.put("sqcap",(char)(0x2293));
            charMap.put("sqcup",(char)(0x2294));
            charMap.put("triangleleft",(char)(0x22B2));
            charMap.put("triangleright",(char)(0x22B3));
            charMap.put("infty",(char)(0x221E));
            charMap.put("to",(char)(0x2192));
            charMap.put("leq",(char)(0x2264));
            charMap.put("geq",(char)(0x2265));
            charMap.put("prec",(char)(0x227A));
            charMap.put("succ",(char)(0x227B));
            charMap.put("preceq",(char)(0x227C));
            charMap.put("succeq",(char)(0x227D));
            charMap.put("ll",(char)(0x226A));
            charMap.put("gg",(char)(0x226B));
            charMap.put("subset",(char)(0x2282));
            charMap.put("supset",(char)(0x2283));
            charMap.put("subseteq",(char)(0x2286));
            charMap.put("supseteq",(char)(0x2287));
            charMap.put("sqsubset",(char)(0x228F));
            charMap.put("sqsupset",(char)(0x2290));
            charMap.put("sqsubseteq",(char)(0x2291));
            charMap.put("sqsupseteq",(char)(0x2292));
            charMap.put("in",(char)(0x220A));
            charMap.put("ni",(char)(0x220D));
            charMap.put("vdash",(char)(0x22A2));
            charMap.put("dashv",(char)(0x22A3));
            charMap.put("smile",(char)(0x203F));
            charMap.put("frown",(char)(0x2040));
            charMap.put("mid",(char)(0x2223));
            charMap.put("parallel",(char)(0x2225));
            charMap.put("neq",(char)(0x2260));
            charMap.put("perp",(char)(0x22A5));
            charMap.put("equiv",(char)(0x2261));
            charMap.put("cong",(char)(0x2245));
            charMap.put("sim",(char)(0x223C));
            charMap.put("bowtie",(char)(0x22C8));
            charMap.put("simeq",(char)(0x2243));
            charMap.put("propto",(char)(0x221D));
            charMap.put("asymp",(char)(0x224D));
            charMap.put("models",(char)(0x22A8));
            charMap.put("approx",(char)(0x2248));
            charMap.put("doteq",(char)(0x2250));
            charMap.put("^",'^');
            charMap.put("_",'_');
            
    	}
    	@Override
        public Node buildNode(String code, String rest) {
    		return new PlainText(Character.toString(charMap.get(code)),rest);
    	}
    	private HashMap<String,Character> charMap;
    }
    
    private static class IntegralBuilder implements NodeBuilder {
    	@Override
        public Node buildNode(String code, String rest) {
    		if (rest.charAt(0)!='_') {
    			return new IntegralNode(null,null,rest);
    		}
    		Node subNode=null,superNode=null;
    		int count = 1;
    		int index = 2;
    		String str="";
    		while (count > 0) {
    			if (index>=rest.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (rest.charAt(index)=='}') {
    				count--;
    				if (count>0) str+=rest.charAt(index);
    			} else if (rest.charAt(index)=='{') {
	    			str+=rest.charAt(index);
    				count++;
    			} else {
	    			str+=rest.charAt(index);
    			}
    			index++;
    		}
    		subNode = parseGeneralNode(str);
    		if (rest.charAt(index)!='^') {
    			return new IntegralNode(subNode,null,rest.substring(index));
    		}
    		count = 1;
    		index+=2;
    		str="";
    		while (count > 0) {
    			if (index>=rest.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (rest.charAt(index)=='}') {
    				count--;
    				if (count>0) str+=rest.charAt(index);
    			} else if (rest.charAt(index)=='{') {
	    			str+=rest.charAt(index);
    				count++;
    			} else {
	    			str+=rest.charAt(index);
    			}
    			index++;
    		}
    		superNode = parseGeneralNode(str);
    		return new IntegralNode(subNode,superNode,rest.substring(index));
    	}
    }
    
    private static class LimitBuilder implements NodeBuilder {
    	@Override
        public Node buildNode(String code, String rest) {
    		int count = 1;
    		int index = 2;
    		String str="";
    		while (count > 0) {
    			if (index>=rest.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (rest.charAt(index)=='}') {
    				count--;
    				if (count>0) str+=rest.charAt(index);
    			} else if (rest.charAt(index)=='{') {
	    			str+=rest.charAt(index);
    				count++;
    			} else {
	    			str+=rest.charAt(index);
    			}
    			index++;
    		}
    		Node subNode = parseGeneralNode(str);
    		return new LimitNode(subNode,rest.substring(index));
    	}
    }
    
    private static class SumBuilder implements NodeBuilder {
    	@Override
        public Node buildNode(String code, String rest) {
    		if (rest==null || rest.length()==0 || (rest.charAt(0)!='_' && rest.charAt(0)!='^')) {
    			return new OverUnderNode(null,null,rest);
    		}
    		Node subNode=null,superNode=null;
            int count,index=0;
            String str;
            if(rest.charAt(0)=='_') {
        		count = 1;
        		index = 2;
        		str="";
        		while (count > 0) {
        			if (index>=rest.length()) {
        				throw new FormattedStringException("No closing curly bracket ( } )");
        			}
        			if (rest.charAt(index)=='}') {
        				count--;
        				if (count>0) str+=rest.charAt(index);
        			} else if (rest.charAt(index)=='{') {
    	    			str+=rest.charAt(index);
        				count++;
        			} else {
    	    			str+=rest.charAt(index);
        			}
        			index++;
        		}
        		subNode = parseGeneralNode(str);
            }
            if(rest.length()>index && rest.charAt(index)=='^') {            
        		count = 1;
        		index+=2;
        		str="";
        		while (count > 0) {
        			if (index>=rest.length()) {
        				throw new FormattedStringException("No closing curly bracket ( } )");
        			}
        			if (rest.charAt(index)=='}') {
        				count--;
        				if (count>0) str+=rest.charAt(index);
        			} else if (rest.charAt(index)=='{') {
    	    			str+=rest.charAt(index);
        				count++;
        			} else {
    	    			str+=rest.charAt(index);
        			}
        			index++;
        		}
        		superNode = parseGeneralNode(str);
            }
    		return new OverUnderNode(subNode,superNode,rest.substring(index));
    	}
    }
    
    private static class FracBuilder implements NodeBuilder {
    	@Override
        public Node buildNode(String code, String rest) {
    		if (rest.charAt(0)!='{') {
    			throw new FormattedStringException("Incorrect Format for \\frac. Usage \\frac{1}{2}");
    		}
    		int index=1;
    		int count=1;
    		String numerator="";
    		String denominator="";
    		while (count > 0) {
    			if (index>=rest.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (rest.charAt(index)=='}') {
    				count--;
    				if (count>0) numerator+=rest.charAt(index);
    			} else if (rest.charAt(index)=='{') {
    				numerator+=rest.charAt(index);
    				count++;
    			} else {
    				numerator+=rest.charAt(index);
    			}
    			index++;
    		}
    		index++;
    		count=1;
    		while (count > 0) {
    			if (index>=rest.length()) {
    				throw new FormattedStringException("No closing curly bracket ( } )");
    			}
    			if (rest.charAt(index)=='}') {
    				count--;
    				if (count>0) denominator+=rest.charAt(index);
    			} else if (rest.charAt(index)=='{') {
    				denominator+=rest.charAt(index);
    				count++;
    			} else {
    				denominator+=rest.charAt(index);
    			}
    			index++;
    		}
    		return new FracNode(numerator,denominator,rest.substring(index));
    	}
    }
}
