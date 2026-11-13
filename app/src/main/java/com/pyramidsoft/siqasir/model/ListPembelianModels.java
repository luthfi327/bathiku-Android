package com.pyramidsoft.siqasir.model;

public class ListPembelianModels {
    String namaproduk,hargaproduk, qty;

    public ListPembelianModels(String namaproduk, String hargaproduk, String qty) {
        this.namaproduk = namaproduk;
        this.hargaproduk = hargaproduk;
        this.qty = qty;
    }

    public String getNamaproduk() {
        return namaproduk;
    }

    public void setNamaproduk(String namaproduk) {
        this.namaproduk = namaproduk;
    }

    public String getHargaproduk() {
        return hargaproduk;
    }

    public void setHargaproduk(String hargaproduk) {
        this.hargaproduk = hargaproduk;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }
}
