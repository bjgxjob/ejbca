package se.anatom.ejbca.apply;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.security.cert.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.Provider;
import java.security.Security;
import java.security.KeyStore;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;


import javax.servlet.*;
import javax.servlet.http.*;
import javax.ejb.*;

import javax.rmi.PortableRemoteObject;
import javax.naming.InitialContext;

import se.anatom.ejbca.util.Base64;

import org.apache.log4j.*;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.util.*;
import org.bouncycastle.jce.*;
import org.bouncycastle.jce.netscape.*;
import org.bouncycastle.jce.provider.*;

import se.anatom.ejbca.ca.sign.ISignSessionHome;
import se.anatom.ejbca.ca.sign.ISignSessionRemote;
import se.anatom.ejbca.util.CertTools;
import se.anatom.ejbca.util.KeyTools;
import se.anatom.ejbca.util.FileTools;
import se.anatom.ejbca.ca.exception.AuthStatusException;
import se.anatom.ejbca.ca.exception.AuthLoginException;
import se.anatom.ejbca.ca.exception.SignRequestException;
import se.anatom.ejbca.ca.exception.SignRequestSignatureException;
import se.anatom.ejbca.ra.IUserAdminSessionRemote;
import se.anatom.ejbca.ra.IUserAdminSessionHome;
import se.anatom.ejbca.ra.UserAdminData;
import se.anatom.ejbca.SecConst;

import se.anatom.ejbca.log.Admin;
import se.anatom.ejbca.protocol.PKCS10RequestMessage;

/**
 * Servlet used to install a private key with a corresponding certificate in a
 * browser. A new certificate is installed in the browser in following steps:<br>
 *
 * 1. The key pair is generated by the browser. <br>
 *
 * 2. The public part is sent to the servlet in a post together with user info
 *    ("pkcs10|keygen", "inst", "user", "password"). For internet explorer the public key
 *    is sent as a PKCS10 certificate request. <br>
 *
 * 3. The new certificate is created by calling the RSASignSession session bean. <br>
 *
 * 4. A page containing the new certificate and a script that installs it is returned
 *    to the browser. <br>
 * <p>
 * <p>
 * The following initiation parameters are needed by this servlet: <br>
 *
 * "responseTemplate" file that defines the response to the user (IE). It should have one
 * line with the text "cert =". This line is replaced with the new certificate.
 * "keyStorePass". Password needed to load the key-store. If this parameter is none
 * existing it is assumed that no password is needed. The path could be absolute or
 * relative.<br>
 *
 * @author Original code by Lars Silv?n
 * @version $Id: CertReqServlet.java,v 1.21 2002-11-07 10:31:08 herrvendil Exp $
 */
public class CertReqServlet extends HttpServlet {

    static private Category cat = Category.getInstance( CertReqServlet.class.getName() );

    private InitialContext ctx = null;
    private Admin administrator = null;
    ISignSessionHome home = null;
    IUserAdminSessionHome userdatahome;
    
    private byte bagattributes[] = "Bag Attributes\n".getBytes();  
    private byte friendlyname[] = "    friendlyName: ".getBytes();     
    private byte subject[]  = "subject=/".getBytes();    
    private byte issuer[]  = "issuer=/".getBytes();     
    private byte beginCertificate[] = "-----BEGIN CERTIFICATE-----".getBytes();
    private byte endCertificate[] = "-----END CERTIFICATE-----".getBytes();
    private byte beginPrivateKey[] = "-----BEGIN PRIVATE KEY-----".getBytes();
    private byte endPrivateKey[] = "-----END PRIVATE KEY-----".getBytes();
    private byte NL[] = "\n".getBytes();
    private byte boundrary[] = "outer".getBytes();    

    public void init(ServletConfig config) throws ServletException {
    super.init(config);
        try {
            // Install BouncyCastle provider
            Provider BCJce = new org.bouncycastle.jce.provider.BouncyCastleProvider();
            int result = Security.addProvider(BCJce);

            // Get EJB context and home interfaces
            ctx = new InitialContext();
            home = (ISignSessionHome) PortableRemoteObject.narrow(
                      ctx.lookup("RSASignSession"), ISignSessionHome.class );
            userdatahome = (IUserAdminSessionHome) PortableRemoteObject.narrow(
                             ctx.lookup("UserAdminSession"), IUserAdminSessionHome.class );            
        } catch( Exception e ) {
            throw new ServletException(e);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        Debug debug = new Debug(request,response);
        try {
            String username        = request.getParameter("user");
            String password        = request.getParameter("password");
            String keylengthstring = request.getParameter("keylength");
            int keylength = 1024;
            
            if(keylengthstring != null)
              keylength = Integer.parseInt(keylengthstring);  
            
            administrator = new Admin(Admin.TYPE_PUBLIC_WEB_USER, request.getRemoteAddr());
            cat.debug("Got request for " + username + "/" + password);
            debug.print("<h3>username: "+username+"</h3>");
            
            // Check user
            int tokentype = SecConst.TOKEN_SOFT_BROWSERGEN;
        
            IUserAdminSessionRemote admin = userdatahome.create(administrator);
            UserAdminData data = admin.findUser(username);
            if(data == null)
              throw new ObjectNotFoundException();

                // get users Token Type.
            tokentype = data.getTokenType();                        
            if(tokentype == SecConst.TOKEN_SOFT_P12){
              KeyStore ks = generateToken(username, password, keylength, false);  
              sendP12Token(ks, username, password, response);      
            }
            if(tokentype == SecConst.TOKEN_SOFT_JKS){
              KeyStore ks = generateToken(username, password, keylength, true);  
              sendJKSToken(ks, username, password, response);                 
            }
            if(tokentype == SecConst.TOKEN_SOFT_PEM){
              KeyStore ks = generateToken(username, password, keylength, false);  
              sendPEMTokens(ks, username, password, response);                 
            }            
            if(tokentype == SecConst.TOKEN_SOFT_BROWSERGEN){
            
              // first check if it is a netcsape request,
              if (request.getParameter("keygen") != null) {
                  byte[] reqBytes=request.getParameter("keygen").getBytes();
                  cat.debug("Received NS request:"+new String(reqBytes));
                  if (reqBytes != null) {
                      byte[] certs = nsCertRequest(reqBytes, username, password, debug);
                      sendNewCertToNSClient(certs, response);
                  }
              } else if ( (request.getParameter("pkcs10") != null) || (request.getParameter("PKCS10") != null) ) {
                  // if not netscape, check if it's IE
                  byte[] reqBytes=request.getParameter("pkcs10").getBytes();
                  if (reqBytes == null)
                      reqBytes=request.getParameter("PKCS10").getBytes();
                  cat.debug("Received IE request:"+new String(reqBytes));
                  if (reqBytes != null) {
                      byte[] b64cert=pkcs10CertRequest(
                          reqBytes, username, password, debug);
                      debug.ieCertFix(b64cert);
                      sendNewCertToIEClient(b64cert, response.getOutputStream());
                  }
              } else if (request.getParameter("pkcs10req") != null) {
                  // if not IE, check if it's manual request
                  byte[] reqBytes=request.getParameter("pkcs10req").getBytes();
                  if (reqBytes != null) {
                      byte[] b64cert=pkcs10CertRequest(
                          reqBytes, username, password, debug);
                      sendNewB64Cert(b64cert, response);
                  }
              }
            }  
        } catch (ObjectNotFoundException oe) {
            cat.debug("Non existens username!");
            debug.printMessage("Non existent username!");
            debug.printMessage("To generate a certificate a valid username and password must be entered.");
            debug.printDebugInfo();
            return;
        } catch (AuthStatusException ase) {
            cat.debug("Wrong user status!");
            debug.printMessage("Wrong user status!");
            debug.printMessage("To generate a certificate for a user the user must have status new, failed or inprocess.");
            debug.printDebugInfo();
            return;
        } catch (AuthLoginException ale) {
            cat.debug("Wrong password for user!");
            debug.printMessage("Wrong username or password!");
            debug.printMessage("To generate a certificate a valid username and password must be entered.");
            debug.printDebugInfo();
            return;
        } catch (SignRequestException re) {
            cat.debug("Invalid request!");
            debug.printMessage("Invalid request!");
            debug.printMessage("Please supply a correct request.");
            debug.printDebugInfo();
            return;
        } catch (SignRequestSignatureException se) {
            cat.debug("Invalid signature on certificate request!");
            debug.printMessage("Invalid signature on certificate request!");
            debug.printMessage("Please supply a correctly signed request.");
            debug.printDebugInfo();
            return;
        } catch (java.lang.ArrayIndexOutOfBoundsException ae) {
            cat.debug("Empty or invalid request received.");
            debug.printMessage("Empty or invalid request!");
            debug.printMessage("Please supply a correct request.");
            debug.printDebugInfo();
            return;
        } catch (Exception e) {
            cat.debug(e);
            debug.print("<h3>parameter name and values: </h3>");
            Enumeration paramNames=request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String name=paramNames.nextElement().toString();
                String parameter=request.getParameter(name);
                debug.print("<h4>"+name+":</h4>"+parameter+"<br>");
            }
            debug.takeCareOfException(e);
            debug.printDebugInfo();
        }
    } //doPost

    public void doGet(HttpServletRequest request,  HttpServletResponse response) throws java.io.IOException, ServletException {
        cat.debug(">doGet()");
        Debug debug = new Debug(request,response);
        debug.print("The certificate request servlet only handles POST method.");
        debug.printDebugInfo();
        cat.debug("<doGet()");
    } // doGet

    private void ieCertFormat(byte[] bA, PrintStream out) throws Exception {
        BufferedReader br=new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(bA)) );
        int rowNr=0;
        while ( true ){
            String line=br.readLine();
            if (line==null)
                break;
            if ( line.indexOf("END CERT")<0 ) {
                if ( line.indexOf(" CERT")<0 ) {
                    if ( ++rowNr>1 )
                        out.println(" & _ ");
                    else
                        out.print("    cert = ");
                    out.print('\"'+line+'\"');
                }
            } else
                break;
        }
        out.println();
    }

    private void sendNewCertToIEClient(byte[] b64cert, OutputStream out)
        throws Exception {
        PrintStream ps = new PrintStream(out);
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
                getServletContext().getResourceAsStream(
                    getInitParameter("responseTemplate"))
                    ));
        while ( true ){
            String line=br.readLine();
            if ( line==null )
                break;
            if ( line.indexOf("cert =")<0 )
                ps.println(line);
            else
                ieCertFormat(b64cert, ps);
        }
        ps.close();
        cat.debug("Sent reply to IE client");
        cat.debug(new String(b64cert));
    }

    private void sendNewCertToNSClient(byte[] certs, HttpServletResponse out)
        throws Exception {
        // Set content-type to what NS wants
        out.setContentType("application/x-x509-user-cert");
        out.setContentLength(certs.length);
        // Print the certificate
        out.getOutputStream().write(certs);
        cat.debug("Sent reply to NS client");
        cat.debug(new String(Base64.encode(certs)));
    }
    private void sendNewB64Cert(byte[] b64cert, HttpServletResponse out)
        throws Exception {
        // Set content-type to general file
        out.setContentType("application/octet-stream");
        out.setHeader("Content-disposition", "filename=cert.pem");
        String beg = "-----BEGIN CERTIFICATE-----\n";
        String end = "\n-----END CERTIFICATE-----\n";
        out.setContentLength(b64cert.length+beg.length()+end.length());
        // Write the certificate
        ServletOutputStream os = out.getOutputStream();
        os.write(beg.getBytes());
        os.write(b64cert);
        os.write(end.getBytes());
        out.flushBuffer();
        cat.debug("Sent reply to client");
        cat.debug(new String(b64cert));
    }
    
    private void sendP12Token(KeyStore ks, String username, String kspassword, HttpServletResponse out)
       throws Exception {              
       ByteArrayOutputStream buffer = new ByteArrayOutputStream();       
       ks.store(buffer,kspassword.toCharArray()); 
           
       out.setContentType("application/x-pkcs12");
       out.setHeader("Content-disposition", "filename=" + username + ".p12");
       out.setContentLength(buffer.size());
       buffer.writeTo(out.getOutputStream());
       out.flushBuffer(); 
       buffer.close();
    }
    
    private void sendJKSToken(KeyStore ks, String username, String kspassword,HttpServletResponse out)
       throws Exception {
       ByteArrayOutputStream buffer = new ByteArrayOutputStream();       
       ks.store(buffer,kspassword.toCharArray()); 
           
       out.setContentType("application/octet-stream");
       out.setHeader("Content-disposition", "filename=" + username + ".jks");
       out.setContentLength(buffer.size());
       buffer.writeTo(out.getOutputStream());
       out.flushBuffer(); 
       buffer.close();
    }
    
    private void sendPEMTokens(KeyStore ks, String username, String kspassword,HttpServletResponse out)
       throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();  
        RegularExpression.RE re  = new RegularExpression.RE(",",false);
        String alias = "";
             
        // Find the key private key entry in the keystore
        Enumeration e = ks.aliases();
        Object o = null;
        PrivateKey serverPrivKey = null;
        while (e.hasMoreElements()) {
            o = e.nextElement();
            if(o instanceof String) {
                if ( (ks.isKeyEntry((String) o)) && ((serverPrivKey = (PrivateKey)ks.getKey((String) o, kspassword.toCharArray())) != null) ) {
                    alias = (String) o;  
                    break;
                }
            }
        }

        byte privKeyEncoded[] = "".getBytes();
        if (serverPrivKey != null)
            privKeyEncoded = serverPrivKey.getEncoded();

        //Certificate chain[] = ks.getCertificateChain((String) o);
        Certificate chain[] = KeyTools.getCertChain(ks, (String) o);
        X509Certificate userX509Certificate = (X509Certificate) chain[0];
        
        byte output[] = userX509Certificate.getEncoded();
        String sn = userX509Certificate.getSubjectDN().toString();
        
        String subjectdnpem = re.replace(sn,"/");
        String issuerdnpem = re.replace(userX509Certificate.getIssuerDN().toString(),"/");       
        
        buffer.write(bagattributes);
        buffer.write(friendlyname);
        buffer.write(alias.getBytes());      
        buffer.write(NL);        
        buffer.write(beginPrivateKey);
        buffer.write(NL);
        byte privKey[] = Base64.encode(privKeyEncoded);
        buffer.write(privKey);
        buffer.write(NL);
        buffer.write(endPrivateKey);   
        buffer.write(NL);  
        buffer.write(bagattributes);
        buffer.write(friendlyname);
        buffer.write(alias.getBytes());      
        buffer.write(NL);        
        buffer.write(subject);
        buffer.write(subjectdnpem.getBytes());
        buffer.write(NL);        
        buffer.write(issuer); 
        buffer.write(issuerdnpem.getBytes());
        buffer.write(NL);          
        buffer.write(beginCertificate);
        buffer.write(NL);
        byte userCertB64[] = Base64.encode(output);
        buffer.write(userCertB64);
        buffer.write(NL);
        buffer.write(endCertificate);
        buffer.write(NL);        
 
        if (CertTools.isSelfSigned(userX509Certificate)) {
        } else {
            for(int num = 1;num < chain.length;num++) {
                X509Certificate tmpX509Cert = (X509Certificate) chain[num];
                sn = tmpX509Cert.getSubjectDN().toString();
                String cn = CertTools.getPartFromDN(sn, "CN");
        
                subjectdnpem = re.replace(sn,"/");
                issuerdnpem = re.replace(tmpX509Cert.getIssuerDN().toString(),"/");   
                
                buffer.write(bagattributes);
                buffer.write(friendlyname);
                buffer.write(cn.getBytes());      
                buffer.write(NL);        
                buffer.write(subject);
                buffer.write(subjectdnpem.getBytes());
                buffer.write(NL);        
                buffer.write(issuer); 
                buffer.write(issuerdnpem.getBytes());
                buffer.write(NL);                          
                
                byte tmpOutput[] = tmpX509Cert.getEncoded();
                buffer.write(beginCertificate);
                buffer.write(NL);
                byte tmpCACertB64[] = Base64.encode(tmpOutput);
                buffer.write(tmpCACertB64);
                buffer.write(NL);
                buffer.write(endCertificate);
                buffer.write(NL);
            }
        }
                         
        out.setContentType("application/octet-stream");  
        out.setHeader("Content-disposition", " attachment; filename=" + username + ".pem");        
        buffer.writeTo(out.getOutputStream());
        out.flushBuffer(); 
        buffer.close();           
    }
   

    
    private KeyStore generateToken(String username, String password, int keylength, boolean createJKS)
       throws Exception{
         KeyPair rsaKeys = KeyTools.genKeys(keylength);   
         ISignSessionRemote ss = home.create(administrator);
         X509Certificate cert = (X509Certificate)ss.createCertificate(username, password, rsaKeys.getPublic());

        // Make a certificate chain from the certificate and the CA-certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate[] cachain = ss.getCertificateChain();
        // Verify CA-certificate
        if (CertTools.isSelfSigned((X509Certificate)cachain[cachain.length-1])) {
            try {
                cachain[cachain.length-1].verify(cachain[cachain.length-1].getPublicKey());
            } catch (GeneralSecurityException se) {
                throw new Exception("RootCA certificate does not verify");
            }
        }
        else
            throw new Exception("RootCA certificate not self-signed");
        // Verify that the user-certificate is signed by our CA
        try {
            cert.verify(cachain[0].getPublicKey());
        } catch (GeneralSecurityException se) {
            throw new Exception("Generated certificate does not verify using CA-certificate.");
        }

        // Use CommonName as alias in the keystore
        //String alias = CertTools.getPartFromDN(cert.getSubjectDN().toString(), "CN");
        // Use username as alias in the keystore
        String alias = username;
        // Store keys and certificates in keystore.
        KeyStore ks = null;
        if (createJKS)
            ks = KeyTools.createJKS(alias, rsaKeys.getPrivate(), password, cert, cachain);
        else
            ks = KeyTools.createP12(alias, rsaKeys.getPrivate(), cert, cachain);           
           
        return ks;
    }

    /**
     * Handles NetScape certificate request (KEYGEN), these are constructed as:
     * <pre><code>
     *   SignedPublicKeyAndChallenge ::= SEQUENCE {
     *     publicKeyAndChallenge    PublicKeyAndChallenge,
     *     signatureAlgorithm   AlgorithmIdentifier,
     *     signature        BIT STRING
     *   }
     * </pre>
     *
     * PublicKey's encoded-format has to be RSA X.509.
     * @return byte[] containing DER-encoded certificate.
     */
    private byte[] nsCertRequest(byte[] reqBytes, String username, String password, Debug debug)
        throws Exception {
            byte[] buffer = Base64.decode(reqBytes);
            DERInputStream  in = new DERInputStream(new ByteArrayInputStream(buffer));
            DERConstructedSequence spkac = (DERConstructedSequence)in.readObject();
            NetscapeCertRequest nscr = new NetscapeCertRequest (spkac);
            // Verify POPO, we don't care about the challenge, it's not important.
            nscr.setChallenge("challenge");
            if (nscr.verify("challenge") == false)
                throw new SignRequestSignatureException("Invalid signature in NetscapeCertRequest, popo-verification failed.");
            cat.debug("POPO verification succesful");
            ISignSessionRemote ss = home.create(administrator);
            X509Certificate cert = (X509Certificate) ss.createCertificate(username, password, nscr.getPublicKey());
            //Certificate[] chain = ss.getCertificateChain();

            byte[] pkcs7 = ss.createPKCS7(cert);
            cat.debug("Created certificate (PKCS7) for "+ username);
            debug.print("<h4>Generated certificate:</h4>");
            debug.printInsertLineBreaks(cert.toString().getBytes());
            return pkcs7;
    } //nsCertRequest

    /**
     * Handles PKCS10 certificate request, these are constructed as:
     * <pre><code>
     * CertificationRequest ::= SEQUENCE {
     * certificationRequestInfo  CertificationRequestInfo,
     * signatureAlgorithm          AlgorithmIdentifier{{ SignatureAlgorithms }},
     * signature                       BIT STRING
     * }
     * CertificationRequestInfo ::= SEQUENCE {
     * version             INTEGER { v1(0) } (v1,...),
     * subject             Name,
     * subjectPKInfo   SubjectPublicKeyInfo{{ PKInfoAlgorithms }},
     * attributes          [0] Attributes{{ CRIAttributes }}
     * }
     * SubjectPublicKeyInfo { ALGORITHM : IOSet} ::= SEQUENCE {
     * algorithm           AlgorithmIdentifier {{IOSet}},
     * subjectPublicKey    BIT STRING
     * }
     * </pre>
     *
     * PublicKey's encoded-format has to be RSA X.509.
     */
    private byte[] pkcs10CertRequest(byte[] b64Encoded, String username, String password, Debug debug)
        throws Exception {
        X509Certificate cert;
        byte[] buffer;
        try {
            // A real PKCS10 PEM request
            String beginKey = "-----BEGIN CERTIFICATE REQUEST-----";
            String endKey = "-----END CERTIFICATE REQUEST-----";
            buffer = FileTools.getBytesFromPEM(b64Encoded, beginKey, endKey);
        } catch (IOException e) {
            try {
                // Keytool PKCS10 PEM request
                String beginKey = "-----BEGIN NEW CERTIFICATE REQUEST-----";
                String endKey = "-----END NEW CERTIFICATE REQUEST-----";
                buffer = FileTools.getBytesFromPEM(b64Encoded, beginKey, endKey);
            } catch (IOException ioe) {
                // IE PKCS10 Base64 coded request
                buffer = Base64.decode(b64Encoded);
            }
            /*
            ISignSessionRemote ss = home.create(administrator);
            cert = (X509Certificate) ss.createCertificate(username, password, new PKCS10RequestMessage(buffer));
            */
        }
        ISignSessionRemote ss = home.create(administrator);
        cert = (X509Certificate) ss.createCertificate(username, password, new PKCS10RequestMessage(buffer));
        byte[] pkcs7 = ss.createPKCS7(cert);
        cat.debug("Created certificate (PKCS7) for " + username);
        debug.print("<h4>Generated certificate:</h4>");
        debug.printInsertLineBreaks(cert.toString().getBytes());
        return Base64.encode(pkcs7);
    } //ieCertRequest

    /**
     * Prints debug info back to browser client
     **/
    private class Debug {
        final private ByteArrayOutputStream buffer;
        final private PrintStream printer;
        final private HttpServletRequest request;
        final private HttpServletResponse response;
        Debug(HttpServletRequest request, HttpServletResponse response){
            buffer=new ByteArrayOutputStream();
            printer=new PrintStream(buffer);
            this.request=request;
            this.response=response;
        }

        void printDebugInfo() throws IOException, ServletException {
            request.setAttribute("ErrorMessage",new String(buffer.toByteArray()));
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }

        void print(Object o) {
            printer.println(o);
        }
        void printMessage(String msg) {
            print("<p>"+msg);
        }
        void printInsertLineBreaks( byte[] bA ) throws Exception {
            BufferedReader br=new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bA)) );
            while ( true ){
                String line=br.readLine();
                if (line==null)
                    break;
                print(line.toString()+"<br>");
            }
        }
        void takeCareOfException(Throwable t ) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            print("<h4>Exception:</h4>");
            try {
                printInsertLineBreaks( baos.toByteArray() );
            } catch (Exception e) {
                e.printStackTrace(printer);
            }
            request.setAttribute("Exception", "true");
        }
        void ieCertFix(byte[] bA) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream tmpPrinter=new PrintStream(baos);
            ieCertFormat(bA, tmpPrinter);
            printInsertLineBreaks(baos.toByteArray());
        }
    } // Debug

} // CertReqServlet
