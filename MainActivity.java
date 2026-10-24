package com.cleantv.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    // ===== Things you can change =====
    // Number of columns in the grid, and the number of dock slots.
    private static final int COLUMNS = 4;

    // Apps shown in the dock, in this order (package names).
    private static final String[] DOCK_PACKAGES = {
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.stremio.one",
            "org.videolan.vlc"
    };

    // Apps that are never shown (factory and test tools, this launcher's neighbours).
    private static final String[] HIDDEN_PACKAGES = {
            "com.homwee.factory",
            "com.mediatek.testpq",
            "com.mstar.android.tv.disclaimercustomization",
            "me.efesser.flauncher"
    };
    // =================================

    private static class AppItem {
        String pkg;
        String cls;
        String label;
        Drawable banner;
        Drawable icon;
    }

    private ScrollView scroll;
    private TextView greeting;
    private String signature = "";
    private float density = 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!currentSignature().equals(signature)) {
            buildUi();
        }
        updateGreeting();
    }

    @Override
    public void onBackPressed() {
        // Stay on the home screen.
    }

    private int dp(int v) {
        return Math.round(v * density);
    }

    private GradientDrawable rounded(int color, int radiusPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radiusPx);
        return gd;
    }

    private boolean isHidden(String pkg) {
        for (String h : HIDDEN_PACKAGES) {
            if (h.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    private String currentSignature() {
        StringBuilder sb = new StringBuilder();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : getPackageManager().queryIntentActivities(i, 0)) {
                sb.append(ri.activityInfo.packageName).append(',');
            }
        }
        return sb.toString();
    }

    private List<AppItem> loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, AppItem> map = new LinkedHashMap<>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            List<ResolveInfo> list = pm.queryIntentActivities(i, 0);
            for (ResolveInfo ri : list) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || isHidden(pkg) || map.containsKey(pkg)) {
                    continue;
                }
                AppItem a = new AppItem();
                a.pkg = pkg;
                a.cls = ri.activityInfo.name;
                a.label = String.valueOf(ri.loadLabel(pm));
                Drawable b = null;
                try {
                    b = ri.activityInfo.loadBanner(pm);
                } catch (Exception e) {
                    b = null;
                }
                a.banner = b;
                a.icon = ri.loadIcon(pm);
                map.put(pkg, a);
            }
        }
        List<AppItem> result = new ArrayList<>(map.values());
        Collections.sort(result, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem x, AppItem y) {
                return x.label.compareToIgnoreCase(y.label);
            }
        });
        return result;
    }

    private void buildUi() {
        signature = currentSignature();
        List<AppItem> all = loadApps();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int sidePad = dp(72);
        int gap = dp(20);
        int bandPad = dp(18);
        int total = dm.widthPixels - 2 * sidePad;
        int gridTileW = (total - (COLUMNS - 1) * gap) / COLUMNS;
        int dockTileW = (total - 2 * bandPad - (COLUMNS - 1) * gap) / COLUMNS;

        List<AppItem> dock = new ArrayList<>();
        for (String p : DOCK_PACKAGES) {
            if (dock.size() >= COLUMNS) {
                break;
            }
            for (AppItem a : all) {
                if (a.pkg.equals(p)) {
                    dock.add(a);
                    break;
                }
            }
        }
        List<AppItem> rest = new ArrayList<>();
        for (AppItem a : all) {
            if (!dock.contains(a)) {
                rest.add(a);
            }
        }

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(sidePad, dp(40), sidePad, dp(60));
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ----- Header: greeting, clock, date -----
        greeting = new TextView(this);
        greeting.setTextColor(Color.parseColor("#8E8E93"));
        greeting.setTextSize(22);
        greeting.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        content.addView(greeting);

        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextColor(Color.WHITE);
        time.setTextSize(84);
        time.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        content.addView(time);

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, MMMM d");
        date.setFormat24Hour("EEEE, MMMM d");
        date.setTextColor(Color.parseColor("#8E8E93"));
        date.setTextSize(22);
        date.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.bottomMargin = dp(36);
        content.addView(date, dlp);

        // ----- Dock band -----
        LinearLayout band = new LinearLayout(this);
        band.setOrientation(LinearLayout.HORIZONTAL);
        band.setPadding(bandPad, bandPad, bandPad, bandPad);
        band.setBackground(rounded(Color.parseColor("#1A1A1C"), dp(28)));
        band.setClipChildren(false);
        band.setClipToPadding(false);
        for (int i = 0; i < dock.size(); i++) {
            View t = makeTile(dock.get(i), dockTileW, true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dockTileW, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i < dock.size() - 1) {
                lp.rightMargin = gap;
            }
            band.addView(t, lp);
        }
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.bottomMargin = dp(32);
        content.addView(band, blp);

        // ----- Grid with the rest of the apps -----
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(COLUMNS);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        for (int i = 0; i < rest.size(); i++) {
            View t = makeTile(rest.get(i), gridTileW, false);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = gridTileW;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            int right = (i % COLUMNS == COLUMNS - 1) ? 0 : gap;
            lp.setMargins(0, 0, right, dp(16));
            grid.addView(t, lp);
        }
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scroll);
        updateGreeting();

        View first = null;
        if (band.getChildCount() > 0) {
            first = band.getChildAt(0);
        } else if (grid.getChildCount() > 0) {
            first = grid.getChildAt(0);
        }
        if (first != null) {
            first.requestFocus();
        }
    }

    private View makeTile(final AppItem a, int w, final boolean isDock) {
        int h = w * 9 / 16;

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);

        final FrameLayout card = new FrameLayout(this);
        card.setBackground(rounded(Color.parseColor("#1C1C1E"), dp(16)));
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(16));
            }
        });

        ImageView iv = new ImageView(this);
        if (a.banner != null) {
            iv.setImageDrawable(a.banner);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            iv.setImageDrawable(a.icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(dp(28), dp(20), dp(28), dp(20));
        }
        card.addView(iv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final TextView label = new TextView(this);
        label.setText(a.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(16);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setVisibility(View.INVISIBLE);

        final GradientDrawable ring = new GradientDrawable();
        ring.setColor(Color.TRANSPARENT);
        ring.setCornerRadius(dp(16));
        ring.setStroke(dp(3), Color.WHITE);

        card.setFocusable(true);
        card.setClickable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(a);
            }
        });
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                float s = hasFocus ? 1.08f : 1f;
                v.animate().scaleX(s).scaleY(s).setDuration(120).start();
                card.setForeground(hasFocus ? ring : null);
                label.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
                if (hasFocus && isDock && scroll != null) {
                    scroll.smoothScrollTo(0, 0);
                }
            }
        });

        col.addView(card, new LinearLayout.LayoutParams(w, h));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(w, dp(36));
        llp.topMargin = dp(10);
        col.addView(label, llp);
        return col;
    }

    private void launch(AppItem a) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName(a.pkg, a.cls));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            // App could not be opened; ignore.
        }
    }

    private void updateGreeting() {
        if (greeting == null) {
            return;
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = hour < 12 ? "Good morning" : (hour < 18 ? "Good afternoon" : "Good evening");
        greeting.setText(g);
    }
}
