/*
 * Copyright 2013 Kirk Rader
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.rader.wyfy;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * {@link FragmentActivity} to display {@link SavedRowsFragment}
 * 
 * @author Kirk
 */
public class SavedRowsActivity extends FragmentActivity {

    /**
     * {@link SavedRowsFragment}
     */
    private SavedRowsFragment deleteRowsFragment;

    /**
     * @param savedInstanceState
     *            saved state or <code>null</code>
     * 
     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_rows_activity);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        deleteRowsFragment = new SavedRowsFragment();
        transaction.add(R.id.saved_rows_activity, deleteRowsFragment);
        transaction.commit();

    }
}
