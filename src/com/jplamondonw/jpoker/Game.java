package com.jplamondonw.jpoker;

import com.jplamondonw.jpoker.framework.ConsoleHelper;
import com.jplamondonw.jpoker.framework.Constants;
import com.jplamondonw.jpoker.framework.Deck;
import com.jplamondonw.jpoker.framework.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Manages an ongoing game.
 */
public class Game
{
    // Properties
    //******************************
    /**
     * The console to which to draw and from which to get input.
     */
    private final ConsoleHelper console;

    /**
     * The underlying game board.
     */
    private final GameBoard board = new GameBoard();

    /**
     * The game log shown to the user.
     */
    private final GameLog log = new GameLog(5);

    /**
     * Whether the user has the dealer coin.
     */
    public boolean isUserDealer;


    // Public methods
    //******************************
    public Game(ConsoleHelper console)
    {
        this.console = console;
        this.board.draw(console, Constants.ScreenLayout.GAME_BOARD.y, Constants.ScreenLayout.GAME_BOARD.x);
    }

    /**
     * Run the game until it completes.
     */
    public void run() throws IOException, InterruptedException
    {
        // get references for simplicity
        GameBoard board = this.board;
        Deck deck = this.board.deck;
        Player user = this.board.user;
        Player bot = this.board.bot;

        // start game
        this.log.add("Welcome to Texas Hold'em heads-up tournament style! We'll be");
        this.log.add("playing with a few house rules. (If you know the standard rules,");
        this.log.add("you'll be fine.)");

        // start game loop
        while(true)
        {
            // confirm game start
            this.log.add("Ready? (y)es (n)o");
            this.draw();
            if (this.getChoice("y", "n").equals("n"))
            {
                this.log.add("Good bye. :)");
                this.draw();
                break;
            }
            this.log.clear();

            // report initial state
            this.log.add("You each have $" + user.chips + " in chips. We'll start the big blind at $" + this.board.bigBlind + ".");

            // randomly choose dealer
            this.isUserDealer = new Random().nextBoolean();
            if(this.isUserDealer)
                this.log.add("Rolling the dice... you have the dealer coin!");
            else
                this.log.add("Rolling the dice... they have the dealer coin.");
            this.draw();

            // place initial bets & distribute cards
            this.log.add("Placing initial bets.");
            this.log.add("Dealing hole cards.");
            this.getDealer().bet(board.smallBlind);
            this.getOther().bet(board.bigBlind);
            user.hand.add(Arrays.asList(deck.drawCard(), deck.drawCard()));
            bot.hand.add(Arrays.asList(deck.drawCard(), deck.drawCard()));
            this.draw();

            // preflop phase
            this.runBettingRound(board, this.board.bigBlind);
            this.draw();

            // end game
            break;
        }
    }

    // Private methods
    //******************************
    // Game logic
    //**************
    /**
     * Run a round of betting.
     * @param board The game board.
     * @param minBet The minimum bet.
     */
    private void runBettingRound(GameBoard board, int minBet)
    {
        Player user = board.user;
        Player bot = board.bot;

        boolean botCanBet = true;
        boolean userCanBet = true;
        while(botCanBet && userCanBet)
        {
            // run bot action (if first)
            if(!this.isUserDealer)
                botCanBet = this.runBotBet(bot, user, minBet);

            // run player action
            if(user.chips < minBet - (user.bet % minBet))
            {
                this.log.add("You don't have enough chips to bet.");
                userCanBet = false;
            }
            else
            {
                // get available player actions
                String question;
                List<String> choices = new ArrayList<>();
                {
                    List<String> labels = new ArrayList<>();

                    // fold
                    labels.add("(f)old");
                    choices.add("f");

                    // call (or check)
                    if(bot.bet > user.bet)
                    {
                        labels.add("(c)all");
                        choices.add("c");
                    }
                    else
                    {
                        labels.add("(c)heck");
                        choices.add("c");
                    }

                    // raise (or bet)
                    if((bot.bet + user.bet) > 0)
                    {
                        labels.add("(r)aise");
                        choices.add("r");
                    }
                    else
                    {
                        labels.add("(b)et");
                        choices.add("b");
                    }

                    // build question
                    question = "Do you want to " + String.join(" or ", labels) + "?";
                }

                // perform player action
                this.log.add(question);
                this.draw();
                switch(this.getChoice(choices.toArray(new String[0])))
                {
                    // fold
                    // ends betting round
                    case "f":
                        this.log.add("You folded.");
                        return;

                    // call/check
                    case "c":
                        // call
                        if(bot.bet > user.bet)
                        {
                            int bet = bot.bet - user.bet;
                            user.bet(bet);
                            this.log.add("You called for $" + bet + ".");
                        }

                        // check
                        else
                            this.log.add("You checked.");
                        break;

                    // raise/bet
                    case "b":
                    case "r":
                        String verb = (bot.bet + user.bet) > 0 ? "raise" : "bet";

                        // get raise amount
                        int bet;
                        this.log.add("How much do you want to " + verb + "?");
                        while(true)
                        {
                            this.draw();

                            // get amount
                            String input = this.getInput();
                            if(!input.chars().allMatch(Character::isDigit))
                            {
                                this.log.add("Please enter a numeric " + verb + ".");
                                continue;
                            }
                            bet = Integer.parseInt(input);

                            // validate
                            if(bet < minBet)
                                this.log.add("The minimum " + verb + " is $" + minBet + ".");
                            else if(bet > user.chips)
                                this.log.add("You don't have that many chips to " + verb + ".");
                            else
                                break;
                        }

                        // round to nearest interval
                        bet -= bet % minBet;

                        // place bet
                        this.log.add("You bet $" + bet + ".");
                        user.bet(bet);
                        break;

                    // shouldn't happen
                    default:
                        this.log.add("Uh oh. That choice isn't valid. Let's pretend that didn't happen.");
                        break;
                }
            }

            // let bot bet if they're second
            if(this.isUserDealer)
                botCanBet = this.runBotBet(bot, user, minBet);

            // check round end condition
            if(user.bet == bot.bet)
                break;
        }
    }

    /**
     * Perform the bot's action during a betting round.
     * @param bot The bot player.
     * @param user The human player.
     * @param minBet The minimum bet.
     */
    private boolean runBotBet(Player bot, Player user, int minBet)
    {
        // get minimum bet allowed
        int bet = Math.max(Math.max(user.bet - bot.bet, bot.bet - minBet), 0);

        // place bet
        if(bet == 0)
        {
            this.log.add("They called.");
            return true;
        }
        if(bot.bet(bet))
        {
            this.log.add("They bet $" + bet + ".");
            return true;
        }
        else
        {
            this.log.add("They don't have enough chips to bet.");
            return false;
        }
    }

    /**
     * Get the player who has the dealer coin.
     */
    private Player getDealer()
    {
        return this.isUserDealer ? this.board.user : this.board.bot;
    }

    /**
     * Get the non-dealer player.
     */
    private Player getOther()
    {
        return this.isUserDealer ? this.board.bot : this.board.user;
    }

    /**
     * Get the other player.
     * @param player The player to ignore.
     */
    private Player getOther(Player player)
    {
        return player == this.board.user ? this.board.bot : this.board.user;
    }

    // Drawing & UI
    //**************
    /**
     * Draw the current game state.
     */
    private void draw()
    {
        // clear screen
        this.console.clear();

        // draw elements
        this.board.draw(console, Constants.ScreenLayout.GAME_BOARD.y, Constants.ScreenLayout.GAME_BOARD.x);
        this.log.draw(console, Constants.ScreenLayout.GAME_LOG.y, Constants.ScreenLayout.GAME_LOG.x);

        // move cursor out of the way
        this.console.setCursor(Constants.ScreenLayout.USER_INPUT);
    }

    /**
     * Get input from the user.
     */
    private String getInput()
    {
        this.console.setCursor(Constants.ScreenLayout.USER_INPUT);
        this.console.clearFromCursorLine();
        this.console.out.print("> ");
        return this.console.console.readLine();
    }

    /**
     * Get the user's choice.
     * @param choices The available choices. These are not case-sensitive.
     */
    private String getChoice(String... choices)
    {
        // get case-insensitive choices
        List<String> choiceLookup = new ArrayList<>();
        for(String choice : choices)
            choiceLookup.add(choice.toLowerCase());

        // get valid input
        while(true)
        {
            // draw input
            String choice = this.getInput().toLowerCase();
            if(choiceLookup.contains(choice))
                return choice;
        }
    }
}
