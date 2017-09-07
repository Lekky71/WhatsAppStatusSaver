package com.hashcode.whatsappstatussaver.views;

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
import com.hashcode.whatsappstatussaver.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by oluwalekefakorede on 06/09/2017.
 */

public class StatusListAdapter extends ArrayAdapter<String> implements CompoundButton.OnCheckedChangeListener {
    private ArrayList<String> statusPaths;
    private Context mContext;
    public SparseBooleanArray mCheckStates;

    public void setSelectedStatuses(ArrayList<String> selectedStatuses) {
        this.selectedStatuses = selectedStatuses;
    }

    public ArrayList<String> selectedStatuses;

    public ArrayList<String> getSelectedStatuses() {
        return selectedStatuses;
    }

    public StatusListAdapter(Context context, ArrayList<String> statuses){
        super(context,-1,statuses);
        mContext = context;
        statusPaths = statuses;
        selectedStatuses = new ArrayList<>();
        mCheckStates = new SparseBooleanArray(statuses.size());
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
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View itemView = inflater.inflate(R.layout.each_status_view, parent, false);
        Log.e("Loading the vies", "It did so");
        ImageView statusImageView = (ImageView) itemView.findViewById(R.id.status_image);
        VideoView statusVideoView = itemView.findViewById(R.id.status_video);
        ImageView playVideoImageView = (ImageView) itemView.findViewById(R.id.video_play_button);

        String statusPath = statusPaths.get(position);
        final String fullStatusPath = getFolderPath()+"/"+statusPath;
        if(statusPath.endsWith(".jpg")){
            GlideApp.with(mContext).load(fullStatusPath)
                    .into(statusImageView);
        }
        else if(statusPath.endsWith(".gif")){
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

        if(mCheckStates.get(position, false)){
            itemView.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        }

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Selecting image","it works");

                if(mCheckStates.get(position,false)){
                    selectedStatuses.remove(fullStatusPath);
                    Log.e("Selection","It saw it as selected");
                    v.setBackground(null);
                    mCheckStates.delete(position);
//                    v.setBackground(null);
                }else{
//                    v.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                    v.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                    selectedStatuses.add(fullStatusPath);
                    mCheckStates.put(position,true);
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

}
