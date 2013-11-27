package com.example.wal;

import java.util.LinkedList;
import android.widget.ArrayAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;


public class WALInterface extends Activity {

	EditText username;
	EditText password;
	WALConnection connection;
	
	Thread connectionThread;
	private static final String DEFAULT_USER = "mjv58@cornell.edu";
	private static final String DEFAULT_PASS = "c3q3v1";
	
	public class ServiceMonitor implements ServiceConnection {
		
		public WALConnection connection;
		public ProgressBar bar;
		public Spinner exercisespinner;
		public Spinner lessonspinner;
		ArrayAdapter<CharSequence> lessonadapter;
		ArrayAdapter<CharSequence> exerciseadapter;
		
		public void setLessonspinner(Spinner l)
		{
			lessonspinner = l;
		}
		
		public void setExercisespinner(Spinner e)
		{
			exercisespinner = e;
		}
		
		public void setProgressBar(ProgressBar b)
		{
			bar = b;
		}
		
		public ServiceMonitor(WALConnection c)
		{
			connection = c;
		}
		
		public class ProgressListener implements ProgressUpdateListener {
			
			public void onProgress(int progress)
			{
				bar.setProgress(progress);
				if (progress == 100)
				{
					setContentView(R.layout.wal);
					
				}
			}

			@Override
			public void onExercisesDownloaded(LinkedList<String> exercises) {
				
				
			}

			@Override
			public void onLessonsDownloaded(LinkedList<String> lessons) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTracksDownloaded(LinkedList<Track> tracks) {
				// TODO Auto-generated method stub
				
			}
		}
		
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			connection = ((WALConnection.ConnectionBinder) arg1).getService();
			connection.registerProgressListener(new ProgressListener());
			connection.login();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Context context = getApplicationContext();
			CharSequence text = "Server has disconnected";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			return;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		Button login = (Button) findViewById(R.id.btn_login);
		username = (EditText) findViewById(R.id.et_un);
		password = (EditText) findViewById(R.id.et_pw);
		username.setText(DEFAULT_USER);
		password.setText(DEFAULT_PASS);
		login.setOnClickListener(new OnClickListener() {
			public void onClick(View v)
			{
			    String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
			    CharSequence inputStr = username.getText().toString();
			    Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
			    Matcher matcher = pattern.matcher(inputStr);
				if (!matcher.matches())
				{
					Context context = getApplicationContext();
					CharSequence text = "That was not an email";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
					return;
				}
				else
				{
					if (password.getText().toString().trim() != "")
					{
						setContentView(R.layout.waiting);
						Intent i = new Intent(getApplicationContext(), WALConnection.class);
						i.putExtra("password", password.getText().toString().trim());
						i.putExtra("username", username.getText().toString().trim());
						ServiceMonitor monitor = new ServiceMonitor(connection);
						monitor.setProgressBar((ProgressBar) findViewById(R.id.progressBar));
						bindService(i, monitor, Context.BIND_AUTO_CREATE);
						getApplicationContext().startService(i);
						
					}
					else
					{
						Context context = getApplicationContext();
						CharSequence text = "Please enter your password";
						int duration = Toast.LENGTH_SHORT;

						Toast toast = Toast.makeText(context, text, duration);
						toast.show();
						return;
					}
				}
			}
		});
	}
		

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
