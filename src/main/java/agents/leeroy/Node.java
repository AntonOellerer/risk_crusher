package agents.leeroy;

import java.util.List;

public interface Node {
    List<Node> getSuccessors();

    boolean isLeafNode();

    int getId();
}
