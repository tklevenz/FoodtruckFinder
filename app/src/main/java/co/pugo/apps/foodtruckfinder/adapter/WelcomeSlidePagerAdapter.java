package co.pugo.apps.foodtruckfinder.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by tobias on 2.2.2017.
 */

public class WelcomeSlidePagerAdapter extends PagerAdapter {
  private int[] mPageLayouts;
  private Context mContext;

  public WelcomeSlidePagerAdapter(Context context, int[] layouts) {
    mContext = context;
    mPageLayouts = layouts;
  }

  @Override
  public int getCount() {
    return mPageLayouts.length;
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View view = inflater.inflate(mPageLayouts[position], container, false);
    container.addView(view);

    return view;
  }

  @Override
  public boolean isViewFromObject(View view, Object object) {
    return view == object;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    View view = (View) object;
    container.removeView(view);
  }
}
