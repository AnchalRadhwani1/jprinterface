package org.cscs.jprinterface.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cscs.jprinterface.queue.QueueManager;

/** Listen for incoming Client connections.
 * Note: ClientSocketServer is bound to a queuemanager. This is passed
 * to ClientRequestHandlers so that they can register there own QueueListeners
 * to notify clients about activity.
 * 
 * @author chris *
 */
public class ClientSocketServer {
	private static final Logger logger = Logger.getLogger(ClientSocketServer.class.getName());
	
	private final int port;
	private final ExecutorService listeningExecutor;
	private final ExecutorService clientExecutor;
	ServerSocket ss = null;
	
	private final QueueManager queueManager;
	public ClientSocketServer(int port, QueueManager queueManager) {
		this(port,queueManager,5);
	}
	
			
	public ClientSocketServer(int port, QueueManager queueManager, int simultanousClients) {
		this.port = port;
		//TODO: give the rejection policy of this queue some thought
		clientExecutor = Executors.newFixedThreadPool(simultanousClients);
		listeningExecutor = Executors.newSingleThreadExecutor();
		this.queueManager = queueManager;
	}

	public void start() {
		try {
			ss = new ServerSocket(port);			
			logger.info(String.format("Listening on %d", port ));

		} catch (IOException io) {
			throw new RuntimeException("failed startup", io);			
		}
		/* submit a runnable that accepts client connections (submitting
		 * further runnable in the process TODO: use two executors 
		 */
		
		listeningExecutor.submit(new Runnable() {
			public void run() {
				try {
					while (true ) {
						
						Socket clientSocket = null;
						try {
						    clientSocket = ss.accept();
							logger.info(String.format("Handling connection from %s", clientSocket.getRemoteSocketAddress()));
							ClientRequestHandler handler = new ClientRequestHandler(clientSocket, queueManager);
							clientExecutor.execute(handler);
						} catch (SocketException se) {
							break;
						} catch (IOException e) {
							logger.log(Level.WARNING, "Error handling connection", e);
						}
					}
				} finally {
					logger.info("Client socket server listening runnable existing...");
				}
			}
		});
		
		
	}

	public void shutdown() {
		try {
			if (ss != null) ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientExecutor.shutdownNow();
		listeningExecutor.shutdownNow();
	}

}
