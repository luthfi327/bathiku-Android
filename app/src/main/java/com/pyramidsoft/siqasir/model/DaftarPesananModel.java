package com.pyramidsoft.siqasir.model;

public class DaftarPesananModel {
    String jenis_orderan, nama_pembeli, jml, meja, totalharga, invoice;

    public DaftarPesananModel(String jenis_orderan, String nama_pembeli, String jml, String meja, String totalharga, String invoice) {
        this.jenis_orderan = jenis_orderan;
        this.nama_pembeli = nama_pembeli;
        this.jml = jml;
        this.meja = meja;
        this.totalharga = totalharga;
        this.invoice = invoice;
    }

    public String getJenis_orderan(){return jenis_orderan;}

    public void setJenis_orderan(String jenis_orderan){this.jenis_orderan = jenis_orderan;}

    public String getNama_pembeli() {
        return nama_pembeli;
    }

    public void setNama_pembeli(String nama_pembeli) {
        this.nama_pembeli = nama_pembeli;
    }

    public String getJml() {
        return jml;
    }

    public void setJml(String jml) {
        this.jml = jml;
    }

    public String getMeja() {
        return meja;
    }

    public void setMeja(String meja) {
        this.meja = meja;
    }

    public String getTotalharga() {
        return totalharga;
    }

    public void setTotalharga(String totalharga) {
        this.totalharga = totalharga;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }
}
