package com.pyramidsoft.siqasir.model;

public class JenisUsahaModel {

    public String id;
    public String nama;

    @Override
    public String toString() {
        // yang tampil di dropdown
        return nama;
    }
}
