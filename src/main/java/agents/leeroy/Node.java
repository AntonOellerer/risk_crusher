package agents.leeroy;

import java.util.List;
import java.util.Optional;

public interface Node {
    List<Node> getSuccessors();

    boolean isLeafNode();

    int getId();

    Optional<Node> getParent();

    int getVisitCount();

    void incrementVisitCount();

    double getWinScore();

    void incrementWinScore(double increment);

    int getPlayer();

    boolean isExpanded();
}
