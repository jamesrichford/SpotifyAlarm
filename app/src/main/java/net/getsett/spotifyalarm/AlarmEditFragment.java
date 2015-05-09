package net.getsett.spotifyalarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.acra.ACRA;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlarmEditFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlarmEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlarmEditFragment extends Fragment
implements View.OnClickListener {

    private JSONArray _spotifyPlaylists;

    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "spotify-alarm://callback";
    private String _spotifyToken = "";
    private RequestQueue queue;
    private static final int REQUEST_CODE = 1337;

    @Override
    public void onClick(View v) {
        if (getActivity().findViewById(R.id.button4) == v){

            Context context = getActivity().getApplicationContext();
            Intent intent = new Intent(context, AlarmBroadcastReceiver.class);


            SeekBar seekBar = (SeekBar)getActivity().findViewById(R.id.seekBar3);


            AlarmManager alarms ;
            alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            TimePicker time = (TimePicker)getActivity().findViewById(R.id.timePicker2);
            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.HOUR_OF_DAY, time.getCurrentHour());
            calendar.set(Calendar.MINUTE, time.getCurrentMinute());
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTime().before(new Date())){
                calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
            }

            Alarm alarm = new Alarm();
            alarm.setLightFadeTimeInMinutes(seekBar.getProgress());
            alarm.setTime(calendar.getTime());

            new AlarmRepository().add(alarm);

            intent.putExtra("AlarmId", alarm.getId());
            intent.putExtra("Minutes", seekBar.getProgress());
            intent.putExtra("SpotifyToken", _spotifyToken);
            Spinner s = (Spinner) getActivity().findViewById(R.id.spinner2);
            try {
                intent.putExtra("SpotifyUri", _spotifyPlaylists.getJSONObject((int)s.getSelectedItemId()).get("uri").toString());
            }
            catch (JSONException exception){
                ACRA.getErrorReporter().handleSilentException(exception);
            }


            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            alarms.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
        }
    }
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AlarmEditFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlarmEditFragment newInstance(String param1, String param2) {
        AlarmEditFragment fragment = new AlarmEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public AlarmEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_alarm_edit, container, false);

        view.findViewById(R.id.button4).setOnClickListener(this);

        SeekBar sk =(SeekBar) view.findViewById(R.id.seekBar3);

        final TextView seekBarValue = (TextView)view.findViewById(R.id.textView4);

        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                seekBarValue.setText(String.valueOf(progress) + " Minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);


        queue = Volley.newRequestQueue(getActivity().getApplicationContext());
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(getString(R.string.spotifyApiKey), AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        Intent intent = AuthenticationClient.createLoginActivityIntent(getActivity(), request);
        startActivityForResult(intent, REQUEST_CODE);

        // To close LoginActivity
        AuthenticationClient.stopLoginActivity(getActivity(), REQUEST_CODE);

        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                _spotifyToken = response.getAccessToken();

                getUsername();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    private void getUsername(){
        //Get the users username
        JsonObjectRequest usernameRequest = new SpotifyWebApiRequest(
                Request.Method.GET,
                "https://api.spotify.com/v1/me",
                null,
                _spotifyToken,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // display response
                        //Log.d("Response", response.toString());

                        try {
                            getUserPlaylists(response.get("id").toString());
                        }
                        catch (JSONException exception){
                            ACRA.getErrorReporter().handleSilentException(exception);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ACRA.getErrorReporter().handleSilentException(error);
                    }
                }
        );

        queue.add(usernameRequest);
    }

    private void getUserPlaylists(String username){
        //Get the users username
        JsonObjectRequest playlistRequest = new SpotifyWebApiRequest(
                Request.Method.GET,
                "https://api.spotify.com/v1/users/" + username + "/playlists",
                null,
                _spotifyToken,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        // display response
                        try {
                            _spotifyPlaylists = response.getJSONArray("items");

                            List<String> playlistNames = new ArrayList<String>();
                            for(int i = 0; i < _spotifyPlaylists.length(); i++){
                                playlistNames.add(_spotifyPlaylists.getJSONObject(i).get("name").toString());
                            }
                            String[] playlistNamesArray = new String[playlistNames.size()];
                            playlistNamesArray = playlistNames.toArray(playlistNamesArray);

                            Spinner s = (Spinner) getActivity().findViewById(R.id.spinner2);
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                                    android.R.layout.simple_spinner_item, playlistNamesArray);
                            s.setAdapter(adapter);
                        }
                        catch (JSONException exception)
                        {
                            ACRA.getErrorReporter().handleSilentException(exception);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ACRA.getErrorReporter().handleSilentException(error);
                    }
                }
        );
        queue.add(playlistRequest);
    }

}
