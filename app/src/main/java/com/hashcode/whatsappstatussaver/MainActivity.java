package com.hashcode.whatsappstatussaver;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.hashcode.whatsappstatussaver.data.StatusSavingService;
import com.hashcode.whatsappstatussaver.views.GlideApp;
import com.hashcode.whatsappstatussaver.views.StatusListAdapter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, StatusListAdapter.StatusClickListener{
    Context mContext;
    ArrayList<String> allStatusPaths;
    ArrayList<String> selectedStatuses;
    SwipeRefreshLayout swipeRefreshLayout;
    final int MY_PERMISSION_REQUEST_WRITE_STORAGE = 100;

    //Using ListView
    GridView mGridView;
    StatusListAdapter statusListAdapter;

    private final static String ACTION_FETCH_STATUS = "fetch-status";
    private final static String ACTION_SAVE_STATUS = "save-status";

    private FetchStatusReceiver fetchStatusReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(FetchStatusReceiver.PROCESS_FETCH);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        fetchStatusReceiver = new FetchStatusReceiver();
        registerReceiver(fetchStatusReceiver, filter);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = getBaseContext();
        allStatusPaths = new ArrayList<>();
        selectedStatuses = new ArrayList<>();
        mGridView = (GridView) findViewById(R.id.status_grid_view);
        mGridView.setColumnWidth(3);
        statusListAdapter = new StatusListAdapter(mContext,allStatusPaths);

        statusListAdapter.setStatusClickListener(this);
        //Button to save the statuses.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(statusListAdapter.getSelectedStatuses().size() != 0){
                    StatusSavingService.performSave(mContext,statusListAdapter.getSelectedStatuses());
                    statusListAdapter.mCheckStates.clear();
                    Snackbar.make(view, "Statuses saved", Snackbar.LENGTH_LONG).show();
                    statusListAdapter.setSelectedStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    StatusSavingService.performFetch(mContext);
                }
                else{
                    Snackbar.make(view, "No status selected", Snackbar.LENGTH_LONG).show();
                }

            }
        });
        swipeRefreshLayout.setOnRefreshListener(this);
        askForContactPermission();

    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(fetchStatusReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            swipeRefreshLayout.setRefreshing(true);
            StatusSavingService.performFetch(mContext);
            return true;
        }
        else if(id == R.id.action_help){
            showHelpPopup(MainActivity.this ,new Point(0,2));
            return true;
        }
        else if(id == R.id.action_share){
            String mimeType="text/plain";
            String title = "Share  WhatsApp Status Saver App";
            ShareCompat.IntentBuilder.from(this)
                    .setType(mimeType)
                    .setChooserTitle(title)
                    .setText(getResources().getString(R.string.share_text))
                    .startChooser();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        StatusSavingService.performFetch(mContext);

    }

    @Override
    public void onStatusLongClick(int position, String url) {
        showImagePopup(new Point(0,2),url);
    }

    /*
        Receiver for displaying the status after the background fetch.
     */
    public class FetchStatusReceiver extends BroadcastReceiver{
        public static final String PROCESS_FETCH = "setup-all-views";
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hasWhatsApp = intent.getBooleanExtra("the-user-has-whatsapp",true);
            if(!hasWhatsApp){
                Snackbar.make(mGridView,"Sorry, you do not have WhatsApp Installed",Snackbar.LENGTH_LONG).show();
            }
            else {
                ArrayList<String> receivedStatus = intent.getStringArrayListExtra(StatusSavingService.FETCHED_STATUSES);
                statusListAdapter.setFolderPath(intent.getStringExtra(StatusSavingService.FOLDER_PATH));
//            statusListAdapter.swapStatus(receivedStatus);
                mGridView.setAdapter(statusListAdapter);
                statusListAdapter.swapStatus(receivedStatus);
                //Setting up the recycler view
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }


    private void showHelpPopup(final Activity context, Point p) {
        // Inflate the popup_layout.xml
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.help_popup_layout);
        dialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.copyFrom(dialog.getWindow().getAttributes());
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        // Creating the PopupWindow

        // Clear the default translucent background

        // Displaying the popup at the specified location, + offsets.

        // Getting a reference to Close button, and close the popup when clicked.
        FloatingActionButton close = (FloatingActionButton) dialog.findViewById(R.id.close_help_popup_button);
        close.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
    }

    public void showImagePopup(Point p, final String uri) {
        Activity context = MainActivity.this;
        //COMPLETED solving video problem

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.image_popup_layout);
        dialog.show();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.copyFrom(dialog.getWindow().getAttributes());
        dialog.getWindow().setAttributes(lp);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));


        // Getting a reference to Close button, and close the popup when clicked.
        FloatingActionButton close = (FloatingActionButton) dialog.findViewById(R.id.close_image_popup_button);
        ImageView statusImage = (ImageView) dialog.findViewById(R.id.full_status_image_view);
        final SimpleExoPlayerView simpleExoPlayerView = dialog.findViewById(R.id.full_status_video_view);
        final SimpleExoPlayer player ;
        if(uri.endsWith(".jpg")){
            GlideApp.with(context).load(uri).fitCenter().into(statusImage);
        }
        else if(uri.endsWith(".mp4")){
            statusImage.setVisibility(View.GONE);
            simpleExoPlayerView.setVisibility(View.VISIBLE);
            Uri myUri = Uri.parse(uri); // initialize Uri here

            // 1. Create a default TrackSelector
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

// 2. Create a default LoadControl
            LoadControl loadControl = new DefaultLoadControl();

// 3. Create the player
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

//Set media controller
            simpleExoPlayerView.setUseController(true);
            simpleExoPlayerView.requestFocus();

// Bind the player to the view.
            simpleExoPlayerView.setPlayer(player);

            //Measures bandwidth during playback. Can be null if not required.
            DefaultBandwidthMeter bandwidthMeterA = new DefaultBandwidthMeter();
//Produces DataSource instances through which media data is loaded.
            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.
                    getUserAgent(this, "exoplayer2example"), bandwidthMeterA);
//Produces Extractor instances for parsing the media data.
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

            MediaSource videoSource = new ExtractorMediaSource(myUri, dataSourceFactory, extractorsFactory, null, null);
            player.prepare(videoSource);
            player.setPlayWhenReady(true); //run file/link when ready to play.
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    player.release();
                }
            });

        }
        close.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                popup.dismiss();
                dialog.cancel();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    public void askForContactPermission(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(permissionCheck != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_WRITE_STORAGE);

            }else{
                StatusSavingService.performFetch(mContext);
            }
        }
        else{
            StatusSavingService.performFetch(mContext);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_WRITE_STORAGE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    StatusSavingService.performFetch(mContext);
                }
                else {
                    Toast.makeText(getBaseContext(),"Sorry, This app cannot work on your device",Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this,MainActivity.class));
                    finish();
                }
                return;
        }
    }
}
