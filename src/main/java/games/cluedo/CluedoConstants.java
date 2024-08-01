package games.cluedo;

import utilities.Hash;
import utilities.Vector2D;

import java.awt.*;
import java.util.HashMap;

public class CluedoConstants {

    public enum Character {
        MISS_SCARLETT,
        COL_MUSTARD,
        DR_ORCHID,
        REV_GREEN,
        MRS_PEACOCK,
        PROF_PLUM;
    }

    public enum Weapon {
        ROPE,
        DAGGER,
        WRENCH,
        REVOLVER,
        CANDLESTICK,
        LEAD_PIPE;
    }

    public enum Room {
        KITCHEN,
        DINING_ROOM,
        LOUNGE,
        BATHROOM,
        STUDY,
        LIBRARY,
        BILLIARD_ROOM,
        CONSERVATORY,
        BALLROOM;
    }

    // TODO put these hash maps into a JSON data file
    public static HashMap<String, Vector2D> roomNameToCoordinate = new HashMap<>(){{
        put("START", new Vector2D(300,1000));
        put("KITCHEN", new Vector2D(100,120));
        put("DINING_ROOM", new Vector2D(100,320));
        put("LOUNGE", new Vector2D(100,550));
        put("BATHROOM", new Vector2D(300,550));
        put("STUDY", new Vector2D(500,550));
        put("LIBRARY", new Vector2D(500,410));
        put("BILLIARD_ROOM", new Vector2D(500,265));
        put("CONSERVATORY", new Vector2D(500,120));
        put("BALLROOM", new Vector2D(300,120));
    }};

    public static HashMap<Integer, Color> characterIndexToColor = new HashMap<>(){{
       put(0, new Color(204, 0, 0));
       put(1, new Color(191, 144, 0));
       put(2, new Color(194,123, 160));
       put(3, new Color(106,168,79));
       put(4, new Color(60,120 ,216));
       put(5, new Color(116, 27, 71));

    }};

}
