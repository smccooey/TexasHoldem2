package texasholdem.client;

import texasholdem.Heartbeat;
import texasholdem.SharedUtilities;
import texasholdem.TexasHoldemConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Listens for incoming datagrams and forwards them to the client.
 */
class ClientListener extends Thread implements ClientState, TexasHoldemConstants {

   /**
    * The multicast socket to listen on
    */
   private final MulticastSocket socket;

   /**
    * The associated client
    */
   private final GameClient client;

   /**
    * true if the listener has been canceled
    */
   private volatile boolean cancel;

   /**
    * The server's address
    */
   private final SocketAddress server;

   /**
    * Schedules timeout tasks
    */
   private final ScheduledThreadPoolExecutor scheduler;

   /**
    * Scheduled task to tell client it is no longer in contact with the server
    */
   private ScheduledFuture<?> future;

   /**
    * Constructs a new client listener.
    * @param client The associated client
    * @param socket The multicast socket to listen on
    * @param server The server's address
    */
   ClientListener(GameClient client, MulticastSocket socket, SocketAddress server) {
      this.socket = socket;
      this.client = client;
      cancel = false;
      this.server = server;
      scheduler = new ScheduledThreadPoolExecutor(1);
      schedule();
   }

   /**
    * Listens for incoming datagrams and either responds with a
    * heartbeat or forwards them to the client.
    */
   @Override
   public void run() {
      DatagramPacket packetIn = new DatagramPacket(new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
      while(!cancel) {
         try {
            socket.receive(packetIn);
            Object obj = SharedUtilities.toObject(packetIn.getData());
            if(obj instanceof Heartbeat && client.getState() != JOINING) {
               // Reset timeout
               schedule();
               // Send heartbeat to server
               byte[] hbBytes = SharedUtilities.toByteArray(new Heartbeat(client.getId()));
               socket.send(new DatagramPacket(hbBytes, hbBytes.length, server));
            }
            else {
               // Forward object to client
               client.receiveObject(obj);
            }
         }
         catch(IOException | ClassNotFoundException ioecnfe) {
            ioecnfe.printStackTrace();
            System.exit(1);
         }
      }
   }

   /**
    * Cancels the listener, causing the {@link #run()} method to stop.
    */
   void cancel() {
      cancel = true;
   }

   private void schedule() {
      if(future != null) {
         future.cancel(true);
      }
      future = scheduler.schedule(client::drop, DROP_TIMEOUT, MILLISECONDS);
   }
}