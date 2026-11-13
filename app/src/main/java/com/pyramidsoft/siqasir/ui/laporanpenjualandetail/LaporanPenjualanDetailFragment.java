package com.pyramidsoft.siqasir.ui.laporanpenjualandetail;

import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pyramidsoft.siqasir.Command.PrinterCommand;
import com.pyramidsoft.siqasir.R;
import com.pyramidsoft.siqasir.databinding.FragmentLaporanPenjualanDetailBinding;
import com.pyramidsoft.siqasir.db.DataBaseHelper;
import com.pyramidsoft.siqasir.printer.service.PerintahPrinter;
import com.pyramidsoft.siqasir.utils.GlobalToast;
import com.pyramidsoft.siqasir.utils.SessionManager;
import com.zj.btsdk.BluetoothService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LaporanPenjualanDetailFragment extends Fragment {

    private FragmentLaporanPenjualanDetailBinding binding;
    private Calendar today;
    private LaporanPenjualanDetailAdapter adapter;
    private final List<LaporanPenjualanDetailItem> laporanDetailList = new ArrayList<>();
    private DataBaseHelper dbHelper;
    private SessionManager session;

    private String hakAkses;
    private String kodePegawai;
    private String idToko;

    private List<String> tokoIdList = new ArrayList<>();
    private List<String> tokoNamaList = new ArrayList<>();
    private String selectedIdToko;

    private BluetoothService mBluetoothService = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    private boolean hasAutoConnected = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLaporanPenjualanDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        dbHelper = new DataBaseHelper(requireContext());
        session = new SessionManager(requireContext());

        hakAkses = session.getHakAkses();
        kodePegawai = session.getKodeUsers();
        idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);

        idToko = dbHelper.getIdTokoByKodePegawai(kodePegawai);
        selectedIdToko = idToko;

        if ("OWNER".equalsIgnoreCase(hakAkses)) {
            binding.layoutFilterToko.setVisibility(View.VISIBLE);
            setupSpinnerToko();
        } else {
            binding.layoutFilterToko.setVisibility(View.GONE);
        }

        today = Calendar.getInstance();

        setupRecyclerView();
        setupSpinner();
        setupFilterRadio();
        setupDatePicker();

        setupSpinnerToko();

        binding.rbTahun.setChecked(true);
        setDefaultTahun();
        loadData();
        binding.btnCsv.setOnClickListener(v -> {
            showCsvDownloadDialog();
        });
        binding.btnPdf.setOnClickListener(v -> {
            showConfirmGeneratePdf();
        });
        binding.btnPrint.setOnClickListener(v -> {
            printLaporanPenjualanDetailThermal();
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!hasAutoConnected) {
            autoReconnectPrinter();   // ✅ hanya sekali
            hasAutoConnected = true;  // tandai sudah pernah reconnect
        }
    }

    private void autoReconnectPrinter() {

        if (mBluetoothService == null) return;
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) return;

        String alamatPrinter = session.getAlamatPrinter();
        if (alamatPrinter == null || alamatPrinter.isEmpty()) {
            Log.d("PRINTER_DEBUG", "Alamat kosong");
            return;
        }

        try {
            BluetoothDevice device =
                    mBluetoothAdapter.getRemoteDevice(alamatPrinter);

            mBluetoothService.connect(device);

        } catch (Exception e) {
            Log.e("PRINTER_DEBUG", "ERROR CONNECT", e);
        }
    }

    private String formatRupiah(double value) {
        Locale localeID = new Locale("id", "ID");
        java.text.NumberFormat rupiah =
                java.text.NumberFormat.getCurrencyInstance(localeID);

        rupiah.setMaximumFractionDigits(0);
        rupiah.setMinimumFractionDigits(0);

        return rupiah.format(value);
    }

    private void setupSpinnerToko() {

        tokoIdList.clear();
        tokoNamaList.clear();

        Cursor cursor = dbHelper.getListToko();

        int defaultIndex = 0;

        if (cursor != null && cursor.moveToFirst()) {
            int index = 0;
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id_toko"));
                String nama = cursor.getString(cursor.getColumnIndexOrThrow("nama_toko"));

                tokoIdList.add(id);
                tokoNamaList.add(nama);

                if (id.equals(idToko)) {
                    defaultIndex = index;
                }
                index++;

            } while (cursor.moveToNext());

            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                tokoNamaList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spToko.setAdapter(adapter);
        binding.spToko.setSelection(defaultIndex);

        binding.spToko.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedIdToko = tokoIdList.get(position);
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void setupRecyclerView() {
        adapter = new LaporanPenjualanDetailAdapter(laporanDetailList);
        binding.rvLaporanPenjualan.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLaporanPenjualan.setAdapter(adapter);
    }

    private void setupSpinner() {
        List<String> years = new ArrayList<>();
        int currentYear = today.get(Calendar.YEAR);
        for (int i = currentYear; i >= currentYear - 5; i--) {
            years.add(String.valueOf(i));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                years
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spYear.setAdapter(yearAdapter);
        binding.spYearMonth.setAdapter(yearAdapter);

        List<String> months = List.of(
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        );

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                months
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spMonth.setAdapter(monthAdapter);

        binding.spYear.setOnItemSelectedListener(simpleReloadListener);
        binding.spYearMonth.setOnItemSelectedListener(simpleReloadListener);
        binding.spMonth.setOnItemSelectedListener(simpleReloadListener);
    }

    private final AdapterView.OnItemSelectedListener simpleReloadListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    loadData();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    private void setupFilterRadio() {
        binding.rgFilterType.setOnCheckedChangeListener((group, checkedId) -> {

            binding.layoutFilterTahun.setVisibility(View.GONE);
            binding.layoutFilterBulan.setVisibility(View.GONE);
            binding.layoutFilterHari.setVisibility(View.GONE);

            if (checkedId == binding.rbTahun.getId()) {
                binding.layoutFilterTahun.setVisibility(View.VISIBLE);
                setDefaultTahun();
            } else if (checkedId == binding.rbBulan.getId()) {
                binding.layoutFilterBulan.setVisibility(View.VISIBLE);
                setDefaultBulan();
            } else if (checkedId == binding.rbHari.getId()) {
                binding.layoutFilterHari.setVisibility(View.VISIBLE);
                setDefaultHari();
            }

            loadData();
        });
    }

    private void setDefaultTahun() {
        binding.spYear.setSelection(0);
    }

    private void setDefaultBulan() {
        binding.spYearMonth.setSelection(0);
        binding.spMonth.setSelection(today.get(Calendar.MONTH));
    }

    private void setDefaultHari() {
        String todayText = formatDate(today);
        binding.tvStartDate.setText(todayText);
        binding.tvEndDate.setText(todayText);
    }

    private void setupDatePicker() {
        binding.tvStartDate.setOnClickListener(v -> showDatePicker(binding.tvStartDate));
        binding.tvEndDate.setOnClickListener(v -> showDatePicker(binding.tvEndDate));
    }

    private void showDatePicker(View targetView) {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    ((TextView) targetView).setText(formatDate(selected));
                    loadData();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private String formatDate(Calendar calendar) {
        return new SimpleDateFormat("dd-MM-yy", new Locale("id", "ID"))
                .format(calendar.getTime());
    }

    private void loadData() {

        laporanDetailList.clear();

        binding.tvTotalPenjualan.setText(formatRupiah(0));
        binding.tvTotalLaba.setText(formatRupiah(0));

        Cursor cursor = null;

        if (binding.rbTahun.isChecked()) {

            cursor = dbHelper.getLaporanPenjualanDetailTahunan(
                    binding.spYear.getSelectedItem().toString(),
                    hakAkses,
                    kodePegawai,
                    selectedIdToko
            );

        } else if (binding.rbBulan.isChecked()) {

            String tahun = binding.spYearMonth.getSelectedItem().toString();
            String bulan = String.format(
                    Locale.US,
                    "%02d",
                    binding.spMonth.getSelectedItemPosition() + 1
            );

            cursor = dbHelper.getLaporanPenjualanDetailBulanan(
                    tahun,
                    bulan,
                    hakAkses,
                    kodePegawai,
                    selectedIdToko
            );

        } else if (binding.rbHari.isChecked()) {

            cursor = dbHelper.getLaporanPenjualanDetailHarian(
                    binding.tvStartDate.getText().toString(),
                    binding.tvEndDate.getText().toString(),
                    hakAkses,
                    kodePegawai,
                    selectedIdToko
            );
        }

        if (cursor != null && cursor.moveToFirst()) {

            double totalPenjualan = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("total_penjualan")
            );
            double totalLaba = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("total_laba")
            );

            binding.tvTotalPenjualan.setText(formatRupiah(totalPenjualan));
            binding.tvTotalLaba.setText(formatRupiah(totalLaba));

            do {
                laporanDetailList.add(new LaporanPenjualanDetailItem(
                        cursor.getString(cursor.getColumnIndexOrThrow("tanggal")),
                        cursor.getString(cursor.getColumnIndexOrThrow("nama_produk")),
                        cursor.getString(cursor.getColumnIndexOrThrow("qty")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("penjualan_kotor")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("laba_kotor"))
                ));
            } while (cursor.moveToNext());

            cursor.close();
        }

        adapter.notifyDataSetChanged();
    }

    private String getPeriodeLaporanText() {

        if (binding.rbTahun.isChecked()) {
            String tahun = binding.spYear.getSelectedItem().toString();
            return "Laporan Tahun " + tahun;

        } else if (binding.rbBulan.isChecked()) {
            String tahun = binding.spYearMonth.getSelectedItem().toString();
            String bulan = binding.spMonth.getSelectedItem().toString();
            return "Laporan " + bulan + " " + tahun;

        } else if (binding.rbHari.isChecked()) {
            String start = binding.tvStartDate.getText().toString();
            String end = binding.tvEndDate.getText().toString();
            return "Laporan " + start + " s/d " + end;
        }

        return "";
    }

    private void showCsvDownloadDialog() {

        String fileName =
                "laporan_penjualan_detail_owner_" +
                        new SimpleDateFormat(
                                "ddMMyyyy_HHmmss",
                                Locale.getDefault()
                        ).format(new Date()) +
                        ".csv";

        new AlertDialog.Builder(requireContext())
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

        if (laporanDetailList.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Tidak ada data untuk diexport",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            String tanggal = new SimpleDateFormat(
                    "HH-mm-dd-MM-yyyy",
                    Locale.getDefault()
            ).format(new Date());

            String fileName =
                    "laporan_penjualan_detail_owner_" + tanggal + ".csv";

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

                OutputStream outputStream =
                        requireContext()
                                .getContentResolver()
                                .openOutputStream(uri);

                OutputStreamWriter writer =
                        new OutputStreamWriter(outputStream);

                writer.append(
                        "Tanggal,Produk,Qty,Penjualan,Laba\n"
                );

                for (LaporanPenjualanDetailItem item : laporanDetailList) {

                    writer.append("\"")
                            .append(item.tanggal)
                            .append("\",\"")
                            .append(item.produk.replace("\"", "\"\""))
                            .append("\",\"")
                            .append(item.qty)
                            .append("\",\"")
                            .append(formatRupiah(item.totalPenjualan))
                            .append("\",\"")
                            .append(formatRupiah(item.totalLaba))
                            .append("\"\n");
                }

                writer.append("\n");
                writer.append("TOTAL,,,")
                        .append(
                                binding.tvTotalPenjualan
                                        .getText()
                                        .toString()
                        )
                        .append(",")
                        .append(
                                binding.tvTotalLaba
                                        .getText()
                                        .toString()
                        )
                        .append("\n");

                writer.flush();
                writer.close();

                new AlertDialog.Builder(requireContext())
                        .setTitle("Berhasil")
                        .setMessage(
                                "CSV berhasil disimpan di folder Download\n\n" +
                                        "Nama file:\n" + fileName
                        )
                        .setPositiveButton("OK", null)
                        .show();
            }

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(
                    requireContext(),
                    "Gagal export CSV",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showConfirmGeneratePdf() {
        if (laporanDetailList.isEmpty()) return;
        previewPdf();
    }

    private void previewPdf() {
        try {
            File pdfFile = new File(
                    requireContext().getCacheDir(),
                    "preview_laporan_penjualan_detail.pdf"
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

            String fileName = "laporan_penjualan_detail_owner_" + tanggal + ".pdf";

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
        if (laporanDetailList.isEmpty()) {
            return document;
        }

        Paint textPaint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerTextPaint = new Paint();
        Paint linePaint = new Paint();
        Paint headerBgPaint = new Paint();
        Paint rowAltBgPaint = new Paint();
        Paint totalBgPaint = new Paint();
        Paint totalTextPaint = new Paint();
        Paint tanggalPaint = new Paint();

        textPaint.setTextSize(10);
        textPaint.setColor(Color.BLACK);

        tanggalPaint.setTextSize(10);
        tanggalPaint.setFakeBoldText(true);
        tanggalPaint.setColor(Color.BLACK);

        titlePaint.setTextSize(16);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.BLACK);

        headerTextPaint.setTextSize(10);
        headerTextPaint.setFakeBoldText(true);
        headerTextPaint.setColor(Color.BLACK);

        totalTextPaint.setTextSize(10);
        totalTextPaint.setFakeBoldText(true);
        totalTextPaint.setColor(Color.BLACK);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);
        linePaint.setColor(Color.parseColor("#808080"));

        headerBgPaint.setStyle(Paint.Style.FILL);
        headerBgPaint.setColor(Color.parseColor("#E0E0E0"));

        rowAltBgPaint.setStyle(Paint.Style.FILL);
        rowAltBgPaint.setColor(Color.parseColor("#F7F7F7"));

        totalBgPaint.setStyle(Paint.Style.FILL);
        totalBgPaint.setColor(Color.parseColor("#EEEEEE"));

        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();

        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = 40;

        String appName = "SIQASIR";
        float appWidth = titlePaint.measureText(appName);
        canvas.drawText(appName, (pageWidth - appWidth) / 2f, y, titlePaint);
        y += 22;

        String titleText = "LAPORAN PENJUALAN DETAIL";
        float titleWidth = titlePaint.measureText(titleText);
        canvas.drawText(titleText, (pageWidth - titleWidth) / 2f, y, titlePaint);
        y += 16;

        String periodeText = getPeriodeLaporanText();
        float periodeWidth = textPaint.measureText(periodeText);
        canvas.drawText(periodeText, (pageWidth - periodeWidth) / 2f, y, textPaint);
        y += 18;

        canvas.drawLine(40, y, pageWidth - 40, y, linePaint);
        y += 20;

        int left = 40;
        int right = pageWidth - 40;
        int rowHeight = 20;

        int colProduk = left;
        int colQty = 250;
        int colPenjualan = 300;
        int colLaba = 420;

        canvas.drawRect(left, y, right, y + rowHeight, headerBgPaint);
        canvas.drawRect(left, y, right, y + rowHeight, linePaint);

        canvas.drawText("Produk", colProduk + 5, y + 14, headerTextPaint);
        canvas.drawText("Qty", colQty + 5, y + 14, headerTextPaint);
        canvas.drawText("Penjualan", colPenjualan + 5, y + 14, headerTextPaint);
        canvas.drawText("Laba", colLaba + 5, y + 14, headerTextPaint);

        y += rowHeight;

        boolean alternate = false;
        String lastTanggal = "";

        for (LaporanPenjualanDetailItem item : laporanDetailList) {

            if (y > pageHeight - 100) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;

                canvas.drawRect(left, y, right, y + rowHeight, headerBgPaint);
                canvas.drawRect(left, y, right, y + rowHeight, linePaint);

                canvas.drawText("Produk", colProduk + 5, y + 14, headerTextPaint);
                canvas.drawText("Qty", colQty + 5, y + 14, headerTextPaint);
                canvas.drawText("Penjualan", colPenjualan + 5, y + 14, headerTextPaint);
                canvas.drawText("Laba", colLaba + 5, y + 14, headerTextPaint);

                y += rowHeight;
                alternate = false;
                lastTanggal = "";
            }

            if (!item.tanggal.equals(lastTanggal)) {

                y += 12;

                Paint tanggalBg = new Paint();
                tanggalBg.setColor(Color.parseColor("#EEEEEE"));
                canvas.drawRect(left, y - 10, right, y + 10, tanggalBg);
                canvas.drawRect(left, y- 10, right, y + 10, linePaint);

                canvas.drawText(item.tanggal, left + 5, y + 5, tanggalPaint);
                y += 18;

                lastTanggal = item.tanggal;
                alternate = false;
            }

            if (alternate) {
                canvas.drawRect(left, y, right, y + rowHeight, rowAltBgPaint);
            }

            canvas.drawRect(left, y, right, y + rowHeight, linePaint);

            canvas.drawText(item.produk, colProduk + 5, y + 14, textPaint);
            canvas.drawText(item.qty, colQty + 5, y + 14, textPaint);
            canvas.drawText(
                    formatRupiah(item.totalPenjualan),
                    colPenjualan + 5,
                    y + 14,
                    textPaint
            );
            canvas.drawText(
                    formatRupiah(item.totalLaba),
                    colLaba + 5,
                    y + 14,
                    textPaint
            );

            y += rowHeight;
            alternate = !alternate;
        }

        canvas.drawRect(left, y, right, y + rowHeight, totalBgPaint);
        canvas.drawRect(left, y, right, y + rowHeight, linePaint);

        canvas.drawText("TOTAL", colProduk + 5, y + 14, totalTextPaint);
        canvas.drawText(binding.tvTotalPenjualan.getText().toString(),
                colPenjualan + 5, y + 14, totalTextPaint);
        canvas.drawText(binding.tvTotalLaba.getText().toString(),
                colLaba + 5, y + 14, totalTextPaint);

        // ===== FOOTER =====
        int footerY = pageHeight - 40;

        canvas.drawLine(40, footerY - 15, pageWidth - 40, footerY - 15, linePaint);

        String printedDate = "Dicetak pada: " +
                new SimpleDateFormat("dd MMMM yyyy HH:mm", new Locale("id", "ID"))
                        .format(new Date());

        canvas.drawText(
                printedDate,
                40,
                footerY,
                textPaint
        );

        String brand = "Bathiku by PyramidSoft";
        float brandWidth = textPaint.measureText(brand);

        canvas.drawText(
                brand,
                pageWidth - 40 - brandWidth,
                footerY,
                textPaint
        );

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

    private void printLaporanPenjualanDetailThermal() {

        // ===== CEK PRINTER =====
        if (laporanDetailList.isEmpty()) {
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
        String alamatPrinter = session.getAlamatPrinter();
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
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);

        SendDataString("SIQASIR\n");
        SendDataString("LAPORAN PENJUALAN DETAIL\n");

        String periode = getPeriodeLaporanText();
        SendDataString(periode + "\n");
        SendDataString("--------------------------------\n");

        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);

        double totalPenjualan = 0;
        double totalLaba = 0;

        String lastTanggal = "";

        for (LaporanPenjualanDetailItem item : laporanDetailList) {

            if (!item.tanggal.equals(lastTanggal)) {
                SendDataString("\n");
                SendDataString(item.tanggal + "\n");
                SendDataString("--------------------------------\n");
                lastTanggal = item.tanggal;
            }

            totalPenjualan += item.totalPenjualan;
            totalLaba += item.totalLaba;

            SendDataString(item.produk + "\n");
            SendDataString(leftRightAlign("Qty", item.qty));
            SendDataString(leftRightAlign("Penjualan", formatRupiah(item.totalPenjualan)));
            SendDataString(leftRightAlign("Laba", formatRupiah(item.totalLaba)));
            SendDataString("\n");
        }

        SendDataString("--------------------------------\n");
        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataString("TOTAL\n");

        SendDataByte(PerintahPrinter.ESC_ALIGN_LEFT);
        SendDataString(leftRightAlign("Total Penjualan", formatRupiah(totalPenjualan)));
        SendDataString(leftRightAlign("Total Laba", formatRupiah(totalLaba)));
        SendDataString("\n");

        SendDataByte(PerintahPrinter.ESC_ALIGN_CENTER);
        SendDataString(
                "Dicetak: " +
                        new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                                .format(new Date()) + "\n"
        );

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                mBluetoothService.stop();
            }
        }
    }
}
