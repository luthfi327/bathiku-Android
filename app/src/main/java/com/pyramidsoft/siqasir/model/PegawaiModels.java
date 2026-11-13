package com.pyramidsoft.siqasir.model;

public class PegawaiModels {
    String idPegawai, idToko; // Tambahan baru
    String kodePegawai, namaPegawai, gender, nohp, posisi, hakAkses, status, username, password, pin;

    public PegawaiModels(String kodePegawai, String namaPegawai, String posisi) {
        this.kodePegawai = kodePegawai;
        this.namaPegawai = namaPegawai;
        this.posisi = posisi;
    }

    public PegawaiModels(String idPegawai, String idToko, String kodePegawai, String namaPegawai, String posisi) {
        this.idPegawai = idPegawai;
        this.idToko = idToko;
        this.kodePegawai = kodePegawai;
        this.namaPegawai = namaPegawai;
        this.posisi = posisi;
    }

    public PegawaiModels() {
    }

    public String getIdPegawai() {
        return idPegawai;
    }

    public void setIdPegawai(String idPegawai) {
        this.idPegawai = idPegawai;
    }

    public String getIdToko() {
        return idToko;
    }

    public void setIdToko(String idToko) {
        this.idToko = idToko;
    }

    public String getKodePegawai() {
        return kodePegawai;
    }

    public void setKodePegawai(String kodePegawai) {
        this.kodePegawai = kodePegawai;
    }

    public String getNamaPegawai() {
        return namaPegawai;
    }

    public void setNamaPegawai(String namaPegawai) {
        this.namaPegawai = namaPegawai;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNohp() {
        return nohp;
    }

    public void setNohp(String nohp) {
        this.nohp = nohp;
    }

    public String getPosisi() {
        return posisi;
    }

    public void setPosisi(String posisi) {
        this.posisi = posisi;
    }

    public String getHakAkses() {
        return hakAkses;
    }

    public void setHakAkses(String hakAkses) {
        this.hakAkses = hakAkses;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
