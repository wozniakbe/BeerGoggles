package edu.msoe.wozniakbe.beergoggles.activities;

import android.app.ListActivity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import edu.msoe.wozniakbe.beergoggles.R;
import edu.msoe.wozniakbe.beergoggles.src.Beer;

public class MainActivity extends ListActivity {

    private DatabaseReference databaseReference;
    private final String BEERS_PATH = "beers";
    private ArrayList<Beer> beers;
    private ArrayList<String> beerNames;

    private EditText searchText;
    private Button searchButton;
    private ArrayAdapter<String> adapter; // TODO: Change all this to beer object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeGui();

        beers = new ArrayList<>();
        beerNames = new ArrayList<>();

        adapter=new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                beerNames);

        setListAdapter(adapter);
        databaseReference = FirebaseDatabase.getInstance().getReference(BEERS_PATH);
    }

    private void initializeGui(){
        this.searchText = findViewById(R.id.searchText);
        this.searchButton = findViewById(R.id.searchButton);
        this.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchClicked();
            }
        });
    }

    private void onSearchClicked(){
        ValueEventListener beerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                beers.clear();

                for (DataSnapshot child: dataSnapshot.getChildren()) {
                    beers.add(child.getValue(Beer.class));
                    beerNames.add(child.getValue(Beer.class).getName());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                //Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        databaseReference.orderByChild("name").startAt(searchText.getText().toString()).endAt(searchText.getText().toString()).addListenerForSingleValueEvent(beerListener);
    }
}
