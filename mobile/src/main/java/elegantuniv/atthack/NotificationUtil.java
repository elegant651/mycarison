package elegantuniv.atthack;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class NotificationUtil {

    public static final int WEAR_NOTIFICAITON_ID = 1001;
    private static final int PLAY_N_PAUSE_ID = 4327;

    public static PendingIntent getNullIntent(Context context, String url){
        Log.d("atthack", "nullIntent");

        Intent intent = new Intent(context, SensordroneControl.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("isIgnore", true);
        intent.putExtra("tst", 1);
        return PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getPostIntent(Context context, String url){
        Log.d("atthack", "postIntent");

        Intent intent = new Intent(context, SensordroneControl.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("isIgnore", false);
        intent.putExtra("tst", 1);
        return PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    public static PendingIntent getPhoneCallIntent(Context context, String phoneNum){
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNum));
        return PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

//    public static PendingIntent getChangeMoviePendingIntent(Context context, long nextMovieId) {
//        Intent intent = new Intent(context, PlayerActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra(MovieList.ARG_ITEM_ID, nextMovieId);
//        intent.putExtra(context.getString(R.string.should_start), true);
//        return PendingIntent.getActivity(context, (int)nextMovieId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//    }
//
//    public static PendingIntent getPlayOrPausePendingIntent(
//            PlayerActivity context, PlayerActivity.PlaybackState playstate) {
//        Intent intent = new Intent(context, PlayerActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        switch(playstate){
//            case PLAYING:
//            case BUFFERING:
//                intent.putExtra("pause", true);
//                break;
//            default:
//                intent.putExtra("play", true);
//                break;
//        }
//        return PendingIntent.getActivity(context, PLAY_N_PAUSE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//    }
}