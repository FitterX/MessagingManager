package com.message.messagingmanager.view.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.message.messagingmanager.R
import com.message.messagingmanager.model.Contacts
import com.message.messagingmanager.view.activity.ContactsActivity
import com.message.messagingmanager.view.activity.ScheduleMessageActivity
import java.util.*

class ContactsAdapter(private val contactsList: ArrayList<Contacts>, private var contactsActivity : ContactsActivity, private var spinnerValue: String): RecyclerView.Adapter<ContactsAdapter.DataViewHolder>(), Filterable {

    private var searchContactsList: ArrayList<Contacts> = ArrayList(contactsList)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_person,parent,false)
        return DataViewHolder(view)
    }

    override fun getItemCount(): Int {
        return contactsList.size
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {

        val contact = contactsList[position]

        holder.getCardView()!!.setOnClickListener {
            val intent = Intent(contactsActivity, ScheduleMessageActivity::class.java)
            intent.putExtra("SmsReceiverName", contact.getName())
            intent.putExtra("SmsReceiverNumber", contact.getPhone())
            intent.putExtra("spinnerValue", spinnerValue)
            contactsActivity.startActivity(intent)
            contactsActivity.finish()
        }

        holder.getTxtViewPersonName()!!.text = contact.getName()
        holder.getTxtViewPhoneNumber()!!.text = contact.getPhone()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredSearchContactsList: ArrayList<Contacts> = ArrayList()

                if (constraint == null || constraint.isEmpty()) {
                    filteredSearchContactsList.addAll(searchContactsList)
                } else {
                    val filterPattern: String = constraint.toString().toLowerCase(Locale.getDefault())
                        .trim()
                    for (item in searchContactsList) {
                        if (item.getName().toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                            filteredSearchContactsList.add(item)
                        }
                    }
                }

                val filterResult = FilterResults()
                filterResult.values = filteredSearchContactsList
                return filterResult
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                contactsList.clear()
                contactsList.addAll(results!!.values as Collection<Contacts>)
                notifyDataSetChanged()
            }
        }
    }

    inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var cardView: CardView? = null
        private var txtViewPersonName: TextView? = null
        private var txtViewPhoneNumber: TextView? = null

        fun getCardView(): CardView? {
            if (cardView == null){
                cardView = itemView.findViewById(R.id.cardView)
            }
            return cardView
        }

        fun getTxtViewPersonName(): TextView? {
            if (txtViewPersonName == null) {
                txtViewPersonName = itemView.findViewById(R.id.txtViewPersonName)
            }
            return txtViewPersonName
        }

        fun getTxtViewPhoneNumber(): TextView? {
            if (txtViewPhoneNumber == null) {
                txtViewPhoneNumber = itemView.findViewById(R.id.txtViewPhoneNumber)
            }
            return txtViewPhoneNumber
        }
    }
}