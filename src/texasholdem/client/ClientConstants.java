package texasholdem.client;

import texasholdem.TexasHoldemConstants;

/**
 * Constants used by the client
 */
interface ClientConstants extends TexasHoldemConstants {

   /**
    * State value when the player is attempting to join a game
    */
   int JOINING = 0;

   /**
    * State value when the player has joined a game and is waiting for it to
    * begin
    */
   int JOINED = 1;

   /**
    * State value when the player is in a game
    */
   int IN_GAME = 2;

   /**
    * State value when the player is not in a game or attempting to join one
    */
   int IDLE = 3;
}