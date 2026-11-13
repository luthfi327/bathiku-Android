package com.pyramidsoft.siqasir.model;

public class JenisKedatanganModels {
    String id_jenis_kedatangan, id_toko, jenis_kedatangan;

    public JenisKedatanganModels() {
    }

    public JenisKedatanganModels(String id_jenis_kedatangan, String id_toko, String jenis_kedatangan) {
        this.id_jenis_kedatangan = id_jenis_kedatangan;
        this.id_toko = id_toko;
        this.jenis_kedatangan = jenis_kedatangan;
    }

    public String getId_jenis_kedatangan() { return id_jenis_kedatangan; }
    public void setId_jenis_kedatangan(String id_jenis_kedatangan) {
        this.id_jenis_kedatangan = id_jenis_kedatangan;
    }

    public String getId_toko() { return id_toko; }
    public void setId_toko(String id_toko) {
        this.id_toko = id_toko;
    }

    public String getJenis_kedatangan() { return jenis_kedatangan; }
    public void setJenis_kedatangan(String jenis_kedatangan) {
        this.jenis_kedatangan = jenis_kedatangan;
    }
}
