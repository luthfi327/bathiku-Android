package com.pyramidsoft.siqasir.model;

public class RiwayatPenjualanModels {
    String jenisKedatangan, namaCust,qty,invoice,totalTransaksi;

    public RiwayatPenjualanModels(String jenisKedatangan, String namaCust, String qty, String invoice, String totalTransaksi) {
        this.jenisKedatangan = jenisKedatangan;
        this.namaCust = namaCust;
        this.qty = qty;
        this.invoice = invoice;
        this.totalTransaksi = totalTransaksi;
    }

    public String getJenisKedatangan() {
        return jenisKedatangan;
    }

    public void setJenisKedatangan(String jenisKedatangan) {
        this.jenisKedatangan = jenisKedatangan;
    }

    public String getNamaCust() {
        return namaCust;
    }

    public void setNamaCust(String namaCust) {
        this.namaCust = namaCust;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }

    public String getTotalTransaksi() {
        return totalTransaksi;
    }

    public void setTotalTransaksi(String totalTransaksi) {
        this.totalTransaksi = totalTransaksi;
    }
}
