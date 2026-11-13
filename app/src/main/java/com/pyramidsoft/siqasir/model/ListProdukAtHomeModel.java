package com.pyramidsoft.siqasir.model;

public class ListProdukAtHomeModel {
    String kd_produk;
    String nama_produk;
    String hrg_potongan;
    String hrg_sale;
    String kategori;
    byte[] ivProduk;
    Integer stok;

    public ListProdukAtHomeModel(String kd_produk, String nama_produk, String hrg_potongan, String hrg_sale, String kategori, byte[] ivProduk, Integer stok) {
        this.kd_produk = kd_produk;
        this.nama_produk = nama_produk;
        this.hrg_potongan = hrg_potongan;
        this.hrg_sale = hrg_sale;
        this.kategori = kategori;
        this.ivProduk = ivProduk;
        this.stok = stok;
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

    public String getHrg_potongan() {
        return hrg_potongan;
    }

    public void setHrg_potongan(String hrg_potongan) {
        this.hrg_potongan = hrg_potongan;
    }

    public String getHrg_sale() {
        return hrg_sale;
    }

    public void setHrg_sale(String hrg_sale) {
        this.hrg_sale = hrg_sale;
    }

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public byte[] getIvProduk() {
        return ivProduk;
    }

    public void setIvProduk(byte[] ivProduk) {
        this.ivProduk = ivProduk;
    }

    public Integer getStok() {
        return stok;
    }

    public void setStok(Integer stok) {
        this.stok = stok;
    }
}
