package com.pyramidsoft.siqasir.ui.daftarpesanan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DaftarPesananModel extends ViewModel {

    private MutableLiveData<String> mText;

    public DaftarPesananModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is daftarpesanan fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
