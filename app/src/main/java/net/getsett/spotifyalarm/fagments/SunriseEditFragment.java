package net.getsett.spotifyalarm.fagments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import net.getsett.spotifyalarm.R;
import net.getsett.spotifyalarm.adapters.HueLightBulbAdapter;
import net.getsett.spotifyalarm.adapters.SpotifyPlaylistAdapter;
import net.getsett.spotifyalarm.broadcastreceivers.AlarmBroadcastReceiver;
import net.getsett.spotifyalarm.dataaccess.AlarmRepository;
import net.getsett.spotifyalarm.integrations.philipshue.HueBridge;
import net.getsett.spotifyalarm.integrations.philipshue.HueLightBulb;
import net.getsett.spotifyalarm.integrations.spotify.SpotifyPlaylist;
import net.getsett.spotifyalarm.integrations.spotify.SpotifyToken;
import net.getsett.spotifyalarm.integrations.spotify.SpotifyTokenGenerator;
import net.getsett.spotifyalarm.integrations.spotify.SpotifyUser;
import net.getsett.spotifyalarm.models.Alarm;
import net.getsett.spotifyalarm.models.HueOptions;
import net.getsett.spotifyalarm.models.Options;
import net.getsett.spotifyalarm.models.SpotifyOptions;

import org.json.JSONArray;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SunriseEditFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SunriseEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SunriseEditFragment extends Fragment
implements View.OnClickListener, CompoundButton.OnCheckedChangeListener  {

    private JSONArray _spotifyPlaylists;

    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "spotify-alarm://callback";
    private SpotifyTokenGenerator _spotifyTokenGenerator;
    private String _spotifyCode = "";
    private SpotifyToken _spotifyToken;
    private RequestQueue queue;
    private static final int REQUEST_CODE = 1337;
    private Map<String, Integer> _lights = new HashMap<String, Integer>();

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (getActivity().findViewById(R.id.playlistSwitch) == buttonView) {
            getActivity().findViewById(R.id.playlistSelect).setEnabled(isChecked);
            getActivity().findViewById(R.id.playlistRandomise).setEnabled(isChecked);
        }
        else if (getActivity().findViewById(R.id.lightSwitch) == buttonView) {
            getActivity().findViewById(R.id.lightSelect).setEnabled(isChecked);
        }
        //whatever you want
    }

    @Override
    public void onClick(View v) {
        if (getActivity().findViewById(R.id.setAlarmButton) == v){

            Context context = getActivity().getApplicationContext();
            Intent intent = new Intent(context, AlarmBroadcastReceiver.class);


            SeekBar seekBar = (SeekBar)getActivity().findViewById(R.id.sunriseEasePeriod);


            AlarmManager alarms ;
            alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            TimePicker time = (TimePicker)getActivity().findViewById(R.id.timeSelect);
            Calendar calendar = Calendar.getInstance();

            calendar.set(Calendar.HOUR_OF_DAY, time.getCurrentHour());
            calendar.set(Calendar.MINUTE, time.getCurrentMinute());
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTime().before(new Date())){
                calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
            }

            Alarm alarm = new Alarm();
            alarm.setLightFadeTimeInMinutes(seekBar.getProgress() + 1);
            alarm.setTime(calendar.getTime());

            new AlarmRepository().add(alarm);

            Options options = new Options();
            options.TimeToSunset = seekBar.getProgress() + 1;

            //If audio is requested by the user then get the details
            if (((Switch)getActivity().findViewById(R.id.playlistSwitch)).isChecked()) {

                options.SpotifyOptions = new SpotifyOptions();
                options.SpotifyOptions.RefreshToken = _spotifyToken.getRefreshToken();
                options.SpotifyOptions.Randomise = ((Switch)getActivity().findViewById(R.id.playlistRandomise)).isChecked();

                Spinner s = (Spinner) getActivity().findViewById(R.id.playlistSelect);
                options.SpotifyOptions.PlaylistUri = ((SpotifyPlaylist)s.getSelectedItem()).Uri;
            }

            if (((Switch)getActivity().findViewById(R.id.lightSwitch)).isChecked()) {

                options.HueOptions = new HueOptions();

                Spinner s = (Spinner) getActivity().findViewById(R.id.lightSelect);

                options.HueOptions.LightBulbId =  ((HueLightBulb)s.getSelectedItem()).getId();
            }

            intent.putExtra("options", options);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            // set the alarm to start the selected ease period before the desired wake up time
            alarms.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() - (options.TimeToSunset * 60000), alarmIntent);

            Toast toast = Toast.makeText(getActivity(), "Alarm set", Toast.LENGTH_SHORT);
            toast.show();
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
    public static SunriseEditFragment newInstance(String param1, String param2) {
        SunriseEditFragment fragment = new SunriseEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public SunriseEditFragment() {
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
        View view = inflater.inflate(R.layout.fragment_sunrise_edit, container, false);

        view.findViewById(R.id.setAlarmButton).setOnClickListener(this);
        ((Switch)view.findViewById(R.id.playlistSwitch)).setOnCheckedChangeListener(this);
        ((Switch)view.findViewById(R.id.lightSwitch)).setOnCheckedChangeListener(this);

        SeekBar sk =(SeekBar) view.findViewById(R.id.sunriseEasePeriod);

        final TextView seekBarValue = (TextView)view.findViewById(R.id.easePeriodText);

        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                String seekBarText = String.valueOf(progress + 1) + " minute";
                if (progress != 1){
                    seekBarText += "s";
                }
                seekBarValue.setText(seekBarText);
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
                new AuthenticationRequest.Builder(getString(R.string.spotifyApiKey), AuthenticationResponse.Type.CODE, REDIRECT_URI);
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
            if (response.getType() == AuthenticationResponse.Type.CODE) {

                _spotifyCode = response.getCode();
                new SetupPlaylistsTask().execute();
                new SetupLightsTask().execute();

            }
        }
    }


    private class SetupPlaylistsTask extends AsyncTask<Void, Void, List<SpotifyPlaylist>> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected List<SpotifyPlaylist> doInBackground(Void... nothing) {
            //loadImageFromNetwork(urls[0]);

            _spotifyTokenGenerator = new SpotifyTokenGenerator(getActivity());
            _spotifyToken = _spotifyTokenGenerator.getToken(_spotifyCode);
            SpotifyUser user = new SpotifyUser(_spotifyToken, getActivity());
            return user.getPlaylists();
        }

        protected void onProgressUpdate(Void... progress) {
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(List<SpotifyPlaylist> playlists) {
            //mImageView.setImageBitmap(result);
            // When the loop is finished, updates the notification

            Spinner s = (Spinner) getActivity().findViewById(R.id.playlistSelect);
            SpotifyPlaylist[] playlistsArray = new SpotifyPlaylist[playlists.size()];
            playlistsArray = playlists.toArray(playlistsArray);
            SpotifyPlaylistAdapter adapter = new SpotifyPlaylistAdapter(
                    getActivity(),
                    playlistsArray
            );
            s.setAdapter(adapter);
        }
    }

    private class SetupLightsTask extends AsyncTask<Void, Void, List<HueLightBulb>> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected List<HueLightBulb> doInBackground(Void... nothing) {
            //loadImageFromNetwork(urls[0]);
            HueBridge bridge = new HueBridge(getActivity());

            return bridge.getAllLightBulbs();
        }

        protected void onProgressUpdate(Void... progress) {
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(List<HueLightBulb> lightBulbs) {
            //mImageView.setImageBitmap(result);
            // When the loop is finished, updates the notification

            Spinner s = (Spinner) getActivity().findViewById(R.id.lightSelect);
            HueLightBulb[] lightsArray = new HueLightBulb[lightBulbs.size()];
            lightsArray = lightBulbs.toArray(lightsArray);
            HueLightBulbAdapter adapter = new HueLightBulbAdapter(
                    getActivity(),
                    lightsArray);
            s.setAdapter(adapter);
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
}
