package com.pyramidsoft.siqasir.model;

public class ProdukPesananModel {
    String kd_produk;
    String nama_produk;
    String harga;
    String total_harga;
    String harga_diskon;
    String harga_sale;
    String jml;

    public ProdukPesananModel(String kd_produk, String nama_produk, String harga, String total_harga, String harga_diskon, String harga_sale, String jml) {
        this.kd_produk = kd_produk;
        this.nama_produk = nama_produk;
        this.harga = harga;
        this.total_harga = total_harga;
        this.harga_diskon = harga_diskon;
        this.harga_sale = harga_sale;
        this.jml = jml;
    }

    public String getKd_produk() {
        return kd_produk;
    }

    public void setKd_produk(String kd_produk) {
        this.kd_produk = kd_produk;
    }

    public String getNama_produk() {
        return nama_produk;
    }

    public void setNama_produk(String nama_produk) {
        this.nama_produk = nama_produk;
    }

    public String getHarga() {
        return harga;
    }

    public void setHarga(String harga) {
        this.harga = harga;
    }

    public String getTotal_harga() {
        return total_harga;
    }

    public void setTotal_harga(String total_harga) {
        this.total_harga = total_harga;
    }

    public String getJml() {
        return jml;
    }

    public void setJml(String jml) {
        this.jml = jml;
    }

    public String getHarga_diskon() {
        return harga_diskon;
    }

    public void setHarga_diskon(String harga_diskon) {
        this.harga_diskon = harga_diskon;
    }

    public String getHarga_sale() {
        return harga_sale;
    }

    public void setHarga_sale(String harga_sale) {
        this.harga_sale = harga_sale;
    }
}
