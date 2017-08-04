package fs.explorer.controllers;

import fs.explorer.providers.dirtree.path.PathContainerUtils;
import fs.explorer.providers.dirtree.TreeDataProvider;
import fs.explorer.providers.dirtree.TreeNodeData;
import fs.explorer.models.dirtree.DirTreeModel;
import fs.explorer.models.dirtree.ExtTreeNodeData;
import fs.explorer.views.DirTreePane;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.function.Consumer;

public class DirTreeController {
    private final DirTreePane dirTreePane;
    private final DirTreeModel dirTreeModel;
    private final PreviewController previewController;
    private final StatusBarController statusBarController;
    private TreeDataProvider treeDataProvider;

    private static final String DATA_PROVIDER_ERROR = "Failed to load data";

    public DirTreeController(
            DirTreePane dirTreePane,
            DirTreeModel dirTreeModel,
            PreviewController previewController,
            StatusBarController statusBarController,
            TreeDataProvider defaultDataProvider
    ) {
        this.dirTreePane = dirTreePane;
        this.dirTreeModel = dirTreeModel;
        this.previewController = previewController;
        this.statusBarController = statusBarController;
        this.treeDataProvider = defaultDataProvider;
    }

    public TreeDataProvider getTreeDataProvider() { return treeDataProvider; }

    public void resetDataProvider(TreeDataProvider treeDataProvider) {
        this.treeDataProvider = treeDataProvider;
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        dirTreeModel.removeAllChildren(root);
        treeDataProvider.getTopNode(nodeData -> {
            dirTreeModel.addNullDirChild(root, nodeData);
            dirTreePane.expandPath(new TreePath(root.getPath()));
        });
    }

    public void handleTreeSelection(
            TreeSelectionEvent e, DefaultMutableTreeNode lastSelectedNode) {
        if (lastSelectedNode == null) {
            return;
        }
        ExtTreeNodeData extNodeData = dirTreeModel.getExtNodeData(lastSelectedNode);
        if(extNodeData.getType() == ExtTreeNodeData.Type.NORMAL) {
            previewController.updatePreview(extNodeData.getNodeData());
        }
    }

    public void handleTreeExpansion(TreeExpansionEvent event) {
        TreePath treePath = event.getPath();
        if(treePath == null) {
            return;
        }
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if(node == null) {
            return;
        }
        ExtTreeNodeData extNodeData = dirTreeModel.getExtNodeData(node);
        if(extNodeData.getType() == ExtTreeNodeData.Type.NORMAL &&
                extNodeData.getStatus() == ExtTreeNodeData.Status.NULL) {
            loadContents(node, extNodeData);
        }
    }

    private void loadContents(DefaultMutableTreeNode node, ExtTreeNodeData extNodeData) {
        extNodeData.setStatus(ExtTreeNodeData.Status.LOADING);
        treeDataProvider.getNodesFor(
                extNodeData.getNodeData(),
                contentsInserter(node, extNodeData),
                loadContentsErrorHandler(node, extNodeData)
        );
    }

    private Consumer<List<TreeNodeData>> contentsInserter(
            DefaultMutableTreeNode node, ExtTreeNodeData extNodeData) {
        return contents -> {
            if(!dirTreeModel.containsNode(node)) {
                return;
            }
            dirTreeModel.removeAllChildren(node);
            if(contents.isEmpty()) {
                dirTreeModel.addFakeChild(node, "<empty>");
            } else {
                for(TreeNodeData nodeData : contents) {
                    if(PathContainerUtils.isDirectoryPath(nodeData.getPath())) {
                        dirTreeModel.addNullDirChild(node, nodeData);
                    } else {
                        dirTreeModel.addFileChild(node, nodeData);
                    }
                }
            }
            dirTreePane.expandPath(new TreePath(node.getPath()));
            extNodeData.setStatus(ExtTreeNodeData.Status.LOADED);
        };
    }

    private Consumer<String> loadContentsErrorHandler(
            DefaultMutableTreeNode node, ExtTreeNodeData extNodeData) {
        return errorMessage -> {
            if(!dirTreeModel.containsNode(node)) {
                return;
            }
            dirTreeModel.removeAllChildren(node);
            dirTreeModel.addFakeChild(node, "<error>");
            statusBarController.setErrorMessage(DATA_PROVIDER_ERROR, errorMessage);
            dirTreePane.expandPath(new TreePath(node.getPath()));
            extNodeData.setStatus(ExtTreeNodeData.Status.LOADED);
        };
    }
}
