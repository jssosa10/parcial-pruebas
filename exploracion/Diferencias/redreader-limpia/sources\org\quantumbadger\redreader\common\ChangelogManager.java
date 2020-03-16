package org.quantumbadger.redreader.common;

import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.quantumbadger.redreader.R;

public class ChangelogManager {
    public static void generateViews(AppCompatActivity context, LinearLayout items, boolean showAll) {
        RRThemeAttributes attr = new RRThemeAttributes(context);
        int outerPaddingPx = General.dpToPixels(context, 12.0f);
        items.setPadding(outerPaddingPx, 0, outerPaddingPx, outerPaddingPx);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("changelog.txt")), 131072);
            String curVersionName = null;
            int itemsToShow = 10;
            while (true) {
                String readLine = br.readLine();
                String line = readLine;
                if (readLine == null) {
                    return;
                }
                if (line.length() == 0) {
                    curVersionName = null;
                    if (!showAll) {
                        itemsToShow--;
                        if (itemsToShow <= 0) {
                            return;
                        }
                    } else {
                        continue;
                    }
                } else if (curVersionName == null) {
                    curVersionName = line.split("/")[1];
                    TextView header = (TextView) LayoutInflater.from(context).inflate(R.layout.list_sectionheader, items, false);
                    header.setText(curVersionName);
                    header.setTextColor(attr.colorAccent);
                    items.addView(header);
                } else {
                    LinearLayout bulletItem = new LinearLayout(context);
                    int paddingPx = General.dpToPixels(context, 6.0f);
                    bulletItem.setPadding(paddingPx, paddingPx, paddingPx, 0);
                    TextView bullet = new TextView(context);
                    bullet.setText("•  ");
                    bulletItem.addView(bullet);
                    TextView text = new TextView(context);
                    text.setText(line);
                    bulletItem.addView(text);
                    items.addView(bulletItem);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
