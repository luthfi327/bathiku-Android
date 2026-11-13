package com.pyramidsoft.siqasir.ui.laporanpenjualandetail;

public class LaporanPenjualanDetailItem {

    public String tanggal;
    public String produk;
    public String qty;              // ✅ TEXT dari SQLite
    public double totalPenjualan;
    public double totalLaba;

    public LaporanPenjualanDetailItem(
            String tanggal,
            String produk,
            String qty,
            double totalPenjualan,
            double totalLaba
    ) {
        this.tanggal = tanggal;
        this.produk = produk;
        this.qty = qty;
        this.totalPenjualan = totalPenjualan;
        this.totalLaba = totalLaba;
    }
}

