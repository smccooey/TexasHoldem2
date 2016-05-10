package texasholdem;

import java.io.Serializable;

/**
 * Heartbeat object sent to maintain membership in the game
 */
public class Heartbeat implements Serializable {

   /**
    * The sender's id
    */
   private final long sender;

   /**
    * Constructs a heartbeat from a client.
    * @param sender The sender's id
    */
   public Heartbeat(long sender) {
      this.sender = sender;
   }

   /**
    * Returns the sender's id.
    * @return The sender's id
    */
   public long getSender() {
      return sender;
   }
}