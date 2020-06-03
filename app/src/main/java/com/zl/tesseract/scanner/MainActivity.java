package com.zl.tesseract.scanner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zl.tesseract.R;
import com.zl.tesseract.scanner.tess.TesseractCallback;
import com.zl.tesseract.scanner.tess.TesseractThread;
import com.zl.tesseract.scanner.utils.BmpUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "DBG_" + MainActivity.class.getName();
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 7;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATUS = 8;
    private static final int MY_START_FILE_SELECT = 122;
    TextView txView;
    Button btnSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txView = findViewById(R.id.ocr_result);
        btnSelect = findViewById(R.id.selectfile);

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sImei = "";
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    Activity#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for Activity#requestPermissions for more details.
                            return;
                        }
                    }
                    sImei = telephonyManager.getDeviceId();
                }
                if (!ValidateIMEI(sImei)){
                    txView.setText("");
                    Intent intent = new Intent()
                            .setType("image/*")
                            .setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent, "Select Screenshot file"), MY_START_FILE_SELECT);
                }else{
                    Toast.makeText(MainActivity.this, "IMEI="+sImei, Toast.LENGTH_LONG).show();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Explain to the user why we need to read the contacts
                }

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }

            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                    // Explain to the user why we need to read the contacts
                }

                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATUS);
            }
        }


        /*
// Find the last picture
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                //MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = MainActivity.this.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

// Put it in the image view
        if (cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            File imageFile = new File(imageLocation);
            if (imageFile.exists()) {   // TODO: is there a better way to do this?
                Bitmap bmp = BitmapFactory.decodeFile(imageLocation);
                //BmpUtils.parseMultiCode(bmp);
                //Log.d(TAG, decode(imageLocation));
                TesseractThread mTesseractThread = new TesseractThread(bmp, new TesseractCallback() {

                    @Override
                    public void succeed(String result) {
                        Message message = Message.obtain();
                        message.what = 0;
                        message.obj = result;
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void fail() {
                        Message message = Message.obtain();
                        message.what = 1;
                        mHandler.sendMessage(message);
                    }
                });

                Thread thread = new Thread(mTesseractThread);
                thread.start();
            }
        }

         */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == MY_START_FILE_SELECT && resultCode==RESULT_OK)
        {
            Uri selectedimg = data.getData();
            //imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedimg));
            Bitmap bmp = null;
            try {
                bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedimg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(bmp==null) return;
            TesseractThread mTesseractThread = new TesseractThread(bmp, new TesseractCallback() {

                @Override
                public void succeed(String result) {
                    Message message = Message.obtain();
                    message.what = 0;
                    message.obj = result;
                    mHandler.sendMessage(message);
                }

                @Override
                public void fail() {
                    Message message = Message.obtain();
                    message.what = 1;
                    mHandler.sendMessage(message);
                }
            });

            Thread thread = new Thread(mTesseractThread);
            thread.start();

        }
    }

    public  List<String> parseDomXml(String sxml){
        List<String> items = new ArrayList<>();
        DocumentBuilderFactory documentBuilderFactory=  DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder= null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        InputStream stream = new ByteArrayInputStream(sxml.getBytes(StandardCharsets.UTF_8));
        try {
            Document document= documentBuilder.parse(stream);
            Element element=document.getDocumentElement();
            NodeList nodeList=element.getElementsByTagName("div");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element pElement= (Element) nodeList.item(i);
                NodeList nodeListp=pElement.getElementsByTagName("p");

                for (int ii = 0; ii < nodeListp.getLength(); i++) {
                    Element spanElement= (Element) nodeListp.item(i);
                    NodeList nodeListline=spanElement.getElementsByTagName("span");


                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return items;

    }

    public List<String> parseXml(String sxml){
        List<String> items = new ArrayList<>();
        try {

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput( new StringReader( sxml ) ); // pass input whatever xml you have
            int eventType = xpp.getEventType();
            Boolean bFind = false;
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if(eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(TAG,"Start document");
                } else if(eventType == XmlPullParser.START_TAG) {
                    Log.d(TAG,"Start tag "+xpp.getName());
                    if(xpp.getName().equals("em")){
                        bFind = true;
                    }else{
                        bFind = false;
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    Log.d(TAG,"End tag "+xpp.getName());
                } else if(eventType == XmlPullParser.TEXT) {
                    Log.d(TAG,"Text "+xpp.getText()); // here you get the text from xml
                    if(bFind){
                        if(!TextUtils.isEmpty(xpp.getText().trim())) {
                            items.add(xpp.getText());
                        }
                    }
                }

                eventType = xpp.next();
            }
            Log.d(TAG,"End document");

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }

    private Boolean ValidateIMEI(String sImei){
        Boolean bFind = false;
        if (TextUtils.isEmpty(sImei)) return bFind;
        if (sImei.length() == 15){
            int sum = 0;
            boolean errorflag = false;
            for (int i = 0; i <= 14; i++) {
                //getting ascii value for each character
                char c = sImei.charAt(i);
                int number = c;
                //Assigning number values to corrsponding Ascii value
                if (number < 48 || number > 57) {
                    errorflag = true;
                    break;
                } else
                {
                    switch (number) {
                        case 48:
                            number = 0;
                            break;
                        case 49:
                            number = 1;
                            break;
                        case 50:
                            number = 2;
                            break;
                        case 51:
                            number = 3;
                            break;
                        case 52:
                            number = 4;
                            break;
                        case 53:
                            number = 5;
                            break;
                        case 54:
                            number = 6;
                            break;
                        case 55:
                            number = 7;
                            break;
                        case 56:
                            number = 8;
                            break;
                        case 57:
                            number = 9;
                            break;
                    }
                    //Double the even number and divide it by 10. add quotient and remainder
                    if ((i + 1) % 2 == 0) {
                        number = number * 2;
                        number = number / 10 + number % 10;
                    }
                    sum = sum + number;
                }
            }
            // Check the error flag to avoid overWriting of Warning Lable
            if (!errorflag) {
                if (sum % 10 == 0) {
                    bFind = true;
                }
            }
        }
        return bFind;
    }

    private String FindImei(List<String> items){
        String sImei="";
        Pattern p = Pattern.compile("\\d{1,15}");
        Pattern pimei = Pattern.compile("IIVIEI|IMEI");
        Boolean bFind = false;
        Boolean bImei = false;
        for (String item: items) {
            Matcher mimei = pimei.matcher(item);
            if(mimei.matches()){
                bImei = true;
            }
            String ss = item;
            if(bImei) {
                ss = ss.replace('i', '1');
                ss = ss.replace('o', '0');
                ss = ss.replace('O', '0');
            }
            Matcher m = p.matcher(ss);
            if(m.matches()){
                bFind = ValidateIMEI(ss);
                if(bFind){
                   sImei = ss;
                }else {
                    sImei += ss;
                }
            }else{
                sImei="";
            }

            bFind = ValidateIMEI(sImei);

            if(bFind){
                items.add(sImei);
                Toast.makeText(this, "IMEI="+sImei, Toast.LENGTH_LONG).show();
                break;
            }
        }
        return sImei;
    }

    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case 0:
                    //txView.setText((String) msg.obj);
                    List<String> items = parseXml((String)msg.obj);
                    String sImei = FindImei(items);
                    Log.d(TAG, "Find IMEI:"+sImei);
                    for (String item: items) {
                        Log.d(TAG, item);

                        txView.append(item);
                        txView.append(System.getProperty("line.separator"));
                    }
                    break;
                case 1:
                    Toast.makeText(MainActivity.this, "无法识别", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };


}
