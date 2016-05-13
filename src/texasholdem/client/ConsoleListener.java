package texasholdem.client;

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

   private Scanner in;

   private volatile boolean cancel;

   ConsoleListener(GameClient client, Scanner in) {
      this.client = client;
      this.in = in;
      cancel = false;
   }

   public void run() {
      while(!cancel) {
         System.out.println("Enter 1 to start game.");
         if(in.nextLine().equals("1")) {
            client.receiveObject(new StartRequest());
            cancel();
         }
      }
   }

   void cancel() {
      cancel = true;
   }
}
