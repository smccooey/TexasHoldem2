package texasholdem.server;

import texasholdem.Heartbeat;
import texasholdem.SharedUtilities;
import texasholdem.TexasHoldemConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class HeartbeatSender extends Thread implements TexasHoldemConstants {
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
    * Constructs server multicast heartbeat sender.
    * @param group The multicast group address
    * @param socket The socket over which to send the heartbeat
    */
   HeartbeatSender(SocketAddress group, DatagramSocket socket) {
      this.group = group;
      this.socket = socket;
      cancel = false;
   }

   /**
    * Periodically sends a heartbeat to the server.
    */
   @Override
   public void run() {
      DatagramPacket hbPacket = null;
      try {
         byte[] hbBytes = SharedUtilities.toByteArray(new Heartbeat());
         hbPacket = new DatagramPacket(hbBytes, hbBytes.length, group);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
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