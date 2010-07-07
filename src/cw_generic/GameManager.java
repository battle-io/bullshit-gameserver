/*
 * The GameManager class implements functions specific to the game of Bullshit.
 * Most game server development occurs here and in the game server class.
 */

package cw_generic;


public class GameManager {
    private int gid;
    private int[] botIDs;
    private static int currentgid=1;

    
    public GameManager(int[] botIDs){
        this.botIDs = botIDs;
        this.gid = currentgid;
        currentgid++;

    }

    public int getGID(){
        return gid;
    }

}