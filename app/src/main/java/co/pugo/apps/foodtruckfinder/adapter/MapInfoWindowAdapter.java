package co.pugo.apps.foodtruckfinder.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import co.pugo.apps.foodtruckfinder.R;

/**
 * Created by tobias on 10.1.2017.
 */

public class MapInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

  private LayoutInflater mInflater;
  private View mInfoWindow;

  public MapInfoWindowAdapter(LayoutInflater  inflater) {
    mInflater = inflater;
  }

  @Override
  public View getInfoWindow(Marker marker) {
    return null;
  }

  @Override
  public View getInfoContents(Marker marker) {
    if (mInfoWindow == null)
      mInfoWindow = mInflater.inflate(R.layout.map_info_window, null);

    TextView title = (TextView)mInfoWindow.findViewById(R.id.info_window_title);
    title.setText(marker.getTitle());
    TextView schedule = (TextView)mInfoWindow.findViewById(R.id.info_window_schedule);
    schedule.setText(marker.getSnippet());

    return mInfoWindow;
  }
}
