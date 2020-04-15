
package com.example.blogapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.blogapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class RegisterAct extends AppCompatActivity {

    ImageView imgUserPhoto;
    static  int PReqCode = 1;
    static  int REQUESCODE = 1;
    Uri pickedImgUri;

    private EditText userEmail,userPassword,userPassword2,userName;
    private ProgressBar loadingProgress;
    private Button regBtn;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        imgUserPhoto = findViewById(R.id.regUserPhoto);
        userEmail = findViewById(R.id.regMail);
        userPassword = findViewById(R.id.regPassword);
        userPassword2 = findViewById(R.id.regPassword2);
        userName = findViewById(R.id.regName);
        loadingProgress = findViewById(R.id.progressBar);
        regBtn = findViewById(R.id.regBtn);

        mAuth = FirebaseAuth.getInstance();

        loadingProgress.setVisibility(View.INVISIBLE);
        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                regBtn.setVisibility(View.INVISIBLE);
                loadingProgress.setVisibility(View.VISIBLE);
                final String email = userEmail.getText().toString();
                final String password = userPassword.getText().toString();
                final String password2 = userPassword2.getText().toString();
                final String name = userName.getText().toString();

                if (email.isEmpty() || name.isEmpty() || password.isEmpty() ||
                    !password.equals(password2)) {
                    // kalau ada yang salah : semua harus di isi
                    // akan menampilkan pesan
                    showMessage("Please verify all fields");
                    regBtn.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.INVISIBLE);
                }
                else
                {
                    // jika semua benar, dan sudah terisi, maka akun dapat di buat
                    // method CreateUserAccount digunakan jika email yang digunakan itu valid
                    CreateUserAccount(email,name,password);

                }
            }
        });


        imgUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= 22) {

                    checkAndRequestForPermission();
                }
                else
                {
                    openGallery();
                }
            }
        });
    }

    private void CreateUserAccount(String email, final String name, String password) {

        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            //akun berhasil di buat
                            showMessage("Account created");
                            // setelah akun berhasil di buat maka foto profil dan nama akan berubah
                            updateUserInfo(name, pickedImgUri, mAuth.getCurrentUser());
                        }
                        else
                        {
                            // akun gagal di buat
                            showMessage("Account creation failed" + task.getException().getMessage());
                            regBtn.setVisibility(View.VISIBLE);
                            loadingProgress.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    //update user photo and name
    private void updateUserInfo(final String name, Uri pickedImgUri, final FirebaseUser currentUser)
    {
        // pertama kudu upload user photo ke firebase storage dan mendapatkan Uri
        StorageReference mStorage = FirebaseStorage.getInstance().getReference().child("users_photo");
        final StorageReference imageFilePath = mStorage.child(pickedImgUri.getLastPathSegment());
        imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
            // Jika upload gambar berhasil
            imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .setPhotoUri(uri)
                            .build();

                    currentUser.updateProfile(profileUpdate)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful())
                                    {
                                        // update data user berhasil
                                        showMessage("Register Complete");
                                        updateUI();
                                    }
                                }
                            });
                }
            });
            }
        });
    }

    private void updateUI() {
        Intent homeActivity = new Intent(getApplicationContext(),Home.class);
        startActivity(homeActivity);
        finish();
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message,Toast.LENGTH_LONG).show();
    }

    private void openGallery() {
        //TODO : membuka galeri dan menuggu user memilih photo

        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,REQUESCODE);
    }

    private void checkAndRequestForPermission() {
        if(ContextCompat.checkSelfPermission(RegisterAct.this, Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(RegisterAct.this,Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                Toast.makeText(RegisterAct.this,"Please accept for required permission",Toast.LENGTH_SHORT).show();
            }
            else
            {
                ActivityCompat.requestPermissions(RegisterAct.this,
                                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                                    PReqCode);
            }
        }
        else
            openGallery();

        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUESCODE && data != null);
        {
            //user berhasil memilih photo
            //akan menyimpannya di Variabel Uri
            pickedImgUri = data.getData();
            imgUserPhoto.setImageURI(pickedImgUri);

        }
    }
}
