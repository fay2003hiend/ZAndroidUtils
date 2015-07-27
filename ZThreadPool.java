package com.dx.utils;

import java.util.ArrayList;
import java.util.List;

public class ZThreadPool {
    final String TAG = "ZThreadPool";

    public static abstract class ZThread extends Thread {
        ZThreadPool parent;
        String tag;

        public ZThread(ZThreadPool parent, String tag) {
            this.parent = parent;
            this.tag = tag;
        }

        public void run() {
            if (parent != null) {
                parent.addThread(this);
            }
            try {
                core_run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (parent != null) {
                parent.removeThread(this);
            }
        }

        public abstract void core_run() throws Exception;
    }

    private List<ZThread> threadList = new ArrayList<ZThread>();

    private void addThread(ZThread thread) {
        synchronized (threadList) {
            Log.i(TAG, "addThread " + thread.tag);
            threadList.add(thread);
        }
    }

    private void removeThread(ZThread thread) {
        synchronized (threadList) {
            Log.i(TAG, "removeThread " + thread.tag);
            threadList.remove(thread);
        }
    }

    public boolean hasThread(String tag) {
        boolean ret = false;
        synchronized (threadList) {
            for (ZThread t : threadList) {
                if (t.tag.equals(tag)) {
                    ret = true;
                    break;
                }
            }
        }
        Log.i(TAG, "hasThread:" + tag + " = " + ret);
        return ret;
    }

}
