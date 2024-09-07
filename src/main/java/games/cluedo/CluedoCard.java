package games.cluedo;

import core.components.Card;

public class CluedoCard extends Card {

    public Object cardName;

    public CluedoCard(CluedoConstants.Character cardName) {
        super(cardName.toString());
        this.cardName = cardName;
    }

    public CluedoCard(CluedoConstants.Weapon cardName) {
        super(cardName.toString());
        this.cardName = cardName;
    }
    public CluedoCard(CluedoConstants.Room cardName) {
        super(cardName.toString());
        this.cardName = cardName;
    }

    private CluedoCard(Object cardType, int ID) {
        super(cardType.toString(), ID);
        this.cardName = cardType;
    }

    @Override
    public Card copy() {
        return new CluedoCard(cardName, componentID);
    }

    @Override
    public String toString() {
        return cardName.toString();
    }

}
