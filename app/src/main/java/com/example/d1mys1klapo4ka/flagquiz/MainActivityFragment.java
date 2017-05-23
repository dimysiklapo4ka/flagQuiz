package com.example.d1mys1klapo4ka.flagquiz;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    //Строка используется при регистрации извещений об ошибках
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAG_IN_QUIZ = 10;//При каждой игре нужно отгадать заданное кол-во флагов

    private List<String> fileNameList;//Имена файлов с флагами
    private List<String> quizCountriesList;//Страны текущей викторины
    private Set<String> regionsSet;//Регионы текущей викторины
    private String correctAnswer;//Правильная страна для текущего флага
    private int totalGuesses;//Кол-во попыток
    private int correctAnswers;//Кол-во правильных ответов
    private int guessRow;//Кол-во строк с кнопками вариантов
    private SecureRandom random;//Генератор случайных чисел
    private Handler handler;//Для задержки загрузки следующего флага
    private Animation shakeAnimation;//Анимация неправильного ответа

    private LinearLayout llQuiz;//Макет с викториной
    private TextView tvQuestionsNumber;//Номер текущего вопроса
    private ImageView ivFlag;//Для вывода флага
    private LinearLayout[] quessLinearLayouts;//Строки с кнопками
    private TextView tvAnswer;//Для правильного ответа

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //Загрузка анимации для неправильных ответов
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        //Получение ссылок на компоненты графического интерфейса
        llQuiz = (LinearLayout)view.findViewById(R.id.ll_quiz);
        tvQuestionsNumber = (TextView)view.findViewById(R.id.tv_questionNumber);
        ivFlag = (ImageView)view.findViewById(R.id.iv_flag);
        quessLinearLayouts = new LinearLayout[4];
        quessLinearLayouts[0] = (LinearLayout)view.findViewById(R.id.ll_row1);
        quessLinearLayouts[1] = (LinearLayout)view.findViewById(R.id.ll_row2);
        quessLinearLayouts[2] = (LinearLayout)view.findViewById(R.id.ll_row3);
        quessLinearLayouts[3] = (LinearLayout)view.findViewById(R.id.ll_row4);

        tvAnswer = (TextView)view.findViewById(R.id.tv_answer);

        //Настройка слушателей для кнопок ответов
        for (LinearLayout row : quessLinearLayouts){

            for (int colum = 0; colum < row.getChildCount(); colum++){
                Button button = (Button)row.getChildAt(colum);
                button.setOnClickListener(guessButtonListener);
            }

        }

        //Назначение текста для номера вопроса
        tvQuestionsNumber.setText(getString(R.string.question, 1, FLAG_IN_QUIZ));
        return view;
    }

    //Обновление количества строк с кнопками на основании обьекта SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences){

        //получение количества отображаемых вариантов ответа
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRow = Integer.parseInt(choices) / 2;

        //все компоненты LinearLayout скрываются
        for (LinearLayout layout : quessLinearLayouts){
            layout.setVisibility(View.GONE);
        }

        //отображаются нужные компоненты linearLayout
        for (int row = 0; row <guessRow; row++){
            quessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    //Обновление выбранных регионов по данным из SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    //Настройка и запуск следующей серии вопросов
    public void resetQuiz(){
        //
        AssetManager asset = getActivity().getAssets();
        fileNameList.clear();//

        try {
            //перебор всех регионов
            for (String region : regionsSet){
                String[] paths = asset.list(region);

                for (String path : paths){
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }

        correctAnswers = 0;//сброс количества правильных ответов
        totalGuesses = 0;//сброс общего количества попыток
        quizCountriesList.clear();//

        int flagCountries = 1;
        int numberOfFlag = fileNameList.size();

        //добавление FLAG_IN_QUIZ случайных файлов в quizCountriesList
        while (flagCountries <= FLAG_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlag);

            //получение случайного имени файла
            String filename = fileNameList.get(randomIndex);

            //Если регион включен но еще не был выбран
            if (!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);//
                ++flagCountries;
            }
        }

        loadNextFlag();
    }

    //
    public void loadNextFlag(){
        //
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;//
        tvAnswer.setText("");//

        //
        tvQuestionsNumber.setText(getString(
                R.string.question, (correctAnswers + 1), FLAG_IN_QUIZ));

        //
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //
        AssetManager assetManager = getActivity().getAssets();

        //
        //
        try (InputStream stream = assetManager.open(region + "/" + nextImage + ".png")){
            //
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            ivFlag.setImageDrawable(flag);

            animate(false);

        } catch (IOException e) {

            Log.e(TAG, "Error loading" + nextImage, e);
        }

        Collections.shuffle(fileNameList);//

        //
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //
        for (int row = 0; row < guessRow; row++){
            //
            for (int column = 0; column < quessLinearLayouts[row].getChildCount(); column++){
                //
                Button newGuessButton = (Button)quessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //
        int row = random.nextInt(guessRow);//
        int column = random.nextInt(2);//
        LinearLayout randomRow = quessLinearLayouts[row];//
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }
}
