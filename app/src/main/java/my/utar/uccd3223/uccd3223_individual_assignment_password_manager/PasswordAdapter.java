package my.utar.uccd3223.uccd3223_individual_assignment_password_manager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adapter for dashboard password cards
public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {

    private final Context context;
    private List<PasswordEntry> entries;
    private OnItemClickListener listener;

    // Callback for card tap and copy button interactions
    public interface OnItemClickListener {
        void onItemClick(PasswordEntry entry);
        void onCopyClick(PasswordEntry entry, View anchorView);
    }

    public PasswordAdapter(Context context, List<PasswordEntry> entries,
                           OnItemClickListener listener) {
        this.context = context;
        this.entries = entries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_password, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PasswordEntry entry = entries.get(position);

        holder.tvTitle.setText(entry.getTitle());
        holder.tvUsername.setText(entry.getUsername() != null ?
                entry.getUsername() : "");
        holder.tvCategory.setText(entry.getCategory() != null ?
                entry.getCategory() : "");

        // Show remark if present
        String remark = entry.getRemark();
        if (remark != null && !remark.isEmpty()) {
            holder.tvRemark.setText(remark);
            holder.tvRemark.setVisibility(View.VISIBLE);
        } else {
            holder.tvRemark.setVisibility(View.GONE);
        }

        // Set category icon
        if (entry.getCategory() != null) {
            switch (entry.getCategory()) {
                case "Website":
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_web);
                    break;
                case "App":
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_app);
                    break;
                case "Bank PIN":
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_bank);
                    break;
                default:
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_lock);
                    break;
            }
        }

        setupCopyButton(holder, entry);
        setupOpenLinkButton(holder, entry);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(entry);
            }
        });
    }

    // Delegate copy click to activity for biometric auth
    private void setupCopyButton(@NonNull ViewHolder holder,
                                 @NonNull PasswordEntry entry) {
        holder.btnCopy.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCopyClick(entry, holder.btnCopy);
            }
        });
    }

    // Handle open link based on category: website opens browser, app launches package
    private void setupOpenLinkButton(@NonNull ViewHolder holder,
                                     @NonNull PasswordEntry entry) {
        holder.btnOpenLink.setOnClickListener(v -> {
            String category = entry.getCategory();
            String url = entry.getUrl();

            if ("Website".equals(category)) {
                openWebsite(url);
            } else if ("App".equals(category)) {
                launchApp(url);
            } else {
                Toast.makeText(context, R.string.no_link_for_category,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Open URL in browser, prepend https:// if no scheme present
    private void openWebsite(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, R.string.no_url,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }

    // Launch app by package ID, or redirect to Play Store if not installed
    private void launchApp(String packageId) {
        if (packageId == null || packageId.isEmpty()) {
            Toast.makeText(context, R.string.no_url,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageId);

        if (launchIntent != null) {
            context.startActivity(launchIntent);
        } else {
            // App not installed, redirect to Play Store
            Toast.makeText(context, R.string.app_not_installed,
                    Toast.LENGTH_SHORT).show();

            String playStoreUrl =
                    "https://play.google.com/store/apps/details?id=" + packageId;
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(playStoreUrl));
            context.startActivity(playStoreIntent);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void updateData(List<PasswordEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvUsername, tvCategory, tvRemark;
        ImageView ivCategoryIcon;
        ImageButton btnCopy, btnOpenLink;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRemark = itemView.findViewById(R.id.tvRemark);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnOpenLink = itemView.findViewById(R.id.btnOpenLink);
        }
    }
}
