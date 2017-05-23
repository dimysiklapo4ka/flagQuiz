package com.example.d1mys1klapo4ka.flagquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //ключи для чтения данных из sharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true;//Включение портретного режима
    private boolean preferencesChanged = true;//Для уведомления об изменении настроек.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Задание значений по умолчанию в файле sharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //Регистрация слушателя для изменений sharedPreferences
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(prefereferencesChangeListener);

        //Определение размеров экрана
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        //для планшета значение phoneDevice присваивается false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE){

            phoneDevice = false;//т.е не соответствует размерам экрана
        }

        //Установка для телефонов только портретной ориентации
        if (phoneDevice){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged){
            /**После задания настроек по умолчанию инициализировать MainActivityFragment
             *и запустить викторину */
            MainActivityFragment quizFragment = (MainActivityFragment)getSupportFragmentManager()
                    .findFragmentById(R.id.quizFragment);//находим content_main

            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));

            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    //Иконка меню должна отображаться на устройстве с портретной ориентацией
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //Получаем текущую ориентацию экрана
        int orientation = getResources().getConfiguration().orientation;

        //заполнение меню
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            getMenuInflater().inflate(R.menu.menu_main, menu);

            return true;
        }else {
            return false;
        }
    }

    //отображение SettingsActivity при запуске устройства в портретной ориентации
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);

        return super.onOptionsItemSelected(item);
    }

    //слушатель изменения конфигурации SharedPreferences приложения
    private SharedPreferences.OnSharedPreferenceChangeListener prefereferencesChangeListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {

                //метод вызывается при внесении изменений в настройки
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                preferencesChanged = true;//Свидетельствует что настройки были изменены

                    MainActivityFragment quizFragment = (MainActivityFragment)
                            getSupportFragmentManager().findFragmentById(R.id.quizFragment);

                    if (key.equals(CHOICES)){//если изменилось количество вариантов ответа

                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();

                    }else if(key.equals(REGIONS)) {//Если изменился регион

                        Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                        if (regions != null && regions.size() > 0) {

                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();

                        } else {
                            //должен быть хоть один регион, по умолчанию Северная Америка
                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            regions.add(getString(R.string.default_region));
                            editor.putStringSet(REGIONS, regions);
                            editor.apply();

                            Toast.makeText(MainActivity.this, R.string.default_region, Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                }
            };


}
