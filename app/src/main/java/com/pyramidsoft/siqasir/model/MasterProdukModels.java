package com.pyramidsoft.siqasir.model;

public class MasterProdukModels {
    String KdProduk, KdToko, KdUser, NmProduk, Kat, sku, hrgBeli, hrgJual, hrgDiskon, laba;
    Integer favorit, stok;
    private byte[] gbrProduk;

    public MasterProdukModels() {
    }

    public MasterProdukModels(String kdProduk, String nmProduk, String kat, byte[] gbrProduk, String sku,
                              String hrgBeli, String hrgJual, String hrgDiskon, Integer favorit, String laba, Integer stok) {
        KdProduk = kdProduk;
        NmProduk = nmProduk;
        Kat = kat;
        this.gbrProduk = gbrProduk;
        this.sku = sku;
        this.hrgBeli = hrgBeli;
        this.hrgJual = hrgJual;
        this.hrgDiskon = hrgDiskon;
        this.favorit = favorit;
        this.laba = laba;
        this.stok = stok;
    }

    public String getLaba() {
        return laba;
    }

    public void setLaba(String laba) {
        this.laba = laba;
    }

    public Integer getStok() {
        return stok;
    }

    public void setStok(Integer stok) {
        this.stok = stok;
    }

    public String getKdProduk() {
        return KdProduk;
    }

    public void setKdProduk(String kdProduk) {
        KdProduk = kdProduk;
    }

    public byte[] getGbrProduk() {
        return gbrProduk;
    }

    public void setGbrProduk(byte[] gbrProduk) {
        this.gbrProduk = gbrProduk;
    }

    public String getKdToko() {
        return KdToko;
    }

    public void setKdToko(String kdToko) {
        KdToko = kdToko;
    }

    public String getKdUser() {
        return KdUser;
    }

    public void setKdUser(String kdUser) {
        KdUser = kdUser;
    }

    public String getNmProduk() {
        return NmProduk;
    }

    public void setNmProduk(String nmProduk) {
        NmProduk = nmProduk;
    }

    public String getKat() {
        return Kat;
    }

    public void setKat(String kat) {
        Kat = kat;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getHrgBeli() {
        return hrgBeli;
    }

    public void setHrgBeli(String hrgBeli) {
        this.hrgBeli = hrgBeli;
    }

    public String getHrgJual() {
        return hrgJual;
    }

    public void setHrgJual(String hrgJual) {
        this.hrgJual = hrgJual;
    }

    public String getHrgDiskon() {
        return hrgDiskon;
    }

    public void setHrgDiskon(String hrgDiskon) {
        this.hrgDiskon = hrgDiskon;
    }

    public Integer getFavorit() {
        return favorit;
    }

    public void setFavorit(Integer favorit) {
        this.favorit = favorit;
    }
}
