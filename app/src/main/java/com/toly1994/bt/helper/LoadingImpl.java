package com.toly1994.bt.helper;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2018/9/30 0030:10:32<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：
 */
public class LoadingImpl implements ILoading {
    private ProgressDialog mProgressDialog;
    private Context mContext;

    public LoadingImpl(Context context) {
        mContext = context;
    }

    @Override
    public void show(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(mContext, "",
                    msg, true, false);
        } else if (mProgressDialog.isShowing()) {
            mProgressDialog.setTitle("");
            mProgressDialog.setMessage(msg);
        }
        mProgressDialog.show();
    }

    @Override
    public void hide() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
