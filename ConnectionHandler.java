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

import javax.net.ssl.SSLSocket;


/**
 *	SFTP Server v0.1
 *
 *  Abhishek Srivastava
 *  (aas2234@columbia.edu)
 *
 */
public class ConnectionHandler implements Runnable {
	
	SSLSocket csock;
	BufferedReader sockIn;
	PrintWriter sockOut;
	String dataStorePath;
	ConnectionHandler(SSLSocket csock, String dataStorePath) {
		this.csock = csock;
		if(dataStorePath.endsWith("/"))
			this.dataStorePath = dataStorePath;
		else 
			this.dataStorePath = dataStorePath + "/";
		
	    try {
			sockIn = new BufferedReader(new InputStreamReader(csock.getInputStream()));
			sockOut = new PrintWriter(csock.getOutputStream(),true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("ConnectionHandler initialized for new incoming connection.");
	}
	
	String readCommand() throws IOException {
		return sockIn.readLine();
	}
	

	/**
	 * Execute LS command
	 * @param cmd
	 */
	void execLS(String cmd) {
		try {

			System.out.println("execLS() : executing command : " + cmd);
			Process p = Runtime.getRuntime().exec(cmd + " " + dataStorePath);
            BufferedReader stdInput = new BufferedReader(new 
	                 InputStreamReader(p.getInputStream()));
	        String line;
	        while ((line = stdInput.readLine()) != null) {
	        	sockOut.write(line + "\r\n");
		        sockOut.flush();
	        	System.out.println(line);
	        	System.out.flush();
	        }
	        sockOut.write("\r\n");
	        sockOut.flush();
	        System.out.flush(); 
	        
		} catch (IOException e) {
			System.out.println("I/O Exception occurred.");
		}
		
	}
	
	/**
	 * Execute PUT command
	 * @param cmd
	 * @throws IOException
	 */
	void execPUT(String cmd) throws IOException {
		//make a new file with new name
		String [] tokens = cmd.split(" ");
		File f = new File(dataStorePath + tokens[1]);
		BufferedWriter fout = null;
		String line;
		line = sockIn.readLine();
		
		try {	
			fout = new BufferedWriter(new FileWriter(f));
			int fileLength = Integer.parseInt(line);
			char [] fileData = new char[fileLength];
			
			sockIn.read(fileData, 0, fileLength);
			//write to file	
			fout.write(fileData, 0, fileLength);
			fout.flush();	
		} catch(FileNotFoundException e) {
			System.out.println("File not found exception.");
		} catch (Exception e) {
			System.out.println("Exception occurred while reading in from socket and writing to file");
		} finally {
			if(fout != null)
				fout.close();
		}
		System.out.println("Transfer of " + tokens[1] + " complete.");
	}
	
	/**
	 * Execute GET command
	 * @param cmd
	 */
	void execGET(String cmd) {
		//check if file exists
		// if yes, send its length. if not, send <FNF>
		// then send its data
		
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
			sockOut.write("<FNF>" + "\r\n");
			sockOut.flush();
		}
		System.out.println("Transfer of " + tokens[1] + " complete.");
	}
	
	
	@Override
	public void run() {
			String cmd = "";
			try {
				while(!cmd.startsWith("exit")) {
					System.out.println("Waiting to accept next command from client.");
					cmd = readCommand();
					System.out.println("Received command " + cmd + " from client.");
					
					if(cmd.startsWith("ls")) {
						// call method execLS()
						System.out.println("execLS() method called to service command.");
						execLS(cmd);
					} else if(cmd.startsWith("get")) {
						// call method execGET
						System.out.println("execGET() method called to service command.");
						execGET(cmd);
					} else if(cmd.startsWith("put")) {
						// call method execPUT
						System.out.println("execPUT() method called to service command.");
						execPUT(cmd);
					} else if(cmd.startsWith("exit")) {
						break;
					}
					else {
						// Unrecognized command !
					}
				}
			} catch (IOException e) {
				System.out.println("I/O Exception occurred.");
			}
			try {
				csock.close();
			} catch (IOException e) {
				System.out.println("I/O Exception occurred.");
			}
		}
	
}
