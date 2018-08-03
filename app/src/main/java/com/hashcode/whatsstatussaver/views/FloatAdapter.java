package com.hashcode.whatsstatussaver.views;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hashcode.whatsstatussaver.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by oluwalekefakorede on 06/10/2017.
 */

public class FloatAdapter extends RecyclerView.Adapter<FloatAdapter.FloatViewHolder> implements
        CompoundButton.OnCheckedChangeListener{
    public SparseBooleanArray mPicturesCheckStates;
    public SparseBooleanArray mVideosCheckStates;
    public ArrayList<String> selectedPicturesStatuses;
    public ArrayList<String> selectedVidoesStatuses;
    StatusClickListener statusClickListener;
    private ArrayList<String> statusPaths;
    private Context mContext;
    private ArrayList<View> viewArrayList=new ArrayList<>();

    public FloatAdapter(Context context, ArrayList<String> statuses){
        mContext = context;
        statusPaths = statuses;
        selectedPicturesStatuses = new ArrayList<>();
        selectedVidoesStatuses = new ArrayList<>();
        mPicturesCheckStates = new SparseBooleanArray(statuses.size());
        mVideosCheckStates = new SparseBooleanArray(statuses.size());
    }
    @Override
    public FloatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater =  LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.float_status_view, parent, false);
//        int height = parent.getMeasuredHeight() / 4;
//        int width = parent.getMinimumWidth() /3;
//        itemView.setMinimumHeight(height);
//        itemView.setMinimumWidth(width);
        return new FloatAdapter.FloatViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(FloatViewHolder holder,int pos) {
        final int position = holder.getAdapterPosition();
        final String statusPath = statusPaths.get(pos);
        //changed fullStatusPath to only statusPath

        if(statusPath.endsWith(".jpg")){
            holder.playVideoImageView.setVisibility(View.INVISIBLE);
            GlideApp.with(mContext).load(statusPath)
                    .into(holder.statusImageView);
        }
        else if(statusPath.endsWith(".gif")){
            holder.playVideoImageView.setVisibility(View.INVISIBLE);
            GlideApp.with(mContext)
                    .load(statusPath)
                    .error(R.color.colorWhite)
                    .placeholder(R.color.colorWhite)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(holder.statusImageView);
        }
        else if(statusPath.endsWith(".mp4")){
            holder.playVideoImageView.setVisibility(View.VISIBLE);
            GlideApp.with(mContext)
                    .asBitmap()
                    .load(Uri.fromFile(new File(statusPath)))
                    .into(holder.statusImageView);
        }
        holder.itemView.setTag(statusPath);
//mPicturesCheckStates.get(position, false) &&
        if(statusPath.endsWith(".jpg")
                && selectedPicturesStatuses.contains(statusPath)){
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        }
        //mVideosCheckStates.get(position,false) &&
        else if(statusPath.endsWith(".mp4") &&
                selectedVidoesStatuses.contains(statusPath)){
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
        }
        else {
            holder.itemView.setBackground(null);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Selecting image","it works");

                if(mPicturesCheckStates.get(position,false)){
                    selectedPicturesStatuses.remove(statusPath);
                    Log.e("Selection","It saw it as selected");
                    v.setBackground(null);
                    mPicturesCheckStates.delete(position);
                    viewArrayList.remove(v);

                }
                else if(mVideosCheckStates.get(position,false)){
                    selectedVidoesStatuses.remove(statusPath);
                    Log.e("Selection","It saw it as selected");
                    v.setBackground(null);
                    mVideosCheckStates.delete(position);
                    viewArrayList.remove(v);

                }
                else{
                    v.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));
                    if(statusPath.endsWith(".jpg")){
                        selectedPicturesStatuses.add(statusPath);
                        mPicturesCheckStates.put(position,true);
                    }else if(statusPath.endsWith(".mp4")){
                        selectedVidoesStatuses.add(statusPath);
                        mVideosCheckStates.put(position,true);
                    }
                    viewArrayList.add(v);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                statusClickListener.onStatusLongClick(position, statusPath);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return statusPaths.size();
    }

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

    public void setStatusClickListener(FloatAdapter.StatusClickListener statusClickListener) {
        this.statusClickListener = statusClickListener;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }
    
    public class FloatViewHolder extends RecyclerView.ViewHolder {
        ImageView statusImageView ;
        ImageView playVideoImageView;
        public FloatViewHolder(View itemView) {
            super(itemView);
            statusImageView = itemView.findViewById(R.id.status_image);
            playVideoImageView = itemView.findViewById(R.id.video_play_button);

            //fixed the width and height issue
            int dimensionInDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120,
                    mContext.getResources().getDisplayMetrics());
            statusImageView.getLayoutParams().height = dimensionInDp;
            statusImageView.getLayoutParams().width = dimensionInDp;

        }
    }

    public void swapStatus(ArrayList<String> data) {
        if (statusPaths != null) {
            statusPaths = new ArrayList<>();
        }
        statusPaths = data;

        notifyDataSetChanged();
    }


    public void clearSelectedStatuses(){
        for (View v:viewArrayList){
            v.setBackground(null);
        }
        selectedPicturesStatuses.clear();
        selectedVidoesStatuses.clear();
        mPicturesCheckStates.clear();
        mVideosCheckStates.clear();
        notifyDataSetChanged();
    }

    public interface StatusClickListener{
        void onStatusLongClick(int position, String url);
    }


}
