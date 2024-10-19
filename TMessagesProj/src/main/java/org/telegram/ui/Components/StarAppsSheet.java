/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package org.telegram.ui.Components;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

public class StarAppsSheet extends BottomSheetWithRecyclerListView {

    private DialogsBotsAdapter adapter;

    public StarAppsSheet(Context context) {
        super(context, null, true, false, false, null);

        fixNavigationBar();
        handleOffset = true;
        setShowHandle(true);

        setSlidingActionBar();

        recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
        recyclerListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                adapter.checkBottom();
            }
        });
        recyclerListView.setOnItemClickListener((view, position) -> {
            position--;
            Object obj = adapter.getObject(position);
            if (obj instanceof TLRPC.User) {
                MessagesController.getInstance(currentAccount).openApp(attachedFragment, (TLRPC.User) obj, null, 0, null);
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.SearchAppsExamples);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new DialogsBotsAdapter(listView, getContext(), currentAccount, 0, true, resourcesProvider);
        adapter.setApplyBackground(false);
        return adapter;
    }


}
