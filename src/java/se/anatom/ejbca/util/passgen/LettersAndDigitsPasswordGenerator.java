package se.anatom.ejbca.util.passgen;

/**
 * LettersAndDigitsPasswordGenerator is a class generating random passwords containing letters 
 * or digits.
 * 
 * @version $Id: LettersAndDigitsPasswordGenerator.java,v 1.2 2003-12-05 14:49:10 herrvendil Exp $
 */
public class LettersAndDigitsPasswordGenerator extends BasePasswordGenerator{
    
    private static final char[] USEDCHARS = {'1','2','3','4','5','6','7','8','9','0',
    	                                                              'q','Q','w','W','e','E','r','R','t','T',
    	                                                              'y','Y','u','U','i','I','o','O','p','P','a',
    	                                                             'A','s','S','d','D','f','F','g','G','h','H',
    	                                                             'j','J','k','K','l','L','z','Z','x','X','c','C',
    	                                                             'v','V','b','B','n','N','m','M'};
    
    
    public LettersAndDigitsPasswordGenerator(){
    	super(USEDCHARS);
    }
      
}
