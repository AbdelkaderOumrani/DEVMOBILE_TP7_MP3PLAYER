package com.kadiomr.mp3player;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.time.LocalTime;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    EditText audioUrl;
    Button downloadButton;
    Uri uri;
    ConditionVariable mCondition;
    MediaPlayer mediaPlayer;
    ImageView playPauseButton;
    String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadButton = (Button) findViewById(R.id.downloadButton);
        audioUrl = (EditText) findViewById(R.id.audioUrl);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uri = Uri.parse(audioUrl.getText().toString());
                new DownloadTask().execute(uri);
            }
        });
    }

    public static final long MAGIC=86400000L;

    public int DateToDays (){
        //  convert a date to an integer and back again
        Date date = new Date();
        long currentTime=date.getTime();
        currentTime=currentTime/MAGIC;
        return (int) currentTime;
    }

    private class DownloadTask extends AsyncTask<Uri, Integer, Integer> {
        @Override
        protected Integer doInBackground(Uri... uris) {
            int count = uris.length;
            Log.v("download_URL",uri.toString());
            for (int i = 0; i < count; i++) {
                DownloadData(uris[i], i);
                publishProgress(1);
            }
            return count;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.v("progress_bar",values[0].toString());
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), " Start Downloading ...",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Integer s) {
            Toast.makeText(getApplicationContext(), s + " Files Downloaded ",
                    Toast.LENGTH_SHORT).show();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            mediaPlayer = MediaPlayer.create(getApplicationContext(),Uri.fromFile(file));
            playPauseButton = (ImageView) findViewById(R.id.playPauseButton);
            playPauseButton.setVisibility(View.VISIBLE);
            playPauseButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        playPauseButton.setImageResource(R.drawable.play);
                    }
                    else{
                        mediaPlayer.start();
                        playPauseButton.setImageResource(R.drawable.pause);
                    }
                }
            });

        }

        private void DownloadData(Uri uri, int i) {

            DownloadManager downloadmanager = (DownloadManager)
                    getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(fileName);
            request.setDescription(uri.toString());
            fileName = "audio" + DateToDays() + ".mp3";
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            final long downloadId = downloadmanager.enqueue(request);
            Log.v("Download_ID",String.valueOf(downloadId));
            mCondition = new ConditionVariable(false);

            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (downloadId == reference) {
                        mCondition.open();
                    }
                }
            };
            getApplicationContext().registerReceiver(receiver, filter);
            mCondition.block();
        }
    }
}