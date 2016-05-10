package texasholdem.server;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import texasholdem.Ack;
import texasholdem.Rejection;
import texasholdem.SharedUtilities;
import texasholdem.TexasHoldemConstants;
import texasholdem.gamestate.GameState;
import texasholdem.gamestate.Player;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class GameServer implements TexasHoldemConstants {

   /**
    * Socket for all communication with clients
    */
   private MulticastSocket socket;

   /**
    * Thread to listen for incoming packets
    */
   private ServerListener listener;

   /**
    * Scheduled timeouts for client acks for gamestate multicasts
    */
   private final ConcurrentHashMap<Long, ScheduledFuture<?>> clientAckTimeouts;

   /**
    * Timeout scheduler
    */
   private final ScheduledThreadPoolExecutor scheduler;

   /**
    * Periodically multicasts heartbeats to clients
    */
   private HeartbeatSender hbSender;

   /**
    * Group address
    */
   private final SocketAddress group;

    /**
    * true if the game has been canceled
    */
   private volatile boolean cancel;

   /**
    * Queue of objects which have been received but not yet handled
    */
   private Queue<Object> received;

   /**
    * The server's id
    */
   private final long id;

   /**
    * The current game state
    */
   private volatile GameState gameState;

   /**
    * Main method.
    * @param args Command-line arguments
    */
   public static void main(String[] args) {
      new GameServer();
   }

   /**
    * Constructs a new game server.
    */
   private GameServer() {
      cancel = false;
      InetAddress groupAddress = null;
      try {
         groupAddress = InetAddress.getByAddress(MULTICAST_ADDRESS);
      }
      catch(UnknownHostException uhe) {
         uhe.printStackTrace();
         System.exit(1);
      }
      group = new InetSocketAddress(groupAddress, PORT);

      long tempId = 0;
      try {
         socket = new MulticastSocket(PORT);
         tempId = SharedUtilities.bytesToLong(socket.getNetworkInterface().getHardwareAddress());
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
         System.exit(1);
      }
      id = tempId;

      gameState = new GameState();

      scheduler = new ScheduledThreadPoolExecutor(MAX_PLAYERS);

      clientAckTimeouts = new ConcurrentHashMap<>();
      received = new ConcurrentLinkedQueue<>();
      listener = new ServerListener(this, socket);
      listener.start();
      hbSender = new HeartbeatSender(group, socket, id);
      hbSender.start();

      // Do stuff();
      doStuff();
   }


   /**
    * Does stuff as long as there is stuff to be done.
    */
   private void doStuff() {
      while(true) {
         if(!received.isEmpty()) {
            Object obj = received.poll();
            if(obj != null) {
               if(obj instanceof Player) {
                  Player newPlayer = (Player)obj;
                  if(gameState.getMode() == WAITING_MODE) {
                     // This is the first player. Start new game
                     gameState.setMode(PREGAME_MODE);
                     multicastGameState();
                  }
                  else if(gameState.getMode() == PREGAME_MODE) {
                     // Add player to game
                     if(gameState.addPlayer(newPlayer)) {
                        if(gameState.isFull()) {
                           gameState.setMode(GAME_MODE);
                        }
                        multicastGameState();
                     }
                     else {
                        // Unicast a rejection
                        Rejection rejection = new Rejection(newPlayer.getId(), "Game is full.");
                        byte[] rejectionBytes = null;
                        try {
                           rejectionBytes = SharedUtilities.toByteArray(rejection);
                        }
                        catch(IOException ioe) {
                           ioe.printStackTrace();
                        }
                        DatagramPacket packetOut =
                              new DatagramPacket(rejectionBytes, rejectionBytes.length,
                                    newPlayer.getAddress());
                        try {
                           socket.send(packetOut);
                        }
                        catch(IOException ioe) {
                           ioe.printStackTrace();
                        }
                     }
                  }
                  else {
                     // Game already underway; reject player
                  }

               }
               else if(obj instanceof GameState) {
                  GameState newGameState = (GameState)obj;
                  if(newGameState.getSender() == gameState.getCurrentPlayer().getId()) {
                     // Gamestate received from current player
                  }
               }
               else if(obj instanceof Ack) {
                  Ack ack = (Ack)obj;
                  if(ack.getSequenceNumber() == gameState.getSequenceNumber()) {
                     // This is an ack for the current gamestate
                     ScheduledFuture<?> future = clientAckTimeouts.remove(ack.getSender());
                     if(future != null) {
                        future.cancel(false);
                     }
                  }
               }
               else {
                  throw new RuntimeException("Unexpected " + obj.getClass().getName() +
                        "received.");
               }
            }
         }
         // Take a nap
         synchronized(this) {
            try {
               wait();
            }
            catch(InterruptedException ie) {
               // Do nothing
            }
         }
      }
   }

   /**
    * Drops the specified player from the game.
    * @param id The id of the player to be dropped
    */
   void dropPlayer(long id) {

   }

   /**
    * Invoked when the listener receives an object (other than a heartbeat)
    * from a client.
    * @param obj The object received
    */
   void receiveObject(Object obj) {
      received.add(obj);
      synchronized(this) {
         notifyAll();
      }
   }

   /**
    * Cancels the server and its associated classes.
    */
   private void cancel()  {
      hbSender.cancel();
      listener.cancel();
      cancel = true;
   }

   /**
    * Sends the current gamestate to all players in the multicast group.
    */
   private void multicastGameState() {
      gameState.incrementSequenceNumber();
      byte[] stateBytes = null;
      try {
         stateBytes = SharedUtilities.toByteArray(gameState);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
      if(stateBytes != null) {
         for(Player player : gameState.getPlayers()) {
            ScheduledFuture<?> future = clientAckTimeouts.get(player.getId());
            if(future != null) {
               future.cancel(false);
            }
            future = scheduler.schedule(() -> send(player, gameState), HEARTBEAT_INTERVAL,
                  MILLISECONDS);
            clientAckTimeouts.put(player.getId(), future);
            DatagramPacket packetOut = new DatagramPacket(stateBytes, stateBytes.length, group);
            try {
               socket.send(packetOut);
            }
            catch(IOException ioe) {
               ioe.printStackTrace();
            }
         }
      }
   }

   /**
    * Sends an object to a single player. Schedules a check to see
    * @param player The player
    * @param ser The serializable object to be sent
    */
   private void send(Player player, Serializable ser) {
      byte[] bytes = null;
      try {
         bytes =SharedUtilities.toByteArray(ser);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
      if(bytes != null) {
         ScheduledFuture<?> future = clientAckTimeouts.get(player.getId());
         if(future != null) {
            future.cancel(false);
         }
         future = scheduler.schedule(() -> send(player, gameState), HEARTBEAT_INTERVAL,
               MILLISECONDS);
         clientAckTimeouts.put(player.getId(), future);
         DatagramPacket packetOut = new DatagramPacket(bytes, bytes.length,
               player.getAddress());

         try {
            socket.send(packetOut);
         }
         catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }
   }
}