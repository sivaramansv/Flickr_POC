package ssivaram.cmu.flickr_poc;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.GridView;
import android.widget.Toast;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.auth.Permission;
import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.oauth.OAuthToken;
import com.googlecode.flickrjandroid.people.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    public static final String CALLBACK_SCHEME = "flickrj-android-sample-oauth"; //$NON-NLS-1$
    public static final String PREFS_NAME = "flickrj-android-sample-pref"; //$NON-NLS-1$
    public static final String KEY_OAUTH_TOKEN = "flickrj-android-oauthToken"; //$NON-NLS-1$
    public static final String KEY_TOKEN_SECRET = "flickrj-android-tokenSecret"; //$NON-NLS-1$
    public static final String KEY_USER_NAME = "flickrj-android-userName"; //$NON-NLS-1$
    public static final String KEY_USER_ID = "flickrj-android-userId"; //$NON-NLS-1$
    static final String PREF_KEY_LOGIN = "isLoggedIn";

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);
    GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gridView=(GridView)findViewById(R.id.grid_view);

        OAuth oauth = getOAuthToken();

        if(!isLoggedInAlready()) {

            Toast.makeText(getApplicationContext(),"Not logged in",Toast.LENGTH_LONG).show();
            if (oauth == null || oauth.getUser() == null) {
                new OAuthTask(this).execute();
            }
        }
        else {

            Toast.makeText(getApplicationContext(),"Logged in",Toast.LENGTH_LONG).show();
            load(oauth);
        }

    }
    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isLoggedInAlready() {
        // return twitter login status from Shared Preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean(PREF_KEY_LOGIN, false);
    }

    public OAuth getOAuthToken() {
        //Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String oauthTokenString = settings.getString(KEY_OAUTH_TOKEN, null);
        String tokenSecret = settings.getString(KEY_TOKEN_SECRET, null);

        if (oauthTokenString == null && tokenSecret == null) {
            logger.warn("No oauth token retrieved"); //$NON-NLS-1$
            return null;
        }

        OAuth oauth = new OAuth();
        String userName = settings.getString(KEY_USER_NAME, null);
        String userId = settings.getString(KEY_USER_ID, null);

        if (userId != null) {
            User user = new User();
            Log.i("User id",userId);
            user.setUsername(userName);
            user.setId(userId);
            oauth.setUser(user);
        }
        else
        {
            Log.i("User id:","null");
        }
        OAuthToken oauthToken = new OAuthToken();
        oauth.setToken(oauthToken);
        oauthToken.setOauthToken(oauthTokenString);
        oauthToken.setOauthTokenSecret(tokenSecret);
        logger.debug("Retrieved token", oauthTokenString, tokenSecret); //$NON-NLS-1$
        return oauth;
    }
    private void load(OAuth oauth) {
        if (oauth != null) {

                Log.i("Loaded","Hell yeah!");
                Log.i("Oauth",oauth.getToken().toString());
                 Log.i("Oauth",oauth.getToken().toString());
                new LoadPhotoStreamTask(this,gridView).execute(oauth);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.i("On", "Resume!");
        Intent intent = getIntent();
        String scheme = intent.getScheme();
        OAuth savedToken = getOAuthToken();
        if (CALLBACK_SCHEME.equals(scheme) && (savedToken == null || savedToken.getUser() == null)) {
            Uri uri = intent.getData();
            String query = uri.getQuery();
            logger.debug("Returned Query: {}", query); //$NON-NLS-1$
            String[] data = query.split("&"); //$NON-NLS-1$
            if (data != null && data.length == 2) {
                String oauthToken = data[0].substring(data[0].indexOf("=") + 1); //$NON-NLS-1$
                String oauthVerifier = data[1]
                        .substring(data[1].indexOf("=") + 1); //$NON-NLS-1$
                logger.debug("OAuth Token: {}; OAuth Verifier: {}", oauthToken, oauthVerifier); //$NON-NLS-1$

                OAuth oauth = getOAuthToken();
                if (oauth != null && oauth.getToken() != null && oauth.getToken().getOauthTokenSecret() != null) {
                    GetOAuthTokenTask task = new GetOAuthTokenTask(this);
                    task.execute(oauthToken, oauth.getToken().getOauthTokenSecret(), oauthVerifier);
                }
            }
        }

    }
    @Override
    protected void onNewIntent(Intent intent) {
        //this is very important, otherwise you would get a null Scheme in the onResume later on.
        setIntent(intent);
    }
    public void saveOAuthToken(String userName, String userId, String token, String tokenSecret) {
        logger.debug("Saving userName=%s, userId=%s, oauth token={}, and token secret={}", new String[]{userName, userId, token, tokenSecret}); //$NON-NLS-1$

        SharedPreferences sp = getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_OAUTH_TOKEN, token);
        editor.putString(KEY_TOKEN_SECRET, tokenSecret);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_ID, userId);
        editor.putBoolean(PREF_KEY_LOGIN, true);
        editor.commit();
    }
    public void onOAuthDone(OAuth result) {
        Log.i("OAD","called");
        if (result == null) {
            Toast.makeText(this,
                    "Authorization failed", //$NON-NLS-1$
                    Toast.LENGTH_LONG).show();
        } else {
            User user = result.getUser();
            OAuthToken token = result.getToken();
            if (user == null || user.getId() == null || token == null
                    || token.getOauthToken() == null
                    || token.getOauthTokenSecret() == null) {
                Toast.makeText(this,
                        "Authorization failed", //$NON-NLS-1$
                        Toast.LENGTH_LONG).show();
                return;
            }
            String message = String.format(Locale.US, "Authorization Succeed: user=%s, userId=%s, oauthToken=%s, tokenSecret=%s", //$NON-NLS-1$
                    user.getUsername(), user.getId(), token.getOauthToken(), token.getOauthTokenSecret());
            Toast.makeText(this,
                    message,
                    Toast.LENGTH_LONG).show();
            saveOAuthToken(user.getUsername(), user.getId(), token.getOauthToken(), token.getOauthTokenSecret());
            load(result);
        }
    }
}
    class OAuthTask extends AsyncTask<Void, Integer, String> {


    private static final Uri OAUTH_CALLBACK_URI = Uri.parse(MainActivity.CALLBACK_SCHEME
            + "://oauth"); //$NON-NLS-1$

    /**
     * The context.
     */
    private Context mContext;

    /**
     * The progress dialog before going to the browser.
     */
    private ProgressDialog mProgressDialog;

    /**
     * Constructor.
     *
     * @param context
     */
    public OAuthTask(Context context) {
        super();
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = ProgressDialog.show(mContext,
                "", "Generating the authorization request..."); //$NON-NLS-1$ //$NON-NLS-2$
        mProgressDialog.setCanceledOnTouchOutside(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected String doInBackground(Void... params) {
        try {
            Flickr f = FlickrHelper.getInstance().getFlickr();
            OAuthToken oauthToken = f.getOAuthInterface().getRequestToken(
                    OAUTH_CALLBACK_URI.toString());
            saveTokenSecrent(oauthToken.getOauthTokenSecret());
            URL oauthUrl = f.getOAuthInterface().buildAuthenticationUrl(
                    Permission.READ, oauthToken);
            return oauthUrl.toString();
        } catch (Exception e) {

            return "error:" + e.getMessage(); //$NON-NLS-1$
        }
    }

        /**
         * Saves the oauth token secrent.
         *
         * @param tokenSecret
         */
        private void saveTokenSecrent(String tokenSecret) {
            MainActivity act = (MainActivity) mContext;
            act.saveOAuthToken(null, null, null, tokenSecret);
        }

        @Override
        protected void onPostExecute(String result) {
            final Dialog auth_dialog;
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
        }
            if (result != null && !result.startsWith("error") ) { //$NON-NLS-1$
            Log.i("Result",Uri.parse(result).toString());
            Log.i("On post","entered");
            /*auth_dialog = new Dialog(mContext);
            auth_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            auth_dialog.setContentView(R.layout.twitter_webview);
            WebView web = (WebView)auth_dialog.findViewById(R.id.webv);
            web.getSettings().setJavaScriptEnabled(true);
            web.loadUrl(result);
            web.setWebViewClient(new WebViewClient() {
                boolean authComplete = false;

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (url.contains("oauth_verifier") && !authComplete) {
                        authComplete = true;
                        Uri uri = Uri.parse(url);
                        //oauth_verifier = uri.getQueryParameter("oauth_verifier");
                        auth_dialog.dismiss();

                    } else if (url.contains("denied")) {
                        auth_dialog.dismiss();
                        Toast.makeText(mContext, "Sorry !Permission Denied", Toast.LENGTH_SHORT).show();

                    }
                }
            });
            auth_dialog.show();
            auth_dialog.setCancelable(true);
            */    mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(result)));

        } else {
            Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
        }
    }

}