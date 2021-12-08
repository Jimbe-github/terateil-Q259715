package com.teratail.q259715;
//package com.example.workouttimer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.*;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CountDownTimerActivity extends AppCompatActivity implements Workout.Listener {
  private TextView setTextView, countTextView, statusTextView, lefttimeTextView;
  private ProgressBar progressBar;
  private Button startStopButton, resetButton;
  private View.OnClickListener onStartListener, onStopListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_count_down_timer);

    //データベースからトレーニングの中身を取得する
    Intent intent = getIntent();
    int id = intent.getIntExtra("id", 0);
    final Workout workout = getWorkout(id);

    setTextView = findViewById(R.id.setNumber);
    countTextView = findViewById(R.id.countNumber);
    statusTextView = findViewById(R.id.status);
    lefttimeTextView = findViewById(R.id.timeLeft);

    progressBar = findViewById(R.id.pb);

    startStopButton = findViewById(R.id.button_start_stop);

    resetButton = findViewById(R.id.button_reset);
    resetButton.setOnClickListener(v -> reset(workout));

    onStartListener = v -> workout.start(CountDownTimerActivity.this);
    onStopListener = v -> workout.stop();

    reset(workout);
  }

  private Workout getWorkout(int id) {
    return new Workout("test",10,5,2,2,20); //テスト用
    /*
    try (Cursor cursor = NewListItemActivity.mSQLiteHelper.getData("SELECT * FROM WORKOUTLIST WHERE id=" + id);) {
      while(cursor.moveToNext()) {
        String name = cursor.getString(1);
        int workout_time = cursor.getInt(2);
        int rest_time = cursor.getInt(3);
        int set_count = cursor.getInt(4);
        int set_number = cursor.getInt(5);
        int set_during = cursor.getInt(6);
        return new Workout(name,workout_time,rest_time,set_count,set_number,set_during);
      }
    }
    throw new IllegalArgumentException("データがありません. id="+id);
    */
  }

  private void showWorkout(WorkoutProperties properties) {
    countTextView.setText(""+properties.getCount());
    setTextView.setText(""+properties.getSet());
    statusTextView.setText(""+properties.getStatus().getText());

    if(progressBar.getMax() != properties.getTickMax()) {
      progressBar.setMax(properties.getTickMax());
    }
    if(properties.getStatus() == Workout.Status.Training) {
      progressBar.setProgress(properties.getTickMax() - properties.getTick());
      if(progressBar.getSecondaryProgress() != 0)progressBar.setSecondaryProgress(0);
    } else {
      if(progressBar.getProgress() != 0) progressBar.setProgress(0);
      progressBar.setSecondaryProgress(properties.getTickMax() - properties.getTick());
    }

    int left = properties.getLeftSecond();
    int minute = left / 60;
    int second = left % 60;
    lefttimeTextView.setText(String.format((minute>0?"%1$02d:":"")+"%2$02d", minute, second));
  }

  private void reset(Workout workout) {
    workout.reset();
    showWorkout(workout);

    setStart();
    startStopButton.setEnabled(true);
    resetButton.setEnabled(false);
  }

  @Override
  public void start(WorkoutProperties properties) {
    setStop();
    startStopButton.setEnabled(true);
    resetButton.setEnabled(false);
  }

  @Override
  public void progress(WorkoutProperties properties) {
    showWorkout(properties);
  }

  @Override
  public void finish(WorkoutProperties properties) {
    showWorkout(properties);
    startStopButton.setEnabled(false);
    resetButton.setEnabled(true);
  }

  @Override
  public void stop(WorkoutProperties properties) {
    setRestart();
    startStopButton.setEnabled(true);
    resetButton.setEnabled(true);
  }

  private void setStart() {
    startStopButton.setText("Start");
    startStopButton.setOnClickListener(onStartListener);
  }
  private void setRestart() {
    startStopButton.setText("Restart");
    startStopButton.setOnClickListener(onStartListener);
  }
  private void setStop() {
    startStopButton.setText("Stop");
    startStopButton.setOnClickListener(onStopListener);
  }
}