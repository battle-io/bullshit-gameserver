/**
 * The ChallengeTimer class periodically injects a “CHALLENGE” command into the
 * command queue.  challengeInterval controls the width of this interval.
 */
package cw_generic;

import java.util.concurrent.*;
import org.apache.log4j.Logger;

public class ChallengeTimer extends Thread {
    private Logger logger;
    public BlockingQueue<Command> commandQueue = new LinkedBlockingQueue();
    private long challengeInterval;

    public ChallengeTimer(BlockingQueue<Command> commandQueue, Logger logger, long challengeInterval) {
        this.logger = logger;
        this.commandQueue = commandQueue;
        this.challengeInterval = challengeInterval;        
    }

    @Override
    public void run() {
        logger.info("Challenge Timer Started");
        int x=0;
        try {
            this.setName("ChallengeInterval");
            Command cmd=new Command("CHALLENGE", "null");
            while(true){
                Thread.sleep(challengeInterval);             
                commandQueue.add(cmd);
            }
        } catch (InterruptedException e) {
            logger.fatal("ChallengeInterval Interrupted");
            return;
        }
    }
}

