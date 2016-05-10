package texasholdem;

/**
 * Constants used by the game.
 */
public interface TexasHoldemConstants {

   /**
    * Port number
    */
   int PORT = 2714;

   /**
    * Multicast address used by the clients and server
    */
   byte[] MULTICAST_ADDRESS =  { -31, -111, 14, 36 }; // "225.145.14.36"

   /**
    * Server's IP address
    */
   String SERVER_ADDRESS = "pi.oswego.edu";

   /**
    * Maximum size of a packet sent by any node
    */
   int MAX_PACKET_SIZE = 1500;

   /**
    * Interval for sending heartbeats to server
    */
   int HEARTBEAT_INTERVAL = 1000;

   /**
    * Maximum number of players
    */
   int MAX_PLAYERS = 8;

   /**
    * Timeout before the client assumes that the server is no longer connected
    */
   int DROP_TIMEOUT = 5 * HEARTBEAT_INTERVAL;

   /*
    * Game modes
    */

   /**
    * Waiting for first player to join
    */
   int WAITING_MODE = 0;

   /**
    * First player has joined, but game has not started
    */
   int PREGAME_MODE = 1;

   /**
    * Game in progress
    */
   int GAME_MODE = 2;


}