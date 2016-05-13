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
   byte[] SERVER_ADDRESS = { -127, 3, 20, 26 };

   /**
    * Maximum size of a packet sent by any node
    */
   int MAX_PACKET_SIZE = 5000;

   /**
    * Interval for sending heartbeats to clients
    */
   int HEARTBEAT_INTERVAL = 500;

   /**
    * Maximum number of players
    */
   int MAX_PLAYERS = 8;

   /**
    * Timeout before the client assumes that the server is no longer connected
    */
   int DROP_TIMEOUT = 5 * HEARTBEAT_INTERVAL;

   /**
    * Timeout for an expected ACK
    */
   int ACK_TIMEOUT = 2 * HEARTBEAT_INTERVAL;

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