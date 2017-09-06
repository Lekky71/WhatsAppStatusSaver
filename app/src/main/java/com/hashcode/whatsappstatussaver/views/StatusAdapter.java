package com.hashcode.whatsappstatussaver.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.VideoView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hashcode.whatsappstatussaver.MainActivity;
import com.hashcode.whatsappstatussaver.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by oluwalekefakorede on 03/09/2017.
 */

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

    private ArrayList<String> statusPaths;
    private Context mContext;

    public ArrayList<String> getSelectedStatuses() {
        return selectedStatuses;
    }

    public void setStatusClickListener(StatusClickListener statusClickListener) {
        this.statusClickListener = statusClickListener;
    }

    StatusClickListener statusClickListener;

    public ArrayList<String> selectedStatuses;

    public interface StatusClickListener{
        void onStatusLongClick(int position, String url);
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    private String folderPath;


    public StatusAdapter(Context context, ArrayList<String> statuses){
        mContext = context;
        statusPaths = statuses;
//        try {
//            statusClickListener = (StatusClickListener) mContext;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(context.toString()
//                    + " must implement StatusClickListener");
//        }
    }

    @Override
    public StatusViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.each_status_view, parent, false);
        return new StatusViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(StatusViewHolder holder, final int position) {
        String statusPath = statusPaths.get(position);
        final String fullStatusPath = getFolderPath()+"/"+statusPath;
        selectedStatuses = new ArrayList<>();
        if(statusPath.endsWith(".jpg")){
            GlideApp.with(mContext).load(fullStatusPath)
                    .into(holder.statusImageView);
        }
        else if(statusPath.endsWith(".gif")){
            GlideApp.with(mContext)
                    .load(fullStatusPath)
                    .error(Color.GRAY)
                    .placeholder(Color.GRAY)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(holder.statusImageView);
        }
        else if(statusPath.endsWith(".mp4")){
            holder.playVideoImageView.setVisibility(View.VISIBLE);
            GlideApp.with(mContext)
                    .asBitmap()
                    .load(Uri.fromFile(new File(fullStatusPath)))
                    .into(holder.statusImageView);
        }
        holder.itemView.setTag(fullStatusPath);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Selecting image","it works");

                if(v.isSelected()){
                    v.setSelected(false);
                    v.setBackground(null);
                    selectedStatuses.remove(fullStatusPath);
                }else{
                    v.setSelected(true);
                    v.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                    selectedStatuses.add(fullStatusPath);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                statusClickListener.onStatusLongClick(position,fullStatusPath);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return statusPaths.size();
    }

    public class StatusViewHolder extends RecyclerView.ViewHolder{
        ImageView statusImageView;
        VideoView statusVideoView;
        ImageView playVideoImageView;
        public StatusViewHolder(View itemView) {
            super(itemView);
            statusImageView = itemView.findViewById(R.id.status_image);
            statusVideoView = itemView.findViewById(R.id.status_video);
            playVideoImageView = itemView.findViewById(R.id.video_play_button);
        }

    }

    public void swapStatus(ArrayList<String> data) {
        if (statusPaths != null) {
            statusPaths = new ArrayList<>();
        }
        statusPaths = data;
        notifyDataSetChanged();
    }

}
