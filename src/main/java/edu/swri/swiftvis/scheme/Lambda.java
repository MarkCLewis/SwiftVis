/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

import edu.swri.swiftvis.util.ArrayUtility;

public class Lambda implements SchemeFunction {
    public Lambda(ConsCell a,ConsCell b,SchemeEnvironment env) {
        argNames=new String[a.length()];
        int i=0;
        for(SchemeElement rover=a; rover!=ConsCell.nullInstance(); rover=rover.cdr(),++i) {
            argNames[i]=rover.car().toString();
        }
        body=bindNames(b,env);
    }

    @Override
    public SchemeElement car() {
        throw new SchemeException("Tried to take car of a lambda: "+this);
    }

    @Override
    public SchemeElement cdr() {
        throw new SchemeException("Tried to take cdr of a lambda: "+this);
    }

    @Override
    public SchemeElement execute(SchemeEnvironment env, ConsCell args) {
        SchemeEnvironment local=new SchemeEnvironment(env.getGlobalEnvironment(),false);
        int i=0;
        for(SchemeElement rover=args; i<argNames.length; rover=rover.cdr(),++i) {
            local.addNameValue(argNames[i],rover.car().eval(env));
        }
        return body.eval(local);
    }

    @Override
    public SchemeElement eval(SchemeEnvironment env) {
        return this;
    }
    
    public void doBinding(SchemeEnvironment env) {
        body=bindNames(body,env);
    }
    
    private ConsCell bindNames(ConsCell b,SchemeEnvironment env) {
        if(b==ConsCell.nullInstance()) {
            return b;
        } else if(b.car() instanceof SchemeName) {
            if(ArrayUtility.getIndex(argNames,b.car().toString())<0) {
                SchemeElement boundValue=env.getNameValue(b.car().toString());
                if(boundValue==null) {
                    return new ConsCell(b.car(),bindNames((ConsCell)b.cdr(),env));
                } else {
                    return new ConsCell(boundValue,bindNames((ConsCell)b.cdr(),env));
                }
            } else {
                return new ConsCell(b.car(),bindNames((ConsCell)b.cdr(),env));
            }
        } else if(b.car() instanceof ConsCell) {
            return new ConsCell(bindNames((ConsCell)b.car(),env),bindNames((ConsCell)b.cdr(),env));
        } else {
            return new ConsCell(b.car(),bindNames((ConsCell)b.cdr(),env));            
        }
    }
    
    private String[] argNames;
    private ConsCell body;
    private static final long serialVersionUID=11846098357623546l;
}
