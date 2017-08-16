package com.wehud.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wehud.model.User;

import java.util.List;

public final class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersVH> {

    private List<User> mUsers;
    private static int mSelectedItem = -1;

    public UsersAdapter(List<User> users) {
        mUsers = users;
    }

    @Override
    public UsersVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new UsersVH(view);
    }

    @Override
    public void onBindViewHolder(UsersVH holder, int position) {
        User user = mUsers.get(position);

        String username = user.getUsername();

        holder.username.setText(username);
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    @Override
    public long getItemId(int position) {
        if (position == -1) return mSelectedItem;
        return super.getItemId(position);
    }

    static class UsersVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView username;

        UsersVH(View view) {
            super(view);
            view.setOnClickListener(this);
            username = (TextView) view.findViewById(android.R.id.text1);
        }

        @Override
        public void onClick(View view) {
            mSelectedItem = getAdapterPosition();
        }
    }
}
