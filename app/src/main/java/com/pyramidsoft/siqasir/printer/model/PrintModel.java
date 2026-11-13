package com.pyramidsoft.siqasir.printer.model;

public class PrintModel {
    String id,nama_printer,alamat_printer,tipe_printer;

    public PrintModel(String id, String nama_printer, String alamat_printer) {
        this.id = id;
        this.nama_printer = nama_printer;
        this.alamat_printer = alamat_printer;
    }

    public PrintModel() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNama_printer() {
        return nama_printer;
    }

    public void setNama_printer(String nama_printer) {
        this.nama_printer = nama_printer;
    }

    public String getAlamat_printer() {
        return alamat_printer;
    }

    public void setAlamat_printer(String alamat_printer) {
        this.alamat_printer = alamat_printer;
    }

    public String getTipe_printer() {
        return tipe_printer;
    }

    public void setTipe_printer(String tipe_printer) {
        this.tipe_printer = tipe_printer;
    }
}
