package com.hashcode.whatsstatussaver.floatingbutton;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.andremion.counterfab.CounterFab;
import com.hashcode.whatsstatussaver.MainActivity;
import com.hashcode.whatsstatussaver.R;
import com.hashcode.whatsstatussaver.data.LastModifiedFileComparator;
import com.hashcode.whatsstatussaver.views.FloatAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by oluwalekefakorede on 26/09/2017.
 */

public class FloatingButtonService extends Service implements SwipeRefreshLayout.OnRefreshListener,
        FloatAdapter.StatusClickListener {
    private final static String ACTION_FETCH_STATUS = "fetch-status";
    private final static String ACTION_SAVE_STATUS = "save-status";
    final int MY_PERMISSION_REQUEST_WRITE_STORAGE = 100;
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
    //Using ListView
    RecyclerView mRecyclerView;
    BottomNavigationView navigation;
    FloatAdapter floatAdapter;

    ImageView shareImageView;
    CounterFab floatingButton;
    FloatingActionButton expandedButton;
    RelativeLayout expandedLayout;
    public View.OnTouchListener expandedListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            floatingButton.setVisibility(View.VISIBLE);
            expandedButton.setVisibility(View.GONE);
            expandedLayout.setVisibility(View.GONE);
            return true;
        }
    };
    ImageView closeImageView;
    ImageView exitImageVIew;
    Display rootDisplay;
    RelativeLayout rootRelativeLayout;
    WindowManager.LayoutParams params;
    boolean isButtonClosed;
    private WindowManager mWindowManager;
    private View mFloatingView;
    View.OnTouchListener floatButtonTouchListener = new View.OnTouchListener() {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            rootDisplay = mWindowManager.getDefaultDisplay();
            Point size = new Point();
            rootDisplay.getSize(size);
            int width = size.x;
            int height = size.y;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
//                    closeImageView.setVisibility(View.VISIBLE);
                    //remember the initial position.
                    initialX = params.x;
                    initialY = params.y;

                    //get the touch location
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_UP:
//                    closeImageView.setVisibility(View.VISIBLE);
                    int Xdiff = (int) (event.getRawX() - initialTouchX);
                    int Ydiff = (int) (event.getRawY() - initialTouchY);


                    //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                    //So that is click event.
                    if (Xdiff < 10 && Ydiff < 10) {
                        if (isViewCollapsed()) {
                            //When user clicks on the image view of the collapsed layout,
                            //visibility of the collapsed layout will be changed to "View.GONE"
                            //and expanded view will become visible.
                            floatingButton.setVisibility(View.GONE);
                            expandedButton.setVisibility(View.VISIBLE);
                            expandedLayout.setVisibility(View.VISIBLE);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    //Calculate the X and Y coordinates of the view.
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    //Update the layout with new X & Y coordinate
                    mWindowManager.updateViewLayout(mFloatingView, params);
                    if (params.y >= (getScreenHeight() - 300)) {
                        Intent closeIntent = new Intent(mContext, FloatingButtonService.class);
                        stopService(closeIntent);
                    }
                    return true;

                case MotionEvent.ACTION_HOVER_EXIT:
                    closeImageView.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
    };
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_pictures:
                    floatAdapter.swapStatus(allPicturePaths);
                    bottomSelected = "pictures";
                    return true;
                case R.id.navigation_videos:
                    bottomSelected = "videos";
                    floatAdapter.swapStatus(allVideoPaths);
                    return true;
            }
            return false;
        }

    };

    private View.OnClickListener exitImageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent closeIntent = new Intent(mContext, FloatingButtonService.class);
            stopService(closeIntent);
        }
    };

    public FloatingButtonService() {
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFloatingView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.floating_button_layout, null);
        mContext = getApplicationContext();
        params = setUpAllViews();
        floatingButton = mFloatingView.findViewById(R.id.floating_status_head);
        expandedButton = mFloatingView.findViewById(R.id.floating_status_head_expanded);
        expandedLayout = mFloatingView.findViewById(R.id.expanded_root_view);
        closeImageView = mFloatingView.findViewById(R.id.floating_close_button);
        exitImageVIew = mFloatingView.findViewById(R.id.exit_image_view);
        shareImageView = mFloatingView.findViewById(R.id.float_share_button);

        rootRelativeLayout = mFloatingView.findViewById(R.id.float_overall_layout);
        isButtonClosed = true;
        swipeRefreshLayout = mFloatingView.findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark));
        Toolbar toolbar = mFloatingView.findViewById(R.id.toolbar);
        allStatusPaths = new ArrayList<>();
        selectedStatuses = new ArrayList<>();
        allPicturePaths = new ArrayList<>();
        allVideoPaths = new ArrayList<>();
        mRecyclerView = mFloatingView.findViewById(R.id.status_grid_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        floatAdapter = new FloatAdapter(mContext, allStatusPaths);
        mRecyclerView.setAdapter(floatAdapter);

        floatAdapter.setStatusClickListener(this);

        navigation = mFloatingView.findViewById(R.id.main_bottom_navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        floatingButton.setOnTouchListener(floatButtonTouchListener);
        expandedButton.setOnTouchListener(expandedListener);
        //Button to save the statuses.
        exitImageVIew.setOnClickListener(exitImageClickListener);
        FloatingActionButton fab = mFloatingView.findViewById(R.id.fab_save);

        shareImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent imageIntent = new Intent(FloatingButtonService.this, MainActivity.class);
                imageIntent.putExtra("share-app", "share this app");
                imageIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(imageIntent);
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int numOfSelectedPic = floatAdapter.getSelectedPicturesStatuses().size();
                int numOfSelectedVideos = floatAdapter.getSelectedVidoesStatuses().size();
                if (numOfSelectedPic != 0 &&
                        navigation.getSelectedItemId() == R.id.navigation_pictures) {
                    saveAllSelectedStatus(floatAdapter.getSelectedPicturesStatuses());
                    String message = numOfSelectedPic == 1 ? "Picture saved" : numOfSelectedPic + " pictures saved";
                    floatAdapter.mPicturesCheckStates.clear();
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    floatAdapter.setSelectedPicturesStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    fetchStatuses();
                } else if (numOfSelectedVideos != 0 &&
                        navigation.getSelectedItemId() == R.id.navigation_videos) {
                    saveAllSelectedStatus(floatAdapter.getSelectedVidoesStatuses());
                    String message = numOfSelectedVideos == 1 ? "Video saved" : numOfSelectedVideos + " videos saved";
                    floatAdapter.mVideosCheckStates.clear();
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                    floatAdapter.setSelectedVidoesStatuses(new ArrayList<String>());
                    swipeRefreshLayout.setRefreshing(true);
                    fetchStatuses();
                } else {
                    String typeMessage = navigation.getSelectedItemId() == R.id.navigation_pictures ?
                            "No picture selected" : "No video selected";
                    Snackbar.make(navigation, typeMessage, Snackbar.LENGTH_SHORT).show();
                }

            }
        });
        swipeRefreshLayout.setOnRefreshListener(this);
        askForFloatingPermission();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);

    }

    @SuppressLint("InflateParams")
    public WindowManager.LayoutParams setUpAllViews() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        try {
            if (mWindowManager != null) {
                mWindowManager.addView(mFloatingView, params);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return params;
    }

    @Override
    public void onRefresh() {
//        StatusSavingService.performFetch(mContext);
        fetchStatuses();
        //TODO 5
        floatAdapter.clearSelectedStatuses();
    }

    @Override
    public void onStatusLongClick(int position, String url) {
        showImagePopup(new Point(0, 2), url);
        floatingButton.setVisibility(View.VISIBLE);
        expandedButton.setVisibility(View.GONE);
        expandedLayout.setVisibility(View.GONE);
    }

    private void showHelpPopup(final Activity context) {
        Intent imageIntent = new Intent(this, MainActivity.class);
        imageIntent.putExtra("HELP_KEY", "show-help-dialog");
        startActivity(imageIntent);

    }

    public void showImagePopup(Point p, final String uri) {
        Intent imageIntent = new Intent(this, MainActivity.class);
        imageIntent.putExtra("STATUS_KEY", uri);
        imageIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(imageIntent);

    }

    /*
        Receiver for displaying the status after the background fetch.
     */

    public void askForFloatingPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(FloatingButtonService.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                //close the app
            } else {
                fetchStatuses();
            }
        } else {
            fetchStatuses();
        }
    }

    private boolean isViewCollapsed() {
        return mFloatingView == null || mFloatingView.findViewById(R.id.floating_status_head).getVisibility() == View.VISIBLE;
    }

    private ArrayList<String> fetchAllStatus() {
        String foldPath = new StringBuffer().append(Environment.getExternalStorageDirectory()
                .getAbsolutePath()).append("/WhatsApp/Media/.Statuses/").toString();

        String businessfoldPath = new StringBuffer().append(Environment.getExternalStorageDirectory()
                .getAbsolutePath()).append("/WhatsApp Business/Media/.Statuses/").toString();

        File whatsAppFile = new File(foldPath);
        File whatsAppBusinessFile = new File(businessfoldPath);
        if (whatsAppFile.exists()) {

        }
        else if (!whatsAppFile.exists()) {

            if (!whatsAppBusinessFile.exists()) {
                Intent broadcastBusinessIntent = new Intent();
                broadcastBusinessIntent.setAction(MainActivity.FetchStatusReceiver.PROCESS_FETCH);
                broadcastBusinessIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastBusinessIntent.putExtra("the-user-has-whatsapp", false);
                broadcastBusinessIntent.putExtra("the-user-has-business-whatsapp", false);
                sendBroadcast(broadcastBusinessIntent);
                return null;
            }
        }

        Date currentDate = new Date();
        long cTime = currentDate.getTime();
        File whatsappFiles[] = whatsAppFile.listFiles() == null ? new File[0] : whatsAppFile.listFiles() ;
        File businessFiles[] = whatsAppBusinessFile.listFiles() == null ? new File[0] : whatsAppBusinessFile.listFiles();


        Arrays.sort(whatsappFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

        Arrays.sort(businessFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

        ArrayList<String> whatsAppStatuses = new ArrayList<>();

        for (int i = 0; i < whatsappFiles.length; i++) {
            long diff = cTime - whatsappFiles[i].lastModified();
            if (diff <= (24 * 60 * 60 * 1000)) {
                whatsAppStatuses.add(foldPath + whatsappFiles[i].getName());

            }            //here populate your listview
        }

        for (int i = 0; i < businessFiles.length; i++) {
            long diff = cTime - businessFiles[i].lastModified();
            if (diff <= (24 * 60 * 60 * 1000)) {
                whatsAppStatuses.add(businessfoldPath + businessFiles[i].getName());

            }
        }
        return whatsAppStatuses;
    }

    private void saveAllSelectedStatus(ArrayList<String> statuses) {
        String fileType = Environment.DIRECTORY_PICTURES;
        for (String status : statuses) {
            if (status.endsWith(".jpg")) {
                fileType = "Pictures";
            } else if (status.endsWith(".gif")) {
                fileType = "Gifs";
            } else if (status.endsWith(".mp4")) {
                fileType = "Videos";
            }
            String[] splitStatus = status.split("/");
            String destinationFilename = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/WhatsAppSaver" + File.separatorChar + splitStatus[splitStatus.length - 1];
            try {
                copyFile(new File(status), new File(destinationFilename));
                Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                intent.setData(Uri.fromFile(new File(destinationFilename)));
                sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void copyFile(File file, File file2) throws IOException {
        Throwable th;
        Throwable th2;
        if (!file2.getParentFile().exists()) {
            file2.getParentFile().mkdirs();
        }
        if (!file2.exists()) {
            file2.createNewFile();
        }
        FileChannel fileChannel = (FileChannel) null;
        FileChannel fileChannel2 = (FileChannel) null;
        FileChannel channel;
        try {
            channel = new FileInputStream(file).getChannel();
            try {
                fileChannel = new FileOutputStream(file2).getChannel();
            } catch (Throwable th3) {
                th = th3;
                if (channel != null) {
                    channel.close();
                }
                if (fileChannel2 != null) {
                    fileChannel2.close();
                }
                throw th;
            }
            try {
                fileChannel.transferFrom(channel, (long) 0, channel.size());
                if (channel != null) {
                    channel.close();
                }
                if (fileChannel != null) {
                    fileChannel.close();
                }
            } catch (Throwable th4) {
                th2 = th4;
                fileChannel2 = fileChannel;
                th = th2;
                if (channel != null) {
                    channel.close();
                }
                if (fileChannel2 != null) {
                    fileChannel2.close();
                }
                throw th;
            }
        } catch (Throwable th5) {
            th2 = th5;
            channel = fileChannel;
            th = th2;
            if (channel != null) {
                channel.close();
            }
            if (fileChannel2 != null) {
                fileChannel2.close();
            }
        }
    }

    public void fetchStatuses() {
        ArrayList<String> receivedStatus = fetchAllStatus();

        allPicturePaths.clear();
        allVideoPaths.clear();
        if (receivedStatus != null) {
            for (String path : receivedStatus) {
                if (path.endsWith(".jpg")) {
                    allPicturePaths.add(path);
                } else if (path.endsWith(".mp4")) {
                    allVideoPaths.add(path);
                }
            }
        }
        String foldPath = Environment.getExternalStorageDirectory()
                .getAbsolutePath() +
                "/WhatsApp/Media/.Statuses/";
//        floatAdapter.setFolderPath(foldPath);
//            floatAdapter.swapStatus(receivedStatus);
        if (bottomSelected.equals("pictures")) floatAdapter.swapStatus(allPicturePaths);
        else floatAdapter.swapStatus(allVideoPaths);
        mRecyclerView.setAdapter(floatAdapter);
        //Setting up the recycler view
        swipeRefreshLayout.setRefreshing(false);
    }

    public int checkAddedStatuses() {
        String foldPath = new StringBuffer().append(Environment.getExternalStorageDirectory()
                .getAbsolutePath()).append("/WhatsApp/Media/.Statuses/").toString();

        File f = new File(foldPath);
        return 0;
    }

}
