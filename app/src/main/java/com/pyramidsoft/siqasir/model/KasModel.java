package com.pyramidsoft.siqasir.model;

public class KasModel {

    private String tgl_buka,tgl_tutup, kode_rekapkas;

    public KasModel(String kode_rekapkas, String tgl_buka, String tgl_tutup) {
        this.kode_rekapkas = kode_rekapkas;
        this.tgl_buka = tgl_buka;
        this.tgl_tutup = tgl_tutup;
    }

    public String getTgl_buka() {
        return tgl_buka;
    }

    public void setTgl_buka(String tgl_buka) {
        this.tgl_buka = tgl_buka;
    }

    public String getTgl_tutup() {
        return tgl_tutup;
    }

    public void setTgl_tutup(String tgl_tutup) {
        this.tgl_tutup = tgl_tutup;
    }

    public String getKode_rekapkas() {
        return kode_rekapkas;
    }

    public void setKode_rekapkas(String kode_rekapkas) {
        this.kode_rekapkas = kode_rekapkas;
    }
}