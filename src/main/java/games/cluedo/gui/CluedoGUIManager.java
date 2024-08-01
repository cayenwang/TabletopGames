package games.cluedo.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.PartialObservableDeck;
import games.cluedo.CluedoConstants;
import games.cluedo.CluedoGameState;
import games.cluedo.CluedoParameters;
import games.cluedo.actions.ChooseCharacter;
import games.cluedo.actions.GuessPartOfCaseFile;
import games.cluedo.actions.ShowHintCard;
import games.cluedo.cards.CluedoCard;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class CluedoGUIManager extends AbstractGUIManager {
    // Settings for display areas
    final static int playerAreaWidth = 350;
    final static int playerAreaHeight = 135;
    final static int cCardWidth = 90;
    final static int cCardHeight = 110;

    CluedoDeckView[] playerHands;
    CluedoBoardView board;
    // Currently active player
    int activePlayer = -1;

    Border[] playerViewBorders;
    JLabel[] playerTurnOrderLabel;

    public CluedoGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);

        if (game != null) {
            CluedoGameState cgs = (CluedoGameState) game.getGameState();
            CluedoParameters cp = (CluedoParameters) cgs.getGameParameters();

            if (cgs != null) {
                // Initialise active player
                activePlayer = cgs.getCurrentPlayer();

                // Initialise size of window
                int nPlayers = cgs.getNPlayers();
                this.width = playerAreaWidth * 2;
                this.height = playerAreaHeight * 10; // TODO what is this doing

                // Create main game area that will hold all game views and sub-panels
                playerHands = new CluedoDeckView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                playerTurnOrderLabel = new JLabel[nPlayers];
                JPanel mainGameArea = new JPanel(new BorderLayout());
                JPanel topGameArea = new JPanel(new BorderLayout());
                JPanel bottomGameArea = new JPanel(new FlowLayout());
                JPanel middleGameArea = new JPanel(new FlowLayout());
                JPanel turnOrderPanel = new JPanel(new FlowLayout());
                JPanel cardsPanel1 = new JPanel(new FlowLayout());
                JPanel cardsPanel2 = new JPanel(new FlowLayout());

                topGameArea.add(turnOrderPanel, BorderLayout.NORTH);
                topGameArea.add(cardsPanel1, BorderLayout.SOUTH);
                bottomGameArea.add(cardsPanel2, BorderLayout.SOUTH);
                mainGameArea.add(topGameArea, BorderLayout.NORTH);
                mainGameArea.add(bottomGameArea, BorderLayout.SOUTH);
                mainGameArea.add(middleGameArea, BorderLayout.CENTER);

                // Get agent names
                List<String> agentNames = new ArrayList<>();
                for (int i = 0; i < nPlayers; i++) {
                    String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                    String agentName = split[split.length - 1];
                    agentNames.add(agentName);
                }

                int h = Collections.frequency(agentNames, "HumanGUIPlayer");

                // Human ac players' hands go on the bottom and agents' hands go on the top
                for (int i = 0; i < nPlayers; i++) {
                    String agentName = agentNames.get(i);

                    // Create border, layouts and keep track of this view
                    TitledBorder title = BorderFactory.createTitledBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                            TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
                    playerViewBorders[i] = title;

                    if (agentName.equals("HumanGUIPlayer")) {
                        CluedoDeckView playerHand = new CluedoDeckView(i, cgs.getPlayerHandCards().get(i), false, cp.getDataPath(), 1, cgs);
                        playerHand.setBorder(title);
                        cardsPanel2.add(playerHand);
                        playerHands[i] = playerHand;
                    } else {
                        CluedoDeckView playerHand = new CluedoDeckView(i, cgs.getPlayerHandCards().get(i), false, cp.getDataPath(), 1, cgs);
                        playerHand.setBorder(title);
                        cardsPanel1.add(playerHand);
                        playerHands[i] = playerHand;
                    }
                }

                // Board goes in the centre
                board = new CluedoBoardView(cgs, cgs.getGameBoard(), playerAreaWidth, playerAreaWidth);
                middleGameArea.add(board);

                // Turn order indicator
                turnOrderPanel.add(new JLabel("Turn Order: "));
                for (int i = 0; i < nPlayers; i++) {
                    JLabel playerLabel = new JLabel("Player " + i);
                    turnOrderPanel.add(playerLabel);
                    playerTurnOrderLabel[i] = playerLabel;
                }

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Cluedo", cgs, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createGridActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, null, null, null);

                // Add all views to frame
                parent.setLayout(new BorderLayout());
                parent.add(mainGameArea, BorderLayout.CENTER);
                parent.add(infoPanel, BorderLayout.NORTH);
                parent.add(actionPanel, BorderLayout.SOUTH);

                parent.revalidate();
                parent.setVisible(true);
                parent.repaint();
            }
        }
    }

    protected JComponent createGridActionPanel(IScreenHighlight[] highlights, int width, int height, boolean opaque, Consumer<ActionButton> onActionSelected,
                                           Consumer<ActionButton> onMouseEnter,
                                           Consumer<ActionButton> onMouseExit) {
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new GridLayout(0,3));

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights, onActionSelected, onMouseEnter, onMouseExit);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        JScrollPane pane = new JScrollPane(actionPanel);
        pane.setPreferredSize(new Dimension(width, height));

        pane.setMinimumSize(new Dimension(width, height));
        pane.setPreferredSize(new Dimension(width, height));

        actionPanel.setOpaque(opaque);
        pane.setOpaque(opaque);
        pane.getViewport().setOpaque(opaque);

        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        return pane;
    }

    @Override
    public int getMaxActionSpace() {
        return 10;
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);

            switch (cgs.getGamePhase().toString()) {
                case "chooseCharacter":
                    for (int i=0; i<actions.size(); i++) {
                        ChooseCharacter action = (ChooseCharacter) actions.get(i);
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setButtonAction(action, "Play as " + CluedoConstants.Character.values()[action.getCharacter()].toString());
                    }
                    break;
                case "makeSuggestion":
                    for (int i=0; i<actions.size(); i++) {
                        GuessPartOfCaseFile action = (GuessPartOfCaseFile) actions.get(i);
                        actionButtons[i].setVisible(true);
                        actionButtons[i].setButtonAction(action, "Suggest " + action.getGuessName());
                    }
                    break;
                case "revealCards":
                    for (int i=0; i<actions.size(); i++) {
                        if (actions.get(i) instanceof DoNothing) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setButtonAction(actions.get(i), "Have no cards to show");
                        } else {
                            ShowHintCard action = (ShowHintCard) actions.get(i);
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setButtonAction(action, "Show " + cgs.getPlayerHandCards().get(cgs.getCurrentPlayer()).get(action.getCardToShow()).toString() + " to Player " + cgs.getCurrentTurnPlayerId());
                        }
                    }
                    break;
                case "makeAccusation":
                    for (int i=0; i<actions.size(); i++) {
                        if (actions.get(i) instanceof DoNothing) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setButtonAction(actions.get(i), "Pass making accusation this turn");
                        } else {
                            GuessPartOfCaseFile action = (GuessPartOfCaseFile) actions.get(i);
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setButtonAction(action, "Accuse " + action.getGuessName());
                        }
                    }
                    break;
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        if (gameState != null) {
            for (int i=0; i<cgs.getNPlayers(); i++) {
                // Update all playerHands (notably the visibilities)
                playerHands[i].updateComponent(cgs.getPlayerHandCards().get(i));
            }
            // Update the board
            board.updateComponent(cgs.getGameBoard());

            // Update player highlights once they've chosen a character to play
            for (int i : cgs.characterToPlayerMap.keySet()) {
                Border compound = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CluedoConstants.characterIndexToColor.get(i), 5),
                        playerViewBorders[cgs.characterToPlayerMap.get(i)]);
                playerHands[cgs.characterToPlayerMap.get(i)].setBorder(compound);
            }

            // Update the turn order once they've all chosen a character to play
            if (cgs.characterToPlayerMap.size() == cgs.getNPlayers()) {
                int i=1;
                for (int j : cgs.getTurnOrderQueue()) {
                    // Set label order
                    playerTurnOrderLabel[i].setText("Player " + j);
                    for (int k : cgs.characterToPlayerMap.keySet()) {
                        if (cgs.characterToPlayerMap.get(k) == j) {
                            playerTurnOrderLabel[i].setForeground(CluedoConstants.characterIndexToColor.get(k));
                        }
                    }
                    i = (i+1) % cgs.getNPlayers();
                }
            }
        }
    }
}
