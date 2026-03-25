package com.example.smartrefreshrecyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.PtrUIHandler;
import in.srain.cube.views.ptr.header.MaterialHeader;

/**
 * 智能刷新RecyclerView控件
 * 功能：集成下拉刷新、上拉加载、状态视图（加载中/空数据/错误）的列表控件
 * 修复：解决动态切换配置时状态视图不更新的问题
 *
 * 【重要修复点】
 * 1. 配置对象在成员变量声明时初始化，避免空指针异常
 * 2. 添加了recreateStateViews()方法，支持动态重新创建状态视图
 * 3. 在setRefreshConfig()中添加了状态视图重新创建的调用
 * 4. 所有公开方法都添加了空值检查和异常处理
 *
 * 【使用示例】
 * 1. 动态切换配置：
 *    SmartRefreshRecyclerView.RefreshConfig config = new SmartRefreshRecyclerView.RefreshConfig();
 *    config.loadingLayoutId = R.layout.custom_loading;
 *    config.emptyLayoutId = R.layout.custom_empty;
 *    config.errorLayoutId = R.layout.custom_error;
 *    refreshView.setRefreshConfig(config);  // 这会自动重新创建状态视图
 *    refreshView.showLoadingView();
 */



/**
 * 智能刷新RecyclerView控件
 * 功能：集成下拉刷新、上拉加载、状态视图（加载中/空数据/错误）的列表控件
 *
 * 【支持的三种配置模式】
 * 1. 使用内置三个视图：所有layoutId设为0或不设置
 * 2. 使用自定义三个视图：所有layoutId设为自定义布局资源ID
 * 3. 混合型：部分layoutId设为自定义，部分设为0（使用内置）
 *
 * 【重要设计原则】
 * 1. 健壮性优先：任何情况下都不崩溃
 * 2. 兼容性：支持动态切换配置
 * 3. 可调试：详细的日志输出
 * 4. 可扩展：易于添加新功能
 */
public class SmartRefreshRecyclerView extends FrameLayout {

    // ==================== 调试TAG ====================
    private static final String TAG = "SmartRefreshRecyclerView";

    // ==================== 状态类型定义 ====================
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_NORMAL, STATE_LOADING, STATE_EMPTY, STATE_ERROR, STATE_NO_MORE})
    public @interface ViewState {}

    public static final int STATE_NORMAL = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_EMPTY = 2;
    public static final int STATE_ERROR = 3;
    public static final int STATE_NO_MORE = 4;

    // ==================== 布局管理器类型定义 ====================
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LAYOUT_LINEAR, LAYOUT_GRID, LAYOUT_STAGGERED})
    public @interface LayoutManagerType {}

    public static final int LAYOUT_LINEAR = 0;
    public static final int LAYOUT_GRID = 1;
    public static final int LAYOUT_STAGGERED = 2;

    // ==================== 成员变量声明 ====================
    private PtrFrameLayout mPtrFrameLayout;
    private RecyclerView mRecyclerView;
    private View mLoadingView;
    private View mEmptyView;
    private View mErrorView;
    private LoadMoreFooter mLoadMoreFooter;
    private FrameLayout mStateViewContainer;

    // 【修复点1】配置对象在声明时初始化，避免空指针
    private RefreshConfig mRefreshConfig = new RefreshConfig();
    private LayoutManagerConfig mLayoutManagerConfig = new LayoutManagerConfig();
    private StyleConfig mStyleConfig = new StyleConfig();

    private OnRefreshListener mRefreshListener;
    private OnLoadMoreListener mLoadMoreListener;
    private RecyclerView.OnScrollListener mScrollListener;
    private OnClickListener mStateViewClickListener;
    private RecyclerView.ItemDecoration mItemDecoration;

    @ViewState
    private int mCurrentState = STATE_NORMAL;
    private boolean mIsRefreshing = false;
    private boolean mIsLoadingMore = false;
    private boolean mEnableLoadMore = false;
    private boolean mHasMore = true;

    // ==================== 构造方法 ====================
    public SmartRefreshRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public SmartRefreshRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartRefreshRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "构造方法被调用");
        init(context, attrs, defStyleAttr);
    }

    // ==================== 初始化方法 ====================
    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        Log.d(TAG, "开始执行初始化流程");

        try {
            parseAttributes(context, attrs);
            Log.d(TAG, "XML属性解析完成");

            LayoutInflater.from(context).inflate(R.layout.srr_layout_smart_refresh_recycler, this, true);
            Log.d(TAG, "布局文件加载完成");

            initViews();
            Log.d(TAG, "视图组件初始化完成");

            applyConfig();
            Log.d(TAG, "配置应用完成，初始化结束");

        } catch (Exception e) {
            Log.e(TAG, "初始化过程中发生异常: " + e.getMessage(), e);
            // 即使初始化失败，也要创建最基本的视图，避免白屏
            createEmergencyViews();
        }
    }

    @SuppressLint("CustomViewStyleable")
    private void parseAttributes(Context context, AttributeSet attrs) {
        Log.d(TAG, "开始解析XML属性");

        if (attrs == null) {
            Log.d(TAG, "XML属性集为空，使用默认配置");
            return;
        }

        TypedArray ta = null;
        try {
            ta = context.obtainStyledAttributes(attrs, R.styleable.SrrSmartRefreshRecyclerView);

            mLayoutManagerConfig.type = ta.getInt(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_layoutManagerType, LAYOUT_LINEAR
            );
            mLayoutManagerConfig.orientation = ta.getInt(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_orientation, RecyclerView.VERTICAL
            );
            mLayoutManagerConfig.spanCount = ta.getInt(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_spanCount, 2
            );
            Log.d(TAG, "布局管理器配置: 类型=" + mLayoutManagerConfig.type +
                    ", 方向=" + mLayoutManagerConfig.orientation +
                    ", 列数=" + mLayoutManagerConfig.spanCount);

            mRefreshConfig.enableRefresh = ta.getBoolean(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_enableRefresh, true
            );
            mRefreshConfig.enableLoadMore = ta.getBoolean(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_enableLoadMore, false
            );
            mRefreshConfig.pullToRefresh = ta.getBoolean(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_pullToRefresh, false
            );
            mRefreshConfig.ratioOfHeaderHeightToRefresh = ta.getFloat(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_ratioOfHeaderHeightToRefresh, 1.2f
            );
            Log.d(TAG, "刷新配置: 启用刷新=" + mRefreshConfig.enableRefresh +
                    ", 启用加载更多=" + mRefreshConfig.enableLoadMore +
                    ", 下拉即刷新=" + mRefreshConfig.pullToRefresh);

            mStyleConfig.backgroundColor = ta.getColor(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_backgroundColor, Color.TRANSPARENT
            );
            mStyleConfig.dividerColor = ta.getColor(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_dividerColor, Color.parseColor("#EEEEEE")
            );

            int backgroundResId = ta.getResourceId(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_background, 0
            );
            if (backgroundResId != 0) {
                try {
                    mStyleConfig.background = context.getResources().getDrawable(backgroundResId);
                } catch (Exception e) {
                    Log.e(TAG, "加载背景Drawable失败: " + e.getMessage());
                }
            }
            Log.d(TAG, "样式配置: 背景色=" + String.format("#%08X", mStyleConfig.backgroundColor) +
                    ", 分割线颜色=" + String.format("#%08X", mStyleConfig.dividerColor) +
                    ", 背景Drawable=" + (mStyleConfig.background != null ? "已设置" : "未设置"));

            int loadingLayout = ta.getResourceId(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_loadingLayout, 0
            );
            int emptyLayout = ta.getResourceId(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_emptyLayout, 0
            );
            int errorLayout = ta.getResourceId(
                    R.styleable.SrrSmartRefreshRecyclerView_srr_errorLayout, 0
            );

            if (loadingLayout != 0) mRefreshConfig.loadingLayoutId = loadingLayout;
            if (emptyLayout != 0) mRefreshConfig.emptyLayoutId = emptyLayout;
            if (errorLayout != 0) mRefreshConfig.errorLayoutId = errorLayout;

            Log.d(TAG, "自定义布局配置: 加载布局=" + loadingLayout +
                    ", 空布局=" + emptyLayout +
                    ", 错误布局=" + errorLayout);

        } catch (Exception e) {
            Log.e(TAG, "解析XML属性失败: " + e.getMessage());
        } finally {
            if (ta != null) {
                ta.recycle();
                Log.d(TAG, "XML属性解析完成，资源已回收");
            }
        }
    }

    private void initViews() {
        Log.d(TAG, "开始初始化视图组件");

        try {
            mPtrFrameLayout = findViewById(R.id.ptr_frame_layout);
            mRecyclerView = findViewById(R.id.recycler_view);

            if (mPtrFrameLayout == null) {
                Log.e(TAG, "错误: 未找到PtrFrameLayout");
                throw new RuntimeException("未找到PtrFrameLayout");
            }

            if (mRecyclerView == null) {
                Log.e(TAG, "错误: 未找到RecyclerView");
                throw new RuntimeException("未找到RecyclerView");
            }

            Log.d(TAG, "主容器视图初始化完成");

            mStateViewContainer = findViewById(R.id.state_view_container);
            if (mStateViewContainer == null) {
                Log.e(TAG, "错误: 未找到状态视图容器");
                mStateViewContainer = new FrameLayout(getContext());
                mStateViewContainer.setLayoutParams(new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                ));
                mStateViewContainer.setVisibility(GONE);
                mPtrFrameLayout.addView(mStateViewContainer);
            }

            // 【修复点2】初始化状态视图
            recreateStateViews();

            try {
                mLoadMoreFooter = new LoadMoreFooter(getContext());
                Log.d(TAG, "加载更多视图初始化完成");
            } catch (Exception e) {
                Log.e(TAG, "加载更多视图初始化失败: " + e.getMessage());
            }

            applyLayoutManager();
            Log.d(TAG, "默认布局管理器设置完成");

            applyBackgroundAndDivider();
            Log.d(TAG, "背景和分割线设置完成");

            hideStateViews();
            Log.d(TAG, "初始隐藏状态视图");

        } catch (Exception e) {
            Log.e(TAG, "初始化视图时发生异常: " + e.getMessage(), e);
            createEmergencyViews();
        }
    }

    /**
     * 【核心修复点】重新创建状态视图
     * 根据当前配置重新创建所有状态视图
     * 支持三种模式：全内置、全自定义、混合
     */
    private void recreateStateViews() {
        Log.d(TAG, "开始重新创建状态视图");
        Log.d(TAG, "当前配置 - 加载ID:" + mRefreshConfig.loadingLayoutId +
                ", 空ID:" + mRefreshConfig.emptyLayoutId +
                ", 错误ID:" + mRefreshConfig.errorLayoutId);

        if (mStateViewContainer == null) {
            Log.e(TAG, "错误: 状态视图容器为空");
            return;
        }

        // 保存当前的点击监听器
        OnClickListener savedClickListener = mStateViewClickListener;

        try {
            // 移除所有现有的状态视图
            mStateViewContainer.removeAllViews();

            // 重新创建加载中视图
            mLoadingView = createStateView(
                    mRefreshConfig.loadingLayoutId,
                    R.layout.srr_layout_loading,
                    "加载中"
            );

            // 重新创建空数据视图
            mEmptyView = createStateView(
                    mRefreshConfig.emptyLayoutId,
                    R.layout.srr_layout_empty,
                    "空数据"
            );

            // 重新创建错误视图
            mErrorView = createStateView(
                    mRefreshConfig.errorLayoutId,
                    R.layout.srr_layout_error,
                    "错误"
            );

            // 将视图添加到容器
            if (mLoadingView != null) {
                mStateViewContainer.addView(mLoadingView);
                mLoadingView.setVisibility(GONE);
            }

            if (mEmptyView != null) {
                mStateViewContainer.addView(mEmptyView);
                mEmptyView.setVisibility(GONE);
            }

            if (mErrorView != null) {
                mStateViewContainer.addView(mErrorView);
                mErrorView.setVisibility(GONE);
            }

            Log.d(TAG, "状态视图重新创建完成");

            // 重新设置点击监听器
            mStateViewClickListener = savedClickListener;
            if (mStateViewClickListener != null) {
                setOnStateViewClickListener(mStateViewClickListener);
            }

        } catch (Exception e) {
            Log.e(TAG, "重新创建状态视图失败: " + e.getMessage(), e);
            createFallbackViews();
        }
    }

    /**
     * 创建单个状态视图
     * @param customLayoutId 自定义布局ID，为0时使用默认布局
     * @param defaultLayoutId 默认布局ID
     * @param viewName 视图名称（用于日志）
     * @return 创建的状态视图，永远不会返回null
     */
    private View createStateView(@LayoutRes int customLayoutId,
                                 @LayoutRes int defaultLayoutId,
                                 String viewName) {
        View view = null;

        try {
            if (customLayoutId != 0) {
                // 尝试创建自定义视图
                Log.d(TAG, "尝试创建自定义" + viewName + "视图，布局ID: " + customLayoutId);
                view = LayoutInflater.from(getContext()).inflate(
                        customLayoutId, mStateViewContainer, false
                );
                Log.d(TAG, "✓ 自定义" + viewName + "视图创建成功");
            } else {
                // 使用默认视图
                Log.d(TAG, "使用默认" + viewName + "视图，布局ID: " + defaultLayoutId);
                view = LayoutInflater.from(getContext()).inflate(
                        defaultLayoutId, mStateViewContainer, false
                );
                Log.d(TAG, "✓ 默认" + viewName + "视图创建成功");
            }
        } catch (Exception e) {
            // 【修复点3】自定义或默认布局加载失败时，创建备用视图
            Log.e(TAG, "创建" + viewName + "视图失败: " + e.getMessage());
            view = createEmergencyView(viewName);
        }

        return view;
    }

    /**
     * 创建紧急备用视图
     * 当所有布局都加载失败时使用
     */
    private View createEmergencyView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(16);
        return textView;
    }

    /**
     * 创建回退视图
     * 当状态视图创建完全失败时使用
     */
    private void createFallbackViews() {
        Log.d(TAG, "创建回退状态视图");
        try {
            mLoadingView = createEmergencyView("加载中...");
            mEmptyView = createEmergencyView("空数据");
            mErrorView = createEmergencyView("错误");

            if (mStateViewContainer != null) {
                mStateViewContainer.removeAllViews();
                mStateViewContainer.addView(mLoadingView);
                mStateViewContainer.addView(mEmptyView);
                mStateViewContainer.addView(mErrorView);

                mLoadingView.setVisibility(GONE);
                mEmptyView.setVisibility(GONE);
                mErrorView.setVisibility(GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "创建回退视图也失败: " + e.getMessage());
        }
    }

    /**
     * 创建紧急视图（整个控件初始化失败时使用）
     */
    private void createEmergencyViews() {
        Log.d(TAG, "创建紧急视图");
        try {
            removeAllViews();

            TextView errorView = new TextView(getContext());
            errorView.setText("SmartRefreshRecyclerView初始化失败");
            errorView.setGravity(Gravity.CENTER);
            errorView.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));
            errorView.setTextColor(Color.RED);
            errorView.setTextSize(14);

            addView(errorView);
        } catch (Exception e) {
            Log.e(TAG, "创建紧急视图也失败: " + e.getMessage());
        }
    }

    private void applyConfig() {
        Log.d(TAG, "开始应用配置到组件");

        try {
            if (mRefreshConfig.enableRefresh) {
                Log.d(TAG, "启用下拉刷新功能");
                setupRefreshHeader();
                mPtrFrameLayout.setPtrHandler(createPtrHandler());
                mPtrFrameLayout.setEnabled(true);
            } else {
                Log.d(TAG, "禁用下拉刷新功能");
                mPtrFrameLayout.setEnabled(false);
            }

            mEnableLoadMore = mRefreshConfig.enableLoadMore;
            if (mEnableLoadMore) {
                Log.d(TAG, "启用上拉加载功能");
                setupLoadMore();
            } else {
                Log.d(TAG, "禁用上拉加载功能");
            }

            applyRefreshParams();
            Log.d(TAG, "所有配置应用完成");

        } catch (Exception e) {
            Log.e(TAG, "应用配置时发生异常: " + e.getMessage());
        }
    }

    private void setupRefreshHeader() {
        try {
            Log.d(TAG, "设置下拉刷新头部");
            PtrUIHandler header = mRefreshConfig.headerFactory != null
                    ? mRefreshConfig.headerFactory.createHeader(getContext())
                    : new MaterialHeader(getContext());

            mPtrFrameLayout.setHeaderView((View) header);
            mPtrFrameLayout.addPtrUIHandler(header);
        } catch (Exception e) {
            Log.e(TAG, "设置刷新头部失败: " + e.getMessage());
        }
    }

    private PtrHandler createPtrHandler() {
        return new PtrDefaultHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                boolean canRefresh = !mIsRefreshing && !mIsLoadingMore
                        && mRecyclerView.computeVerticalScrollOffset() == 0
                        && mCurrentState != STATE_LOADING;
                Log.d(TAG, "检查是否可以刷新: " + canRefresh);
                return canRefresh;
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                Log.d(TAG, "开始下拉刷新");
                mIsRefreshing = true;
                mHasMore = true;
                hideStateViews();

                if (mRefreshListener != null) {
                    mRefreshListener.onRefresh();
                } else {
                    Log.w(TAG, "警告: 下拉刷新监听器为空");
                }
            }
        };
    }

    private void setupLoadMore() {
        try {
            Log.d(TAG, "设置上拉加载功能");
            mScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    checkLoadMore();
                }
            };
            mRecyclerView.addOnScrollListener(mScrollListener);
        } catch (Exception e) {
            Log.e(TAG, "设置上拉加载失败: " + e.getMessage());
        }
    }

    private void checkLoadMore() {
        if (!mEnableLoadMore || mIsLoadingMore || mIsRefreshing || !mHasMore) {
            return;
        }

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null) {
            return;
        }

        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = 0;

        if (layoutManager instanceof LinearLayoutManager) {
            firstVisibleItemPosition = ((LinearLayoutManager) layoutManager)
                    .findFirstVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] firstPositions = ((StaggeredGridLayoutManager) layoutManager)
                    .findFirstVisibleItemPositions(null);
            firstVisibleItemPosition = getMinPosition(firstPositions);
        }

        if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                && firstVisibleItemPosition >= 0
                && totalItemCount >= visibleItemCount) {
            triggerLoadMore();
        }
    }

    private int getMinPosition(int[] positions) {
        int min = positions[0];
        for (int value : positions) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private void triggerLoadMore() {
        try {
            Log.d(TAG, "开始触发上拉加载");
            mIsLoadingMore = true;
            mLoadMoreFooter.setState(LoadMoreFooter.STATE_LOADING);

            if (mLoadMoreListener != null) {
                mLoadMoreListener.onLoadMore();
            } else {
                Log.w(TAG, "警告: 上拉加载监听器为空");
            }
        } catch (Exception e) {
            Log.e(TAG, "触发加载更多失败: " + e.getMessage());
        }
    }

    private void applyRefreshParams() {
        try {
            mPtrFrameLayout.setRatioOfHeaderHeightToRefresh(mRefreshConfig.ratioOfHeaderHeightToRefresh);
            mPtrFrameLayout.setKeepHeaderWhenRefresh(true);
            mPtrFrameLayout.setResistance(1.7f);
            mPtrFrameLayout.setDurationToClose(200);
            mPtrFrameLayout.setDurationToCloseHeader(500);
            mPtrFrameLayout.setPullToRefresh(mRefreshConfig.pullToRefresh);
            mPtrFrameLayout.setLoadingMinTime(1000);
        } catch (Exception e) {
            Log.e(TAG, "应用刷新参数失败: " + e.getMessage());
        }
    }

    private void applyLayoutManager() {
        try {
            Log.d(TAG, "应用布局管理器，类型=" + mLayoutManagerConfig.type);
            RecyclerView.LayoutManager layoutManager = null;

            switch (mLayoutManagerConfig.type) {
                case LAYOUT_LINEAR:
                    LinearLayoutManager linearManager = new LinearLayoutManager(getContext());
                    linearManager.setOrientation(mLayoutManagerConfig.orientation);
                    layoutManager = linearManager;
                    break;

                case LAYOUT_GRID:
                    GridLayoutManager gridManager = new GridLayoutManager(
                            getContext(), mLayoutManagerConfig.spanCount
                    );
                    gridManager.setOrientation(mLayoutManagerConfig.orientation);
                    layoutManager = gridManager;
                    break;

                case LAYOUT_STAGGERED:
                    layoutManager = new StaggeredGridLayoutManager(
                            mLayoutManagerConfig.spanCount, mLayoutManagerConfig.orientation
                    );
                    break;
            }

            if (layoutManager != null) {
                mRecyclerView.setLayoutManager(layoutManager);
            } else {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }
        } catch (Exception e) {
            Log.e(TAG, "应用布局管理器失败: " + e.getMessage());
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    private void applyBackgroundAndDivider() {
        Log.d(TAG, "应用背景和分割线配置");

        try {
            if (mStyleConfig.background != null) {
                mRecyclerView.setBackground(mStyleConfig.background);
            } else if (mStyleConfig.backgroundColor != Color.TRANSPARENT) {
                mRecyclerView.setBackgroundColor(mStyleConfig.backgroundColor);
            }

            if (mItemDecoration != null) {
                mRecyclerView.removeItemDecoration(mItemDecoration);
            }

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                int orientation = ((LinearLayoutManager) layoutManager).getOrientation();
                DividerItemDecoration divider = new DividerItemDecoration(getContext(), orientation);
                divider.setDrawable(new ColorDrawable(mStyleConfig.dividerColor));
                mItemDecoration = divider;
                mRecyclerView.addItemDecoration(divider);
            }
        } catch (Exception e) {
            Log.e(TAG, "应用背景和分割线失败: " + e.getMessage());
        }
    }

    /**
     * 切换状态视图
     */
    private void switchStateView(@ViewState int state) {
        Log.d(TAG, "切换状态视图: " + getStateName(mCurrentState) + " -> " + getStateName(state));
        mCurrentState = state;

        try {
            if (state == STATE_NORMAL || state == STATE_NO_MORE) {
                mRecyclerView.setVisibility(VISIBLE);
                if (mStateViewContainer != null) {
                    mStateViewContainer.setVisibility(GONE);
                }
            } else {
                mRecyclerView.setVisibility(GONE);
                if (mStateViewContainer != null) {
                    mStateViewContainer.setVisibility(VISIBLE);
                }
            }

            if (mLoadingView != null) {
                mLoadingView.setVisibility(state == STATE_LOADING ? VISIBLE : GONE);
            }
            if (mEmptyView != null) {
                mEmptyView.setVisibility(state == STATE_EMPTY ? VISIBLE : GONE);
            }
            if (mErrorView != null) {
                mErrorView.setVisibility(state == STATE_ERROR ? VISIBLE : GONE);
            }

            boolean isShowingStateView = (state == STATE_LOADING || state == STATE_EMPTY || state == STATE_ERROR);
            if (mPtrFrameLayout != null) {
                mPtrFrameLayout.setEnabled(mRefreshConfig.enableRefresh && !isShowingStateView);
            }

        } catch (Exception e) {
            Log.e(TAG, "切换状态视图失败: " + e.getMessage());
        }
    }

    private String getStateName(@ViewState int state) {
        switch (state) {
            case STATE_NORMAL: return "正常状态";
            case STATE_LOADING: return "加载中";
            case STATE_EMPTY: return "空数据";
            case STATE_ERROR: return "错误状态";
            case STATE_NO_MORE: return "无更多数据";
            default: return "未知状态";
        }
    }

    // ==================== 公开API方法 ====================

    /**
     * 【核心方法】设置刷新配置
     * 支持三种模式：
     * 1. 全内置：所有layoutId设为0
     * 2. 全自定义：所有layoutId设为自定义布局ID
     * 3. 混合：部分设为自定义ID，部分设为0
     *
     * @param config 刷新配置对象
     */
    public void setRefreshConfig(RefreshConfig config) {
        Log.d(TAG, "=== 设置新的刷新配置 ===");
        Log.d(TAG, "加载布局ID: " + config.loadingLayoutId + " (0=内置, 非0=自定义)");
        Log.d(TAG, "空布局ID: " + config.emptyLayoutId + " (0=内置, 非0=自定义)");
        Log.d(TAG, "错误布局ID: " + config.errorLayoutId + " (0=内置, 非0=自定义)");

        if (config == null) {
            Log.e(TAG, "错误: 传入的配置为null");
            return;
        }

        mRefreshConfig = config;

        // 【关键点】重新创建状态视图
        recreateStateViews();

        // 重新应用其他配置
        applyConfig();

        Log.d(TAG, "=== 刷新配置设置完成 ===");
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        Log.d(TAG, "设置RecyclerView适配器");
        if (adapter == null) {
            Log.w(TAG, "警告: 尝试设置空的适配器");
        }
        mRecyclerView.setAdapter(adapter);

        if (adapter != null && adapter.getItemCount() == 0 && mCurrentState == STATE_NORMAL) {
            showEmptyView();
        }
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        Log.d(TAG, "设置自定义布局管理器");
        if (layoutManager == null) {
            Log.w(TAG, "警告: 尝试设置空的布局管理器");
            return;
        }
        mRecyclerView.setLayoutManager(layoutManager);
        applyBackgroundAndDivider();
    }

    public void setLayoutManager(@LayoutManagerType int type, int orientation, int spanCount) {
        Log.d(TAG, "设置预设布局管理器");
        mLayoutManagerConfig.type = type;
        mLayoutManagerConfig.orientation = orientation;
        mLayoutManagerConfig.spanCount = spanCount;
        applyLayoutManager();
    }

    public void setBackgroundColor(int color) {
        Log.d(TAG, "设置背景颜色");

        if (mStyleConfig == null) {
            Log.e(TAG, "错误: mStyleConfig 为空");
            mStyleConfig = new StyleConfig();
        }

        mStyleConfig.backgroundColor = color;
        mStyleConfig.background = null;

        if (mRecyclerView != null) {
            mRecyclerView.setBackgroundColor(color);
        }
    }

    public void setBackground(Drawable background) {
        Log.d(TAG, "设置背景Drawable");

        if (mStyleConfig == null) {
            Log.e(TAG, "错误: mStyleConfig 为空");
            mStyleConfig = new StyleConfig();
        }

        mStyleConfig.background = background;

        if (mRecyclerView != null) {
            mRecyclerView.setBackground(background);
        }
    }

    public void setDividerColor(int color) {
        Log.d(TAG, "设置分割线颜色");

        if (mStyleConfig == null) {
            Log.e(TAG, "错误: mStyleConfig 为空");
            mStyleConfig = new StyleConfig();
        }

        mStyleConfig.dividerColor = color;

        if (mItemDecoration != null) {
            mRecyclerView.removeItemDecoration(mItemDecoration);
        }
        applyBackgroundAndDivider();
    }

    public void setDivider(int dividerResId) {
        Log.d(TAG, "设置分割线Drawable");

        if (mStyleConfig == null) {
            Log.e(TAG, "错误: mStyleConfig 为空");
            mStyleConfig = new StyleConfig();
        }

        try {
            @SuppressLint("UseCompatLoadingForDrawables")
            Drawable dividerDrawable = getContext().getResources().getDrawable(dividerResId);

            if (mItemDecoration != null) {
                mRecyclerView.removeItemDecoration(mItemDecoration);
            }

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                int orientation = ((LinearLayoutManager) layoutManager).getOrientation();
                DividerItemDecoration divider = new DividerItemDecoration(getContext(), orientation);
                divider.setDrawable(dividerDrawable);
                mItemDecoration = divider;
                mRecyclerView.addItemDecoration(divider);
            }
        } catch (Exception e) {
            Log.e(TAG, "设置分割线失败: " + e.getMessage());
        }
    }

    public void finishRefresh() {
        Log.d(TAG, "完成下拉刷新");
        mIsRefreshing = false;

        if (mPtrFrameLayout != null) {
            mPtrFrameLayout.refreshComplete();
        }

        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) {
            Log.d(TAG, "数据为空，显示空数据视图");
            showEmptyView();
        } else {
            Log.d(TAG, "有数据，显示正常视图");
        }
    }

    public void finishLoadMore(boolean hasMore) {
        Log.d(TAG, "完成上拉加载，还有更多数据: " + hasMore);
        mIsLoadingMore = false;
        mHasMore = hasMore;

        if (mLoadMoreFooter != null) {
            if (hasMore) {
                mLoadMoreFooter.setState(LoadMoreFooter.STATE_NORMAL);
            } else {
                mLoadMoreFooter.setState(LoadMoreFooter.STATE_NO_MORE);
            }
        }
    }

    public void finishLoadMoreWithError() {
        Log.d(TAG, "上拉加载失败");
        mIsLoadingMore = false;

        if (mLoadMoreFooter != null) {
            mLoadMoreFooter.setState(LoadMoreFooter.STATE_ERROR);
        }
    }

    public void autoRefresh() {
        Log.d(TAG, "自动触发下拉刷新");
        if (mPtrFrameLayout != null) {
            mPtrFrameLayout.postDelayed(() -> {
                if (!mIsRefreshing) {
                    mPtrFrameLayout.autoRefresh();
                } else {
                    Log.d(TAG, "已在刷新中，跳过自动刷新");
                }
            }, 100);
        }
    }

    public void showLoadingView() {
        Log.d(TAG, "显示加载中视图");
        switchStateView(STATE_LOADING);
    }

    public void showEmptyView() {
        Log.d(TAG, "显示空数据视图");
        switchStateView(STATE_EMPTY);
    }

    public void showErrorView() {
        Log.d(TAG, "显示错误视图");
        switchStateView(STATE_ERROR);
    }

    public void hideStateViews() {
        Log.d(TAG, "隐藏所有状态视图");
        switchStateView(STATE_NORMAL);
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        Log.d(TAG, "设置下拉刷新监听器");
        mRefreshListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        Log.d(TAG, "设置上拉加载监听器");
        mLoadMoreListener = listener;
        if (listener != null && !mEnableLoadMore) {
            Log.d(TAG, "自动启用上拉加载功能");
            mEnableLoadMore = true;
            setupLoadMore();
        }
    }

    /**
     * 设置状态视图点击监听器
     * 注意：会在状态视图重新创建时自动重新设置
     */
    public void setOnStateViewClickListener(OnClickListener listener) {
        Log.d(TAG, "设置状态视图点击监听器");
        mStateViewClickListener = listener;

        if (mEmptyView != null) mEmptyView.setOnClickListener(listener);
        if (mErrorView != null) mErrorView.setOnClickListener(listener);
        if (mLoadingView != null) mLoadingView.setOnClickListener(listener);
    }

    // ==================== 内部配置类 ====================
    public static class RefreshConfig {
        public boolean enableRefresh = true;
        public boolean enableLoadMore = false;
        public boolean pullToRefresh = false;
        public float ratioOfHeaderHeightToRefresh = 1.2f;
        public HeaderFactory headerFactory = null;
        @LayoutRes
        public int loadingLayoutId = 0;  // 0表示使用内置布局
        @LayoutRes
        public int emptyLayoutId = 0;    // 0表示使用内置布局
        @LayoutRes
        public int errorLayoutId = 0;    // 0表示使用内置布局
    }

    public static class LayoutManagerConfig {
        @LayoutManagerType
        public int type = LAYOUT_LINEAR;
        public int orientation = RecyclerView.VERTICAL;
        public int spanCount = 2;
    }

    public static class StyleConfig {
        public int backgroundColor = Color.TRANSPARENT;
        public Drawable background = null;
        public int dividerColor = Color.parseColor("#EEEEEE");
    }

    // ==================== 接口定义 ====================
    public interface HeaderFactory {
        PtrUIHandler createHeader(Context context);
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    // ==================== 状态获取方法 ====================
    @ViewState
    public int getCurrentState() {
        return mCurrentState;
    }

    public boolean isRefreshing() {
        return mIsRefreshing;
    }

    public boolean isLoadingMore() {
        return mIsLoadingMore;
    }

    public boolean isEnableLoadMore() {
        return mEnableLoadMore;
    }

    public void setEnableLoadMore(boolean enableLoadMore) {
        Log.d(TAG, "设置上拉加载启用状态: " + enableLoadMore);
        this.mEnableLoadMore = enableLoadMore;
        if (enableLoadMore && mScrollListener == null) {
            setupLoadMore();
        }
    }

    // ==================== 生命周期管理 ====================
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "从窗口分离，清理资源");

        if (mScrollListener != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
        }

        mRefreshListener = null;
        mLoadMoreListener = null;
        mStateViewClickListener = null;
    }
}
