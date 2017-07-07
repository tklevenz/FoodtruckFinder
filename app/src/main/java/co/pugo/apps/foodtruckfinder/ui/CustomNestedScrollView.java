package co.pugo.apps.foodtruckfinder.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by tobias on 30.9.2016.
 */
public class CustomNestedScrollView extends NestedScrollView {

  private int mPaddingTop;

  public CustomNestedScrollView(Context context) {
    super(context);
  }

  public CustomNestedScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CustomNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void onAttachedToWindow() {
    mPaddingTop = getPaddingTop();
    super.onAttachedToWindow();
  }

  /**
   * Fixing problem with recyclerView in nested scrollview requesting focus
   * http://stackoverflow.com/questions/36314836/recycler-view-inside-nestedscrollview-causes-scroll-to-start-in-the-middle
   */
  @Override
  public void requestChildFocus(View child, View focused) {
    //super.requestChildFocus(child, focused);
  }

  /**
   * http://stackoverflow.com/questions/36314836/recycler-view-inside-nestedscrollview-causes-scroll-to-start-in-the-middle
   */
  @Override
  protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
    Log.d("CustomNestedScrollView", "onRequestChildFocus paddingTop " + this.getPaddingTop());

    return false;
  }

  /**
   * Fixing layout adding paddingTop when activity was launched from intent filterTags
   */
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    setPadding(getPaddingLeft(), mPaddingTop, getPaddingRight(), getPaddingRight());
    super.onLayout(changed, l, t, r, b);
  }
}
