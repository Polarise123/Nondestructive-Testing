package com.kc.myapplication;


//import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
//import android.content.ContentResolver;
//import android.content.ContentUris;
//import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
//import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

//import android.net.Uri;
//import android.os.Build;
import android.os.Bundle;

//import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import android.os.Environment;
//import android.os.FileUtils;
//import android.provider.DocumentsContract;
//import android.provider.MediaStore;
//import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
//import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.kc.myapplication.ml.Hfs;
import com.kc.myapplication.ml.Hys1;
import com.kc.myapplication.ml.Tx2;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.manager.PictureCacheManager;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.tools.StringUtils;
//import com.luck.picture.lib.style.PictureParameterStyle;
//import com.luck.picture.lib.tools.StringUtils;

//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

//import java.io.File;
//import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import io.reactivex.Observer;
//import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
   // private static final String TAG = "OpenCV";
    private final int maxSelectNum = 4;//??????????????????????????????
    private final List<LocalMedia> selectList = new ArrayList<>();
    private static ArrayList<Float> imageArray = new ArrayList<>();
    private GridImageAdapter adapter;
    private RecyclerView mRecyclerView;
    private PopupWindow pop;
   // private PictureParameterStyle mPictureParameterStyle;
 //   private List<LocalMedia> list = new ArrayList<>();
    private String path;

    private EditText editText;
    private int num = 0;
    private int whichModel =3;
    private ByteBuffer inputBuffer;
    //private ArrayList<Float> inputArray;
    private String temperature;
    private static float imagfloat;
    private TextView textView;
    private TextView textView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler);
        Button buttonmodel = findViewById(R.id.ButtonModel);
        Button buttonmodel1 = findViewById(R.id.ButtonModel1);
        textView = findViewById(R.id.Count);
        textView1 = findViewById(R.id.Array);
        RadioGroup buttonGroup = findViewById(R.id.RadioGroup);
//        RadioButton RB1 = findViewById(R.id.rb_hfs);
//        RadioButton RB2 = findViewById(R.id.rb_tx);
//        RadioButton RB3 = findViewById(R.id.rb_hys);
        editText = findViewById(R.id.EditText);
        iniLoadOpenCV();
        initWidget();

        buttonGroup.setOnCheckedChangeListener(ChangeRadioGroup);
        buttonmodel1.setOnClickListener(v -> {
            imageArray = new ArrayList<>();
            Log.i("imagarray3", String.valueOf(imageArray));
            textView.setText("?????????");
            Toast.makeText(getApplicationContext(), "??????????????????",
                    Toast.LENGTH_SHORT).show();
            //textView1.setText(imageArray.size());
        });
        buttonmodel.setOnClickListener(v -> {
            temperature =editText.getText().toString();
            if (!temperature.equals("") &&imageArray.size() == 4) {
                    ArrayList<Float> inputArray = new ArrayList<>();
//                    if(imageArray.size() == 4){
//                    textView1.setText("??????????????????"); }
                    inputArray = imageArray;
                    if(inputArray.size() == 4) {

                        inputArray.add(Float.parseFloat(temperature));
                        Log.i("inputArray", String.valueOf(inputArray));

                        //textView1.setText(inputArray.size());
                        inputBuffer = getBuffer(inputArray);
                        if (whichModel == 1) {
                            runModelTX(inputBuffer);
                        } else if (whichModel == 2) {
                            runModelHYS(inputBuffer);
                        } else if (whichModel == 3) {
                            runModelHFS(inputBuffer);
                        }
                        //imageArray = new ArrayList<>();
                        //Log.i("imagarray1", String.valueOf(imageArray));

                    }else{
                        Toast.makeText(getApplicationContext(), "???????????????",

                                Toast.LENGTH_SHORT).show();

                    }

            } else{
                Toast.makeText(getApplicationContext(), "???????????????????????????",

                        Toast.LENGTH_SHORT).show();
                imageArray.remove(4);
                //ToastUtil.shotMsg(this, "?????????????????????");
                Log.i("temperature", temperature);
                Log.i("imagarray2", String.valueOf(imageArray));
                //Log.i("inputArray", String.valueOf(inputArray));
            }

        });

    }
    private void initWidget() {
        FullyGridLayoutManager manager = new FullyGridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        adapter = new GridImageAdapter(this, onAddPicClickListener);//?????????????????????
        adapter.setList(selectList);//???????????????
        adapter.setSelectMax(maxSelectNum);//?????????8???
        mRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((position, v) -> {
            if (selectList.size() > 0) {
                LocalMedia media = selectList.get(position);
                String mimeType = media.getMimeType();
                int mediaType = PictureMimeType.getMimeType(mimeType);
                switch (mediaType) {
                    case PictureConfig.TYPE_VIDEO:
                        // ????????????
                        PictureSelector.create(MainActivity.this)
                                .themeStyle(R.style.picture_default_style)
                                //.setPictureStyle(mPictureParameterStyle)// ???????????????????????????
                                .externalPictureVideo(TextUtils.isEmpty(media.getAndroidQToPath()) ? media.getPath() : media.getAndroidQToPath());
                        break;
                    //case PictureConfig.TYPE_AUDIO:
                        // ????????????
                        //PictureSelector.create(MainActivity.this)
                                //.externalPictureAudio(PictureMimeType.isContent(media.getPath()) ? media.getAndroidQToPath() : media.getPath());
                        //break;
                    default:
                        // ???????????? ???????????????????????????
//                        PictureWindowAnimationStyle animationStyle = new PictureWindowAnimationStyle();
//                        animationStyle.activityPreviewEnterAnimation = R.anim.picture_anim_up_in;
//                        animationStyle.activityPreviewExitAnimation = R.anim.picture_anim_down_out;
                        PictureSelector.create(MainActivity.this)
                                .themeStyle(R.style.picture_default_style) // xml????????????
                                //.setPictureStyle(mPictureParameterStyle)// ???????????????????????????
                                //.setPictureWindowAnimationStyle(animationStyle)// ???????????????????????????
                                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)// ????????????Activity????????????????????????????????????
                                .isNotPreviewDownload(true)// ????????????????????????????????????
                                //.bindCustomPlayVideoCallback(new MyVideoSelectedPlayCallback(getContext()))// ???????????????????????????????????????????????????????????????????????????
                                .imageEngine(GlideEngine.createGlideEngine())// ??????????????????????????????????????????
                                .openExternalPreview(position, selectList);

                        break;
                }


            }
        });//item??????
    }

    private final GridImageAdapter.onAddPicClickListener onAddPicClickListener = this::showPop;//??????????????????

    private void showPop() {
        View bottomView = View.inflate(MainActivity.this, R.layout.layout_bottom_dialog, null);
        TextView mAlbum = bottomView.findViewById(R.id.tv_album);
        TextView mCamera = bottomView.findViewById(R.id.tv_camera);
        TextView mCancel = bottomView.findViewById(R.id.tv_cancel);

        pop = new PopupWindow(bottomView, -1, -2);
        pop.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pop.setOutsideTouchable(true);
        pop.setFocusable(true);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.5f;
        getWindow().setAttributes(lp);
        pop.setOnDismissListener(() -> {
            WindowManager.LayoutParams lp1 = getWindow().getAttributes();
            lp1.alpha = 1f;
            getWindow().setAttributes(lp1);
        });
        pop.setAnimationStyle(R.style.main_menu_photo_anim);
        pop.showAtLocation(getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);

        @SuppressLint("NonConstantResourceId") View.OnClickListener clickListener = view -> {
            switch (view.getId()) {
                case R.id.tv_album:
                    //??????
                    PictureSelector.create(MainActivity.this)
                            .openGallery(PictureMimeType.ofImage())
                            .imageEngine(GlideEngine.createGlideEngine()) // ?????????Demo GlideEngine.java
                            .maxSelectNum(maxSelectNum)
                            .minSelectNum(1)
                            .imageSpanCount(4)
                            .selectionMode(PictureConfig.MULTIPLE)
                            .forResult(PictureConfig.CHOOSE_REQUEST);
                    //Toast.makeText(getApplicationContext(), "????????????????????????",

                            //Toast.LENGTH_LONG).show();
                    break;
                case R.id.tv_camera:
                    //??????
                    PictureSelector.create(MainActivity.this)
                            .openCamera(PictureMimeType.ofImage())
                            .imageEngine(GlideEngine.createGlideEngine())
                            .forResult(PictureConfig.CHOOSE_REQUEST);
                    break;
                case R.id.tv_cancel:
                    //??????
                    //closePopupWindow();
                    break;
            }
            closePopupWindow();
        };

        mAlbum.setOnClickListener(clickListener);
        mCamera.setOnClickListener(clickListener);
        mCancel.setOnClickListener(clickListener);
    }

    public void closePopupWindow() {
        if (pop != null && pop.isShowing()) {
            pop.dismiss();
            pop = null;
        }
    }
    //
    private final RadioGroup.OnCheckedChangeListener ChangeRadioGroup =new RadioGroup.OnCheckedChangeListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onCheckedChanged (RadioGroup group,int checkId){
            switch (checkId) {
                case R.id.rb_tx:
                    whichModel=1;
                    break;
                case R.id.rb_hys:
                    whichModel=2;
                    break;

                default:
                    whichModel=3;
            }
        }
    };//????????????

    public float getImgArray(String path) {
        //????????????
        // Uri uri=stringtoUri(this,path);
        //Mat imgMat = Imgcodecs.imread(getFileAbsolutePath(this,uri));
        Mat imgMat = Imgcodecs.imread(path);
        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(imgMat, imgMat, 15);
        ArrayList<Float> lineArrayb = new ArrayList<>();
        //ArrayList<Float> imagArray = new ArrayList<Float>();
        ArrayList<Double> lineArraya = new ArrayList<>();
        for (int i = 3948; i < 4048; i=i+10) {
            for (int j = 3245; j < 3355; j++) {
                lineArraya.add(imgMat.get(i, j)[0]);
            }
            //float b =getAvgD(lineArraya);
            lineArrayb.add(getAvgD(lineArraya)); }
        //Log.i("imagfloat1", String.valueOf(lineArrayb));
        imagfloat =getAvgF(lineArrayb);
        Log.i("imagfloat2", String.valueOf(imagfloat));

        return imagfloat;
    }//?????????????????????


    public static ArrayList<Float> conArray(float imagfloat) {

        imageArray.add(imagfloat);
        Log.i("imageArray:", String.valueOf(imageArray));

        return imageArray;
    }

    private static float getAvgF(ArrayList<Float> list) {
        float sum =0;
        for(int i=0;i<list.size();i++) {
            sum +=list.get(i);
        }
        float ArrayAvge =(float) ((int) (sum / list.size()));
        return ArrayAvge;
    }


    private static float getAvgD(ArrayList<Double> list) {
        Double sum= Double.valueOf(0);
        for(int i=0;i<list.size();i++) {
            sum+=list.get(i);
        }
        float ArrayAvge = (float) (sum / list.size());
        return ArrayAvge;
    }


    private static  ByteBuffer getBuffer(ArrayList<Float> inputArray) {
        ByteBuffer inputBuffer;
        inputBuffer = ByteBuffer.allocateDirect(4 * 5);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();
        for (Float element: inputArray) {
            inputBuffer.putFloat(element);
        }
        return inputBuffer;
    }


    @SuppressLint("StringFormatMatches")
    private void runModelHYS(ByteBuffer inputBuffer) {
        try {
            Hys1 model = Hys1.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 5}, DataType.FLOAT32);
            inputFeature0.loadBuffer(inputBuffer);

            // Runs model inference and gets result.
            Hys1.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            //Log.i("hys", Arrays.toString(outputFeature0.getFloatArray()));
            Log.i("hys", String.valueOf(outputFeature0.getFloatArray()[0]));
            //textView.setText( outputFeature0.getFloatArray()[0]);


            //textView.setText(Arrays.toString(outputFeature0.getFloatArray()));
            textView.setText(String.format(getString(R.string.sugar_result), outputFeature0.getFloatArray()[0]));

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @SuppressLint("StringFormatMatches")
    private void runModelTX(ByteBuffer inputBuffer) {
        try {
            Tx2 model = Tx2.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 5}, DataType.FLOAT32);
            inputFeature0.loadBuffer(inputBuffer);

            // Runs model inference and gets result.
            Tx2.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Log.i("tx", String.valueOf(outputFeature0));
            textView.setText(String.format(getString(R.string.sugar_result), outputFeature0.getFloatArray()[0]));
            //textView.setText(Arrays.toString(outputFeature0.getFloatArray()));
            //textView.setText(String.format(getString(R.string.sugar_result), outputFeature0));
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }


    @SuppressLint("StringFormatMatches")
    private void runModelHFS(ByteBuffer inputBuffer) {
        try {
            Hfs model = Hfs.newInstance(this);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 5}, DataType.FLOAT32);
            inputFeature0.loadBuffer(inputBuffer);

            // Runs model inference and gets result.
            Hfs.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            textView.setText(String.format(getString(R.string.sugar_result), outputFeature0.getFloatArray()[0]));
            //Log.i("hfs", String.valueOf(outputFeature0));
            //textView.setText(Arrays.toString(outputFeature0.getFloatArray()));
            //textView.setText(String.format(getString(R.string.sugar_result), outputFeature0.getFloatArray()));
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //LocalMedia media;

        List<LocalMedia> images;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    // ????????????????????????

                    images = PictureSelector.obtainMultipleResult(data);
                    selectList.addAll(images);
                    for(int i=0; i<4;i++){
                           LocalMedia media = images.get(i);
                            //Log.i("media", String.valueOf(media));
                            path = media.getRealPath();
                            //Log.i("IMGpath", path);
                            getImgArray(path);
                            conArray(imagfloat);
                            Log.i("imagfloat1", "??????????????????");
                            }
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            for(int i=0; i<4;i++){
//                            LocalMedia media = images.get(i);
//                            //Log.i("media", String.valueOf(media));
//                            path = media.getRealPath();
//                            //Log.i("IMGpath", path);
//                            getImgArray(path);
//                            conArray(imagfloat);
//                            //Log.i("imagfloat1", "??????????????????");
//                            }
//                        }}).start();
                    //textView1.setText(imageArray.size());



                    Toast.makeText(getApplicationContext(), "????????????????????????",

                            Toast.LENGTH_SHORT).show();


                    //selectList.get;
                    //String path = images.getPath();
                    //Log.i("IMGpath", path);
                    //Log.i("data", String.valueOf(data));
                    //String a =String.valueOf(selectList);
                    //String Str = new String(String.valueOf(selectList));

                    //Log.i("selectList", String.valueOf(selectList));
                    //Log.i("images", String.valueOf(images));
//                    selectList = PictureSelector.obtainMultipleResult(data);
                    // LocalMedia ??????????????????path
                    // 1.media.getPath(); ?????????path
                    // 2.media.getCutPath();????????????path????????????media.isCut();?????????true
                    // 3.media.getCompressPath();????????????path????????????media.isCompressed();?????????true
                    // ????????????????????????????????????????????????????????????????????????????????????
                    adapter.setList(selectList);
                    adapter.notifyDataSetChanged();
                    //String list_str = StringUtils.(list,",")
                    //textView1.setText(imageArray);
                    break;
            }
        }
    }




    /**
     * ???????????????????????????????????????????????????????????????OOM
     * ??????:????????????????????????????????? ?????????????????????
     */
    private void clearCache() {
        // ?????????????????????????????????????????????????????? ??????:????????????????????????????????? ?????????????????????
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //PictureCacheManager.deleteCacheDirFile(this, PictureMimeType.ofImage());
            PictureCacheManager.deleteAllCacheDirRefreshFile(this);
        } else {
            PermissionChecker.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
        }
    }
   // private static void path(int position){
        //LocalMedia media = selectList.get(position);
        //Log.i("????????????::", media.getPath());
    //}
   //OpenCV?????????????????????????????????????????????
   private void iniLoadOpenCV() {
       boolean success = OpenCVLoader.initDebug();
       if(success) {
           Toast.makeText(this. getApplicationContext(), "Loading OpenCV Libraries...", Toast.LENGTH_LONG) . show();
       } else
           Toast.makeText(this. getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG);
   }

}