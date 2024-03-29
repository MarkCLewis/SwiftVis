/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

public class SchemeValue extends Object implements SchemeElement {
    public SchemeValue(String val) {
        value=Double.parseDouble(val);
    }
    
    public SchemeValue(double val) {
        value=val;
    }

    @Override
    public SchemeElement eval(SchemeEnvironment env) {
        return this;
    }

    @Override
    public SchemeElement car() {
        throw new SchemeException("Tried to take car of value: "+value);
    }

    @Override
    public SchemeElement cdr() {
        throw new SchemeException("Tried to take cdr of value: "+value);
    }

    public double numericValue() {
        return value;
    }
    
    @Override
    public String toString() {
        if(Math.round(value)==value) {
            return Integer.toString((int)value);
        }
        return Double.toString(value);
    }
    
    private final double value;
    private static final long serialVersionUID=346232325656347458l;
}
