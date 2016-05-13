package texasholdem.client;

import java.util.NoSuchElementException;
import java.util.Scanner;
import texasholdem.StartRequest;

/**
 * Listens for console input.
 */
public class ConsoleListener extends Thread {

   /**
    * The game client
    */
   private GameClient client;


   private volatile boolean cancel;

   ConsoleListener(GameClient client) {
      this.client = client;
      cancel = false;
   }

   public void run() {
      System.out.println("Enter 1 to start game.");
      while(!cancel) {
         try {
            String input = GameClient.in.nextLine();
            if(input.equals("1")) {
               client.receiveObject(new StartRequest());
               cancel();
            }
         }
         catch(IndexOutOfBoundsException | NoSuchElementException ioobensee) {

         }
      }
   }

   void cancel() {
      cancel = true;
   }
}
