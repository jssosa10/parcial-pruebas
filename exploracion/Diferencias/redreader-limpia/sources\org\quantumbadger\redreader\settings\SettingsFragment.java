package org.quantumbadger.redreader.settings;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.R.xml;
import org.quantumbadger.redreader.activities.ChangelogActivity;
import org.quantumbadger.redreader.activities.HtmlViewActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.TorCommon;

public final class SettingsFragment extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        String panel = getArguments().getString("panel");
        Class<xml> cls = xml.class;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("prefs_");
            sb.append(panel);
            addPreferencesFromResource(cls.getDeclaredField(sb.toString()).getInt(null));
            int[] editTextPrefsToUpdate = {R.string.pref_behaviour_comment_min_key};
            for (int pref : new int[]{R.string.pref_appearance_twopane_key, R.string.pref_behaviour_self_post_tap_actions_key, R.string.pref_behaviour_fling_post_left_key, R.string.pref_behaviour_fling_post_right_key, R.string.pref_behaviour_fling_comment_left_key, R.string.pref_behaviour_fling_comment_right_key, R.string.pref_appearance_theme_key, R.string.pref_appearance_navbar_color_key, R.string.pref_cache_maxage_listing_key, R.string.pref_cache_maxage_thumb_key, R.string.pref_cache_maxage_image_key, R.string.pref_cache_maxage_entry_key, R.string.pref_appearance_fontscale_posts_key, R.string.pref_appearance_fontscale_comments_key, R.string.pref_appearance_fontscale_inbox_key, R.string.pref_behaviour_actions_comment_tap_key, R.string.pref_behaviour_actions_comment_longclick_key, R.string.pref_behaviour_commentsort_key, R.string.pref_behaviour_postsort_key, R.string.pref_appearance_langforce_key, R.string.pref_behaviour_postcount_key, R.string.pref_behaviour_bezel_toolbar_swipezone_key, R.string.pref_behaviour_imageview_mode_key, R.string.pref_behaviour_albumview_mode_key, R.string.pref_behaviour_gifview_mode_key, R.string.pref_behaviour_videoview_mode_key, R.string.pref_behaviour_screenorientation_key, R.string.pref_behaviour_gallery_swipe_length_key, R.string.pref_behaviour_pinned_subredditsort_key, R.string.pref_behaviour_blocked_subredditsort_key, R.string.pref_cache_rerequest_postlist_age_key}) {
                final ListPreference listPreference = (ListPreference) findPreference(getString(pref));
                if (listPreference != null) {
                    int index = listPreference.findIndexOfValue(listPreference.getValue());
                    if (index >= 0) {
                        listPreference.setSummary(listPreference.getEntries()[index]);
                        listPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                            public boolean onPreferenceChange(Preference preference, Object newValue) {
                                int index = listPreference.findIndexOfValue((String) newValue);
                                ListPreference listPreference = listPreference;
                                listPreference.setSummary(listPreference.getEntries()[index]);
                                return true;
                            }
                        });
                    }
                }
            }
            for (int pref2 : editTextPrefsToUpdate) {
                final EditTextPreference editTextPreference = (EditTextPreference) findPreference(getString(pref2));
                if (editTextPreference != null) {
                    editTextPreference.setSummary(editTextPreference.getText());
                    editTextPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (newValue != null) {
                                editTextPreference.setSummary(newValue.toString());
                            } else {
                                editTextPreference.setSummary("(null)");
                            }
                            return true;
                        }
                    });
                }
            }
            Preference versionPref = findPreference(getString(R.string.pref_about_version_key));
            Preference changelogPref = findPreference(getString(R.string.pref_about_changelog_key));
            Preference torPref = findPreference(getString(R.string.pref_network_tor_key));
            Preference licensePref = findPreference(getString(R.string.pref_about_license_key));
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (versionPref != null) {
                    versionPref.setSummary(pInfo.versionName);
                }
                if (changelogPref != null) {
                    changelogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            context.startActivity(new Intent(context, ChangelogActivity.class));
                            return true;
                        }
                    });
                }
                if (licensePref != null) {
                    licensePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            HtmlViewActivity.showAsset(context, "license.html");
                            return true;
                        }
                    });
                }
                if (torPref != null) {
                    torPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, final Object newValue) {
                            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {
                                    TorCommon.updateTorStatus(context);
                                    if (TorCommon.isTorEnabled() != Boolean.TRUE.equals(newValue)) {
                                        throw new RuntimeException("Tor not correctly enabled after preference change");
                                    }
                                }
                            });
                            return true;
                        }
                    });
                }
                if (VERSION.SDK_INT < 21) {
                    Preference pref3 = findPreference(getString(R.string.pref_appearance_navbar_color_key));
                    if (pref3 != null) {
                        pref3.setEnabled(false);
                        pref3.setSummary(R.string.pref_not_supported_before_lollipop);
                    }
                }
                Preference cacheLocationPref = findPreference(getString(R.string.pref_cache_location_key));
                if (cacheLocationPref != null) {
                    cacheLocationPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            SettingsFragment.this.showChooseStorageLocationDialog();
                            return true;
                        }
                    });
                    updateStorageLocationText(PrefsUtility.pref_cache_location(context, PreferenceManager.getDefaultSharedPreferences(context)));
                }
            } catch (NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e2) {
            throw new RuntimeException(e2);
        } catch (NoSuchFieldException e3) {
            throw new RuntimeException(e3);
        }
    }

    /* access modifiers changed from: private */
    public void showChooseStorageLocationDialog() {
        String currentStorage;
        final Context context = getActivity();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentStorage2 = PrefsUtility.pref_cache_location(context, prefs);
        List<File> checkPaths = CacheManager.getCacheDirs(context);
        final List<File> folders = new ArrayList<>(checkPaths.size());
        List<CharSequence> choices = new ArrayList<>(checkPaths.size());
        int selectedIndex = 0;
        int i = 0;
        while (i < checkPaths.size()) {
            File dir = (File) checkPaths.get(i);
            if (dir == null || !dir.exists() || !dir.canRead()) {
                currentStorage = currentStorage2;
            } else if (!dir.canWrite()) {
                currentStorage = currentStorage2;
            } else {
                folders.add(dir);
                if (currentStorage2.equals(dir.getAbsolutePath())) {
                    selectedIndex = i;
                }
                String path = dir.getAbsolutePath();
                String freeSpace = General.addUnits(General.getFreeSpaceAvailable(path));
                if (!path.endsWith("/")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(path);
                    sb.append("/");
                    path = sb.toString();
                }
                String appCachePostfix = "org.quantumbadger.redreader/cache/";
                StringBuilder sb2 = new StringBuilder();
                currentStorage = currentStorage2;
                sb2.append("Android/data/");
                sb2.append(appCachePostfix);
                if (path.endsWith(sb2.toString())) {
                    path = path.substring(0, (path.length() - appCachePostfix.length()) - 14);
                } else if (path.endsWith(appCachePostfix)) {
                    path = path.substring(0, (path.length() - appCachePostfix.length()) - 1);
                }
                StringBuilder sb3 = new StringBuilder();
                sb3.append("<small>");
                sb3.append(path);
                sb3.append(" [");
                sb3.append(freeSpace);
                sb3.append("]</small>");
                choices.add(Html.fromHtml(sb3.toString()));
            }
            i++;
            currentStorage2 = currentStorage;
        }
        new Builder(context).setTitle(R.string.pref_cache_location_title).setSingleChoiceItems((CharSequence[]) choices.toArray(new CharSequence[choices.size()]), selectedIndex, new OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
                String path = ((File) folders.get(i)).getAbsolutePath();
                PrefsUtility.pref_cache_location(context, prefs, path);
                SettingsFragment.this.updateStorageLocationText(path);
            }
        }).setNegativeButton(R.string.dialog_close, new OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        }).create().show();
    }

    /* access modifiers changed from: private */
    public void updateStorageLocationText(String path) {
        findPreference(getString(R.string.pref_cache_location_key)).setSummary(path);
    }
}
