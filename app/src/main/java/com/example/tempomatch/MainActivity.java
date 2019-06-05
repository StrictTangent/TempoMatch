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

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //////////

    private final boolean DEBUG = true; // controls whether we display the current MODEs

    /////////////////////

    private boolean USER_TRACKS;
    private boolean USER_PLAYLISTS;
    private boolean PREDEFINED;
    private boolean CATEGORY_PLAYLISTS;

    private boolean GOT_USER_TRACKS;
    private boolean GOT_PREDEFINED;
    private boolean GOT_USER_PLAYLISTS;
    private boolean GOT_CATEGORY;

    private final int TYPE_CATEGORY = 1;
    private final int TYPE_PREDEFINED = 2;
    private final int TYPE_USER = 3;

    //MODE FIELDS////////////////////

    private int MODE;
    private boolean MUSIC;
    private final int STARTUP = 0;
    private final int PLAYING = 1;
    private final int PAUSED = 2;
    private final int SET_PACE = 3;
    private final int FIND_PACE = 4;
    private final int TIMING = 5;

    // ImageViews for buttons
    private ImageView button_CENTER;
    private ImageView button_PREV;
    private ImageView button_NEXT;
    private ImageView button_SET;
    private ImageView button_FIND;

    // Drawables for button images
    private Drawable playDraw;
    private Drawable pauseDraw;
    private Drawable prevDraw;
    private Drawable nextDraw;
    private Drawable setDraw;
    private Drawable findDraw;
    private Drawable stopWatch;

    // TextViews for UI
    private TextView bpmView;
    private TextView paceView;
    private TextView titleView;
    private TextView nowPlaying;

    private TextView debugMusic;
    private TextView debugMode;

    // ImageView for album art
    private ImageView albumArt;

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

    private final int TEMPO_TIME = 30000; // how long we spend tracking users steps (in milliseconds)
    private final float MARGIN = 4; // the +- margin for bpm when choosing songs

    private float tempo; // the target tempo - CURRENTLY UNUSED

    private float bpm; // the pace the user sets (the number above the metronome)
    private float pace; // the current pace

    /////////////////////////////////

    private List<String> predefinedSpotifyPlaylists; // The playlists to pull songs from (USER_ID:PlaylistID)
    //private List<String> spotifySongs; // The songs to pull from (Track ID)
    private Playlist currentSpotifyPlaylist; // the most recently created Spotify playlist

    //private List<Playlist> playlists;   // the list of playlists to consider
    private Set<AudioFeaturesTrack> featuresTracks; // ALL of the featuresTracks
    private Map<String, Float> beatMap; // a Map of known track URIs to tempos

    private int PLAYLIST_SIZE; // the maximum size of a created playlist

    ///////////////////////////////////

    private List<String> userSongIDs; // list for users track ids
    //private List<String> userPlaylistSongIDs; // list for track ids from user's playlists.

    private List<String> categorySongIDs;
    private List<AudioFeaturesTrack> categoryFeaturesTracks;
    private Map<String, Float> categoryBeatMap;

    private List<AudioFeaturesTrack> predefinedFeaturesTracks;
    private Map<String, Float> predefinedBeatMap;

    private List<AudioFeaturesTrack> userSavedFeaturesTracks;
    private Map<String, Float> userSavedBeatMap;

    private List<AudioFeaturesTrack> userPlaylistFeaturesTracks;
    private Map<String, Float> userPlaylistBeatMap;

    ////////////////////////////////
    // The ImageLoader for handling album art
    private ImageLoader imageLoader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize modes
        MODE = STARTUP;
        MUSIC = false;

        // Get and set parameters for music sources
        Intent intent = getIntent();
        boolean[] parameters = (boolean[]) intent.getExtras().get("parameters");

        USER_TRACKS = parameters[0];
        USER_PLAYLISTS = parameters[1];
        CATEGORY_PLAYLISTS = parameters[2];
        PREDEFINED = parameters[3];



        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
			.build();
        ImageLoader.getInstance().init(config);
        imageLoader = ImageLoader.getInstance();

        setContentView(R.layout.main_ui);

        // initialize views & drawables
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
        bpmView = findViewById(R.id.bpmView);

        titleView = findViewById(R.id.titleText);
        nowPlaying = findViewById(R.id.nowPlaying);

        debugMode = findViewById(R.id.debugMODE);
        debugMusic = findViewById(R.id.debugMUSIC);

        albumArt = findViewById(R.id.albumArt);

        // initialize pace & bpm values
        pace = 0;
        bpm = 120;

        PLAYLIST_SIZE = 20;

        if (!DEBUG) {
            debugMusic.setVisibility(View.INVISIBLE);
            debugMode.setVisibility(View.INVISIBLE);
        }

        // update the views
        updateViews();

        // initialize array of default playlist IDs
        predefinedSpotifyPlaylists = new ArrayList<String>();
            predefinedSpotifyPlaylists.add("1k704ydfoeesod05jv58rh3oe:0ze1Ymim1dpGKRlx9H8vC8");
            predefinedSpotifyPlaylists.add("dmxuazvp7p5fwjq7882m7uwd8:0mmqC3RIi8h7nApdD9gV7x");
            predefinedSpotifyPlaylists.add("1166998660:355AmRU0BBJtzr9hwupxcP");
            predefinedSpotifyPlaylists.add("nikewomen:13thjkLTYZmZvjdz4u6kxh");
            predefinedSpotifyPlaylists.add("spotify:37i9dQZF1DWZ2xRu8ajLOe");
            predefinedSpotifyPlaylists.add("sonymusicfinland:7gO9WmJaPmIviOlvK1m95P");



        // initialize booleans for finding pace
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

        System.out.println("WE HAVE RESUMED!");

        if (appRemote !=null && !appRemote.isConnected()){
            System.out.println("OK WE'RE GETTING THE APP REMOTE AGAIN...");
            getRemote();
        }


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
    }

    // Runs whenever there is a new sensor event
    // Collects the pace of the user
    @Override
    public void onSensorChanged(SensorEvent event) {

        // IF we're currently checking the User's Tempo...
        if (activityCounting) {
            currTime = System.currentTimeMillis();
            currSteps = event.values[0];

            if (!timing) { // if we aren't timing, set the initial time and step values...
                timing = true;
                prevTime = currTime;
                prevSteps = currSteps;

            } else { //when we are currently timing...

                paceView.setText((((currSteps - prevSteps) / (currTime - prevTime)) * 60000)
                        + " steps/min");

                if ((currTime - prevTime) >= TEMPO_TIME) { // if TEMPO_TIME milliseconds have elapsed, calculate steps per minute
                    activityCounting = false;
                    timing = false;
                    float stepsPerMinute = (((currSteps - prevSteps) / (currTime - prevTime)) * 60000);
                    tempo = stepsPerMinute;
                    // go ahead and make queue based on tempo.
                    makeQueue(stepsPerMinute);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Set Tempo Button has been pressed, go ahead and collect the user's tempo
    public void getSteps(View view) {
        Toast.makeText(this,"Recording Pace...", Toast.LENGTH_LONG).show();
        activityCounting = true;
        timing = false;
    }

    // Makes necessary calls to collect Track information from various sources...
    private void getMasterSongCollection(){
        //featuresTracks = new ArrayList<AudioFeaturesTrack>();
        //beatMap = new HashMap<String, Float>();

        ////NOTE////
        // Perhaps there should be separate List<AudioFeaturesTracks> for User's Tracks, User's Playlists, and the Predefined Playlists
        // All of them will be loaded at the outset, BUT...
        // Then only those catagories which the user has opted for will be considered (iterated through) during makeQueue()
        ////NOTE////

        GOT_USER_TRACKS = true;
        GOT_USER_PLAYLISTS = true;
        GOT_PREDEFINED = true;
        GOT_CATEGORY = true;


        if (USER_TRACKS){
            GOT_USER_TRACKS = false;
            userSavedBeatMap = new HashMap<String, Float>();
            getUserTracks();
        }

        if (USER_PLAYLISTS){
            GOT_USER_PLAYLISTS = false;
            userPlaylistBeatMap = new HashMap<String, Float>();
            getUserPlaylists();

        }

        if (PREDEFINED){
            GOT_PREDEFINED = false;
            predefinedBeatMap = new HashMap<String, Float>();
            predefinedFeaturesTracks = new ArrayList<AudioFeaturesTrack>();
            getPlaylists(predefinedSpotifyPlaylists, TYPE_PREDEFINED, predefinedBeatMap);
        }

        if (CATEGORY_PLAYLISTS){
            categoryBeatMap = new HashMap<String, Float>();
            GOT_CATEGORY = false;
            getCategoryPlaylists("workout");
        }


    }

    // Check if all relevant Track data has been acquired
    // then set mode to FIND_FACE and update views...
    private void checkGetMasterDone(){

        if (GOT_PREDEFINED && GOT_USER_PLAYLISTS && GOT_USER_TRACKS && GOT_CATEGORY) {
            MODE = FIND_PACE;
            button_CENTER.setVisibility(View.VISIBLE);
            updateViews();
        }
    }

    private void getUserPlaylists(){
        getUserPlaylists(new ArrayList<String>(), 0);
    }

    private void getUserPlaylists(final List<String> spotifyPlaylists, final int offset){
        userPlaylistFeaturesTracks = new ArrayList<AudioFeaturesTrack>();
        Map options = new HashMap<String, Object>();
        options.put("limit", 50);
        options.put("offset", offset);
        spotify.getPlaylists(USER_NAME, options, new SpotifyCallback<Pager<PlaylistSimple>>() {
            @Override
            public void failure(SpotifyError spotifyError) {

            }

            @Override
            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                for (PlaylistSimple playlist : playlistSimplePager.items){
                    spotifyPlaylists.add(playlist.owner+ ":" + playlist.id);
                }
                if (playlistSimplePager.total > 50 && playlistSimplePager.items.size() == 50){
                    getUserPlaylists(spotifyPlaylists, offset + 50);
                } else {
                    // we must have got through all of them so
                    System.out.println("ok, go get user's playlists...");
                    getPlaylists(spotifyPlaylists, TYPE_USER, userPlaylistBeatMap);
                }
            }
        });
    }

    private void getCategoryPlaylists(String category){
        categorySongIDs = new ArrayList<String>();
        categoryFeaturesTracks = new ArrayList<AudioFeaturesTrack>();
        List<String> spotifyPlaylists = new ArrayList<String>();
        getCategoryPlaylists(category, 0, spotifyPlaylists);
    }

    private void getCategoryPlaylists(final String category, final int offset, final List<String> spotifyPlaylists){
        //String category = "workout";
        //int offset = 0;

        Map options = new HashMap<String, Object>();
        options.put("country", "US");
        options.put("limit", 50);
        options.put("offset", offset);


        spotify.getPlaylistsForCategory(category, options, new SpotifyCallback<PlaylistsPager>() {
            @Override
            public void failure(SpotifyError spotifyError) {

            }

            @Override
            public void success(PlaylistsPager playlistsPager, Response response) {
                for (PlaylistSimple playlist : playlistsPager.playlists.items){
                    spotifyPlaylists.add(playlist.owner + ":" + playlist.id);
                }
                if (playlistsPager.playlists.total > 50 && playlistsPager.playlists.items.size() == 50){
                    getCategoryPlaylists(category, offset + 50, spotifyPlaylists);
                } else {
                    // we must have got through all of them so
                    System.out.println("ok, go get category playlists...");
                    getPlaylists(spotifyPlaylists, TYPE_CATEGORY, categoryBeatMap);
                }
            }
        });
    }

    // Gets Track data for the user's Saved Tracks
    private void getUserTracks(){
        userSongIDs = new ArrayList<String>();
        userSavedFeaturesTracks = new ArrayList<AudioFeaturesTrack>();
        spotify.getMySavedTracks(new SpotifyCallback<Pager<SavedTrack>>() {
            @Override
            public void failure(SpotifyError spotifyError) {
                System.out.println("Could not get user tracks!");
            }

            @Override
            public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                System.out.println("User Tracks size: " + savedTrackPager.items.size());
                for (SavedTrack track : savedTrackPager.items){
                    userSongIDs.add(track.track.id);
                }
                System.out.println("SongIDS size: " + userSongIDs.size());
                String result = "";
                for (int i = 0; i < userSongIDs.size(); i++){
                    result += userSongIDs.get(i) + ",";
                    System.out.println("i = " + i);
                    if (i % 99 == 0 || i == userSongIDs.size()-1){
                        System.out.println(result);
                        spotify.getTracksAudioFeatures(result, new SpotifyCallback<AudioFeaturesTracks>() {
                            @Override
                            public void failure(SpotifyError spotifyError) {
                                System.out.println("Could not get user tracks!");
                            }

                            @Override
                            public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                                // For each AudioFeaturesTrack, add it to the list....
                                for (AudioFeaturesTrack track : audioFeaturesTracks.audio_features) {

                                    //userSavedFeaturesTracks.add(track);
                                    userSavedBeatMap.put(track.uri, track.tempo);
                                    //beatMap.put(track.uri, track.tempo);
                                    // When we've reached the last Track, call checkGetMasterDone()...
                                    if (track.id.equals(userSongIDs.get(userSongIDs.size()-1))){
                                        System.out.println("Got User Tracks!");
                                        GOT_USER_TRACKS = true;
                                        checkGetMasterDone();
                                    }
                                }
                            }
                        });
                        result = "";
                    }
                }

            }
        });
    }

    // Gets Track data from the predefined collection of work-out playlists
    private void getPlaylists(final List<String> spotifyPlaylists, final int PLAYLIST_TYPE, final Map<String, Float> currentBeatMap){//final List<AudioFeaturesTrack> featuresList){

        final List<Playlist> playlists = new ArrayList<Playlist>();
        //featuresTracks = new ArrayList<AudioFeaturesTrack>();
        //beatMap = new HashMap<String, Float>();
        final List<String> spotifySongs = new ArrayList<String>();



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
                        String result = "";
                        for (int j = 0; j < spotifySongs.size(); j++){
                            String songToAdd = spotifySongs.get(j);
                            if (songToAdd != null) result += songToAdd+",";

                            if (j % 99 == 0 || j == spotifySongs.size()-1){
                                spotify.getTracksAudioFeatures(result, new SpotifyCallback<AudioFeaturesTracks>() {
                                    @Override
                                    public void failure(SpotifyError spotifyError) {

                                    }

                                    @Override
                                    public void success(AudioFeaturesTracks audioFeaturesTracks, Response response) {
                                        System.out.println(PLAYLIST_TYPE);
                                        for (AudioFeaturesTrack track : audioFeaturesTracks.audio_features){
                                            if (track != null) {
                                                //featuresList.add(track);
                                                currentBeatMap.put(track.uri, track.tempo);
                                                //beatMap.put(track.uri, track.tempo);
                                                // if we've processed the last AudioFeaturesTrack...
                                                if (track.id.equals(spotifySongs.get(spotifySongs.size() - 1))) {
                                                    if (PLAYLIST_TYPE == TYPE_PREDEFINED) {
                                                        System.out.println("Got Predefined!");
                                                        GOT_PREDEFINED = true;
                                                    }
                                                    if (PLAYLIST_TYPE == TYPE_CATEGORY) {
                                                        System.out.println("Got Category!");
                                                        GOT_CATEGORY = true;
                                                    }
                                                    if (PLAYLIST_TYPE == TYPE_USER) {
                                                        System.out.println("Got Users Playlists!");
                                                        GOT_USER_PLAYLISTS = true;
                                                    }

                                                    checkGetMasterDone();
                                                    //MODE = FIND_PACE;
                                                    //button_CENTER.setVisibility(View.VISIBLE);
                                                    //updateViews();
                                                }
                                            }
                                        }
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

        Map<String, Float> combinedBeatMap = new HashMap<String, Float>();

        System.out.println("Map Size: " + combinedBeatMap.size());
        if (USER_TRACKS) combinedBeatMap.putAll(userSavedBeatMap);
        System.out.println("Map Size: " + combinedBeatMap.size());
        if (CATEGORY_PLAYLISTS) combinedBeatMap.putAll(categoryBeatMap);
        System.out.println("Map Size: " + combinedBeatMap.size());
        if (PREDEFINED) combinedBeatMap.putAll(predefinedBeatMap);
        System.out.println("Map Size: " + combinedBeatMap.size());
        if (USER_PLAYLISTS) combinedBeatMap.putAll(userPlaylistBeatMap);
        System.out.println("Map Size: " + combinedBeatMap.size());

        beatMap = combinedBeatMap;

        List<String> songQueue = new ArrayList<String>();

        for (String URI : combinedBeatMap.keySet()){
            float trackTempo = combinedBeatMap.get(URI);
            if (trackTempo >= tempo-MARGIN && trackTempo <= tempo+MARGIN){
                songQueue.add(URI);
            }
        }

        pace = tempo;
        if (!songQueue.isEmpty()){
            makePlaylist(songQueue, tempo);
        } else {
            MODE = 4;
            updateViews();
            Toast.makeText(this, "Sorry, No Songs Match That Tempo!", Toast.LENGTH_LONG).show();
        }

    }

   // Create a new playlist on the user's account before adding tracks to it...
    private void makePlaylist(final List<String> songs, float tempo){

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

   // Adds songs to a Spotify Playlist based on a List of AudioFeaturesTracks
   private void addSongsToPlaylist(List<String> songs, final Playlist playlist){
       Collections.shuffle(songs);
       String uris = "";

       int max = PLAYLIST_SIZE;
       if (songs.size() < max) max = songs.size();

       for (int i = 0; i < max; i++){
           uris += songs.get(i) + ",";
       }

       Map<String, Object> map1 = new HashMap<String, Object>();
       map1.put("uris", uris);

       // Seems the second map field in addTracksToPlayList can't just be null... so we create this.
       Map<String, Object> map2 = new HashMap<String, Object>();
       map2.put("uris", new JSONArray());

       spotify.addTracksToPlaylist(USER_NAME, playlist.id, map1, map2, new SpotifyCallback<Pager<PlaylistTrack>>() {
           @Override
           public void failure(SpotifyError spotifyError) {
                MODE = FIND_PACE;
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

    // Fetches the authorization token for Spotify.
    // Ultimately calls getPlaylists() to get the pre-defined playlists THIS SHOULD BE AMENDED TO ACCOUNT FOR SONGS IN THE USER'S LIBRARY
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
                    //getPlaylists();
                    getMasterSongCollection();
                }
            });
        }
    }


    // Sets up the App-Remote & Subscribe to PlayerState
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
                        if (MODE != TIMING && MODE != STARTUP && playerState.isPaused) MODE = PAUSED;
                            else if (MODE != TIMING && MODE != STARTUP) MODE = PLAYING;
                        updateViews();

                        nowPlaying.setText(playerState.track.name + " by " + playerState.track.artist.name + " - BPM:" + beatMap.get(playerState.track.uri));
                        System.out.println(playerState.track.name + " by " + playerState.track.artist.name);

                        spotify.getTrack(playerState.track.uri.split(":")[2], new SpotifyCallback<Track>() {
                            @Override
                            public void failure(SpotifyError spotifyError) {

                            }

                            @Override
                            public void success(Track track, Response response) {
                                imageLoader.displayImage(track.album.images.get(0).url, albumArt);
                            }
                        });
                    }
                });
            }


            @Override
            public void onFailure(Throwable throwable) {

            }
        });


    }

    @Override
    protected void onStop() {
        super.onStop();
        //SpotifyAppRemote.disconnect(appRemote);
    }

    /////////////////////////////////////////

    // Update all the UI views...
    private void updateViews(){

        debugMusic.setText("Music = " + MUSIC);
        debugMode.setText("MODE = " + MODE);

        paceView.setText("PACE: " + pace);
        bpmView.setText(bpm + " bmp");

        if (MODE == STARTUP){
            titleView.setText("TEMPO MATCH");
            albumArt.setVisibility(View.INVISIBLE);
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

            nowPlaying.setVisibility(View.INVISIBLE);
            albumArt.setVisibility(View.INVISIBLE);

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
            albumArt.setVisibility(View.VISIBLE);

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
            albumArt.setVisibility(View.INVISIBLE);

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

    // Handle Center Button clicks...
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

    // Handle Set Pace Button clicks
    public void onSetClicked(View view) {

        if (MODE == SET_PACE){ //if mode already is SET (play button is in lower right)

            setPlayMode();
        } else {
            MODE = SET_PACE;
        }
        updateViews();
    }

    // Handle Find Pace Button clicks
    public void onFindClicked(View view) {

        if (MODE == FIND_PACE){

            setPlayMode();
        } else {
            MODE = FIND_PACE;
        }
        updateViews();
    }

    // Handle Next Button clicks...
    public void onNextClicked(View view) {
        if (MODE == SET_PACE){
            bpm++;
            bpmView.setText((bpm) + " bpm");
        } else if (MODE == PLAYING || MODE == PAUSED) {
            appRemote.getPlayerApi().skipNext();
            if (MODE == PAUSED) appRemote.getPlayerApi().pause();
        }
    }

    // Handle Previous Button clicks...
    public void onPrevClicked(View view) {
        if (MODE == SET_PACE){
            bpm--;
            bpmView.setText((bpm) + " bpm");
        } else if (MODE == PLAYING || MODE == PAUSED){
            appRemote.getPlayerApi().skipPrevious();
            if (MODE == PAUSED) appRemote.getPlayerApi().pause();
        }
    }

    // Sets mode to either PAUSED or PLAYING depending on the Spotify PlayerSatate
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
