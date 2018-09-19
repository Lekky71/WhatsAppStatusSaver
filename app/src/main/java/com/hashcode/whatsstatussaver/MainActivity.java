package com.hashcode.whatsstatussaver;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import com.hashcode.whatsstatussaver.data.StatusSavingService;
import com.hashcode.whatsstatussaver.floatingbutton.FloatingButtonService;
import com.hashcode.whatsstatussaver.views.FloatAdapter;
import com.hashcode.whatsstatussaver.views.GlideApp;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener,
        FloatAdapter.StatusClickListener {
    private final static String ACTION_FETCH_STATUS = "fetch-status";
    private final static String ACTION_SAVE_STATUS = "save-status";
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private static final String TAG = MainActivity.class.getSimpleName();
    final int MY_PERMISSION_REQUEST_WRITE_STORAGE = 100;
    Context mContext;
    ArrayList<String> allStatusPaths;
    ArrayList<String> selectedStatuses;
    String bottomSelected = "pictures";
    //ArrayList containing the videos and the pictures
    ArrayList<String> allPicturePaths;
    ArrayList<String> allVideoPaths;
    SwipeRefreshLayout swipeRefreshLayout;
    //Using ListView
    RecyclerView mRecyclerView;
    BottomNavigationView navigation;
    FloatAdapter statusListAdapter;
    int cells = 3;
    //SharedPref
    boolean isFloatingAllowed;
    private FetchStatusReceiver fetchStatusReceiver;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pictures:
                    statusListAdapter.swapStatus(allPicturePaths);
                    bottomSelected = "pictures";
                    mergeVideoButton.setVisibility(View.GONE);
                    return true;
                case R.id.navigation_videos:
                    bottomSelected = "videos";
                    statusListAdapter.swapStatus(allVideoPaths);
                    mergeVideoButton.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }

    };
    private int orientation;

    SharedPreferences sharedPreferences;

    //The video merge functionality
    FloatingActionButton mergeVideoButton;
    //statusListAdapter.getSelectedVidoesStatuses() will be used to collect the paths to all the videos to be merged together.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("settings-pref", MODE_PRIVATE);
        isFloatingAllowed = sharedPreferences.getBoolean("floating-allowed", false);
        orientation = this.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            cells = 5;
        }

        IntentFilter filter = new IntentFilter(FetchStatusReceiver.PROCESS_FETCH);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        fetchStatusReceiver = new FetchStatusReceiver();
        registerReceiver(fetchStatusReceiver, filter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = getApplicationContext();
        allStatusPaths = new ArrayList<>();
        selectedStatuses = new ArrayList<>();
        allPicturePaths = new ArrayList<>();
        allVideoPaths = new ArrayList<>();
        mRecyclerView = findViewById(R.id.status_grid_view);
        mergeVideoButton = findViewById(R.id.fab_merge_videos);

        mergeVideoButton.setVisibility(View.GONE);

        RecyclerView.LayoutManager gridLayoutManager = new GridLayoutManager(this, cells);

        mRecyclerView.setLayoutManager(gridLayoutManager);
        gridLayoutManager.setAutoMeasureEnabled(true);

        statusListAdapter = new FloatAdapter(mContext, allStatusPaths);

        statusListAdapter.setStatusClickListener(this);

        navigation = (BottomNavigationView) findViewById(R.id.main_bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Button to save the statuses.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int numOfSelectedPic = statusListAdapter.getSelectedPicturesStatuses().size();
                int numOfSelectedVideos = statusListAdapter.getSelectedVidoesStatuses().size();
                if (numOfSelectedPic != 0 &&
                        navigation.getSelectedItemId() == R.id.navigation_pictures) {
                    StatusSavingService.performSave(mContext, statusListAdapter.getSelectedPicturesStatuses());
                    String message = numOfSelectedPic == 1 ? "Picture saved" : numOfSelectedPic + " pictures saved";
                    statusListAdapter.mPicturesCheckStates.clear();
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    statusListAdapter.setSelectedPicturesStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    StatusSavingService.performFetch(mContext);
                } else if (numOfSelectedVideos != 0 &&
                        navigation.getSelectedItemId() == R.id.navigation_videos) {
                    StatusSavingService.performSave(mContext, statusListAdapter.getSelectedVidoesStatuses());
                    String message = numOfSelectedVideos == 1 ? "Video saved" : numOfSelectedVideos + " videos saved";
                    statusListAdapter.mVideosCheckStates.clear();
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    statusListAdapter.setSelectedVidoesStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    StatusSavingService.performFetch(mContext);
//                    navigation.setSelectedItemId(R.id.navigation_pictures);
                } else {
                    String typeMessage = navigation.getSelectedItemId() == R.id.navigation_pictures ?
                            "No picture selected" : "No video selected";
                    Snackbar.make(navigation, typeMessage, Snackbar.LENGTH_SHORT).show();
                }

            }
        });
        swipeRefreshLayout.setOnRefreshListener(this);
        askForContactPermission();
        startFloating();
        Intent receivedIntent = getIntent();
        if (receivedIntent != null) {
            String link = receivedIntent.getStringExtra("STATUS_KEY");
            String shareApp = receivedIntent.getStringExtra("share-app");
            if (link != null) showImagePopup(new Point(0, 2), link);
            else if (shareApp != null) {
                String mimeType = "text/plain";
                String title = "Share  WhatsApp Status Saver App";
                ShareCompat.IntentBuilder.from(this)
                        .setType(mimeType)
                        .setChooserTitle(title)
                        .setText(getResources().getString(R.string.share_text))
                        .startChooser();
            }
        }
        mergeVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int numOfSelectedVideos = statusListAdapter.getSelectedVidoesStatuses().size();
                if(numOfSelectedVideos < 2){
                    String typeMessage = "You have to select more than one video";
                    Snackbar.make(navigation, typeMessage, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                StatusSavingService.performMerge(mContext,statusListAdapter.getSelectedVidoesStatuses());
                swipeRefreshLayout.setRefreshing(true);
                StatusSavingService.performFetch(mContext);
            }
        });

    }

    @Override
    protected void onDestroy() {
//        startService(new Intent(this, FloatingButtonService.class));
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
        } else if (id == R.id.action_help) {
            showHelpPopup(MainActivity.this);
            return true;
        } else if (id == R.id.action_share) {
            String mimeType = "text/plain";
            String title = "Share  WhatsApp Status Saver App";
            ShareCompat.IntentBuilder.from(this)
                    .setType(mimeType)
                    .setChooserTitle(title)
                    .setText(getResources().getString(R.string.share_text))
                    .startChooser();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        StatusSavingService.performFetch(mContext);
        statusListAdapter.clearSelectedStatuses();
    }

    @Override
    public void onStatusLongClick(int position, String url) {
        showImagePopup(new Point(0, 2), url);
    }

    private void showHelpPopup(final Activity context) {
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
        dialog.getWindow().setDimAmount(0);
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
        dialog.getWindow().setDimAmount(0);


        // Getting a reference to Close button, and close the popup when clicked.
        FloatingActionButton close = (FloatingActionButton) dialog.findViewById(R.id.close_image_popup_button);
        ImageView statusImage = (ImageView) dialog.findViewById(R.id.full_status_image_view);
        final SimpleExoPlayerView simpleExoPlayerView = dialog.findViewById(R.id.full_status_video_view);
        final SimpleExoPlayer player;
        if (uri.endsWith(".jpg")) {
            GlideApp.with(context).load(uri).into(statusImage);
        } else if (uri.endsWith(".mp4")) {
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


    public void askForContactPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_WRITE_STORAGE);

            } else {
                StatusSavingService.performFetch(mContext);
            }
        } else {
            StatusSavingService.performFetch(mContext);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StatusSavingService.performFetch(mContext);
                } else {
                    Toast.makeText(getBaseContext(), "Sorry, This app cannot work on your device", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
        }
    }

    /**
     * If SelectedStatuses is not empty, clear the selected Views first and if empty, close the app.
     */
    @Override
    public void onBackPressed() {
        if (statusListAdapter.getSelectedPicturesStatuses().size() == 0 ||
                statusListAdapter.getSelectedVidoesStatuses().size() == 0) super.onBackPressed();
        else statusListAdapter.clearSelectedStatuses();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isFloatingAllowed = sharedPreferences.getBoolean("floating-allowed", false);
        if (isFloatingAllowed) startFloating();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isFloatingAllowed = sharedPreferences.getBoolean("floating-allowed", false);
        stopFloating();
    }

    public void startFloating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "You have to allow this app to draw over other apps in order" +
                    " for the floating circle to work", Toast.LENGTH_LONG).show();
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            initializeView();
        }
    }

    public void stopFloating() {
        stopService(new Intent(MainActivity.this, FloatingButtonService.class));
    }

    private void initializeView() {
        startService(new Intent(MainActivity.this, FloatingButtonService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            //Check if the permission is granted or not.
            if (resultCode == RESULT_OK) {
                initializeView();
            } else { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*
        Receiver for displaying the status after the background fetch.
     */
    public class FetchStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_FETCH = "setup-all-views";

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hasWhatsApp = intent.getBooleanExtra("the-user-has-whatsapp", true);
            boolean hasBusinessWhatsApp = intent.getBooleanExtra("the-user-has-business-whatsapp", true);
            if (!hasWhatsApp && !hasBusinessWhatsApp) {
                Snackbar.make(mRecyclerView, "Sorry, you do not have WhatsApp Installed", Snackbar.LENGTH_LONG).show();
            } else if (hasWhatsApp || hasBusinessWhatsApp) {
                ArrayList<String> receivedStatus = intent.getStringArrayListExtra(StatusSavingService.FETCHED_STATUSES);
                allPicturePaths.clear();
                allVideoPaths.clear();
                if(receivedStatus != null){
                    for (String path : receivedStatus) {
                        if (path.endsWith(".jpg")) {
                            allPicturePaths.add(path);
                        } else if (path.endsWith(".mp4")) {
                            allVideoPaths.add(path);
                        }
                    }
                    if (bottomSelected.equals("pictures"))
                        statusListAdapter.swapStatus(allPicturePaths);
                    else statusListAdapter.swapStatus(allVideoPaths);
                    mRecyclerView.setAdapter(statusListAdapter);
                    //Setting up the recycler view
                    swipeRefreshLayout.setRefreshing(false);
                }
                else {
                    Snackbar.make(mRecyclerView, "No status at the moment", Snackbar.LENGTH_LONG).show();
                }

            }
        }
    }
}
