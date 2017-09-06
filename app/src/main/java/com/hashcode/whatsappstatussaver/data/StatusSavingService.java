package com.hashcode.whatsappstatussaver.data;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hashcode.whatsappstatussaver.MainActivity;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by oluwalekefakorede on 03/09/2017.
 */

public class StatusSavingService extends IntentService {
    private final static String ACTION_FETCH_STATUS = "fetch-status";
    private final static String ACTION_SAVE_STATUS = "save-status";

    private static final String TAG = StatusSavingService.class.getSimpleName();

// /mnt/shell/emulated/0/WhatsApp/Media/.Statuses

    public final static String FETCHED_STATUSES = "fetched-statuses";
    public static final String SELECTED_STATUSES = "selected-statuses";
    public static String FOLDER_PATH ="folder-path";

    //   /storage/sdcard0/WhatsApp/Media/.Statuses
    // whatsapp://send?text=
    public StatusSavingService(){
        super(TAG);

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String action = intent.getAction();
        if(ACTION_FETCH_STATUS.equals(action)){
            fetchAllStatus();
        }
        else if(ACTION_SAVE_STATUS.equals(action)){
            ArrayList<String> selectedStatuses = intent.getStringArrayListExtra(SELECTED_STATUSES);
            saveAllSelectedStatus(selectedStatuses);
        }
    }

    private void fetchAllStatus(){
        String state = Environment.getExternalStorageState();
        String folderPath = "/storage/sdcard0/WhatsApp/Media/.Statuses";
        String foldPath = new StringBuffer().append(Environment.getExternalStorageDirectory()
                .getAbsolutePath()).append("/WhatsApp/Media/.Statuses/").toString();



        File f = new File(foldPath);
        if(f.exists()){
            Log.d("the whatsapp folder", "yes existence");
        }

        if(f.setReadable(true)){
            Log.d("the whatsapp folder", "yes can read from");
        }
        File files[] = f.listFiles();
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        Log.d("Files", "Size: "+ files.length);
        ArrayList<String> statuses = new ArrayList<>();
        for (int i=0; i < files.length; i++){
            statuses.add(files[i].getName());
            //here populate your listview
            Log.d("Files", "FileName:" + files[i].getName());
        }
        sendFetchBroadCast(statuses,foldPath);
    }


    private void saveAllSelectedStatus(ArrayList<String> statuses){
//        String externalPath = Environment.getExternalStorageDirectory().toString()+"/Pictures";
//        String internalPath = "storage/emulated/0";
        String fileType = Environment.DIRECTORY_PICTURES;
        for(String status : statuses){
            if(status.endsWith(".jpg")){
                fileType = "Pictures";
            }else if(status.endsWith(".gif")){
                fileType ="Gifs";
            }else if(status.endsWith(".mp4")){
                fileType="Videos";
            }
            String [] splitStatus = status.split("/");
//            savefile(Uri.parse(status),fileType,splitStatus[splitStatus.length-1]);
            String destinationFilename = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    +"/WhatsAppSaver"+File.separatorChar+splitStatus[splitStatus.length-1];
            try {
                copyFile(new File(status),new File(destinationFilename));
                Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                intent.setData(Uri.fromFile(new File(destinationFilename)));
                sendBroadcast(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("Saving images ","saved image url "+status);

            Log.e("Saving images ","saved image "+splitStatus[splitStatus.length-1]);
        }
    }

    public static void performSave(Context context, ArrayList<String> paths){
        Intent intent = new Intent(context,StatusSavingService.class);
        intent.setAction(ACTION_SAVE_STATUS);
        intent.putExtra(SELECTED_STATUSES,paths);
        context.startService(intent);
    }
    public static void performFetch(Context context){
        Intent intent = new Intent(context,StatusSavingService.class);
        intent.setAction(ACTION_FETCH_STATUS);
        context.startService(intent);
    }
    public void sendFetchBroadCast(ArrayList<String> statuses, String folderPath){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.FetchStatusReceiver.PROCESS_FETCH);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(FOLDER_PATH,folderPath);
        broadcastIntent.putExtra(FETCHED_STATUSES,statuses);
        sendBroadcast(broadcastIntent);
    }

    void savefile(Uri sourceuri, String fileType, String filename)
    {
        String sourceFilename= sourceuri.getPath();
        String destinationFilename = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                +"/WhatsAppSaver"+File.separatorChar+filename;
        Log.e("Saving images ","saved image directory "+destinationFilename);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sourceFilename));
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
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

}
