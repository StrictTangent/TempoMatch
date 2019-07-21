# TempoMatch

Android app utilizing Spotify's Android SDK to generate playlists of songs with Beats Per Minute (BPM) matching the pace of a user's run/walk. 

![alt text](/images/screenshots.png)

TO USE:

Clone this repository with Android Studio: https://developer.android.com/studio

Create a new Client ID on your Spotify Developer Dashboard: https://developer.spotify.com/dashboard/login

On the page for your app on your Spotify Developer Dashboard, go to EDIT SETTINGS. Set the Redirect URI to: playlistgenerator://callback

Add an Android Package with your package name (com.example.tempomatch) AND SHA1 Fingerprint. This can be obtained running the signingReport in Android Studio:

Click on Gradle (top right) > app > Tasks > android > signingReport

In MainActivity.java you must change the field:

public static final String CLIENT_ID = "56876e9226e34cb6a4461d3877309b75"; to be the client ID in your own Spotify Developer Dashboard.

RESOURCES:

Libraries:
https://github.com/spotify/android-sdk/releases
https://github.com/kaaes/spotify-web-api-android
https://github.com/nostra13/Android-Universal-Image-Loader

Artwork: https://www.flaticon.com/authors/smashicons
