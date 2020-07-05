/*******************************************************************************
 * Copyright (c) 2013 Imperial College London.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Raul Castro Fernandez - initial design and implementation
 *     Martin Rouaux - Changes to support scale-in of operators
 ******************************************************************************/
package uk.ac.imperial.lsds.seep.infrastructure;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.imperial.lsds.seep.comm.NodeManagerCommunication;
import uk.ac.imperial.lsds.seep.infrastructure.dynamiccodedeployer.ExtendedObjectInputStream;
import uk.ac.imperial.lsds.seep.infrastructure.dynamiccodedeployer.RuntimeClassLoader;
import uk.ac.imperial.lsds.seep.infrastructure.master.Infrastructure;
import uk.ac.imperial.lsds.seep.infrastructure.monitor.comm.serialization.MetricsTuple;
import uk.ac.imperial.lsds.seep.infrastructure.monitor.slave.MonitorSlave;
import uk.ac.imperial.lsds.seep.infrastructure.monitor.slave.MonitorSlaveFactory;
import uk.ac.imperial.lsds.seep.operator.EndPoint;
import uk.ac.imperial.lsds.seep.operator.Operator;
import uk.ac.imperial.lsds.seep.operator.OperatorStaticInformation;
import uk.ac.imperial.lsds.seep.runtimeengine.CoreRE;
import uk.ac.imperial.lsds.seep.state.StateWrapper;
import uk.ac.imperial.lsds.seep.GLOBALS;

/**
 * NodeManager. This is the entity that controls the system info associated to a given node, for instance, the monitor of the node, and the 
 * operators that are within that node.
 */

public class NodeManager{
	
	final private Logger LOG = LoggerFactory.getLogger(NodeManager.class);
	
	private WorkerNodeDescription nodeDescr;
	private RuntimeClassLoader rcl = null;
	
	//Endpoint of the central node
	private int bindPort;
	private InetAddress bindAddr;
	//Bind port of this NodeManager
	private int ownPort;
	private NodeManagerCommunication bcu = new NodeManagerCommunication();
	
	static public boolean monitorOfSink = false;
	static public long clock = 0;
	static public MonitorSlave monitorSlave;
	static public int second;
	static public double throughput;
	//private final String[] interfacePrefs = {"eth", "lo" , "wlan"};	
	private final String[] interfacePrefs;
	private static boolean ignoreIpv6 = true;
	private Thread monitorT = null;
	
	public NodeManager(int bindPort, InetAddress bindAddr, int ownPort) {
		this.bindPort = bindPort;
		this.bindAddr = bindAddr;
       
		this.ownPort = ownPort;

		interfacePrefs = getInterfacePrefs();

		try {
			if (Boolean.parseBoolean(GLOBALS.valueFor("separateControlNet")))
			{
				//nodeDescr = new WorkerNodeDescription(InetAddress.getLocalHost(), ownPort);
				InetAddress controlIp = InetAddress.getLocalHost();

				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
				{
					NetworkInterface intf = en.nextElement();
					LOG.info("Interface " + intf.getName() + " " + intf.getDisplayName());

					for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) 
					{
						InetAddress next = enumIpAddr.nextElement();
						LOG.info("Local IP " + next.toString() );
						if (next.toString().contains("172")) { controlIp = next; }
					}
				}
			
				LOG.info("Choosing separate control net with control ip="+controlIp); 
				nodeDescr = new WorkerNodeDescription(InetAddress.getLocalHost(), controlIp, ownPort);
			}
			else
			{
				int preferredIndex = -1;
				InetAddress preferredIp = null;
				for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
				{
					NetworkInterface intf = en.nextElement();
					LOG.info("Interface " + intf.getName() + " " + intf.getDisplayName());

					int matchedIndex = getMatchingIndex(intf.getName());
					if (preferredIndex < 0 || (matchedIndex >= 0 && matchedIndex < preferredIndex)) 
					{ 
						boolean preferredHasIp = false;
						LOG.info("New preferred interface "+intf.getName()+" with match index "+matchedIndex);
						for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) 
						{
							InetAddress next = enumIpAddr.nextElement();
							if (!ignoreIpv6 || next instanceof Inet4Address)
							{
								LOG.info("New preferred address " + next.toString() );
								preferredIp = next; 
								preferredHasIp = true;
							}
							else
							{
								LOG.info("Ignoring IPv6 address " + next.toString() );
							}
						}

						if (preferredHasIp) { preferredIndex = matchedIndex; }
					}
					else
					{
						LOG.info("Ignoring interface "+intf.getName()+" with match index "+matchedIndex);
					}
				}
				
				LOG.info("Choosing wireless net data/control ip="+preferredIp); 
				nodeDescr = new WorkerNodeDescription(preferredIp, preferredIp, ownPort);
			}
		} 
		catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		}
        
		rcl = new RuntimeClassLoader(new URL[0], this.getClass().getClassLoader());
	}
	
	private int getMatchingIndex(String intf)
	{
		if (interfacePrefs ==  null) { return -1; }
		for (int i = 0; i < interfacePrefs.length; i++)
		{
			if (intf.startsWith(interfacePrefs[i])) { return i; }	
		} 
		return -1;
	}

	private String[] getInterfacePrefs()
	{
		if (GLOBALS.valueFor("interfacePrefs") == null || GLOBALS.valueFor("interfacePrefs").equals(""))
		{	
			if (Boolean.parseBoolean(GLOBALS.valueFor("useCoreAddr")) && Boolean.parseBoolean(GLOBALS.valueFor("piAdHocDeployment")))
			{
				throw new RuntimeException("Logic error - Can't use core and pi at the same time.");
			}
			else if (Boolean.parseBoolean(GLOBALS.valueFor("useCoreAddr")) || !Boolean.parseBoolean(GLOBALS.valueFor("piAdHocDeployment")))
			{
				return new String[]{"eth", "lo" , "wlan"};
			}
			else
			{
				return new String[]{"wlan1", "wlan0", "lo" , "eth"};
			}
		} 
		else
		{

			return GLOBALS.valueFor("interfacePrefs").split(";");
		}
	}

	/// \todo{the client-server model implemented here is crap, must be refactored}
	static public void setSystemStable(){
        MetricsTuple tuple = new MetricsTuple();
        tuple.setOperatorId(Infrastructure.RESET_SYSTEM_STABLE_TIME_OP_ID);
		monitorSlave.pushMetricsTuple(tuple);
	}
	
	public void init() {
		// Get unique identifier for this node
		int nodeId = nodeDescr.getNodeId();
		//Initialize node engine ( CoreRE + ProcessingUnit )
		CoreRE core = new CoreRE(nodeDescr, rcl);
		
		//Local variables
		ServerSocket serverSocket = null;
		PrintWriter out = null;
		ExtendedObjectInputStream ois = null;
		
		Object o = null;
		boolean listen = true;
		
		try{
			serverSocket = new ServerSocket(ownPort);
			LOG.info("Waiting for incoming requests on port: {}", ownPort);
			Socket clientSocket = null;
			//Send bootstrap information
			bcu.sendBootstrapInformation(nodeDescr.getIp(), nodeDescr.getControlIp(), bindPort, bindAddr, ownPort);
			while(listen){
				//Accept incoming connections
				clientSocket = serverSocket.accept();
				//Establish output stream
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				//Establish input stream, which receives serialized objects
				ois = new ExtendedObjectInputStream(clientSocket.getInputStream(), rcl);
				//Read the serialized object sent.
				ObjectStreamClass osc = ois.readClassDescriptor();
				//Lazy load of the required class in case is an operator
				if(!(osc.getName().equals("java.lang.String")) && !(osc.getName().equals("java.lang.Integer"))){
					LOG.debug("-> Received Unknown Class -> {} <- Using custom class loader to resolve it", osc.getName());
					rcl.loadClass(osc.getName());
					o = ois.readObject();
					if(o instanceof Operator){
						LOG.debug("-> OPERATOR resolved, OP-ID: {}", ((Operator)o).getOperatorId());
                    }
					else if (o instanceof StateWrapper){
						LOG.info("-> STATE resolved, Class: {}", o.getClass().getName());
					}
                    
                    out.println("ack");
                    out.flush();
				}
				else{
					o = ois.readObject();
				}
                
				//Check the class of the object received and initialized accordingly
				if(o instanceof ArrayList<?>){
					core.pushStarTopology((ArrayList<EndPoint>)o);
                    
                    out.println("ack");
                    out.flush();
				}
				else if(o instanceof Operator){
                    // Initialize monitor slave, start thread, we do it at
                    // this stage because we need to know the node is running an operator
                    int operatorId = ((Operator)o).getOperatorId();

                    MonitorSlaveFactory factory = new MonitorSlaveFactory(operatorId);
                    monitorSlave = factory.create();

                    monitorT = new Thread(monitorSlave);
                    monitorT.start();

                    LOG.info("-> Node Monitor running for operatorId={}", operatorId);
                    
					core.pushOperator((Operator)o);
                    
                    out.println("ack");
                    out.flush();
                }
				else if(o instanceof Integer){
					core.setOpReady((Integer)o);
                    
                    out.println("ack");
                    out.flush();
				}
				else if(o instanceof String){
					String tokens[] = ((String)o).split(" ");
					
                    LOG.debug("Tokens received: " +tokens[0]);
					if(tokens[0].equals("CODE")){
						LOG.info("-> CODE Command");
						//Send ACK back
						out.println("ack");
						// Establish subconnection to receive the code
						LOG.info("-> Waiting for receiving the CODE...");
						Socket subConnection = serverSocket.accept();
						DataInputStream dis = new DataInputStream(subConnection.getInputStream());
						int codeSize = dis.readInt();
						byte[] serializedFile = new byte[codeSize];
						dis.readFully(serializedFile);
						int bytesRead = serializedFile.length;
						if(bytesRead != codeSize){
							LOG.warn("Mismatch between read and file size");
						}
						else{
							LOG.info("-> CODE received completely");
						}
						//Here I have the serialized bytes of the file, we materialize the real file
						//For now the name of the file is always query.jar
						FileOutputStream fos = new FileOutputStream(new File(ownPort+"query.jar"));
						fos.write(serializedFile);
						fos.close();
						dis.close();
						subConnection.close();
						out.println("ack");
						//At this point we should have the file on disk
						File pathToCode = new File(ownPort+"query.jar");
						if(pathToCode.exists()){
							LOG.info("-> Loading CODE from: {}", pathToCode.getAbsolutePath());
							loadCodeToRuntime(pathToCode);
						}
						else{
							LOG.error("-> No access to the CODE");
						}
					}
					if(tokens[0].equals("STOP")){
						LOG.info("-> STOP Command");
						core.stopDataProcessing();
						
                        // Stop the monitoring slave, this node is stopping
                        if (monitorSlave != null) {
                            monitorSlave.stop();
                        }
                        
                        listen = false;
						
                        LOG.info("Sending ACK message back to the master");
                        out.println("ack");
						out.flush();
                        
                        //since listen=false now, finish the loop
						continue;
					}
					if(tokens[0].equals("SET-RUNTIME")) {
						LOG.info("-> SET-RUNTIME Command");
						core.setRuntime();
						out.println("ack");
					}
					if(tokens[0].equals("START")){
						LOG.info("-> START Command");
                        core.startDataProcessingAsync();
                        
                        //We call the processData method on the source
                        /// \todo {Is START used? is necessary to answer with ack? why is this not using startOperator?}
                        out.println("ack");
					}
					if(tokens[0].equals("CLOCK")){
						LOG.info("-> CLOCK Command");
						NodeManager.clock = System.currentTimeMillis();
						out.println("ack");
					}
				}
               
                o = null;
				ois.close();
				out.close();
				clientSocket.close();
			}
            
            LOG.info("Waiting before stopping manager and terminating this process");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                LOG.error("Unable to wait for 5 seconds");
            }
            
            serverSocket.close();
            System.exit(0);
		}
		//For now send nack, probably this is not the best option...
		catch(IOException io){
			System.out.println("IOException: "+io.getMessage());
			io.printStackTrace();
		}
		catch(IllegalThreadStateException itse){
			System.out.println("IllegalThreadStateException, no problem, monitor thing");
			itse.printStackTrace();
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		catch (SecurityException e) {
			e.printStackTrace();
		} 
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		} 
	}
	
	private void loadCodeToRuntime(File pathToCode){
		URL urlToCode = null;
		try {
			urlToCode = new URL("file://"+pathToCode.getAbsolutePath());
			System.out.println("Loading into class loader: "+urlToCode.toString());
			URL[] urls = new URL[1];
			urls[0] = urlToCode;
			rcl.addURL(urlToCode);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}
