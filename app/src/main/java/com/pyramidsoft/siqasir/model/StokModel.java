package com.pyramidsoft.siqasir.model;

public class StokModel {
    private int no;
    private String kode;
    private String nama;
    private String qty;
    private String hpp; // <-- String (sudah diformat)

    public StokModel(int no, String kode, String nama, String qty, String hpp) {
        this.no = no;
        this.kode = kode;
        this.nama = nama;
        this.qty = qty;
        this.hpp = hpp;
    }

    public int getNo() {
        return no;
    }

    public String getKode() {
        return kode;
    }

    public String getNama() {
        return nama;
    }

    public String getQty() {
        return qty;
    }

    public String getHpp() {
        return hpp;
    }
}
