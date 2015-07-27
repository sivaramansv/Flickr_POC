package ssivaram.cmu.flickr_poc;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.GeoData;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import com.google.android.gms.maps.model.LatLng;

public class LazyAdapter extends BaseAdapter {

    private Activity activity;
    private PhotoList photos;
    private static LayoutInflater inflater=null;
    private String oauthToken;
    private String oauthTokenSecret;

    public LazyAdapter(Activity a, PhotoList d,String oauthToken, String oauthTokenSecret) {
        activity = a;
        photos = d;
        this.oauthToken=oauthToken;
        this.oauthTokenSecret=oauthTokenSecret;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return photos.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int count=getCount();
        Log.i("Photos count GetView:",Integer.toString(getCount()));
        View vi = convertView;
        if(convertView == null)
            vi = inflater.inflate(R.layout.row, null);
        /*Flickr f = FlickrHelper.getInstance().getFlickrAuthed(oauthToken,
                oauthTokenSecret);*/

        TextView text=(TextView)vi.findViewById(R.id.textView);;
        ImageView image=(ImageView)vi.findViewById(R.id.imageView);
        Photo photo = photos.get(position);

       /* GeoData geoData= null;
        try {
            geoData = f.getGeoInterface().getLocation(photo.getId());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FlickrException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("Geodata",Float.toString(geoData.getLatitude()));*/
        //new GetLocationTask(activity,text,oauthToken,oauthTokenSecret).execute(photo.getId());
        if (image != null){
            ImageDownloadTask task = new ImageDownloadTask(image);
            Drawable drawable = new ImageUtils.DownloadedDrawable(task);
            image.setImageDrawable(drawable);
            task.execute(photo.getSmallSquareUrl());
        }


        Log.i("Photo", photo.getSmallUrl());
        //text.setText(photo.getTitle());
        Log.i("Photo", photo.getId());
        //new DownloadImageTask(image).execute(photo.getSmallUrl());
        return vi;
    }
}
//Download image task
class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}
//Get Location Task
class GetLocationTask extends AsyncTask<String, Void, GeoData> {
    TextView textView;
    String oauthToken;
    String oauthTokenSecret;
    Activity a;
    public GetLocationTask(Activity activity,TextView textView,String oauth,String oauthTokenSecret) {
        this.a=activity;
        this.textView=textView;
        this.oauthToken=oauth;
        this.oauthTokenSecret=oauthTokenSecret;
    }

    protected GeoData doInBackground(String... photoID) {

         GeoData geoData= null;
        try {
            Flickr f = FlickrHelper.getInstance().getFlickrAuthed(oauthToken,
                    oauthTokenSecret);
            geoData = f.getGeoInterface().getLocation(photoID[0]);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FlickrException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
       // Log.i("Geodata",Float.toString(geoData.getLatitude()));
        return geoData;

    }

    protected void onPostExecute(GeoData geoData) {
        if(geoData!=null)
        {
            Log.i("Geodata", Float.toString(geoData.getLatitude()));
            LatLng latLng=new LatLng(geoData.getLatitude(),geoData.getLongitude());
            Address addressLocation=LocationUtils.getAddressFromLocation(a,latLng);
            textView.setText(addressLocation.getAddressLine(0));
        }
    }
}
