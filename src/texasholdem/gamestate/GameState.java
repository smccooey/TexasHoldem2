/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package texasholdem.gamestate;

import java.util.List;

/**
 *
 * @author al3x901
 */
public class GameState {

   private Player currentPlayer; // WHOSE TURN IS IT?
   // needed because when same gs object is
   // sent to all players, players need to be individually updated.
   private List<Player> players;
   private int sequenceNumber;
   private List<Card> tableCards;
   private int pot = 0;
   private int numberOfturnsLeft;
   private int handsDealt = 0;
   private String message;
   private int MODE; // what mode is the server in

   public int getMODE() {
      return MODE;
   }

   public void setMODE(int MODE) {
      this.MODE = MODE;
   }


   public GameState(){

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

   public List<Card> getTableCards() {
      return tableCards;
   }

   public int getPot() {
      return pot;
   }

   public void setCurrentPlayer(Player currentPlayer) {
      this.currentPlayer = currentPlayer;
   }


   public void setTableCards(List<Card> tableCards) {
      this.tableCards = tableCards;
   }

   public void setPot(int pot) {
      this.pot = pot;
   }

   public List<Player> getPlayers() {
      return players;
   }

   public void setPlayers(List<Player> players) {
      this.players = players;
   }

   @Override
   public String toString() {
      return "GameState{" + "currentPlayer=" + currentPlayer + ", players=" + players.size() + ", tableCards=" + tableCards.size() + ", pot=" + pot + ", numberOfturnsLeft=" + numberOfturnsLeft + '}';
   }

   /**
    * Returns the gamestate's current sequence number.
    * @return The sequence number
    */
   public int getSequenceNumber() {
      return sequenceNumber;
   }

}