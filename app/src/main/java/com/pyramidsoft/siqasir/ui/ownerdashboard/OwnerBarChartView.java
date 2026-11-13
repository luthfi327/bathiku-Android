package com.pyramidsoft.siqasir.ui.ownerdashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OwnerBarChartView
 *
 * Custom View yang menggambar bar chart sederhana menggunakan Canvas API.
 * Tidak memerlukan library MPAndroidChart atau dependency eksternal apapun.
 *
 * Fitur:
 *   - Bar berwarna ungu (#6F42C1) sesuai warna tema app
 *   - Label tanggal di bawah setiap bar (format: dd/MM)
 *   - Nilai omzet di atas setiap bar (format: singkat "12.5rb", "1.2jt")
 *   - Bar dengan sudut melengkung (rounded corners) di bagian atas
 *   - Garis baseline horizontal
 *   - Handling nilai kosong (semua 0 → tampilkan placeholder)
 *
 * Penggunaan di XML:
 *   <com.pyramidsoft.siqasir.ui.ownerdashboard.OwnerBarChartView
 *       android:id="@+id/barChartPenjualan"
 *       android:layout_width="match_parent"
 *       android:layout_height="160dp" />
 *
 * Penggunaan dari Java:
 *   binding.barChartPenjualan.setData(listNilai, listLabel);
 */
public class OwnerBarChartView extends View {

    // =========================================================================
    // Paint objects (dibuat sekali di konstruktor untuk performa)
    // =========================================================================
    private final Paint paintBar;
    private final Paint paintBarEmpty;
    private final Paint paintLabel;
    private final Paint paintValue;
    private final Paint paintBaseline;
    private final Paint paintPlaceholder;

    // =========================================================================
    // Data
    // =========================================================================
    private List<Float>  nilaiList  = new ArrayList<>();
    private List<String> labelList  = new ArrayList<>();
    private float maxNilai = 1f;   // default 1 supaya tidak div-by-zero

    // =========================================================================
    // Dimensi & konstanta
    // =========================================================================
    private static final float PADDING_HORIZONTAL_DP = 12f;
    private static final float PADDING_TOP_DP        = 28f;   // ruang untuk nilai di atas bar
    private static final float PADDING_BOTTOM_DP     = 28f;   // ruang untuk label di bawah bar
    private static final float BAR_RADIUS_DP         = 4f;
    private static final int   COLOR_BAR_ACTIVE       = Color.parseColor("#6F42C1");
    private static final int   COLOR_BAR_EMPTY        = Color.parseColor("#E9ECEF");
    private static final int   COLOR_LABEL            = Color.parseColor("#6B7280");
    private static final int   COLOR_VALUE            = Color.parseColor("#374151");
    private static final int   COLOR_BASELINE         = Color.parseColor("#D1D5DB");

    // =========================================================================
    // Konstruktor
    // =========================================================================

    public OwnerBarChartView(Context context) {
        this(context, null);
    }

    public OwnerBarChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OwnerBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = context.getResources().getDisplayMetrics().density;

        paintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBar.setColor(COLOR_BAR_ACTIVE);
        paintBar.setStyle(Paint.Style.FILL);

        paintBarEmpty = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarEmpty.setColor(COLOR_BAR_EMPTY);
        paintBarEmpty.setStyle(Paint.Style.FILL);

        paintLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabel.setColor(COLOR_LABEL);
        paintLabel.setTextSize(10 * density);
        paintLabel.setTextAlign(Paint.Align.CENTER);

        paintValue = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintValue.setColor(COLOR_VALUE);
        paintValue.setTextSize(9 * density);
        paintValue.setTextAlign(Paint.Align.CENTER);

        paintBaseline = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBaseline.setColor(COLOR_BASELINE);
        paintBaseline.setStrokeWidth(1 * density);

        paintPlaceholder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPlaceholder.setColor(COLOR_LABEL);
        paintPlaceholder.setTextSize(12 * density);
        paintPlaceholder.setTextAlign(Paint.Align.CENTER);
    }

    // =========================================================================
    // API Publik
    // =========================================================================

    /**
     * Set data yang akan digambar.
     *
     * @param nilai  List nilai omzet per hari (float, dalam rupiah)
     * @param labels List label per hari (String, contoh "21/01")
     */
    public void setData(List<Float> nilai, List<String> labels) {
        this.nilaiList  = nilai  != null ? nilai  : new ArrayList<>();
        this.labelList  = labels != null ? labels : new ArrayList<>();

        // Hitung nilai maksimum untuk scaling bar
        maxNilai = 1f;
        for (float v : nilaiList) {
            if (v > maxNilai) maxNilai = v;
        }

        invalidate();   // trigger onDraw()
    }

    // =========================================================================
    // Drawing
    // =========================================================================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        if (nilaiList.isEmpty()) {
            // Tampilkan placeholder teks jika belum ada data
            canvas.drawText("Belum ada data penjualan",
                    w / 2f, h / 2f, paintPlaceholder);
            return;
        }

        float density           = getContext().getResources().getDisplayMetrics().density;
        float paddingH          = PADDING_HORIZONTAL_DP * density;
        float paddingTop        = PADDING_TOP_DP * density;
        float paddingBottom     = PADDING_BOTTOM_DP * density;
        float barRadius         = BAR_RADIUS_DP * density;

        int   count             = nilaiList.size();
        float usableWidth       = w - 2 * paddingH;
        float barWidth          = usableWidth / count * 0.55f;
        float gap               = usableWidth / count;
        float chartTop          = paddingTop;
        float chartBottom       = h - paddingBottom;
        float chartHeight       = chartBottom - chartTop;

        // Garis baseline
        canvas.drawLine(paddingH, chartBottom, w - paddingH, chartBottom, paintBaseline);

        for (int i = 0; i < count; i++) {
            float nilai      = nilaiList.get(i);
            float centerX    = paddingH + gap * i + gap / 2f;
            float barHeightRatio = (maxNilai > 0) ? (nilai / maxNilai) : 0f;
            float minBarHeight   = 4 * density;   // bar minimum agar tetap terlihat
            float barH           = Math.max(chartHeight * barHeightRatio, nilai > 0 ? minBarHeight : 0f);

            float left   = centerX - barWidth / 2f;
            float right  = centerX + barWidth / 2f;
            float top    = chartBottom - barH;
            float bottom = chartBottom;

            // Gambar bar (rounded hanya di atas jika ada nilai, kotak penuh jika kosong)
            RectF rect = new RectF(left, top, right, bottom);
            if (nilai > 0) {
                canvas.drawRoundRect(rect, barRadius, barRadius, paintBar);
                // Tutup rounded corner di bagian bawah dengan persegi
                if (barH > barRadius) {
                    canvas.drawRect(left, top + barRadius, right, bottom, paintBar);
                }
            } else {
                canvas.drawRoundRect(rect, barRadius, barRadius, paintBarEmpty);
            }

            // Label tanggal di bawah bar
            if (i < labelList.size()) {
                canvas.drawText(labelList.get(i), centerX, chartBottom + 16 * density, paintLabel);
            }

            // Nilai omzet di atas bar (hanya jika > 0)
            if (nilai > 0) {
                String label = formatSingkat(nilai);
                canvas.drawText(label, centerX, top - 4 * density, paintValue);
            }
        }
    }

    // =========================================================================
    // Helper format nilai singkat
    // =========================================================================

    /**
     * Format nilai ke bentuk singkat: 12500 → "12,5rb", 1200000 → "1,2jt"
     */
    private String formatSingkat(float nilai) {
        if (nilai >= 1_000_000f) {
            float juta = nilai / 1_000_000f;
            if (juta == (int) juta) return (int) juta + "jt";
            return String.format(Locale.getDefault(), "%.1fjt", juta);
        } else if (nilai >= 1_000f) {
            float ribu = nilai / 1_000f;
            if (ribu == (int) ribu) return (int) ribu + "rb";
            return String.format(Locale.getDefault(), "%.1frb", ribu);
        } else {
            return String.valueOf((int) nilai);
        }
    }
}
