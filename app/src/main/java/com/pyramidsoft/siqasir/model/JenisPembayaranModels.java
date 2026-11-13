package com.pyramidsoft.siqasir.model;

public class JenisPembayaranModels {
    String jenis_pembayaran;
    int total;

    public JenisPembayaranModels(String jenis_pembayaran, int total) {
        this.jenis_pembayaran = jenis_pembayaran;
        this.total = total;
    }

    public String getJenis_pembayaran() {
        return jenis_pembayaran;
    }

    public void setJenis_pembayaran(String jenis_pembayaran) {
        this.jenis_pembayaran = jenis_pembayaran;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
