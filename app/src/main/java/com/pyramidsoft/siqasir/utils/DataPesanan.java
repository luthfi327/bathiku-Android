package com.pyramidsoft.siqasir.utils;

import com.pyramidsoft.siqasir.adapter.PesananProdukAdapter;
import com.pyramidsoft.siqasir.model.ProdukPesananModel;

import java.util.ArrayList;
import java.util.List;

public class DataPesanan {
    static String InvoiceEdit = "";
    private static List<ProdukPesananModel> arrayPesanan = new ArrayList<>();
    private static PesananProdukAdapter adapter;
    static DataPelanggan dataPelanggan = new DataPelanggan("", "", "", "", "");

    public static void setInvoiceEdit(String invoiceEdit) {
        InvoiceEdit = invoiceEdit;
      }

    public static String getInvoiceEdit() {
        return InvoiceEdit;
    }

    public static void setAdapter(PesananProdukAdapter pesananProdukAdapter) {
        adapter = pesananProdukAdapter;
    }

    public static void setDefaultDataPesanan() {
        InvoiceEdit = "";
        arrayPesanan = new ArrayList<>();
        dataPelanggan = new DataPelanggan("", "", "", "", "");
    }
    public static void setArrayPesanan(List<ProdukPesananModel> pesanan) {
        arrayPesanan = pesanan;
    }

    public static void addNewPesanan(ProdukPesananModel model) {
        if (!isExist(model)) {
            arrayPesanan.add(model);
        }
    }

    public static void addPesanan(int i) {
        int qtyplus = Integer.parseInt(arrayPesanan.get(i).getJml()) + 1;
        int ctotalharga = qtyplus * Integer.parseInt(arrayPesanan.get(i).getHarga());
        arrayPesanan.get(i).setJml(String.valueOf(qtyplus));
        arrayPesanan.get(i).setTotal_harga(String.valueOf(ctotalharga));
    }

    public static void kurangiPesanan(int i) {
        if (Integer.parseInt(arrayPesanan.get(i).getJml()) > 1) {
            int qtymin = Integer.parseInt(arrayPesanan.get(i).getJml()) - 1;
            int ctotalharga = qtymin * Integer.parseInt(arrayPesanan.get(i).getHarga());
            arrayPesanan.get(i).setJml(String.valueOf(qtymin));
            arrayPesanan.get(i).setTotal_harga(String.valueOf(ctotalharga));
        }
    }

    public static void removePesanan(int i) {
        if (i >= 0 && i < arrayPesanan.size()) {
            arrayPesanan.remove(i);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else {
        }
    }

    public static boolean isExist(ProdukPesananModel model) {
        for (int i = 0; i < arrayPesanan.size(); i++) {
            if (arrayPesanan.get(i).getNama_produk().equals(model.getNama_produk())) {
                int qtyplus = Integer.parseInt(arrayPesanan.get(i).getJml()) + 1;
                int ctotalharga = qtyplus * Integer.parseInt(arrayPesanan.get(i).getHarga());
                arrayPesanan.get(i).setJml(String.valueOf(qtyplus));
                arrayPesanan.get(i).setTotal_harga(String.valueOf(ctotalharga));
                return true;
            }
        }
        return false;
    }

    public static String getTotalPesanan() {
        int total = 0;
        for (int i = 0; i < arrayPesanan.size(); i++) {
            total += Integer.parseInt(arrayPesanan.get(i).getJml());
        }
        return String.valueOf(total);
    }

    public static String getTotalHargaSaleProduk() {
        int total = 0;
        for (int i = 0; i < arrayPesanan.size(); i++) {
            total += Integer.parseInt(arrayPesanan.get(i).getJml()) * Integer.parseInt(arrayPesanan.get(i).getHarga_sale());
        }
        return String.valueOf(total);
    }

    public static String getTotalHargaPesanan() {
        int total = 0;
        for (int i = 0; i < arrayPesanan.size(); i++) {
            total += Integer.parseInt(arrayPesanan.get(i).getTotal_harga());
        }
        return String.valueOf(total);
    }

    public static String getPotonganDiskon() {
        int potongan = 0;
        int subtotal = Integer.parseInt(getTotalHargaPesanan());
        int diskon = Integer.parseInt(getDiskon());
        if (getTipediskon().equals("Persen")) {
            double persen = diskon / 100.0f;
            double totaldis = subtotal * persen;
            potongan = (int) totaldis;
        } else if (getTipediskon().equals("Nominal")) {
            potongan = diskon;

        }
        return String.valueOf(potongan);
    }

    public static String getTotalHargawithDiskon() {
        int subtotal = Integer.parseInt(getTotalHargaPesanan());
        int totalharga = subtotal - Integer.parseInt(getPotonganDiskon());
        return String.valueOf(totalharga);

    }

    public static List<ProdukPesananModel> getArrayDiskonProduk() {
        List<ProdukPesananModel> list = new ArrayList<>();
        for (ProdukPesananModel model : arrayPesanan) {
            if (!model.getHarga_diskon().equals("0")) {
                list.add(model);
            }
        }
        return list;
    }

    public static void updateQty(int index, int qty) {
        if (index < 0 || index >= arrayPesanan.size()) return;

        if (qty < 1) qty = 1;

        ProdukPesananModel model = arrayPesanan.get(index);

        int harga = Integer.parseInt(model.getHarga());
        int totalHarga = qty * harga;

        model.setJml(String.valueOf(qty));
        model.setTotal_harga(String.valueOf(totalHarga));

        if (adapter != null) {
            adapter.notifyItemChanged(index);
        }
    }

    public static List<ProdukPesananModel> getArrayPesanan() {
        return arrayPesanan;
    }

    public static void setDataPelanggan(DataPelanggan data) {
        dataPelanggan = data;
    }

    public static void resetDataPelanggan() {
        dataPelanggan = new DataPelanggan("", "", "", "", "");
    }

    public static Boolean isPelangganEmpty() {
        return dataPelanggan.jenisKedatangan.isEmpty();
    }

    public static Boolean isDiskonEmpty() {
        return dataPelanggan.getDiskon().isEmpty() || dataPelanggan.getDiskon().equals("0");
    }

    public static String getNamaPelanggan() {
        return dataPelanggan.getNamaPelanggan();
    }

    public static void setNamaPelanggan(String namaPelanggan) {
        dataPelanggan.setNamaPelanggan(namaPelanggan);
    }

    public static String getIdMember() {
        return dataPelanggan.getIdMember();
    }

    public static void setIdMember(String idMember) {
        dataPelanggan.setIdMember(idMember);
    }

    public static String getJenisKedatangan() {
        return dataPelanggan.getJenisKedatangan();
    }

    public static void setJenisKedatangan(String jenisKedatangan) {
        dataPelanggan.setJenisKedatangan(jenisKedatangan);
    }

    public static String getDiskon() {
        return dataPelanggan.getDiskon();
    }

    public static void setDiskon(String diskon) {
        dataPelanggan.setDiskon(diskon);

    }

    public static String getTipediskon() {
        return dataPelanggan.getTipediskon();
    }

    public static void setTipediskon(String tipediskon) {
        dataPelanggan.setTipediskon(tipediskon);
    }

    public static class DataPelanggan {
        String namaPelanggan;
        String idMember;
        String jenisKedatangan;
        String diskon;
        String tipediskon;


        public DataPelanggan(String namaPelanggan, String idMember, String jenisKedatangan, String diskon, String tipediskon) {
            this.namaPelanggan = namaPelanggan;
            this.idMember = idMember;
            this.jenisKedatangan = jenisKedatangan;
            this.diskon = diskon;
            this.tipediskon = tipediskon;
        }

        public String getNamaPelanggan() {
            return namaPelanggan;
        }

        public void setNamaPelanggan(String namaPelanggan) {
            this.namaPelanggan = namaPelanggan;
        }

        public String getIdMember() {
            return idMember;
        }

        public void setIdMember(String idMember) {
            this.idMember = idMember;
        }

        public String getJenisKedatangan() {
            return jenisKedatangan;
        }

        public void setJenisKedatangan(String jenisKedatangan) {
            this.jenisKedatangan = jenisKedatangan;
        }

        public String getDiskon() {
            return diskon;
        }

        public void setDiskon(String diskon) {
            this.diskon = diskon;
        }

        public String getTipediskon() {
            return tipediskon;
        }

        public void setTipediskon(String tipediskon) {
            this.tipediskon = tipediskon;
        }
    }
}
