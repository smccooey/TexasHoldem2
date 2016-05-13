package texasholdem.client;

import texasholdem.Ack;
import texasholdem.Rejection;
import texasholdem.SharedUtilities;
import texasholdem.StartRequest;
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
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
    * Schedules timeout tasks
    */
   private final ScheduledThreadPoolExecutor scheduler;

   /**
    * The current future which checks for pending ACKs
    */
   private ScheduledFuture<?> ackFuture;

   private ScheduledFuture<?> joinFuture;

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

   private ConsoleListener consoleListener;

   public volatile boolean gameStateUpdated;

   // private int port;

   private InetAddress groupAddress;

   public static Scanner in = new Scanner(System.in);

   /**
    * Constructs a client in the Texas Hold 'em game.
    */
   public GameClient() {
      System.out.print("Enter your name: ");
      name = in.nextLine();
/*
      System.out.print("Enter your port number: ");
      port = Integer.parseInt(in.nextLine());
*/
      // name = "name" + Math.random();
      groupAddress = null;
      InetAddress serverAddress = null;
      try {
         groupAddress = InetAddress.getByAddress(MULTICAST_ADDRESS);
         serverAddress = InetAddress.getByAddress(SERVER_ADDRESS);
         socket = new MulticastSocket(PORT);
         socket.joinGroup(groupAddress);
         // socket.setSoTimeout(DROP_TIMEOUT);
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
         System.exit(1);
      }
      long id = new Random().nextLong();
      group = new InetSocketAddress(groupAddress, PORT);
      server = new InetSocketAddress(serverAddress, PORT);

      // Create the player object
/*
      try {
         player = new Player(name, SharedUtilities.bytesToLong(socket.getNetworkInterface().
               getHardwareAddress()));
      }
      catch(SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
*/
      player = new Player(name, id);

      state = JOINING;
      scheduler = new ScheduledThreadPoolExecutor(2);

      // Set up listener for incoming packets
      listener = new ClientListener(this, socket, server);
      listener.start();

      received = new ConcurrentLinkedQueue<>();

      cancel = false;

      // Attempt to join the game
      new Thread(() -> joinGame()).start();

      gameStateUpdated = false;

      // Do stuff
      doStuff();
   }

   public static void main(String[] args) {
      new GameClient();
   }

   /**
    * First attempts to join the game, then simply reacts to new input.
    */
   private void doStuff() {

      while(!cancel) {
         if(!received.isEmpty()) {
            // System.err.println("Polling received-object queue.");
            Object obj = received.poll();
            // System.err.println("Client received " + obj + " while in " + state + ".\n");
            if(obj != null) {
               // The new gamestate SHOULD contain this client's player,
               // confirming that the player has joined the game.
               if(state == JOINING) {
                  // System.err.println("Client state JOINING.\n");
                  if(obj instanceof GameState) {
                     // System.err.println("Client received GameState " + obj + "\n");
                     GameState newGameState = (GameState)obj;
                     // First acknowledge receipt
                     send(new Ack(newGameState.getSequenceNumber(), getId()));

                     // Further processing only needed if this is a new gamestate
                     if(gameState == null ||
                           newGameState.getSequenceNumber() > gameState.getSequenceNumber()) {
                        // Update gamestate field and let main thread handle it
                        gameState = newGameState;
                        int index = gameState.getPlayers().indexOf(player);
                        if(index != -1) {
                           String msg = gameState.getMessage();
                           if(msg != null) {
                              System.out.println(msg);
                           }
                           player = gameState.getPlayers().get(index);
                           System.out.println("You " + (player.isOp() ? "are" : "are not") +
                                 " the OP.");
                           state = JOINED;
                        }
                     }
                  }
                  else if(obj instanceof Rejection) {
                     // System.err.println("Client received Rejection " + obj + "\n");
                     Rejection rejection = (Rejection)obj;
                     assert rejection.getId() == getId();
                     System.out.println("Could not join game.\n" + rejection.getMessage());
                     state = IDLE;
                  }
                  else if(obj instanceof Ack) {
                     handleAck((Ack)obj);
                  }
               }
               else if(state == JOINED) {
                  // Player is waiting for game to start
                  if(obj instanceof StartRequest) {
                     // System.err.println("Handling startrequest.\n");
                     gameState.setMode(GAME_MODE);
                     state = IN_GAME;
                     if(consoleListener != null) {
                        consoleListener.cancel();
                        consoleListener = null;
                     }
                     send(gameState);
                  }
                  else if(obj instanceof GameState) {
                     // System.err.println("Client received GameState " + obj + "\n");
                     GameState newGameState = (GameState)obj;
                     send(new Ack(newGameState.getSequenceNumber(), getId()));
                     if(newGameState.getSequenceNumber() > gameState.getSequenceNumber()) {
                        gameState = newGameState;
                        if(gameState.getMode() == PREGAME_MODE) {
                           System.out.println("Players in game:");
                           for(Player p : gameState.getPlayers()) {
                              System.out.println("   " + p.getUsername());
                           }
                           if(consoleListener == null) {
                              consoleListener = new ConsoleListener(this);
                              consoleListener.start();
                           }
                        }
                        else if(gameState.getMode() == GAME_MODE) {
                           state = IN_GAME;
                        }
                     }
                  }
                  else if(obj instanceof Ack) {
                     handleAck((Ack)obj);
                  }

                  else if(gameState.getMode() == GAME_MODE) {
                     state = IN_GAME;
                     System.out.println("Game has started.");
                     takeTurn();
                  }
                  System.out.println(gameState.getMessage());
                  System.out.println();
               }
               else if(state == IN_GAME) {
                  if(obj instanceof GameState) {
                     // System.err.println("Client received GameState " + obj + "\n");
                     GameState newGameState = (GameState)obj;
                     send(new Ack(newGameState.getSequenceNumber(), getId()));
                     if(newGameState.getSequenceNumber() > gameState.getSequenceNumber()) {
                        int i = newGameState.getPlayers().indexOf(player);
                        player = newGameState.getPlayers().get(i);
                        gameState = newGameState;
                        if(gameState.getMode() == GAME_MODE) {
                           takeTurn();
                        }
                        else if(gameState.getMode() == GAME_OVER) {
                           System.out.println("Game over.\n" + gameState.getMessage());
                           state = IDLE;
                           System.exit(0);
                        }
                     }
                  }
                  else if(obj instanceof Ack) {
                     handleAck((Ack)obj);
                  }
               }
               else if(state == IDLE) {
                  System.out.println("Goodbye!");
                  System.exit(0);
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

   private void takeTurn() {
      if(gameState.getCurrentPlayer().equals(player)) {
         int index = gameState.getPlayers().indexOf(player);
         if(index != -1) {
            gameState.getPlayers().set(index, player);
         }

         Future<Boolean> turnTaker = Executors.newSingleThreadExecutor().submit(() -> player.takeTurn());

         new Thread(() -> {
            try {
               turnTaker.get();
            }
            catch(InterruptedException | ExecutionException ieee) {
               ieee.printStackTrace();
            }
            // System.err.println("SENDING GAME STATE");
            player = gameState.getPlayers().get(index);
            send(gameState);
            if(player.fold) {
               System.out.println("Folding.");
               cancel();
               System.exit(0);
            }
         }).start();
      }
      else {
         gameState.printGameStatus();
      }
   }

   /**
    * Invoked when the listener thread receives a serialized object from the server.
    * @param obj The object received
    */
   void receiveObject(Object obj) {
      if(obj == null) {
         throw new NullPointerException("Null received by GameClient#receiveObject.");
      }
      // System.err.println(obj + " added to queue.\n");
      received.add(obj);
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
      if(consoleListener != null) {
         consoleListener.cancel();
      }
      System.out.println("Disconnected from server; exiting.");
      System.exit(1);
      synchronized(this) {
         notifyAll();
      }
   }

   /**
    * Attempts to join the game by sending a Player object to the server.
    */
   private void joinGame() {
      for(int i = 0; i < 5; i++) {
         // System.err.println("Attempt #" + (i + 1) + " to join game.\n");
         send(player);
         try {
            Thread.sleep(DROP_TIMEOUT * 2);
         }
         catch(InterruptedException ie) {
         }
         if(state != JOINING) {
            break;
         }
      }
      synchronized(this) {
         notifyAll();
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
    * Invoked when the client detects that it has been disconnected from the server. This occurs
    * when the client has not detected a heartbeat from the server for a period of five heartbeat
    * intervals.
    */
   void drop() {
      cancel();
   }

   /**
    * Sends a serializable object to the server.
    * @param ser The object to be sent
    */
   public void send(Serializable ser) {
      if(ser instanceof GameState) {
         if(ackFuture != null) {
            ackFuture.cancel(true);
         }
         GameState gs = (GameState)ser;
         expectedAck = gs.getSequenceNumber();
         gs.setSender(getId());
      }
      try {
         byte[] bytes = SharedUtilities.toByteArray(ser);
         // System.err.println("Client sending " + ser + "\n");
         socket.send(new DatagramPacket(bytes, bytes.length, server));
      }
      catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }

   void handleAck(Ack ack) {
      // System.err.println("Client received Ack " + ack + "\n");
      if(ackFuture != null) {
         ackFuture.cancel(true);
      }
   }
}