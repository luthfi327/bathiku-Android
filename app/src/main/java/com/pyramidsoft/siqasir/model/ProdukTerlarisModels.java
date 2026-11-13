package com.pyramidsoft.siqasir.model;

public class ProdukTerlarisModels {
    private String produk, qty, nominal;

    public ProdukTerlarisModels(String produk, String qty, String nominal) {
        this.produk = produk;
        this.qty = qty;
        this.nominal = nominal;
    }

    public String getProduk() {
        return produk;
    }

    public void setProduk(String produk) {
        this.produk = produk;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getNominal() {
        return nominal;
    }

    public void setNominal(String nominal) {
        this.nominal = nominal;
    }
}
