/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * This class encapsulates the execution environment of a scheme program.  It has all the names
 * that are currently bound to values and what values they are bound to.  We have to be able to layer
 * these so that local variables can be definied without overwriting the global ones that they might
 * mask.
 * 
 * @author Mark Lewis
 */
public class SchemeEnvironment {
    public SchemeEnvironment(SchemeEnvironment p,boolean isGlobal) {
        parentEnv=p;
        treatAsGlobal=isGlobal;
    }
    
    public SchemeElement addNameValue(SchemeName name,SchemeElement value) {
        return addNameValue(name.toString(),value);
    }
    
    public SchemeElement addNameValue(String name,SchemeElement value) {
        hash.put(name,value);
        return value;
    }
    
    public SchemeElement getNameValue(SchemeName name) {
        return getNameValue(name.toString());
    }
    
    public SchemeElement getNameValue(String name) {
        SchemeElement ret=hash.get(name);
        if(ret==null && parentEnv!=null) return parentEnv.getNameValue(name);
        return ret;
    }
    
    public SchemeEnvironment getGlobalEnvironment() {
        if(parentEnv==null || treatAsGlobal) return this;
        return parentEnv.getGlobalEnvironment();
    }
    
    public void clearName(String name) {
        hash.remove(name);
    }
    
    public String knownNames() {
        StringBuffer buf=new StringBuffer();
        int cnt=0;
        String[] keys=new String[hash.size()];
        hash.keySet().toArray(keys);
        Arrays.sort(keys);
        for(String s:keys) {
            buf.append(s);
            cnt+=s.length()/10+1;
            int spaces=11-s.length()%10;
            for(int i=0; i<spaces; ++i) buf.append(" ");
            if(cnt%5==0) buf.append("\n");
        }
        return buf.toString();
    }
    
    public static synchronized SchemeEnvironment defaultGlobalEnvironment() {
        if(globalEnv==null) {
            globalEnv=new SchemeEnvironment(null,true);
            globalEnv.addNameValue("+",new HardcodedFunction.Plus());
            globalEnv.addNameValue("-",new HardcodedFunction.Minus());
            globalEnv.addNameValue("*",new HardcodedFunction.Mult());
            globalEnv.addNameValue("/",new HardcodedFunction.Div());
            globalEnv.addNameValue("pow",new HardcodedFunction.Pow());
            globalEnv.addNameValue("sin",new HardcodedFunction.Sin());
            globalEnv.addNameValue("cos",new HardcodedFunction.Cos());
            globalEnv.addNameValue("tan",new HardcodedFunction.Tan());
            globalEnv.addNameValue("sqrt",new HardcodedFunction.Sqrt());
            globalEnv.addNameValue("<",new HardcodedFunction.LT());
            globalEnv.addNameValue(">",new HardcodedFunction.GT());
            globalEnv.addNameValue("<=",new HardcodedFunction.LTE());
            globalEnv.addNameValue(">=",new HardcodedFunction.GTE());
            globalEnv.addNameValue("=",new HardcodedFunction.EQ());
            globalEnv.addNameValue("not",new HardcodedFunction.Not());
            globalEnv.addNameValue("and",new HardcodedFunction.And());
            globalEnv.addNameValue("or",new HardcodedFunction.Or());
            globalEnv.addNameValue("xor",new HardcodedFunction.Xor());
            globalEnv.addNameValue("null?",new HardcodedFunction.isNull());
            globalEnv.addNameValue("number?",new HardcodedFunction.isNumber());
            globalEnv.addNameValue("symbol?",new HardcodedFunction.isSymbol());
            globalEnv.addNameValue("pair?",new HardcodedFunction.isPair());
            globalEnv.addNameValue("eq?",new HardcodedFunction.isEq());
            globalEnv.addNameValue("eqv?",new HardcodedFunction.isEqv());
            globalEnv.addNameValue("equals?",new HardcodedFunction.isEquals());
            globalEnv.addNameValue("if",new HardcodedFunction.If());
            globalEnv.addNameValue("cond",new HardcodedFunction.Cond());
            globalEnv.addNameValue("let",new HardcodedFunction.Letrec());
            globalEnv.addNameValue("letrec",new HardcodedFunction.Letrec());
            globalEnv.addNameValue("begin",new HardcodedFunction.Begin());
            globalEnv.addNameValue("cons",new HardcodedFunction.Cons());
            globalEnv.addNameValue("list",new HardcodedFunction.MakeList());
            globalEnv.addNameValue("car",new HardcodedFunction.Car());
            globalEnv.addNameValue("cdr",new HardcodedFunction.Cdr());
            globalEnv.addNameValue("caar",new HardcodedFunction.Caar());
            globalEnv.addNameValue("cadr",new HardcodedFunction.Cadr());
            globalEnv.addNameValue("cdar",new HardcodedFunction.Cdar());
            globalEnv.addNameValue("cddr",new HardcodedFunction.Cddr());
            globalEnv.addNameValue("caaar",new HardcodedFunction.Caaar());
            globalEnv.addNameValue("caadr",new HardcodedFunction.Caadr());
            globalEnv.addNameValue("cadar",new HardcodedFunction.Cadar());
            globalEnv.addNameValue("caddr",new HardcodedFunction.Caddr());
            globalEnv.addNameValue("cdaar",new HardcodedFunction.Cdaar());
            globalEnv.addNameValue("cdadr",new HardcodedFunction.Cdadr());
            globalEnv.addNameValue("cddar",new HardcodedFunction.Cddar());
            globalEnv.addNameValue("cdddr",new HardcodedFunction.Cdddr());
            globalEnv.addNameValue("length",new HardcodedFunction.Length());
            globalEnv.addNameValue("quote",new HardcodedFunction.Quote());
            globalEnv.addNameValue("eval",new HardcodedFunction.Eval());
            globalEnv.addNameValue("define",new HardcodedFunction.Define());
            globalEnv.addNameValue("lambda",new HardcodedFunction.LambdaMaker());
            globalEnv.addNameValue("display",new HardcodedFunction.Display());
            globalEnv.addNameValue("newline",new HardcodedFunction.Newline());
            globalEnv.addNameValue("map",new HardcodedFunction.Map());
            globalEnv.addNameValue("selectMap",new HardcodedFunction.SelectMap());
            globalEnv.addNameValue("select",new HardcodedFunction.Select());
            globalEnv.addNameValue("buildList",new HardcodedFunction.BuildList());
            globalEnv.addNameValue("buildListFromLists",new HardcodedFunction.BuildListFromLists());
            globalEnv.addNameValue("#t",SchemeBoolean.TRUE);
            globalEnv.addNameValue("#f",SchemeBoolean.FALSE);
            globalEnv.addNameValue("PI",new SchemeValue(Math.PI));
        }
        return globalEnv;
    }
    
    public static void resetGlobalEnvironment() {
        globalEnv=null;
    }
    
    private final SchemeEnvironment parentEnv;
    private final Hashtable<String,SchemeElement> hash=new Hashtable<String,SchemeElement>();
    private final boolean treatAsGlobal;
    
    private static SchemeEnvironment globalEnv=null;
}
