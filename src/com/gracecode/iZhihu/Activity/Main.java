package com.gracecode.iZhihu.Activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.gracecode.iZhihu.Fragments.FavoritesList;
import com.gracecode.iZhihu.Fragments.QuestionsList;
import com.gracecode.iZhihu.Listener.MainTabListener;
import com.gracecode.iZhihu.R;
import com.gracecode.iZhihu.Tasks.FetchQuestionTask;

public class Main extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void rebuildTables() {
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.removeAllTabs();

        ActionBar.Tab mainTab = actionBar.newTab()
            .setText(getString(R.string.tab_index))
            .setTabListener(new MainTabListener(context, QuestionsList.class.getName()));

        ActionBar.Tab favoritesTab = actionBar.newTab()
            .setText(getString(R.string.tab_favorite))
            .setTabListener(new MainTabListener(context, FavoritesList.class.getName()));

        actionBar.addTab(mainTab);
        //actionBar.addTab(favoritesTab);
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchQuestionsFromServer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void fetchQuestionsFromServer() {
        new FetchQuestionTask(context, new FetchQuestionTask.Callback() {
            @Override
            public void onPostExecute() {
                rebuildTables();
            }
        }).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                fetchQuestionsFromServer();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}