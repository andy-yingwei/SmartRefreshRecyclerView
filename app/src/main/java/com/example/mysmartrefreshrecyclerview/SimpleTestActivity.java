package com.example.mysmartrefreshrecyclerview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartrefreshrecyclerview.SmartRefreshRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SimpleTestActivity extends AppCompatActivity {

    private static final String TAG = "SimpleTest";

    private SmartRefreshRecyclerView mRefreshView;
    private TextView mTvStatus;
    private List<String> mDataList = new ArrayList<>();
    private SimpleAdapter mAdapter;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // 标记当前使用的是内置视图还是自定义视图
    private boolean mUseCustomViews = false;
    private SmartRefreshRecyclerView.RefreshConfig mCurrentConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_test);

        Log.d(TAG, "=== 开始测试 SmartRefreshRecyclerView ===");
        Log.d(TAG, "自定义加载布局ID: " + R.layout.layout_simple_custom_loading);
        Log.d(TAG, "自定义空布局ID: " + R.layout.layout_simple_custom_empty);
        Log.d(TAG, "自定义错误布局ID: " + R.layout.layout_simple_custom_error);

        // 初始使用默认配置
        mCurrentConfig = createDefaultConfig();

        initViews();
        setupListeners();

        updateStatus("初始化完成，当前使用内置状态视图");

        // 初始显示内置加载中视图
        mRefreshView.showLoadingView();

        // 2秒后加载测试数据
        mHandler.postDelayed(this::loadTestData, 2000);
    }

    private SmartRefreshRecyclerView.RefreshConfig createDefaultConfig() {
        SmartRefreshRecyclerView.RefreshConfig config = new SmartRefreshRecyclerView.RefreshConfig();
        config.enableRefresh = true;
        config.enableLoadMore = true;
        config.loadingLayoutId = 0;  // 0表示使用内置布局
        config.emptyLayoutId = 0;
        config.errorLayoutId = 0;
        Log.d(TAG, "创建默认配置: 加载ID=0, 空ID=0, 错误ID=0");
        return config;
    }

    private SmartRefreshRecyclerView.RefreshConfig createCustomConfig() {
        SmartRefreshRecyclerView.RefreshConfig config = new SmartRefreshRecyclerView.RefreshConfig();
        config.enableRefresh = true;
        config.enableLoadMore = true;
        config.loadingLayoutId = R.layout.layout_simple_custom_loading;
        config.emptyLayoutId = R.layout.layout_simple_custom_empty;
        config.errorLayoutId = R.layout.layout_simple_custom_error;
        Log.d(TAG, "创建自定义配置: 加载ID=" + config.loadingLayoutId +
                ", 空ID=" + config.emptyLayoutId +
                ", 错误ID=" + config.errorLayoutId);
        return config;
    }

    private void initViews() {
        mRefreshView = findViewById(R.id.refresh_view);
        mTvStatus = findViewById(R.id.tv_status);

        if (mRefreshView == null) {
            Log.e(TAG, "错误: 未找到refresh_view");
            Toast.makeText(this, "未找到SmartRefreshRecyclerView", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "SmartRefreshRecyclerView初始化成功");

        // 初始化适配器
        mAdapter = new SimpleAdapter();
        mRefreshView.setAdapter(mAdapter);

        // 启用刷新和加载更多
        mRefreshView.setEnableLoadMore(true);
    }

    private void setupListeners() {
        // 1. 内置状态视图测试
        findViewById(R.id.btn_default_loading).setOnClickListener(v -> {
            Log.d(TAG, "点击: 测试内置加载中视图");
            mUseCustomViews = false;
            mCurrentConfig = createDefaultConfig();

            // 设置默认配置
            Log.d(TAG, "设置默认配置...");
            mRefreshView.setRefreshConfig(mCurrentConfig);

            mRefreshView.showLoadingView();
            updateStatus("显示内置加载中视图(应无🔥标识)");
            Log.d(TAG, "已显示内置加载中视图");

            // 3秒后自动隐藏
            mHandler.postDelayed(() -> {
                mRefreshView.hideStateViews();
                updateStatus("自动隐藏状态视图");
            }, 3000);
        });

        findViewById(R.id.btn_default_empty).setOnClickListener(v -> {
            Log.d(TAG, "点击: 测试内置空数据视图");
            mUseCustomViews = false;
            mCurrentConfig = createDefaultConfig();

            // 设置默认配置
            Log.d(TAG, "设置默认配置...");
            mRefreshView.setRefreshConfig(mCurrentConfig);

            mRefreshView.showEmptyView();
            updateStatus("显示内置空数据视图(应无🔥标识)");
            Log.d(TAG, "已显示内置空数据视图");
        });

        findViewById(R.id.btn_default_error).setOnClickListener(v -> {
            Log.d(TAG, "点击: 测试内置错误视图");
            mUseCustomViews = false;
            mCurrentConfig = createDefaultConfig();

            // 设置默认配置
            Log.d(TAG, "设置默认配置...");
            mRefreshView.setRefreshConfig(mCurrentConfig);

            mRefreshView.showErrorView();
            updateStatus("显示内置错误视图(应无🔥标识)");
            Log.d(TAG, "已显示内置错误视图");
        });

        // 2. 自定义状态视图测试
        findViewById(R.id.btn_custom_loading).setOnClickListener(v -> {
            Log.d(TAG, "点击: 测试自定义加载中视图");
            mUseCustomViews = true;
            mCurrentConfig = createCustomConfig();

            // 设置自定义配置
            Log.d(TAG, "设置自定义配置...");
            mRefreshView.setRefreshConfig(mCurrentConfig);

            mRefreshView.showLoadingView();
            updateStatus("显示自定义加载中视图(应有🔥标识，绿色背景)");
            Log.d(TAG, "已显示自定义加载中视图");

            // 绑定自定义视图中的点击事件
            bindCustomViewClickListeners();

            // 3秒后自动隐藏
            mHandler.postDelayed(() -> {
                mRefreshView.hideStateViews();
                updateStatus("自动隐藏自定义状态视图");
            }, 3000);
        });

        findViewById(R.id.btn_custom_empty).setOnClickListener(v -> {
            Log.d(TAG, "点击: 测试自定义空数据视图");
            mUseCustomViews = true;
            mCurrentConfig = createCustomConfig();

            // 设置自定义配置
            Log.d(TAG, "设置自定义配置...");
            mRefreshView.setRefreshConfig(mCurrentConfig);

            mRefreshView.showEmptyView();
            updateStatus("显示自定义空数据视图(应有🔥标识，橙色背景)");
            Log.d(TAG, "已显示自定义空数据视图");

            // 绑定自定义视图中的点击事件
            bindCustomViewClickListeners();
        });

        findViewById(R.id.btn_custom_error).setOnClickListener(v -> {
            Log.d(TAG, "点击: 测试自定义错误视图");
            mUseCustomViews = true;
            mCurrentConfig = createCustomConfig();

            // 设置自定义配置
            Log.d(TAG, "设置自定义配置...");
            mRefreshView.setRefreshConfig(mCurrentConfig);

            mRefreshView.showErrorView();
            updateStatus("显示自定义错误视图(应有🔥标识，红色背景)");
            Log.d(TAG, "已显示自定义错误视图");

            // 绑定自定义视图中的点击事件
            bindCustomViewClickListeners();
        });

        // 3. 功能测试
        findViewById(R.id.btn_load_data).setOnClickListener(v -> {
            Log.d(TAG, "点击: 加载测试数据");
            loadTestData();
        });

        findViewById(R.id.btn_clear_data).setOnClickListener(v -> {
            Log.d(TAG, "点击: 清空数据");
            mDataList.clear();
            mAdapter.notifyDataSetChanged();

            // 显示空视图
            mRefreshView.showEmptyView();
            String viewType = mUseCustomViews ? "自定义" : "内置";
            updateStatus("已清空数据，显示" + viewType + "空视图");
        });

        findViewById(R.id.btn_auto_refresh).setOnClickListener(v -> {
            Log.d(TAG, "点击: 触发自动刷新");
            mRefreshView.autoRefresh();
            updateStatus("触发自动刷新");
        });

        // 4. 设置下拉刷新监听
        mRefreshView.setOnRefreshListener(new SmartRefreshRecyclerView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "下拉刷新被触发");
                updateStatus("下拉刷新中...");

                mHandler.postDelayed(() -> {
                    mDataList.clear();
                    for (int i = 1; i <= 10; i++) {
                        mDataList.add("刷新数据 " + i);
                    }
                    mAdapter.notifyDataSetChanged();

                    mRefreshView.finishRefresh();
                    updateStatus("下拉刷新完成，加载" + mDataList.size() + "条数据");
                    Toast.makeText(SimpleTestActivity.this, "刷新完成", Toast.LENGTH_SHORT).show();
                }, 1500);
            }
        });

        // 5. 设置上拉加载监听
        mRefreshView.setOnLoadMoreListener(new SmartRefreshRecyclerView.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                Log.d(TAG, "上拉加载被触发");
                updateStatus("上拉加载中...");

                mHandler.postDelayed(() -> {
                    int start = mDataList.size() + 1;
                    for (int i = 1; i <= 5; i++) {
                        mDataList.add("加载更多 " + (start + i - 1));
                    }
                    mAdapter.notifyDataSetChanged();

                    // 模拟最多加载20条数据
                    boolean hasMore = mDataList.size() < 20;
                    mRefreshView.finishLoadMore(hasMore);
                    updateStatus("上拉加载完成，共" + mDataList.size() + "条数据");
                    Toast.makeText(SimpleTestActivity.this, "加载了5条数据", Toast.LENGTH_SHORT).show();
                }, 1500);
            }
        });

        // 6. 设置状态视图点击监听
        mRefreshView.setOnStateViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String viewType = mUseCustomViews ? "自定义" : "内置";
                int state = mRefreshView.getCurrentState();
                String stateName = "";

                switch (state) {
                    case SmartRefreshRecyclerView.STATE_LOADING:
                        stateName = "加载中";
                        break;
                    case SmartRefreshRecyclerView.STATE_EMPTY:
                        stateName = "空数据";
                        break;
                    case SmartRefreshRecyclerView.STATE_ERROR:
                        stateName = "错误";
                        break;
                }

                Log.d(TAG, "点击" + viewType + stateName + "视图");
                updateStatus("点击" + viewType + stateName + "视图，重新加载");

                mRefreshView.showLoadingView();
                mHandler.postDelayed(() -> {
                    loadTestData();
                }, 1500);
            }
        });
    }

    /**
     * 绑定自定义视图中的点击事件
     */
    private void bindCustomViewClickListeners() {
        mHandler.postDelayed(() -> {
            // 查找自定义视图中的按钮
            View customRetryBtn = mRefreshView.findViewById(R.id.btn_retry);
            if (customRetryBtn != null) {
                Log.d(TAG, "找到自定义重试按钮");
                customRetryBtn.setOnClickListener(v -> {
                    Toast.makeText(SimpleTestActivity.this, "点击了自定义视图的重试按钮", Toast.LENGTH_SHORT).show();
                    updateStatus("自定义视图点击重试");

                    mRefreshView.showLoadingView();
                    mHandler.postDelayed(() -> {
                        loadTestData();
                    }, 1500);
                });
            } else {
                Log.d(TAG, "未找到自定义重试按钮");
            }
        }, 200);
    }

    private void loadTestData() {
        Log.d(TAG, "加载测试数据");
        updateStatus("正在加载数据...");

        mRefreshView.showLoadingView();

        mHandler.postDelayed(() -> {
            mDataList.clear();
            for (int i = 1; i <= 8; i++) {
                mDataList.add("测试数据 " + i);
            }
            mAdapter.notifyDataSetChanged();

            mRefreshView.hideStateViews();
            updateStatus("数据加载完成，共" + mDataList.size() + "条");
            Toast.makeText(this, "加载了8条测试数据", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void updateStatus(String message) {
        String prefix = mUseCustomViews ? "[自定义]" : "[内置]";
        String statusText = "状态: " + prefix + " " + message;
        mTvStatus.setText(statusText);
        Log.d(TAG, message);
    }

    /**
     * 简单适配器
     */
    private class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // 使用系统自带的简单布局
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String data = mDataList.get(position);
            holder.textView.setText(data);

            // 简单设置背景色区分
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(0xFFF5F5F5);
            } else {
                holder.itemView.setBackgroundColor(0xFFFFFFFF);
            }

            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(SimpleTestActivity.this,
                        "点击: " + data, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}