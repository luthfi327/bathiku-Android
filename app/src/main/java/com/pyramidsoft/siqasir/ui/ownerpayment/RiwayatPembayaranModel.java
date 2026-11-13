package com.pyramidsoft.siqasir.ui.ownerpayment;

/**
 * RiwayatPembayaranModel
 *
 * POJO untuk satu item dari response endpoint:
 *   GET /api/payment/history/{kode_owner}
 *
 * Field disesuaikan persis dengan kolom yang dikembalikan server:
 *   id, external_id, amount, durasi_bulan, status, harga_kategori,
 *   paid_at, created_at, expired_at, old_exp_date, new_exp_date
 */
public class RiwayatPembayaranModel {

    private long   id;
    private String external_id;
    private long   amount;
    private int    durasi_bulan;
    private String status;          // PAID | PENDING | CANCELLED | EXPIRED
    private String harga_kategori;  // normal | promo_bulan4 | trial_promo
    private String paid_at;
    private String created_at;
    private String expired_at;
    private String old_exp_date;
    private String new_exp_date;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public RiwayatPembayaranModel(
            long   id,
            String external_id,
            long   amount,
            int    durasi_bulan,
            String status,
            String harga_kategori,
            String paid_at,
            String created_at,
            String expired_at,
            String old_exp_date,
            String new_exp_date
    ) {
        this.id             = id;
        this.external_id    = external_id;
        this.amount         = amount;
        this.durasi_bulan   = durasi_bulan;
        this.status         = status;
        this.harga_kategori = harga_kategori;
        this.paid_at        = paid_at;
        this.created_at     = created_at;
        this.expired_at     = expired_at;
        this.old_exp_date   = old_exp_date;
        this.new_exp_date   = new_exp_date;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public long   getId()            { return id; }
    public String getExternalId()    { return external_id; }
    public long   getAmount()        { return amount; }
    public int    getDurasiBulan()   { return durasi_bulan; }
    public String getStatus()        { return status; }
    public String getHargaKategori() { return harga_kategori; }
    public String getPaidAt()        { return paid_at; }
    public String getCreatedAt()     { return created_at; }
    public String getExpiredAt()     { return expired_at; }
    public String getOldExpDate()    { return old_exp_date; }
    public String getNewExpDate()    { return new_exp_date; }
}