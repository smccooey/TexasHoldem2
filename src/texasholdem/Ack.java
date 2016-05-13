package texasholdem;

import java.io.Serializable;

/**
 * Acknowledgement of receipt of the gamestate object.
 */
public class Ack implements Serializable {

   /**
    * The sequence number of the gamestate being acknowledged
    */
   private final int sequenceNumber;

   /**
    * The id of the player sending the ACK
    */
   private final long sender;

   /**
    * Constructs a gamestate ACK for the gamestate with the specified
    * sequence number.
    * @param sequenceNumber The sequence number of the gamestate being
    *        acknowledged
    * @param sender The id of the player sending the ACK
    */
   public Ack(int sequenceNumber, long sender) {
      this.sequenceNumber = sequenceNumber;
      this.sender = sender;
   }

   /**
    * Returns the sequence number of the gamestate being acknowledged.
    * @return The sequence number of the gamestate being acknowledged
    */
   public int getSequenceNumber() {
      return sequenceNumber;
   }

   /**
    * Returns the id of the ACK's sender.
    * @return The id of the ACK's sender
    */
   public long getSender() {
      return sender;
   }

   public String toString() {
      return getClass().getName() + ": seqno=" + sequenceNumber + "; sender=" + sender;
   }
}