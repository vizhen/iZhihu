package com.gracecode.iZhihu.activity;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.gracecode.iZhihu.R;
import com.gracecode.iZhihu.adapter.QuestionsAdapter;
import com.gracecode.iZhihu.dao.Question;
import com.gracecode.iZhihu.fragment.QuestionsListFragment;
import com.gracecode.iZhihu.fragment.ScrollTabsFragment;
import com.gracecode.iZhihu.task.FetchQuestionTask;
import com.gracecode.iZhihu.task.SearchQuestionTask;
import com.gracecode.iZhihu.util.Helper;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends BaseActivity implements MenuItem.OnActionExpandListener {
    private static final int MESSAGE_UPDATE_LOADING = 0x01;
    private static final int MESSAGE_UPDATE_COMPLETE = 0x02;
    public static final int MESSAGE_UPDATE_SHOW_RESULT = 0x03;

    private ScrollTabsFragment mScrollTabsFragment;
    private MenuItem mMenuRefersh;
    private FetchQuestionTask mFetchQuestionsTask;
    private Boolean mFocusRefresh = false;
    private InputMethodManager mInputMethodManager;
    private AutoCompleteTextView mAutoCompleteTextView;
    private MenuItem mMenuSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScrollTabsFragment = new ScrollTabsFragment();
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, mScrollTabsFragment)
                .commit();
    }


    @Override
    protected void onStart() {
        super.onStart();

        mFetchQuestionsTask = new FetchQuestionTask(context, new FetchQuestionTask.Callback() {
            @Override
            public void onFinished() {
                UIChangedChangedHandler.sendEmptyMessage(MESSAGE_UPDATE_COMPLETE);
                if (mFocusRefresh) {
                    UIChangedChangedHandler.sendEmptyMessage(MESSAGE_UPDATE_SHOW_RESULT);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        UIChangedChangedHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Helper.isNetworkConnected(context)) {
                    fetchQuestionsFromServer(false);
                }
            }
        }, 1000);

        if (mMenuSearchView != null) {
            mMenuSearchView.collapseActionView();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        mMenuRefersh = menu.findItem(R.id.menu_refresh);

        mMenuSearchView = menu.findItem(R.id.menu_search);
        mMenuSearchView.setOnActionExpandListener(this);
        return true;
    }


    /**
     * 刷新 UI 线程集中的地方
     */
    private final android.os.Handler UIChangedChangedHandler = new android.os.Handler() {
        /**
         * 判断是否第一次启动
         *
         * @return Boolean
         */
        private boolean isFirstRun() {
            Boolean status = mSharedPreferences.getBoolean(getString(R.string.app_name), true);
            if (status) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(getString(R.string.app_name), false);
                editor.commit();
            }
            return status;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_LOADING:
                    if (isFirstRun()) {
                        progressDialog = ProgressDialog.show(
                                Main.this,
                                getString(R.string.app_name), getString(R.string.loading), false, false);
                    }

                    Animation rotation = AnimationUtils.loadAnimation(context, R.anim.refresh_rotate);
                    RelativeLayout layout = new RelativeLayout(context);
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                    ImageView imageView = new ImageView(context);
                    imageView.setLayoutParams(params);

                    layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.TOP);
                    layout.addView(imageView);

                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_refersh));
                    imageView.startAnimation(rotation);

                    if (mMenuRefersh != null) {
                        mMenuRefersh.setActionView(layout);
                    }
                    break;

                case MESSAGE_UPDATE_COMPLETE:
                    try {
                        Fragment fragment = mScrollTabsFragment.getCurrentFragment();
                        if (fragment instanceof QuestionsListFragment) {
                            ((QuestionsListFragment) fragment).updateQuestionsFromDatabase();
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } finally {
                        mScrollTabsFragment.notifyDatasetChanged();
                    }

                    if (mMenuRefersh != null) {
                        View v = mMenuRefersh.getActionView();
                        if (v != null) {
                            v.clearAnimation();
                        }
                        mMenuRefersh.setActionView(null);
                    }

                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }

                    if (mFetchQuestionsTask.hasError()) {
                        Helper.showShortToast(context, mFetchQuestionsTask.getErrorMessage());
                    }

                    break;

                case MESSAGE_UPDATE_SHOW_RESULT:
                    if (mFetchQuestionsTask != null) {
                        String message = getString(R.string.no_newer_questions);
                        if (mFetchQuestionsTask.getAffectedRows() > 0) {
                            message = String.format(getString(R.string.affectRows), mFetchQuestionsTask.getAffectedRows());
                        }

                        Helper.showShortToast(context, message);
                    }
                    break;
            }
        }
    };


    private ProgressDialog progressDialog;

    /**
     * 从服务器获取条目
     *
     * @param focus 强制刷新
     */
    void fetchQuestionsFromServer(final Boolean focus) {
        UIChangedChangedHandler.sendEmptyMessage(MESSAGE_UPDATE_LOADING);

        Boolean isNeedCacheThumbnails = mSharedPreferences.getBoolean(context.getString(R.string.key_enable_cache), true);
        mFetchQuestionsTask.setIsNeedCacheThumbnails(isNeedCacheThumbnails);

        this.mFocusRefresh = focus;
        // Start sync from new thread.
        mFetchQuestionsTask.start(focus);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                fetchQuestionsFromServer(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem menuItem) {
        mAutoCompleteTextView = (AutoCompleteTextView) menuItem.getActionView().findViewById(R.id.search);

        questionsAdapter = new QuestionsAdapter(this, searchedQuestions);
        questionsAdapter.setHideDescription(true); // Hide Description

        mAutoCompleteTextView.addTextChangedListener(textWatcher);
        mAutoCompleteTextView.setAdapter(questionsAdapter);
        mAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Helper.startDetailActivity(Main.this, searchedQuestions, i);
            }
        });

        // Request focus.
        mAutoCompleteTextView.requestFocus();
        (new Timer()).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        mInputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }, 10
        );
        return true;
    }

    ArrayList<Question> searchedQuestions = new ArrayList<Question>();
    QuestionsAdapter questionsAdapter;

    @Override
    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
        if (mAutoCompleteTextView != null) {
            mInputMethodManager.hideSoftInputFromWindow(mAutoCompleteTextView.getApplicationWindowToken(), 0);
            mAutoCompleteTextView.setText("");
        }
        return true;
    }


    /**
     *
     */
    private TextWatcher textWatcher = new TextWatcher() {
        private String searchString = "";

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            searchString = editable.toString().trim();

            if (searchString != null && searchString.length() > 0) {
                new SearchQuestionTask(context, new SearchQuestionTask.Callback() {
                    @Override
                    public void onPreExecute() {
                        // ...
                    }

                    @Override
                    public void onPostExecute(ArrayList<Question> result) {
                        searchedQuestions.clear();
                        searchedQuestions.addAll(result);
                        if (questionsAdapter != null) {
                            questionsAdapter.notifyDataSetChanged();
                        }
                    }
                }).execute(searchString);
            }
        }
    };
}
