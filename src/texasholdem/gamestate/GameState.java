/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package texasholdem.gamestate;

import java.io.Serializable;
import java.util.ArrayList;

import texasholdem.TexasHoldemConstants;


/**
 *
 * @author al3x901
 */
public class GameState implements Serializable, TexasHoldemConstants {

   private Player currentPlayer; // WHOSE TURN IS IT?
   private ArrayList<Player> players; // needed because when same gs object is sent to all players, playes need to be individually updated.
   private int sequenceNumber;
   private ArrayList<Card> tableCards;
   private int pot = 0;
   private int numberOfturnsLeft;
   private int handsDealt = 0;
   private String message;
   private int mode; // what mode is the server in

   /**
    * The sender's id
    */
   private long sender;

   public int getMode() {
      return mode;
   }

   public void setMode(int mode) {
      this.mode = mode;
   }

   /**
    * Constructs a new game state using the specified player as the OP.
    */
   public GameState(){
      players = new ArrayList<>(MAX_PLAYERS);
      sequenceNumber = 0;
      mode = WAITING_MODE;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public int getHandsDealt() {
      return handsDealt;
   }

   public void setHandsDealt(int handsDealt) {
      this.handsDealt = handsDealt;
   }

   public int getNumberOfturnsLeft() {
      return numberOfturnsLeft;
   }

   public void setNumberOfturnsLeft(int numberOfturnsLeft) {
      this.numberOfturnsLeft = numberOfturnsLeft;
   }

   public Player getCurrentPlayer() {
      return currentPlayer;
   }

   public ArrayList<Card> getTableCards() {
      return tableCards;
   }

   public int getPot() {
      return pot;
   }

   public void setCurrentPlayer(Player currentPlayer) {
      this.currentPlayer = currentPlayer;
   }


   public void setTableCards(ArrayList<Card> tableCards) {
      this.tableCards = tableCards;
   }

   public void setPot(int pot) {
      this.pot = pot;
   }

   public ArrayList<Player> getPlayers() {
      return players;
   }

   public void setPlayers(ArrayList<Player> players) {
      this.players = players;
   }

   @Override
   public String toString() {
      return "GameState{" + "currentPlayer=" + currentPlayer + ", players=" + players.size() +
            ", tableCards=" + tableCards.size() + ", pot=" + pot + ", numberOfturnsLeft=" +
            numberOfturnsLeft + '}';
   }

   /*
    * These methods added by Sean
    */
   /**
    * Returns the gamestate's current sequence number.
    * @return The sequence number
    */
   public int getSequenceNumber() {
      return sequenceNumber;
   }

   /**
    * Sets the sender's id.
    * @param sender The sender's id
    */
   public void setSender(long sender) {
      this.sender = sender;
   }

   /**
    * Returns the id of the sender of the gamestate.
    * @return The sender's id
    */
   public long getSender() {
      return sender;
   }

   /**
    * Increments the sequence number.
    */
   public void incrementSequenceNumber() {
      sequenceNumber++;
   }

   /**
    * Attempts to add the specified player to the game. Returns true if the
    * player has been added, false otherwise.
    * @param p The player to be added
    * @return true iff the player is successfully added
    */
   public boolean addPlayer(Player p) {
      if(players.contains(p) || isFull()) {
         return false;
      }
      if(players.isEmpty()) {
         p.setAsOp();
      }
      players.add(p);
      return true;
   }

   /**
    * Returns true if the list of players has reached its maximum size.
    * @return true if the list of players has reached its maximum size
    */
   public boolean isFull() {
      return players.size() == MAX_PLAYERS;
   }
}