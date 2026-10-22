package com.akstarcompany.salony.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akstarcompany.salony.ai.model.ChatMessage;
import com.example.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMessageActionListener {
        void onPlayVoice(ChatMessage message);
        void onStopVoice(ChatMessage message);
        void onCopyText(ChatMessage message);
        void onShareText(ChatMessage message);
    }

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<ChatMessage> messageList;
    private final OnMessageActionListener listener;
    private final SimpleDateFormat timeFormat;

    public ChatAdapter(List<ChatMessage> messageList, OnMessageActionListener listener) {
        this.messageList = messageList;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_ai_message, parent, false);
            return new AiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String timeStr = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvUserMessage.setText(message.getText());
            userHolder.tvUserTime.setText(timeStr);
        } else if (holder instanceof AiViewHolder) {
            AiViewHolder aiHolder = (AiViewHolder) holder;
            aiHolder.tvAiMessage.setText(message.getText());
            aiHolder.tvAiTime.setText(timeStr);

            if (message.getActionLabel() != null && !message.getActionLabel().isEmpty()) {
                aiHolder.tvActionBadge.setVisibility(View.VISIBLE);
                aiHolder.tvActionBadge.setText("⚡ " + message.getActionLabel());
            } else {
                aiHolder.tvActionBadge.setVisibility(View.GONE);
            }

            aiHolder.btnPlayVoice.setOnClickListener(v -> {
                if (listener != null) listener.onPlayVoice(message);
            });

            aiHolder.btnStopVoice.setOnClickListener(v -> {
                if (listener != null) listener.onStopVoice(message);
            });

            aiHolder.btnCopyText.setOnClickListener(v -> {
                if (listener != null) listener.onCopyText(message);
            });

            aiHolder.btnShareText.setOnClickListener(v -> {
                if (listener != null) listener.onShareText(message);
            });
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserMessage;
        TextView tvUserTime;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvUserTime = itemView.findViewById(R.id.tvUserTime);
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvAiMessage;
        TextView tvAiTime;
        TextView tvActionBadge;
        ImageButton btnPlayVoice;
        ImageButton btnStopVoice;
        ImageButton btnCopyText;
        ImageButton btnShareText;

        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAiMessage = itemView.findViewById(R.id.tvAiMessage);
            tvAiTime = itemView.findViewById(R.id.tvAiTime);
            tvActionBadge = itemView.findViewById(R.id.tvActionBadge);
            btnPlayVoice = itemView.findViewById(R.id.btnPlayVoice);
            btnStopVoice = itemView.findViewById(R.id.btnStopVoice);
            btnCopyText = itemView.findViewById(R.id.btnCopyText);
            btnShareText = itemView.findViewById(R.id.btnShareText);
        }
    }
}
