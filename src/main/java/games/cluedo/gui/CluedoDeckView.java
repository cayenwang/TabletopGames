package games.cluedo.gui;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.cluedo.CluedoGameState;
import games.cluedo.CluedoCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.cluedo.gui.CluedoGUIManager.*;

public class CluedoDeckView extends DeckView<CluedoCard> {

    Image backOfCard;
    String dataPath;
    CluedoGameState cgs;

    public CluedoDeckView(int player, PartialObservableDeck<CluedoCard> d, boolean visible, String dataPath, double scale, AbstractGameState gameState) {
        super(player, d, visible, (int) (cCardWidth*scale), (int) (cCardHeight*scale), new Rectangle(5, 5, (int) (playerAreaWidth*scale), (int) (playerAreaHeight*scale)));
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "BACK_OF_CARD.png");
        this.dataPath = dataPath;
        cgs = (CluedoGameState) gameState;
    }

    @Override
    public void drawDeck(Graphics2D g) {
        PartialObservableDeck<CluedoCard> deck = (PartialObservableDeck<CluedoCard>) component;
        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            int offset = Math.max((rect.width - itemWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize() - 1; i >= 0; i--) {
                if (i < deck.getSize()) {
                    CluedoCard card = deck.get(i);
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, itemWidth, itemHeight);
                    rects[i] = r;
                    drawComponent(g, r, card, deck.getVisibilityForPlayer(i, cgs.getCurrentPlayer()));
                }
            }
        }
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, CluedoCard card, boolean visible) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + card.toString() + ".png");
        CardView.drawCard(g, rect, card, cardFace, backOfCard, visible);
    }
}
