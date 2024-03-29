/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

public class ConsCell implements SchemeElement {
    public ConsCell(SchemeElement hd,SchemeElement tl) {
        head=hd;
        tail=tl;
    }

    @Override
    public SchemeElement eval(SchemeEnvironment env) {
        if(head instanceof SchemeFunction) {
            if(tail instanceof ConsCell) {
                SchemeFunction func=(SchemeFunction)head;
                return func.execute(env,(ConsCell)tail);
            } else {
                throw new SchemeException("Invalid function call.  Arguments not a list.");
            }
        } else if(head instanceof SchemeName) {
            if(tail instanceof ConsCell) {
                SchemeFunction func=(SchemeFunction)env.getNameValue(head.toString());
                if(func==null) throw new SchemeException("Couldn't find name: "+head.toString());
                return func.execute(env,(ConsCell)tail);
            } else {
                throw new SchemeException("Invalid function call.  Arguments not a list.");
            }            
        } else {
            if(this==nullInst) return this;
            SchemeElement headE=head.eval(env);
            if(headE instanceof SchemeFunction) {
                if(tail instanceof ConsCell) {
                    SchemeFunction func=(SchemeFunction)headE;
                    return func.execute(env,(ConsCell)tail);
                } else {
                    throw new SchemeException("Invalid function call.  Arguments not a list.");
                }
            } else {
                //return this;
                throw new SchemeException("Eval on cons cell that doesn't start with a function.\nhead="+head+", tail="+tail);
            }
        }
    }

    @Override
    public SchemeElement car() {
        if(head==null) throw new SchemeException("Taking car on cons cell without head.");
        return head;
    }

    @Override
    public SchemeElement cdr() {
        if(tail==null) throw new SchemeException("Taking cdr on cons cell without tail.");
        return tail;
    }

    public int length() {
        int ret=0;
        for(SchemeElement rover=this; rover!=ConsCell.nullInstance(); rover=rover.cdr()) {
            ret++;
        }
        return ret;
    }
    
    @Override
    public String toString() {
        StringBuffer buf=new StringBuffer("(");
        for(SchemeElement rover=this; rover!=ConsCell.nullInstance(); rover=rover.cdr()) {
            if(rover!=this) buf.append(' ');
            buf.append(rover.car().toString());
        }
        buf.append(')');
        return new String(buf);
    }
    
    public static ConsCell nullInstance() {
        return nullInst;
    }

    private final SchemeElement head;
    private final SchemeElement tail;
    
    private static final ConsCell nullInst=new ConsCell(null,null);
    
    private static final long serialVersionUID=342569087456332l;
}
