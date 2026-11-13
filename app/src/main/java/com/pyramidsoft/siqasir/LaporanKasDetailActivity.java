package com.pyramidsoft.siqasir;

import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pyramidsoft.siqasir.Command.PrinterCommand;
import com.pyramidsoft.siqasir.adapter.ListKasKeluarAdapter;
import com.pyramidsoft.siqasir.databinding.ActivityLaporanKasDetailBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.KasKeluarModel;
import com.pyramidsoft.siqasir.printer.service.PerintahPrinter;
import com.pyramidsoft.siqasir.ui.laporan.fragment.PenjualanFragment;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;
import com.zj.btsdk.BluetoothService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LaporanKasDetailActivity extends AppCompatActivity {

    private ActivityLaporanKasDetailBinding binding;
    SQLiteDatabase mDatabase;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    String kode_rekapkas;
    ListKasKeluarAdapter kasKeluarAdapter;

    private BluetoothService mBluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLaporanKasDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mBluetoothService = PenjualanFragment.mBluetoothService;

        pDialog = new ProgressDialog(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DataBaseHelper(this);
        mDatabase = openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        ImageView ivClose = findViewById(R.id.IvClose);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.btnDownloadPdf.setOnClickListener(v -> {previewPdf();});
        binding.btnPrintThermal.setOnClickListener(v -> {printLaporanKasThermal();});
        binding.btnDownloadCsv.setOnClickListener(v -> {
            showCsvDownloadDialog();
        });

        if (getIntent().getStringExtra("kode_rekapkas") != null) {
            kode_rekapkas = getIntent().getStringExtra("kode_rekapkas");
            getDetailLaporanKas(kode_rekapkas);
            listKasKeluar(kode_rekapkas);
        }
    }

    private void getDetailLaporanKas(String kode_rekapkas) {
        pDialog.show();

        Cursor cursor = dbHelper.detailLaporanKas(kode_rekapkas);

        if (cursor.moveToFirst()) {

            String waktu_mulai_full = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.date_awal));
            String waktu_tutup_full = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.date_tutup));

            String waktu_mulai = waktu_mulai_full.split(",").length > 1
                    ? waktu_mulai_full.split(",")[1].trim()
                    : waktu_mulai_full;

            String waktu_tutup = waktu_tutup_full.split(",").length > 1
                    ? waktu_tutup_full.split(",")[1].trim()
                    : waktu_tutup_full;

            String total_penjualan = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.total_penjualan));
            String modal_awal = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.total_modalawal));
            String kas_keluar = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.total_kaskeluar));
            String penerimaan_sistem = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.penerimaan_sistem));
            String penerimaan_tunai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.penerimaan_tunai));
            String penerimaan_nontunai = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.penerimaan_nontunai));
            String selisih = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseHelper.selisih));

            binding.tvWaktuMulai.setText(waktu_mulai);
            binding.tvWaktuTutup.setText(waktu_tutup);
            binding.tvPemasukanKasir.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(total_penjualan));
            binding.tvKasAwal.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(modal_awal));
            binding.tvPengeluaranKasir.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(kas_keluar));
            binding.tvPenerimaanSistem.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(penerimaan_sistem));
            binding.tvTunai.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(penerimaan_tunai));
            binding.tvNonTunai.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(penerimaan_nontunai));
            binding.tvSelisih.setText("Rp"+ DecimalsFormat.priceWithoutDecimal(selisih));

            pDialog.dismiss();
        }
    }

    public void listKasKeluar(String kode_rekapkas) {

        Cursor cursorproduct = dbHelper.listKasKeluar(kode_rekapkas);

        ArrayList<KasKeluarModel> data = new ArrayList<>();

        if (cursorproduct.moveToFirst()) {
            do {
                data.add(new KasKeluarModel(
                        cursorproduct.getString(0),
                        cursorproduct.getString(1)
                ));
            } while (cursorproduct.moveToNext());
        }
        if (data.isEmpty()) {
        }

        cursorproduct.close();

        kasKeluarAdapter = new ListKasKeluarAdapter(data, this, mDatabase);

        binding.rvKasKeluar.setAdapter(kasKeluarAdapter);

    }

    private void SendDataByte(byte[] data) {
        if (mBluetoothService == null || mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(this, "Printer belum terhubung");
            return;
        }
        mBluetoothService.write(data);
    }

    private void SendDataString(String data) {
        if (mBluetoothService == null || mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(this, "Printer belum terhubung");
            return;
        }
        try {
            mBluetoothService.write(data.getBytes("GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void printLaporanKasThermal() {

        SendDataByte(PrinterCommand.POS_Set_PrtInit());

        if (mBluetoothService == null ||
                mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {

            Toast.makeText(this,
                    "Printer belum siap, tunggu sebentar...",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final String LINE = "--------------------------------\n";

        // ===== HEADER =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataByte(PrinterCommand.POS_Set_Bold(1));

        SendDataString("SIQASIR\n");
        SendDataString("LAPORAN KAS\n");

        SendDataByte(PrinterCommand.POS_Set_Bold(0));
        SendDataString(LINE);

        // ===== INFO =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);

        SendDataString("Mulai : " + binding.tvWaktuMulai.getText() + "\n");
        SendDataString("Tutup : " + binding.tvWaktuTutup.getText() + "\n");

        SendDataString(LINE);

        // ===== DATA UTAMA =====
        SendDataString(leftRightAlign("Penjualan",
                binding.tvPemasukanKasir.getText().toString()));

        SendDataString(leftRightAlign("Modal Awal",
                binding.tvKasAwal.getText().toString()));

        SendDataString(leftRightAlign("Kas Keluar",
                binding.tvPengeluaranKasir.getText().toString()));

        SendDataString(LINE);

        // ===== RINCIAN KAS KELUAR =====
        if (kasKeluarAdapter != null
                && kasKeluarAdapter.getData() != null
                && !kasKeluarAdapter.getData().isEmpty()) {

            SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
            SendDataByte(PrinterCommand.POS_Set_Bold(1));
            SendDataString("RINCIAN KAS KELUAR\n");
            SendDataByte(PrinterCommand.POS_Set_Bold(0));

            SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);

            for (KasKeluarModel item : kasKeluarAdapter.getData()) {

                String nominal = "Rp " +
                        DecimalsFormat.priceWithoutDecimal(item.getNominal());

                SendDataString(leftRightAlign(
                        item.getKeperluan(),
                        nominal
                ));
            }

            SendDataString(LINE);
        }

        // ===== PENERIMAAN =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataByte(PrinterCommand.POS_Set_Bold(1));
        SendDataString("PENERIMAAN\n");
        SendDataByte(PrinterCommand.POS_Set_Bold(0));

        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);

        SendDataString(leftRightAlign("Sistem",
                binding.tvPenerimaanSistem.getText().toString()));

        SendDataString(leftRightAlign("Tunai",
                binding.tvTunai.getText().toString()));

        SendDataString(leftRightAlign("Non Tunai",
                binding.tvNonTunai.getText().toString()));

        SendDataString(LINE);

        // ===== SELISIH =====
        SendDataByte(PrinterCommand.POS_Set_Bold(1));
        SendDataString(leftRightAlign("SELISIH",
                binding.tvSelisih.getText().toString()));
        SendDataByte(PrinterCommand.POS_Set_Bold(0));

        SendDataString("\n");

        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataString("Dicetak: " +
                new SimpleDateFormat("dd-MM-yyyy HH:mm",
                        Locale.getDefault()).format(new Date()) + "\n");

        SendDataString("Bathiku by PyramidSoft\n\n\n");

        SendDataByte(PrinterCommand.POS_Set_Cut(1));
    }

    private String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        if (ans.length() < 32) {
            int a = str1.length() + str2.length();
            int n = (32 - a);
            ans = str1 + new String(new char[n]).replace("\0", " ") + str2 + "\n";
        }
        return ans;
    }

    private void showCsvDownloadDialog() {

        String fileName =
                "laporan_kas_" +
                        new SimpleDateFormat(
                                "ddMMyyyy_HHmmss",
                                Locale.getDefault()
                        ).format(new Date()) +
                        ".csv";

        new AlertDialog.Builder(this)
                .setTitle("Download CSV")
                .setMessage(
                        "File akan disimpan di folder Download.\n\n" +
                                "Nama file:\n" +
                                fileName +
                                "\n\nLanjutkan download?"
                )
                .setNegativeButton("Batal", null)
                .setPositiveButton(
                        "Download",
                        (dialog, which) -> saveCsvToDownloads()
                )
                .show();
    }

    private void saveCsvToDownloads() {

        try {

            String fileName =
                    "laporan_kas_" +
                            new SimpleDateFormat(
                                    "ddMMyyyy_HHmmss",
                                    Locale.getDefault()
                            ).format(new Date()) +
                            ".csv";

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
            );

            if (uri == null) {
                Toast.makeText(this,
                        "Gagal membuat file CSV",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStreamWriter writer =
                    new OutputStreamWriter(
                            getContentResolver().openOutputStream(uri)
                    );

            writer.append("LAPORAN KAS\n");
            writer.append("\n");

            writer.append("Waktu Mulai,");
            writer.append(binding.tvWaktuMulai.getText().toString());
            writer.append("\n");

            writer.append("Waktu Tutup,");
            writer.append(binding.tvWaktuTutup.getText().toString());
            writer.append("\n\n");

            writer.append("Penjualan,");
            writer.append(binding.tvPemasukanKasir.getText().toString());
            writer.append("\n");

            writer.append("Modal Awal,");
            writer.append(binding.tvKasAwal.getText().toString());
            writer.append("\n");

            writer.append("Kas Keluar,");
            writer.append(binding.tvPengeluaranKasir.getText().toString());
            writer.append("\n\n");

            if (kasKeluarAdapter != null
                    && kasKeluarAdapter.getData() != null
                    && !kasKeluarAdapter.getData().isEmpty()) {

                writer.append("RINCIAN KAS KELUAR\n");
                writer.append("Keperluan,Nominal\n");

                for (KasKeluarModel item : kasKeluarAdapter.getData()) {

                    writer.append("\"")
                            .append(item.getKeperluan())
                            .append("\",");

                    writer.append("\"Rp ")
                            .append(DecimalsFormat.priceWithoutDecimal(item.getNominal()))
                            .append("\"");

                    writer.append("\n");
                }

                writer.append("\n");
            }

            writer.append("PENERIMAAN\n");

            writer.append("Sistem,");
            writer.append(binding.tvPenerimaanSistem.getText().toString());
            writer.append("\n");

            writer.append("Tunai,");
            writer.append(binding.tvTunai.getText().toString());
            writer.append("\n");

            writer.append("Non Tunai,");
            writer.append(binding.tvNonTunai.getText().toString());
            writer.append("\n\n");

            writer.append("Selisih,");
            writer.append(binding.tvSelisih.getText().toString());
            writer.append("\n");

            writer.flush();
            writer.close();

            new AlertDialog.Builder(this)
                    .setTitle("Berhasil")
                    .setMessage(
                            "CSV berhasil disimpan di folder Download\n\n" +
                                    "Nama file:\n" +
                                    fileName
                    )
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(this,
                    "Gagal menyimpan CSV",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void previewPdf() {
        try {
            File pdfFile = new File(
                    this.getCacheDir(),
                    "preview_laporan_penjualan.pdf"
            );

            PdfDocument document = createPdfDocument();
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            View view = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_pdf_preview, null, false);

            LinearLayout pageContainer = view.findViewById(R.id.pageContainer);

            ParcelFileDescriptor pfd =
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);

            PdfRenderer renderer = new PdfRenderer(pfd);

            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 32);

                pageContainer.addView(imageView);
            }

            renderer.close();
            pfd.close();

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            view.findViewById(R.id.btnCancel)
                    .setOnClickListener(v -> dialog.dismiss());

            view.findViewById(R.id.btnDownload)
                    .setOnClickListener(v -> {
                        dialog.dismiss();
                        savePdfToDownloads();
                    });

            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePdfToDownloads() {

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH-mm-dd-MM-yyyy", Locale.getDefault());

            String tanggal = sdf.format(new Date());

            String fileName = "laporan_penjualan_owner_" + tanggal + ".pdf";

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                uri = this
                        .getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (uri != null) {
                OutputStream os = this
                        .getContentResolver()
                        .openOutputStream(uri);

                PdfDocument document = createPdfDocument();
                document.writeTo(os);
                document.close();
                os.close();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Berhasil")
                    .setMessage(
                            "PDF berhasil disimpan di folder Download\n\n" +
                                    "Nama file:\n" + fileName
                    )
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int drawRow(Canvas canvas,
                        String label,
                        String value,
                        int margin,
                        int y,
                        int pageWidth,
                        Paint labelPaint,
                        Paint valuePaint) {

        canvas.drawText(label, margin, y, labelPaint);

        float valueWidth = valuePaint.measureText(value);

        canvas.drawText(value,
                pageWidth - margin - valueWidth,
                y,
                valuePaint);

        return y + 20;
    }

    private PdfDocument createPdfDocument() {

        PdfDocument document = new PdfDocument();

        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(20);
        titlePaint.setFakeBoldText(true);

        Paint sectionPaint = new Paint();
        sectionPaint.setTextSize(14);
        sectionPaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setTextSize(12);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(12);
        boldPaint.setFakeBoldText(true);

        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1);

        int margin = 50;
        int y = 70;

        // ===== HEADER =====
        String title = "SIQASIR";
        float titleWidth = titlePaint.measureText(title);
        canvas.drawText(title, (pageWidth - titleWidth) / 2, y, titlePaint);
        y += 25;

        String subtitle = "LAPORAN KAS";
        float subWidth = sectionPaint.measureText(subtitle);
        canvas.drawText(subtitle, (pageWidth - subWidth) / 2, y, sectionPaint);
        y += 25;

        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 30;

        // ===== INFO =====
        canvas.drawText("Waktu Mulai", margin, y, textPaint);
        canvas.drawText(binding.tvWaktuMulai.getText().toString(),
                pageWidth - margin - boldPaint.measureText(binding.tvWaktuMulai.getText().toString()),
                y, boldPaint);
        y += 20;

        canvas.drawText("Waktu Tutup", margin, y, textPaint);
        canvas.drawText(binding.tvWaktuTutup.getText().toString(),
                pageWidth - margin - boldPaint.measureText(binding.tvWaktuTutup.getText().toString()),
                y, boldPaint);
        y += 30;

        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 30;

        // ===== DATA UTAMA =====
        y = drawRow(canvas, "Penjualan",
                binding.tvPemasukanKasir.getText().toString(),
                margin, y, pageWidth, textPaint, boldPaint);

        y = drawRow(canvas, "Modal Awal",
                binding.tvKasAwal.getText().toString(),
                margin, y, pageWidth, textPaint, boldPaint);

        y = drawRow(canvas, "Kas Keluar",
                binding.tvPengeluaranKasir.getText().toString(),
                margin, y, pageWidth, textPaint, boldPaint);

        y += 20;

        // ===== PENERIMAAN =====
        canvas.drawText("PENERIMAAN", margin, y, sectionPaint);
        y += 20;

        y = drawRow(canvas, "Sistem",
                binding.tvPenerimaanSistem.getText().toString(),
                margin, y, pageWidth, textPaint, textPaint);

        y = drawRow(canvas, "Tunai",
                binding.tvTunai.getText().toString(),
                margin, y, pageWidth, textPaint, textPaint);

        y = drawRow(canvas, "Non Tunai",
                binding.tvNonTunai.getText().toString(),
                margin, y, pageWidth, textPaint, textPaint);

        y += 20;

        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 25;

        // ===== SELISIH =====
        y += 5;

        int boxTop = y - 20;
        int boxBottom = y + 15;

        linePaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(margin, boxTop, pageWidth - margin, boxBottom, linePaint);

        y = drawRow(canvas, "SELISIH",
                binding.tvSelisih.getText().toString(),
                margin + 15,
                y,
                pageWidth - 15,
                boldPaint,
                boldPaint);

        y += 30;

        canvas.drawText("Dicetak: " +
                        new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                                .format(new Date()),
                margin, y, textPaint);

        // Branding kanan bawah
        canvas.drawText("Bathiku by PyramidSoft",
                pageWidth - margin - 160,
                pageHeight - 40,
                textPaint);

        document.finishPage(page);
        return document;
    }
}
