/*
 * A command object facilitates communication between the bots,  the switch,
 * and the game server.  A command consists of two parts: Command Type and
 * Metadata.  Each game has its own published command protocol, a list of
 * command types and an ordered sequence of parameters.
 *
 * In the game server, Command objects are frequently packaged in “CommandPacks”
 * (cmdPack object).  A cmdPack consists of a Command as well as optional
 * routing information if the command should be forwarded again by the switch
 * to a bot.  Depending on which constructor is used, the command pack will
 * insert a destination bid & actionTime to the head of a command’s metadata.
 * This information is extracted by the switch, removed, and the core command
 * is then forwarded to the appropriate destination.
 *
 * The actionTime parameter sets the maximum amount of time (in milliseconds)
 * a bot has to respond to the command.  If the bot fails to respond within
 * the allotted time, it is disconnected by the switch and the game server
 * will be notified with a XXX command.  If the actionTime parameter is set
 * to “0” (zero), no response time is enforced or even required.
 * SERVER_MESSAGE commands will always have the actionTime parameter set
 * to zero as a bot response is never required.
 *
 * The Command object class is common for the switch and the game servers.
 * Additional parameters such as IP and port are required by the switch for
 * command forwarding to bots.
 */

package cw_generic;

import java.net.*;


public class Command {
    
    private String cmdType;
    private String metaData;
    private InetAddress targetIP;
    private int targetPort;   

    public String getCommandType (){
        return cmdType;
    }
       
    public void setCommandType (String value){
        cmdType = value;
    }
    
    public InetAddress getIP (){
        return targetIP;
    }

    public void setIP (InetAddress value){
        targetIP = value;
    }    
    
    public int getPort (){
        return targetPort;
    }

    public void setPort (int value){
        targetPort = value;
    }
    
    public String getMetaData (){
        return metaData;
    }

    public void setMetaData (String value){
        metaData = value;
    }
            
    public Command(String type, InetAddress IP, int port, String metaData)
    {
        this.cmdType = type;
        this.targetIP = IP;
        this.targetPort = port;
        this.metaData = metaData;
    }

    public Command(InetAddress IP, int port, String metaData)
    {
        this.targetIP = IP;
        this.targetPort = port;
        this.metaData = metaData;
    }
        
    public Command(String type, String metaData)
    {
        this.cmdType = type;
        this.metaData = metaData;
    }

    public Command(String type)
    {
        this.cmdType = type;
        this.metaData = "";
    }
    
    public Command()
    {
        this.metaData = "";
    }

    // Incomming commands from the switch always have a bid inserted to the head
    // of the metaData.  getBID simply returns this value.  If not, do work.
    public int getBID(){
        try{
            String[] parts = metaData.split(":");
            int bid = Integer.parseInt(parts[0]);
            return bid;
         }catch(Exception e){
            return -1;
        }
    }

}
