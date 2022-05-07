package com.socialmedia.hichat.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.bumptech.glide.Glide;
import com.socialmedia.hichat.Activity.ProfileActivity;
import com.socialmedia.hichat.Model.Contacts;
import com.socialmedia.hichat.R;
import com.socialmedia.hichat.databinding.UserDisplayLayoutBinding;


import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder> implements Filterable {

    Context context;
    ArrayList<Contacts> contacts;
    ArrayList<Contacts> contactsAll;

    public ContactsAdapter(Context context,ArrayList<Contacts> contacts){
        this.context=context;
        this.contacts=contacts;
        this.contactsAll=new ArrayList<>(contacts);
    }
    @NonNull
    @Override
    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.user_display_layout,parent,false);
        return new ContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactsViewHolder holder, int position) {
        Contacts contact = contacts.get(position);
        holder.binding.findFriendUsername.setText(contact.getName());
        holder.binding.findFriendStatus.setText(contact.getStatus());
        Glide.with(context)
                .load(contact.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(holder.binding.findFriendProfileimg);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String visit_user_id = contact.getUid();
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("visit_user_id",visit_user_id);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    Filter filter = new Filter() {

        //run on background thread
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Contacts> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0)
            {
                filteredList.addAll(contactsAll);
            }
            else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Contacts contacts: contactsAll){
                    if (contacts.getName().toLowerCase().contains(filterPattern)){
                        filteredList.add(contacts);
                    }
                }
            }

            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;
            return filterResults;
        }

        //run om ui thread
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            contacts.clear();
            contacts.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public class ContactsViewHolder extends RecyclerView.ViewHolder {

        UserDisplayLayoutBinding binding;
        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = UserDisplayLayoutBinding.bind(itemView);
        }
    }
}