package dabs.xcsoar;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Gibt das zwischengespeicherte Original-DABS-PDF lesend an einen PDF-Betrachter weiter. */
public class PdfProvider extends ContentProvider {
    static final String AUTHORITY = "dabs.xcsoar.pdf";

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) { return "application/pdf"; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = Updater.pdfFile(getContext());
        if (!f.isFile()) throw new FileNotFoundException("Noch kein DABS geladen");
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String sel, String[] args, String sort) {
        File f = Updater.pdfFile(getContext());
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{"DABS.pdf", f.length()});
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
