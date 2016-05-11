package texasholdem.server;

import texasholdem.Heartbeat;
import texasholdem.SharedUtilities;
import texasholdem.TexasHoldemConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

class HeartbeatSender extends Thread implements TexasHoldemConstants {
   /**
    * Address to which heartbeats are sent
    */
   private final SocketAddress group;

   /**
    * Socket on which heartbeats are sent
    */
   private final DatagramSocket socket;

   /**
    * true if the heartbeat sender has been canceled
    */
   private volatile boolean cancel;

   /**
    * The server's id
    */
   private final long id;

   /**
    * Constructs server multicast heartbeat sender.
    * @param group The multicast group address
    * @param socket The socket over which to send the heartbeat
    * @param id The server's id
    */
   HeartbeatSender(SocketAddress group, DatagramSocket socket, long id) {
      this.group = group;
      this.socket = socket;
      this.id = id;
      cancel = false;
   }

   /**
    * Periodically sends a heartbeat to the server.
    */
   @Override
   public void run() {
      byte[] hbBytes = new byte[0];
      try {
         hbBytes = SharedUtilities.toByteArray(new Heartbeat(id));
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
      DatagramPacket hbPacket = new DatagramPacket(hbBytes, hbBytes.length, group);;
      while(!cancel) {
         try {
            socket.send(hbPacket);
            Thread.sleep(HEARTBEAT_INTERVAL);
         }
         catch(IOException | InterruptedException ioeie) {
            ioeie.printStackTrace();
         }
      }
   }

   /**
    * Cancels the heartbeat sender
    */
   void cancel() {
      cancel = true;
   }
}