package texasholdem.server;

import texasholdem.Heartbeat;
import texasholdem.SharedUtilities;
import texasholdem.TexasHoldemConstants;
import texasholdem.gamestate.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ServerListener extends Thread implements TexasHoldemConstants {

   /**
    * The game server
    */
   private final GameServer server;

   /**
    * Maps player ids to their heartbeat timeouts
    */
   private final ConcurrentHashMap<Long, ScheduledFuture<?>> heartbeatTimeouts;

   /**
    * Socket on which incoming packets are received
    */
   private final DatagramSocket socket;

   /**
    * true if the game has been canceled
    */
   private volatile boolean cancel;

   /**
    * Schedules client drop tasks
    */
   private final ScheduledThreadPoolExecutor scheduler;

   /**
    * Constructs a listener thread to receive incoming packets.
    * @param socket The socket used
    */
   ServerListener(GameServer server, DatagramSocket socket) {
      this.server = server;
      this.socket = socket;
      heartbeatTimeouts = new ConcurrentHashMap<>();
      cancel = false;
      scheduler = new ScheduledThreadPoolExecutor(MAX_PLAYERS);
   }

   /**
    * Listens for incoming packets and forwards them to the server.
    */
   @Override
   public void run() {
      DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_SIZE],
            MAX_PACKET_SIZE);
      while(!cancel) {
         // Receive a packet
         Object obj = null;
         try {
            socket.receive(packet);
            obj = SharedUtilities.toObject(packet.getData());
         }
         catch(IOException | ClassNotFoundException ioecnfe) {
            ioecnfe.printStackTrace();
         }
         if(obj == null) {
            continue;
         }
         else if(obj instanceof Heartbeat) {
            Heartbeat hb = (Heartbeat)obj;
            long sender = hb.getSender();
            schedule(sender);
         }
         else {
            if(obj instanceof Player) {
               ((Player)obj).setAddress(packet.getSocketAddress());
            }
            server.receiveObject(obj);
         }
      }
   }

   /**
    * Schedules a task to drop the player. If there is already a scheduled task to drop the player,
    * that task is canceled.
    * @param id The id of the player to be dropped
    */
   private void schedule(long id) {
      ScheduledFuture<?> future = heartbeatTimeouts.get(id);
      if(future != null) {
         future.cancel(true);
      }
      heartbeatTimeouts.put(id, scheduler.schedule(() -> server.dropPlayer(id), DROP_TIMEOUT,
            MILLISECONDS));
   }

   /**
    * Cancels the listener.
    */
   void cancel() {
      scheduler.shutdown();
      cancel = true;
   }

   /**
    * Removes any heartbeat timeout check for the specified player, removing them from the game as
    * far as the listener is concerned.
    * @param id The id of the player to be removed
    */
   void dropPlayer(long id) {
      ScheduledFuture<?> future = heartbeatTimeouts.remove(id);
      if(future != null) {
         future.cancel(true);
      }
   }
}