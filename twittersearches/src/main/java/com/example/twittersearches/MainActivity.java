package com.example.twittersearches;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // name of SharedPreferences XML file that stores the saved searches
    private static final String SEARCHES = "searches";
    private EditText queryEditText;                        // where user enters a query
    private EditText tagEditText;                          // where user enters a query's tag
    private FloatingActionButton saveFloatingActionButton; // save search
    private SharedPreferences savedSearches;               // user's favorite searches
    private List<String> tags;                             // list of tags for saved searches
    private SearchesAdapter adapter;                       // for binding data to RecyclerVie

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        queryEditText = ((TextInputLayout) findViewById(R.id.queryTextInputLayout)).getEditText();
        queryEditText.addTextChangedListener(textWatcher);
        tagEditText = ((TextInputLayout) findViewById(R.id.tagTextInputLayout)).getEditText();
        tagEditText.addTextChangedListener(textWatcher);

        // get the SharedPreferences containing the user's saved searches
        savedSearches = getSharedPreferences(SEARCHES, MODE_PRIVATE);

        // store the saved tags in an ArrayList then sort them
        tags = new ArrayList<>(savedSearches.getAll().keySet());
        Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        // use a LinearLayoutManager to display items in a vertical list
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // create RecyclerView.Adapter to bind tags to the RecyclerView
        adapter = new SearchesAdapter(tags, itemClickListener, itemLongClickListener);
        recyclerView.setAdapter(adapter);

        // specify a custom ItemDecorator to draw lines between list items
        recyclerView.addItemDecoration(new ItemDivider(this));

        // register listener to save a new or edited search
        saveFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        saveFloatingActionButton.setOnClickListener(saveButtonListener);
        updateSaveFAB(); // hides button because EditTexts initially empt
    }

    // hide/show saveFloatingActionButton based on EditTexts' contents
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        // hide/show the saveFloatingActionButton after user changes input
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateSaveFAB();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    // shows or hides the saveFloatingActionButton
    private void updateSaveFAB() {
        // check if there is input in both EditTexts
        if (queryEditText.getText().toString().isEmpty() || tagEditText.getText().toString().isEmpty()) {
            saveFloatingActionButton.hide();
        } else {
            saveFloatingActionButton.show();
        }
    }

    // saveButtonListener save a tag-query pair into SharedPreferences
    private final View.OnClickListener saveButtonListener = new View.OnClickListener() {
        // add/update search if neither query nor tag is empty
        @Override
        public void onClick(View v) {
            String query = queryEditText.getText().toString();
            String tag = tagEditText.getText().toString();
            if (!query.isEmpty() && !tag.isEmpty()) {
                // hide the virtual keyboard
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(v.getWindowToken(), 0);
                addTaggedSearch(tag, query);    // add/update the search
                queryEditText.setText("");      // clear queryEditText
                tagEditText.setText("");        // clear tagEditText
                queryEditText.requestFocus();   // queryEditText gets focus
            }
        }
    };

    // itemClickListener launches web browser to display search results
    private final View.OnClickListener itemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // get query string and create a URL representing the search
            String tag = ((TextView) v).getText().toString();
            String urlString = getString(R.string.search_URL) +
                    Uri.encode(savedSearches.getString(tag, ""), "UTF-8");
            // create an Intent to launch a web browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
            startActivity(webIntent); // show results in web browser
        }
    };

    // itemLongClickListener displays a dialog allowing the user to share edit or delete a saved search
    private final View.OnLongClickListener itemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            // get the tag that the user long touched
            final String tag = ((TextView) v).getText().toString();

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.share_edit_delete_title, tag));
            // set list of items to display and create event handler
            builder.setItems(R.array.dialog_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            shareSearch(tag);
                            break;
                        case 1:
                            // set EditTexts to match chosen tag and query
                            tagEditText.setText(tag);
                            queryEditText.setText(savedSearches.getString(tag, ""));
                            break;
                        case 2:
                            deleteSearch(tag);
                            break;
                    }
                }
            });
            builder.setNegativeButton(getString(R.string.cancel), null);
            builder.create().show(); // display the AlertDialog
            return true;
        }
    };

    // deletes a search after the user confirms the delete operation
    private void deleteSearch(final String tag) {
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
        confirmBuilder.setMessage(getString(R.string.confirm_message, tag));
        confirmBuilder.setNegativeButton(getString(R.string.cancel), null);
        confirmBuilder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tags.remove(tag); // remove tag from tags
                SharedPreferences.Editor preferencesEditor = savedSearches.edit();
                preferencesEditor.remove(tag); // remove search
                preferencesEditor.apply(); // save the changes
                // rebind tags to RecyclerView to show updated list
                adapter.notifyDataSetChanged();
            }
        });
        confirmBuilder.create().show();
    }

    // allow user to choose an app for sharing URL of a saved search
    private void shareSearch(String tag) {
        // create the URL representing the search
        String urlString = getString(R.string.search_URL) +
                Uri.encode(savedSearches.getString(tag, ""), "UTF-8");

        // create Intent to share urlString
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, urlString));
        shareIntent.setType("text/plain");

        // display apps that can share plain text
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_search)));
    }

    // add new search to file, then refresh all buttons
    private void addTaggedSearch(String tag, String query) {
        // get a SharedPreferences.Editor to store new tag/query pair
        SharedPreferences.Editor preferencesEditor = savedSearches.edit();
        preferencesEditor.putString(tag, query);    // store current search
        preferencesEditor.apply();                  // store the updated preferences

        // if tag is new, add to and sort tags, then display updated list
        if (!tags.contains(tag)) {
            tags.add(tag); // add new tag
            Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
            adapter.notifyDataSetChanged();         // update tags in RecyclerView
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
