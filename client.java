import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 *	SFTP Client v0.1
 *
 *  Abhishek Srivastava
 *  (aas2234@columbia.edu)
 *
 */
public class client implements HandshakeCompletedListener{

	
	private SSLSocketFactory csockFactory;
	private SSLSocket cSocket;
	private SSLContext ctx;
	private KeyManagerFactory kmf;
	private KeyStore ks;
	private BufferedReader sockIn;
	private PrintWriter sockOut;
	private String dataStorePath;
	
	/**
	 * @param args
	 */

	public client(String dataStorePath) {
		if(dataStorePath.endsWith("/"))
			this.dataStorePath = dataStorePath;
		else
			this.dataStorePath = dataStorePath + "/";
		
	}
	
	/**
	 * Create SSLSocketFactory instance for creating SSLSocket instances
	 * @param keyStorePath
	 * @param keyStorePasswd
	 */
	void createSSLSocketFactory(String keyStorePath, String keyStorePasswd){
				
		try {
			ctx = SSLContext.getInstance("TLS");
			
			kmf = KeyManagerFactory.getInstance("SunX509");
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(keyStorePath), keyStorePasswd.toCharArray());
			kmf.init(ks, keyStorePasswd.toCharArray());
			
			ctx.init(kmf.getKeyManagers(), new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Algorithm does not exist.");
			System.exit(1);
				
		} catch (KeyStoreException e) {
			
			System.out.println("KeyStore File does not exist or in incorrect format.");
			System.exit(1);
		} catch (CertificateException e) {
		
			System.out.println("Certificate incorrect or in improper format.");
			System.exit(1);
		} catch (FileNotFoundException e) {
			
			System.out.println("KeyStore File not found.");
			System.exit(1);
		} catch (IOException e) {
			
			System.out.println("I/O Error.");
		} catch (UnrecoverableKeyException e) {
			
			System.out.println("Unrecoverable Key exception.");
		} catch (KeyManagementException e) {
			
			System.out.println("Key Management exception.");
		}
		csockFactory = ctx.getSocketFactory();
		
	}

	/**
	 * Connect to host at given port
	 * @param host
	 * @param port
	 * @return
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	SSLSocket connect(String host, int port) throws IOException, UnknownHostException {
		SSLSocket csock = (SSLSocket)(csockFactory.createSocket(host, port));
		sockIn = new BufferedReader(new InputStreamReader(csock.getInputStream()));
		sockOut = new PrintWriter(csock.getOutputStream(),true);
		return csock;
	}
	
	
	public void handshakeCompleted(HandshakeCompletedEvent arg0) {
		// TODO Auto-generated method stub
		
		System.out.println("Handshake Completed with remote server.");
		cliMode();
	} 
	
	/**
	 * Execute LS command
	 * @param cmd
	 * @throws IOException
	 */
	void execLS(String cmd) throws IOException {
		sockOut.write(cmd + "\r\n");
		sockOut.flush();
		String line;
		do {
			line = sockIn.readLine();
			System.out.println(line);
			System.out.flush();
		}while((line.length()!=0) && (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));
		
		System.out.flush();
	}
	
	/**
	 * Execute LLS command
	 * @param cmd
	 * @throws IOException
	 */
 	void execLLS(String cmd) throws IOException {
			String s;
			Process p = Runtime.getRuntime().exec("ls" + " " + dataStorePath);
            BufferedReader stdInput = new BufferedReader(new 
	                 InputStreamReader(p.getInputStream()));
	        
	        while ((s = stdInput.readLine()) != null) {
	        	System.out.println(s);
	        	System.out.flush();
	        }
	        System.out.flush();    
	}
	
 	/**
 	 * Execute GET command
 	 * @param cmd
 	 * @throws IOException
 	 */
	void execGET(String cmd) throws IOException {
		sockOut.write(cmd + "\r\n");
		sockOut.flush();
		String line;
		line = sockIn.readLine();
		
		// File not found on server
		if(line.startsWith("<FNF>")) {
			System.out.println("File not found on server.");
			return;
		} else {
			String [] tokens = cmd.split(" ");
			File f = new File(dataStorePath + tokens[1]);
			BufferedWriter fout = null;
			
			try {	
				fout = new BufferedWriter(new FileWriter(f));
				int fileLength = Integer.parseInt(line);
				char [] fileData = new char[fileLength];
				
				sockIn.read(fileData, 0, fileLength);
				//write to file	
				fout.write(fileData, 0, fileLength);
				fout.flush();	
			} catch(FileNotFoundException e) {
				System.out.println("File does not exist.");
			} catch (Exception e) {
				System.out.println("Exception occurred while reading in file and writing to socket");
			} finally {
				if(fout != null)
					fout.close();
				System.out.println("Transfer of " + tokens[1] + " complete.");
			}
		}
		
	}
	
	/**
	 * Execute PUT command
	 * @param cmd
	 */
	void execPUT(String cmd) {
		sockOut.write(cmd + "\r\n");
		sockOut.flush();
		
		String [] tokens = cmd.split(" ");
		File f = new File(dataStorePath + tokens[1]);
		
		if(f.exists()) {
			try {
				BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				sockOut.write(f.length() + "\r\n");
				sockOut.flush();
				char [] bdata = new char[512];
				int readBytes = 0;
				while((readBytes = fin.read(bdata)) != -1) {
					sockOut.write(bdata,0,readBytes);
					sockOut.flush();
				}
			} catch (FileNotFoundException e) {
				sockOut.write("<FNF>" + "\r\n");
				sockOut.flush();
			} catch (Exception e) {
				System.out.println("Unknown exception occurred.");
			}
 			
		} else {
			System.out.println("File does not exist in client store.");
		}
		System.out.println("Transfer of " + tokens[1] + " complete.");
	}
	
	/**
	 * CLI mode of client
	 */
	public void cliMode() {
		String prompt = "\n#client->";
		String cmd="";
		BufferedReader bin = new BufferedReader(new InputStreamReader(System.in));
		System.out.flush();
		
		while(cmd.startsWith("exit") != true){
			System.out.print(prompt);
			System.out.flush();
			try {
				cmd = bin.readLine();
				if(cmd.startsWith("ls")) {
					execLS(cmd);
					System.out.flush();
				} else if (cmd.startsWith("lls")) {
					execLLS(cmd);
					System.out.flush();
				} else if (cmd.startsWith("put")) {
					execPUT(cmd);
					System.out.flush();
				} else if (cmd.startsWith("get")) {
					execGET(cmd);	
					System.out.flush();
				} else if(cmd.startsWith("exit")) {
					sockOut.write(cmd + "\r\n");
					sockOut.flush();
					break;
				} else {
					System.out.println("Unrecognized command : " + cmd);
					System.out.flush();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("I/O Exception occurred.");
			} catch(Exception e) {
				System.out.println("Server closed socket or is unavailable.");
			}
			System.out.flush();
		}
		
		System.out.println("Client exiting..");
		try {
			cSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("I/O Exception occurred.");
		} catch(Exception e) {
			System.out.println("Socket Read-Write Error. Exiting ..");
		}
	}
	
	
	private static class DefaultTrustManager implements X509TrustManager {
       @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {}
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//TODO : Remove later and replace by command line args
		
		if(args.length != 4) {
			System.out.println("Incorrect number of arguments.");
			System.out.println("Usage : ./client <Server IP-Address> <Server Port> <Client-Trust-Store> <Client-Data>");
			System.exit(1);
		} else {
			File trustFile = new File(args[2]);
			File dataFile = new File(args[3]);
			if(!trustFile.exists()) {
				System.out.println("TrustStore File Path incorrect or it does not exist. Exiting ..");
				System.exit(2);
			}
			if(!dataFile.exists() || !dataFile.isDirectory()) {
				System.out.println("Path to Data directory incorrect or directory does not exist. Exiting ..");
				System.exit(2);
			}
		}
		
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		System.setProperty("javax.net.ssl.trustStore", args[2]);
		
		String keyStorePath = args[2];
	    String keyStorePassword = "123456";
	    
	    client c = new client(args[3]);
	    
	    c.createSSLSocketFactory(keyStorePath, keyStorePassword);
	    	try {
	    		c.cSocket = c.connect(host, port);

	    	} catch(UnknownHostException e ) {
	    		System.out.println("Unknown host " + host);
	    	} catch(IOException e){
	    		System.out.println("IO Exception occured.");
	    	} 
	    
		try{
		    c.cSocket.addHandshakeCompletedListener(c);
			c.cSocket.startHandshake();
		//csocket.addHandshakeCompletedListener();
	    // Create streams to securely send and receive data to the client
	    	   
		} catch(IOException e ) {
			System.out.println("SSL handshake failed. Failed to verify certificate.");
			System.out.println("IO Exception occurred.");
		} catch (Exception e) {
			System.out.println("Connection to server failed. Incorrect port and/or IP address.");
		}
		
	}

}
