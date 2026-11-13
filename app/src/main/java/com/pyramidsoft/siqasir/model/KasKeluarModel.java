package com.pyramidsoft.siqasir.model;

public class KasKeluarModel {
    private String keperluan, nominal;

    public KasKeluarModel(String keperluan, String nominal) {
        this.keperluan = keperluan;
        this.nominal = nominal;
    }

    public String getKeperluan() {
        return keperluan;
    }

    public void setKeperluan(String keperluan) {
        this.keperluan = keperluan;
    }

    public String getNominal() {
        return nominal;
    }

    public void setNominal(String nominal) {
        this.nominal = nominal;
    }
}

