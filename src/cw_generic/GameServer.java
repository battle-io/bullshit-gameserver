/*
 * The GameServer class is responsible for coordinating responses to incoming
 * commands from the switch and the various game server threads.  The
 * commandQueue data structure stores all incomming commands for processing.
 * There is also a sendQueue for messages to be sent to the switch as well as
 * to individual bots (via the switch).  Commands destined for the browser
 * (via the Thrift interface) are placed in the outQueue construct.  GameServer
 * is also responsible for maintaining a list of all connected bots and ongoing
 * games.  These lists grow and shrink in response to bot logins and
 * disconnections.
 * All incoming commands are processed in order by the CommandProcessor, a
 * subclass to GameServer.  This function parses a command by type and calls
 * the appropriate routine.  It is important to note that the CommandProcessor
 * is a single threaded object.  Only one command can be acted upon
 * simultaneously.  This reduces the risk of concurrency issues.
 * GameServer subroutines are fairly self documenting.  Login functions add new
 * bot instances to the bots array.  Disconnections must be robustly handled
 * to prevent alienating data.  With each CHALLENGE command, new matches are
 * initiated for all idle bots.
 * As mentioned elsewhere, Thrift is used to communicate directly with the web
 * browser.  All thrift functions are indicated in the function comments.
 * Thrift calls are not guaranteed to be thread safe so extra care is
 * required to reduce the risk of concurrency issues.  Additional information
 * on Thrift can be found here: http://incubator.apache.org/thrift/.
 */
package cw_generic;

import java.util.*;
import java.util.concurrent.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class GameServer {

    public final BlockingQueue<Command> commandQueue = new LinkedBlockingQueue();                                    // List of all commands to be processed by the commandProcessor()
    public final BlockingQueue<CmdPack> sendQueue = new LinkedBlockingQueue();                                    // List of commands to be sent to the switch and/or bots        
    private static final Logger logger = Logger.getLogger(GameServer.class);
    static private List<BotManager> bots = Collections.synchronizedList(new ArrayList<BotManager>());    // Actively maintained to include all online bots.
    static private List<GameManager> games = Collections.synchronizedList(new ArrayList<GameManager>());   // All currently active games.
    private long challengeInterval = 60000;  // Sets the frequency of CHALLENGE events in miliseconds

    // Configure Switch Listener (connection & authentication parameters)
    private String serverName   = "cu_bullshit";
    private String serverIP     = "www.code-wars.com";
    private String serverKey    = "13579";
    private int    serverPort   = 3000;

    public static void main(String args[]) throws Exception {
        PropertyConfigurator.configure(args[0]);
        GameServer gs = new GameServer(args);
    }

    public GameServer(String[] args) throws Exception {
        this.challengeInterval = Long.parseLong(args[1]);
        new Thread(new SwitchListener(this.commandQueue, this.sendQueue, logger, serverIP, serverName, serverPort, serverKey), "SwitchListener").start();
        new Thread(new CommandProcessor(), "CommandProcessor").start();
        new Thread(new ChallengeTimer(this.commandQueue, logger, this.challengeInterval), "ChallengeTimer").start();
    }
   

    /*
     * Login a connecting bot.  The LOGIN_INFORM command sent from the switch
     * has two parameters in metaData.  The first parameter is always an integer
     * representing the bid of the connecting bot.  The second parameter is a
     * comma delimited list of parameter/value pairs.  For example:
     *      LOGIN_INFORM<<144:mode=1,language=null
     * The additional parameters originate from the database maintained local
     * to the switch.  Stored procedures retreive this data when a bot logs in.
     */    
    private void login(Command cmd) {
        BotManager b;
        //If incomming bid is valid and does not already exist in the bots list, add new bot.
        if (cmd.getBID()!=-1){
            if ((b = botByBID(cmd.getBID())) == null) {
                //Extract additional parameters and feed into BotManager constructor.
                b = new BotManager(cmd.getBID());
                bots.add(b);
                sendQueue.add(new CmdPack(new Command("SERVER_MESSAGE", "GameServer Confirmed Connection"), b.getBID(), 0));
                logger.info("Bot : " + b.getBID() + " logged in.");
            }else{
                logger.warn("Bot : " + cmd.getBID() + " was already logged in.");
            }
        }else{
            logger.info("Invalid LOGIN_INFORM command received.");
        }
    }

    /*
     * All Bots are required to echo a GAME_INITIALIZE command.  When the echo is
     * received by the game server, the bots status is set to ready indicating
     * that it is prepared to begin the current game.  Once all bots signal that
     * they are ready, cards will be dealt and the game will begin.
     */
    private void acceptChallenge(Command cmd) {
        BotManager b;
        GameManager g;
        //If incomming bid is valid and does not already exist in the bots list, add new bot.
        if (cmd.getBID()!=-1 & (b = botByBID(cmd.getBID())) != null) {
            //Verify that bot is involved in a challenge
            if ((g = gameByGID(b.getGID())) != null) {
                /*
                g.setReady(b.getBID());
                if(g.allBotsReady()){
                    //Deal Cards to all players
                    //Ask First player to make a move
                }else{
                    sendQueue.add(new CmdPack(new Command("SERVER_MESSAGE", "Challenge Accepted!  Waiting for opponents..."), b.getBID()));
                }
                 */
            }else{
                logger.error(b.getBID() + " is not involved in a challenge.");
            }
        }else{
            logger.error("Bot: " + b.getBID() + " does not exist in bots list.");
        }
    }

    /*
     * Process a move made by a bot.  Handle protocol test moves differently from
     * standard moves.  If the game has ended (successfully) log it and send
     * game reports to both bots.
     */
    private void actionReply(Command cmd) {
        BotManager b;
        GameManager g;
        String[] metaData;
        String discardedCards;
        if ((b = botByBID(cmd.getBID())) != null) {
            if ((g = gameByGID(b.getGID())) != null) {
                /*
                metaData = cmd.getMetaData().split(":");
                if (metaData.length == 2) {
                    discardedCards = metaData[1]; // separated by commas
                    //Verify Discarded Cards are Valid & Legal (GameManagerRoutines)
                    //Send TURN_SUMMARY commands to all bots.
                    //If game was a protocol test, terminate game and reset bot.
                }else{
                     // Invalid ACTION_REPLY MetaData
                     logger.error("Bot: " + b.getBID() + " sent an invalid ACTION_REPLY cmd.");
                }
                 */
            }else{
                // Bot is not involved in a game.  Commection should be terminated.
                logger.error("Bot: " + b.getBID() + " sent an ACTION_REPLY while not involved in a game.");
            }
        }else{
            // Bot does not exist in bots list.  Connection should be terminated.
            logger.error("Bot: " + b.getBID() + " does not exist on the server..");
        }
    }

    private void bullshitCalled(Command cmd){
        BotManager b;
        GameManager g;
        String[] metaData;
        if ((b = botByBID(cmd.getBID())) != null) {
            if ((g = gameByGID(b.getGID())) != null) {
                metaData = cmd.getMetaData().split(":");
                if (metaData.length == 2) {
                    //The 1st parameter (not including bid) is the call decision
                    if (metaData[1].equals("bullshit")){
                        //log call in GameManager
                    }else{
                        //log non-call in GameManger
                    }
                    /*
                     * IF all bots have replied {
                     *    IF at least 1 bullshit has been called {
                     *        Broadcast BULLSHIT_REPORT to all bots
                     *        Send DEAL_CARDS message to challenge loser
                     *    }
                     *    Send Action request to next in turn bot. (new round begins)
                     * }
                     */
                }
            }
        }
    }

    /*
     * Periodically schedule games between any connected bots.
     */
    private void challengeEvent() {        
        // For all Idle bots, form groups of opponents and start games
        // Games are initiated by sending all participants a GAME_INITIALIZE command
    }

    /*
     * Simply print any Server message originating from the switch to the console.
     */
    private void switchMessage(Command cmd) {
        logger.warn(cmd.getMetaData());
    }

    /*
     * If the bot is disconnected by the game server, relay the information to
     * switch.  The message will eventually make its way back to the bot &
     * can be used for debugging purposes.
     */
    private void disconnectionByGameServer(BotManager b, String reason) {
        int gid = b.getGID();
        GameManager g;
        if ((g = gameByGID(gid)) != null) {
            sendQueue.add(new CmdPack(new Command("DISCONNECT_BOT_REMOTE", reason), b.getBID()));
        }
        disconnect(b);
    }

    /*
     * If a bot was disconnected by the switch (failed response time tests)
     * the switch will send a disconnect command to the game server.  This
     * function removes an already stale bot from the game server's data.
     */
    private void disconnectionBySwitch(Command cmd) {
        BotManager b;
        if ((b = botByBID(cmd.getBID())) != null) {
            disconnect(b);
        } else {
            logger.warn("Invalid Disconnect command recieved from switch");
        }
    }

    /*
     * Actually Disconnect and remove a bot from the server.  It is important
     * to release any resources bound to that bot (active games, etc).
     */
    private void disconnect(BotManager b) {
        if (b.isBusy()) {
            GameManager g;
            if ((g = gameByGID(b.getGID())) != null) {
                /*
                if (g.getBID1() > 0 || b.getBID() != g.getBID1()) {
                    sendQueue.add(new CmdPack(new Command("GAME_ABORT", g.getGameData()), g.getBID1()));
                }
                if (g.getBID2() > 0 || b.getBID() != g.getBID2()) {
                    sendQueue.add(new CmdPack(new Command("GAME_ABORT", g.getGameData()), g.getBID2()));
                }
                sendQueue.add(new CmdPack(new Command("SERVER_MESSAGE", "Opponent disconnected or made an invalid move!"), g.getOppX()));
                // Set all opponents to IDLE!
                games.remove(g);
                 * */
            }
        }        
        bots.remove(b);
        logger.info("Bot " + b.getBID() + " has been removed from list.");
    }


    /*
     * This function takes a string in csv format and extracts the
     * desired parameter/value pair:
     *    e.g. getParamValue("param1=value1,param2=value2", "param1""
     *         returns "value1"
     */
    private String getParamValue(String name, String params) {
        String val = "";
        String[] parts;
        String[] pair;
        parts = params.split(",");
        for (int i = 0; i < parts.length; i++) {
            pair = parts[i].split("=");
            if (pair[0].equals(name)) {
                return pair[1].toString();
            }
        }
        return null;
    }

    private BotManager botByBID(int bid) {
        for (BotManager b : bots) {
            if (b.getBID() == bid) {
                return b;
            }
        }
        return null;
    }

    private BotManager botByGID(int gid) {
        for (BotManager b : bots) {
            if (b.getGID() == gid) {
                return b;
            }
        }
        return null;
    }

    private GameManager gameByGID(int gid) {
        for (GameManager g : games) {
            if (g.getGID() == gid) {
                return g;
            }
        }
        return null;
    }

    public class CommandProcessor extends Thread {

        private Command cmd;

        @Override
        public void run() {
            while (true) {
                try {
                    cmd = commandQueue.take();
                    if (cmd.getCommandType().equals("LOGIN_INFORM")) {
                        login(cmd);
                    } else if (cmd.getCommandType().equals("GAME_INITIALIZE")) {
                        acceptChallenge(cmd);
                    } else if (cmd.getCommandType().equals("ACTION_REPLY")) {
                        actionReply(cmd);
                    } else if (cmd.getCommandType().equals("DISCONNECT_BOT_REMOTE")) {
                        disconnectionBySwitch(cmd);
                    } else if (cmd.getCommandType().equals("CHALLENGE")) {
                        challengeEvent();
                    } else if (cmd.getCommandType().equals("CALL_BULLSHIT")) {
                        bullshitCalled(cmd);
                    } else if (cmd.getCommandType().equals("SERVER_MESSAGE")) {
                        switchMessage(cmd);
                    } else {
                        logger.warn("Unrecognized Command Forwarded From Server: " + cmd.getCommandType());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}