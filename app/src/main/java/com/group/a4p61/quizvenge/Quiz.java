package com.group.a4p61.quizvenge;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Quiz extends android.app.Fragment implements View.OnClickListener {
    private ReadQuiz readQuiz;
    private LinearLayout linearLayout;
    private String  correctAnswer;
    public int score;
    public int seconds;
    public Boolean isOver;
    private View view;
    private MessageHandler messageHandler;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.activity_quiz, container, false);


        readQuiz = new ReadQuiz(100,getContext());
        linearLayout =view.findViewById(R.id.linearLayout);
        newQuestion();
        this.isOver=false;
        this.score=0;
        final TextView timerView = view.findViewById(R.id.timer);
        new CountDownTimer(seconds*1000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int)millisUntilFinished/1000;
                timerView.setText(Integer.toString(seconds/60)+":"+((seconds%60)<10?"0":"")+Integer.toString(seconds%60));
            }

            @Override
            public void onFinish() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isOver=true;
                        messageHandler.write(("SCORE:"+Integer.toString(score)).getBytes());
                    }
                }).start();

            }
        }.start();

        return view;
    }

    public void newQuestion() {
        linearLayout.removeAllViews();
        TextView question = view.findViewById(R.id.question);
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
            Button b = new Button(getContext());
            b.setText(qArray[n]);
            b.setOnClickListener(this);
            linearLayout.addView(b);
        }
    }

    public void setMessageHandler(MessageHandler obj) {
        messageHandler = obj;
    }



    @Override
    public void onClick(View v) {
        if(((Button)v).getText().equals(this.correctAnswer)) {
            Toast.makeText(getContext(),"Correct",Toast.LENGTH_SHORT).show();
            this.score++;
            Toast.makeText(getContext(),"Correct",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(),"Incorrect",Toast.LENGTH_SHORT).show();
            this.score--;
        }
        TextView scoreView = view.findViewById(R.id.score);
        scoreView.setText("Score:"+Integer.toString(this.score));
        newQuestion();
    }
}
