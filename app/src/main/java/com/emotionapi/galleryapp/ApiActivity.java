package com.emotionapi.galleryapp;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URI;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.CAMERA;

public class ApiActivity extends AppCompatActivity {

    private ImageView imageView; // variable to hold the image view in our activity_main.xml
    private TextView resultText; // variable to hold the text view in our activity_main.xml
    private static final int RESULT_LOAD_IMAGE  = 100;
    private static final int REQUEST_CAMERA_CODE = 300;
    private static final int REQUEST_PERMISSION_CODE = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);

        // initiate our image view and text view
        imageView = (ImageView) findViewById(R.id.imageView);
        resultText = (TextView) findViewById(R.id.resultText);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage(view);
                Toast.makeText(ApiActivity.this,"Saved Successfully!",Toast.LENGTH_LONG).show();
            }
        });
    }

    // when the "GET EMOTION" Button is clicked this function is called
    public void getEmotion(View view) {
        // run the GetEmotionCall class in the background
        GetEmotionCall emotionCall = new GetEmotionCall(imageView);
        emotionCall.execute();
    }

    // when the "GET IMAGE" Button is clicked this function is called
    public void getImage(View view) {
        // check if user has given us permission to access the gallery
        if(checkPermission()) {
            Intent choosePhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(choosePhotoIntent, RESULT_LOAD_IMAGE);
        }
        else {
            requestPermission();
        }
    }

    // This function gets the selected picture from the gallery and shows it on the image view
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // get the photo URI from the gallery, find the file path from URI and send the file path to ConfirmPhoto
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            // a string variable which will store the path to the image in the gallery
            String picturePath= cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            imageView.setImageBitmap(bitmap);
        }

        if (requestCode == REQUEST_CAMERA_CODE && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }

    // convert image to base 64 so that we can send the image to Emotion API
    public byte[] toBase64(ImageView imgPreview) {
        Bitmap bm = ((BitmapDrawable) imgPreview.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        return baos.toByteArray();
    }


    // if permission is not given we get permission
    private void requestPermission() {
        ActivityCompat.requestPermissions(ApiActivity.this,new String[]{READ_EXTERNAL_STORAGE,CAMERA}, REQUEST_PERMISSION_CODE);
    }


    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),READ_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(),CAMERA);
        return result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }

    public void getCameraImage(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_CAMERA_CODE);
        }
    }
    public void saveImage(View view)
    {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        String string=null;
        string=resultText.getText().toString().toLowerCase();
        if (string.contains("neutral"))
        {
            int x=1;
            saveImageFile(bitmap,x);
        }
        else if (string.contains("happiness"))
        {
            int x=2;
            saveImageFile(bitmap,x);
        }
        else if (string.contains("sad"))
        {
            int x=3;
            saveImageFile(bitmap,x);
        }
        else if (string.contains("angry"))
        {
            int x=4;
            saveImageFile(bitmap,x);
        }
        else
        {
            int x=5;
            saveImageFile(bitmap,x);
        }

    }

    public String saveImageFile(Bitmap bitmap,int x) {
        FileOutputStream out = null;
        String filename;
        if (x==1)
        {
            filename = getFilenameneutral();
        }
        else if (x==2)
        {
            filename = getFilenamehappy();
        }
        else if (x==3)
        {
            filename = getFilenamesad();
        }
        else if (x==4)
        {
            filename = getFilenameangry();
        }
        else
        {
            filename = getFilenameother();
        }

        try {
            out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return filename;
    }

    private String getFilenamehappy() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath(), "Emotion_analysis/happy");
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".jpg");
        return uriSting;
    }
    private String getFilenamesad() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath(), "Emotion_analysis/sad");
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".jpg");
        return uriSting;
    }
    private String getFilenameneutral() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath(), "Emotion_analysis/neutral");
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".jpg");
        return uriSting;
    }
    private String getFilenameangry() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath(), "Emotion_analysis/angry");
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".jpg");
        return uriSting;
    }
    private String getFilenameother() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath(), "Emotion_analysis/other");
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/"
                + System.currentTimeMillis() + ".jpg");
        return uriSting;
    }

    // asynchronous class which makes the api call in the background
    private class GetEmotionCall extends AsyncTask<Void, Void, String> {

        private final ImageView img;

        GetEmotionCall(ImageView img) {
            this.img = img;
        }

        // this function is called before the api call is made
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultText.setText("Getting results...");
        }

        // this function is called when the api call is made
        @Override
        protected String doInBackground(Void... params) {
            HttpClient httpclient = HttpClients.createDefault();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            try {
                URIBuilder builder = new URIBuilder("https://westus.api.cognitive.microsoft.com/emotion/v1.0/recognize");

                URI uri = builder.build();
                HttpPost request = new HttpPost(uri);
                request.setHeader("Content-Type", "application/octet-stream");
                // enter you subscription key here
                request.setHeader("Ocp-Apim-Subscription-Key", "3e3424dc18d5420fb932f1271d7cc501");

                // Request body.The parameter of setEntity converts the image to base64
                request.setEntity(new ByteArrayEntity(toBase64(img)));

                // getting a response and assigning it to the string res
                HttpResponse response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();
                String res = EntityUtils.toString(entity);

                return res;

            }
            catch (Exception e){
                return "null";
            }

        }

        // this function is called when we get a result from the API call
        @Override
        protected void onPostExecute(String result) {
            JSONArray jsonArray = null;
            try {
                // convert the string to JSONArray
                jsonArray = new JSONArray(result);
                String emotions = "";
                // get the scores object from the results
                for(int i = 0;i<jsonArray.length();i++) {
                    JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                    JSONObject scores = jsonObject.getJSONObject("scores");
                    double max = 0;
                    String emotion = "";
                    for (int j = 0; j < scores.names().length(); j++) {
                        if (scores.getDouble(scores.names().getString(j)) > max) {
                            max = scores.getDouble(scores.names().getString(j));
                            emotion = scores.names().getString(j);
                        }
                    }
                    emotions += emotion + "\n";
                }
                resultText.setText(emotions);

            } catch (JSONException e) {
                resultText.setText("No emotion detected. Try again later");
            }
        }
    }
}

