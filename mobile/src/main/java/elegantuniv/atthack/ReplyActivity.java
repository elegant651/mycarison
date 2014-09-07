package elegantuniv.atthack;



import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ReplyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply);

        final String EXTRA_VOICE_REPLY = "extra_voice_reply";
        Bundle remoteInput = RemoteInput.getResultsFromIntent(getIntent());
        if(null != remoteInput) {
            TextView text = (TextView) findViewById(R.id.reply_text);
            text.setText(String.format("You replied: %s", remoteInput.getCharSequence(EXTRA_VOICE_REPLY)));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
