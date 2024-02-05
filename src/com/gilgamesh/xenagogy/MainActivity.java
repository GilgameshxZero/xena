package com.gilgamesh.xenagogy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // setContentView(R.layout.activity_main);

    // TextView textView = (TextView)findViewById(R.id.my_text);
    // text.setText("Hello, world!");

    TextView textView = new TextView(this);
    textView.setText(getString(R.string.hello_world));
    setContentView(textView);
  }
}
