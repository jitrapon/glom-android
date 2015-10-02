package com.abborg.glom.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;

import java.util.Collections;
import java.util.List;

/**
 * This class is responsible for providing the view the information it needs to render
 *
 * Created by Boat on 30/9/58.
 */
public class UserAvatarAdapter extends BaseAdapter {

    /* List of generated avatar images */
    private List<Bitmap> avatars = Collections.emptyList();

    private Context context;

    private List<User> users;

    private static LayoutInflater inflater;

    public UserAvatarAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void update(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    public void removeItem(int position) {
        //TODO
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class ViewHolder {
        ImageView avatar;
        TextView primaryText;
        TextView secondaryText;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        User user = users.get(position);

        // populate the images and texts
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.user_avatar, null);

            holder.avatar = (ImageView) convertView.findViewById(R.id.userAvatar);
            holder.primaryText = (TextView) convertView.findViewById(R.id.userPrimaryText);
            holder.secondaryText = (TextView) convertView.findViewById(R.id.userSecondaryText);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder) convertView.getTag();

        Glide.with(context)
                .load(user.getAvatar()).fitCenter()
                .transform(new CircleTransform(context))
                .override((int) context.getResources().getDimension(R.dimen.user_avatar_width),
                        (int) context.getResources().getDimension(R.dimen.user_avatar_height))
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .crossFade(1000)
                .into(holder.avatar);
        holder.primaryText.setText(users.get(position).getName());
        holder.secondaryText.setText(users.get(position).getId());

        return convertView;
    }
}
