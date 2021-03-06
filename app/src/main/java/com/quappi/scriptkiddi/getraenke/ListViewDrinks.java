package com.quappi.scriptkiddi.getraenke;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.quappi.scriptkiddi.getraenke.adapter.DrinksListViewAdapter;
import com.quappi.scriptkiddi.getraenke.controller.drinkController;
import com.quappi.scriptkiddi.getraenke.events.DrinkControllerInitFinished;
import com.quappi.scriptkiddi.getraenke.events.DrinkUpdated;
import com.quappi.scriptkiddi.getraenke.events.PersonUpdated;
import com.quappi.scriptkiddi.getraenke.utils.Drink;
import com.quappi.scriptkiddi.getraenke.utils.Person;
import com.quappi.scriptkiddi.getraenke.utils.exception.WrongPasswordException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class ListViewDrinks extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private DrinksListViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Person person;
    private ArrayList<Drink> drinks = new ArrayList<>();
    private final static String TAG = "ViewDrinks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view_drinks);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        drinkController.init(getApplicationContext());
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        //Set Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter

        this.person = (Person)getIntent().getSerializableExtra("Person");

        TextView user = (TextView) findViewById(R.id.user);
        user.setText(String.format("User: %s %s", person.getFirstName(), person.getLastName()));
        TextView moneyOwed = (TextView) findViewById(R.id.money_owed);
        moneyOwed.setText(String.format("Guthaben: %.2f €", person.getCredit()));
        moneyOwed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), PayActivity.class);
                intent.putExtra("Person", person);
                startActivity(intent);
            }
        });

        mAdapter = new DrinksListViewAdapter(drinks, this.person, getApplicationContext());
        mRecyclerView.setAdapter(mAdapter);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new IntentIntegrator(ListViewDrinks.this).initiateScan();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drinks_menu_logged_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.check_balance:
                Intent intent = new Intent(this, PayActivity.class);
                intent.putExtra("Person", person);
                this.startActivity(intent);
                return true;
            case R.id.manage_person:
                intent = new Intent(this, ManagePersonActivity.class);
                intent.putExtra("Person", person);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Get the results:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Drink drink = drinkController.get(result.getContents());

                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        drinks.clear();
        drinks.addAll(drinkController.getAll());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DrinkUpdated event) {
        if(!drinks.contains(event.getDrink())){
            drinks.add(event.getDrink());
        }


        mAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(WrongPasswordException event) {

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PersonUpdated event) {
        TextView moneyOwed = (TextView) findViewById(R.id.money_owed);
        moneyOwed.setText(String.format("Guthaben: %.2f €", event.getPerson().getCredit()));
        person = event.getPerson();
        mAdapter.updatePerson(person);
    }

    @Subscribe
    public void onMessageEvent(DrinkControllerInitFinished event) {
        Log.d(TAG, "initDrinkController");

        drinks.addAll(drinkController.getAll());

        mAdapter.notifyDataSetChanged();
    }
}
