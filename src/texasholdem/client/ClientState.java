package texasholdem.client;

/**
 * State values for the client
 */
interface ClientState {

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
    * State value when the player has not in a game or attempting to joing one
    */
   int IDLE = 3;
}
