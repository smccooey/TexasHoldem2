package texasholdem.gamestate;

import java.io.Serializable;
import java.util.List;
import java.util.Scanner;

public class Player implements Serializable {

   private boolean isOp;
   private static final long serialVersionUID = 4664480702994610549L;
   private Card[] cards = new Card[2];
   private RankingEnum rankingEnum = null;
   private List<Card> rankingList = null;
   private Card highCard = null;
   private String username;
   private int amountOfMoney;
   Scanner sc = new Scanner(System.in);


   public boolean[] play; // 0- check, 1-raise, 2- fold 3- call
   private int turnOutCome;
   public Player(String name){
      username = name;
      amountOfMoney = 1000;
      isOp = false;
      play = new boolean[4];
   }

   // accept a boolean wether player needs to raise or not.
   public void takeTurn(){
      System.out.println("\n");
      showState();
      boolean didLastPlayerRaise = false;
      turnOutCome = -1;
      if(!didLastPlayerRaise){
         System.out.println("CHECK 0, FOLD 1,RAISE 2");
         turnOutCome = sc.nextInt();
         if (turnOutCome == 2) {
            boolean validPlay = false;
            while (!validPlay) {
               System.out.println("HOW MUCH? you have [" + amountOfMoney + "]");
               turnOutCome = sc.nextInt();
               if (!(turnOutCome > amountOfMoney) && turnOutCome > 1) {
                  takeMoney(turnOutCome);
                  validPlay = true;
               } else {
                  System.out.println("INVALID AMOUNT: " + turnOutCome);
               }
            }
         }
      } else {
         System.out.println("CALL 3, FOLD 1,RAISE 2");
         turnOutCome = sc.nextInt();
         if (turnOutCome == 2) {
            boolean validPlay = false;
            while (!validPlay) {
               System.out.println("HOW MUCH? you have [" + amountOfMoney + "]");
               turnOutCome = sc.nextInt();
               if (!(turnOutCome > amountOfMoney) && turnOutCome > 1) {
                  takeMoney(turnOutCome);
                  validPlay = true;
               } else {
                  System.out.println("INVALID AMOUNT: " + turnOutCome);
               }
            }
         }
      }
   }

   public String getUsername() {
      return username;
   }

   public int takeMoney(int amount){
      if(amountOfMoney > amount){
         amountOfMoney-=amount;
         return amount;
      } else {
         System.out.println("TAKING TOO MUCH MONEY!");
      }
      return -1;
   }

   public int getTurnOutCome() {
      return turnOutCome;
   }

   public void setTurnOutCome(int turnOutCome) {
      this.turnOutCome = turnOutCome;
   }

   public void setAsOp(){
      isOp = true;
   }

   public Card getHighCard() {
      return highCard;
   }

   public void setHighCard(Card highCard) {
      this.highCard = highCard;
   }

   public RankingEnum getRankingEnum() {
      return rankingEnum;
   }

   public void setRankingEnum(RankingEnum rankingEnum) {
      this.rankingEnum = rankingEnum;
   }

   public List<Card> getRankingList() {
      return rankingList;
   }

   public void setRankingList(List<Card> rankingList) {
      this.rankingList = rankingList;
   }

   public Card[] getCards() {
      return cards;
   }

   public void resetTakeTurn(){
      turnOutCome = 0;
   }

   public void setCards(Card[] cards) {
      this.cards = cards;
   }

   //Show cards and amount of money
   public void showState(){
      System.out.println(username+" you Have a ["+cards[0].toString()+"] and ["+cards[1].toString()+"] $" + amountOfMoney);
   }

   public int getAmountOfMoney() {
      return amountOfMoney;
   }

   public boolean isOp() {
      return isOp;
   }

   @Override
   public String toString() {
      return "Player{" + "isOp=" + isOp + ", username=" + username + ", amountOfMoney=" + amountOfMoney + '}';
   }
}