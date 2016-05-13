package texasholdem.gamestate;

import java.io.Serializable;
import java.net.SocketAddress;
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
    private boolean callMode;
    private int raiseAmount;
    private int amountOnTable;

    public boolean[] play; // 0- check, 1-raise, 2- fold 3- call 
    private int turnOutCome;

   /**
    * The player's unique id
    */
   private final long id;

   /**
    * The player's IP address and port number; used for unicast packets directed
    * to the player
    */
   private SocketAddress address;

   public Player(String name, long id) {
      username = name;
      amountOfMoney = 1000;
      amountOnTable = 0;
      isOp = false;
      play = new boolean[4];
      callMode = false;
      this.id = id;
   }

    // accept a boolean whether player needs to raise or not.
    public void takeTurn(Scanner sc) {
        System.out.println("\n");
        showState();
        turnOutCome = -1;
        if (!callMode) {
            System.out.println("CHECK 0, FOLD 1,RAISE 2");
            turnOutCome = sc.nextInt();
            if (turnOutCome == 2) {
                boolean validPlay = false;
                while (!validPlay) {
                    if (amountOnTable > 0) {
                        System.out.println("Choose an amount between to be raised on " + amountOnTable);
                    } else {
                        System.out.println("HOW MUCH? you have [" + amountOfMoney + "]");
                    }
                    turnOutCome = sc.nextInt();
                    if (!(turnOutCome > amountOfMoney) && turnOutCome > 1) {
                        takeMoney(turnOutCome);
                        amountOnTable += turnOutCome;
                        validPlay = true;
                    } else {
                        System.out.println("INVALID AMOUNT: " + turnOutCome);
                    }
                }
            }
        } else {
            System.out.println("CALL 0, FOLD 1,RAISE 2");
            System.out.println("BET TO PAY: $" + (raiseAmount - amountOnTable));
            turnOutCome = sc.nextInt();
            if (turnOutCome == 2) {
                boolean validPlay = false;
                while (!validPlay) {
                    System.out.println("HOW MUCH? you have [" + amountOfMoney + "]");
                    System.out.println("Choose an amount between to be raised on " + raiseAmount);
                    turnOutCome = sc.nextInt();
                    if (!(turnOutCome + raiseAmount > amountOfMoney) && turnOutCome > 1) {
                        amountOnTable += turnOutCome;
                        turnOutCome += raiseAmount;
                        takeMoney(turnOutCome);
                        callMode = false;
                        validPlay = true;
                    } else {
                        System.out.println("INVALID AMOUNT: " + turnOutCome);
                    }
                }
            } else if (turnOutCome == 0) {
                if (!(raiseAmount > amountOfMoney) && raiseAmount > 1) {
                    int callAmount = raiseAmount - amountOnTable;
                    takeMoney(callAmount);
                    amountOnTable += callAmount;
                    turnOutCome = callAmount;
                } else {
                    System.out.println("GO ALL IN 0, OR FOLD 1");
                    turnOutCome = sc.nextInt();
                    if (turnOutCome == 1) {
                        takeMoney(amountOfMoney);
                        turnOutCome = amountOfMoney;
                    }
                }
                raiseAmount = 0;
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public int takeMoney(int amount) {
        if (amountOfMoney >= amount) {
            amountOfMoney -= amount;
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

    public void setAsOp() {
        isOp = true;
    }

    public boolean isCallModeOn() {
        return callMode;
    }

    public void setCallMode(boolean callMode) {
        this.callMode = callMode;
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

    public void resetTakeTurn() {
        turnOutCome = 0;
    }

    public void setCards(Card[] cards) {
        this.cards = cards;
    }

    //Show cards and amount of money
    public void showState() {
        System.out.println(username + " you Have a [" + cards[0].toString() + "] and [" +
              cards[1].toString() + "] $" + amountOfMoney + " Money on table $" + amountOnTable);
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

    public int getRaiseAmount() {
        return raiseAmount;
    }

    public void setRaiseAmount(int raiseAmount) {
        this.raiseAmount = raiseAmount + amountOnTable;
    }

    public int getAmountOnTable() {
        return amountOnTable;
    }

    public void setAmountOnTable(int amountOnTable) {
        this.amountOnTable = amountOnTable;
    }

   /**
    * Returns the user's id.
    * @return The user's id
    */
   public long getId() {
      return id;
   }

   /**
    * Determines whether two players are in fact the same player.
    * @param other The other player
    * @return true iff the other player's id is the same as this player's
    */
   public boolean equals(Object other) {
      return other instanceof Player && id == ((Player)other).getId();
   }

   /**
    * Sets the player's address
    * @param address The new address
    */
   public void setAddress(SocketAddress address) {
      this.address = address;
   }

   /**
    * Returns the player's address
    * @return The player's address
    */
   public SocketAddress getAddress() {
      return address;
   }

}