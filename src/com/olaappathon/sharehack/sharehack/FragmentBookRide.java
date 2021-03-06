package com.olaappathon.sharehack.sharehack;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.olaappathon.sharehack.sharehack.data.RideData;
import com.olaappathon.sharehack.sharehack.utils.Constant;
import com.olaappathon.sharehack.sharehack.utils.Utils;

public class FragmentBookRide extends Fragment {

	private static Context mContext;
	private static RideData mRideData;

	private Button mButtonRideNow;
	private Button mButtonRidelater;
	private EditText mEditTextDestination;
	private EditText mEditTextSource;

	static final LatLng HAMBURG = new LatLng(53.558, 9.927);
	static final LatLng KIEL = new LatLng(53.551, 9.993);
	private GoogleMap map;

	public FragmentBookRide() {
	}

	private static ProgressDialog pDialog;

	private static void showpDialog() {
		if (!pDialog.isShowing())
			pDialog.show();
	}

	private static void hidepDialog() {
		if (pDialog.isShowing())
			pDialog.dismiss();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		pDialog = new ProgressDialog(getActivity());
		pDialog.setMessage("Please wait...");
		pDialog.setCancelable(false);

		View rootView = inflater.inflate(R.layout.fragment_bookride, container, false);
		mContext = getActivity().getApplicationContext();
		init(rootView);

		return rootView;
	}

	View.OnClickListener mClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_ride_now:
				processRideNow();
				break;
			case R.id.button_ride_later:
				processRideLater();
			default:
				break;
			}
		}

	};

	private void processRideLater() {
		showDialogue(AlertDialogFragment.newInstance(true));
	}

	private void processRideNow() {
		showDialogue(AlertDialogFragment.newInstance(false));
	}

	private void showDialogue(DialogFragment newFragment) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		newFragment.show(ft, "dialog");
	}

	private void init(View view) {

		mRideData = new RideData();
		mButtonRidelater = (Button) view.findViewById(R.id.button_ride_later);
		mButtonRideNow = (Button) view.findViewById(R.id.button_ride_now);

		mButtonRidelater.setOnClickListener(mClickListener);
		mButtonRideNow.setOnClickListener(mClickListener);

		mEditTextDestination = (EditText) view.findViewById(R.id.edittext_destination);
		// mEditTextSource = (EditText) view.findViewById(R.id.edittext_source);

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();

		if (map != null) {
			Marker hamburg = map.addMarker(new MarkerOptions().position(HAMBURG).title("Ola"));
			Marker kiel = map.addMarker(new MarkerOptions().position(KIEL).title("Ola").snippet("Ola is cool").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
		}
	}

	private static void sendDataToServer() {
		showpDialog();
		String url = String.format(Utils.URL_POST_RIDE_REQUEST, Utils.getStringFromPref(mContext, Constant.PREF_KEY_IP_ADDRESS, "192.168.1.31"));
		System.out.println("url" + url);

		StringRequest postRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
			@Override
			public void onResponse(String response) {
				hidepDialog();
				Toast.makeText(mContext, R.string.ride_request_sent, Toast.LENGTH_SHORT).show();
				Log.d("Response", response);
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				hidepDialog();
				Toast.makeText(mContext, R.string.error_responce, Toast.LENGTH_SHORT).show();
				Log.d("Error.Response", error.getMessage() + " " + error.getLocalizedMessage() + " " + error.getCause());
			}
		}) {
			@Override
			protected Map<String, String> getParams() {
				return Utils.getKeyValuePair(mRideData);
			}
		};
		AppController.getInstance().getRequestQueue().add(postRequest);
	}

	public static class AlertDialogFragment extends DialogFragment {
		static private boolean mShowTimer;
		RadioGroup mRadioGroupShare;
		CheckBox mCheckBoxShare;
		TimePicker mTimePicker;
		Button mRideOK, mRideCancel;
		static AlertDialogFragment mFrag;
		EditText mEditTextNoOfMembers;

		private static String pad(int c) {
			if (c >= 10)
				return String.valueOf(c);
			else
				return "0" + String.valueOf(c);
		}

		View.OnClickListener mClickListener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.button_ok:
					mRideData.setId(1);
					mRideData.setNoOfMembers(mEditTextNoOfMembers.getText().toString());
					sendDataToServer();
					mFrag.dismiss();
					break;
				case R.id.button_cancel:
					mFrag.dismiss();
				default:
					break;
				}
			}

		};

		private void setTime() {
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);

			mTimePicker.setCurrentHour(hour);
			mTimePicker.setCurrentMinute(minute);
		}

		public static AlertDialogFragment newInstance(boolean showTime) {
			mFrag = new AlertDialogFragment();
			mShowTimer = showTime;
			return mFrag;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			View v = inflater.inflate(R.layout.fragment_ridenow, container, false);

			getDialog().setTitle("Ride!!");

			mEditTextNoOfMembers = (EditText) v.findViewById(R.id.no_of_members);
			mRideOK = (Button) v.findViewById(R.id.button_ok);
			mRideCancel = (Button) v.findViewById(R.id.button_cancel);
			mRideOK.setOnClickListener(mClickListener);
			mRideCancel.setOnClickListener(mClickListener);
			mCheckBoxShare = (CheckBox) v.findViewById(R.id.checkbox_share_ride);

			mTimePicker = (TimePicker) v.findViewById(R.id.timepicker);
			mTimePicker.setVisibility(mShowTimer ? View.VISIBLE : View.GONE);
			mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

				@Override
				public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
					mRideData.setTime((new StringBuilder().append(pad(hourOfDay)).append(":").append(pad(minute))).toString());

					mTimePicker.setCurrentHour(hourOfDay);
					mTimePicker.setCurrentMinute(minute);
				}
			});
			setTime();

			mRadioGroupShare = (RadioGroup) v.findViewById(R.id.radio_group_share);
			mCheckBoxShare.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mRadioGroupShare.setVisibility(isChecked ? View.VISIBLE : View.GONE);
					mRideData.setShare(isChecked ? "" : Constant.SHARE_NOT);
				}
			});
			mRadioGroupShare.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					switch (checkedId) {
					case R.id.radio_public:
						mRideData.setShare(Constant.SHARE_PUBLIC);
						break;
					case R.id.radio_only_contact:
						mRideData.setShare(Constant.SHARE_CONTACT);
					default:
						break;
					}
				}
			});

			return v;
		}
	}

}
