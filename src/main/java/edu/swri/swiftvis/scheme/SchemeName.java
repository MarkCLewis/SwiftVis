/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

public class SchemeName implements SchemeElement {
    public SchemeName(String n) {
        name=n;
    }

    @Override
    public SchemeElement eval(SchemeEnvironment env) {
        return env.getNameValue(this);
    }

    @Override
    public SchemeElement car() {
        throw new SchemeException("Can't get car of a name: "+name);
    }

    @Override
    public SchemeElement cdr() {
        throw new SchemeException("Can't get cdr of a name: "+name);
    }

    public double numericValue() {
        throw new SchemeException("A name does not have a numerical value.  You must evaluate it first: "+name);
    }
    
    @Override
    public String toString() { return name; }

    private final String name;
    private static final long serialVersionUID=99457237236724375l;
}
