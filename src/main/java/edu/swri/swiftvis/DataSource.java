package edu.swri.swiftvis;


/* Generated by Together */

public interface DataSource extends GraphElement {
    void addOutput(DataSink sink);

    void removeOutput(DataSink sink);
    
    int getNumOutputs();
    
    DataSink getOutput(int which);

    /**
     * Returns the number of data elements that this source has in it.  I'm using
     * this instead of an iterator because direct access is much more efficient
     * when trying to make tables of data.
     * @param stream The stream to check.
     * @param The data stream we want the number of elements from.
     * @return The number of data elements in this source.
     */
    int getNumElements(int stream);

    /**
     * Returns the specified data element for this source.  I'm using
     * this instead of an iterator because direct access is much more efficient
     * when trying to make tables of data.
     * @param i Which data element to return.  Should be between 0 and getNumElements()-1.
     * @param stream The stream to check.
     * @param s Which data stream to pull from.
     * @return The selected element.
     * @throws ArrayIndexOutOfBoundsException if the provided index is out of bounds.
     */
    DataElement getElement(int i, int stream);
    
    int getNumStreams();

    int getNumParameters(int stream);

    /**
     * Tells you what a particular parameter is used for.
     * @param stream The stream to check.
     */
    String getParameterDescription(int stream, int which);

    int getNumValues(int stream);

    /**
     * Tells you what a particular value is used for.
     * @param stream The stream to check.
     */
    String getValueDescription(int stream, int which);
    
}
