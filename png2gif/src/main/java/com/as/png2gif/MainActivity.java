package com.as.png2gif;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.as.png2gif.adapter.ImageAdapter;
import com.as.png2gif.gifutil.AnimatedGifEncoder;
import com.bumptech.glide.Glide;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;
import com.nanchen.compresshelper.CompressHelper;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;
import com.zqf.base.util.utils_blankj.ImageUtils;
import com.zqf.base.util.utils_blankj.ToastUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CHOOSE = 23;
    private int changeTime = 500;

    private ImageView imagegif;
    private RecyclerView recyclerview_image;
    private RadioGroup rg;
    private LinearLayoutManager linearLayoutManager;
    private List<String> listPath;
    private File gifFile;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AndPermission.with(this)
                .permission(Permission.Group.STORAGE)
                .onGranted(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {

                    }
                })
                .onDenied(new Action() {
                    @Override
                    public void onAction(List<String> permissions) {
                        ToastUtils.showShort("需要开启存储权限");
                        recreate();
                    }
                })
                .start();


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("转啊转啊我的骄傲放纵");

        recyclerview_image = findViewById(R.id.recyclerview_image);
        rg = findViewById(R.id.rg);
        imagegif = findViewById(R.id.imagegif);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb0:
                        changeTime = 5000;
                        break;
                    case R.id.rb1:
                        changeTime = 8000;
                        break;
                    case R.id.rb2:
                        changeTime = 11000;
                        break;
                    case R.id.rb3:
                        changeTime = 14000;
                        break;
                    case R.id.rb4:
                        changeTime = 17000;
                        break;
                    default:
                        break;
                }
            }
        });

    }


    public void butselect(View view) {
        Matisse.from(MainActivity.this)
                .choose(MimeType.ofImage())
                .countable(true)
                .maxSelectable(50)
                .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .gridExpectedSize(300)//每个图片的宽度
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .forResult(REQUEST_CODE_CHOOSE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            listPath = Matisse.obtainPathResult(data);
            ImageAdapter imageAdapter = new ImageAdapter(R.layout.item_image, listPath);
            recyclerview_image.setLayoutManager(linearLayoutManager);
            recyclerview_image.setAdapter(imageAdapter);
        }
    }

    public void but2gif(View view) {
        progressDialog.show();
        makeGif();
    }

    /**
     * 生成gif图片
     */
    public void makeGif() {
        if (listPath == null) {
            progressDialog.dismiss();
            return;
        }
        if (listPath.size() <= 0) {
            progressDialog.dismiss();
            return;
        }

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/data/data/" + this.getPackageName() + "/cache";
        File pathFile = new File(path);
        String gifname = System.currentTimeMillis() + ".gif";
        gifFile = new File(pathFile, gifname);

        if (!pathFile.exists())
            pathFile.mkdirs();
        if (!gifFile.exists())
            try {
                gifFile.createNewFile();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                OutputStream os;
                try {
                    os = new FileOutputStream(gifFile);
                    AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
                    gifEncoder.start(os);  //注意顺序

                    for (int i = 0; i < listPath.size(); i++) {
                        Bitmap bitmap = CompressHelper.getDefault(MainActivity.this).compressToBitmap(new File(listPath.get(i)));

                        gifEncoder.addFrame(bitmap);
                    }

                    gifEncoder.setDelay(changeTime);
                    gifEncoder.setRepeat(0);
                    gifEncoder.finish();


                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                progressDialog.cancel();

                Glide.with(MainActivity.this)
                        .load(gifFile)
                        .asGif()
                        .into(imagegif);
            }
        }.execute();

    }

    public void butsave(View view) {
        insertImageToSystem(gifFile);
    }

    public void insertImageToSystem(File file) {
//         其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(),
                    file.getAbsolutePath(), file.getName(), null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Intent intentBroadcast = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intentBroadcast.setData(Uri.fromFile(file));
        sendBroadcast(intentBroadcast);
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
    }

    public void copyFile(String oldPath, final String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}