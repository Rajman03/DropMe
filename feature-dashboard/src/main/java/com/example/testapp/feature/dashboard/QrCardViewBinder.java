package com.example.testapp.feature.dashboard;

import com.example.testapp.core.QRCodeHelper;
import com.example.testapp.core.SocialPlatform;
import com.example.testapp.core.UserQrItem;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class QrCardViewBinder {

    public interface CardActionListener {
        void onCardClicked(UserQrItem item);
        void onFavoriteToggled(UserQrItem item, boolean isFavorite);
        void onDeleteRequested(UserQrItem item);
    }

    public static View createCardView(Context context, ViewGroup parent, UserQrItem item, CardActionListener listener) {
        View cardView = LayoutInflater.from(context).inflate(R.layout.item_user_qr_card, parent, false);

        ImageView ivPlatformIcon = cardView.findViewById(R.id.ivCardPlatformIcon);
        TextView tvTag = cardView.findViewById(R.id.tvCardPlatformTag);
        TextView tvName = cardView.findViewById(R.id.tvCardName);
        TextView tvUrl = cardView.findViewById(R.id.tvCardUrl);
        ImageView btnHeart = cardView.findViewById(R.id.btnCardHeart);
        ImageView ivPreview = cardView.findViewById(R.id.ivCardQrPreview);
        ProgressBar pbLoading = cardView.findViewById(R.id.pbCardQrLoading);
        View btnOpenDetail = cardView.findViewById(R.id.btnCardOpenDetail);
        Button btnShowBig = cardView.findViewById(R.id.btnCardShowBig);
        FrameLayout btnDelete = cardView.findViewById(R.id.btnCardDelete);

        SocialPlatform platform = SocialPlatform.fromKey(item.getIcon());
        if (ivPlatformIcon != null) {
            ivPlatformIcon.setImageResource(platform.getIconRes());
        }
        if (tvTag != null) {
            tvTag.setText(platform.getDisplayName());
        }
        if (tvName != null) {
            tvName.setText(item.getName());
        }
        if (tvUrl != null) {
            tvUrl.setText(item.getUrl());
        }

        // Heart favorite button
        if (btnHeart != null) {
            btnHeart.setImageResource(item.isFavorite() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            btnHeart.setOnClickListener(v -> {
                boolean newFav = !item.isFavorite();
                item.setFavorite(newFav);
                btnHeart.setImageResource(newFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                Toast.makeText(context, newFav ? "Dodano do ulubionych" : "Usunięto z ulubionych", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onFavoriteToggled(item, newFav);
                }
            });
        }

        // Generate QR thumbnail
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        QRCodeHelper.generateQrCode(item.getUrl(), item.getQrColor(), bmp -> {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            if (bmp != null && ivPreview != null) {
                ivPreview.setImageBitmap(bmp);
            }
        });

        // Open details
        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onCardClicked(item);
            }
        };
        cardView.setOnClickListener(clickListener);
        if (btnOpenDetail != null) btnOpenDetail.setOnClickListener(clickListener);
        if (btnShowBig != null) btnShowBig.setOnClickListener(clickListener);

        // Delete button
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Usuń kod QR")
                        .setMessage("Czy na pewno chcesz usunąć kod QR dla: " + item.getName() + "?")
                        .setPositiveButton("Usuń", (d, w) -> {
                            if (listener != null) {
                                listener.onDeleteRequested(item);
                            }
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            });
        }

        return cardView;
    }
}
