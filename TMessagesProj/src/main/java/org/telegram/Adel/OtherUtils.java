package org.telegram.Adel;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.Charset;

public class OtherUtils
{
	public static boolean isActivityRunning()
	{
		boolean result = false;
		for (RunningTaskInfo runningTaskInfo : ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(Integer.MAX_VALUE))
		{
			if (ApplicationLoader.applicationContext.getPackageName().equalsIgnoreCase(runningTaskInfo.baseActivity.getPackageName()))
			{
				result = true;
			}
		}
		return result;
	}

	public static boolean isMyServiceRunning(Class<?> serviceClass)
	{
		for (RunningServiceInfo service : ((ActivityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE))
		{
			if (serviceClass.getName().equals(service.service.getClassName()))
			{
				return true;
			}
		}
		return false;
	}

	public static void writeStringToFile(File file, String data, Charset charset, boolean append) throws Exception
	{
		String dataToWrite;

		if (append)
		{
			dataToWrite = readFileToString(file, charset) + data;
		}
		else
		{
			dataToWrite = data;
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(dataToWrite);
		writer.close();
	}

	public static String readFileToString(File file, Charset charset) throws Exception
	{
		if (!file.exists())
		{
			return "";
		}
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		try
		{
			StringBuilder stringBuilder = new StringBuilder();
			while (true)
			{
				String line = bufferedReader.readLine();
				if (line == null)
				{
					break;
				}
				stringBuilder.append(line);
			}
			String stringBuilder2 = stringBuilder.toString();
			return stringBuilder2;
		} finally
		{
			bufferedReader.close();
		}
	}

	public static byte[] toByteArray(InputStream inputStream) throws Exception
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[]                data   = new byte[8192];
		while (true)
		{
			int nRead = inputStream.read(data);
			if (nRead != -1)
			{
				buffer.write(data, 0, nRead);
			}
			else
			{
				buffer.flush();
				return buffer.toByteArray();
			}
		}
	}

	public static void copyInputStreamToFile(InputStream inputStream, File file) throws Exception
	{
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
		BufferedInputStream  bufferedInputStream  = new BufferedInputStream(inputStream);
		byte[]               bytes                = new byte[8192];
		while (true)
		{
			int c = bufferedInputStream.read(bytes);
			if (c != -1)
			{
				bufferedOutputStream.write(bytes, 0, c);
			}
			else
			{
				bufferedInputStream.close();
				bufferedOutputStream.flush();
				return;
			}
		}
	}

	public static int getMaxViewCount(String viewString)
	{
		int    toZarb;
		String type = viewString.substring(viewString.length() - 1);
		if (type.equals("K"))
		{
			toZarb = 1000;
		}
		else if (type.equals("M"))
		{
			toZarb = 1000000;
		}
		else
		{
			toZarb = 1;
		}
		return Integer.parseInt(viewString.substring(0, viewString.length() - 1).replace(" ", "")) * toZarb;
	}

	public static void showDialog(Context context, String title, String message)
	{
		AlertDialog dialog = new AlertDialog.Builder(context)
				.setTitle(title)
				.setMessage(message)
				.show();
		android.widget.TextView textView = (android.widget.TextView) dialog.getWindow().findViewById(android.R.id.message);
		textView.setTypeface(AndroidUtilities.getTypeface(null));
		textView = (android.widget.TextView) dialog.getWindow().findViewById(R.id.alertTitle);
		textView.setTypeface(AndroidUtilities.getTypeface(null));

		// Getting the view elements
		//		TextView textView = (TextView) alertDialog.getWindow().findViewById(android.R.id.message);
		//		TextView alertTitle = (TextView) alertDialog.getWindow().findViewById(R.id.alertTitle);
		//		Button button1 = (Button) alertDialog.getWindow().findViewById(android.R.id.button1);
		//		Button button2 = (Button) alertDialog.getWindow().findViewById(android.R.id.button2);
	}

	public static void showSnack(View view, String message, int length, int textColor)
	{
		Snackbar snack = Snackbar.make(view, message, length);

		android.widget.TextView tv = (android.widget.TextView) snack.getView().findViewById(android.support.design.R.id.snackbar_text);
		tv.setTextColor(textColor);
		tv.setTypeface(AndroidUtilities.getTypeface(null));

		snack.show();
	}
}
