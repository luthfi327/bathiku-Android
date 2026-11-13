package com.pyramidsoft.siqasir.ui.laporanpenjualan;

public class LaporanPenjualanItem {

    public String tanggal;
    public int totalTransaksi;
    public double totalPenjualan;
    public double totalLaba;

    public LaporanPenjualanItem(
            String tanggal,
            int totalTransaksi,
            double totalPenjualan,
            double totalLaba
    ) {
        this.tanggal = tanggal;
        this.totalTransaksi = totalTransaksi;
        this.totalPenjualan = totalPenjualan;
        this.totalLaba = totalLaba;
    }
}
