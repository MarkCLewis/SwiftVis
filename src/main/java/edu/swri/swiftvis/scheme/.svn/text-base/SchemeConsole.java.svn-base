/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

import java.util.Scanner;

/**
 * This console is intended primarily for the testing of the scheme implementation.  It
 * takes user input just like an scm environment and provides output.
 * 
 * @author Mark Lewis
 */
public class SchemeConsole {
    public static void main(String[] args) {
        SchemeEnvironment env=SchemeEnvironment.defaultGlobalEnvironment();
        String[][] tests={
                {"5","5"},
                {"(define n 2)","2"},
                {"n","2"},
                {"(define m (+ 4 5))","9"},
                {"(+ 5 6)","11"},
                {"(* 5 6)","30"},
                {"(- 7 2)","5"},
                {"(/ 9 3)","3"},
                {"(+ (* 4 2) (- 6 3))","11"},
                {"(+ 5 6 9)","20"},
                {"(< 2 3)","true"},
                {"(< 3 2)","false"},
                {"(> 2 3)","false"},
                {"(> 3 2)","true"},
                {"(< 2 2)","false"},
                {"(<= 2 3)","true"},
                {"(<= 3 3)","true"},
                {"(>= 2 3)","false"},
                {"(= 3 2)","false"},
                {"(= 3 3)","true"},
                {"#t","true"},
                {"#f","false"},
                {"(not #t)","false"},
                {"(and #t #t)","true"},
                {"(and #f #t)","false"},
                {"(and #f #f)","false"},
                {"(and #t #t #f)","false"},
                {"(and #t #t #t)","true"},
                {"(or #t #t)","true"},
                {"(or #f #f)","false"},
                {"(or #f #t)","true"},
                {"(or #f #t #f)","true"},
                {"(or #f #f #f)","false"},
                {"'(4 5 6)","(4 5 6)"},
                {"(quote (4 5 6))","(4 5 6)"},
                {"(list 4 5 6)","(4 5 6)"},
                {"(list 'a 'b 'c)","(a b c)"},
                {"(car '(4 5 6))","4"},
                {"(cdr '(4 5 6))","(5 6)"},
                {"(cons 4 '(5 6))","(4 5 6)"},
                {"(cdr (cdr '(4 5 6)))","(6)"},
                {"(cdr (cdr (cdr '(4 5 6))))","()"},
                {"(length '(4 5 6))","3"},
                {"(null? '(4 5 6))","false"},
                {"(null? ())","true"},
                {"(null? (cdr '(4)))","true"},
                {"(null? (cdr (cons 4 ())))","true"},
                {"(begin (+ 2 3) (+ 4 5))","9"},
                {"(define (len lst) (if (null? lst) 0 (+ 1 (len (cdr lst)))))",""},
                {"(len '(4))","1"},
                {"(len '(4 5 6))","3"},
                {"(define fact (lambda (n) (if (< n 2) 1 (* n (fact (- n 1))))))",""},
                {"(fact 5)","120"},
                {"(define (fib n) (if (< n 3) 1 (+ (fib (- n 1)) (fib (- n 2)))))",""},
                {"(fib 5)","5"},
                {"(map (lambda (x) (+ x 1)) '(4 5 6))","(5 6 7)"},
                {"(selectMap (lambda (x) (< x 6)) (lambda (x) (+ x 1)) '(4 5 6))","(5 6)"},
                {"(map (lambda (lst) (car lst)) '((4 5) (6 7) (8 9)))","(4 6 8)"},
                {"(selectMap (lambda (x) (> (car x) 5)) (lambda (lst) (car lst)) '((4 5) (6 7) (8 9)))","(6 8)"},
                {"(define (inc n) (+ n 1))",""},
                {"(map inc '(4 5 6))","(5 6 7)"},
                {"(let ((a 4) (b 5)) (* a b))","20"},
                {"(define cplus (lambda (x) (lambda (y) (+ x y))))",""},
                {"((cplus 6) 7)","13"},
                {"(define plus3 (cplus 3))",""},
                {"(plus3 7)","10"}
        };
        int failCount=0;
        long start=System.currentTimeMillis();
        for(String[] pair:tests) {
            try {
                SchemeElement eval=parse(pair[0],env).eval(env);
                if(pair[1].length()>0 && !eval.toString().equals(pair[1])) {
                    System.err.println("Test failed: "+pair[0]+"\n  Expected: "+pair[1]+" got "+eval);
                    failCount++;
                }
            } catch(Exception e) {
                System.err.println("Test error: "+pair[0]);
                e.printStackTrace();
                failCount++;                
            }
        }
        System.out.println("Testing took "+(System.currentTimeMillis()-start)+" millis.");
        System.out.println("Failed "+failCount+" of "+tests.length+" tests.");
        SchemeEnvironment.resetGlobalEnvironment();
        env=SchemeEnvironment.defaultGlobalEnvironment();
        Scanner in=new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            int parensCnt=0;
            String command="";
            while(command.length()<1 || parensCnt!=0) {
                String line=in.nextLine();
                command+=line;
                for(int i=0; i<line.length(); ++i)  {
                    if(line.charAt(i)=='(') {
                        parensCnt++;
                    } else if(line.charAt(i)==')') {
                        parensCnt--;
                    }
                }
            }
            if(command.equals("(quit)")) return;
            if(command.equals("(defined)")) System.out.println(env.knownNames());
            else try {
                System.out.println(parse(command,env).eval(env));
            } catch(SchemeException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void parseAndExecuteMany(String exp,SchemeEnvironment env) {
        int pind=exp.indexOf('(');
        while(pind>=0) {
            int i,parensCnt=1;
            for(i=pind+1; parensCnt>0; ++i) {
                if(exp.charAt(i)=='(') {
                    parensCnt++;
                } else if(exp.charAt(i)==')') {
                    parensCnt--;
                }
            }
            parse(exp.substring(pind,i),env).eval(env);
            pind=exp.indexOf('(',i+1);
        }
    }
    
    public static SchemeElement parse(String exp,SchemeEnvironment env) {
        exp=exp.trim();
        if(exp.startsWith("'")) {
            return parse("(quote "+exp.substring(1)+")",env);
        } else if(exp.startsWith("(")) {
            return parseList(exp.substring(1,exp.length()-1),env);
        } else {
            return parseElement(exp,env);
        }
    }
    
    public static SchemeElement parseList(String lst,SchemeEnvironment env) {
        lst=lst.trim();
        if(lst.length()<1) return ConsCell.nullInstance();
        int i;
        if(lst.startsWith("(")) {
            int parensCnt=1;
            for(i=1; parensCnt>0; ++i) {
                if(lst.charAt(i)=='(') {
                    parensCnt++;
                } else if(lst.charAt(i)==')') {
                    parensCnt--;
                }
            }
        } else if(lst.startsWith("'(")) {
            int parensCnt=1;
            for(i=2; parensCnt>0; ++i) {
                if(lst.charAt(i)=='(') {
                    parensCnt++;
                } else if(lst.charAt(i)==')') {
                    parensCnt--;
                }
            }
        } else {
            for(i=0; i<lst.length() && !Character.isWhitespace(lst.charAt(i)); ++i);
        }
        return new ConsCell(parse(lst.substring(0,i),env),parseList(lst.substring(i),env));
    }
    
    public static SchemeElement parseElement(String el,SchemeEnvironment env) {
        try {
            return new SchemeValue(Double.parseDouble(el));
        } catch(NumberFormatException e) {
            return new SchemeName(el);
        }
    }
}
