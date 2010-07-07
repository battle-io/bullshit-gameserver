/*
 * The BotManager class is responsible for maintaining bot  state on the game
 * server. Each bot is identified by a unique bot id (bid).  This ID is assigned
 * to a bot during creation and persists on the switch.  bid is always the first
 * parameter in the LOGIN_INFORM command.  All other fields are challenge
 * specific and are obtained from the switch or from a local database.
 */

package cw_generic;

public class BotManager  {            
    private int bid;             // Bot Id (sent from switch)
    private boolean busy;        // True when bot is involved in a game. False otherwise.
    private int gid;             // game id (when currently involved in a game)
    private int mode;            // Integer flag representing mode: debug=0, Live/Competition=1.  Currently broken in c4.    
    
    //Constructor
    public BotManager(int bid){
        this.bid = bid;        
    }
    public int getBID(){
        return this.bid;
    }
    public int getGID(){
        return this.gid;
    }
    public void setGID(int in){
        this.gid = in;
    }
    public int getMode(){
        return this.mode;
    }
    public void setMode(int mode){
        this.mode=mode; // 1 live 0 Debug
    }
    public void setBusy(boolean in){
        this.busy=in;
    }
    public boolean isBusy(){
        return this.busy;
    }       
 }
