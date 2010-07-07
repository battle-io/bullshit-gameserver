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
 */
package cw_generic;

 public class CmdPack{
        public Command cmd;
        private int bid;        
        private String cString;

        // Used to send command to the switch (no additional forwarding required)
        public CmdPack(Command cmd){
            this.cmd = cmd;
        }

        // Send command to a bot without a time requirement (actionTime=0)
        public CmdPack(Command cmd, int bid){
            this.cmd = cmd;
            this.bid = bid;            
            this.cString = cmd.getCommandType()+"<<"+cmd.getMetaData();
            if(bid>0){
                this.cmd.setMetaData(bid+":0:"+cmd.getMetaData());
            }
        }

        // Send cmd to bot and require a response within actionTime (milliseconds)
        public CmdPack(Command cmd, int bid, long actionTime){
            this.cmd = cmd;
            this.bid = bid;            
            this.cString = cmd.getCommandType()+"<<"+cmd.getMetaData();
            if(bid>0){
                this.cmd.setMetaData(bid+":"+actionTime+":"+cmd.getMetaData());
            }
        }
        
        public int getBID(){
            return bid;
        }

        public OutputPack getOutPack(){
            return new OutputPack(this.bid, this.cString);
        }

    }
