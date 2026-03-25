package com.example.smartrefreshrecyclerview;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class LoadMoreFooter extends RecyclerView.ViewHolder {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_NO_MORE = 2;
    public static final int STATE_ERROR = 3;

    private ProgressBar progressBar;
    private TextView textView;
    private int currentState = STATE_NORMAL;

    public LoadMoreFooter(Context context) {
        super(LayoutInflater.from(context).inflate(R.layout.srr_layout_load_more_footer, null));
        progressBar = itemView.findViewById(R.id.progress_bar);
        textView = itemView.findViewById(R.id.tv_hint);
        setState(STATE_NORMAL);
    }

    public void setState(int state) {
        currentState = state;
        switch (state) {
            case STATE_NORMAL:
                progressBar.setVisibility(View.GONE);
                textView.setText("上拉加载更多");
                break;
            case STATE_LOADING:
                progressBar.setVisibility(View.VISIBLE);
                textView.setText("正在加载...");
                break;
            case STATE_NO_MORE:
                progressBar.setVisibility(View.GONE);
                textView.setText("已加载全部数据");
                break;
            case STATE_ERROR:
                progressBar.setVisibility(View.GONE);
                textView.setText("加载失败，点击重试");
                break;
        }
    }

    public int getState() {
        return currentState;
    }

    public void setRetryListener(View.OnClickListener listener) {
        itemView.setOnClickListener(listener);
    }
}