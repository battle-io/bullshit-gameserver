/*
 * The SwitchListener handles all the communication between the game server and
 * the switch.  First, a connection to the switch is iniated by opening a
 * socket and transmitting a LOGIN_INFORM command which identifies that the
 * game server is online.  Once authenticated, the game server listens for
 * any incomming commands from the switch.   A dedicated thread is spawned
 * to service the socket connection.  As commands are received, the thread
 * immediately puts them in the commandQueue.  If disconnected, the thread
 * attempts to reconnect periodically.

 * The switch must send periodic “pings” to let the switch know that it is alive.
 * The Ping thread injects ping commands into the commandQueue which are
 * forwarded to the switch using standard means.
 */

package cw_generic;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.log4j.Logger;

public class SwitchListener extends Thread{
    private Logger logger;
    private Socket socket;
    private InetAddress serverIP;
    private int serverPort;       
    private static PrintWriter out;
    private String serverName, serverKey;
    public BlockingQueue<Command> commandQueue = new LinkedBlockingQueue();
    public BlockingQueue<CmdPack> sendQueue = new LinkedBlockingQueue();
    public List<OutputPack> outQueue = Collections.synchronizedList(new ArrayList<OutputPack>());

    public SwitchListener(BlockingQueue<Command> commandQueue, BlockingQueue<CmdPack> sendQueue, Logger logger, String serverIP, String serverName, int serverPort, String serverKey){
        try {
            this.serverPort     = serverPort;
            this.serverIP       = InetAddress.getByName(serverIP);
            this.serverName     = serverName;
            this.serverKey      = serverKey;
            this.commandQueue   = commandQueue;
            this.sendQueue      = sendQueue;
            this.logger         = logger;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket(){
        return this.socket;
    }

    @Override
    public void run() {

        while (true) {
            try {                
                logger.info("Attempting to connect to SWITCH @ " + serverIP + ":" + serverPort);
                this.socket = new Socket(serverIP, serverPort);
                logger.info("Connected to SWITCH!");
                CommandSender cs = new CommandSender(this.socket, this.sendQueue);
                cs.start();
                //new Thread(new CommandSender(this.socket, this.sendQueue), "CommandSender").start();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
                PingThread pt = new PingThread(out);
                pt.start();
                out.println("REGISTER<<"+serverName+":"+serverKey);
                try {
                    while (true) {
                        String cmd = in.readLine();
                        String[] parts = cmd.split("<<");
                        String cmdType = parts[0];
                        String metaData = parts[1];
                        Command command = new Command(cmdType, socket.getInetAddress(), socket.getPort(), metaData);
                        commandQueue.add(command);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        this.socket.close();
                        pt.stopThread();
                        cs.stopThread();
                        cs.interrupt(); //CommandSender sits in Blocking IO.  Interrupt Required.
                        logger.error("Disconnected From SWITCH.  Retrying in 5s...");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private class PingThread extends Thread {

        private PrintWriter out;
        private boolean stop;

        public PingThread(PrintWriter out) {
            this.out = out;
            this.stop = false;
        }

        public void stopThread(){
            this.stop=true;
        }

        @Override
        public void run() {
            currentThread().setName("PingThread");
            while (!stop) {
                try {
                    Thread.sleep(20 * 1000);
                    out.println("SERVER_PING<<null");
                } catch (InterruptedException e) {
                    stop = true;
                }
            }
        }
    }

}
