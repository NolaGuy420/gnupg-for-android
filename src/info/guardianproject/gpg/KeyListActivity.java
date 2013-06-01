/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.gpg;

import info.guardianproject.gpg.apg_compat.Apg;
import info.guardianproject.gpg.apg_compat.Id;

import java.util.Vector;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class KeyListActivity extends Activity {
    protected ListView mListView;
    protected KeyListAdapter mListAdapter;
    protected View mFilterLayout;
    protected Button mClearFilterButton;
    protected TextView mFilterInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.key_list);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        mListView = (ListView) findViewById(R.id.list);
        String action = getIntent().getAction();
        if (action == null || action.equals(""))
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        else if (action.equals(Apg.Intent.SELECT_PUBLIC_KEYS))
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        else if (action.equals(Apg.Intent.SELECT_SECRET_KEY)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mListView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    Intent data = new Intent();
                    String[] userId = (String[])mListView.getItemAtPosition(position);
                    data.putExtra(Apg.EXTRA_KEY_ID, id);
                    data.putExtra(Apg.EXTRA_USER_ID, Apg.userId(userId));
                    setResult(RESULT_OK, data);
                    finish();
                }
            });
        }

        Button okButton = (Button) findViewById(R.id.btn_ok);
        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                okClicked();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cancelClicked();
            }
        });

        mFilterLayout = findViewById(R.id.layout_filter);
        mFilterInfo = (TextView) mFilterLayout.findViewById(R.id.filterInfo);
        mClearFilterButton = (Button) mFilterLayout.findViewById(R.id.btn_clear);

        mClearFilterButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                handleIntent(new Intent());
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String searchString = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchString = intent.getStringExtra(SearchManager.QUERY);
            if (searchString != null && searchString.trim().length() == 0) {
                searchString = null;
            }
        }

        long selectedKeyIds[] = null;
        selectedKeyIds = intent.getLongArrayExtra(Apg.EXTRA_SELECTION);

        if (selectedKeyIds == null) {
            Vector<Long> vector = new Vector<Long>();
            for (int i = 0; i < mListView.getCount(); ++i) {
                if (mListView.isItemChecked(i)) {
                    vector.add(mListView.getItemIdAtPosition(i));
                }
            }
            selectedKeyIds = new long[vector.size()];
            for (int i = 0; i < vector.size(); ++i) {
                selectedKeyIds[i] = vector.get(i);
            }
        }

        if (searchString == null) {
            mFilterLayout.setVisibility(View.GONE);
        } else {
            mFilterLayout.setVisibility(View.VISIBLE);
            mFilterInfo.setText(getString(R.string.filterInfo, searchString));
        }

        mListAdapter = new KeyListAdapter(this, mListView, getIntent().getAction(),
                searchString, selectedKeyIds);
        mListView.setAdapter(mListAdapter);

        if (selectedKeyIds != null) {
            for (int i = 0; i < mListAdapter.getCount(); ++i) {
                long keyId = mListAdapter.getItemId(i);
                for (int j = 0; j < selectedKeyIds.length; ++j) {
                    if (keyId == selectedKeyIds[j]) {
                        mListView.setItemChecked(i, true);
                        break;
                    }
                }
            }
        }
    }

    private void cancelClicked() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private void okClicked() {
        Intent data = new Intent();
        Vector<Long> keys = new Vector<Long>();
        Vector<String> userIds = new Vector<String>();
        for (int i = 0; i < mListView.getCount(); ++i) {
            if (mListView.isItemChecked(i)) {
                keys.add(mListView.getItemIdAtPosition(i));
                String userId[] = (String[]) mListView.getItemAtPosition(i);
                userIds.add(Apg.userId(userId));
            }
        }
        long selectedKeyIds[] = new long[keys.size()];
        for (int i = 0; i < keys.size(); ++i) {
            selectedKeyIds[i] = keys.get(i);
        }
        String userIdArray[] = new String[0];
        data.putExtra(Apg.EXTRA_SELECTION, selectedKeyIds);
        data.putExtra(Apg.EXTRA_USER_IDS, userIds.toArray(userIdArray));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.search, 0, R.string.menu_search)
                .setIcon(android.R.drawable.ic_menu_search);
        return true;
    }
}
