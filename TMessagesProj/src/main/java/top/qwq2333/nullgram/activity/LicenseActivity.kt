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
package top.qwq2333.nullgram.activity

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ActionBar.Theme.ResourcesProvider
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.BlurredRecyclerView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.RecyclerListView.SelectionAdapter
import org.telegram.ui.Components.SizeNotifierFrameLayout
import top.qwq2333.nullgram.createLinear
import top.qwq2333.nullgram.utils.Log

class LicenseActivity : BaseFragment() {
    private var listView: ListView? = null
    private var resourcesProvider: ResourcesProvider? = null

    @Serializable
    data class AboutLibraries(
        val libraries: List<LibraryLicense>,
        val licenses: Map<String, License>
    ) {
        @Serializable
        data class DeveloperInfo(
            val name: String? = null
        )

        @Serializable
        data class LibraryLicense(
            val uniqueId: String,
            val name: String? = null,
            val website: String? = null,
            val licenses: List<String>,
            val developers: List<DeveloperInfo> = emptyList()
        )

        @Serializable
        data class License(
            val content: String? = null,
            val hash: String,
            val url: String,
            val name: String?
        )
    }

    val list: AboutLibraries by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        val libs = ApplicationLoader.applicationContext.resources.openRawResource(R.raw.aboutlibraries)
        val content = libs.bufferedReader().use { x -> x.readText() }
        json.decodeFromString(content)
    }

    override fun createView(context: Context): View {
        super.onFragmentCreate()

        actionBar.apply {
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle(LocaleController.getString("OpenSource", R.string.OpenSource))
            if (AndroidUtilities.isTablet()) {
                occupyStatusBar = false
            }
            setActionBarMenuOnItemClick(object : ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) {
                        finishFragment()
                    }
                }
            })
        }

        val frameLayout = SizeNotifierFrameLayout(context).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }.also { fragmentView = it }

        BlurredRecyclerView(context).apply {
            setClipToPadding(false)
            setItemAnimator(null)
            setItemViewCacheSize(20)
            additionalClipBottom = AndroidUtilities.dp(200f)
            isVerticalScrollBarEnabled = false
            layoutManager = LinearLayoutManager(context)
            adapter = LicenseAdapter(list)
        }.also {
            frameLayout.addView(it, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        }
        return fragmentView
    }

    override fun setParentLayout(layout: INavigationLayout?) {
        if (layout != null && layout.lastFragment != null) {
            resourcesProvider = layout.lastFragment.resourceProvider
        }
        super.setParentLayout(layout)
    }

    private class LicenseAdapter(val aboutLibraries: AboutLibraries) : SelectionAdapter() {
        val libraries: List<Pair<AboutLibraries.License, List<AboutLibraries.LibraryLicense>>> by lazy {
            mutableListOf<Pair<AboutLibraries.License, List<AboutLibraries.LibraryLicense>>>().apply {
                aboutLibraries.licenses.forEach { (k, v) ->
                    val libraries = aboutLibraries.libraries.filter { it.licenses.contains(k) }
                    add(v to libraries)
                }
            }.toList()
        }

        val layoutList: List<Pair<String, Any?>> by lazy {
            mutableListOf<Pair<String, Any?>>().apply {
                libraries.forEach {
                    add("lic" to it.first)
                    it.first.apply {
                        if (content != null) add("desc" to content)
                    }
                    add("space" to null)
                    add("lib" to null)
                    add("desc1" to it.second)
                    add("space" to null)
                }
            }.toList()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
            0 -> RecyclerListView.Holder(HeaderCell(parent.context).apply {
                setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                setLayoutParams(RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT))
            })

            1, 5 -> RecyclerListView.Holder(TextView(parent.context).apply {
                setTextColor(Theme.getColor(Theme.key_dialogTextGray))
                movementMethod = ScrollingMovementMethod.getInstance()
                isNestedScrollingEnabled = true
                isVerticalScrollBarEnabled = true
                textSize = 12f
                layoutParams = createLinear {
                    height = AndroidUtilities.dp(100f)
                    rightMargin = AndroidUtilities.dp(40f)
                    leftMargin = AndroidUtilities.dp(40f)
                    topMargin = AndroidUtilities.dp(8f)
                }
                setOnTouchListener { v, _ ->
                    performClick()
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    false
                }
            })

            2, 3 -> RecyclerListView.Holder(TextInfoPrivacyCell(parent.context).apply {
                setLayoutParams(RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT))
                setTextColor(Theme.getColor(Theme.key_dialogTextGray))
                background = Theme.getThemedDrawable(parent.context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow)
            })

            4 -> RecyclerListView.Holder(ShadowSectionCell(parent.context))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            Log.d("LicenseAdapter", "onBindViewHolder: $pos")
            when (getItemViewType(pos)) {
                0 -> (holder.itemView as HeaderCell).apply {
                    setTextColor(Theme.getColor(Theme.key_dialogTextBlack))
                    setText((layoutList[pos].second as AboutLibraries.License).name)
                }

                1 -> (holder.itemView as TextView).apply {
                    text = layoutList[pos].second as String
                }

                2 -> (holder.itemView as TextInfoPrivacyCell).apply {
                    text = layoutList[pos].second as String
                }

                3 -> (holder.itemView as TextInfoPrivacyCell).apply {
                    text = "Dependencies that are using this license:"
                }

                4 -> (holder.itemView as ShadowSectionCell).apply {
                    setLayoutParams(RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(12f)))
                }

                5 -> (holder.itemView as TextView).apply {
                    text = (layoutList[pos].second as List<AboutLibraries.LibraryLicense>).joinToString("\n") {
                        it.name ?: it.uniqueId
                    }
                }
            }
        }

        override fun getItemViewType(pos: Int): Int = when (layoutList[pos].first) {
            "lic" -> 0
            "desc" -> 1
            "url" -> -1 // reserved
            "lib" -> 3
            "space" -> 4
            "desc1" -> 5
            else -> -1
        }

        override fun getItemCount(): Int = layoutList.size

        override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean = true

    }
}
