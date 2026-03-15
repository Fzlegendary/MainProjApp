package com.example.mainproj.messaging;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mainproj.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class MessagingActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatModel> chatList;

    private EditText searchChats;
    private FloatingActionButton addChatButton;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_messaging);

        // initialize views
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        searchChats = findViewById(R.id.searchChats);
        addChatButton = findViewById(R.id.addChatButton);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationDrawer);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // chat list
        chatList = new ArrayList<>();

        chatList.add(new ChatModel("John","Hey there!","12:30"));
        chatList.add(new ChatModel("Alex","See you later","11:10"));

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        chatAdapter = new ChatAdapter(chatList,this);

        chatRecyclerView.setAdapter(chatAdapter);

        addChatButton.setOnClickListener(v -> {

            Toast.makeText(this,"Open Add User Screen",Toast.LENGTH_SHORT).show();

        });

        // drawer menu clicks
        navigationView.setNavigationItemSelectedListener(item -> {

            if(item.getItemId()==R.id.menu_logout){

                Toast.makeText(this,"Logging out",Toast.LENGTH_SHORT).show();

                finish();

            }

            return true;
        });
    }

    public void showChatOptions(ChatModel chat){

        String[] options={"Open Profile","Close DM"};

        AlertDialog.Builder builder=new AlertDialog.Builder(this);

        builder.setTitle(chat.getUsername());

        builder.setItems(options,(dialog,which)->{

            if(which==0){

                Toast.makeText(this,"Open profile",Toast.LENGTH_SHORT).show();

            }else if(which==1){

                Toast.makeText(this,"Close DM",Toast.LENGTH_SHORT).show();

            }

        });

        builder.show();
    }
}