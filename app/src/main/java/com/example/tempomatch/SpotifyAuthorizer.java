package com.example.tempomatch;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class SpotifyAuthorizer extends Activity {

    public static String USER_NAME;
    public static final String CLIENT_ID = "56876e9226e34cb6a4461d3877309b75";
    public static final String REDIRECT_URI = "spotifyplaylists://callback";
    public static final int AUTH_TOKEN_REQUEST_CODE = 1987;
    private String mAccessToken;

    public SpotifyAuthorizer(){
        fetchToken();
    }

    public String getToken(){
        return mAccessToken;
    }

    public void fetchToken(){
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
        }
    }

}
