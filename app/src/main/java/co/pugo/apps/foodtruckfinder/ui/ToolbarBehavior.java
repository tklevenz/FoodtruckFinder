package co.pugo.apps.foodtruckfinder.ui;

import android.content.Context;

import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by tobias on 1.11.2017.
 * https://stackoverflow.com/questions/32398500/toolbar-in-appbarlayout-is-scrollable-although-recyclerview-has-not-enough-conte
 */

public class ToolbarBehavior extends AppBarLayout.Behavior {
  private boolean scrollableRecyclerView = false;
  private int count;

  public ToolbarBehavior() {
  }

  public ToolbarBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
    return scrollableRecyclerView && super.onInterceptTouchEvent(parent, child, ev);
  }

  @Override
  public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes) {
    updatedScrollable(directTargetChild);
    return scrollableRecyclerView && super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes);
  }

  @Override
  public boolean onNestedFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY, boolean consumed) {
    return scrollableRecyclerView && super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
  }

  private void updatedScrollable(View directTargetChild) {
    if (directTargetChild instanceof RecyclerView) {
      RecyclerView recyclerView = (RecyclerView) directTargetChild;
      RecyclerView.Adapter adapter = recyclerView.getAdapter();
      if (adapter != null) {
        if (adapter.getItemCount() != count) {
          scrollableRecyclerView = false;
          count = adapter.getItemCount();
          RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
          if (layoutManager != null) {
            int lastVisibleItem = 0;
            if (layoutManager instanceof LinearLayoutManager) {
              LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
              lastVisibleItem = Math.abs(linearLayoutManager.findLastCompletelyVisibleItemPosition());
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
              StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
              int[] lastItems = staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(new int[staggeredGridLayoutManager.getSpanCount()]);
              lastVisibleItem = Math.abs(lastItems[lastItems.length - 1]);
            }
            scrollableRecyclerView = lastVisibleItem < count - 1;
          }
        }
      }
    } else scrollableRecyclerView = true;
  }
}
