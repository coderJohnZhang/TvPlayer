package com.tcl.john.tvplayer

import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

import java.util.ArrayList

class ListViewAdapter<T>(mContext: Context, dataList: List<T>, private val layoutId: Int, private val variableId: Int) : BaseAdapter() {
    private val TAG = ListViewAdapter::class.java.name
    private val inflater: LayoutInflater
    private var dataList: List<T> = ArrayList()

    init {
        this.dataList = dataList
        inflater = LayoutInflater.from(mContext)
    }

    fun updateData(dataList: List<T>?) {
        if (dataList != null) {
            this.dataList = dataList
            notifyDataSetChanged()
        }
    }

    override fun getCount(): Int {
        return dataList.size
    }

    override fun getItem(position: Int): T {
        return dataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val dataBinding: ViewDataBinding = if (convertView == null) {
            DataBindingUtil.inflate(inflater, layoutId, parent, false)
        } else {
            DataBindingUtil.getBinding(convertView)
        }
        dataBinding.setVariable(variableId, dataList[position])

        return dataBinding.root
    }
}