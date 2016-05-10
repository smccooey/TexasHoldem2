package texasholdem.client;

import texasholdem.Ack;
import texasholdem.SharedUtilities;
import texasholdem.gamestate.GameState;
import texasholdem.gamestate.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GameClient. Includes a separate listener thread to receive incoming
 * packets and forward them to this class. Note that all wait/notify calls
 * for this package should be synchronized on this client object.
 */
public class GameClient implements ClientConstants {

   /**
    * Separate thread to listen for incoming packets
    */
   private ClientListener listener;

   /**
    * Multicast address
    */
   private final SocketAddress group;

   /**
    * Server's address
    */
   private final SocketAddress server;

   /**
    * Multicast socket
    */
   private MulticastSocket socket;

   /**
    * The most current game state this client has received
    */
   private GameState gameState;

   /**
    * The Player object representing this client's player
    */
   private Player player;

   /**
    * Reads user input
    */
   private Scanner in;

   /**
    * Schedules timeout tasks
    */
   ScheduledThreadPoolExecutor scheduler;

   ScheduledFuture<?> future;

   /**
    * true if there is a new gamestate object that has not yet been handled
    */
   private final AtomicBoolean gameStateUpdated;

   /**
    * The player's name
    */
   private String name;

   /**
    * The client's current state
    */
   private volatile int state;

   /**
    * true if the client's participation in the game has been canceled
    */
   private volatile boolean cancel;

   /**
    * Constructs a client in the Texas Hold 'em game.
    */
   public GameClient() {
      in = new Scanner(System.in);
      System.out.println("Enter your name: ");
      name = in.nextLine();
      InetAddress groupAddress = null, serverAddress = null;
      try {
         groupAddress = InetAddress.getByAddress(MULTICAST_ADDRESS);
         serverAddress = InetAddress.getByAddress(MULTICAST_ADDRESS);
         socket = new MulticastSocket(PORT);
         socket.joinGroup(groupAddress);
         // socket.setSoTimeout(DROP_TIMEOUT);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
         System.exit(1);
      }

      group = new InetSocketAddress(groupAddress, PORT);
      server = new InetSocketAddress(serverAddress, PORT);

      // Create the player object
      try {
         player = new Player(name, SharedUtilities.bytesToLong(
               socket.getNetworkInterface().getHardwareAddress()));
      }
      catch(SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }

      state = JOINING;
      scheduler = new ScheduledThreadPoolExecutor(1);
      // Attempt to join the game
      joinGame();

      gameStateUpdated = new AtomicBoolean(false);

      // Set up listener for incoming packets
      listener = new ClientListener(this, socket, server);
      listener.start();

      cancel = false;

      // Do stuff
      doStuff();
   }

   /**
    * Reacts to new input.
    */
   private void doStuff() {
      while(!cancel) {
         if(gameStateUpdated.get()) {
            if(gameStateUpdated.compareAndSet(true, false)) {
               if(state == JOINING) {
                  // The new gamestate SHOULD contain this client's player,
                  // confirming that the player has joined the game.
                  if(state == JOINING) {
                     if(gameState.getPlayers().contains(player)) {
                        // Player has now joined the game
                        state = JOINED;
                        System.out.println("Players in game:");
                        for(Player p : gameState.getPlayers()) {
                           System.out.println("   " + p.getUsername());
                        }
                        System.out.println();
                     }
                     else {
                        // Player not in game. Resend player object
                        joinGame();
                     }
                  }
                  else if(state == JOINED) {
                     // Player is waiting for game to start
                     System.out.println("Players in game:");
                     for(Player p : gameState.getPlayers()) {
                        System.out.println("   " + p.getUsername());
                     }
                     System.out.println();
                  }
                  else if(state == IN_GAME) {

                  }
               }
            }
            else {
               // Pretty sure this should never happen, but just in case...
               throw new RuntimeException("Unexpected false value for " +
                     "gameStateUpdated.");
            }
         }
         try {
            // Take a nap
            synchronized(this) {
               wait();
            }
         }
         catch(InterruptedException ie) {
            // Do nothing
         }
      }
   }

   /**
    * Invoked when the listener thread receives a serialized object from the
    * server.
    * @param obj The object received
    */
   void receiveObject(Object obj) {
      if(obj == null) {
         throw new NullPointerException("Null received by " +
               "GameClient.receiveObject.");
      }
      else if(obj instanceof GameState) {
         GameState newGameState = (GameState)obj;
         // First acknowledge receipt
         Ack ack = new Ack(newGameState.getSequenceNumber(),
               getId());
         try {
            byte[] ackBytes = SharedUtilities.toByteArray(ack);
            socket.send(new DatagramPacket(ackBytes, ackBytes.length, server));
         }
         catch(IOException ioe) {
            ioe.printStackTrace();
         }

         // Further processing only needed if this is a new gamestate
         if(newGameState.getSequenceNumber() > gameState.getSequenceNumber()) {
            // Update gamestate field and let main thread handle it
            gameState = newGameState;
            gameStateUpdated.set(true);
         }
      }
      else {
         throw new RuntimeException("Unexpected object " +
               obj.getClass().getName() + " received.");
      }
      // Wake up the client if it is napping.
      synchronized(this) {
         notifyAll();
      }
   }

   /**
    * Returns the player's unique id.
    * @return The player's id
    */
   public long getId() {
      return player.getId();
   }

   /**
    * Cancels the client, shutting down packet listener.
    */
   public void cancel() {
      listener.cancel();
      cancel = true;
   }

   /**
    * Attempts to join the game by sending a Player object to the server.
    */
   private void joinGame() {
      try {
         byte[] playerBytes = SharedUtilities.toByteArray(player);
         DatagramPacket packet = new DatagramPacket(playerBytes,
               playerBytes.length, server);
         for(int attempts = 0; state == JOINING && attempts < 5; attempts++) {
            socket.send(packet);
            try {
               synchronized(this) {
                  wait(DROP_TIMEOUT);
               }
            }
            catch(InterruptedException ie) {
               continue;
            }
         }
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }

      if(state == JOINING) {
         // Five attempts have failed. Give up.
         System.out.print("Failed to join game.");
         state = IDLE;
      }
   }

   /**
    * Returns the client's current state.
    * @return Current state
    */
   int getState() {
      return state;
   }

   /**
    * Invoked when the client detects that it has been disconnected from the
    * server. This occurs when the client has not detected a heartbeat from
    * the server for a period of five heartbeat intervals.
    */
   void drop() {
      // ????
   }
}
