/*
 * The OutputPack object contains a command destined for the web interface.
 * The connectFour game server uses the Thrift transport protocol for updating
 * the web in real time.  All web-bound messages must be retrieved by calls
 * made from the webpage.  An OutputPack object stores a command until it is
 * requested.  The exp parameter determines when a command should expire and
 * be removed via the commandCleaner command.  cString contains the entire
 * command expanded in string form.
 */
package cw_generic;

public class OutputPack {

    private int bid;
    private long exp = 10000 + System.currentTimeMillis();
    private String cString;

    public OutputPack(int bid, String cString) {
        this.bid = bid;
        this.cString = cString;
    }

    public String getCmd() {
        return this.cString;
    }

    public int getBID() {
        return this.bid;
    }

    public long getEXP() {
        return this.exp;
    }
}
