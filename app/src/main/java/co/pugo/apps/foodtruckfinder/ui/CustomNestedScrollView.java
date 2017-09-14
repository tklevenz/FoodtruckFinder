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
   * Fixing layout adding paddingTop
   */
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    setPadding(getPaddingLeft(), mPaddingTop, getPaddingRight(), getPaddingRight());
    super.onLayout(changed, l, t, r, b);
  }
}
