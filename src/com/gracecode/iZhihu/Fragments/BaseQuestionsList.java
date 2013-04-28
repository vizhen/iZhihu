package com.gracecode.iZhihu.Fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import com.gracecode.iZhihu.Dao.Database;
import com.gracecode.iZhihu.R;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * User: mingcheng
 * Date: 13-4-27
 */
public class BaseQuestionsList extends ListFragment {
    protected Activity activity;
    protected Context context;
    protected Database database;
    protected Cursor questions;

    public BaseQuestionsList() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.activity = getActivity();
        this.context = activity.getApplicationContext();
        this.database = new Database(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_questions, container, false);
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        if (questions != null && questions.moveToPosition(position)) {
            String title = questions.getString(questions.getColumnIndex("question_title"));
            Toast.makeText(activity, title, Toast.LENGTH_SHORT).show();
        }
    }

    public Cursor getRecentQuestion() {
        questions = database.getRecentQuestions();
        return questions;
    }

    public Cursor getFavoritesQuestion() {
        questions = database.getFavoritesQuestion();
        return questions;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

}