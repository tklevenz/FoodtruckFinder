package co.pugo.apps.foodtruckfinder.ui;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import co.pugo.apps.foodtruckfinder.R;

/**
 * Created by tobias on 6.2.2017.
 */

public class EditRadiusDialog extends DialogFragment implements TextView.OnEditorActionListener {
  private EditText mEditText;

  public EditRadiusDialog() {}

  public interface EditRadiusDialogListener {
    void onFinishEditDialog(String input);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setStyle(DialogFragment.STYLE_NORMAL, 0);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_edit_radius, container);
    mEditText = (EditText) view.findViewById(R.id.txt_radius);
    getDialog().setTitle(R.string.pref_location_radius_title);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    mEditText.setText(prefs.getString(getString(R.string.pref_location_radius_key), getString(R.string.default_radius)));
    mEditText.requestFocus();
    getDialog().getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    mEditText.setOnEditorActionListener(this);

    return view;
  }

  @Override
  public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
    if (EditorInfo.IME_ACTION_DONE == i) {
      EditRadiusDialogListener activity = (EditRadiusDialogListener) getActivity();
      activity.onFinishEditDialog(mEditText.getText().toString());
      this.dismiss();
      return true;
    }
    return false;
  }
}
