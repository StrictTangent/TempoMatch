package com.example.tempomatch;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //////////

    private final boolean DEBUG = false;

    /////////////////////

    private List<String> spotifyPlaylists;
    private List<String> spotifySongs;

    private Playlist currentSpotifyPlaylist;




    //MODE FIELDS////////////////////


    private int MODE;
    private boolean MUSIC;
    private final int STARTUP = 0;
    private final int PLAYING = 1;
    private final int PAUSED = 2;
    private final int SET_PACE = 3;
    private final int FIND_PACE = 4;
    private final int TIMING = 5;

    private ImageView button_CENTER;
    private ImageView button_PREV;
    private ImageView button_NEXT;
    private ImageView button_SET;
    private ImageView button_FIND;

    private Drawable playDraw;
    private Drawable pauseDraw;
    private Drawable prevDraw;
    private Drawable nextDraw;
    private Drawable setDraw;
    private Drawable findDraw;
    private Drawable stopWatch;

    private TextView bpmView;
    private TextView paceView;
    private TextView titleView;
    private TextView nowPlaying;

    private TextView debugMusic;
    private TextView debugMode;





    ///////////////////////



    //SPOTIFY FIELDS
    public static String USER_NAME;
    public static final String CLIENT_ID = "56876e9226e34cb6a4461d3877309b75";
    public static final String REDIRECT_URI = "spotifyplaylists://callback";
    public static final int AUTH_TOKEN_REQUEST_CODE = 2019;
    private String mAccessToken;
    private SpotifyService spotify;
    private SpotifyAppRemote appRemote;

    //////////////////////////////

    // STEP COUNTER FIELDS
    private SensorManager sensorManager;

    private Button getTempoButton;
    private TextView count; // display count for debugging
    private TextView stepView; // display steps
    boolean activityRunning;   //DO WE NEED THIS? THIS IS AN ARTIFACT OF THE EXAMPLE CODE I FOUND

    boolean activityCounting; // boolean representing whether we are collecting user's pace

    private boolean timing; // boolean representing whether we are timing for the user's pace
    private long prevTime;  // initial time for calculating user's pace
    private long currTime;  // current time for a given sensor event
    private float prevSteps;// initial steps for calculating user's pace
    private float currSteps;// current steps for a given sensor event

    private final int TEMPO_TIME = 30000;
    private final float MARGIN = 4;

    private float tempo;

    private float bpm; // the pace the user sets.
    private float pace; // the current pace

    /////////////////////////////////
    List<Playlist> playlists;
    List<AudioFeaturesTrack> featuresTracks;

    List<AudioFeaturesTrack> songQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.main_ui);
        //setContentView(R.layout.splash);
        // initialize views
        count = (TextView) findViewById(R.id.countText);
        ///stepView = findViewById(R.id.stepView);
        //getTempoButton = findViewById(R.id.getSteps);
        //getTempoButton.setVisibility(View.INVISIBLE);


        MODE = STARTUP;
        MUSIC = false;

        button_CENTER = findViewById(R.id.buttonCENTER);
        button_FIND = findViewById(R.id.buttonFIND);
        button_NEXT = findViewById(R.id.buttonNEXT);
        button_PREV = findViewById(R.id.buttonPREV);
        button_SET = findViewById(R.id.buttonSET);

        nextDraw = getResources().getDrawable(R.drawable.ic_next);
        prevDraw = getResources().getDrawable(R.drawable.ic_rewind);
        playDraw = getResources().getDrawable(R.drawable.ic_play_button_2);
        pauseDraw = getResources().getDrawable(R.drawable.ic_pause);
        setDraw = getResources().getDrawable(R.drawable.ic_metronome);
        findDraw = getResources().getDrawable(R.drawable.ic_running);
        stopWatch = getResources().getDrawable(R.drawable.ic_stopwatch);

        paceView = findViewById(R.id.paceView);
        pace = 0;
        bpmView = findViewById(R.id.bpmView);
        bpm = 120;

        titleView = findViewById(R.id.titleText);
        nowPlaying = findViewById(R.id.nowPlaying);

        debugMode = findViewById(R.id.debugMODE);
        debugMusic = findViewById(R.id.debugMUSIC);

        if (!DEBUG) {
            debugMusic.setVisibility(View.INVISIBLE);
            debugMode.setVisibility(View.INVISIBLE);
        }

        updateViews();

         spotifyPlaylists = new ArrayList<String>();
            spotifyPlaylists.add("1k704ydfoeesod05jv58rh3oe:0ze1Ymim1dpGKRlx9H8vC8");
            spotifyPlaylists.add("dmxuazvp7p5fwjq7882m7uwd8:0mmqC3RIi8h7nApdD9gV7x");
            spotifyPlaylists.add("1166998660:355AmRU0BBJtzr9hwupxcP");
            spotifyPlaylists.add("nikewomen:13thjkLTYZmZvjdz4u6kxh");
            spotifyPlaylists.add("spotify:37i9dQZF1DWZ2xRu8ajLOe");
            spotifyPlaylists.add("sonymusicfinland:7gO9WmJaPmIviOlvK1m95P");



        // initialize booleans
        timing = false;
        activityCounting = false;

        // initialize the SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // get spotify token
        getToken();

        // set up appremote
        getRemote();


    }


    @Override
    protected  void onResume() {
        super.onResume();
        activityRunning = true; //DO WE NEED THIS??

        // create a sensor and initialize registerListener
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null){
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            //Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        activityRunning = false;

    }

    // Runs whenever there is a new sensor event (I think!)
    @Override
    public void onSensorChanged(SensorEvent event) {

        // IF we're currently checking the User's Tempo...
        if (activityCounting) {
            currTime = System.currentTimeMillis();
            currSteps = event.values[0];

            //String messageExtra = "\n";
            //messageExtra += "Current time: " + currTime + "ms";

            if (!timing) { // if we aren't timing, set the initial time and step values...
                timing = true;
                prevTime = currTime;
                prevSteps = currSteps;
                //messageExtra += "\n" + "timing: " + timing;

            } else { //when we are currently timing...
                //messageExtra += "\n" + "difference = " + (currTime - prevTime);

                paceView.setText((((currSteps - prevSteps) / (currTime - prevTime)) * 60000)
                        + " steps/min");

                if ((currTime - prevTime) >= TEMPO_TIME) { // if TEMPO_TIME milliseconds have elapsed, calculate steps per minute
                    activityCounting = false;
                    timing = false;
                    float stepsPerMinute = (((currSteps - prevSteps) / (currTime - prevTime)) * 60000);
                    tempo = stepsPerMinute;
                    // go ahead and make queue based on tempo.
                    makeQueue(stepsPerMinute);
                    //stepView.setText("Steps per Minute = " + stepsPerMinute);
                }
            }

            if (activityRunning) {
                //count.setText("Steps: " + String.valueOf(currSteps) + "\n" + messageExtra + " activityCounting: " + activityCounting);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Set Tempo Button has been pressed, go ahead and collect the user's tempo
    public void getSteps(View view) {
        Toast.makeText(this,"Set Tempo!", Toast.LENGTH_LONG).show();
        activityCounting = true;
        timing = false;
    }




    // Process all the data from the playlist pool
    private void getPlaylists(){

        playlists = new ArrayList<Playlist>();
        featuresTracks = new ArrayList<AudioFeaturesTrack>();
        spotifySongs = new ArrayList<String>();

        // get all the playlists...
        for (int j = 0; j < spotifyPlaylists.size(); j++) {
            String[] user_list = spotifyPlaylists.get(j).split(":");


            spotify.getPlaylist(user_list[0], user_list[1], new SpotifyCallback<Playlist>() {
                @Override
                public void failure(SpotifyError spotifyError) {

                }

                @Override
                public void success(final Playlist playlist, Response response) {
                    playlists.add(playlist);

                    // get all the songs in each playlist...
                    for (int i = 0; i < playlist.tracks.items.size(); i++) {
                        spotifySongs.add(playlist.tracks.items.get(i).track.id);
                    }
                    // if we've processed the last song in the last playlist...
                    String last = spotifyPlaylists.get(spotifyPlaylists.size()-1).split(":")[1];
                    if (playlist.id.equals(last)){
                        //String result = spotifySongs.get(0);
                        String result = "";
                        for (int j = 0; j < spotifySongs.size(); j++){
                            result += spotifySongs.get(j)+",";

                            if (j % 99 == 0 || j == spotifySongs.size()-1){
                                spotify.getTracksAudioFeatures(result, new SpotifyCallback<AudioFeaturesTracks>() {
                                    @Override
                                    public void failure(SpotifyError spotifyError) {

                                    }

                                    @Override
                                    public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                                        for (AudioFeaturesTrack track : audioFeaturesTracks.audio_features){
                                            featuresTracks.add(track);
                                            if (track.id.equals(spotifySongs.get(spotifySongs.size()-1))){
                                                //getTempoButton.setVisibility(View.VISIBLE); ////////////////////////////////CHECK THIS!!!!!!!!!
                                                MODE = FIND_PACE;
                                                button_CENTER.setVisibility(View.VISIBLE);
                                                updateViews();
                                            }
                                        }

                                        //getTempoButton.setVisibility(View.VISIBLE);
                                    }
                                });
                                result = "";
                            }

                        }



                    }
                }
            });
        }
    }



    // add all the songs to the queue that meet BPM requirements
    private void makeQueue(float tempo){
        songQueue = new LinkedList<AudioFeaturesTrack>();

        for (int i = 0; i < featuresTracks.size(); i++){
            float trackTempo = featuresTracks.get(i).tempo;
            if (trackTempo >= tempo-MARGIN && trackTempo <= tempo+MARGIN){
                songQueue.add(featuresTracks.get(i));
            }
        }

        pace = tempo;
        makePlaylist(songQueue, tempo);


    }

   private void makePlaylist(final List<AudioFeaturesTrack> songs, float tempo){

        String playlistName = "Tempo: " + tempo;

        Map<String, Object> options = new HashMap<>();
        options.put("name", playlistName);

        spotify.createPlaylist(USER_NAME, options, new SpotifyCallback<Playlist>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                MODE = 4;
                updateViews();
            }

            @Override
            public void success(Playlist playlist, Response response) {
                currentSpotifyPlaylist = playlist;
                addSongsToPlaylist(songs, playlist);
            }
        });

   }

   private void addSongsToPlaylist(List<AudioFeaturesTrack> songs, final Playlist playlist){
       Collections.shuffle(songs);
       String uris = "";

       int max = 20;
       if (songs.size() < max) max = songs.size();

       for (int i = 0; i < max; i++){
           uris += songs.get(i).uri + ",";
       }

       Map<String, Object> map1 = new HashMap<String, Object>();
       map1.put("uris", uris);

       // Seems the second map field in addTracksToPlayList can't just be null... so we create this.
       Map<String, Object> map2 = new HashMap<String, Object>();
       map2.put("uris", new JSONArray());

       spotify.addTracksToPlaylist(USER_NAME, playlist.id, map1, map2, new SpotifyCallback<Pager<PlaylistTrack>>() {
           @Override
           public void failure(SpotifyError spotifyError) {
                MODE = 4;
                updateViews();
           }

           @Override
           public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {


            appRemote.getPlayerApi().play(playlist.uri);
               MODE = PLAYING;
               MUSIC = true;
               updateViews();
           }
       });
   }











    ////////////////////////////////
    //SET UP SPOTIFY AUTHORIZATION//
    ////////////////////////////////

    public void getToken(){
        final AuthenticationRequest request = getAuthenticationRequest(AuthenticationResponse.Type.TOKEN);
        AuthenticationClient.openLoginActivity(this, AUTH_TOKEN_REQUEST_CODE, request);
    }

    private AuthenticationRequest getAuthenticationRequest(AuthenticationResponse.Type type) {
        return new AuthenticationRequest.Builder(CLIENT_ID, type, REDIRECT_URI)
                .setShowDialog(false)
                //.setScopes(new String[]{"playlist-modify-public","user-library-read"})
                .setScopes(new String[]{"user-read-recently-played","user-top-read","user-library-modify","user-library-read",
                        "playlist-read-private","playlist-modify-public","playlist-modify-private","playlist-read-collaborative",
                        "user-read-email","user-read-birthdate","user-read-private","user-read-playback-state","user-modify-playback-state",
                        "user-read-currently-playing","app-remote-control","streaming","user-follow-read","user-follow-modify"})
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);

        if (AUTH_TOKEN_REQUEST_CODE == requestCode) {
            mAccessToken = response.getAccessToken();
            //count.setText(mAccessToken);

            // Set up and initialize our Spotify Service field, 'spotify'
            SpotifyApi api = new SpotifyApi();
            api.setAccessToken(mAccessToken);
            this.spotify = api.getService();

            // set USER_NAME to be the currently logged on user.
            this.spotify.getMe(new SpotifyCallback<UserPrivate>() {
                @Override
                public void failure(SpotifyError spotifyError) {

                }

                @Override
                public void success(UserPrivate userPrivate, Response response) {
                    USER_NAME = userPrivate.id;
                    getPlaylists();
                }
            });
        }
    }



    private void getRemote(){

        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                appRemote = spotifyAppRemote;

                // Subscribe to PlayerState
                appRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
                    @Override
                    public void onEvent(PlayerState playerState) {
                        if (MODE != TIMING && playerState.isPaused) MODE = PAUSED;
                            else if (MODE != TIMING) MODE = PLAYING;
                        updateViews();

                        nowPlaying.setText("PLAYING: \n" + playerState.track.name + " by " + playerState.track.artist.name);
                        System.out.println(playerState.track.name + " by " + playerState.track.artist.name);
                    }
                });
            }


            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }

    /////////////////////////////////////////

    private void updateViews(){

        debugMusic.setText("Music = " + MUSIC);
        debugMode.setText("MODE = " + MODE);

        paceView.setText("PACE: " + pace);
        bpmView.setText(bpm + " bmp");

        if (MODE == STARTUP){
            titleView.setText("TEMPO MATCH");
            button_PREV.setVisibility(View.INVISIBLE);
            button_NEXT.setVisibility(View.INVISIBLE);

            button_CENTER.setVisibility(View.INVISIBLE);
            button_FIND.setVisibility(View.INVISIBLE);
            button_SET.setVisibility(View.INVISIBLE);
            bpmView.setVisibility(View.INVISIBLE);
            paceView.setVisibility(View.INVISIBLE);

            nowPlaying.setVisibility(View.INVISIBLE);

        } else if (MODE == TIMING){

            bpmView.setText("START MOVING!!");
            button_PREV.setVisibility(View.INVISIBLE);
            button_NEXT.setVisibility(View.INVISIBLE);

            button_CENTER.setImageDrawable(stopWatch);
            button_FIND.setVisibility(View.INVISIBLE);
            button_SET.setVisibility(View.INVISIBLE);

            bpmView.setVisibility(View.VISIBLE);
            paceView.setVisibility(View.VISIBLE);

            titleView.setVisibility(View.INVISIBLE);

        } else if (MUSIC){

            nowPlaying.setVisibility(View.VISIBLE);

            if (MODE == PLAYING){
                button_PREV.setVisibility(View.VISIBLE);
                button_NEXT.setVisibility(View.VISIBLE);

                button_CENTER.setImageDrawable(pauseDraw);

                button_FIND.setVisibility(View.VISIBLE);
                button_FIND.setImageDrawable(findDraw);

                button_SET.setVisibility(View.VISIBLE);
                button_SET.setImageDrawable(setDraw);

                bpmView.setVisibility(View.INVISIBLE);
                paceView.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.INVISIBLE);

            } else if (MODE == PAUSED){
                button_PREV.setVisibility(View.VISIBLE);
                button_NEXT.setVisibility(View.VISIBLE);

                button_CENTER.setImageDrawable(playDraw);

                button_FIND.setVisibility(View.VISIBLE);
                button_FIND.setImageDrawable(findDraw);

                button_SET.setVisibility(View.VISIBLE);
                button_SET.setImageDrawable(setDraw);

                bpmView.setVisibility(View.INVISIBLE);
                paceView.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.INVISIBLE);

            } else if (MODE == SET_PACE){
                button_PREV.setVisibility(View.VISIBLE);
                button_NEXT.setVisibility(View.VISIBLE);

                button_CENTER.setImageDrawable(setDraw);

                button_FIND.setVisibility(View.VISIBLE);
                button_FIND.setImageDrawable(findDraw);

                button_SET.setVisibility(View.VISIBLE);
                button_SET.setImageDrawable(playDraw);

                bpmView.setVisibility(View.VISIBLE);
                paceView.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.INVISIBLE);

            } else if (MODE == FIND_PACE){
                button_PREV.setVisibility(View.INVISIBLE);
                button_NEXT.setVisibility(View.INVISIBLE);

                button_CENTER.setImageDrawable(findDraw);

                button_FIND.setVisibility(View.VISIBLE);
                button_FIND.setImageDrawable(playDraw);

                button_SET.setVisibility(View.VISIBLE);
                button_SET.setImageDrawable(setDraw);

                bpmView.setVisibility(View.INVISIBLE);
                paceView.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.INVISIBLE);

            }

        } else {

            nowPlaying.setVisibility(View.INVISIBLE);

            if (MODE == SET_PACE){
                button_PREV.setVisibility(View.VISIBLE);
                button_NEXT.setVisibility(View.VISIBLE);

                button_CENTER.setImageDrawable(setDraw);

                button_FIND.setVisibility(View.VISIBLE);
                button_FIND.setImageDrawable(findDraw);

                button_SET.setVisibility(View.INVISIBLE);

                bpmView.setVisibility(View.VISIBLE);
                paceView.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.INVISIBLE);

            } else if (MODE == FIND_PACE){
                button_PREV.setVisibility(View.INVISIBLE);
                button_NEXT.setVisibility(View.INVISIBLE);

                button_CENTER.setImageDrawable(findDraw);

                button_FIND.setVisibility(View.INVISIBLE);

                button_SET.setVisibility(View.VISIBLE);
                button_SET.setImageDrawable(setDraw);

                bpmView.setVisibility(View.INVISIBLE);
                paceView.setVisibility(View.VISIBLE);

                titleView.setVisibility(View.INVISIBLE);

            }
        }

    }


    public void onCenterClicked(View view) {
        if (MODE == PLAYING){
            appRemote.getPlayerApi().pause();
            MODE = PAUSED;
            updateViews();
        } else if (MODE == PAUSED){
            appRemote.getPlayerApi().resume();
            MODE = PLAYING;
            updateViews();

        } else if (MODE == SET_PACE) {
            appRemote.getPlayerApi().pause();
            makeQueue(bpm);
        } else if (MODE == FIND_PACE){
            appRemote.getPlayerApi().pause();
            MODE = TIMING;
            updateViews();
            getSteps(view);
        }
    }

    public void onSetClicked(View view) {

        if (MODE == SET_PACE){ //if mode already is SET (play button is in lower right)

            setPlayMode();
        } else {
            MODE = SET_PACE;
        }
        updateViews();
    }

    public void onFindClicked(View view) {

        if (MODE == FIND_PACE){

            setPlayMode();
        } else {
            MODE = FIND_PACE;
        }
        updateViews();
    }

    public void onNextClicked(View view) {
        if (MODE == SET_PACE){
            bpm++;
            bpmView.setText((bpm) + " bpm");
        } else if (MODE == PLAYING) {
            appRemote.getPlayerApi().skipNext();
        }
    }

    public void onPrevClicked(View view) {
        if (MODE == SET_PACE){
            bpm--;
            bpmView.setText((bpm) + " bpm");
        } else if (MODE == PLAYING){
            appRemote.getPlayerApi().skipPrevious();
        }
    }

    private void setPlayMode(){
        appRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
            @Override
            public void onResult(PlayerState playerState) {
                if (playerState.isPaused) MODE = PAUSED;
                    else MODE = PLAYING;
                updateViews();
            }
        });
    }
}
