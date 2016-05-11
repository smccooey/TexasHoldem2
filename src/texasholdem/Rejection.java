package texasholdem;

import java.io.Serializable;

/**
 * Notice to a player that their attempt to join the game has been rejected
 */
public class Rejection implements Serializable {

   /**
    * The player's id
    */
   private final long id;

   /**
    * Message from the server
    */
   private final String message;

   /**
    * Constructs a rejection notice for the specified player with the specified
    * message.
    * @param id The player's id
    * @param message The message from the server
    */
   public Rejection(long id, String message) {
      this.id = id;
      this.message = message;
   }

   /**
    * Returns the id of the recipient of this notice.
    * @return The recipient's id
    */
   public long getId() {
      return id;
   }

   /**
    * Returns the message for this notice.
    * @return The message
    */
   public String getMessage() {
      return message;
   }
}
