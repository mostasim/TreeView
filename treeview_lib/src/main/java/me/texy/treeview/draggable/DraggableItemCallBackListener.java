package me.texy.treeview.draggable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeViewAdapter;
import me.texy.treeview.base.BaseNodeViewBinder;
import me.texy.treeview.helper.TreeHelper;


public class DraggableItemCallBackListener extends ItemTouchHelper.Callback {
    public static final String TAG = "__MB_DEBUG__" + DraggableItemCallBackListener.class.getSimpleName();

    private ItemTouchHelperAdapter mItemTouchHelperAdapter;
    private Bitmap mDraggableImage = null;
    private Paint mPaint;

    private int lastFromIndex = -1;
    private int lastToIndex = -1;

    public DraggableItemCallBackListener(ItemTouchHelperAdapter mItemTouchHelperAdapter) {
        this.mItemTouchHelperAdapter = mItemTouchHelperAdapter;
        mPaint = new Paint();
    }

    /**
     * @param recyclerView
     * @param viewHolder
     * @return flags
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlag = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlag, swipeFlag);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//        mItemTouchHelperAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        Log.e(TAG, "move");
        return true;
    }

    @Override
    public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
//        mItemTouchHelperAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        Log.e(TAG, "moved");
        lastFromIndex = viewHolder.getAdapterPosition();
        lastToIndex = target.getAdapterPosition();

    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mItemTouchHelperAdapter.onItemDismiss(viewHolder.getAdapterPosition());

    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        mDraggableImage = null;
        Log.e(TAG, "Selected Change ");
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

        mDraggableImage = null;
        Log.e(TAG, "Clear View " + "last From" + lastFromIndex + " To " + lastToIndex);
        mItemTouchHelperAdapter.onItemMove(lastFromIndex, lastToIndex);
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (isCurrentlyActive /*|| true*/) {

            float y = viewHolder.itemView.getY();
            float x = viewHolder.itemView.getX();

            if (mDraggableImage == null)
                mDraggableImage = getRecyclerViewScreenshot(recyclerView, viewHolder.getAdapterPosition());

            canvas.drawBitmap(mDraggableImage, x, y, mPaint);
            Log.e(TAG, "OnChildDraw Finish");
        }
        super.onChildDrawOver(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }


    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    private Bitmap getRecyclerViewScreenshot(RecyclerView recyclerView, int adapterPosition) {

        Paint paint = new Paint();

        BaseNodeViewBinder baseNodeViewHolder = (BaseNodeViewBinder) recyclerView.getAdapter().createViewHolder(recyclerView, 0);
        TreeViewAdapter adapter = (TreeViewAdapter) recyclerView.getAdapter();
        adapter.onBindViewHolder(baseNodeViewHolder, adapterPosition);

        TreeNode treeNode = baseNodeViewHolder.getTreeNode();

        BaseNodeViewBinder holder = (BaseNodeViewBinder) recyclerView.getAdapter().createViewHolder(recyclerView, treeNode.getLevel());
        adapter.onBindViewHolder(holder, adapterPosition);

        //todo initialize all viewholder here to avoid null
        ArrayList<BaseNodeViewBinder> nodeViewBinderArrayList = new ArrayList<>();
        int viewTypes = adapter.getBaseNodeViewFactory().levelTypes.size();
        for (Integer i : adapter.getBaseNodeViewFactory().levelTypes) {
            BaseNodeViewBinder nodeViewBinder = (BaseNodeViewBinder) recyclerView.getAdapter().createViewHolder(recyclerView, i);
            nodeViewBinderArrayList.add(nodeViewBinder);
        }
        Log.e(TAG, "ViewTypes " + viewTypes);
        Log.e(TAG, "Childs Node: Item pos:" + adapterPosition + " count " +
//                rootNode.getChildren().size() +
//                "\nlevel " + rootNode.getLevel() +
                "\nlevel child " + treeNode.getLevel() +
                "\nparent count " + treeNode.getParent().getChildren().size() +
                "\nchild pos " + treeNode.getIndex() +
                "\nhasChild->" + treeNode.hasChild() +
                "\nExpanded Childs: " + TreeHelper.expandedNode(treeNode, true).size());


        List<TreeNode> expandedNode = TreeHelper.expandedNode(treeNode, true);
        int newBitmapSize = expandedNode.size() + 1;

        holder.itemView.measure(View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        holder.itemView.layout(0, 0, holder.itemView.getMeasuredWidth(), holder.itemView.getMeasuredHeight());

        Bitmap bigBitmap = Bitmap.createBitmap(recyclerView.getMeasuredWidth(), holder.itemView.getMeasuredHeight() * newBitmapSize,
                Bitmap.Config.ARGB_8888);

        Canvas bigCanvas = new Canvas(bigBitmap);
        bigCanvas.drawColor(Color.TRANSPARENT);

        int iHeight = 0;

        holder.itemView.setDrawingCacheEnabled(true);
        holder.itemView.buildDrawingCache();
        bigCanvas.drawBitmap(holder.itemView.getDrawingCache(), 0f, iHeight, paint);
        holder.itemView.setDrawingCacheEnabled(false);
        holder.itemView.destroyDrawingCache();

        iHeight += holder.itemView.getMeasuredHeight();


        if (treeNode.hasChild() && treeNode.isExpanded()) {


            int lastPositionOfChildViews = adapterPosition + newBitmapSize - 1;
            Log.e(TAG, "lastPositionOfChildViews " + lastPositionOfChildViews);
            BaseNodeViewBinder childViewHolder = (BaseNodeViewBinder) recyclerView.findViewHolderForAdapterPosition(adapterPosition + 1);
            //todo
            for (int i = adapterPosition + 1; i <= lastPositionOfChildViews; i++) {
                childViewHolder = (BaseNodeViewBinder) recyclerView.findViewHolderForAdapterPosition(i);

                if (childViewHolder == null) {
                    Log.e(TAG, "Holder NULL " + i +
                            "TYPE : " + recyclerView.getAdapter().getItemViewType(i));
                    childViewHolder = nodeViewBinderArrayList.get((recyclerView.getAdapter().getItemViewType(i)) > 3 ? 3 : recyclerView.getAdapter().getItemViewType(i));
                } else {
                    Log.e(TAG, "Holder NOT NULL " + i);
                }
                adapter.onBindViewHolder(childViewHolder, i);
                childViewHolder.itemView.setDrawingCacheEnabled(true);
                childViewHolder.itemView.buildDrawingCache();
                Bitmap drawingCache = childViewHolder.itemView.getDrawingCache();

                if (drawingCache == null)
                    Log.e(TAG, "Cache NULL");
                else
                    bigCanvas.drawBitmap(drawingCache, 0f, iHeight, paint);

                iHeight += childViewHolder.itemView.getMeasuredHeight();
                childViewHolder.itemView.setDrawingCacheEnabled(false);
                childViewHolder.itemView.destroyDrawingCache();
            }
        }
        return bigBitmap;
    }
}
