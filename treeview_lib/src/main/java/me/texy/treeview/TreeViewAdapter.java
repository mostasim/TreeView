/*
 * Copyright 2016 - 2017 ShineM (Xinyuan)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under.
 */

package me.texy.treeview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

import me.texy.treeview.base.BaseNodeViewBinder;
import me.texy.treeview.base.BaseNodeViewFactory;
import me.texy.treeview.base.CheckableNodeViewBinder;
import me.texy.treeview.draggable.ItemTouchHelperAdapter;
import me.texy.treeview.helper.TreeHelper;

/**
 * Created by xinyuanzhong on 2017/4/21.
 * Modified by Mostasim Billah
 */

public class TreeViewAdapter extends RecyclerView.Adapter implements ItemTouchHelperAdapter {

    public static final String TAG = "__MB__DEBUG__";
    private Context context;

    private TreeNode root;

    private ArrayList<TreeNode> expandedNodeList;

    private BaseNodeViewFactory baseNodeViewFactory;

    private View EMPTY_PARAMETER;

    private TreeView treeView;

    TreeViewAdapter(Context context, TreeNode root,
                    @NonNull BaseNodeViewFactory baseNodeViewFactory) {
        this.context = context;
        this.root = root;
        this.baseNodeViewFactory = baseNodeViewFactory;

        this.EMPTY_PARAMETER = new View(context);
        this.expandedNodeList = new ArrayList<>();

        buildExpandedNodeList();
        Log.e(TAG, "Expanded list size :" + expandedNodeList.size());
    }

    public TreeNode getRoot() {
        return root;
    }

    public BaseNodeViewFactory getBaseNodeViewFactory() {
        return baseNodeViewFactory;
    }

    private void buildExpandedNodeList() {
        expandedNodeList.clear();

        for (TreeNode child : root.getChildren()) {
            insertNode(expandedNodeList, child);
        }
    }

    private void insertNode(List<TreeNode> nodeList, TreeNode treeNode) {
        nodeList.add(treeNode);

        if (!treeNode.hasChild()) {
            return;
        }
        if (treeNode.isExpanded()) {
            for (TreeNode child : treeNode.getChildren()) {
                insertNode(nodeList, child);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return expandedNodeList.get(position).getLevel();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int level) {

        if (level > 3) level = 3;

        View view = LayoutInflater.from(context).inflate(
                baseNodeViewFactory.getNodeViewBinder(EMPTY_PARAMETER, level).getLayoutId(),
                parent, false
        );

        BaseNodeViewBinder nodeViewBinder = baseNodeViewFactory.getNodeViewBinder(view, level);
        nodeViewBinder.setTreeView(treeView);
        return nodeViewBinder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final View nodeView = holder.itemView;
        final TreeNode treeNode = expandedNodeList.get(position);
        final BaseNodeViewBinder viewBinder = (BaseNodeViewBinder) holder;

        if (viewBinder.getToggleTriggerViewId() != 0) {
            View triggerToggleView = nodeView.findViewById(viewBinder.getToggleTriggerViewId());

            if (triggerToggleView != null) {
                triggerToggleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onNodeToggled(treeNode);
                        viewBinder.onNodeToggled(treeNode, treeNode.isExpanded());
                    }
                });
            }
        } else if (treeNode.isItemClickEnable()) {
            nodeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNodeToggled(treeNode);
                    viewBinder.onNodeToggled(treeNode, treeNode.isExpanded());
                }
            });
        }

        if (viewBinder instanceof CheckableNodeViewBinder) {
            setupCheckableItem(nodeView, treeNode, (CheckableNodeViewBinder) viewBinder);
        }

        viewBinder.bindView(treeNode);
        viewBinder.setTreeNode(treeNode);
    }

    private void setupCheckableItem(View nodeView,
                                    final TreeNode treeNode,
                                    final CheckableNodeViewBinder viewBinder) {
        final View view = nodeView.findViewById(viewBinder.getCheckableViewId());

        if (view instanceof CheckBox) {
            final CheckBox checkableView = (CheckBox) view;
            checkableView.setChecked(treeNode.isSelected());

            checkableView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = checkableView.isChecked();
                    selectNode(checked, treeNode);
                    viewBinder.onNodeSelectedChanged(treeNode, checked);
                }
            });
        } else {
            throw new ClassCastException("The getCheckableViewId() " +
                    "must return a CheckBox's id");
        }
    }

    void selectNode(boolean checked, TreeNode treeNode) {
        treeNode.setSelected(checked);

        selectChildren(treeNode, checked);
        selectParentIfNeed(treeNode, checked);
    }

    private void selectChildren(TreeNode treeNode, boolean checked) {
        List<TreeNode> impactedChildren = TreeHelper.selectNodeAndChild(treeNode, checked);
        int index = expandedNodeList.indexOf(treeNode);
        if (index != -1 && impactedChildren.size() > 0) {
            notifyItemRangeChanged(index, impactedChildren.size() + 1);
        }
    }

    private void selectParentIfNeed(TreeNode treeNode, boolean checked) {
        List<TreeNode> impactedParents = TreeHelper.selectParentIfNeedWhenNodeSelected(treeNode, checked);
        if (impactedParents.size() > 0) {
            for (TreeNode parent : impactedParents) {
                int position = expandedNodeList.indexOf(parent);
                if (position != -1) notifyItemChanged(position);
            }
        }
    }

    private void onNodeToggled(TreeNode treeNode) {
        treeNode.setExpanded(!treeNode.isExpanded());

        if (treeNode.isExpanded()) {
            expandNode(treeNode);
        } else {
            collapseNode(treeNode);
        }
    }

    @Override
    public int getItemCount() {
        return expandedNodeList == null ? 0 : expandedNodeList.size();
    }

    /**
     * Refresh all,this operation is only used for refreshing list when a large of nodes have
     * changed value or structure because it take much calculation.
     */
    void refreshView() {
        buildExpandedNodeList();
        notifyDataSetChanged();
    }

    // Insert a node list after index.
    private void insertNodesAtIndex(int index, List<TreeNode> additionNodes) {
        if (index < 0 || index > expandedNodeList.size() - 1 || additionNodes == null) {
            return;
        }
        expandedNodeList.addAll(index + 1, additionNodes);
        notifyItemRangeInserted(index + 1, additionNodes.size());
        Log.e(TAG, "Expanded list size :" + expandedNodeList.size());
    }

    //Remove a node list after index.
    private void removeNodesAtIndex(int index, List<TreeNode> removedNodes) {
        if (index < 0 || index > expandedNodeList.size() - 1 || removedNodes == null) {
            return;
        }
        expandedNodeList.removeAll(removedNodes);
        notifyItemRangeRemoved(index + 1, removedNodes.size());
        Log.e(TAG, "Expanded list size :" + expandedNodeList.size());
    }

    /**
     * Expand node. This operation will keep the structure of children(not expand children)
     */
    void expandNode(TreeNode treeNode) {
        if (treeNode == null) {
            return;
        }
        List<TreeNode> additionNodes = TreeHelper.expandNode(treeNode, false);
        int index = expandedNodeList.indexOf(treeNode);

        insertNodesAtIndex(index, additionNodes);
        Log.e(TAG, "Expanded list size :" + expandedNodeList.size());
    }


    /**
     * Collapse node. This operation will keep the structure of children(not collapse children)
     */
    void collapseNode(TreeNode treeNode) {
        if (treeNode == null) {
            return;
        }
        List<TreeNode> removedNodes = TreeHelper.collapseNode(treeNode, false);
        int index = expandedNodeList.indexOf(treeNode);

        removeNodesAtIndex(index, removedNodes);
        Log.e(TAG, "Expanded list size :" + expandedNodeList.size());
    }

    /**
     * Delete a node from list.This operation will also delete its children.
     */
    void deleteNode(TreeNode node) {
        if (node == null || node.getParent() == null) {
            return;
        }
        List<TreeNode> allNodes = TreeHelper.getAllNodes(root);
        if (allNodes.indexOf(node) != -1) {
            node.getParent().removeChild(node);
        }

        //remove children form list before delete
        collapseNode(node);

        int index = expandedNodeList.indexOf(node);
        if (index != -1) {
            expandedNodeList.remove(node);
        }
        notifyItemRemoved(index);
        Log.e(TAG, "Expanded list size :" + expandedNodeList.size());
    }

    void setTreeView(TreeView treeView) {
        this.treeView = treeView;

    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {

        Log.e(TAG, "Move->from " + fromPosition + " to " + toPosition);

//        changeOrderOfLevel(expandedNodeList.get(fromPosition), expandedNodeList.get(toPosition));
//
//        expandedNodeList.get(toPosition).addChild(expandedNodeList.get(fromPosition));

//        if (fromPosition < toPosition) {
//            for (int i = fromPosition; i < toPosition; i++) {
//                Collections.swap(expandedNodeList, i, i + 1);
//            }
//        } else {
//            for (int i = fromPosition; i > toPosition; i--) {
//                Collections.swap(expandedNodeList, i, i - 1);
//            }
//        }

        // notifyItemMoved(fromPosition, toPosition);
//        notifyDataSetChanged();
        buildRootNodeFromExpandedList();
    }

    private void changeOrderOfLevel(TreeNode fromPosition, TreeNode toPosition) {

        int currentLevel = fromPosition.getLevel();
        int modifiedLevel = toPosition.getLevel() + 1;
        fromPosition.setLevel(modifiedLevel);
        if (fromPosition.hasChild()) {
            for (TreeNode treeNode : fromPosition.getChildren()) {
                changeOrderOfLevel(treeNode, treeNode.getParent());
            }
        } else {
            return;
        }
    }


    private void buildRootNodeFromExpandedList() {
        TreeNode node = TreeNode.root();
        for (TreeNode child : expandedNodeList) {
            if (child.getParent() == root) {
                node.addChild(child);
            }
        }
        root = node;
    }

    @Override
    public void onItemDismiss(int position) {
        Log.e(TAG, "Dismiss->from " + position);
    }
}
