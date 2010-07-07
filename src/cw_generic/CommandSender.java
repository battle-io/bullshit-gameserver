/*
 * The CommandSender class continually consumes the “sendQueue” and forwards
 * messages to the switch.  All outgoing commands are first sent to the switch
 * regardless of final destination.  For all messages with a bot as the final
 * destination, a bid and "action time" parameter is inserted at the front of
 * the metadata. The switch will remove these parameter before forwarding the
 * message to the appropriate bot.  Several commands are reserved for game
 * server to switch communication and will never be forwarded.  The sendQueue
 * contains valid commands.  It is important to remember that commands sent to
 * the switch will not match the online bot documentation because of the
 * addition of the bid/routing parameter in the metadata.
 */

package cw_generic;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class CommandSender extends Thread {
        private PrintWriter out;
        private CmdPack cmdPack;
        private String cmdOut;
        private Command cmd;
        private Socket socket;
        public BlockingQueue<CmdPack> sendQueue = new LinkedBlockingQueue();
        private boolean stop;

        public CommandSender(Socket socket, BlockingQueue<CmdPack> sendQueue){
            this.socket = socket;
            this.sendQueue = sendQueue;
            this.stop = false;
        }

        public void stopThread(){
            this.stop=true;
        }

        @Override
        public void run() {
            currentThread().setName("CommandSender");
            while (!stop) {
                try {
                    cmdPack = sendQueue.take();   //Blocks until commandPack exists
                    if(cmdPack.getBID()>=0){
                        cmd = cmdPack.cmd;
                        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")), true);
                        cmdOut = cmd.getCommandType().toString()+"<<"+cmd.getMetaData();
                        out.println(cmdOut);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
