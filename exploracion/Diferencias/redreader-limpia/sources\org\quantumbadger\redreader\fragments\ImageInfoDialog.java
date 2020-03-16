package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.image.ImageInfo;

public final class ImageInfoDialog extends PropertiesDialog {
    public static ImageInfoDialog newInstance(ImageInfo info2) {
        ImageInfoDialog pp = new ImageInfoDialog();
        Bundle args = new Bundle();
        args.putParcelable("info", info2);
        pp.setArguments(args);
        return pp;
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return context.getString(R.string.props_image_title);
    }

    /* access modifiers changed from: protected */
    public void prepare(AppCompatActivity context, LinearLayout items) {
        ImageInfo info2 = (ImageInfo) getArguments().getParcelable("info");
        boolean first = true;
        if (info2.title != null && info2.title.trim().length() > 0) {
            items.addView(propView((Context) context, (int) R.string.props_title, (CharSequence) info2.title.trim(), true));
            first = false;
        }
        if (info2.caption != null && info2.caption.trim().length() > 0) {
            items.addView(propView((Context) context, (int) R.string.props_caption, (CharSequence) info2.caption.trim(), first));
            first = false;
        }
        items.addView(propView((Context) context, (int) R.string.props_url, (CharSequence) info2.urlOriginal, first));
        if (info2.width != null && info2.height != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(info2.width);
            sb.append(" x ");
            sb.append(info2.height);
            items.addView(propView((Context) context, (int) R.string.props_resolution, (CharSequence) sb.toString(), false));
        }
    }
}
