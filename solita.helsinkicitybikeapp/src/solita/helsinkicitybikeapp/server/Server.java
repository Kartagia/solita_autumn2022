package solita.helsinkicitybikeapp.server;

import solita.helsinkicitybikeapp.model.Config;

/**
 * The class of the main server listening the user connections and creating the HTML UI
 * for users. 
 * 
 * @author Antti Kautiainen
 *
 */
public class Server extends Thread {

	public static class Servlet extends javax.servlet.http.HttpServlet {
		
	}
	
	private final Config configuration; 
	
	/**
	 * Creates a new server with given configuration. 
	 * @param configuration The configuration of the created server. 
	 */
	public Server(Config configuration) {
		this.configuration = configuration; 
	}
	
	
	
	/**
	 * The main program starting the server. 
	 * @param args The command line arguments. 
	 */
	public static void main(String[] args) {
		Config configuration = new Config();
		Server server = new Server(configuration); 
		server.start(); 
	}



	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
