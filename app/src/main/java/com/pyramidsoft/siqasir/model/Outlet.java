package com.pyramidsoft.siqasir.model;

public class Outlet {
    private int idToko;
    private String kodeToko;
    private String namaToko;
    private String kota;
    private String provinsi;
    private String email;
    private String noTelepon;
    private byte[] logo; // tambahkan field logo

    public Outlet(int idToko, String kodeToko, String namaToko, String kota,
                  String provinsi, String email, String noTelepon, byte[] logo) {
        this.idToko = idToko;
        this.kodeToko = kodeToko;
        this.namaToko = namaToko;
        this.kota = kota;
        this.provinsi = provinsi;
        this.email = email;
        this.noTelepon = noTelepon;
        this.logo = logo;
    }

    public int getIdToko() { return idToko; }
    public String getKodeToko() { return kodeToko; }
    public String getNamaToko() { return namaToko; }
    public String getKota() { return kota; }
    public String getProvinsi() { return provinsi; }
    public String getEmail() { return email; }
    public String getNoTelepon() { return noTelepon; }
    public byte[] getLogo() { return logo; }

    public void setLogo(byte[] logo) { this.logo = logo; }

    @Override
    public String toString() {
        return namaToko; // atau kodeToko + " - " + namaToko
    }
}
