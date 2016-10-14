package co.pugo.apps.foodtruckfinder.adapter;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.pugo.apps.foodtruckfinder.R;
import co.pugo.apps.foodtruckfinder.data.TagsColumns;

/**
 * Created by tobias on 11.10.2016.
 */

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.TagsAdapterViewHolder> {


  private Cursor mCursor;

  @Override
  public TagsAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (parent instanceof RecyclerView) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag, parent, false);
      return new TagsAdapter.TagsAdapterViewHolder(view);
    } else {
      throw new RuntimeException("Not bound to RecyclerView");
    }
  }

  @Override
  public void onBindViewHolder(TagsAdapterViewHolder holder, int position) {
    mCursor.moveToPosition(position);
    holder.tag.setText(mCursor.getString(mCursor.getColumnIndex(TagsColumns.TAG)));
  }

  @Override
  public int getItemCount() {
    if (null == mCursor) return 0;
    return mCursor.getCount();
  }

  public void swapCursor(Cursor cursor) {
    mCursor = cursor;
    notifyDataSetChanged();
  }

  public class TagsAdapterViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.tag) TextView tag;
    public TagsAdapterViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
