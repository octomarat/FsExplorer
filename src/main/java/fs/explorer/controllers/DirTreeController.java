package fs.explorer.controllers;

import fs.explorer.models.dirtree.DirTreeModel.DirTreeNode;
import fs.explorer.providers.dirtree.TreeDataProvider;
import fs.explorer.providers.dirtree.TreeNodeData;
import fs.explorer.models.dirtree.DirTreeModel;
import fs.explorer.models.dirtree.ExtTreeNodeData;
import fs.explorer.providers.dirtree.path.TargetType;
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

    private DirTreeNode lastSelectedNode;

    private static final String DATA_PROVIDER_ERROR = "Failed to load data";
    private static final String INTERNAL_ERROR = "internal error";

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

    public DirTreeController(
            DirTreePane dirTreePane,
            DirTreeModel dirTreeModel,
            PreviewController previewController,
            StatusBarController statusBarController
    ) {
        this(dirTreePane, dirTreeModel, previewController, statusBarController, null);
    }

    public TreeDataProvider getTreeDataProvider() {
        return treeDataProvider;
    }

    public void resetDataProvider(TreeDataProvider treeDataProvider) {
        if (treeDataProvider == null) {
            statusBarController.setErrorMessage(DATA_PROVIDER_ERROR, INTERNAL_ERROR);
            return;
        }
        this.treeDataProvider = treeDataProvider;
        DirTreeNode root = dirTreeModel.getRoot();
        dirTreeModel.removeAllChildren(root);
        this.treeDataProvider.getTopNode(nodeData -> {
            dirTreeModel.addNullDirChild(root, nodeData);
            dirTreePane.expandPath(root.getTreePath());
        });
    }

    public void handleTreeSelection(
            TreeSelectionEvent e, DefaultMutableTreeNode lastSelectedInnerNode) {
        if (lastSelectedInnerNode == null) {
            return;
        }
        this.lastSelectedNode = dirTreeModel.fromInnerNode(lastSelectedInnerNode);
        ExtTreeNodeData extNodeData = lastSelectedNode.getExtTreeNodeData();
        if (extNodeData.getType() == ExtTreeNodeData.Type.NORMAL) {
            previewController.updatePreview(extNodeData.getNodeData());
        }
    }

    public void handleTreeExpansion(TreeExpansionEvent event) {
        TreePath treePath = event.getPath();
        if (treePath == null) {
            return;
        }
        DefaultMutableTreeNode innerNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if (innerNode == null) {
            return;
        }
        DirTreeNode node = dirTreeModel.fromInnerNode(innerNode);
        ExtTreeNodeData extNodeData = node.getExtTreeNodeData();
        if (extNodeData.getType() == ExtTreeNodeData.Type.NORMAL &&
                extNodeData.getStatus() == ExtTreeNodeData.Status.NULL) {
            reloadContents(node);
        }
    }

    public void reloadLastSelectedNode() {
        if (lastSelectedNode == null) {
            return;
        }
        ExtTreeNodeData extNodeData = lastSelectedNode.getExtTreeNodeData();
        ExtTreeNodeData.Status status = extNodeData.getStatus();
        boolean reloadNeeded = extNodeData.getType() == ExtTreeNodeData.Type.NORMAL &&
                (status == ExtTreeNodeData.Status.NULL || status == ExtTreeNodeData.Status.LOADED);
        if (reloadNeeded) {
            TargetType targetType = extNodeData.getNodeData().getPathTargetType();
            // TODO support reload for zip archives
            if (targetType == TargetType.DIRECTORY) {
                reloadContents(lastSelectedNode);
            } else if (targetType == TargetType.FILE) {
                previewController.updatePreview(extNodeData.getNodeData());
            }
        }
    }

    private void reloadContents(DirTreeNode node) {
        if (treeDataProvider == null) {
            statusBarController.setErrorMessage(DATA_PROVIDER_ERROR, INTERNAL_ERROR);
            return;
        }
        ExtTreeNodeData extNodeData = node.getExtTreeNodeData();
        extNodeData.setStatus(ExtTreeNodeData.Status.LOADING);
        treeDataProvider.getNodesFor(
                extNodeData.getNodeData(),
                contentsInserter(node, extNodeData),
                loadContentsErrorHandler(node, extNodeData)
        );
    }

    private Consumer<List<TreeNodeData>> contentsInserter(
            DirTreeNode node, ExtTreeNodeData extNodeData) {
        return contents -> {
            if (!dirTreeModel.containsNode(node)) {
                return;
            }
            dirTreeModel.removeAllChildren(node);
            if (contents.isEmpty()) {
                dirTreeModel.addFakeChild(node, "<empty>");
            } else {
                for (TreeNodeData nodeData : contents) {
                    TargetType targetType = nodeData.getPathTargetType();
                    if (targetType == TargetType.DIRECTORY ||
                            targetType == TargetType.ZIP_ARCHIVE) {
                        dirTreeModel.addNullDirChild(node, nodeData);
                    } else {
                        dirTreeModel.addFileChild(node, nodeData);
                    }
                }
            }
            dirTreePane.expandPath(node.getTreePath());
            extNodeData.setStatus(ExtTreeNodeData.Status.LOADED);
        };
    }

    private Consumer<String> loadContentsErrorHandler(
            DirTreeNode node, ExtTreeNodeData extNodeData) {
        return errorMessage -> {
            if (!dirTreeModel.containsNode(node)) {
                return;
            }
            dirTreeModel.removeAllChildren(node);
            dirTreeModel.addFakeChild(node, "<error>");
            statusBarController.setErrorMessage(DATA_PROVIDER_ERROR, errorMessage);
            dirTreePane.expandPath(node.getTreePath());
            extNodeData.setStatus(ExtTreeNodeData.Status.LOADED);
        };
    }
}
