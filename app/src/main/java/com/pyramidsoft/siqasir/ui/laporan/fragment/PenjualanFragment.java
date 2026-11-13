package com.pyramidsoft.siqasir.ui.laporan.fragment;

import static android.content.Context.MODE_PRIVATE;
import static com.pyramidsoft.siqasir.db.DataBaseHelper.database_name;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pyramidsoft.siqasir.Command.PrinterCommand;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.adapter.MetodePembayaranAdapter;
import com.pyramidsoft.siqasir.adapter.ProdukTerlarisAdapter;
import com.pyramidsoft.siqasir.databinding.FragmentPenjualanBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.model.MetodePembayaranModels;
import com.pyramidsoft.siqasir.model.ProdukTerlarisModels;
import com.pyramidsoft.siqasir.printer.service.PerintahPrinter;
import com.pyramidsoft.siqasir.ui.laporan.LaporanFragment;
import com.pyramidsoft.siqasir.utils.DecimalsFormat;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.ProgressDialog;
import com.pyramidsoft.siqasir.utils.SessionManager;
import com.zj.btsdk.BluetoothService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PenjualanFragment extends Fragment {

    private FragmentPenjualanBinding binding;
    String sl1, sl2;
    SessionManager sessionManager;
    ProgressDialog pDialog;
    DataBaseHelper dbHelper;
    SQLiteDatabase mDatabase;

    ProdukTerlarisAdapter produkTerlarisAdapter;
    MetodePembayaranAdapter pembayaranAdapter;
    Activity activity;
    String tanggal1, tanggal2;

    public static BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter = null;

    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;
    private ArrayList<MetodePembayaranModels> metodePembayaranList = new ArrayList<>();
    private boolean hasAutoConnected = false;

    private boolean isDownloadMenuOpen = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPenjualanBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth tidak tersedia", Toast.LENGTH_LONG).show();
        } else {
            mBluetoothService = new BluetoothService(requireContext(), new Handler(msg -> {
                switch (msg.what) {
                    case BluetoothService.MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                GlobalToast.ShowToast(requireContext(), "Printer terhubung");
                                break;
                        }
                        break;
                }
                return true;
            }));
        }

        activity = getActivity();
        dateFormatter = new SimpleDateFormat("dd-MM-yy", new Locale("id", "ID"));

        pDialog = new ProgressDialog(activity);
        sessionManager = new SessionManager(activity);
        dbHelper = new DataBaseHelper(activity);
        mDatabase = activity.openOrCreateDatabase(database_name, MODE_PRIVATE, null);

        binding.rvMetodePembayaran.setHasFixedSize(true);
        binding.rvProdukTerlaris.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(activity, 1,
                GridLayoutManager.VERTICAL, false);
        GridLayoutManager layoutManager2 = new GridLayoutManager(activity, 1,
                GridLayoutManager.VERTICAL, false);
        binding.rvProdukTerlaris.setLayoutManager(layoutManager2);
        binding.rvMetodePembayaran.setLayoutManager(layoutManager);

        sl1 = "invisible";
        sl2 = "invisible";

        binding.tvTgl1.setOnClickListener(this::onViewClicked);
        binding.tvTgl2.setOnClickListener(this::onViewClicked);
        binding.tvMetodePembayaran.setOnClickListener(this::onViewClicked);
        binding.tvProdukTerlaris.setOnClickListener(this::onViewClicked);
        binding.fabDownload.setOnClickListener(v -> {
            toggleDownloadMenu();
        });
        binding.fabDownloadPdf.setOnClickListener(v -> {

            previewPdf();

            binding.fabDownloadPdf.setVisibility(View.GONE);
            binding.fabDownloadExcel.setVisibility(View.GONE);
            isDownloadMenuOpen = false;
        });
        binding.fabDownloadExcel.setOnClickListener(v -> {

            showExcelDownloadDialog();

            binding.fabDownloadPdf.setVisibility(View.GONE);
            binding.fabDownloadExcel.setVisibility(View.GONE);
            isDownloadMenuOpen = false;
        });
        binding.fabPrintThermal.setOnClickListener(v -> {printLaporanPenjualanThermal();});

        setMode();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasAutoConnected) {
            autoReconnectPrinter();
            hasAutoConnected = true;
        }
    }

    private void toggleDownloadMenu() {

        if (isDownloadMenuOpen) {

            binding.fabDownloadPdf.setVisibility(View.GONE);
            binding.fabDownloadExcel.setVisibility(View.GONE);

        } else {

            binding.fabDownloadPdf.setVisibility(View.VISIBLE);
            binding.fabDownloadExcel.setVisibility(View.VISIBLE);
        }

        isDownloadMenuOpen = !isDownloadMenuOpen;
    }

    private void autoReconnectPrinter() {

        if (mBluetoothService == null) return;
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) return;

        if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED)
            return;

        String alamatPrinter = sessionManager.getAlamatPrinter();
        if (alamatPrinter == null || alamatPrinter.isEmpty()) return;

        try {
            BluetoothDevice device =
                    mBluetoothAdapter.getRemoteDevice(alamatPrinter);

            mBluetoothService.connect(device);

        } catch (Exception e) {
            Log.e("PRINTER_DEBUG", "ERROR CONNECT", e);
        }
    }

    public void dataMetodePembayaran(String tanggal1, String tanggal2, ArrayList<Integer> tokoIds) {

        Cursor cursorproduct = dbHelper.dataMetodePembayaranPerhari(tanggal1, tanggal2, tokoIds);

        metodePembayaranList.clear();

        if (cursorproduct != null && cursorproduct.moveToFirst()) {
            do {
                metodePembayaranList.add(new MetodePembayaranModels(
                        cursorproduct.getString(0),
                        cursorproduct.getString(1)
                ));
            } while (cursorproduct.moveToNext());
        }

        if (cursorproduct != null) cursorproduct.close();

        pembayaranAdapter = new MetodePembayaranAdapter(metodePembayaranList, activity, mDatabase);
        binding.rvMetodePembayaran.setAdapter(pembayaranAdapter);
    }

    public void dataProdukTerlaris(String tanggal1, String tanggal2, ArrayList<Integer> tokoIds) {

        Cursor cursorproduct = dbHelper.dataProdukTerlarisPerhari(tanggal1, tanggal2, tokoIds);

        ArrayList<ProdukTerlarisModels> data = new ArrayList<>();

        if (cursorproduct != null && cursorproduct.moveToFirst()) {
            do {
                data.add(new ProdukTerlarisModels(
                        cursorproduct.getString(0), // produk
                        cursorproduct.getString(1), // qty
                        cursorproduct.getString(2)  // harga
                ));
            } while (cursorproduct.moveToNext());
        }

        if (cursorproduct != null) cursorproduct.close();

        produkTerlarisAdapter = new ProdukTerlarisAdapter(data, activity, mDatabase);
        binding.rvProdukTerlaris.setAdapter(produkTerlarisAdapter);

        pDialog.dismiss();
    }

    private void setMode() {
        if (sl1.equals("invisible")) {
            binding.imgArrow.setBackgroundResource(R.drawable.arrow_down);
            binding.rvMetodePembayaran.setVisibility(View.GONE);
            sl1 = "visible";
        } else if (sl1.equals("visible")) {
            binding.imgArrow.setBackgroundResource(R.drawable.arrow_up);
            binding.rvMetodePembayaran.setVisibility(View.VISIBLE);
            sl1 = "invisible";
        }

        if (sl2.equals("invisible")) {
            binding.imgArrow2.setBackgroundResource(R.drawable.arrow_down);
            binding.rvProdukTerlaris.setVisibility(View.GONE);
            sl2 = "visible";
        } else if (sl2.equals("visible")) {
            binding.rvProdukTerlaris.setVisibility(View.VISIBLE);
            binding.imgArrow2.setBackgroundResource(R.drawable.arrow_up);
            sl2 = "invisible";
        }
    }

    private void getDetailInvoice(String tanggal1, String tanggal2) {
        pDialog.show();

        LaporanFragment parent = (LaporanFragment) getParentFragment();
        ArrayList<Integer> tokoIds = new ArrayList<>();

        if (parent != null) {
            tokoIds = parent.getSelectedTokoIds();
        }

        if (tokoIds.isEmpty()) {
            String kodePegawai = sessionManager.getKodeUsers();
            String idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
            tokoIds.add(Integer.parseInt(idToko));
        }

        Cursor cursor = dbHelper.laporanDataPenjualan(tanggal1, tanggal2, tokoIds);

        if (cursor != null && cursor.moveToFirst()) {

            String totalPenjualan = cursor.getString(
                    cursor.getColumnIndexOrThrow("total_jml_transaksi"));

            String transaksi = cursor.getString(
                    cursor.getColumnIndexOrThrow("transaksi"));

            String totalLaba = cursor.getString(
                    cursor.getColumnIndexOrThrow("total_laba"));

            binding.tvTotalPenjualan.setText(
                    totalPenjualan == null
                            ? "Rp. 0"
                            : "Rp. " + DecimalsFormat.priceWithoutDecimal(totalPenjualan)
            );

            binding.tvTotalTransaksi.setText(
                    transaksi == null ? "0" : transaksi
            );

            binding.tvTotalLaba.setText(
                    totalLaba == null
                            ? "Rp. 0"
                            : "Rp. " + DecimalsFormat.priceWithoutDecimal(totalLaba)
            );
        }

        if (cursor != null) cursor.close();

        dataMetodePembayaran(tanggal1, tanggal2, tokoIds);
        dataProdukTerlaris(tanggal1, tanggal2, tokoIds);

        pDialog.dismiss();
    }

    private void showDateDialog1() {
        Calendar newCalendar = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(activity, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);

            tanggal1 = dateFormatter.format(newDate.getTime());
            binding.tvTgl1.setText(tanggal1);

            if (tanggal2 != null && !tanggal2.isEmpty()) {
                getDetailInvoice(tanggal1, tanggal2);
            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void showDateDialog2() {
        Calendar newCalendar = Calendar.getInstance();

        datePickerDialog = new DatePickerDialog(activity, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);

            tanggal2 = dateFormatter.format(newDate.getTime());
            binding.tvTgl2.setText(tanggal2);

            getDetailInvoice(tanggal1, tanggal2);
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private String generateCsvContent() {

        StringBuilder csv = new StringBuilder();

        csv.append("SIQASIR - LAPORAN PENJUALAN\n\n");

        csv.append("Periode Awal,")
                .append(tanggal1)
                .append("\n");

        csv.append("Periode Akhir,")
                .append(tanggal2)
                .append("\n");

        csv.append("Tanggal Cetak,")
                .append(
                        new SimpleDateFormat(
                                "dd-MM-yyyy HH:mm",
                                Locale.getDefault()
                        ).format(new Date())
                )
                .append("\n\n");

        csv.append("RINGKASAN PENJUALAN\n\n");

        csv.append("Keterangan,Nilai\n");

        csv.append("Total Transaksi,")
                .append(binding.tvTotalTransaksi.getText())
                .append("\n");

        csv.append("Total Penjualan,")
                .append(binding.tvTotalPenjualan.getText())
                .append("\n");

        csv.append("Total Laba,")
                .append(binding.tvTotalLaba.getText())
                .append("\n\n");

        csv.append("RINCIAN METODE PEMBAYARAN\n\n");

        csv.append("Metode Pembayaran,Nominal\n");

        for (MetodePembayaranModels item : metodePembayaranList) {

            csv.append(item.getMetode())
                    .append(",")

                    .append("Rp. ")
                    .append(
                            DecimalsFormat.priceWithoutDecimal(
                                    item.getNominal()
                            )
                    )
                    .append("\n");
        }

        return csv.toString();
    }

    private void showExcelDownloadDialog() {

        String fileName =
                "laporan_penjualan_" +
                        new SimpleDateFormat(
                                "ddMMyyyy_HHmmss",
                                Locale.getDefault()
                        ).format(new Date()) +
                        ".csv";

        new AlertDialog.Builder(requireContext())
                .setTitle("Download Excel")
                .setMessage(
                        "File akan disimpan di folder Download.\n\n" +
                                "Nama file:\n" +
                                fileName +
                                "\n\nLanjutkan download?"
                )
                .setNegativeButton("Batal", null)
                .setPositiveButton("Download",
                        (dialog, which) -> saveCsvToDownloads())
                .show();
    }

    private void saveCsvToDownloads() {

        try {

            SimpleDateFormat sdf =
                    new SimpleDateFormat(
                            "HH-mm-dd-MM-yyyy",
                            Locale.getDefault()
                    );

            String tanggal = sdf.format(new Date());

            String fileName =
                    "laporan_penjualan_owner_" +
                            tanggal +
                            ".csv";

            ContentValues values = new ContentValues();

            values.put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    fileName
            );

            values.put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "text/csv"
            );

            values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
            );

            Uri uri = requireContext()
                    .getContentResolver()
                    .insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            values
                    );

            if (uri != null) {

                OutputStream os =
                        requireContext()
                                .getContentResolver()
                                .openOutputStream(uri);

                os.write(
                        generateCsvContent()
                                .getBytes()
                );

                os.close();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Berhasil")
                    .setMessage(
                            "File Excel berhasil disimpan di folder Download\n\n" +
                                    "Nama file:\n" +
                                    fileName
                    )
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {

            e.printStackTrace();

            new AlertDialog.Builder(requireContext())
                    .setTitle("Gagal")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void previewPdf() {
        try {
            File pdfFile = new File(
                    requireContext().getCacheDir(),
                    "preview_laporan_penjualan.pdf"
            );

            PdfDocument document = createPdfDocument();
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            View view = LayoutInflater.from(requireContext())
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

                ImageView imageView = new ImageView(requireContext());
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 32);

                pageContainer.addView(imageView);
            }

            renderer.close();
            pfd.close();

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
                uri = requireContext()
                        .getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (uri != null) {
                OutputStream os = requireContext()
                        .getContentResolver()
                        .openOutputStream(uri);

                PdfDocument document = createPdfDocument();
                document.writeTo(os);
                document.close();
                os.close();
            }

            new AlertDialog.Builder(requireContext())
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

    private PdfDocument createPdfDocument() {

        PdfDocument document = new PdfDocument();

        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(18);
        titlePaint.setFakeBoldText(true);

        Paint subtitlePaint = new Paint();
        subtitlePaint.setTextSize(14);
        subtitlePaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setTextSize(12);

        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1);

        Paint boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.FILL);
        boxPaint.setColor(Color.parseColor("#F2F2F2"));

        int margin = 40;
        int y = 60;

        // ===== TITLE CENTER OTOMATIS =====

        String title = "SIQASIR";
        float titleWidth = titlePaint.measureText(title);
        canvas.drawText(title, (pageWidth - titleWidth) / 2f, y, titlePaint);
        y += 25;

        String subtitle = "LAPORAN PENJUALAN";
        float subtitleWidth = subtitlePaint.measureText(subtitle);
        canvas.drawText(subtitle, (pageWidth - subtitleWidth) / 2f, y, subtitlePaint);
        y += 25;

        // Baris 1
        String periode1 = "Periode : " + tanggal1;
        float periode1Width = textPaint.measureText(periode1);
        canvas.drawText(periode1, (pageWidth - periode1Width) / 2f, y, textPaint);
        y += 20;

        // Baris 2
        String periode2 = "Sampai  : " + tanggal2;
        float periode2Width = textPaint.measureText(periode2);
        canvas.drawText(periode2, (pageWidth - periode2Width) / 2f, y, textPaint);
        y += 30;

        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 30;

        // ===== BOX TOTAL =====
        int boxHeight = 85; // lebih tinggi supaya muat 3 baris
        canvas.drawRect(margin, y, pageWidth - margin, y + boxHeight, boxPaint);

        int textStartY = y + 25;
        int lineSpacing = 25;

        // Total Transaksi
        canvas.drawText("Total Transaksi",
                margin + 15, textStartY, textPaint);

        canvas.drawText(binding.tvTotalTransaksi.getText().toString(),
                pageWidth - margin - 80, textStartY, subtitlePaint);

        // Total Penjualan
        canvas.drawText("Total Penjualan",
                margin + 15, textStartY + lineSpacing, textPaint);

        canvas.drawText(binding.tvTotalPenjualan.getText().toString(),
                pageWidth - margin - 150, textStartY + lineSpacing, subtitlePaint);

        // Total Laba
        canvas.drawText("Total Laba",
                margin + 15, textStartY + (lineSpacing * 2), textPaint);

        canvas.drawText(binding.tvTotalLaba.getText().toString(),
                pageWidth - margin - 150, textStartY + (lineSpacing * 2), subtitlePaint);

        y += boxHeight + 30;

        // ===== METODE PEMBAYARAN =====
        canvas.drawText("RINCIAN METODE PEMBAYARAN",
                margin, y, subtitlePaint);

        y += 20;
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 20;

        for (MetodePembayaranModels item : metodePembayaranList) {

            String metode = item.getMetode();
            String nominal = "Rp. " +
                    DecimalsFormat.priceWithoutDecimal(item.getNominal());

            canvas.drawText(metode, margin + 10, y, textPaint);
            canvas.drawText(nominal,
                    pageWidth - margin - 120, y, textPaint);

            y += 20;
        }

        y += 20;
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);

        y += 30;

        // ===== FOOTER =====
        String printedDate = "Dicetak pada: " +
                new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                        .format(new Date());

        canvas.drawText(printedDate, margin, y, textPaint);

        // Branding kanan bawah
        canvas.drawText("Bathiku by PyramidSoft",
                pageWidth - margin - 160,
                pageHeight - 40,
                textPaint);

        document.finishPage(page);
        return document;
    }

    private void SendDataByte(byte[] data) {
        if (mBluetoothService == null || mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(requireContext(), "Printer belum terhubung");
            return;
        }
        mBluetoothService.write(data);
    }

    private void SendDataString(String data) {
        if (mBluetoothService == null || mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            GlobalToast.ShowToast(requireContext(), "Printer belum terhubung");
            return;
        }
        try {
            mBluetoothService.write(data.getBytes("GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private boolean hasBluetoothPermission() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return requireContext().checkSelfPermission(
                    android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }

        return true; // android < 12 tidak perlu runtime permission
    }

    private void requestBluetoothPermission() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN
                    },
                    1001
            );
        }
    }

    private void printLaporanPenjualanThermal() {

        // ===== CEK PRINTER =====
        if (metodePembayaranList.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Data Kosong")
                    .setMessage("Tidak ada data laporan untuk dicetak.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // ===== CEK BLUETOOTH ADAPTER =====
        if (mBluetoothAdapter == null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Bluetooth Tidak Tersedia")
                    .setMessage("Perangkat ini tidak mendukung Bluetooth.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // ===== CEK BLUETOOTH AKTIF =====
        if (!mBluetoothAdapter.isEnabled()) {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Bluetooth Mati")
                    .setMessage("Bluetooth akan diaktifkan dan mencoba menghubungkan printer.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {

                        if (!hasBluetoothPermission()) {
                            requestBluetoothPermission();
                            return;
                        }

                        try {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(enableBtIntent);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(),
                                    "Permission Bluetooth ditolak",
                                    Toast.LENGTH_SHORT).show();
                        }

                        Toast.makeText(requireContext(),
                                "Mengaktifkan Bluetooth...",
                                Toast.LENGTH_SHORT).show();

                        // tunggu bluetooth aktif lalu reconnect saja
                        new Handler().postDelayed(() -> {

                            if (mBluetoothAdapter.isEnabled()) {

                                autoReconnectPrinter();

                                Toast.makeText(requireContext(),
                                        "Mencoba menghubungkan printer...",
                                        Toast.LENGTH_SHORT).show();

                            } else {

                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Gagal Mengaktifkan Bluetooth")
                                        .setMessage("Bluetooth tidak dapat diaktifkan.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }

                        }, 1000); // tunggu bluetooth aktif

                    })
                    .show();

            return;
        }

        // ===== CEK PRINTER TERSIMPAN =====
        String alamatPrinter = sessionManager.getAlamatPrinter();
        if (alamatPrinter == null || alamatPrinter.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Printer Belum Dipilih")
                    .setMessage("Silakan pilih dan hubungkan printer terlebih dahulu di menu pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // ===== CEK STATUS KONEKSI =====
        if (mBluetoothService == null ||
                mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Printer Belum Terhubung")
                    .setMessage("Printer belum siap.\n\nTunggu beberapa saat atau buka ulang halaman ini.")
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        SendDataByte(PrinterCommand.POS_Set_PrtInit());

        // ===== HEADER =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataByte(PrinterCommand.POS_Set_Bold(1));

        SendDataString("SIQASIR\n");
        SendDataString("LAPORAN PENJUALAN\n");

        SendDataByte(PrinterCommand.POS_Set_Bold(0));
        SendDataString(tanggal1 + " - " + tanggal2 + "\n");
        SendDataString("--------------------------------\n");

        // ===== TOTAL =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);

        SendDataString(leftRightAlign(
                "Total Transaksi",
                binding.tvTotalTransaksi.getText().toString()
        ));

        SendDataString(leftRightAlign(
                "Total Penjualan",
                binding.tvTotalPenjualan.getText().toString()
        ));

        SendDataString("--------------------------------\n");

        // ===== METODE PEMBAYARAN =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataByte(PrinterCommand.POS_Set_Bold(1));
        SendDataString("METODE PEMBAYARAN\n");
        SendDataByte(PrinterCommand.POS_Set_Bold(0));

        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);

        for (MetodePembayaranModels item : metodePembayaranList) {

            SendDataString(leftRightAlign(
                    item.getMetode(),
                    "Rp " + DecimalsFormat.priceWithoutDecimal(item.getNominal())
            ));
        }

        SendDataString("--------------------------------\n");

        // ===== FOOTER =====
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataString("Dicetak: " +
                new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                        .format(new Date()) + "\n");

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

    public void onViewClicked(View view) {
        int viewId = view.getId();

        if (viewId == R.id.tvTgl1) {
            showDateDialog1();
        } else if (viewId == R.id.tvTgl2) {
            showDateDialog2();
        } else if (viewId == R.id.tvMetodePembayaran) {
            if (sl1.equals("invisible")) {
                binding.imgArrow.setBackgroundResource(R.drawable.arrow_down);
                binding.rvMetodePembayaran.setVisibility(View.GONE);
                sl1 = "visible";
            } else if (sl1.equals("visible")) {
                binding.imgArrow.setBackgroundResource(R.drawable.arrow_up);
                binding.rvMetodePembayaran.setVisibility(View.VISIBLE);
                sl1 = "invisible";
            }
        } else if (viewId == R.id.tvProdukTerlaris) {
            if (sl2.equals("invisible")) {
                binding.imgArrow2.setBackgroundResource(R.drawable.arrow_down);
                binding.rvProdukTerlaris.setVisibility(View.GONE);
                sl2 = "visible";
            } else if (sl2.equals("visible")) {
                binding.rvProdukTerlaris.setVisibility(View.VISIBLE);
                binding.imgArrow2.setBackgroundResource(R.drawable.arrow_up);
                sl2 = "invisible";
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBluetoothService != null &&
                mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            mBluetoothService.stop();
        }

        binding = null;
    }
}
