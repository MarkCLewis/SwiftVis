/*
 * Created on Jul 2, 2005
 */
package edu.swri.swiftvis.scheme;

public class SchemeBoolean implements SchemeElement {
    private SchemeBoolean(boolean val) {
        value=val;
    }

    @Override
    public SchemeElement eval(SchemeEnvironment env) {
        return this;
    }

    @Override
    public SchemeElement car() {
        throw new SchemeException("Tried to take car of boolean: "+value);
    }

    @Override
    public SchemeElement cdr() {
        throw new SchemeException("Tried to take cdr of boolean: "+value);
    }
    
    @Override
    public String toString() {
        return Boolean.toString(value);
    }
    
    public boolean booleanValue() {
        return value;
    }

    private final boolean value;
    
    public static SchemeBoolean create(boolean val) {
        if(val) return TRUE;
        else return FALSE;
    }
    
    public static final SchemeBoolean TRUE=new SchemeBoolean(true);
    public static final SchemeBoolean FALSE=new SchemeBoolean(false);
    private static final long serialVersionUID=8458349993457237l;
}
