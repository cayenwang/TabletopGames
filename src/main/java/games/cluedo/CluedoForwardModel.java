package games.cluedo;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.PartialObservableDeck;
import games.cluedo.actions.ChooseCharacter;
import games.cluedo.actions.GuessPartOfCaseFile;
import games.cluedo.actions.ShowHintCard;
import games.cluedo.cards.CharacterCard;
import games.cluedo.cards.CluedoCard;
import games.cluedo.cards.RoomCard;
import games.cluedo.cards.WeaponCard;

import java.util.*;

public class CluedoForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        CluedoGameState cgs = (CluedoGameState) firstState;

        cgs.gameBoard = new GraphBoard("Game Board");
        cgs.characterToPlayerMap = new HashMap<>();
        cgs.characterLocations = new ArrayList<>();
        cgs.playerAliveStatus = new ArrayList<>(Collections.nCopies(cgs.getNPlayers(), Boolean.TRUE));
        cgs.playerHandCards = new ArrayList<>();
        cgs.caseFile = new PartialObservableDeck<>("Case File", -1, new boolean[cgs.getNPlayers()]);
        cgs.currentGuess = new PartialObservableDeck<>("Current Guess", -1, new boolean[cgs.getNPlayers()]);
        cgs.turnOrderQueue = new LinkedList<>();
        cgs.reactivePlayers = new LinkedList<>();

        // Initialise the board // TODO change the initialisation so that we can include 'corridor' segments
        BoardNode startNode = new BoardNode(CluedoConstants.Room.values().length, "START");
        cgs.gameBoard.addBoardNode(startNode);
        // For each room, create a BoardNode
        for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
            BoardNode roomNode = new BoardNode(CluedoConstants.Room.values().length, room.name());
            cgs.gameBoard.addBoardNode(roomNode);
            // Add an edge to all previous nodes in the GraphBoard
            for (BoardNode node : cgs.gameBoard.getBoardNodes()) {
                if (!node.equals(roomNode)) {
                    cgs.gameBoard.addConnection(node, roomNode);
                }
            }
        }

        // All characters start in the startNode node
        // Replaces the fact that in the original game, characters start in given positions in the corridor
        // Including a startNode ensures characters don't start in a room
        for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
            cgs.characterLocations.add(startNode.getComponentName());
        }

        // Randomly choose the Character, Weapon and Room to go into the caseFile
        CluedoConstants.Character randomCharacter = CluedoConstants.Character.values()[cgs.getRnd().nextInt(CluedoConstants.Character.values().length)];
        CluedoConstants.Weapon randomWeapon = CluedoConstants.Weapon.values()[cgs.getRnd().nextInt(CluedoConstants.Weapon.values().length)];
        CluedoConstants.Room randomRoom = CluedoConstants.Room.values()[cgs.getRnd().nextInt(CluedoConstants.Room.values().length)];

        CluedoCard randomCharacterCard = new CharacterCard(randomCharacter);
        CluedoCard randomWeaponCard = new WeaponCard(randomWeapon);
        CluedoCard randomRoomCard = new RoomCard(randomRoom);

        cgs.caseFile.add(randomRoomCard, new boolean[cgs.getNPlayers()]);
        cgs.caseFile.add(randomCharacterCard, new boolean[cgs.getNPlayers()]);
        cgs.caseFile.add(randomWeaponCard, new boolean[cgs.getNPlayers()]);

        // Create the deck of all cards (Characters, Weapons and Rooms) without the 3 cards in the caseFile
        cgs.allCards = new PartialObservableDeck<>("All Cards", -1, new boolean[cgs.getNPlayers()]);
        for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
            if (!character.equals(randomCharacter)) {
                cgs.allCards.add(new CharacterCard(character));
            }
        }
        for (CluedoConstants.Weapon weapon : CluedoConstants.Weapon.values()) {
            if (!weapon.equals(randomWeapon)) {
                cgs.allCards.add(new WeaponCard(weapon));
            }
        }
        for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
            if (!room.equals(randomRoom)) {
                cgs.allCards.add(new RoomCard(room));
            }
        }

        // Shuffle the deck of cards and deal them out to players
        cgs.allCards.shuffle(cgs.getRnd());

        for (int i=0; i<cgs.getNPlayers(); i++) {
            boolean[] visible = new boolean[cgs.getNPlayers()];
            visible[i] = true;
            PartialObservableDeck<CluedoCard> playerCards = new PartialObservableDeck<>("Player Cards", i, visible);
            cgs.playerHandCards.add(playerCards);
        }

        int cardCount = 0;
        while (cardCount < cgs.allCards.getSize()) {
            int playerId = cardCount % cgs.getNPlayers();
            CluedoCard c = cgs.allCards.peek(cardCount);
            c.setOwnerId(playerId);
            cgs.playerHandCards.get(playerId).add(c);
            cardCount += 1;
        }

        // Add the cards in the caseFile to the deck of all cards for completeness
        cgs.allCards.add(randomRoomCard, new boolean[cgs.getNPlayers()]);
        cgs.allCards.add(randomCharacterCard, new boolean[cgs.getNPlayers()]);
        cgs.allCards.add(randomWeaponCard, new boolean[cgs.getNPlayers()]);

        // Set the first gamePhase to be choosing characters
        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.chooseCharacter);

        // Set the initial turn order to be ordered by increasing playerId
        for (int i=1; i<cgs.getNPlayers(); i++) {
            cgs.turnOrderQueue.add(i);
        }

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();

        switch (cgs.getGamePhase().toString()) {
            case "chooseCharacter":
                // Player can choose any unchosen character
                for (int i=0; i<CluedoConstants.Character.values().length; i++) {
                    if (!cgs.characterToPlayerMap.containsKey(i)) {
                        actions.add(new ChooseCharacter(cgs.getCurrentPlayer(), i));
                    }
                }
                break;
            case "makeSuggestion":
            case "makeAccusation":
                int currentGuessSize = cgs.currentGuess.getSize();

                if (currentGuessSize == 0) { // First, the player guesses a room
                    for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
                        actions.add(new GuessPartOfCaseFile(cgs, room));
                    }
                } else if (currentGuessSize == 1) { // Second, the player guesses a character
                    for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
                        actions.add(new GuessPartOfCaseFile(cgs, character));
                    }
                } else if (currentGuessSize == 2) { // Finally, the player guesses a weapon
                    for (CluedoConstants.Weapon weapon : CluedoConstants.Weapon.values()) {
                        actions.add(new GuessPartOfCaseFile(cgs, weapon));
                    }
                }
                break;
            case "revealCards":
                if (cgs.getCurrentPlayer() == cgs.currentTurnPlayerId) {
                    actions.add(new DoNothing()); // TODO implement skip currentPlayer in revealCards phase
                } else {
                    PartialObservableDeck<CluedoCard> playerHand = cgs.getPlayerHandCards().get(cgs.getCurrentPlayer());
                    for (int i=0; i<playerHand.getSize(); i++) {
                        if (cgs.currentGuess.contains(playerHand.get(i))) {
                            actions.add(new ShowHintCard(cgs.getCurrentPlayer(), cgs.currentTurnPlayerId, i));
                        }
                    }
                    if (actions.isEmpty()) {
                        actions.add(new DoNothing());
                    }
                }
                break;
        }

        if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeAccusation
                && cgs.currentGuess.getSize() == 0) {
            actions.add(new DoNothing()); // choose to not accuse
        }

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        CluedoGameState cgs = (CluedoGameState) gameState;

        switch (cgs.getGamePhase().toString()) {
            case "chooseCharacter":
                if (cgs.characterToPlayerMap.size() != cgs.getNPlayers()) {
                    endRound(cgs, nextPlayer(cgs));
                } else {
                    // Change turn order to be in order of character index, rather than playerId
                    setPlayerOrder(cgs, cgs.characterToPlayerMap);

                    // set the next player
                    endRound(cgs, nextPlayer(cgs));
                    cgs.currentTurnPlayerId = cgs.getCurrentPlayer();

                    cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);
                }
                break;
            case "makeSuggestion":
                if (cgs.currentGuess.getSize() == 1) { // last action was to suggest a room
                    // Get current character index
                    int currentCharacterIndex = -1;
                    for (int key : cgs.characterToPlayerMap.keySet()) {
                        if (cgs.characterToPlayerMap.get(key) == cgs.getCurrentPlayer()) {
                            currentCharacterIndex = key;
                        }
                    }
                    // Move the character to the room
                    cgs.characterLocations.set(currentCharacterIndex, cgs.currentGuess.get(0).toString());
                }
                if (cgs.currentGuess.getSize() == 2) { // last action was to suggest a character
                    // Get suggested character index
                    int suggestedCharacterIndex = CluedoConstants.Character.valueOf(cgs.currentGuess.get(0).toString()).ordinal();
                    // Move the character to the room
                    cgs.characterLocations.set(suggestedCharacterIndex, cgs.currentGuess.get(1).toString());
                }
                if (cgs.currentGuess.getSize() == 3) { // last action was to suggest a weapon
                    cgs.currentTurnPlayerId = cgs.getCurrentPlayer();
                    // add all players to reactive turn order (excluding current player) in order of character index
                    cgs.reactivePlayers.addAll(cgs.turnOrderQueue);

                    // set the next player
                    endRound(cgs, nextPlayer(cgs));

                    cgs.setGamePhase(CluedoGameState.CluedoGamePhase.revealCards);
                }
                break;
            case "revealCards":
                if (cgs.getCurrentPlayer() == cgs.currentTurnPlayerId) {
                    cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
                    cgs.currentGuess.clear();
                } else {
                    if (action instanceof DoNothing) {
                        endRound(cgs, nextPlayer(cgs));
                    } else {
                        cgs.reactivePlayers.clear();
                        endRound(cgs, cgs.currentTurnPlayerId);
                        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
                        cgs.currentGuess.clear();
                    }
                }
                break;
            case "makeAccusation":
                if (action instanceof DoNothing) {
                    endRound(cgs, nextPlayer(cgs));
                    cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);
                    cgs.currentGuess.clear();
                }
                if (cgs.currentGuess.getSize() == 3) {
                    if (guessMatchesCaseFile(cgs)) {
                        cgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                        for (int i=0; i < cgs.getNPlayers(); i++) {
                            cgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                        }
                        cgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, cgs.getCurrentPlayer());
                    } else {
                        cgs.playerAliveStatus.set(cgs.getCurrentPlayer(), false);
                        if (!(cgs.playerAliveStatus.contains(true))) {
                            cgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                            for (int i=0; i < cgs.getNPlayers(); i++) {
                                cgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                            }
                        } else {
                            endRound(cgs, nextPlayer(cgs));
                            cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);
                            cgs.currentGuess.clear();
                        }
                    }
                }
                break;
        }
    }

    private boolean guessMatchesCaseFile(AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        return cgs.currentGuess.equals(cgs.caseFile);
    }

    protected int nextPlayer(AbstractGameState gameState) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        if (!cgs.reactivePlayers.isEmpty()) {
            return cgs.reactivePlayers.remove();
        } else {
            while (!cgs.playerAliveStatus.get(cgs.turnOrderQueue.peek())) {
                cgs.turnOrderQueue.add(cgs.turnOrderQueue.peek());
                cgs.turnOrderQueue.remove();
            }
            cgs.turnOrderQueue.add(cgs.turnOrderQueue.peek());
            return cgs.turnOrderQueue.remove();
        }
    }

    public void setPlayerOrder(AbstractGameState gameState, HashMap<Integer, Integer> characterToPlayerMap) {
        CluedoGameState cgs = (CluedoGameState) gameState;
        cgs.turnOrderQueue.clear();
        for (int i : characterToPlayerMap.keySet()) {
            cgs.turnOrderQueue.add(characterToPlayerMap.get(i));
        }
    }

}
