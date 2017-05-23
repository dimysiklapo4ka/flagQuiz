package com.example.d1mys1klapo4ka.flagquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


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

    //загрузка следующего флага после правильного ответа
    public void loadNextFlag(){
        //получение имени файла следующего флага и удаление его из списка
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;//обновление правильного ответа
        tvAnswer.setText("");//очистка tvAnswer

        //отображение текущего номера вопроса
        tvQuestionsNumber.setText(getString(
                R.string.question, (correctAnswers + 1), FLAG_IN_QUIZ));

        //извлечение региона из имени следующего изображения
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //использование AssetManager для загрузки следующего изображения
        AssetManager assetManager = getActivity().getAssets();

        //получение обьекта InputStream для ресурса следующего флага
        //и попытка использования InputStream
        try (InputStream stream = assetManager.open(region + "/" + nextImage + ".png")){
            //загрузка изображения через обьект Drawable
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            ivFlag.setImageDrawable(flag);

            animate(false);//анимация появления изображения флага на экране

        } catch (IOException e) {

            Log.e(TAG, "Error loading" + nextImage, e);
        }

        Collections.shuffle(fileNameList);//перестановка имен файлов

        //помещение правильного ответа в конец fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //добавление 2, 4, 6, 8 кнопок в зависимости от значения guessRow
        for (int row = 0; row < guessRow; row++){
            //размещение кнопок в currentTableRow
            for (int column = 0; column < quessLinearLayouts[row].getChildCount(); column++){
                //получение ссылки на Button
                Button newGuessButton = (Button)quessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //назначение названия страны текстом newGuessButton
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //случайная замена одной кнопки правильным ответом
        int row = random.nextInt(guessRow);//выбор случайной строки
        int column = random.nextInt(2);//выбор случайного столбца
        LinearLayout randomRow = quessLinearLayouts[row];//получение строки
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    //метод разбивает имя файла с флагом и возвращает название страны
    private String getCountryName(String name){
        return name.substring(name.indexOf('-') + 1).replace('_',' ');
    }

    //весь макет  появляется или исчезает с экрана
    private void animate(boolean animateOut){
        //предотвращение анимации для первого круга
        if (correctAnswers == 0){
            return;
        }

        //вычисление координат центра
        int centerX = (llQuiz.getLeft() + llQuiz.getRight()) / 2;
        int centerY = (llQuiz.getTop() + llQuiz.getBottom()) / 2;

        //вычисление радиуса анимации
        int radius = Math.max(llQuiz.getWidth(), llQuiz.getHeight());

        Animator animator = null;

        //если изображение должно исчезать с экрана
        if (animateOut){
            //создание круговой анимации
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                animator = ViewAnimationUtils.createCircularReveal(llQuiz, centerX, centerY, radius, 0);

                animator.addListener(new AnimatorListenerAdapter() {
                    //Вызывается при завершении анимации
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadNextFlag();
                    }
                });
            }
        }else {//если макет должен появится
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                animator = ViewAnimationUtils.createCircularReveal(llQuiz, centerX, centerY, 0, radius);
            }
        }

        animator.setDuration(500);//задается продолжительность анимации
        animator.start();//начало анимации
    }

    //вызывается при нажатии кнопки ответа
    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button quessButton = ((Button)v);
            String quess = quessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;//

            if (quess.equals(answer)){//
                ++correctAnswers;//

                //
                tvAnswer.setText(answer + "!");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvAnswer.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));
                }

                disableButtons();//

                //
                if (correctAnswers == FLAG_IN_QUIZ){
                    //
                    DialogFragment quizRezult = new DialogFragment(){

                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses, (1000 / (double)totalGuesses)));

                            //
                            builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    resetQuiz();
                                }
                            });

                            return builder.create();//
                        }
                        //

                    };

                    //
                    quizRezult.setCancelable(false);
                    quizRezult.show(getFragmentManager(), "quiz results");
                }else {
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            animate(true);
                        }
                    }, 2000);//
                }
            }else {
                ivFlag.startAnimation(shakeAnimation);//

                tvAnswer.setText(R.string.incorrect_answer);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tvAnswer.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                }
                quessButton.setEnabled(false);
            }


        }
    };

    //
    private void disableButtons(){
        for (int row = 0; row <guessRow; row++){
            LinearLayout guessRows = quessLinearLayouts[row];
            for (int i = 0; i < guessRows.getChildCount(); i++){
                guessRows.getChildAt(i).setEnabled(false);
            }
        }
    }

}
