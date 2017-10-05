package com.hashcode.whatsstatussaver.views;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.VideoView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hashcode.whatsstatussaver.R;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by oluwalekefakorede on 06/09/2017.
 */

public class FloatStatusAdapter extends ArrayAdapter<String> implements CompoundButton.OnCheckedChangeListener {
    private ArrayList<String> statusPaths;
    private Context mContext;
    public SparseBooleanArray mPicturesCheckStates;
    public SparseBooleanArray mVideosCheckStates;
    private ArrayList<View> viewArrayList=new ArrayList<>();


    public ArrayList<String> getSelectedPicturesStatuses() {
        return selectedPicturesStatuses;
    }

    public void setSelectedPicturesStatuses(ArrayList<String> selectedPicturesStatuses) {
        this.selectedPicturesStatuses = selectedPicturesStatuses;
    }

    public ArrayList<String> getSelectedVidoesStatuses() {
        return selectedVidoesStatuses;
    }

    public void setSelectedVidoesStatuses(ArrayList<String> selectedVidoesStatuses) {
        this.selectedVidoesStatuses = selectedVidoesStatuses;
    }

    public ArrayList<String> selectedPicturesStatuses;
    public ArrayList<String> selectedVidoesStatuses;


    public FloatStatusAdapter(Context context, ArrayList<String> statuses){
        super(context,-1,statuses);
        mContext = context;
        statusPaths = statuses;
        selectedPicturesStatuses = new ArrayList<>();
        selectedVidoesStatuses = new ArrayList<>();
        mPicturesCheckStates = new SparseBooleanArray(statuses.size());
        mVideosCheckStates = new SparseBooleanArray(statuses.size());
    }

    public void setStatusClickListener(StatusClickListener statusClickListener) {
        this.statusClickListener = statusClickListener;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }

    public interface StatusClickListener{
        void onStatusLongClick(int position, String url);
    }

    StatusClickListener statusClickListener;

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    private String folderPath;

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Context context = parent.getContext();
        LayoutInflater inflater =  LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.float_status_view, parent, false);
        Log.e("Loading the vies", "It did so");
        ImageView statusImageView = (ImageView) itemView.findViewById(R.id.status_image);
        ImageView playVideoImageView = (ImageView) itemView.findViewById(R.id.video_play_button);
//        playVideoImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_play_video_white_24dp));
        final String statusPath = statusPaths.get(position);
        final String fullStatusPath = getFolderPath()+"/"+statusPath;
        if(statusPath.endsWith(".jpg")){
            playVideoImageView.setVisibility(View.INVISIBLE);
            GlideApp.with(mContext).load(fullStatusPath)
                    .into(statusImageView);
        }
        else if(statusPath.endsWith(".gif")){
            playVideoImageView.setVisibility(View.INVISIBLE);
            GlideApp.with(mContext)
                    .load(fullStatusPath)
                    .error(Color.GRAY)
                    .placeholder(Color.GRAY)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(statusImageView);
        }
        else if(statusPath.endsWith(".mp4")){
            playVideoImageView.setVisibility(View.VISIBLE);
            GlideApp.with(mContext)
                    .asBitmap()
                    .load(Uri.fromFile(new File(fullStatusPath)))
                    .into(statusImageView);
        }
        itemView.setTag(fullStatusPath);

        if(mPicturesCheckStates.get(position, false) && statusPath.endsWith(".jpg")){
            itemView.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        }
        else if(mVideosCheckStates.get(position,false) && statusPath.endsWith(".mp4")){
            itemView.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        }

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Selecting image","it works");

                if(mPicturesCheckStates.get(position,false)){
                    selectedPicturesStatuses.remove(fullStatusPath);
                    Log.e("Selection","It saw it as selected");
                    v.setBackground(null);
                    mPicturesCheckStates.delete(position);
                    viewArrayList.remove(v);

                }
                else if(mVideosCheckStates.get(position,false)){
                    selectedVidoesStatuses.remove(fullStatusPath);
                    Log.e("Selection","It saw it as selected");
                    v.setBackground(null);
                    mVideosCheckStates.delete(position);
                    viewArrayList.remove(v);

                }
                else{
                    v.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                    if(statusPath.endsWith(".jpg")){
                        selectedPicturesStatuses.add(fullStatusPath);
                        mPicturesCheckStates.put(position,true);
                    }else if(statusPath.endsWith(".mp4")){
                        selectedVidoesStatuses.add(fullStatusPath);
                        mVideosCheckStates.put(position,true);
                    }
                    viewArrayList.add(v);
                }
            }
        });

        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                statusClickListener.onStatusLongClick(position,fullStatusPath);
                return true;
            }
        });

        return itemView;

    }
    public void swapStatus(ArrayList<String> data) {
        if (statusPaths != null) {
            statusPaths = new ArrayList<>();
        }
        statusPaths = data;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return statusPaths.size();
    }

    public void clearSelectedStatused(){
        for (View v:viewArrayList){
            v.setBackground(null);
        }
        selectedPicturesStatuses.clear();
        selectedVidoesStatuses.clear();
        mPicturesCheckStates.clear();
        mVideosCheckStates.clear();
        notifyDataSetChanged();
    }

}
