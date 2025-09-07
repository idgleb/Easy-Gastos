package com.example.gestorgastos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.gestorgastos.R;

public class EmojiGridAdapter extends BaseAdapter {
    
    private Context context;
    private String[] emojis;
    private String selectedEmoji;
    private OnEmojiClickListener listener;
    
    public interface OnEmojiClickListener {
        void onEmojiClick(String emoji);
    }
    
    public EmojiGridAdapter(Context context, String[] emojis, String selectedEmoji) {
        this.context = context;
        this.emojis = emojis;
        this.selectedEmoji = selectedEmoji;
    }
    
    public void setOnEmojiClickListener(OnEmojiClickListener listener) {
        this.listener = listener;
    }
    
    @Override
    public int getCount() {
        return emojis.length;
    }
    
    @Override
    public Object getItem(int position) {
        return emojis[position];
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_emoji, parent, false);
            holder = new ViewHolder();
            holder.tvEmoji = convertView.findViewById(R.id.tvEmoji);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        String emoji = emojis[position];
        holder.tvEmoji.setText(emoji);
        
        // Resaltar emoji seleccionado
        if (emoji.equals(selectedEmoji)) {
            holder.tvEmoji.setBackgroundResource(R.drawable.emoji_selected_background);
        } else {
            holder.tvEmoji.setBackgroundResource(R.drawable.emoji_background);
        }
        
        // Configurar click listener
        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmojiClick(emoji);
            }
        });
        
        return convertView;
    }
    
    static class ViewHolder {
        TextView tvEmoji;
    }
}
