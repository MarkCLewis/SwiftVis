/*
 * Created on Jul 1, 2005
 */
package edu.swri.swiftvis.scheme;

public class SchemeException extends RuntimeException {
    public SchemeException(String message) {
        super(message);
    }
    
    public SchemeException(Exception cause) {
        super(cause);
    }
    
    public SchemeException(String message,Exception cause) {
        super(message,cause);
    }
    
    static final long serialVersionUID=83457987346l;
}
