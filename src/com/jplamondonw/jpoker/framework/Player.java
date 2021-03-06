package com.jplamondonw.jpoker.framework;

/**
 * A game player, including their hand and chips.
 */
public class Player
{
    // Properties
    //******************************
    /**
     * Whether this player is controlled by the user.
     */
    public final boolean isUser;

    /**
     * The player's display name.
     */
    public final String name;

    /**
     * The player's hand of cards.
     */
    public final Hand hand;

    /**
     * The number of chips in the player's bank.
     */
    public int chips = 0;

    /**
     * The number of chips the player has bet in the current round.
     */
    public int bet = 0;


    // Public methods
    //******************************
    /**
     * Construct an instance.
     * @param isUser Whether this player is controlled by the user.
     * @param name The player's display name.
     * @param chips The number of chips this player starts with.
     */
    public Player(boolean isUser, String name, int chips)
    {
        this.isUser = isUser;
        this.hand = new Hand(!isUser);
        this.name = name;
        this.chips = chips;
    }

    /**
     * Bet the specified amount if possible.
     * @param amount The amount to bet.
     * @return Returns whether the player can place that bet.
     */
    public boolean bet(int amount)
    {
        if(this.chips < amount)
            return false;

        this.chips -= amount;
        this.bet += amount;
        return true;
    }

    /**
     * Show all cards in the player's hand.
     */
    public void showHand()
    {
        this.hand.showCards();
    }
}
