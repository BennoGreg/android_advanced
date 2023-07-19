package com.example.canteenchecker.consumerapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.canteenchecker.consumerapp.R
import com.example.canteenchecker.consumerapp.core.Canteen
import com.example.canteenchecker.consumerapp.databinding.ActivityCanteensBinding
import com.example.canteenchecker.consumerapp.databinding.ItemCanteenBinding
import com.example.canteenchecker.consumerapp.proxy.ServiceProxyFactory
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CanteensActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCanteensBinding

    private val canteensAdapter: CanteensAdapter = CanteensAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_canteens)

        binding = ActivityCanteensBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rcvCanteens.layoutManager = LinearLayoutManager(this)
        binding.rcvCanteens.adapter = canteensAdapter

        binding.srlSwipeRefreshLayout.setOnRefreshListener { updateCanteens() }

        binding.btnSearch.setOnClickListener { v: View? -> updateCanteens()}

        updateCanteens()
    }

    private fun updateCanteens() {
        binding.srlSwipeRefreshLayout.isRefreshing = true

        val filter = binding.edtSearch.text.toString()

        /*val executor : ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            // background work here
            val canteens = ServiceProxyFactory.createProxy().getCanteens(filter)
            handler.post {
                // UI thread work here
                canteensAdapter.displayCanteens(canteens)
                binding.srlSwipeRefreshLayout.isRefreshing = false
            }
        }*/

        // kotlin coroutines
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            error("Exception: ${throwable.localizedMessage}")
        }
        CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
            // background work here
            val canteens = ServiceProxyFactory.createProxy().getCanteens(filter)
            withContext(Dispatchers.Main) {
                // UI thread work here
                canteensAdapter.displayCanteens(canteens)
                binding.srlSwipeRefreshLayout.isRefreshing = false
            }
        }

    }

    private class CanteensAdapter : RecyclerView.Adapter<CanteensAdapter.ViewHolder>() {

        private val canteensList: MutableList<Canteen> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanteensAdapter.ViewHolder {
            val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_canteen, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: CanteensAdapter.ViewHolder, position: Int) {
            holder.updateView(canteensList[position])
        }

        override fun getItemCount(): Int {
            return canteensList.size
        }

        fun displayCanteens(canteens: Collection<Canteen>?) {
            canteensList.clear()
            if (canteens != null) {
                canteensList.addAll(canteens)
            }
            notifyDataSetChanged()
        }

        class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val binding = ItemCanteenBinding.bind(itemView)

            fun updateView(canteen: Canteen) {
                binding.txvName.text = canteen.name
                binding.txvDish.text = canteen.dish
                binding.txvDishPrice.text = NumberFormat.getCurrencyInstance().format(canteen.dishPrice)
                binding.rtbAverageRating.rating = canteen.averageRating
                binding.txvAverageRating.text = String.format("%.2f", canteen.averageRating) //canteen.averageRating.toString()

                // start canteen details activity
                itemView.setOnClickListener { v: View ->
                    v.context.startActivity(CanteenDetailsActivity.createIntent(v.context, canteen.id))
                }
            }
        }

    }
}