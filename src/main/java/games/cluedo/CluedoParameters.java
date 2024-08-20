package games.cluedo;

import core.AbstractParameters;

public class CluedoParameters extends AbstractParameters {

    String dataPath = "data/cluedo/";
    Boolean chooseCharacters = false;

    public String getDataPath() { return dataPath; }
    public Boolean getChooseCharacters() { return chooseCharacters; }

    @Override
    protected AbstractParameters _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
