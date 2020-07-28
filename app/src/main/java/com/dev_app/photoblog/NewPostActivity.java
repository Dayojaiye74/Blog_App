package com.dev_app.photoblog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPostActivity extends AppCompatActivity {

    private UploadTask uploadTask;
    private Toolbar newPostToolbar;
    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;
    private Uri postImageUri = null;
    private ProgressBar newPostProgess;
    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String current_user_id;
    private Bitmap compressedImageFile;
     private UploadTask.TaskSnapshot taskSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);


        newPostImage = findViewById(R.id.new_post_image);
        newPostDesc = findViewById(R.id.desc_Text);
        newPostBtn = findViewById(R.id.post_btn);
        newPostProgess = findViewById(R.id.newPost_progress);
        newPostToolbar = findViewById(R.id.new_Post_toolbar);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();


        //To avoid crashing
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firebaseFirestore.setFirestoreSettings(settings);



        current_user_id = firebaseAuth.getCurrentUser().getUid();

        //Toolbar...
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Add image post....
        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512, 512) //Allow quality image
                        .setAspectRatio(1, 2)
                        .start(NewPostActivity.this);
            }
        });


        //The onclick to upload
       newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String desc = newPostDesc.getText().toString();

                if (!TextUtils.isEmpty(desc) && postImageUri != null) {

                    newPostProgess.setVisibility(View.VISIBLE);
                    final String randomName = UUID.randomUUID().toString();

                    // PHOTO UPLOAD
                    File newImageFile = new File(postImageUri.getPath());
                    try {

                        compressedImageFile = new Compressor(NewPostActivity.this)
                                .setMaxHeight(200)
                                .setMaxWidth(200)
                                .setQuality(50)
                                .compressToBitmap(newImageFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imageData = baos.toByteArray();

                    // PHOTO UPLOAD

                    UploadTask filePath = storageReference.child("post_images").child(randomName + ".jpg").putBytes(imageData);

                    filePath.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {

                            final String downloadUri = task.getResult().getStorage().getDownloadUrl().toString();

                            if(task.isSuccessful()){

                                File newThumbFile = new File(postImageUri.getPath());
                                try {

                                    compressedImageFile = new Compressor(NewPostActivity.this)
                                            .setMaxHeight(100)
                                            .setMaxWidth(100)
                                            .setQuality(1)
                                            .compressToBitmap(newThumbFile);

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] thumbData = baos.toByteArray();

                                UploadTask uploadTask = storageReference.child("post_images/thumbs")
                                        .child(randomName + ".jpg").putBytes(thumbData);

                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                        String downloadthumbUri = taskSnapshot.getStorage().getDownloadUrl().toString();

                                        Map<String, Object> postMap = new HashMap<>();
                                        postMap.put("image_url", downloadUri);
                                        postMap.put("image_thumb", downloadthumbUri);
                                        postMap.put("desc", desc);
                                        postMap.put("user_id", current_user_id);
                                        postMap.put("timestamp", FieldValue.serverTimestamp());

                                        firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {

                                                if(task.isSuccessful()){

                                                    Toast.makeText(NewPostActivity.this, "Post was added", Toast.LENGTH_LONG).show();
                                                    Intent mainIntent = new Intent(NewPostActivity.this, MainActivity.class);
                                                    startActivity(mainIntent);
                                                    finish();

                                                } else {


                                                }

                                                newPostProgess.setVisibility(View.INVISIBLE);

                                            }
                                        });

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        //Error handling

                                    }
                                });


                            } else {

                                newPostProgess.setVisibility(View.INVISIBLE);

                            }

                        }
                    });


                }

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                postImageUri = result.getUri();
                newPostImage.setImageURI(postImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();

            }
        }
    }
}











//
//
//
//                                                  // PHOTO UPLOAD
//
//
//                                                  //End onclick to Upload
//                                              }













  /*newPostBtn.setOnClickListener(new View.OnClickListener() {

          String desc = newPostDesc.getText().toString();
          String randomName = FieldValue.serverTimestamp().toString();

@Override
public void onClick(View view) {

        newPostProgess.setVisibility(View.VISIBLE);
final StorageReference ref = storageReference.child("post_images").child(randomName + "jpg");
        uploadTask = ref.putFile(postImageUri);

final Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
@Override
public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

        if (!task.isSuccessful()) {
        newPostProgess.setVisibility(View.VISIBLE);
        throw task.getException();
        } else {
        newPostProgess.setVisibility(View.INVISIBLE);
        String error = task.getException().getMessage();
        Toast.makeText(NewPostActivity.this, "Image Error: " + error, Toast.LENGTH_SHORT).show();
        }

        // Continue with the task to get the download URL
        return ref.getDownloadUrl();
        }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {


@Override
public void onComplete(@NonNull Task<Uri> task) {

        //String desc = newPostDesc.getText().toString();


        if (task.isSuccessful()) {
        Uri downloadUri = task.getResult();

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("image_uri", downloadUri);
        postMap.put("desc", desc);
        postMap.put("user_id", current_user_id);
        postMap.put("timeStamp", FieldValue.serverTimestamp());

        //FirebaseFIreStore......Collection......
        firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
@Override
public void onComplete(@NonNull Task<DocumentReference> task) {

        if (task.isSuccessful()) {
        Toast.makeText(NewPostActivity.this, "Post was added", Toast.LENGTH_SHORT).show();
        Intent mainIntent = new Intent(NewPostActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
        } else {

        String error = task.getException().getMessage();
        Toast.makeText(NewPostActivity.this, "Image Error: " + error, Toast.LENGTH_SHORT).show();
        }

        newPostProgess.setVisibility(View.INVISIBLE);
        }
        });


        } else {
        // Handle failures
        String error = task.getException().getMessage();
        Toast.makeText(NewPostActivity.this, "Image Error: " + error, Toast.LENGTH_SHORT).show();
        }
        }
        });


        }
        })*/