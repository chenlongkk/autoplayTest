package com.cck.music.autoplay;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 辅助服务
 * @author chenlong
 */
public class MainService extends AccessibilityService {
    private static final String TAG = "AutoPlay";
    private static final String HOME_PAGE = "com.yxcorp.gifshow.HomeActivity";
    private static final String VIDEO_PAGE = "com.yxcorp.gifshow.detail.PhotoDetailActivity";
    private static final int MSG_CLOSE_VIDEO = 32;
    private static final int MSG_SCROLL_BACK = 31;
    private static final int MSG_CLICK_VIDEO = 30;
    private static final long WATCH_TIME = 10000;
    private static boolean sStarted;
    private CharSequence mCurrentPage;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CLOSE_VIDEO:
                    closeVideo();
                    break;
                case MSG_SCROLL_BACK:
                    scrollRecyclerView();
                    break;
                case MSG_CLICK_VIDEO:
                    clickVideo();
                    break;
            }
        }
    };

    public static boolean isStarted() {
        return sStarted;
    }

    /**
     * 辅助服务启动时，系统回调onCreate方法
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sStarted = true;
    }

    /**
     * 辅助服务被关闭
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        sStarted = false;
    }

    /**
     * 辅助服务异常
     */
    @Override
    public void onInterrupt() {
        sStarted = false;
    }

    /**
     * 接收到系统发送的事件
     * @param event
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "event:"+event.toString());
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {//窗口状态发生变化时
            mCurrentPage = event.getClassName();//记录当前的页面名称
            mHandler.removeCallbacksAndMessages(null);
            if (TextUtils.equals(mCurrentPage, HOME_PAGE)) {//如果进入的是首页，则向下滑动一次
                mHandler.sendEmptyMessageDelayed(MSG_SCROLL_BACK, 100);
            } else if (TextUtils.equals(mCurrentPage, VIDEO_PAGE)) {//如果进入的是视频播放页, 则延时10s后关闭
                mHandler.sendEmptyMessageDelayed(MSG_CLOSE_VIDEO, WATCH_TIME);
            }
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {//控件滚动时
            mHandler.sendEmptyMessageDelayed(MSG_CLICK_VIDEO, 100);
        }
    }



    private void clickVideo() {
        Log.d(TAG, "clickVideo");
        if (!TextUtils.equals(mCurrentPage, HOME_PAGE)) {
            Log.d(TAG, "not in home page, skip click video");
        }
        AccessibilityNodeInfo info = findViewById("com.smile.gifmaker","container");
        if (info != null) {
            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            Log.d(TAG, "info null");
        }
    }

    private void scrollRecyclerView() {
        Log.d(TAG, "scroll...");
        AccessibilityNodeInfo info = findViewById("com.smile.gifmaker","recycler_view");
        if (info != null) {
            info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        } else {
            Log.d(TAG, "info null");
        }
    }

    private void closeVideo() {
        Log.d(TAG, "back...");
        AccessibilityNodeInfo info = findViewById("com.smile.gifmaker","back_btn");
        if (info != null) {
            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 公共方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 点击该控件
     *
     * @return true表示点击成功
     */
    public static boolean clickView(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            } else {
                AccessibilityNodeInfo parent = nodeInfo.getParent();
                if (parent != null) {
                    boolean b = clickView(parent);
                    parent.recycle();
                    if (b) return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据getRootInActiveWindow查找包含当前text的控件
     *
     * @param containsText 只要内容包含就会找到（应该是根据drawText找的）
     */
    @Nullable
    public List<AccessibilityNodeInfo> findViewByContainsText(@NonNull String containsText) {
        AccessibilityNodeInfo info = getRootInActiveWindow();
        if (info == null) return null;
        List<AccessibilityNodeInfo> list = info.findAccessibilityNodeInfosByText(containsText);
        info.recycle();
        return list;
    }

    /**
     * 根据getRootInActiveWindow查找和当前text相等的控件
     *
     * @param equalsText 需要找的text
     */
    @Nullable
    public List<AccessibilityNodeInfo> findViewByEqualsText(@NonNull String equalsText) {
        List<AccessibilityNodeInfo> listOld = findViewByContainsText(equalsText);
        if (Utils.isEmptyArray(listOld)) {
            return null;
        }
        ArrayList<AccessibilityNodeInfo> listNew = new ArrayList<>();
        for (AccessibilityNodeInfo ani : listOld) {
            if (ani.getText() != null && equalsText.equals(ani.getText().toString())) {
                listNew.add(ani);
            } else {
                ani.recycle();
            }
        }
        return listNew;
    }

    /**
     * 根据getRootInActiveWindow查找当前id的控件
     *
     * @param pageName 被查找项目的包名:com.android.xxx
     * @param idName   id值:tv_main
     */
    @Nullable
    public AccessibilityNodeInfo findViewById(String pageName, String idName) {
        return findViewById(pageName + ":id/" + idName);
    }

    /**
     * 根据getRootInActiveWindow查找当前id的控件
     *
     * @param idfullName id全称:com.android.xxx:id/tv_main
     */
    @Nullable
    public AccessibilityNodeInfo findViewById(String idfullName) {
        List<AccessibilityNodeInfo> list = findViewByIdList(idfullName);
        return Utils.isEmptyArray(list) ? null : list.get(0);
    }

    /**
     * 根据getRootInActiveWindow查找当前id的控件集合(类似listview这种一个页面重复的id很多)
     *
     * @param idfullName id全称:com.android.xxx:id/tv_main
     */
    @Nullable
    public List<AccessibilityNodeInfo> findViewByIdList(String idfullName) {
        try {
            AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
            if (rootInfo == null) return null;
            List<AccessibilityNodeInfo> list = rootInfo.findAccessibilityNodeInfosByViewId(idfullName);
            rootInfo.recycle();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 只找第一个ClassName
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public AccessibilityNodeInfo findViewByFirstClassName(String className) {
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return null;
        AccessibilityNodeInfo info = findViewByFirstClassName(rootInfo, className);
        rootInfo.recycle();
        return info;
    }

    /**
     * 只找第一个ClassName
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public static AccessibilityNodeInfo findViewByFirstClassName(AccessibilityNodeInfo parent, String className) {
        if (parent == null) return null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            if (className.equals(child.getClassName().toString())) {
                return child;
            }
            AccessibilityNodeInfo childChild = findViewByFirstClassName(child, className);
            child.recycle();
            if (childChild != null) {
                return childChild;
            }
        }
        return null;
    }

    /**
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public List<AccessibilityNodeInfo> findViewByClassName(String className) {
        ArrayList<AccessibilityNodeInfo> list = new ArrayList<>();
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return list;
        findViewByClassName(list, rootInfo, className);
        rootInfo.recycle();
        return list;
    }

    /**
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public static void findViewByClassName(List<AccessibilityNodeInfo> list, AccessibilityNodeInfo parent, String className) {
        if (parent == null) return;
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            if (className.equals(child.getClassName().toString())) {
                list.add(child);
            } else {
                findViewByClassName(list, child, className);
                child.recycle();
            }
        }
    }

    /**
     * 只找第一个相等的ContentDescription
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public AccessibilityNodeInfo findViewByFirstEqualsContentDescription(String contentDescription) {
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return null;
        AccessibilityNodeInfo info = findViewByFirstEqualsContentDescription(rootInfo, contentDescription);
        rootInfo.recycle();
        return info;
    }

    /**
     * 只找第一个相等的ContentDescription
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public static AccessibilityNodeInfo findViewByFirstEqualsContentDescription(AccessibilityNodeInfo parent, String contentDescription) {
        if (parent == null) return null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            CharSequence cd = child.getContentDescription();
            if (cd != null && contentDescription.equals(cd.toString())) {
                return child;
            }
            AccessibilityNodeInfo childChild = findViewByFirstEqualsContentDescription(child, contentDescription);
            child.recycle();
            if (childChild != null) {
                return childChild;
            }
        }
        return null;
    }

    /**
     * 只找第一个包含的ContentDescription
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public AccessibilityNodeInfo findViewByFirstContainsContentDescription(String contentDescription) {
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return null;
        AccessibilityNodeInfo info = findViewByFirstContainsContentDescription(rootInfo, contentDescription);
        rootInfo.recycle();
        return info;
    }

    /**
     * 只找第一个包含的ContentDescription
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public static AccessibilityNodeInfo findViewByFirstContainsContentDescription(AccessibilityNodeInfo parent, String contentDescription) {
        if (parent == null) return null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            CharSequence cd = child.getContentDescription();
            if (cd != null && cd.toString().contains(contentDescription)) {
                return child;
            }
            AccessibilityNodeInfo childChild = findViewByFirstContainsContentDescription(child, contentDescription);
            child.recycle();
            if (childChild != null) {
                return childChild;
            }
        }
        return null;
    }

    /**
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public List<AccessibilityNodeInfo> findViewByContentDescription(String contentDescription) {
        ArrayList<AccessibilityNodeInfo> list = new ArrayList<>();
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return list;
        findViewByContentDescription(list, rootInfo, contentDescription);
        rootInfo.recycle();
        return list;
    }

    /**
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public static void findViewByContentDescription(List<AccessibilityNodeInfo> list, AccessibilityNodeInfo parent, String contentDescription) {
        if (parent == null) return;
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            CharSequence cd = child.getContentDescription();
            if (cd != null && contentDescription.equals(cd.toString())) {
                list.add(child);
            } else {
                findViewByContentDescription(list, child, contentDescription);
                child.recycle();
            }
        }
    }

    /**
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public List<AccessibilityNodeInfo> findViewByRect(Rect rect) {
        ArrayList<AccessibilityNodeInfo> list = new ArrayList<>();
        AccessibilityNodeInfo rootInfo = getRootInActiveWindow();
        if (rootInfo == null) return list;
        findViewByRect(list, rootInfo, rect);
        rootInfo.recycle();
        return list;
    }

    public static Rect mRecycleRect = new Rect();

    /**
     * 此方法效率相对较低,建议使用之后保存id然后根据id进行查找
     */
    public static void findViewByRect(List<AccessibilityNodeInfo> list, AccessibilityNodeInfo parent, Rect rect) {
        if (parent == null) return;
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child == null) continue;
            child.getBoundsInScreen(mRecycleRect);
            if (mRecycleRect.contains(rect)) {
                list.add(child);
            } else {
                findViewByRect(list, child, rect);
                child.recycle();
            }
        }
    }

    /**
     * 由于太多,最好回收这些AccessibilityNodeInfo
     */
    public static void recycleAccessibilityNodeInfo(List<AccessibilityNodeInfo> listInfo) {
        if (Utils.isEmptyArray(listInfo)) return;

        for (AccessibilityNodeInfo info : listInfo) {
            info.recycle();
        }
    }
}

