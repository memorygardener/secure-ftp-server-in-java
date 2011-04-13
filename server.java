import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;


/**
 *	SFTP Server v0.1
 *
 *  Abhishek Srivastava
 *  (aas2234@columbia.edu)
 *
 */
public class server {
	
	/**
	 * @param args
	 */
	private SSLServerSocket servSock;
	private SSLServerSocketFactory servSockFactory;
	private SSLContext ctx;
	private KeyManagerFactory keyManagerFactory;
	private KeyStore keyStore;
	
	/**
	 * Create SSLServerSocketFactory for SSLServerSockets
	 * @param keyStorePath
	 * @param keyStorePasswd
	 */
	void createSSLSocketFactory(String keyStorePath, String keyStorePasswd) {
		    
	    try {
	    	
		    keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		    keyStore = KeyStore.getInstance("JKS");
		    keyStore.load(new FileInputStream(keyStorePath), keyStorePasswd.toCharArray());
		    keyManagerFactory.init(keyStore, keyStorePasswd.toCharArray());

		    ctx = SSLContext.getInstance("TLS");
		    ctx.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

		    servSockFactory = ctx.getServerSocketFactory();
		    
	    } catch(IOException e) {
	    	System.out.println("I/O Exception occurred.");
	    	System.exit(1);
	    } catch (NoSuchAlgorithmException e) {
			
	    	System.out.println("Algorithm does not exist.");
	    	System.exit(1);
		} catch (KeyStoreException e) {
			
			System.out.println("KeyStore File does not exist.");
			System.exit(1);
		} catch (CertificateException e) {
			
			System.out.println("Certificate not present or in incorrect format.");
			System.exit(1);
		} catch (UnrecoverableKeyException e) {
			
			System.out.println("Unrecoverable Key Exception.");
			System.exit(1);
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			System.out.println("Key Management Exception.");
			System.exit(1);
		} 
	}
	
	/**
	 * Bind and begin listening to a particular port
	 * @param port
	 * @throws IOException
	 */
	void bindAndListen(int port) throws IOException {
		servSock = (SSLServerSocket)servSockFactory.createServerSocket(port);
	    //mutual 2-way authentication
	    servSock.setNeedClientAuth(true);
	}
	
	/**
	 * Accept a new connection
	 * @return
	 * @throws IOException
	 */
	SSLSocket acceptConnection() throws IOException {
		return (SSLSocket) servSock.accept();
	}
	
	public static void main(String[] args) {
		
		// in args, check if keystore file exists
		if(args.length != 3) {
			System.out.println("Incorrect number of arguments.");
			System.out.println("Usage : ./server <Server Port> <Server-Trust-Store> <Server-Data>");
			System.exit(1);
		} else {
			File trustFile = new File(args[1]);
			File dataFile = new File(args[2]);
			if(!trustFile.exists()) {
				System.out.println("TrustStore File Path incorrect or it does not exist. Exiting ..");
				System.out.println("Usage : ./server <Server Port> <Server-Trust-Store> <Server-Data>");
				System.exit(2);
			}
			if(!dataFile.exists() || !dataFile.isDirectory()) {
				System.out.println("Path to Data directory incorrect or directory does not exist. Exiting ..");
				System.out.println("Usage : ./server <Server Port> <Server-Trust-Store> <Server-Data>");
				System.exit(2);
			}
		}
		
		System.setProperty("javax.net.ssl.trustStore", args[1]);
		
		int port = Integer.parseInt(args[0]);
		String dataStorePath = args[2];
		System.out.println("SFTP Server v0.1 ready to receive connections");
		
		server serv = new server();
		serv.createSSLSocketFactory(args[1], "123456");
		try {
			serv.bindAndListen(port);
			while(true) {
				SSLSocket csock = serv.acceptConnection();
				System.out.println("Starting new thread for incoming connection ..");
				(new Thread(new ConnectionHandler(csock,dataStorePath))).start();
			}
			
		} catch(IOException e) {
			System.out.println("SSL handshake failed. Failed to verify certificate.");
		}
	}	

}
