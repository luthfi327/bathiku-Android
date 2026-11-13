package com.pyramidsoft.siqasir.model;

public class MetodePembayaranModels {
    private String metode, nominal;

    public MetodePembayaranModels(String metode, String nominal) {
        this.metode = metode;
        this.nominal = nominal;
    }

    public String getMetode() {
        return metode;
    }

    public void setMetode(String metode) {
        this.metode = metode;
    }

    public String getNominal() {
        return nominal;
    }

    public void setNominal(String nominal) {
        this.nominal = nominal;
    }
}