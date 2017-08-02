package fs.explorer.models.dirtree;

import fs.explorer.providers.dirtree.TreeNodeData;
import org.junit.Before;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static fs.explorer.models.dirtree.ExtTreeNodeData.*;

public class DirTreeModelTest {
    private DirTreeModel dirTreeModel;

    @Before
    public void setUp() {
        dirTreeModel = new DirTreeModel();
    }

    @Test
    public void createdWithNonNullInnerTreeModel() {
        assertNotNull(dirTreeModel.getInnerTreeModel());
    }

    @Test
    public void createdWithValidRoot() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        assertNotNull(root);
        checkNode(dirTreeModel, root, Type.FAKE, Status.LOADED, null);
        assertEquals(Collections.emptyList(), dirTreeModel.getChildren(root));
    }

    @Test
    public void addsNullDirChild() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        dirTreeModel.addNullDirChild(root, nodeData("dir"));

        List<DefaultMutableTreeNode> children = dirTreeModel.getChildren(root);
        assertEquals(1, children.size());
        DefaultMutableTreeNode node = children.get(0);
        checkNode(dirTreeModel, node, Type.NORMAL, Status.NULL, "dir");

        children = dirTreeModel.getChildren(node);
        assertEquals(1, children.size());
        node = children.get(0);
        checkNode(dirTreeModel, node, Type.FAKE, Status.LOADED, null);
        assertEquals(Collections.emptyList(), dirTreeModel.getChildren(node));
    }

    @Test
    public void addsFileChild() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        dirTreeModel.addFileChild(root, nodeData("file"));

        List<DefaultMutableTreeNode> children = dirTreeModel.getChildren(root);
        assertEquals(1, children.size());
        checkNode(dirTreeModel, children.get(0), Type.NORMAL, Status.LOADED, "file");
        assertEquals(Collections.emptyList(), dirTreeModel.getChildren(children.get(0)));
    }

    @Test
    public void addsFakeChild() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        dirTreeModel.addFakeChild(root, "fake");

        List<DefaultMutableTreeNode> children = dirTreeModel.getChildren(root);
        assertEquals(1, children.size());
        checkNode(dirTreeModel, children.get(0), Type.FAKE, Status.LOADED, "fake");
        assertEquals(Collections.emptyList(), dirTreeModel.getChildren(children.get(0)));
    }

    @Test
    public void addsMultipleChildren() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        DefaultMutableTreeNode dir1 = dirTreeModel.addNullDirChild(root, nodeData("dir1"));
        dirTreeModel.addFileChild(dir1, nodeData("file1"));
        dirTreeModel.addFileChild(dir1, nodeData("file2"));
        DefaultMutableTreeNode dir2 = dirTreeModel.addNullDirChild(root, nodeData("dir2"));
        dirTreeModel.addNullDirChild(dir2, nodeData("dir3"));

        List<DefaultMutableTreeNode> children = dirTreeModel.getChildren(root);
        assertEquals(2, children.size());
        assertEquals(3, dirTreeModel.getChildren(children.get(0)).size());
        assertEquals(2, dirTreeModel.getChildren(children.get(1)).size());
    }

    // TODO test addFileChild to addFileChild result

    @Test
    public void removesAllChildren() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        dirTreeModel.addNullDirChild(root, nodeData("dir1"));
        dirTreeModel.addNullDirChild(root, nodeData("dir2"));
        assertEquals(2, dirTreeModel.getChildren(root).size());
        dirTreeModel.removeAllChildren(root);
        assertEquals(0, dirTreeModel.getChildren(root).size());
    }

    @Test
    public void containsRoot() {
        assertTrue(dirTreeModel.containsNode(dirTreeModel.getRoot()));
    }

    @Test
    public void containsAddedChild1() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        DefaultMutableTreeNode fileNode = dirTreeModel.addFileChild(root, nodeData("file"));
        assertTrue(dirTreeModel.containsNode(fileNode));
    }

    @Test
    public void containsAddedChild2() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        DefaultMutableTreeNode node1 = dirTreeModel.addNullDirChild(root, nodeData("dir1"));
        DefaultMutableTreeNode node2 = dirTreeModel.addNullDirChild(node1, nodeData("dir2"));
        assertTrue(dirTreeModel.containsNode(node2));
    }

    @Test
    public void doesNotContainRemovedChild1() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        DefaultMutableTreeNode fileNode = dirTreeModel.addFileChild(root, nodeData("file"));
        dirTreeModel.removeAllChildren(root);
        assertFalse(dirTreeModel.containsNode(fileNode));
    }

    @Test
    public void doesNotContainRemovedChild2() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        DefaultMutableTreeNode node1 = dirTreeModel.addNullDirChild(root, nodeData("dir1"));
        DefaultMutableTreeNode node2 = dirTreeModel.addNullDirChild(node1, nodeData("dir2"));
        dirTreeModel.removeAllChildren(root);
        assertFalse(dirTreeModel.containsNode(node1));
        assertFalse(dirTreeModel.containsNode(node2));
    }

    @Test
    public void doesNotContainRemovedChild3() {
        DefaultMutableTreeNode root = dirTreeModel.getRoot();
        DefaultMutableTreeNode node1 = dirTreeModel.addNullDirChild(root, nodeData("dir1"));
        DefaultMutableTreeNode node2 = dirTreeModel.addNullDirChild(node1, nodeData("dir2"));
        dirTreeModel.removeAllChildren(node1);
        assertTrue(dirTreeModel.containsNode(node1));
        assertFalse(dirTreeModel.containsNode(node2));
    }

    private TreeNodeData nodeData(String label) {
        TreeNodeData data = mock(TreeNodeData.class);
        when(data.toString()).thenReturn(label);
        return data;
    }

    private void checkNode(
            DirTreeModel dirTreeModel,
            DefaultMutableTreeNode node,
            Type type,
            Status status,
            String label
    ) {
        ExtTreeNodeData extNodeData = dirTreeModel.getExtNodeData(node);
        assertEquals(type, extNodeData.getType());
        assertEquals(status, extNodeData.getStatus());
        if(label != null) {
            assertEquals(label, extNodeData.toString());
        }
    }
}