package com.example.blogapp.Activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.blogapp.Activities.ui.home.HomeFragment;
import com.example.blogapp.Fragments.ProfileFragment;
import com.example.blogapp.Fragments.SettingsFragment;
import com.example.blogapp.Models.Post;
import com.example.blogapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;

    private  static  final int PReqCode = 2;
    private  static  final int REQUESCODE = 2;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FragmentManager fragmentManager;
    FragmentTransaction fragmentTransaction;
    Dialog popAddPost;
    ImageView popupUserImage,popupPostImage,popupAddBtn;
    TextView popupTitle,popupDescription;
    ProgressBar popupClickProgress;
    private Uri pickedImgUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.container,new HomeFragment());
        fragmentTransaction.commit();

        //This
            mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();

        // ini Popup
        iniPopup();
        setupPopupImageClick();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popAddPost.show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        updateNavHeader();

        // set the home fragment as the default
        getSupportFragmentManager().beginTransaction().replace(R.id.container,new HomeFragment()).commit();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_profile, R.id.nav_settings, R.id.nav_signout)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    private void setupPopupImageClick() {

        popupPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkAndRequestForPermission();

            }
        });
    }

    private void checkAndRequestForPermission() {
        if(ContextCompat.checkSelfPermission(Home.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(Home.this,Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                Toast.makeText(Home.this,"Please accept for required permission",Toast.LENGTH_SHORT).show();
            }
            else
            {
                ActivityCompat.requestPermissions(Home.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PReqCode);
            }
        }
        else
            openGallery();


    }

    private void openGallery() {
        //TODO : membuka galeri dan menuggu user memilih photo

        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,REQUESCODE);
    }

    // dimana user memilih gambar
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUESCODE && data != null);
        {
            //user berhasil memilih photo
            //akan menyimpannya di Variabel Uri
            pickedImgUri = data.getData();
            popupPostImage.setImageURI(pickedImgUri);

        }
    }

    private void iniPopup() {

        popAddPost = new Dialog(this);
        popAddPost.setContentView(R.layout.popup_add_post);
        popAddPost.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popAddPost.getWindow().setLayout(Toolbar.LayoutParams.MATCH_PARENT,Toolbar.LayoutParams.WRAP_CONTENT);
        popAddPost.getWindow().getAttributes().gravity = Gravity.TOP;

        // pop up widggets
        popupUserImage = popAddPost.findViewById(R.id.popup_user_image);
        popupPostImage = popAddPost.findViewById(R.id.popup_img);
        popupTitle = popAddPost.findViewById(R.id.popup_title);
        popupDescription = popAddPost.findViewById(R.id.popup_description);
        popupAddBtn = popAddPost.findViewById(R.id.popup_add);
        popupClickProgress = popAddPost.findViewById(R.id.popup_progressBar);

        // load user photo profile

        Glide.with(Home.this).load(currentUser.getPhotoUrl()).into(popupUserImage);

        //add post
        popupAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupAddBtn.setVisibility(View.INVISIBLE);
                popupClickProgress.setVisibility(View.VISIBLE);

                // test semua inputan

                if (!popupTitle.getText().toString().isEmpty()
                    && !popupDescription.getText().toString().isEmpty()
                    && pickedImgUri != null ){

                    // jika semua di isi dan tidak ada yang kosong
                    // TODO membuat post lalu di taro di firebase
                    // pertama, upload photo
                    // akses ke penyimpanan firebase

                    StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("blog_images");
                    final StorageReference imageFilePath = storageReference.child(pickedImgUri.getLastPathSegment());
                    imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageDownloadLink = uri.toString();
                                    //membbuat post di object
                                    Post post = new Post(popupTitle.getText().toString(),
                                            popupDescription.getText().toString(),
                                            imageDownloadLink,
                                            currentUser.getUid(),
                                            currentUser.getPhotoUrl().toString());
                                    // menambah post di database firebase
                                    addPost(post);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Jika ada yang salah dalam upload gambar
                                    showMessage(e.getMessage());
                                    popupClickProgress.setVisibility(View.INVISIBLE);
                                    popupAddBtn.setVisibility(View.VISIBLE);

                                }
                            });
                        }
                    });

                }
                else{
                    showMessage("Harap di isi semua, jangan ada yang kosong");
                    popupAddBtn.setVisibility(View.VISIBLE);
                    popupClickProgress.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void addPost(Post post) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Posts").push();

        //mengambil id yg unik dan post key
        String key  = myRef.getKey();
        post.setPostKey(key);

        // menyimpan post di firebase database
        myRef.setValue(post).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                showMessage("Post berhasil di tambahkan");
                popupClickProgress.setVisibility(View.INVISIBLE);
                popupAddBtn.setVisibility(View.VISIBLE);
                popAddPost.dismiss();
            }
        });


    }

    private void showMessage(String message) {
        Toast.makeText(Home.this,message,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void updateNavHeader() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.nav_username);
        TextView navUserMail = headerView.findViewById(R.id.nav_user_mail);
        ImageView navUserPhoto = headerView.findViewById(R.id.nav_user_photo);

        navUserMail.setText(currentUser.getEmail());
        navUsername.setText(currentUser.getDisplayName());

        //menggunakan Glide untuk load photo user
        //pertama, import library terlebih dahulu

        Glide.with(this).load(currentUser.getPhotoUrl()).into(navUserPhoto);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem Item) {
        int id  = Item.getItemId();

        if (id == R.id.nav_home){
            getSupportFragmentManager().beginTransaction().replace(R.id.container,new HomeFragment()).commit();
        } else if (id == R.id.nav_profile){
            getSupportFragmentManager().beginTransaction().replace(R.id.container,new ProfileFragment()).commit();
        } else if (id == R.id.nav_settings){
            getSupportFragmentManager().beginTransaction().replace(R.id.container,new SettingsFragment()).commit();
        }
        else if (id == R.id.nav_signout){
            FirebaseAuth.getInstance().signOut();
            Intent loginActivity = new Intent(getApplicationContext(),LoginAct.class);
            startActivity(loginActivity);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
