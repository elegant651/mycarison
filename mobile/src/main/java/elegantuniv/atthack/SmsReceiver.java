package elegantuniv.atthack;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

    private Context mContext;

    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        if (bundle != null)
        {
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i=0; i<msgs.length; i++){
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                str += "SMS from " + msgs[i].getOriginatingAddress();
                str += " :";
                str += msgs[i].getMessageBody().toString();
                str += "\n";
            }
            Toast.makeText(context, str, Toast.LENGTH_LONG).show();
            Log.e("atthack", str);

            postWearNotification1(str);

            this.abortBroadcast();
        }
    }

    private void postWearNotification1(String str){
        if(mContext==null){
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentTitle(str)
                .setContentText("where")
                .setSmallIcon(R.drawable.fine_car_ic)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.car_test));
//                .setLargeIcon(R.drawable.ic_launcher);

        NotificationCompat.WearableExtender wearableOptions = new NotificationCompat.WearableExtender();
        wearableOptions.setDisplayIntent(NotificationUtil.getNullIntent(mContext, str));
        wearableOptions.setContentIcon(R.drawable.ic_pause_playcontrol_normal);
        wearableOptions.setContentIconGravity(Gravity.RIGHT|Gravity.BOTTOM);

        NotificationCompat.Action callPost = new NotificationCompat.Action.Builder(
                R.drawable.ic_playnstop, "call",
                NotificationUtil.getPostIntent(mContext, str)).build();

        NotificationCompat.Action ignoreAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_pause_playcontrol_normal, "ignore",
                NotificationUtil.getNullIntent(mContext, str)
        ).build();

        wearableOptions.addAction(callPost).addAction(ignoreAction);

        builder.extend(wearableOptions);

        Notification notification = builder.build();
        NotificationManagerCompat.from(mContext).notify(NotificationUtil.WEAR_NOTIFICAITON_ID, notification);
    }
}
