package com.hashcode.whatsstatussaver.floatingbutton;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;

import com.hashcode.whatsstatussaver.MainActivity;
import com.hashcode.whatsstatussaver.R;
import com.hashcode.whatsstatussaver.data.StatusSavingService;
import com.hashcode.whatsstatussaver.views.StatusListAdapter;

import java.util.ArrayList;

/**
 * Created by oluwalekefakorede on 26/09/2017.
 */

public class FloatingButtonService extends Service implements SwipeRefreshLayout.OnRefreshListener,
        StatusListAdapter.StatusClickListener{
    private WindowManager mWindowManager;
    private View mFloatingView;
    public String TAG = FloatingButtonService.class.getSimpleName();

    /////////
    Context mContext;
    ArrayList<String> allStatusPaths;
    ArrayList<String> selectedStatuses;

    String bottomSelected = "pictures";
    //ArrayList containing the videos and the pictures
    ArrayList<String> allPicturePaths;
    ArrayList<String> allVideoPaths;
    SwipeRefreshLayout swipeRefreshLayout;
    final int MY_PERMISSION_REQUEST_WRITE_STORAGE = 100;

    //Using ListView
    GridView mGridView;
    BottomNavigationView navigation;
    StatusListAdapter statusListAdapter;

    private final static String ACTION_FETCH_STATUS = "fetch-status";
    private final static String ACTION_SAVE_STATUS = "save-status";

    private FloatingButtonService.FetchStatusReceiver fetchStatusReceiver;


    ///////////
    public FloatingButtonService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager.LayoutParams params = setUpAllViews();

        Log.i(TAG, "Floating service has started");
        ////

        IntentFilter filter = new IntentFilter(FetchStatusReceiver.PROCESS_FETCH);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        fetchStatusReceiver = new FetchStatusReceiver();
        registerReceiver(fetchStatusReceiver, filter);
        swipeRefreshLayout = (SwipeRefreshLayout) mFloatingView.findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark));
        Toolbar toolbar = (Toolbar) mFloatingView.findViewById(R.id.toolbar);
        mContext = getApplicationContext();
        allStatusPaths = new ArrayList<>();
        selectedStatuses = new ArrayList<>();
        allPicturePaths = new ArrayList<>();
        allVideoPaths = new ArrayList<>();
        mGridView = (GridView) mFloatingView.findViewById(R.id.status_grid_view);
        mGridView.setColumnWidth(3);
        statusListAdapter = new StatusListAdapter(mContext,allStatusPaths);

        statusListAdapter.setStatusClickListener(this);

        navigation = (BottomNavigationView) mFloatingView.findViewById(R.id.main_bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Button to save the statuses.
        FloatingActionButton fab = (FloatingActionButton) mFloatingView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int numOfSelectedPic = statusListAdapter.getSelectedPicturesStatuses().size();
                int numOfSelectedVideos = statusListAdapter.getSelectedVidoesStatuses().size();
                if( numOfSelectedPic!= 0 &&
                        navigation.getSelectedItemId()==R.id.navigation_pictures){
                    StatusSavingService.performSave(mContext,statusListAdapter.getSelectedPicturesStatuses());
                    String message = numOfSelectedPic == 1 ? "Picture saved" : numOfSelectedPic+" pictures saved";
                    statusListAdapter.mPicturesCheckStates.clear();
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    statusListAdapter.setSelectedPicturesStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    StatusSavingService.performFetch(mContext);
                }
                else if(numOfSelectedVideos!= 0 &&
                        navigation.getSelectedItemId()==R.id.navigation_videos){
                    StatusSavingService.performSave(mContext,statusListAdapter.getSelectedVidoesStatuses());
                    String message = numOfSelectedVideos == 1 ?  "Video saved" : numOfSelectedVideos + " videos saved";
                    statusListAdapter.mVideosCheckStates.clear();
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    statusListAdapter.setSelectedVidoesStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    StatusSavingService.performFetch(mContext);
//                    navigation.setSelectedItemId(R.id.navigation_pictures);
                }
                else{
                    String typeMessage = navigation.getSelectedItemId() == R.id.navigation_pictures ?
                            "No picture selected" : "No video selected";
                    Snackbar.make(navigation, typeMessage, Snackbar.LENGTH_SHORT).show();
                }

            }
        });
        swipeRefreshLayout.setOnRefreshListener(this);
        askForContactPermission();
        /////


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(fetchStatusReceiver);
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);

    }

    @SuppressLint("InflateParams")
    public WindowManager.LayoutParams setUpAllViews(){
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout,null);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);
        return params;
    }

    @Override
    public void onRefresh() {
        StatusSavingService.performFetch(mContext);
        statusListAdapter.clearSelectedStatused();
    }

    @Override
    public void onStatusLongClick(int position, String url) {
        showImagePopup(new Point(0,2),url);
    }

    /*
        Receiver for displaying the status after the background fetch.
     */
    public class FetchStatusReceiver extends BroadcastReceiver {
        public static final String PROCESS_FETCH = "setup-all-views";
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hasWhatsApp = intent.getBooleanExtra("the-user-has-whatsapp",true);
            if(!hasWhatsApp){
                Snackbar.make(mGridView,"Sorry, you do not have WhatsApp Installed",Snackbar.LENGTH_LONG).show();
            }
            else {
                ArrayList<String> receivedStatus = intent.getStringArrayListExtra(StatusSavingService.FETCHED_STATUSES);
                allPicturePaths.clear();
                allVideoPaths.clear();
                for(String path : receivedStatus){
                    if(path.endsWith(".jpg")){
                        allPicturePaths.add(path);
                    }else if(path.endsWith(".mp4")){
                        allVideoPaths.add(path);
                    }
                }
                statusListAdapter.setFolderPath(intent.getStringExtra(StatusSavingService.FOLDER_PATH));
//            statusListAdapter.swapStatus(receivedStatus);
                if(bottomSelected.equals("pictures")) statusListAdapter.swapStatus(allPicturePaths);
                else statusListAdapter.swapStatus(allVideoPaths);
                mGridView.setAdapter(statusListAdapter);
                //Setting up the recycler view
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }



    private void showHelpPopup(final Activity context) {
        Intent imageIntent = new Intent(this,MainActivity.class);
        imageIntent.putExtra("HELP_KEY","show-help-dialog");
        startActivity(imageIntent);

    }

    public void showImagePopup(Point p, final String uri) {
        Intent imageIntent = new Intent(this,MainActivity.class);
        imageIntent.putExtra("STATUS_KEY",uri);
        startActivity(imageIntent);

    }


    public void askForContactPermission(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(FloatingButtonService.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(permissionCheck != PackageManager.PERMISSION_GRANTED){
                //close the app
            }else{
                StatusSavingService.performFetch(mContext);
            }
        }
        else{
            StatusSavingService.performFetch(mContext);
        }
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pictures:
                    statusListAdapter.swapStatus(allPicturePaths);
                    bottomSelected = "pictures";
                    return true;
                case R.id.navigation_videos:
                    bottomSelected = "videos";

                    statusListAdapter.swapStatus(allVideoPaths);
                    return true;
            }
            return false;
        }

    };

}
