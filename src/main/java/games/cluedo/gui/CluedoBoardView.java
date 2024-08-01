package games.cluedo.gui;

import core.AbstractGameState;
import core.components.BoardNode;
import core.components.GraphBoard;
import games.cluedo.CluedoGameState;
import games.cluedo.CluedoConstants;
import games.cluedo.CluedoParameters;
import gui.views.ComponentView;
import utilities.ImageIO;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.nameHash;
import static games.pandemic.PandemicConstants.edgeHash;

public class CluedoBoardView extends ComponentView {
    GraphBoard graphBoard;
    CluedoGameState cgs;

    private final Image background;

    double scale = 0.5;
    int nodeSize = (int) (scale * 30);
    int playerPawnSize = (int)(scale * 20);

    public CluedoBoardView(AbstractGameState gameState, GraphBoard graphBoard, int width, int height) {
        super(graphBoard, width, height);
        cgs = (CluedoGameState) gameState;
        this.graphBoard = cgs.getGameBoard();
        String dataPath = ((CluedoParameters) cgs.getGameParameters()).getDataPath();
        this.background = ImageIO.GetInstance().getImage(dataPath + "BOARD.png");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        drawBoard(g2);
    }

    private void drawBoard(Graphics2D g) {
        // Background
        int w = background.getWidth(null);
        int h = background.getHeight(null);
        g.drawImage(background, 0, 0, (int) (w*scale), (int) (h*scale), null);

        // Graph
        Collection<BoardNode> bList = graphBoard.getBoardNodes();
        for (BoardNode b: bList) {
            Vector2D poss = CluedoConstants.roomNameToCoordinate.get(b.getComponentName());
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));

            // Draw node
            g.setColor(Color.lightGray);
            g.fillOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);

            // Draw edges
//            HashSet<BoardNode> neighbours = b.getNeighbours();
//            for (BoardNode b2: neighbours) {
//                Vector2D poss2 = CluedoConstants.roomNameToCoordinate.get(b2.getComponentName());
//                Vector2D pos2 = new Vector2D((int)(poss2.getX()*scale), (int)(poss2.getY()*scale));
//                g.setColor(Color.white);
//                g.drawLine(pos.getX(), pos.getY(), pos2.getX(), pos2.getY());
//            }

            // Add name label
            g.setColor(Color.black);
            g.setFont(new Font("Inter", Font.PLAIN, 10));
            g.drawString(b.getComponentName(), pos.getX(), pos.getY() - nodeSize/2 - playerPawnSize);

            // Draw characters
            List<Integer> characters = new ArrayList<>();
            for (int i=0; i<6; i++) {
                if (Objects.equals(cgs.getCharacterLocations().get(i), b.getComponentName())) {
                    characters.add(i);
                }
            }
            for (int i=0; i<characters.size(); i++) {
                // Position
                int x = pos.getX() + characters.size() * playerPawnSize / 2 - i * playerPawnSize - playerPawnSize;
                int y = pos.getY() - nodeSize /2 + playerPawnSize /4;

                // Color
                g.setColor(CluedoConstants.characterIndexToColor.get(characters.get(i)));
                g.fillOval(x, y, playerPawnSize, playerPawnSize);
                g.setColor(Color.black);
                g.drawOval(x, y, playerPawnSize, playerPawnSize);
            }


        }

    }

}
