package fs.explorer.datasource;

import java.util.List;
import java.util.function.Consumer;

public interface TreeDataProvider {
    void getTopNode(Consumer<TreeNodeData> onComplete);
    void getNodesFor(TreeNodeData node, Consumer<List<TreeNodeData>> onComplete);
}
