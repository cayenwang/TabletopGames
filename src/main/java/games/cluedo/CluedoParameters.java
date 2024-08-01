package games.cluedo;

import core.AbstractParameters;

public class CluedoParameters extends AbstractParameters {

    String dataPath = "data/cluedo/";

    public String getDataPath() { return dataPath; }

    @Override
    protected AbstractParameters _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
