package com.pyramidsoft.siqasir.ui.bukatutup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BukaTutupViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public BukaTutupViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Buka Tutup Kasir fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
