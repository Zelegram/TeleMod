package org.telegram.mod

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.mod.data.BotScript
import org.telegram.mod.data.ScriptItem
import org.telegram.mod.data.ScriptRepository
import org.telegram.mod.script.ScriptFileManager
import org.telegram.mod.view.ScriptView
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import java.io.File
import java.net.URL

class TeleModActivity(
    private val callback: (() -> Unit)? = null
): BaseFragment() {
    private val botItems = mutableListOf<BotScript>()
    private val mainAdapter = MainAdapter(botItems)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun createView(context: Context?): View {
        setupMenu()

        swipeRefreshLayout = SwipeRefreshLayout(getContext()).apply {
            setOnRefreshListener { loadData() }
        }

        val recycler = RecyclerListView(context, resourceProvider)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = mainAdapter
        fragmentView = swipeRefreshLayout
        swipeRefreshLayout.addView(recycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        return fragmentView
    }

    private fun setupMenu() {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setAllowOverlayTitle(true)
        actionBar.setTitle(context.getString(R.string.telemod_script))

        val menu = actionBar.menu ?: actionBar.createMenu()
        menu.addItem(0, R.drawable.ic_ab_search)
            .setIsSearchField(true)
            .setActionBarMenuItemSearchListener(object : ActionBarMenuItemSearchListener() {
                val tempList = mutableListOf<BotScript>()
                override fun onSearchExpand() {
                    tempList.addAll(botItems)
                }

                override fun onSearchCollapse() {
                    val previousSize = botItems.size
                    botItems.clear()
                    botItems.addAll(tempList)
                    mainAdapter.notifyItemRangeChanged(0, maxOf(previousSize, botItems.size))

                    tempList.clear()
                }

                override fun onTextChanged(editText: EditText) {
                    val query = editText.text.toString().trim()
                    if (query.isNotEmpty()) {
                        val filteredList = tempList.filter { it.toString().contains(query, ignoreCase = true) }

                        val previousSize = botItems.size
                        botItems.clear()
                        mainAdapter.notifyItemRangeRemoved(0, previousSize)

                        botItems.addAll(filteredList)
                        mainAdapter.notifyItemRangeInserted(0, botItems.size)
                    }
                }
            })

        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })
    }

    private fun onClickItem(botName: String, botId: String, item: ScriptItem){
        val dialog = AlertDialog(context, AlertDialog.ALERT_TYPE_LOADING).apply {
            setTitle(botName)
            setMessage(item.title)
        }

        scope.launch {
            dialog.show()

            val result = withContext(Dispatchers.IO){
                runCatching {
                    val directory = File(TeleModConst.getScriptBaseDirectory(context), botId).apply { mkdirs() }
                    val file = File(directory,"${item.author}.js")
                    // Delete if disabled file exist
                    ScriptFileManager.toDisable(file).let { if(it.exists()) it.delete() }
                    // Write file
                    file.writeText(URL(item.url).readText())
                }
            }

            dialog.dismiss()

            if (result.isSuccess) {
                AlertDialog(context,AlertDialog.ALERT_TYPE_MESSAGE).apply {
                    setTitle(botName)
                    setTopAnimation(R.raw.premium_gift, Theme.getColor(Theme.key_dialogTopBackground))
                    setMessage(LocaleController.getString(R.string.script_download_success))
                    setPositiveButton(LocaleController.getString(R.string.launch)) { _, _ ->
                        callback?.let {
                            finishFragment()
                        } ?: context.openInternalLink(item.ref.ifEmpty { "https://t.me/${botId}" })
                    }
                    setNegativeButton(LocaleController.getString(R.string.Done), null)
                }.show()
            }else{
                BulletinFactory.of(this@TeleModActivity).createErrorBulletin("${result.exceptionOrNull()?.message}").show()
            }
        }
    }

    private fun loadData(){
        scope.launch {
            swipeRefreshLayout.isRefreshing = true
            val size = botItems.size
            botItems.clear()
            mainAdapter.notifyItemRangeRemoved(0,size)

            val result = withContext(Dispatchers.IO){
                runCatching { ScriptRepository.loadItems() }
            }

            result.onSuccess {
                botItems.addAll(it)
                mainAdapter.notifyItemRangeInserted(0,botItems.size)
            }.onFailure {
                BulletinFactory.of(this@TeleModActivity).createErrorBulletin("${it.message}").show()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onBecomeFullyVisible() {
        super.onBecomeFullyVisible()
        loadData()
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        scope.cancel()
        callback?.invoke()
    }

    private suspend fun loadDrawable(context: Context, imageUrl: String): Drawable? {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(110)
            .build()
        return loader.execute(request).image?.asDrawable(context.resources)
    }

    inner class MainAdapter(
        private val items: MutableList<BotScript>
    ) : RecyclerListView.SelectionAdapter() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val item = ScriptView(context = parent.context, resourcesProvider =  resourceProvider)
            return RecyclerListView.Holder(item)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val current = items[position]
            val layout = holder.itemView as ScriptView
            layout.header.setText(current.name)
            layout.header.setOnClickListener { holder.itemView.context.openInternalLink("https://t.me/${current.id}") }
            layout.content.layoutManager = LinearLayoutManager(holder.itemView.context)
            layout.content.adapter = ItemAdapter(current.items)
            layout.content.setOnItemClickListener { _, childPosition ->
                onClickItem(current.name,current.id,current.items[childPosition])
            }
        }

        override fun getItemCount(): Int = items.size

        override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean = false
    }

    inner class ItemAdapter(
        private val items: List<ScriptItem>
    ) : RecyclerListView.SelectionAdapter(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return RecyclerListView.Holder(TextCell(parent.context,resourceProvider))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            val cell = holder.itemView as TextCell
            cell.setTextAndIcon(item.title,R.drawable.round_ic_extension_24dp,false)
            cell.setSubtitle("${item.version} | ${item.author}")
            scope.launch {
                loadDrawable(holder.itemView.context,item.icon)?.let {
                    cell.setTextAndIcon(item.title,it,false)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean = true
    }
}