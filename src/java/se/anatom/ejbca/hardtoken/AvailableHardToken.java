/*
 * AvailableHardToken.java
 *
 * Created on den 19 januari 2003, 13:04
 */

package se.anatom.ejbca.hardtoken;

/**
 *  Class representing a to the system available hard token type used by hard token profiles, defined in ejb-jar.xml
 *
 * @author  TomSelleck
 */
public class AvailableHardToken implements java.io.Serializable {
    
    // Public Constructors
    public AvailableHardToken(String name, String classpath){
      this.name=name;
      this.classpath=classpath;
    }
    
    public String getName(){
      return this.name;         
    }
    public String getClassPath(){
      return this.classpath;         
    }    
                  
    // Private fields
    private    String          name;   
    private    String          classpath;
}
