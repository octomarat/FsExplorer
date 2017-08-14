package fs.explorer.providers.dirtree;

import fs.explorer.providers.utils.loading.TreeNodeLoader;

import java.util.List;
import java.util.function.Consumer;

public interface AsyncTreeDataProvider {
    void getTopNode(Consumer<TreeNodeData> onComplete);

    TreeNodeLoader getNodesFor(
            TreeNodeData node,
            Consumer<List<TreeNodeData>> onComplete,
            Consumer<String> onFail
    );
}