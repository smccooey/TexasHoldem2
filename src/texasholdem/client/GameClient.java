package texasholdem.client;

import texasholdem.Ack;
import texasholdem.Rejection;
import texasholdem.SharedUtilities;
import texasholdem.TexasHoldemConstants;
import texasholdem.gamestate.GameState;
import texasholdem.gamestate.Player;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * GameClient. Includes a separate listener thread to receive incoming
 * packets and forward them to this class. Note that all wait/notify calls
 * for this package should be synchronized on this client object.
 */
public class GameClient implements ClientState, TexasHoldemConstants {

   /**
    * Separate thread to listen for incoming packets
    */
   private final ClientListener listener;

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
   private final Scanner in;

   /**
    * Schedules timeout tasks
    */
   private final ScheduledThreadPoolExecutor scheduler;

   /**
    * The current future which checks for pending ACKs
    */
   private ScheduledFuture<?> future;

    /**
    * The player's name
    */
   private final String name;

   /**
    * The client's current state
    */
   private volatile int state;

   /**
    * true if the client's participation in the game has been canceled
    */
   private volatile boolean cancel;

   /**
    * Queue of objects which have been received but not yet handled
    */
   private final Queue<Object> received;

   /**
    * Sequence number of the gamestate for which an ACK is expected
    */
   private volatile int expectedAck;

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
         player = new Player(name, SharedUtilities.bytesToLong(socket.getNetworkInterface().
               getHardwareAddress()));
      }
      catch(SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }

      state = JOINING;
      scheduler = new ScheduledThreadPoolExecutor(2);
      // Attempt to join the game
      joinGame();

      // Set up listener for incoming packets
      listener = new ClientListener(this, socket, server);
      listener.start();

      received = new ConcurrentLinkedQueue<>();

      cancel = false;

      // Do stuff
      doStuff();
   }

   /**
    * First attempts to join the game, then simply reacts to new input.
    */
   private void doStuff() {
      joinGame();
      while(!cancel) {
         if(!received.isEmpty()) {
            Object obj = received.poll();
            if(obj != null) {
               // The new gamestate SHOULD contain this client's player,
               // confirming that the player has joined the game.
               if(state == JOINING) {
                  if(obj instanceof GameState) {
                     GameState newGameState = (GameState)obj;
                     // First acknowledge receipt
                     send(new Ack(newGameState.getSequenceNumber(), getId()), 1);

                     // Further processing only needed if this is a new gamestate
                     if(newGameState.getSequenceNumber() > gameState.getSequenceNumber()) {
                        // Update gamestate field and let main thread handle it
                        gameState = newGameState;
                        if(gameState.getPlayers().contains(player)) {
                           // Player has now joined the game
                           state = JOINED;
                           System.out.println("Players in game:");
                           for(Player p : gameState.getPlayers()) {
                              System.out.println("   " + p.getUsername());
                           }
                           System.out.println();
                        }
                     }
                     else {
                        // Player not in game. Resend player object
                        joinGame();
                     }
                  }
                  else if(obj instanceof Rejection) {
                     Rejection rejection = (Rejection)obj;
                     assert rejection.getId() == getId();
                     System.out.println("Could not join game.\n" + rejection.getMessage());
                     state = IDLE;
                  }
                  else if(obj instanceof Ack) {
                     Ack ack = (Ack)obj;
                     if(ack.getSequenceNumber() == expectedAck && future != null) {
                        future.cancel(true);
                     }
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
               else if(state == IDLE) {

               }
            }
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

   /**
    * Invoked when the listener thread receives a serialized object from the
    * server.
    * @param obj The object received
    */
   void receiveObject(Object obj) {
      if(obj == null) {
         throw new NullPointerException("Null received by GameClient#receiveObject.");
      }
      received.add(obj);
/*
      else if(obj instanceof GameState) {
         GameState newGameState = (GameState)obj;
         // First acknowledge receipt
         Ack ack = new Ack(newGameState.getSequenceNumber(), getId());
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
*/
      // Wake up the client if it is napping.
      synchronized(this) {
         notifyAll();
      }
   }

   /**
    * Returns the player's unique id.
    * @return The player's id
    */
   long getId() {
      return player.getId();
   }

   /**
    * Cancels the client, shutting down packet listener.
    */
   public void cancel() {
      listener.cancel();
      scheduler.shutdown();
      cancel = true;
      synchronized(this) {
         notifyAll();
      }
   }

   /**
    * Attempts to join the game by sending a Player object to the server.
    */
   private void joinGame() {
      send(player, 5);
      // Schedule a task to wait a little while, then check to see if player has joined the game
      scheduler.schedule(() -> {
         if(state == JOINING) {
            System.out.print("Failed to join game.");
            state = IDLE;
         }
         synchronized(this) {
            notifyAll();
         }
      }, DROP_TIMEOUT * 2, MILLISECONDS);
   }

   /**
    * Returns the client's current state.
    * @return Current state
    */
   int getState() {
      return state;
   }

   /**
    * Invoked when the client detects that it has been disconnected from the server. This occurs
    * when the client has not detected a heartbeat from the server for a period of five heartbeat
    * intervals.
    */
   void drop() {
      // ????
   }

   /**
    * Sends a serializable object to the server. This will only attempt to send the object the
    * number of times specified by the tries parameter.
    * @param ser The object to be sent
    * @param tries remaining number of tries
    */
   private void send(Serializable ser, int tries) {
      if(ser instanceof GameState) {
         if(future != null) {
            future.cancel(true);
         }
         expectedAck = ((GameState)ser).getSequenceNumber();
      }
      if(tries > 0) {
         future = scheduler.schedule(() -> send(ser, tries - 1), ACK_TIMEOUT, MILLISECONDS);
      }
      try {
         byte[] bytes = SharedUtilities.toByteArray(ser);
         socket.send(new DatagramPacket(bytes, bytes.length, server));
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }
}