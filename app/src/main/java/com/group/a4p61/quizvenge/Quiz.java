package com.group.a4p61.quizvenge;

import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Random;

public class Quiz extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ReadQuiz rq = new ReadQuiz(100,this);
        TextView question = findViewById(R.id.question);
        String result = rq.getQuestion();
        question.setText(result);
    }
}
