package co.pugo.apps.foodtruckfinder.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.RadioButton;

import co.pugo.apps.foodtruckfinder.R;

/**
 * Created by tobias on 14.10.2016.
 */

public class FilterRadioButton extends RadioButton {
  private final int mCheckedBackgroundColor;
  private final int mCheckedTextColor;
  private final int mUnCheckedBackgroundColor;
  private final int mUnCheckedTextColor;

  public FilterRadioButton(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FilterRadioButton, 0, 0);

    try {
      mCheckedBackgroundColor = a.getInteger(R.styleable.FilterRadioButton_checkedBackgroundColor, Color.WHITE);
      mCheckedTextColor = a.getInteger(R.styleable.FilterRadioButton_checkedTextColor, Color.BLACK);
      mUnCheckedBackgroundColor = a.getInteger(R.styleable.FilterRadioButton_unCheckedBackgroundColor, Color.WHITE);
      mUnCheckedTextColor = a.getInteger(R.styleable.FilterRadioButton_unCheckedTextColor, Color.BLACK);
    } finally {
      a.recycle();
    }

    setBackgroundColor(mUnCheckedBackgroundColor);
    setTextColor(mUnCheckedTextColor);
  }

  @Override
  public void setChecked(boolean checked) {
    super.setChecked(checked);
    setSoundEffectsEnabled(false);
    setButtonDrawable(null);

    setBackgroundColor(mUnCheckedBackgroundColor);
    setTextColor(mUnCheckedTextColor);

    if (isChecked()) {
      setBackgroundColor(mCheckedBackgroundColor);
      setTextColor(mCheckedTextColor);
    }
  }
}
