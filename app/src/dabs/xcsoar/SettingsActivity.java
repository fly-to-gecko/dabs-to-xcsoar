package dabs.xcsoar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/** Einstellungen: Menü mit Unterseiten (Hinweise, XCSoar-Versionen, Dateizugriff, Sprache, Copyright). */
public class SettingsActivity extends Activity {
    private Ui ui;
    private LinearLayout body;
    private String page; // null = Menü, sonst "help", "installs", "storage", "lang", "about"
    private String langAtStart;

    static String title(String p) {
        switch (p) {
            case "help": return L.s("p_help");
            case "installs": return L.s("p_installs");
            case "storage": return L.s("p_storage");
            case "lang": return L.s("p_lang");
            default: return L.s("p_about");
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        Ui.theme(this);
        super.onCreate(b);
        ui = new Ui(this);
        page = getIntent().getStringExtra("page");
        langAtStart = L.pref(this);
        LinearLayout col = ui.page();
        ui.backHeader(col, page == null ? L.s("settings") : title(page));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        col.addView(body);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sprache wurde auf einer Unterseite geändert -> Menü in neuer Sprache neu aufbauen
        if (!langAtStart.equals(L.pref(this))) { recreate(); return; }
        build();
    }

    private void build() {
        body.removeAllViews();
        if (page == null) { menu(); return; }
        switch (page) {
            case "help": help(); break;
            case "installs": installs(); break;
            case "storage": storage(); break;
            case "lang": language(); break;
            default: about();
        }
    }

    /** Menü: große, gut treffbare Einträge. */
    private void menu() {
        int n = 0;
        for (Updater.Install i : Updater.installs()) if (i.exists() && Updater.installEnabled(this, i)) n++;
        boolean ok = Updater.hasStorageAccess(this);
        body.addView(entry("help", L.s("m_help"), false));
        body.addView(entry("installs", L.s(n == 1 ? "m_installs1" : "m_installs", n), false));
        body.addView(entry("storage", L.s(ok ? "m_storage_ok" : "m_storage_missing"), !ok));
        body.addView(entry("lang", langName(L.pref(this)), false));
        body.addView(entry("about", L.s("m_about", versionName()), false));

        // Update erzwingen: großer Eintrag, startet sofort und kehrt zur Startseite zurück
        LinearLayout f = ui.card(false);
        f.setMinimumHeight(ui.dp(72));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(ui.text("⟳  " + L.s("p_force"), 18, ui.accent, true));
        t.addView(ui.text(L.s("m_force"), 14, ui.sub, false));
        TextView fn = ui.text(L.s("force_note"), 12, ui.sub, false);
        fn.setPadding(0, ui.dp(4), 0, 0);
        t.addView(fn);
        f.addView(t, Ui.weight1());
        f.setOnClickListener(v -> {
            if (!Updater.hasStorageAccess(this)) {
                android.widget.Toast.makeText(this, L.s("toast_storage_first"), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Updater.updateAll(this, true);
            android.widget.Toast.makeText(this, L.s("force_started"), android.widget.Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        });
        body.addView(f);
    }

    private LinearLayout entry(String p, String subtitle, boolean warn) {
        LinearLayout c = ui.card(false);
        c.setMinimumHeight(ui.dp(72));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(ui.text(title(p), 18, ui.text, true));
        t.addView(ui.text(subtitle, 14, warn ? Ui.RED : ui.sub, false));
        c.addView(t, Ui.weight1());
        c.addView(ui.text("›", 28, ui.sub, false));
        c.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class).putExtra("page", p)));
        return c;
    }

    private void help() {
        LinearLayout h = ui.card(true);
        h.addView(html(L.s("help_html")));
        body.addView(h);
        body.addView(ui.section(L.s("sec_sources")));
        LinearLayout src = ui.card(true);
        src.addView(html(L.s("sources_html")));
        body.addView(src);
    }

    private void installs() {
        LinearLayout v = ui.card(true);
        v.addView(ui.text(L.s("installs_q"), 13, ui.sub, false));
        for (Updater.Install i : Updater.installs()) {
            String ver = Updater.version(this, i);
            boolean exists = i.exists();
            CheckBox cb = new CheckBox(this);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));
            cb.setText(i.label + (ver != null ? " " + ver : ""));
            cb.setTextColor(exists ? ui.text : ui.sub);
            cb.setTextSize(16);
            cb.setMinHeight(ui.dp(48));
            cb.setChecked(exists && Updater.installEnabled(this, i));
            cb.setEnabled(exists);
            cb.setOnCheckedChangeListener((x, on) -> {
                Updater.setInstallEnabled(this, i, on);
                if (!on) Updater.removeAllFiles(i);
                MainActivity.dirty = true;
            });
            v.addView(cb);
            TextView p = ui.text(exists ? i.target().getPath() : L.s("not_found"), 12, ui.sub, false);
            p.setPadding(ui.dp(32), 0, 0, ui.dp(4));
            v.addView(p);
        }
        body.addView(v);
        TextView note = ui.text(L.s("installs_note"), 13, ui.sub, false);
        note.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), 0);
        body.addView(note);
    }

    private void storage() {
        LinearLayout f = ui.card(true);
        boolean ok = Updater.hasStorageAccess(this);
        f.addView(ui.text(L.s(ok ? "storage_ok" : "storage_no"), 15, ok ? Ui.GREEN : Ui.RED, true));
        if (!ok) {
            Button g = ui.button(L.s("storage_grant"), true);
            g.setOnClickListener(x -> requestStorage());
            f.addView(g);
        }
        TextView why = ui.text(L.s("storage_why"), 13, ui.sub, false);
        why.setPadding(0, ui.dp(8), 0, 0);
        f.addView(why);
        body.addView(f);
    }

    /** Sprachauswahl: Automatisch, Deutsch, English, Français. */
    private void language() {
        LinearLayout c = ui.card(true);
        RadioGroup g = new RadioGroup(this);
        String cur = L.pref(this);
        for (String code : L.CODES) {
            RadioButton r = new RadioButton(this);
            r.setId(android.view.View.generateViewId());
            r.setText(langName(code));
            r.setTextSize(17);
            r.setTextColor(ui.text);
            r.setMinHeight(ui.dp(56));
            r.setButtonTintList(android.content.res.ColorStateList.valueOf(ui.accent));
            g.addView(r);
            if (code.equals(cur)) r.setChecked(true);
            r.setOnCheckedChangeListener((x, on) -> {
                if (!on || code.equals(L.pref(this))) return;
                L.setPref(this, code);
                L.init(this);
                MainActivity.languageChanged = true;
                recreate(); // Seite sofort in der neuen Sprache
            });
        }
        c.addView(g);
        body.addView(c);
    }

    static String langName(String code) {
        switch (code) {
            case "de": return "Deutsch";
            case "en": return "English";
            case "fr": return "Français";
            default: return L.s("lang_auto");
        }
    }

    private void about() {
        LinearLayout c = ui.card(true);
        c.addView(html(L.s("about_html", versionName())));
        body.addView(c);
    }

    private TextView html(String s) {
        TextView t = ui.text(Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY), 14, ui.text, false);
        t.setLinkTextColor(ui.accent);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        MainActivity.dirty = true;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
