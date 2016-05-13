package texasholdem.server;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
   private DatagramSocket socket;

   /**
    * Thread to listen for incoming packets
    */
   private final ServerListener listener;

   /**
    * Scheduled timeouts for client acks for gamestate multicasts
    */
   private final ConcurrentHashMap<Long, ScheduledFuture<?>> ackTimeouts;

   /**
    * Timeout scheduler
    */
   private final ScheduledThreadPoolExecutor scheduler;

   /**
    * Periodically multicasts heartbeats to clients
    */
   private final HeartbeatSender hbSender;

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
   private final Queue<Object> received;

   /**
    * The server's id
    */
   private final long id;

   /**
    * The current game state
    */
   private volatile GameState gameState;

   private static GameServer instance;

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
/*
      long tempId = 0;
*/
      try {
         socket = new DatagramSocket(PORT);
/*
         tempId = SharedUtilities.bytesToLong(((MulticastSocket)socket).getNetworkInterface().
               getHardwareAddress());
*/
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
         System.exit(1);
      }
/*
      id = tempId;
*/
      id = new Random().nextLong();

      gameState = new GameState();

      scheduler = new ScheduledThreadPoolExecutor(MAX_PLAYERS);

      ackTimeouts = new ConcurrentHashMap<>();
      received = new ConcurrentLinkedQueue<>();
      listener = new ServerListener(this, socket);
      listener.start();
      hbSender = new HeartbeatSender(group, socket, id);
      hbSender.start();

      instance = this;

      // Do stuff();
      doStuff();
   }

   private ServerGameRunner runner;

   /**
    * Does stuff as long as there is stuff to be done.
    */
   private void doStuff() {
      while(!cancel) {
         if(!received.isEmpty()) {
            Object obj = received.poll();
            if(obj != null) {
               if(obj instanceof Player) {
                  Player newPlayer = (Player)obj;
                  if(gameState.getMode() == WAITING_MODE) {
                     // This is the first player. Start new game
                     gameState.setMode(PREGAME_MODE);
                     gameState.addPlayer(newPlayer);
                     gameState.setMessage("WAITING FOR PLAYERS...");
                     multicastGameState();
                  }
                  else if(gameState.getMode() == PREGAME_MODE) {
                     // Add player to game
                     if(gameState.addPlayer(newPlayer)) {
                        if(gameState.isFull()) {
                           gameState.setMode(GAME_MODE);
                           gameState.setStartGame(true);

                           new ServerGameRunner(gameState);
                        }
                        multicastGameState();
                     }
                     else {
                        reject(newPlayer, "Game is full.");
                     }
                  }
                  else {
                     // Game already underway; reject player
                     reject(newPlayer, "Game is already in progress.");
                  }
               }
               else if(obj instanceof GameState) {
                  gameState = (GameState)obj;
                  if(gameState.getMode() == PREGAME_MODE) {
                     if(gameState.getPlayers().size() > 1 && gameState.getStartGame()) {
                        gameState.setMode(GAME_MODE);
                        new ServerGameRunner(gameState);
                     }
                     else {
                        gameState.setStartGame(false);
                     }
                  }
                  else if(gameState.getMode() == GAME_MODE) {
                     // Gamestate received from current player
                     System.err.println("Runner: " + runner);
                     if(runner == null) {
                        System.err.println("Starting game runner.");
                        runner = new ServerGameRunner(gameState);
                     }
                     multicastGameState();
                  }
               }
               else if(obj instanceof Ack) {
                  Ack ack = (Ack)obj;
                  if(ack.getSequenceNumber() == gameState.getSequenceNumber()) {
                     // This is an ack for the current gamestate
                     ScheduledFuture<?> future = ackTimeouts.remove(ack.getSender());
                     if(future != null) {
                        future.cancel(true);
                     }
                  }
               }
               else {
                  throw new RuntimeException("Unexpected " + obj.getClass().getName() +
                        "received.");
               }
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

   /**
    * Drops the specified player from the game.
    * @param id The id of the player to be dropped
    */
   void dropPlayer(long id) {
      // Stop checking for player's ACKs
      ScheduledFuture<?> future = ackTimeouts.remove(id);
      // Cancel any outstanding ACKs
      if(future != null) {
         future.cancel(true);
      }

      // Tell the listener to stop expecting heartbeats
      listener.dropPlayer(id);
   }

   /**
    * Invoked when the listener receives an object (other than a heartbeat)
    * from a client.
    * @param obj The object received
    */
   void receiveObject(Object obj) {
      System.err.println(obj + " added to queue.");
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
      scheduler.shutdown();
      cancel = true;
   }

   /**
    * Updates the gamestate object and multicasts it to all clients.
    * @param gameState The new gamestate
    */
   public void multicastGameState(GameState gameState) {
      this.gameState = gameState;
      multicastGameState();
   }

   /**
    * Sends the current gamestate to all players in the multicast group.
    */
   private void multicastGameState() {
      // System.err.println("Multicasting " + gameState + ".");
      gameState.incrementSequenceNumber();
      gameState.setSender(id);
      byte[] stateBytes = null;
      try {
         stateBytes = SharedUtilities.toByteArray(gameState);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
      if(stateBytes != null) {
         // Schedule an ACK check for each player
         for(Player player : gameState.getPlayers()) {
            ScheduledFuture<?> future = ackTimeouts.get(player.getId());
            if(future != null) {
               future.cancel(true);
            }
            future = scheduler.schedule(() -> send(player, gameState), ACK_TIMEOUT, MILLISECONDS);
            ackTimeouts.put(player.getId(), future);
         }
         // Multicast to the group
         DatagramPacket packetOut = new DatagramPacket(stateBytes, stateBytes.length, group);
         try {
            socket.send(packetOut);
         }
         catch(IOException ioe) {
            ioe.printStackTrace();
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
         bytes = SharedUtilities.toByteArray(ser);
         System.err.println("Server sending " + ser + " to " + player.getUsername() + "@" +
               player.getAddress() + "; " + bytes.length + ".");
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
      if(bytes != null) {
         ScheduledFuture<?> future = ackTimeouts.get(player.getId());
         if(future != null) {
            future.cancel(true);
         }
         future = scheduler.schedule(() -> send(player, gameState), HEARTBEAT_INTERVAL,
               MILLISECONDS);
         ackTimeouts.put(player.getId(), future);
         DatagramPacket packetOut = new DatagramPacket(bytes, bytes.length, player.getAddress());
         try {
            socket.send(packetOut);
         }
         catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }
   }

   /**
    * Reject a player from the game. Note that this notice does not require an ACK.
    * @param player The player to be rejected
    * @param msg The message to be sent
    */
   private void reject(Player player, String msg) {
      // Unicast a rejection
      Rejection rejection = new Rejection(player.getId(), msg);
      byte[] rejectionBytes = null;
      try {
         rejectionBytes = SharedUtilities.toByteArray(rejection);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
      if(rejectionBytes != null) {
         DatagramPacket packetOut = new DatagramPacket(rejectionBytes,
               rejectionBytes.length, player.getAddress());
         try {
            socket.send(packetOut);
         }
         catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }
   }

   public static GameServer getInstance() {
      return instance;
   }
}