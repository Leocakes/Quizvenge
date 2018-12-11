package com.group.a4p61.quizvenge;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

public class ReadQuiz {
    JSONObject rootObj;
    JSONObject currentObj;
    Random random;
    Context context;

    public ReadQuiz(int randomSeed, Context context) {
        this.context=context;
        random = new Random(randomSeed);
        try {
            rootObj = new JSONObject(readString(context.getResources().openRawResource(R.raw.questions)));
        } catch(Exception e) {
            e.printStackTrace();
        }
        getNext();
    }

    public String  getQuestion() {
        try {
            return currentObj.getString("question");
        } catch (Exception e) {

        }
        return null;
    }

    public String[] getIncorrectAnswers() {
        try {
            JSONArray result = currentObj.getJSONArray("incorrect_answers");
            String[] incorrectAnswers = new String[result.length()];
            for(int i=0; i<result.length();i++) {
                incorrectAnswers[i]=(String)result.get(i);
            }
            return incorrectAnswers;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return  null;
    }

    public String getCorrectAnswer() {
        try {
            return currentObj.getString("correct_answer");
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getNext() {
        try {
            JSONArray array = rootObj.getJSONArray("results");
            currentObj = (JSONObject) array.get(random.nextInt(array.length()));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public String readString(InputStream is) {
        StringBuilder builder = new StringBuilder();
        BufferedReader  br;
        try {
            br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            String line = br.readLine();

            while (line != null) {

                builder.append(line);
                line = br.readLine();
            }
            br.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
