package games.cluedo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.*;
import core.interfaces.IGamePhase;
import games.GameType;

import java.util.*;

public class CluedoGameState extends AbstractGameState {

    GraphBoard gameBoard;

    // Map of playerId to character index
    // Character index is defined by 0:Scarlett, 1:Mustard, 2:Orchid etc. in turn order
    // This is useful iff the full board is implemented (different characters start near different rooms) and players want to tactically choose the character that starts nearer a specific room
    public HashMap<Integer, Integer> characterToPlayerMap = new HashMap<>();
    // Room names where each character is at, index corresponds to character index as defined above
    List<String> characterLocations;

    // List of whether each player has made an incorrect accusation (and is thus no longer allowed to make suggestions)
    List<Boolean> playerAliveStatus;

    // All cards in the game
    // (useful for implementing suggestions; only have to search through one deck to find the card you're suggesting)
    PartialObservableDeck<CluedoCard> allCards;
    // Cards in each player's hand, index corresponds to playerId
    List<PartialObservableDeck<CluedoCard>> playerHandCards;
    // Cards in the case file
    PartialObservableDeck<CluedoCard> caseFile;
    // Suggestion being made by current player
    public PartialObservableDeck<CluedoCard> currentGuess;

    Queue<Integer> turnOrderQueue;
    Queue<Integer> reactivePlayers;
    // ID of player who owns the current turn (ie the player who made the most recent suggestion)
    int currentTurnPlayerId;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers - number of players
     */
    public CluedoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Cluedo;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
            add(caseFile);
            add(gameBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        CluedoGameState copy = new CluedoGameState(gameParameters, playerId);

        copy.gameBoard = gameBoard.copy();

        copy.characterToPlayerMap = new HashMap<>();
        for (int i : characterToPlayerMap.keySet()) {
            copy.characterToPlayerMap.put(i, characterToPlayerMap.get(i));
        }
        copy.characterLocations = new ArrayList<>();
        for (int i = 0; i < CluedoConstants.Character.values().length; i++) {
            copy.characterLocations.add(characterLocations.get(i));
        }
        copy.playerAliveStatus = new ArrayList<>();
        copy.playerAliveStatus.addAll(playerAliveStatus);

        copy.allCards = allCards.copy();
        copy.playerHandCards = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            copy.playerHandCards.add(playerHandCards.get(i).copy());
        }
        copy.caseFile = caseFile.copy();
        copy.currentGuess = currentGuess.copy();

        copy.turnOrderQueue = new LinkedList<>();
        copy.turnOrderQueue.addAll(turnOrderQueue);
        copy.reactivePlayers = new LinkedList<>();
        copy.reactivePlayers.addAll(reactivePlayers);
        copy.currentTurnPlayerId = currentTurnPlayerId;

        if (playerId != -1) {
            // Add all cards that are not visible to the current player into a deck
            Deck<CluedoCard> unknownCharacters = new Deck<>("Unknown Characters", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            Deck<CluedoCard> unknownWeapons = new Deck<>("Unknown Weapons", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            Deck<CluedoCard> unknownRooms = new Deck<>("Unknown Rooms", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            for (int i = 0; i < nPlayers; i++) {
                if (i != playerId) {
                    PartialObservableDeck<CluedoCard> playerHand = playerHandCards.get(i);
                    for (int j=0; j<playerHand.getSize(); j++) {
                        if (!playerHand.getVisibilityForPlayer(j, playerId)) {
                            String cardType = getTypeOfCard(playerHand.get(j));
                            switch (cardType) {
                                case "Character":
                                    unknownCharacters.add((CluedoCard) playerHand.get(j).copy());
                                    break;
                                case "Weapon":
                                    unknownWeapons.add((CluedoCard) playerHand.get(j).copy());
                                    break;
                                case "Room":
                                    unknownRooms.add((CluedoCard) playerHand.get(j).copy());
                                    break;
                            }
                        }
                    }
                }
            }

            // If there are unknown cards in the players' hands, shuffle all the unknown cards around
            // If there aren't unknown cards in the players' hands, then we know what the caseFile is
            if (unknownCharacters.getSize() != 0 && unknownWeapons.getSize() != 0 && unknownRooms.getSize() != 0) {
                unknownCharacters.add(caseFile.get(1));
                unknownWeapons.add(caseFile.get(2));
                unknownRooms.add(caseFile.get(0));

                // Shuffle deck
                unknownCharacters.shuffle(redeterminisationRnd);
                unknownWeapons.shuffle(redeterminisationRnd);
                unknownRooms.shuffle(redeterminisationRnd);

                // Redistribute shuffled cards
                copy.caseFile.clear();
                copy.caseFile.add(unknownRooms.draw());
                copy.caseFile.add(unknownCharacters.draw());
                copy.caseFile.add(unknownWeapons.draw());

                Deck<CluedoCard> unknownCards = new Deck<>("Unknown Cards", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
                unknownCards.add(unknownCharacters);
                unknownCards.add(unknownWeapons);
                unknownCards.add(unknownRooms);
                unknownRooms.shuffle(redeterminisationRnd);

                for (int i = 0; i < nPlayers; i++) {
                    if (i != playerId) {
                        PartialObservableDeck<CluedoCard> playerHand = playerHandCards.get(i);
                        for (int j=0; j<playerHand.getSize(); j++) {
                            if (!playerHand.getVisibilityForPlayer(j, playerId)) {
                                copy.playerHandCards.get(i).setComponent(j, unknownCards.draw());
                            }
                        }
                    }
                }
            }
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (!playerAliveStatus.get(playerId)) return -1;
        if (getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME) return 1;

        List<Integer> hiddenCardCount = getHiddenCardCount(playerId);
        int possibleCaseFilesCount = hiddenCardCount.get(0) * hiddenCardCount.get(1) * hiddenCardCount.get(2);

        int totalPossibleCaseFiles = CluedoConstants.Character.values().length * CluedoConstants.Weapon.values().length *CluedoConstants.Room.values().length;

        // Having a low number of possible answers is preferable
        return (totalPossibleCaseFiles - possibleCaseFilesCount)/(totalPossibleCaseFiles);
    }

    public List<Integer> getHiddenCardCount(int playerId) {
        // Determine how many possible caseFiles match the cards already seen
        int hiddenCharactersCount = 1;
        int hiddenWeaponsCount = 1;
        int hiddenRoomsCount = 1;
        for (int i = 0; i < nPlayers; i++) {
            for (int j = 0; j < playerHandCards.get(i).getSize(); j++) {
                boolean visible = playerHandCards.get(i).getVisibilityForPlayer(j, playerId);
                if (!visible) {
                    String cardType = getTypeOfCard(playerHandCards.get(i).get(j));
                    switch (cardType) {
                        case "Character":
                            hiddenCharactersCount++;
                            break;
                        case "Weapon":
                            hiddenWeaponsCount++;
                            break;
                        case "Room":
                            hiddenRoomsCount++;
                            break;
                    }
                }
            }
        }
        return List.of(hiddenCharactersCount, hiddenWeaponsCount, hiddenRoomsCount);
    }

    private String getTypeOfCard(CluedoCard card) {
        try {
            CluedoConstants.Character.valueOf(card.getComponentName());
            return "Character";
        } catch (IllegalArgumentException ignored) {}
        try {
            CluedoConstants.Weapon.valueOf(card.getComponentName());
            return "Weapon";
        } catch (IllegalArgumentException ignored) {}
        try {
            CluedoConstants.Room.valueOf(card.getComponentName());
            return "Room";
        } catch (IllegalArgumentException ignored) {}

        System.out.println("No card type found");
        return "Error";
    }

    @Override
    public double getGameScore(int playerId) {
        if (playerResults[playerId] == CoreConstants.GameResult.WIN_GAME) return 1;
        return 0;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CluedoGameState that)) return false;
        return Objects.equals(gameBoard, that.gameBoard)
                && Objects.equals(characterToPlayerMap, that.characterToPlayerMap)
                && Objects.equals(characterLocations, that.characterLocations)
                && Objects.equals(playerAliveStatus, that.playerAliveStatus)
                && Objects.equals(allCards, that.allCards)
                && Objects.equals(playerHandCards, that.playerHandCards)
                && Objects.equals(caseFile, that.caseFile)
                && Objects.equals(currentGuess, that.currentGuess)
                && Objects.equals(turnOrderQueue, that.turnOrderQueue)
                && Objects.equals(reactivePlayers, that.reactivePlayers)
                && Objects.equals(currentTurnPlayerId, that.currentTurnPlayerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameBoard, characterToPlayerMap, characterLocations, playerAliveStatus,
                allCards, playerHandCards, caseFile, currentGuess,
                turnOrderQueue, reactivePlayers, currentTurnPlayerId) + 31 * super.hashCode();
    }

    public enum CluedoGamePhase implements IGamePhase {
        chooseCharacter,
        makeSuggestion,
        revealCards,
        makeAccusation
    }

    public List<PartialObservableDeck<CluedoCard>> getPlayerHandCards() {
        return playerHandCards;
    }

    public PartialObservableDeck<CluedoCard> getAllCards() {
        return allCards;
    }

    public GraphBoard getGameBoard() { return gameBoard; }

    public List<String> getCharacterLocations() { return characterLocations; }

    public int getCurrentTurnPlayerId() { return currentTurnPlayerId; }

    public Queue<Integer> getTurnOrderQueue() {
        return turnOrderQueue;
    }
}
