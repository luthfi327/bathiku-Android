package com.pyramidsoft.siqasir.model;

public class KategoriModels {
    String Kategori, IdKategori, IdToko;

    public KategoriModels() {
    }

    public KategoriModels(String kategori, String idKategori, String idToko) {
        this.Kategori = kategori;
        this.IdKategori = idKategori;
        this.IdToko = idToko;
    }

    public String getKategori() {
        return Kategori;
    }

    public void setKategori(String kategori) {
        this.Kategori = kategori;
    }

    public String getIdKategori() {
        return IdKategori;
    }

    public void setIdKategori(String idKategori) {
        this.IdKategori = idKategori;
    }

    public String getIdToko() {
        return IdToko;
    }

    public void setIdToko(String idToko) {
        this.IdToko = idToko;
    }
}
