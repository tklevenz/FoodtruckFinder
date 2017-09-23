package co.pugo.apps.foodtruckfinder.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by tobias on 2.2.2017.
 * Custom view pager with the ability to disable paging
 */

public class WelcomeSlideViewPager extends ViewPager {
  private boolean mPagingEnabled = true;

  public WelcomeSlideViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    return mPagingEnabled && super.onTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    return mPagingEnabled && super.onInterceptTouchEvent(ev);
  }

  public void setPagingEnabled(boolean enabled) {
    mPagingEnabled = enabled;
  }
}
