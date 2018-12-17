package com.group.a4p61.quizvenge;

import android.content.Intent;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Quiz extends AppCompatActivity implements View.OnClickListener {
    private ReadQuiz readQuiz;
    private LinearLayout linearLayout;
    private String  correctAnswer;
    private int score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        readQuiz = new ReadQuiz(100,this);
        linearLayout =findViewById(R.id.linearLayout);
        newQuestion();
        this.score=0;
    }

    public void newQuestion() {
        linearLayout.removeAllViews();
        TextView question = findViewById(R.id.question);
        String result = readQuiz.getQuestion();
        question.setText(result);
        addButtons();
        readQuiz.getNext();
    }

    public void addButtons() {
        String[] iArray = readQuiz.getIncorrectAnswers();
        this.correctAnswer = readQuiz.getCorrectAnswer();
        String[] qArray = new String[4];
        for(int i=0;i<3;i++) {
            qArray[i]=iArray[i];
        }
        qArray[3]=this.correctAnswer;
        Random r = new Random();
        List<Integer> nums = new LinkedList();
        while(nums.size()<4) {
            Integer num = r.nextInt(4);
            Boolean b = false;
            for(Integer n : nums) {
                if (n==num) {
                    b=true;
                }
            }
            if (!b) {
                nums.add(num);
            }
        }
        for(Integer n : nums) {
            Button b = new Button(this);
            b.setText(qArray[n]);
            b.setOnClickListener(this);
            linearLayout.addView(b);
        }
    }



    @Override
    public void onClick(View v) {
        if(((Button)v).getText().equals(this.correctAnswer)) {
            Toast.makeText(this,"Correct",Toast.LENGTH_SHORT).show();
            this.score++;
            Toast.makeText(this,"Correct",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,"Incorrect",Toast.LENGTH_SHORT).show();
            this.score--;
        }
        newQuestion();
    }
}
